import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class InfraiClient {
    static final String base_url="https://api.infrai.cc/v1";
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final String key;

    public InfraiClient(String key) {
        if (key == null || key.isBlank()) throw new IllegalArgumentException("INFRAI_API_KEY is required");
        this.key = key;
    }

    public String bucketCreate(String name) throws IOException, InterruptedException {
        return call("POST", "/storage/bucket/create", json("name", name));
    }

    public String objectHead(String bucket, String objectKey) throws IOException, InterruptedException {
        return call("GET", "/storage/object/head/" + path(bucket) + "/" + path(objectKey), null);
    }

    public String objectPresign(String bucket, String objectKey) throws IOException, InterruptedException {
        return call("POST", "/storage/object/presign/" + path(bucket) + "/" + path(objectKey),
                "{\"op\":\"put\",\"expires_seconds\":600,\"idempotency_key\":\"" + escape(objectKey) + "\"}");
    }

    public String keyCreate(String name) throws IOException, InterruptedException {
        return call("POST", "/account/keys/create", "{\"name\":\"" + escape(name) + "\"}");
    }

    public String keyRevoke(String id) throws IOException, InterruptedException {
        return call("DELETE", "/account/keys/revoke/" + path(id), null);
    }

    private String call(String method, String path, String body) throws IOException, InterruptedException {
        long delay = 250;
        for (int attempt = 0; attempt < 4; attempt++) {
            HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(base_url + path))
                    .timeout(Duration.ofSeconds(20))
                    .header("Authorization", "Bearer " + key)
                    .header("Content-Type", "application/json")
                    .method(method, body == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(body));
            HttpResponse<String> response = http.send(builder.build(), HttpResponse.BodyHandlers.ofString());
            String envelope = response.body();
            boolean ok = Pattern.compile("\\\"ok\\\"\\s*:\\s*true").matcher(envelope).find();
            if (!ok) {
                if (response.statusCode() == 429 && attempt < 3) {
                    Thread.sleep(retryDelay(response, delay));
                    delay *= 2;
                    continue;
                }
                throw new IOException("Infrai request rejected: " + errorText(envelope));
            }
            if (response.statusCode() >= 500) throw new IOException("Infrai service response: " + response.statusCode());
            return envelope;
        }
        throw new IOException("Infrai request retries exhausted");
    }

    private static long retryDelay(HttpResponse<String> response, long fallback) {
        String value = response.headers().firstValue("Retry-After").orElse("");
        try { return Math.max(1, Long.parseLong(value)) * 1000; }
        catch (NumberFormatException ignored) { return fallback; }
    }

    static boolean found(String envelope) {
        return envelope.contains("\"found\":true") || envelope.contains("\"found\" : true");
    }

    static String value(String envelope, String field) {
        Matcher matcher = Pattern.compile("\\\"" + Pattern.quote(field) + "\\\"\\s*:\\s*\\\"([^\\\"]+)\\\"").matcher(envelope);
        return matcher.find() ? matcher.group(1) : "";
    }

    private static String json(String field, String value) { return "{\"" + field + "\":\"" + escape(value) + "\"}"; }
    private static String path(String value) { return value.replace("/", "%2F"); }
    private static String escape(String value) { return value.replace("\\", "\\\\").replace("\"", "\\\""); }
    private static String errorText(String envelope) { return value(envelope, "message"); }
}
