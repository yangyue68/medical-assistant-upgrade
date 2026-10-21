package langchain4j.auth;

import java.util.function.Supplier;

/** Request-scoped account context used by asynchronous agent tool execution. */
public final class AccountContext {
    private static final ThreadLocal<Long> CURRENT = new ThreadLocal<>();
    private static final ThreadLocal<String> CURRENT_MESSAGE = new ThreadLocal<>();

    private AccountContext() {
    }

    public static Long currentUserId() {
        return CURRENT.get();
    }

    public static void set(Long userId) {
        if (userId == null) CURRENT.remove(); else CURRENT.set(userId);
    }

    public static void clear() {
        CURRENT.remove();
        CURRENT_MESSAGE.remove();
    }

    /** The raw user message being handled by the current request. */
    public static String currentUserMessage() {
        return CURRENT_MESSAGE.get();
    }

    public static void setRequestMessage(String message) {
        if (message == null) CURRENT_MESSAGE.remove(); else CURRENT_MESSAGE.set(message);
    }

    public static void clearRequestMessage() {
        CURRENT_MESSAGE.remove();
    }

    public static <T> T withUser(Long userId, Supplier<T> action) {
        Long previous = CURRENT.get();
        if (userId == null) {
            CURRENT.remove();
        } else {
            CURRENT.set(userId);
        }
        try {
            return action.get();
        } finally {
            if (previous == null) CURRENT.remove(); else CURRENT.set(previous);
        }
    }

    /** Runs an agent request with both the authenticated user and the current message scoped to this thread. */
    public static <T> T withRequest(Long userId, String message, Supplier<T> action) {
        Long previousUser = CURRENT.get();
        String previousMessage = CURRENT_MESSAGE.get();
        set(userId);
        setRequestMessage(message);
        try {
            return action.get();
        } finally {
            if (previousUser == null) CURRENT.remove(); else CURRENT.set(previousUser);
            if (previousMessage == null) CURRENT_MESSAGE.remove(); else CURRENT_MESSAGE.set(previousMessage);
        }
    }
}
