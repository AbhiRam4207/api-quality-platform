package api_quality_platform.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

public class OllamaConnectionTest {

    public static void main(String[] args) throws Exception {
        String apiKey = System.getProperty("OLLAMA_API_KEY", System.getenv("OLLAMA_API_KEY"));
        if (apiKey == null || apiKey.isBlank()) {
            System.err.println("OLLAMA_API_KEY is not set. Export it or pass as -DOLLAMA_API_KEY=...");
            System.exit(1);
        }

        String model = "gemma4:31b";
        String baseUrl = "https://ollama.com";

        Map<String, Object> body = Map.of(
                "model", model,
                "messages", List.of(
                        Map.of("role", "user", "content", "Reply with exactly one sentence: confirm you are Ollama running gemma4:31b.")
                ),
                "stream", false
        );

        RestClient client = RestClient.builder().baseUrl(baseUrl).build();

        System.out.println("Sending request to Ollama cloud API...");

        String jsonResponse = client.post()
                .uri("/api/chat")
                .contentType(MediaType.APPLICATION_JSON)
                .header("Authorization", "Bearer " + apiKey)
                .body(body)
                .retrieve()
                .body(String.class);

        ObjectMapper mapper = new ObjectMapper();
        JsonNode root = mapper.readTree(jsonResponse);
        String reply = root.path("message").path("content").asText();

        System.out.println("=== Ollama Reply ===");
        System.out.println(reply);
        System.out.println("===================");
    }
}
