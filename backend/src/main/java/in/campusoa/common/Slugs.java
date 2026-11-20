package in.campusoa.common;

import java.text.Normalizer;
import java.util.Locale;

/** "Super.Money (Flipkart)" -> "super-money-flipkart". Used for readable company URLs. */
public final class Slugs {

    private Slugs() {}

    public static String of(String text) {
        String ascii = Normalizer.normalize(text, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        String slug = ascii.toLowerCase(Locale.ROOT)
                .replace("&", " and ")
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-+|-+$)", "");
        if (slug.isEmpty()) throw ApiException.badRequest("BAD_SLUG", "Name must contain letters or digits");
        return slug.length() > 80 ? slug.substring(0, 80).replaceAll("-+$", "") : slug;
    }
}
