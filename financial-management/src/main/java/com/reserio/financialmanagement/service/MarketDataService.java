package com.reserio.financialmanagement.service;

import com.reserio.financialmanagement.dto.MarketHistoryPointDTO;
import com.reserio.financialmanagement.dto.YahooQuoteDTO;
import com.reserio.financialmanagement.model.Asset;
import com.reserio.financialmanagement.repository.AssetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.quotes.stock.StockQuote;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
public class MarketDataService {
    private static final String FALLBACK_QUOTE_URL =
            "https://c4rm9elh30.execute-api.us-east-1.amazonaws.com/default/cachedPriceData?ticker={ticker}";
    private static final String FINNHUB_QUOTE_URL =
            "https://finnhub.io/api/v1/quote?symbol={ticker}&token={token}";
    private static final Set<String> FINNHUB_SUPPORTED_TICKERS =
            Arrays.stream(new String[]{"AAPL", "TSLA", "MSFT", "AMZN", "META", "NVDA", "C"})
                    .collect(Collectors.toSet());
    private static final Map<String, String> TICKER_ALIASES = new HashMap<>();

    static {
        TICKER_ALIASES.put("META", "FB");
    }

    @Autowired
    private AssetRepository assetRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    private final Map<String, CachedQuote> quoteCache = new ConcurrentHashMap<>();

    @Value("${market-data.finnhub.api-key:d75tfkpr01qm4b7s5fbgd75tfkpr01qm4b7s5fc0}")
    private String finnhubApiKey;

    @Value("${market-data.finnhub.cache-ttl-ms:300000}")
    private long cacheTtlMs;

    public YahooQuoteDTO getYahooQuote(String ticker) {
        String normalizedTicker = normalizeTicker(ticker);
        if (FINNHUB_SUPPORTED_TICKERS.contains(normalizedTicker)) {
            return getCachedFinnhubQuote(normalizedTicker, false);
        }

        try {
            Stock stock = YahooFinance.get(normalizedTicker);
            if (stock == null || stock.getQuote() == null || stock.getQuote().getPrice() == null) {
                return getFallbackQuote(normalizedTicker);
            }

            StockQuote quote = stock.getQuote();
            YahooQuoteDTO dto = new YahooQuoteDTO();
            dto.setSymbol(stock.getSymbol());
            dto.setName(stock.getName());
            dto.setSource("yahoo-finance");
            dto.setStockExchange(stock.getStockExchange());
            dto.setCurrency(stock.getCurrency());
            dto.setPrice(quote.getPrice());
            dto.setChange(quote.getChange());
            dto.setChangeInPercent(quote.getChangeInPercent());
            dto.setOpen(quote.getOpen());
            dto.setPreviousClose(quote.getPreviousClose());
            dto.setDayLow(quote.getDayLow());
            dto.setDayHigh(quote.getDayHigh());
            return dto;
        } catch (Exception ex) {
            return getFallbackQuote(normalizedTicker);
        }
    }

    public double getMarketPrice(String ticker) {
        return getYahooQuote(ticker).getPrice().doubleValue();
    }

    public List<MarketHistoryPointDTO> getPriceHistory(String ticker) {
        String historyTicker = TICKER_ALIASES.getOrDefault(normalizeTicker(ticker), normalizeTicker(ticker));
        try {
            Map<String, Object> response = restTemplate.getForObject(
                    FALLBACK_QUOTE_URL,
                    Map.class,
                    historyTicker
            );
            if (response == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Market data response is empty");
            }

            Map<String, Object> priceData = getMap(response, "price_data");
            List<?> timestampList = getList(priceData, "timestamp");
            List<?> openList = getList(priceData, "open");
            List<?> highList = getList(priceData, "high");
            List<?> lowList = getList(priceData, "low");
            List<?> closeList = getList(priceData, "close");

            List<MarketHistoryPointDTO> history = new ArrayList<>();
            int size = closeList.size();
            for (int index = 0; index < size; index++) {
                MarketHistoryPointDTO point = new MarketHistoryPointDTO();
                Object timestampValue = index < timestampList.size() ? timestampList.get(index) : null;
                point.setTimestamp(formatTimestamp(timestampValue));
                point.setTimestampValue(convertTimestampValue(timestampValue));
                point.setOpen(index < openList.size() ? getDecimal(openList, index) : null);
                point.setHigh(index < highList.size() ? getDecimal(highList, index) : null);
                point.setLow(index < lowList.size() ? getDecimal(lowList, index) : null);
                point.setClose(getDecimal(closeList, index));
                history.add(point);
            }
            history.sort(Comparator.comparing(
                    MarketHistoryPointDTO::getTimestampValue,
                    Comparator.nullsLast(Long::compareTo)
            ));
            return history;
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to fetch market data history", ex);
        }
    }

    public void updateAssetPrices() {
        List<Asset> assets = assetRepository.findAll();
        for (Asset asset : assets) {
            applyLatestQuote(asset, getYahooQuote(asset.getTicker()));
        }
    }

    public void updateAssetPrice(Long assetId) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new RuntimeException("Asset not found"));
        applyLatestQuote(asset, getYahooQuote(asset.getTicker()));
    }

    @Scheduled(fixedDelayString = "${market-data.finnhub.refresh-ms:300000}", initialDelayString = "${market-data.finnhub.refresh-ms:300000}")
    public void refreshTrackedQuotes() {
        Set<String> trackedTickers = new HashSet<>(quoteCache.keySet());
        trackedTickers.addAll(assetRepository.findAll().stream()
                .map(Asset::getTicker)
                .filter(ticker -> ticker != null && !ticker.trim().isEmpty())
                .map(this::normalizeTicker)
                .collect(Collectors.toSet()));

        for (String ticker : trackedTickers) {
            try {
                YahooQuoteDTO quote = FINNHUB_SUPPORTED_TICKERS.contains(ticker)
                        ? getCachedFinnhubQuote(ticker, true)
                        : getYahooQuote(ticker);
                assetRepository.findAllByTickerOrderByIdAsc(ticker).forEach(asset -> applyLatestQuote(asset, quote));
            } catch (ResponseStatusException ignored) {
            }
        }
    }

    private YahooQuoteDTO getFallbackQuote(String ticker) {
        try {
            Map<String, Object> response = restTemplate.getForObject(
                    FALLBACK_QUOTE_URL,
                    Map.class,
                    ticker
            );
            if (response == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Fallback market data response is empty");
            }

            Map<String, Object> priceData = getMap(response, "price_data");
            List<?> closeList = getList(priceData, "close");
            if (closeList.isEmpty()) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Ticker not found: " + ticker);
            }

            List<?> openList = getList(priceData, "open");
            List<?> highList = getList(priceData, "high");
            List<?> lowList = getList(priceData, "low");
            List<?> timestampList = getList(priceData, "timestamp");

            BigDecimal latestClose = getDecimal(closeList, closeList.size() - 1);
            BigDecimal previousClose = closeList.size() > 1
                    ? getDecimal(closeList, closeList.size() - 2)
                    : latestClose;
            BigDecimal latestOpen = getLastDecimal(openList, latestClose);
            BigDecimal latestHigh = getLastDecimal(highList, latestClose);
            BigDecimal latestLow = getLastDecimal(lowList, latestClose);
            BigDecimal change = latestClose.subtract(previousClose);
            BigDecimal changeInPercent = previousClose.compareTo(BigDecimal.ZERO) == 0
                    ? BigDecimal.ZERO
                    : change.multiply(BigDecimal.valueOf(100)).divide(previousClose, 4, RoundingMode.HALF_UP);

            YahooQuoteDTO dto = new YahooQuoteDTO();
            dto.setSymbol((String) response.getOrDefault("ticker", ticker));
            dto.setName((String) response.getOrDefault("ticker", ticker));
            dto.setSource("cached-price-data");
            dto.setStockExchange(null);
            dto.setCurrency(null);
            dto.setPrice(latestClose);
            dto.setChange(change);
            dto.setChangeInPercent(changeInPercent);
            dto.setOpen(latestOpen);
            dto.setPreviousClose(previousClose);
            dto.setDayLow(latestLow);
            dto.setDayHigh(latestHigh);
            if (!timestampList.isEmpty()) {
                Long timestampValue = convertTimestampValue(timestampList.get(timestampList.size() - 1));
                dto.setLatestTimestamp(timestampValue == null ? null : Instant.ofEpochMilli(timestampValue)
                        .atZone(ZoneId.of("Asia/Shanghai"))
                        .toLocalDateTime()
                        .toString());
            }
            return dto;
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to fetch market data from fallback API", ex);
        }
    }

    private YahooQuoteDTO getCachedFinnhubQuote(String ticker, boolean forceRefresh) {
        String normalizedTicker = normalizeTicker(ticker);
        CachedQuote cached = quoteCache.get(normalizedTicker);
        long now = System.currentTimeMillis();
        if (!forceRefresh && cached != null && now - cached.fetchedAt <= cacheTtlMs) {
            return cached.quote;
        }

        YahooQuoteDTO quote = fetchFinnhubQuote(normalizedTicker);
        quoteCache.put(normalizedTicker, new CachedQuote(quote, now));
        return quote;
    }

    private YahooQuoteDTO fetchFinnhubQuote(String ticker) {
        try {
            Map<String, Object> response = restTemplate.getForObject(
                    FINNHUB_QUOTE_URL,
                    Map.class,
                    ticker,
                    finnhubApiKey
            );
            if (response == null) {
                throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Finnhub quote response is empty");
            }

            YahooQuoteDTO dto = new YahooQuoteDTO();
            dto.setSymbol(ticker);
            dto.setName(ticker);
            dto.setSource("finnhub");
            dto.setPrice(getDecimal(response.get("c")));
            dto.setChange(getDecimal(response.get("d")));
            BigDecimal percentChange = getDecimal(response.get("dp"));
            dto.setChangeInPercent(percentChange);
            dto.setOpen(getDecimal(response.get("o")));
            dto.setPreviousClose(getDecimal(response.get("pc")));
            dto.setDayLow(getDecimal(response.get("l")));
            dto.setDayHigh(getDecimal(response.get("h")));
            Long timestampValue = convertTimestampValue(response.get("t"));
            dto.setLatestTimestamp(timestampValue == null ? null : Instant.ofEpochMilli(timestampValue)
                    .atZone(ZoneId.of("Asia/Shanghai"))
                    .toLocalDateTime()
                    .toString());
            return dto;
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to fetch market data from Finnhub", ex);
        }
    }

    private void applyLatestQuote(Asset asset, YahooQuoteDTO quote) {
        if (asset == null || quote == null || quote.getPrice() == null) {
            return;
        }
        asset.setCurrentPrice(quote.getPrice().doubleValue());
        if (quote.getLatestTimestamp() != null) {
            Long timestampValue = convertTimestampValue(quote.getLatestTimestamp());
            asset.setLastUpdated(timestampValue == null ? new Date() : new Date(timestampValue));
        } else {
            asset.setLastUpdated(new Date());
        }
        assetRepository.save(asset);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getMap(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (!(value instanceof Map)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Fallback market data format is invalid");
        }
        return (Map<String, Object>) value;
    }

    private List<?> getList(Map<String, Object> source, String key) {
        Object value = source.get(key);
        if (!(value instanceof List)) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Fallback market data format is invalid");
        }
        return (List<?>) value;
    }

    private BigDecimal getLastDecimal(List<?> values, BigDecimal defaultValue) {
        if (values == null || values.isEmpty()) {
            return defaultValue;
        }
        return getDecimal(values, values.size() - 1);
    }

    private BigDecimal getDecimal(List<?> values, int index) {
        Object value = values.get(index);
        return getDecimal(value);
    }

    private BigDecimal getDecimal(Object value) {
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        if (value instanceof String) {
            return new BigDecimal((String) value);
        }
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Fallback market data numeric field is invalid");
    }

    private String formatTimestamp(Object value) {
        if (value == null) {
            return null;
        }
        Long numericTimestamp = convertTimestampValue(value);
        if (numericTimestamp == null) {
            return String.valueOf(value);
        }
        return Instant.ofEpochMilli(numericTimestamp)
                .atZone(ZoneId.of("Asia/Shanghai"))
                .toLocalDate()
                .toString();
    }

    private Long convertTimestampValue(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            long numeric = ((Number) value).longValue();
            return numeric < 1_000_000_000_000L ? numeric * 1000L : numeric;
        }

        String text = String.valueOf(value).trim();
        if (text.isEmpty()) {
            return null;
        }
        if (text.matches("^\\d+$")) {
            long numeric = Long.parseLong(text);
            return numeric < 1_000_000_000_000L ? numeric * 1000L : numeric;
        }

        try {
            return Instant.parse(text).toEpochMilli();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDateTime.parse(text).atZone(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();
        } catch (DateTimeParseException ignored) {
        }
        try {
            return LocalDate.parse(text).atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant().toEpochMilli();
        } catch (DateTimeParseException ignored) {
        }
        return null;
    }

    private String normalizeTicker(String ticker) {
        return ticker == null ? "" : ticker.trim().toUpperCase();
    }

    private static class CachedQuote {
        private final YahooQuoteDTO quote;
        private final long fetchedAt;

        private CachedQuote(YahooQuoteDTO quote, long fetchedAt) {
            this.quote = quote;
            this.fetchedAt = fetchedAt;
        }
    }
}
