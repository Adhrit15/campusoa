package in.campusoa.moderation;

/** What can be reported, and which table/column holds it (constants, so safe to put in SQL). */
public enum ContentTarget {
    EXPERIENCE("interview_experience", "experience_id"),
    QUESTION("question", "question_id"),
    COMMENT("comment", "comment_id");

    final String table;
    final String reportColumn;

    ContentTarget(String table, String reportColumn) {
        this.table = table;
        this.reportColumn = reportColumn;
    }
}
