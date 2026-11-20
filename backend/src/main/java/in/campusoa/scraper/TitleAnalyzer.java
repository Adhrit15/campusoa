package in.campusoa.scraper;

import in.campusoa.drive.StageType;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Best-effort extraction from a circular title such as
 * "Metrics4 Analytics Pre-Placement Talk, Online Assessment & Interviews Placement Circular on 18.11.2026".
 * Each method is pure and unit-tested against real-looking titles.
 */
public final class TitleAnalyzer {

    private TitleAnalyzer() {}

    /** "on 12 & 13.11.2026" / "on 06, 07 & 08.01.2026" / "on 18.11.2026." -> first day mentioned. */
    private static final Pattern DATE_AFTER_ON = Pattern.compile(
            "\\bon\\s+(\\d{1,2})(?:\\s*(?:,|&|and|-|to)\\s*\\d{1,2})*\\s*[./-]\\s*(\\d{1,2})\\s*[./-]\\s*(\\d{4})",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern ANY_DATE = Pattern.compile("\\b(\\d{1,2})[./-](\\d{1,2})[./-](\\d{4})\\b");
    /** "01 & 03 October-2026", "27 & 28 July. 2026", "20th-23rd April 2026" -> first day. */
    private static final Pattern MONTH_NAME_DATE = Pattern.compile(
            "\\b(\\d{1,2})(?:st|nd|rd|th)?(?:\\s*(?:,|&|and|-|to)\\s*\\d{1,2}(?:st|nd|rd|th)?)*\\s+"
                    + "(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*[\\s.,-]*(\\d{4})",
            Pattern.CASE_INSENSITIVE);
    private static final java.util.List<String> MONTHS =
            java.util.List.of("jan", "feb", "mar", "apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec");

    public static Optional<LocalDate> date(String title) {
        Matcher named = MONTH_NAME_DATE.matcher(title);
        if (named.find()) {
            try {
                int month = MONTHS.indexOf(named.group(2).toLowerCase(java.util.Locale.ROOT)) + 1;
                return Optional.of(LocalDate.of(Integer.parseInt(named.group(3)), month,
                        Integer.parseInt(named.group(1))));
            } catch (DateTimeException ignored) {
                // fall through to numeric formats
            }
        }
        for (Pattern p : new Pattern[]{DATE_AFTER_ON, ANY_DATE}) {
            Matcher m = p.matcher(title);
            if (m.find()) {
                try {
                    return Optional.of(LocalDate.of(Integer.parseInt(m.group(3)), Integer.parseInt(m.group(2)),
                            Integer.parseInt(m.group(1))));
                } catch (DateTimeException ignored) {
                    // e.g. 31.02.2026: try the next pattern
                }
            }
        }
        return Optional.empty();
    }

    private static final Map<StageType, Pattern> STAGE_PATTERNS = Map.of(
            StageType.PPT, Pattern.compile("pre[- ]?placement\\s+talk|\\bPPT\\b", Pattern.CASE_INSENSITIVE),
            StageType.OA, Pattern.compile("online\\s+(assessment|exam|test)|\\bassessment\\b|\\btest\\b|\\bcoding\\b|\\baptitude\\b",
                    Pattern.CASE_INSENSITIVE),
            StageType.GD, Pattern.compile("group\\s+discussion|\\bGD\\b", Pattern.CASE_INSENSITIVE),
            StageType.INTERVIEW, Pattern.compile("interview", Pattern.CASE_INSENSITIVE),
            StageType.HR, Pattern.compile("\\bHR\\b(?!\\s*Exam)", Pattern.CASE_INSENSITIVE));

    public static Set<StageType> stages(String title) {
        Set<StageType> found = EnumSet.noneOf(StageType.class);
        STAGE_PATTERNS.forEach((type, p) -> {
            if (p.matcher(title).find()) found.add(type);
        });
        return found;
    }

    /** Words that start the "what is happening" part of a title; the company name comes before them. */
    private static final Pattern EVENT_WORDS = Pattern.compile(
            "\\b(pre[- ]?placement|ppt|online|offline|technical|interviews?|in[- ]person|virtual|full\\s+drive|"
                    + "placement\\s+(drive|circular)|hackathon|launch|recruitment|drive|assessment|test|exam|gd|"
                    + "group\\s+discussion|hr|registration|webinar|session|aptitude|coding|pool|campus)\\b",
            Pattern.CASE_INSENSITIVE);

    /** "Skyroot Aerospace In-Person Interviews ..." -> "Skyroot Aerospace". */
    public static String guessCompany(String title) {
        Matcher m = EVENT_WORDS.matcher(title);
        String head = m.find() ? title.substring(0, m.start()) : title;
        head = head.replaceAll("[\\s\\-–,(:]+$", "").trim();
        return head.isEmpty() ? null : head;
    }

    /**
     * Find a known company in the title, trying names/aliases longest first so "Electronic Arts"
     * wins over "EA". Word boundaries stop "EA" from matching inside "Rearrange".
     */
    public static Optional<Long> matchCompany(String title, Map<String, Long> namesLongestFirst) {
        for (var e : namesLongestFirst.entrySet()) {
            Pattern p = Pattern.compile("(?<![A-Za-z0-9])" + Pattern.quote(e.getKey()) + "(?![A-Za-z0-9])",
                    Pattern.CASE_INSENSITIVE);
            if (p.matcher(title).find()) return Optional.of(e.getValue());
        }
        return Optional.empty();
    }
}
