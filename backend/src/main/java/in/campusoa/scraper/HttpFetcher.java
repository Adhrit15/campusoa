package in.campusoa.scraper;

import java.io.IOException;
import java.util.Map;

/**
 * The scraper's only door to the network. An interface, so tests plug in a fake and CI never
 * hits the real website (fast, deterministic, and polite).
 */
public interface HttpFetcher {

    record Response(int status, String body, String etag, String lastModified) {}

    Response get(String url, Map<String, String> headers) throws IOException, InterruptedException;
}
