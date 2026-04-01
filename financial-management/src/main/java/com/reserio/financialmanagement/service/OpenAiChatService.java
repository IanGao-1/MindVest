package com.reserio.financialmanagement.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.reserio.financialmanagement.dto.OpenAiChatRequest;
import com.reserio.financialmanagement.dto.OpenAiChatResponse;
import com.reserio.financialmanagement.model.Asset;
import com.reserio.financialmanagement.repository.AssetRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Service
public class OpenAiChatService {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final AssetRepository assetRepository;
    private final MarketDataService marketDataService;

    private HttpClient httpClient;

    @Value("${openai.api-key:}")
    private String apiKey;

    @Value("${openai.base-url:https://api.openai.com/v1}")
    private String baseUrl;

    @Value("${openai.model:gpt-5-nano}")
    private String defaultModel;

    @Value("${openai.chat.store:true}")
    private boolean defaultStore;

    @Value("${openai.proxy.enabled:true}")
    private boolean proxyEnabled;

    @Value("${openai.proxy.host:127.0.0.1}")
    private String proxyHost;

    @Value("${openai.proxy.port:10808}")
    private int proxyPort;

    @Value("${openai.proxy.type:HTTP}")
    private String proxyType;

    public OpenAiChatService(AssetRepository assetRepository, MarketDataService marketDataService) {
        this.assetRepository = assetRepository;
        this.marketDataService = marketDataService;
    }

    @PostConstruct
    public void init() {
        this.httpClient = buildHttpClient();
    }

    public OpenAiChatResponse chat(OpenAiChatRequest request) {
        validateRequest(request);

        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalArgumentException("OpenAI API key is not configured");
        }

        if ("SOCKS".equalsIgnoreCase(proxyType)) {
            throw new ResponseStatusException(
                    BAD_GATEWAY,
                    "Current backend client is configured for HTTP proxy only. Please set openai.proxy.type=HTTP for 127.0.0.1:10808."
            );
        }

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", getModel(request));
        requestBody.put("input", buildAnalysisPrompt(request.getMessage().trim()));
        requestBody.put("store", request.getStore() == null ? defaultStore : request.getStore());

        if (StringUtils.hasText(request.getSystemPrompt())) {
            requestBody.put("instructions", request.getSystemPrompt().trim());
        }

        try {
            String jsonBody = objectMapper.writeValueAsString(requestBody);
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(normalizeBaseUrl() + "/responses"))
                    .timeout(Duration.ofSeconds(60))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (httpResponse.statusCode() < 200 || httpResponse.statusCode() >= 300) {
                throw new ResponseStatusException(
                        BAD_GATEWAY,
                        "OpenAI request failed: " + httpResponse.statusCode() + " " + httpResponse.body()
                );
            }

            Map<String, Object> body = objectMapper.readValue(httpResponse.body(), Map.class);
            return toChatResponse(body);
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (IOException | InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ResponseStatusException(
                    BAD_GATEWAY,
                    "Failed to reach OpenAI through proxy " + proxyType + "://" + proxyHost + ":" + proxyPort + " - " + ex.getMessage(),
                    ex
            );
        } catch (Exception ex) {
            throw new ResponseStatusException(INTERNAL_SERVER_ERROR, "Failed to call OpenAI API", ex);
        }
    }

    private void validateRequest(OpenAiChatRequest request) {
        if (request == null || !StringUtils.hasText(request.getMessage())) {
            throw new IllegalArgumentException("message cannot be empty");
        }
    }

    private String getModel(OpenAiChatRequest request) {
        return StringUtils.hasText(request.getModel()) ? request.getModel().trim() : defaultModel;
    }

    private String buildAnalysisPrompt(String userQuestion) {
        String dataContext = buildDataContext();
        return "你是专业的个人资产分析师，只能基于以下真实持仓数据回答问题，禁止编造信息。\n"
                + "持仓数据：" + dataContext + "\n"
                + "用户问题：" + userQuestion + "\n"
                + "请用简洁、专业、易懂的英文回答，包含收益计算、成本分析、风险提示。";
    }

    private String buildDataContext() {
        List<Asset> assets = assetRepository.findAll();
        if (assets.isEmpty()) {
            return "当前数据库中没有持仓记录。";
        }

        Map<String, HoldingSnapshot> holdingMap = aggregateHoldingsByTicker(assets);
        List<String> sections = new ArrayList<>();
        sections.add("当前持仓摘要：" + holdingMap.values().stream()
                .map(this::formatHoldingSummary)
                .collect(Collectors.joining(" | ")));

        for (HoldingSnapshot snapshot : holdingMap.values()) {
            sections.add(buildTickerHistorySection(snapshot));
        }
        return String.join("\n", sections);
    }

    private Map<String, HoldingSnapshot> aggregateHoldingsByTicker(List<Asset> assets) {
        Map<String, HoldingSnapshot> holdingMap = new LinkedHashMap<>();
        for (Asset asset : assets) {
            String ticker = normalizeTicker(asset.getTicker());
            if (!StringUtils.hasText(ticker)) {
                continue;
            }

            HoldingSnapshot snapshot = holdingMap.computeIfAbsent(ticker, key -> new HoldingSnapshot());
            double quantity = safeNumber(asset.getQuantity());
            double avgCost = safeNumber(asset.getAvgCost());
            double currentPrice = safeNumber(asset.getCurrentPrice());

            snapshot.ticker = ticker;
            snapshot.name = StringUtils.hasText(asset.getName()) ? asset.getName() : ticker;
            snapshot.type = asset.getType();
            snapshot.totalQuantity += quantity;
            snapshot.totalCostBasis += quantity * avgCost;
            snapshot.totalMarketValue += quantity * currentPrice;
            snapshot.latestCurrentPrice = currentPrice;
        }

        holdingMap.values().forEach(snapshot -> {
            if (snapshot.totalQuantity > 0) {
                snapshot.weightedAvgCost = snapshot.totalCostBasis / snapshot.totalQuantity;
            }
            snapshot.unrealizedPnl = snapshot.totalMarketValue - snapshot.totalCostBasis;
        });
        return holdingMap;
    }

    private String formatHoldingSummary(HoldingSnapshot snapshot) {
        return snapshot.ticker
                + "(名称=" + snapshot.name
                + ", 类型=" + defaultText(snapshot.type, "UNKNOWN")
                + ", 持仓数量=" + formatNumber(snapshot.totalQuantity)
                + ", 持仓成本=" + formatNumber(snapshot.weightedAvgCost)
                + ", 最新价格=" + formatNumber(snapshot.latestCurrentPrice)
                + ", 持仓市值=" + formatNumber(snapshot.totalMarketValue)
                + ", 浮动盈亏=" + formatNumber(snapshot.unrealizedPnl)
                + ")";
    }

    private String buildTickerHistorySection(HoldingSnapshot snapshot) {
        JsonNode historyJson = marketDataService.getLocalHistoryJson(snapshot.ticker);
        List<DailyClosePoint> closePoints = extractDailyClosePoints(historyJson);
        String closeSeries = closePoints.stream()
                .map(point -> point.date + ":" + formatNumber(point.close))
                .collect(Collectors.joining(", "));

        return "标的=" + snapshot.ticker
                + " 的历史收盘价序列（2025-11-05 到 2026-03-31）:"
                + closeSeries;
    }

    private List<DailyClosePoint> extractDailyClosePoints(JsonNode historyJson) {
        JsonNode pricesNode = historyJson.path("prices");
        List<DailyClosePoint> points = new ArrayList<>();
        if (!pricesNode.isArray()) {
            return points;
        }

        for (JsonNode priceNode : pricesNode) {
            String date = priceNode.path("date").asText(null);
            if (!StringUtils.hasText(date) || priceNode.path("close").isMissingNode()) {
                continue;
            }

            DailyClosePoint point = new DailyClosePoint();
            point.date = LocalDate.parse(date);
            point.close = priceNode.path("close").asDouble();
            points.add(point);
        }

        points.sort(Comparator.comparing(item -> item.date));
        return points;
    }

    private HttpClient buildHttpClient() {
        HttpClient.Builder builder = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(30))
                .version(HttpClient.Version.HTTP_1_1);

        if (proxyEnabled && StringUtils.hasText(proxyHost) && proxyPort > 0) {
            builder.proxy(ProxySelector.of(new InetSocketAddress(proxyHost.trim(), proxyPort)));
        }

        return builder.build();
    }

    private String normalizeBaseUrl() {
        return baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    }

    private OpenAiChatResponse toChatResponse(Map<String, Object> body) {
        if (body == null) {
            throw new ResponseStatusException(BAD_GATEWAY, "OpenAI response is empty");
        }

        OpenAiChatResponse response = new OpenAiChatResponse();
        response.setId(asString(body.get("id")));
        response.setModel(asString(body.get("model")));
        response.setReply(extractReply(body));
        response.setCreatedAt(formatCreatedAt(body.get("created_at")));
        return response;
    }

    private String extractReply(Map<String, Object> body) {
        Object outputObject = body.get("output");
        if (!(outputObject instanceof List)) {
            throw new ResponseStatusException(BAD_GATEWAY, "OpenAI response format is invalid");
        }

        StringBuilder builder = new StringBuilder();
        List<?> outputItems = (List<?>) outputObject;
        for (Object outputItem : outputItems) {
            if (!(outputItem instanceof Map)) {
                continue;
            }

            Object contentObject = ((Map<?, ?>) outputItem).get("content");
            if (!(contentObject instanceof List)) {
                continue;
            }

            for (Object contentItem : (List<?>) contentObject) {
                if (!(contentItem instanceof Map)) {
                    continue;
                }

                Map<?, ?> contentMap = (Map<?, ?>) contentItem;
                if (!"output_text".equals(asString(contentMap.get("type")))) {
                    continue;
                }

                String text = asString(contentMap.get("text"));
                if (StringUtils.hasText(text)) {
                    if (builder.length() > 0) {
                        builder.append(System.lineSeparator());
                    }
                    builder.append(text);
                }
            }
        }

        if (builder.length() == 0) {
            throw new ResponseStatusException(BAD_GATEWAY, "OpenAI did not return text content");
        }
        return builder.toString();
    }

    private String formatCreatedAt(Object createdAt) {
        if (!(createdAt instanceof Number)) {
            return null;
        }
        return Instant.ofEpochSecond(((Number) createdAt).longValue())
                .atZone(ZoneId.of("Asia/Shanghai"))
                .toLocalDateTime()
                .toString();
    }

    private String asString(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private double safeNumber(Double value) {
        return value == null ? 0D : value;
    }

    private String formatNumber(double value) {
        return String.format("%.2f", value);
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private String normalizeTicker(String ticker) {
        if (!StringUtils.hasText(ticker)) {
            return "";
        }
        String normalizedTicker = ticker.trim().toUpperCase();
        return "APPL".equals(normalizedTicker) ? "AAPL" : normalizedTicker;
    }

    private static class HoldingSnapshot {
        private String ticker;
        private String name;
        private String type;
        private double totalQuantity;
        private double totalCostBasis;
        private double totalMarketValue;
        private double weightedAvgCost;
        private double latestCurrentPrice;
        private double unrealizedPnl;
    }

    private static class DailyClosePoint {
        private LocalDate date;
        private double close;
    }
}
