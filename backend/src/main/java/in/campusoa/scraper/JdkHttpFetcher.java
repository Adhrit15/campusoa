package in.campusoa.scraper;

import in.campusoa.config.AppProperties;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

/** Real implementation using Java's built-in HttpClient: no extra dependency. */
@Component
public class JdkHttpFetcher implements HttpFetcher {

    private final HttpClient client;
    private final AppProperties.Scraper cfg;

    public JdkHttpFetcher(AppProperties props) {
        this.cfg = props.scraper();
        this.client = HttpClient.newBuilder()
                .connectTimeout(cfg.timeout())
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    @Override
    public Response get(String url, Map<String, String> headers) throws IOException, InterruptedException {
        HttpRequest.Builder req = HttpRequest.newBuilder(URI.create(url))
                .timeout(cfg.timeout())
                .header("User-Agent", cfg.userAgent())   // identify ourselves honestly
                .header("Accept", "text/html,text/plain;q=0.9");
        headers.forEach(req::header);
        HttpResponse<String> res = client.send(req.GET().build(), HttpResponse.BodyHandlers.ofString());
        return new Response(res.statusCode(), res.body(),
                res.headers().firstValue("ETag").orElse(null),
                res.headers().firstValue("Last-Modified").orElse(null));
    }
}
