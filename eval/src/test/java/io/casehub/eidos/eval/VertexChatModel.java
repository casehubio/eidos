package io.casehub.eidos.eval;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.langchain4j.data.message.AiMessage;
import dev.langchain4j.data.message.ChatMessage;
import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.chat.request.ChatRequest;
import dev.langchain4j.model.chat.response.ChatResponse;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.logging.Logger;

/**
 * Calls Claude via the Vertex AI rawPredict endpoint.
 * Auth via {@code gcloud auth print-access-token}; project/region from env vars.
 */
final class VertexChatModel implements ChatModel {

    private static final Logger LOG = Logger.getLogger(VertexChatModel.class.getName());

    private final String endpoint;
    private final HttpClient httpClient;
    private final ObjectMapper mapper;
    private final Duration timeout;

    VertexChatModel(final String projectId, final String region, final String modelId,
                    final Duration timeout) {
        this.endpoint = String.format(
                "https://%s-aiplatform.googleapis.com/v1/projects/%s/locations/%s/publishers/anthropic/models/%s:rawPredict",
                region, projectId, region, modelId);
        this.httpClient = HttpClient.newBuilder().connectTimeout(timeout).build();
        this.mapper = new ObjectMapper();
        this.timeout = timeout;
        LOG.info("VertexChatModel: endpoint=" + endpoint + ", timeout=" + timeout);
    }

    @Override
    public ChatResponse doChat(final ChatRequest request) {
        try {
            final String token = fetchAccessToken();
            final String body = buildRequestBody(request);

            LOG.fine(() -> "Vertex request body: " + body);

            final HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Authorization", "Bearer " + token)
                    .header("Content-Type", "application/json")
                    .timeout(timeout)
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();

            final long start = System.currentTimeMillis();
            final HttpResponse<String> response = httpClient.send(httpRequest,
                    HttpResponse.BodyHandlers.ofString());
            final long elapsed = System.currentTimeMillis() - start;

            if (response.statusCode() != 200) {
                throw new RuntimeException("Vertex API returned " + response.statusCode()
                                           + ": " + response.body());
            }

            final JsonNode root = mapper.readTree(response.body());
            final String text = extractText(root);
            LOG.info(() -> String.format("Vertex call completed in %dms (%d chars response)", elapsed, text.length()));
            return ChatResponse.builder().aiMessage(AiMessage.from(text)).build();

        } catch (final IOException | InterruptedException e) {
            throw new RuntimeException("Vertex API call failed: " + e.getMessage(), e);
        }
    }

    private String buildRequestBody(final ChatRequest request) throws IOException {
        final ObjectNode root = mapper.createObjectNode();
        root.put("anthropic_version", "vertex-2023-10-16");
        root.put("max_tokens", 4096);

        final ArrayNode messages = root.putArray("messages");
        for (final ChatMessage msg : request.messages()) {
            if (msg instanceof SystemMessage sm) {
                root.put("system", sm.text());
            } else if (msg instanceof UserMessage um) {
                final ObjectNode m = messages.addObject();
                m.put("role", "user");
                m.put("content", um.singleText());
            }
        }

        return mapper.writeValueAsString(root);
    }

    private String extractText(final JsonNode root) {
        if (root.has("content") && root.get("content").isArray()) {
            final StringBuilder sb = new StringBuilder();
            for (final JsonNode block : root.get("content")) {
                if ("text".equals(block.path("type").asText())) {
                    sb.append(block.get("text").asText());
                }
            }
            return sb.toString();
        }
        throw new RuntimeException("Unexpected Vertex response format: " + root);
    }

    private String fetchAccessToken() throws IOException {
        try {
            final Process proc = new ProcessBuilder("gcloud", "auth", "print-access-token")
                    .redirectErrorStream(true)
                    .start();
            final String token = new String(proc.getInputStream().readAllBytes()).trim();
            final int exit = proc.waitFor();
            if (exit != 0 || token.isEmpty()) {
                throw new RuntimeException("gcloud auth print-access-token failed (exit=" + exit + "): " + token);
            }
            return token;
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted fetching access token", e);
        }
    }
}
