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

public class MassiveApiTest {

    public static void main(String[] args) throws Exception {

        String apiKey = "6ZJDKZ62YQCQMTX5";

        HttpClient client = HttpClient.newHttpClient();
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        // ✅ 你要搜索的资产关键词（股票 + ETF + commodity）
        List<String> keywords = List.of(
                "Apple"
//                "Tesla",
//                "Microsoft",
//                "Amazon",
//                "Alphabet",
//                "Nvidia",
//                "Meta",
//                "SPY",
//                "VOO",
//                "S&P 500",
//                "SPDR",
//                "Vanguard 500",
//                "TLT",
//                "IEF",
//                "Treasury Bond",
//                "20+ Year Treasury",
//                "VNQ",
//                "Real Estate ETF",
//                "USO",
//                "Oil ETF",
//                "United States Oil"
        );

        for (String keyword : keywords) {

            String url = "https://www.alphavantage.co/query"
                    + "?function=SYMBOL_SEARCH"
                    + "&keywords=" + URLEncoder.encode(keyword, StandardCharsets.UTF_8)
                    + "&apikey=" + apiKey;

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

            if (root.has("Note")) {
                System.out.println("Rate limit reached:");
                System.out.println(root.get("Note").asText());
//                continue;
            }

            if (root.has("Information")) {
                System.out.println("API info:");
                System.out.println(root.get("Information").asText());
//                continue;
            }

            JsonNode matches = root.get("bestMatches");

            if (matches == null || !matches.isArray() || matches.isEmpty()) {
                System.out.println("No results");
                continue;
            }

            for (int i = 0; i < Math.min(5, matches.size()); i++) {
                JsonNode m = matches.get(i);

                String symbol = m.get("1. symbol").asText();
                String name = m.get("2. name").asText();
                String type = m.get("3. type").asText();
                String region = m.get("4. region").asText();

                System.out.println(
                        "symbol=" + symbol
                                + " | name=" + name
                                + " | type=" + type
                                + " | region=" + region
                );
            }


            // ⚠️ 避免免费 API 限流
            Thread.sleep(12000);
        }
    }
}