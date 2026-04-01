import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class Alpha1 {

    public static void main(String[] args) throws Exception {

        String apiKey = "6ZJDKZ62YQCQMTX5";
        String symbol = "AAPL";

        String url = "https://www.alphavantage.co/query"
                + "?function=TIME_SERIES_DAILY"
                + "&symbol=" + symbol
                + "&apikey=" + apiKey;

        HttpClient client = HttpClient.newHttpClient();
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("Status: " + response.statusCode());

        JsonNode root = mapper.readTree(response.body());

        // 处理限流或错误信息
        if (root.has("Note")) {
            System.out.println("Rate limit reached:");
            System.out.println(root.get("Note").asText());
            return;
        }

        if (root.has("Information")) {
            System.out.println("API info:");
            System.out.println(root.get("Information").asText());
            return;
        }

        System.out.println("Formatted Response:");
        System.out.println(mapper.writeValueAsString(root));

        JsonNode timeSeries = root.get("Time Series (Daily)");

        if (timeSeries == null || !timeSeries.fields().hasNext()) {
            System.out.println("No time series data found");
            return;
        }

        // 获取最新一天
        String latestDate = timeSeries.fieldNames().next();
        JsonNode latestData = timeSeries.get(latestDate);

        String open = latestData.get("1. open").asText();
        String high = latestData.get("2. high").asText();
        String low = latestData.get("3. low").asText();
        String close = latestData.get("4. close").asText();
        String volume = latestData.get("5. volume").asText();

        System.out.println("\n===== Latest Daily Data =====");
        System.out.println("Symbol: " + symbol);
        System.out.println("Date: " + latestDate);
        System.out.println("Open: " + open);
        System.out.println("High: " + high);
        System.out.println("Low: " + low);
        System.out.println("Close: " + close);
        System.out.println("Volume: " + volume);
    }
}