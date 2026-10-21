package langchain4j.auth;

import jakarta.servlet.http.HttpSession;

public final class AuthSession {
    public static final String USER_ID = "xiaozhi.auth.userId";
    public static final String USERNAME = "xiaozhi.auth.username";

    private AuthSession() {
    }

    public static Long userId(HttpSession session) {
        Object value = session == null ? null : session.getAttribute(USER_ID);
        return value instanceof Number number ? number.longValue() : null;
    }

    public static void login(HttpSession session, AccountUser user) {
        session.setAttribute(USER_ID, user.getId());
        session.setAttribute(USERNAME, user.getUsername());
    }

    public static void logout(HttpSession session) {
        if (session != null) {
            session.invalidate();
        }
    }
}
