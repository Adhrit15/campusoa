package in.campusoa.scraper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * A small robots.txt parser (the Robots Exclusion Protocol, RFC 9309).
 * <ul>
 *   <li>Rules are grouped under one or more {@code User-agent:} lines.</li>
 *   <li>We use the group naming our bot if there is one, otherwise the {@code *} group.</li>
 *   <li>Within the group, the LONGEST matching rule wins. On a tie, {@code Allow} wins.</li>
 *   <li>{@code *} in a path matches anything; a trailing {@code $} anchors the end.</li>
 * </ul>
 */
public final class RobotsTxt {

    private record Rule(boolean allow, String path, Pattern regex) {}

    private record Group(List<String> agents, List<Rule> rules) {}

    private final List<Group> groups;

    private RobotsTxt(List<Group> groups) {
        this.groups = groups;
    }

    /** Missing robots.txt (404) means "everything allowed". */
    public static RobotsTxt allowAll() {
        return new RobotsTxt(List.of());
    }

    public static RobotsTxt parse(String text) {
        List<Group> groups = new ArrayList<>();
        Group current = null;
        boolean lastWasAgent = false;
        for (String raw : text.split("\\R")) {
            String line = raw.replaceAll("#.*$", "").trim();
            int colon = line.indexOf(':');
            if (colon < 0) continue;
            String key = line.substring(0, colon).trim().toLowerCase(Locale.ROOT);
            String value = line.substring(colon + 1).trim();
            switch (key) {
                case "user-agent" -> {
                    if (current == null || !lastWasAgent) {
                        current = new Group(new ArrayList<>(), new ArrayList<>());
                        groups.add(current);
                    }
                    current.agents().add(value.toLowerCase(Locale.ROOT));
                    lastWasAgent = true;
                }
                case "allow", "disallow" -> {
                    lastWasAgent = false;
                    if (current == null || value.isEmpty()) continue;   // "Disallow:" (empty) = allow all
                    current.rules().add(new Rule(key.equals("allow"), value, toRegex(value)));
                }
                default -> lastWasAgent = false;
            }
        }
        return new RobotsTxt(groups);
    }

    public boolean isAllowed(String userAgentToken, String path) {
        Group group = pick(userAgentToken.toLowerCase(Locale.ROOT));
        if (group == null) return true;
        Rule best = null;
        for (Rule r : group.rules()) {
            if (!r.regex().matcher(path).lookingAt()) continue;
            if (best == null || r.path().length() > best.path().length()
                    || (r.path().length() == best.path().length() && r.allow())) {
                best = r;
            }
        }
        return best == null || best.allow();
    }

    private Group pick(String token) {
        Group star = null;
        for (Group g : groups) {
            for (String agent : g.agents()) {
                if (!agent.equals("*") && token.contains(agent)) return g;
                if (agent.equals("*") && star == null) star = g;
            }
        }
        return star;
    }

    private static Pattern toRegex(String path) {
        boolean anchored = path.endsWith("$");
        String p = anchored ? path.substring(0, path.length() - 1) : path;
        StringBuilder re = new StringBuilder();
        for (String part : p.split("\\*", -1)) {
            if (!re.isEmpty()) re.append(".*");
            re.append(Pattern.quote(part));
        }
        if (anchored) re.append("$");
        return Pattern.compile(re.toString());
    }
}
