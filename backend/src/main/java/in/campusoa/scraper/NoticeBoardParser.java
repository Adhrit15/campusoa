package in.campusoa.scraper;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Turns the notice-board HTML into a list of circulars (title + PDF link).
 * <p>
 * The page (as of Nov 2026) is a WordPress page with a heading "Placement Circulars" followed by a
 * {@code <ul>} of links like
 * {@code <a href=".../Infosys-Interviews-Placement-Circular-on-11.11.2026.pdf">Infosys Interviews Placement Circular on 11.11.2026</a>}.
 * <p>
 * Strategy: start at that heading and collect PDF links until the next heading. If the layout
 * changes and the heading isn't found, fall back to any PDF link whose text mentions "Circular".
 * Only the LISTING is read, never the PDFs, so no student names or roll numbers are ever ingested.
 */
public final class NoticeBoardParser {

    private NoticeBoardParser() {}

    public record Notice(String title, String pdfUrl) {}

    public static List<Notice> parse(String html, String baseUrl) {
        Document doc = Jsoup.parse(html, baseUrl);
        Map<String, Notice> byUrl = new LinkedHashMap<>();   // dedupe, keep page order

        Element heading = doc.select("h1, h2, h3, h4").stream()
                .filter(h -> h.text().trim().matches("(?i)placement circulars"))
                .findFirst().orElse(null);

        if (heading != null) {
            for (Element sib = heading.nextElementSibling(); sib != null; sib = sib.nextElementSibling()) {
                if (sib.tagName().matches("h[1-4]")) break;
                for (Element a : sib.select("a[href]")) add(byUrl, a);
            }
        }
        if (byUrl.isEmpty()) {
            for (Element a : doc.select("a[href]")) {
                if (a.text().toLowerCase().contains("circular")) add(byUrl, a);
            }
        }
        return new ArrayList<>(byUrl.values());
    }

    private static void add(Map<String, Notice> out, Element a) {
        String url = a.absUrl("href");
        String title = a.text().replaceAll("\\s+", " ").trim();
        if (!url.toLowerCase().endsWith(".pdf") || title.isEmpty()) return;
        if (!url.startsWith("https://") && !url.startsWith("http://")) return;
        out.putIfAbsent(url, new Notice(title, url));
    }
}
