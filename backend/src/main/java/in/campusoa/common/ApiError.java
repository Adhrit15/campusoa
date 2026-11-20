package in.campusoa.common;

import java.time.Instant;
import java.util.List;

/**
 * The single JSON shape for every error the API returns, so the frontend needs one error parser.
 * <pre>
 * { "status": 400, "error": "Bad Request", "code": "VALIDATION_FAILED",
 *   "message": "Request has invalid fields", "fieldErrors": [{"field": "year", "message": "..."}],
 *   "path": "/api/experiences", "timestamp": "2026-11-20T10:00:00Z" }
 * </pre>
 */
public record ApiError(
        int status,
        String error,
        String code,
        String message,
        List<FieldError> fieldErrors,
        String path,
        Instant timestamp) {

    public record FieldError(String field, String message) {}
}
