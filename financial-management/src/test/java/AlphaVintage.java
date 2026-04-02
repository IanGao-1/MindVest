import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class AlphaVintage {

    private static final String API_KEY = "6ZJDKZ62YQCQMTX5";
    private static final String DATA_DIR = "/Users/yijian/Desktop/MindVest/financial-management/src/main/resources/data/history";

    public static void main(String[] args) throws Exception {
//        List<String> symbols = List.of("AAPL",
//                "TSLA", "MSFT", "AMZN", "QQQ", "BND", "GLD", "VNQ");
        List<String> symbols = List.of("C",
                "META", "NVDA", "SPY", "VOO", "TLT", "GLD", "SLV","GBP");
        for (String symbol : symbols) {
            downloadAndSaveDailyHistory(symbol);
            Thread.sleep(15000);
        }
    }

    public static void downloadAndSaveDailyHistory(String symbol) throws Exception {
        String url = "https://www.alphavantage.co/query"
                + "?function=TIME_SERIES_DAILY"
                + "&symbol=" + symbol
                + "&outputsize=compact"
                + "&apikey=" + API_KEY;

        HttpClient client = HttpClient.newHttpClient();
        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("=============================");
        System.out.println("Symbol: " + symbol);
        System.out.println("Status: " + response.statusCode());

        JsonNode root = mapper.readTree(response.body());

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

        if (root.has("Error Message")) {
            System.out.println("API error:");
            System.out.println(root.get("Error Message").asText());
            return;
        }

        JsonNode meta = root.get("Meta Data");
        JsonNode timeSeries = root.get("Time Series (Daily)");

        if (timeSeries == null || !timeSeries.fields().hasNext()) {
            System.out.println("No time series data found");
            System.out.println(mapper.writeValueAsString(root));
            return;
        }

        List<DailyPrice> prices = new ArrayList<>();
        Iterator<String> dates = timeSeries.fieldNames();

        while (dates.hasNext()) {
            String date = dates.next();
            JsonNode day = timeSeries.get(date);

            DailyPrice price = new DailyPrice();
            price.date = date;
            price.open = day.get("1. open").asDouble();
            price.high = day.get("2. high").asDouble();
            price.low = day.get("3. low").asDouble();
            price.close = day.get("4. close").asDouble();
            price.volume = day.get("5. volume").asLong();

            prices.add(price);
        }

        LocalHistoryFile historyFile = new LocalHistoryFile();
        historyFile.symbol = symbol;
        historyFile.lastRefreshed = meta != null && meta.has("3. Last Refreshed")
                ? meta.get("3. Last Refreshed").asText()
                : "";
        historyFile.outputSize = meta != null && meta.has("4. Output Size")
                ? meta.get("4. Output Size").asText()
                : "";
        historyFile.timeZone = meta != null && meta.has("5. Time Zone")
                ? meta.get("5. Time Zone").asText()
                : "";

        Path directory = Paths.get(DATA_DIR);
        Files.createDirectories(directory);

        Path outputPath = directory.resolve(symbol + ".json");
        historyFile.prices = mergeWithExistingHistory(outputPath, prices, mapper);
        mapper.writeValue(outputPath.toFile(), historyFile);

        System.out.println("Saved to: " + outputPath.toAbsolutePath());
        System.out.println("Records: " + historyFile.prices.size());

        if (!historyFile.prices.isEmpty()) {
            DailyPrice latest = historyFile.prices.get(0);
            System.out.println("Latest close: " + latest.close + " on " + latest.date);
        }
    }

    private static List<DailyPrice> mergeWithExistingHistory(Path outputPath, List<DailyPrice> latestPrices, ObjectMapper mapper)
            throws Exception {
        Map<String, DailyPrice> mergedByDate = new LinkedHashMap<>();

        for (DailyPrice latestPrice : latestPrices) {
            mergedByDate.put(latestPrice.date, latestPrice);
        }

        if (Files.exists(outputPath)) {
            LocalHistoryFile existingHistory = mapper.readValue(outputPath.toFile(), LocalHistoryFile.class);
            if (existingHistory != null && existingHistory.prices != null) {
                for (DailyPrice existingPrice : existingHistory.prices) {
                    // Keep older local dates such as 2025-11-05 when the compact API no longer returns them.
                    mergedByDate.putIfAbsent(existingPrice.date, existingPrice);
                }
            }
        }

        return mergedByDate.values().stream()
                .sorted(Comparator.comparing((DailyPrice price) -> price.date).reversed())
                .toList();
    }

    public static class DailyPrice {
        public String date;
        public double open;
        public double high;
        public double low;
        public double close;
        public long volume;
    }

    public static class LocalHistoryFile {
        public String symbol;
        public String lastRefreshed;
        public String outputSize;
        public String timeZone;
        public List<DailyPrice> prices;
    }
}
