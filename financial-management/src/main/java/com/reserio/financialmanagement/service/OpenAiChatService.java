package com.reserio.financialmanagement.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.reserio.financialmanagement.dto.OpenAiChatRequest;
import com.reserio.financialmanagement.dto.OpenAiChatResponse;
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
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@Service
public class OpenAiChatService {

    private final ObjectMapper objectMapper = new ObjectMapper();

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
        requestBody.put("input", request.getMessage().trim());
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
}
