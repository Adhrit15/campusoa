package in.campusoa.common;

/**
 * Lifecycle of user-generated content.
 * PUBLISHED: visible to everyone. HIDDEN: auto-hidden after reports or hidden by a moderator,
 * pending review (author and moderators still see it). REMOVED: taken down; only moderators see it.
 */
public enum ContentStatus {
    PUBLISHED, HIDDEN, REMOVED
}
