package in.campusoa.common;

import in.campusoa.auth.AppUserPrincipal;

/**
 * "Who is looking?", reduced to what read rules need. Signed-out visitors are {@link #ANONYMOUS}.
 * Passing this into services (instead of the whole security principal) keeps services easy to
 * unit test and independent of Spring Security.
 */
public record Viewer(Long userId, boolean moderator) {

    public static final Viewer ANONYMOUS = new Viewer(null, false);

    public static Viewer of(AppUserPrincipal p) {
        return p == null ? ANONYMOUS : new Viewer(p.userId(), p.isModerator());
    }

    public boolean signedIn() { return userId != null; }

    public boolean is(Long otherUserId) {
        return userId != null && userId.equals(otherUserId);
    }

    /** Authors and moderators may see content that isn't public (hidden / anonymous author). */
    public boolean canSeePrivateDetailsOf(Long authorId) {
        return moderator || is(authorId);
    }
}
