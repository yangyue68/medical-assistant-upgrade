package langchain4j.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AccountSchemaInitializer {
    private static final Logger log = LoggerFactory.getLogger(AccountSchemaInitializer.class);
    private final JdbcTemplate jdbcTemplate;

    public AccountSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @jakarta.annotation.PostConstruct
    public void initialize() {
        jdbcTemplate.execute("CREATE TABLE IF NOT EXISTS app_user (" +
                "id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                "username VARCHAR(64) NOT NULL UNIQUE," +
                "password_hash VARCHAR(100) NOT NULL," +
                "status TINYINT NOT NULL DEFAULT 1," +
                "created_at DATETIME NOT NULL," +
                "updated_at DATETIME NOT NULL," +
                "last_login_at DATETIME NULL" +
                ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
        try {
            jdbcTemplate.execute("ALTER TABLE appointment ADD COLUMN user_id BIGINT NULL");
        } catch (Exception ignored) {
            // Existing installations already have the column.
        }
        try {
            jdbcTemplate.execute("CREATE INDEX idx_appointment_user_status ON appointment(user_id, status)");
        } catch (Exception ignored) {
            // Existing installations already have the index.
        }
        log.info("Account schema is ready.");
    }
}
