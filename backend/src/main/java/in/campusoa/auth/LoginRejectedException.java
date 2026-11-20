package in.campusoa.auth;

/** Thrown by {@code UserService.registerLogin} when a Google account may not sign in. */
public class LoginRejectedException extends RuntimeException {

    /** Short code passed to the frontend as /login?error=CODE. */
    private final String code;

    public LoginRejectedException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String code() { return code; }
}
