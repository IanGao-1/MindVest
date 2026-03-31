package com.reserio.financialmanagement.service;

import com.reserio.financialmanagement.dto.YahooQuoteDTO;
import com.reserio.financialmanagement.model.Asset;
import com.reserio.financialmanagement.repository.AssetRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.quotes.stock.StockQuote;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class MarketDataService {
    private static final String FALLBACK_QUOTE_URL =
            "https://c4rm9elh30.execute-api.us-east-1.amazonaws.com/default/cachedPriceData?ticker={ticker}";

    @Autowired
    private AssetRepository assetRepository;

    private final RestTemplate restTemplate = new RestTemplate();

    public YahooQuoteDTO getYahooQuote(String ticker) {
        try {
            Stock stock = YahooFinance.get(ticker);
            if (stock == null || stock.getQuote() == null || stock.getQuote().getPrice() == null) {
                return getFallbackQuote(ticker);
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
            return getFallbackQuote(ticker);
        }
    }

    public double getMarketPrice(String ticker) {
        return getYahooQuote(ticker).getPrice().doubleValue();
    }

    public void updateAssetPrices() {
        List<Asset> assets = assetRepository.findAll();
        for (Asset asset : assets) {
            double marketPrice = getMarketPrice(asset.getTicker());
            asset.setCurrentPrice(marketPrice);
            asset.setLastUpdated(new Date());
            assetRepository.save(asset);
        }
    }

    public void updateAssetPrice(Long assetId) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new RuntimeException("Asset not found"));
        double marketPrice = getMarketPrice(asset.getTicker());
        asset.setCurrentPrice(marketPrice);
        asset.setLastUpdated(new Date());
        assetRepository.save(asset);
    }

    private YahooQuoteDTO getFallbackQuote(String ticker) {
        try {
            Map<String, Object> response = restTemplate.getForObject(
                    FALLBACK_QUOTE_URL,
                    Map.class,
                    "TSLA"
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
                dto.setLatestTimestamp(String.valueOf(timestampList.get(timestampList.size() - 1)));
            }
            return dto;
        } catch (RestClientException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Failed to fetch market data from fallback API", ex);
        }
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
        if (value instanceof Number) {
            return BigDecimal.valueOf(((Number) value).doubleValue());
        }
        if (value instanceof String) {
            return new BigDecimal((String) value);
        }
        throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Fallback market data numeric field is invalid");
    }
}
