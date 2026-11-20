package in.campusoa.common;

/**
 * Clamp user-supplied paging so nobody can request {@code size=1000000} and overload the DB.
 * Used by endpoints that run hand-written SQL (JPA endpoints use Spring's {@code Pageable}, whose
 * maximum is set by {@code spring.data.web.pageable.max-page-size}).
 */
public record PageParams(int page, int size) {

    public static final int MAX_SIZE = 50;

    public PageParams {
        page = Math.max(page, 0);
        size = Math.min(Math.max(size, 1), MAX_SIZE);
    }

    public int offset() { return page * size; }
}
