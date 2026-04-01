import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class FinhubSearch {

    public static void main(String[] args) throws Exception {

        String apiKey = "d75tfkpr01qm4b7s5fbgd75tfkpr01qm4b7s5fc0";

        HttpClient client = HttpClient.newHttpClient();
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        boolean printedRawExample = false;

        // Finnhub 搜索更适合：股票用公司名，ETF/商品用 ticker
        List<String> keywords = List.of(
                "FB"
//                "Tesla",
//                "Microsoft",
//                "Amazon",
//                "Meta",
//                "Nvidia",
//                "SPY",
//                "VOO",
//                "QQQ",
//                "BND",
//                "TLT",
//                "IEF",
//                "GLD",
//                "SLV",
//                "VNQ",
//                "USO"
        );

        for (String keyword : keywords) {

            String url = "https://finnhub.io/api/v1/search"
                    + "?q=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8)
                    + "&token=" + apiKey;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .GET()
                    .build();

            HttpResponse<String> response =
                    client.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("\n=============================");
            System.out.println("Keyword: " + keyword);
            System.out.println("Status: " + response.statusCode());

            JsonNode root = mapper.readTree(response.body());

            if (!printedRawExample) {
                System.out.println("Raw example response format:");
                System.out.println(mapper.writeValueAsString(root));
                printedRawExample = true;
            }

            if (root.has("error")) {
                System.out.println("API error:");
                System.out.println(root.get("error").asText());
                continue;
            }

            JsonNode results = root.get("result");
            JsonNode count = root.get("count");

            if (count != null) {
                System.out.println("Match count: " + count.asInt());
            }

            if (results == null || !results.isArray() || results.isEmpty()) {
                System.out.println("No results");
                System.out.println("Full response:");
                System.out.println(mapper.writeValueAsString(root));
                continue;
            }

            for (int i = 0; i < Math.min(5, results.size()); i++) {
                JsonNode item = results.get(i);

                String symbol = text(item, "symbol");
                String displaySymbol = text(item, "displaySymbol");
                String description = text(item, "description");
                String type = text(item, "type");

                System.out.println(
                        "symbol=" + symbol
                                + " | displaySymbol=" + displaySymbol
                                + " | description=" + description
                                + " | type=" + type
                );
            }

            Thread.sleep(1200);
        }
    }

    private static String text(JsonNode node, String fieldName) {
        JsonNode field = node.get(fieldName);
        return field == null ? "" : field.asText();
    }
}