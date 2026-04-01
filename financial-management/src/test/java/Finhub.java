import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class Finhub {

    public static void main(String[] args) throws Exception {

        String apiKey = "d75tfkpr01qm4b7s5fbgd75tfkpr01qm4b7s5fc0";
        String symbol = "AAPL";

        String url = "https://finnhub.io/api/v1/quote"
                + "?symbol=" + symbol
                + "&token=" + apiKey;

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

        if (root.has("error")) {
            System.out.println("API error:");
            System.out.println(root.get("error").asText());
            return;
        }

        System.out.println("Formatted Response:");
        System.out.println(mapper.writeValueAsString(root));

        double currentPrice = root.path("c").asDouble();
        double highPrice = root.path("h").asDouble();
        double lowPrice = root.path("l").asDouble();
        double openPrice = root.path("o").asDouble();
        double previousClose = root.path("pc").asDouble();
        long timestamp = root.path("t").asLong();

        double change = currentPrice - previousClose;
        double changePercent = previousClose == 0 ? 0 : (change / previousClose) * 100;

        System.out.println("\n===== Parsed Result =====");
        System.out.println("Symbol: " + symbol);
        System.out.println("Current Price: " + currentPrice);
        System.out.println("Open: " + openPrice);
        System.out.println("High: " + highPrice);
        System.out.println("Low: " + lowPrice);
        System.out.println("Previous Close: " + previousClose);
        System.out.println("Change: " + String.format("%.2f", change));
        System.out.println("Change %%: " + String.format("%.2f%%", changePercent));
        System.out.println("Timestamp: " + timestamp);
    }
}