import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

public class MassiveApiTest {

    public static void main(String[] args) throws Exception {

        String apiKey = "6ZJDKZ62YQCQMTX5";
//        String ticker = "AAPL";
        String url = "https://www.alphavantage.co/query?function=GLOBAL_QUOTEl&symbol=IBM&"+
                "apikey=6ZJDKZ62YQCQMTX5";

        HttpClient client = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .GET()
                .build();

        HttpResponse<String> response =
                client.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("Status: " + response.statusCode());

        ObjectMapper mapper = new ObjectMapper();
        mapper.enable(SerializationFeature.INDENT_OUTPUT);

        Object json = mapper.readValue(response.body(), Object.class);

        System.out.println("Formatted Response:");
        System.out.println(mapper.writeValueAsString(json));
    }
}