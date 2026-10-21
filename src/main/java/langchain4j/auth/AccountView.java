package langchain4j.auth;

public record AccountView(Long id, String username) {
    public static AccountView from(AccountUser user) {
        return new AccountView(user.getId(), user.getUsername());
    }
}
