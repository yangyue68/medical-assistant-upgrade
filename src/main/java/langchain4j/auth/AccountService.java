package langchain4j.auth;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Pattern;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountService {
    private static final Pattern USERNAME = Pattern.compile("^[A-Za-z0-9_\\u4e00-\\u9fff]{3,32}$");
    private final AccountUserMapper mapper;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public AccountService(AccountUserMapper mapper) {
        this.mapper = mapper;
    }

    @Transactional
    public AccountUser register(String rawUsername, String rawPassword) {
        String username = normalizeUsername(rawUsername);
        validatePassword(rawPassword);
        if (mapper.selectCount(new LambdaQueryWrapper<AccountUser>().eq(AccountUser::getUsername, username)) > 0) {
            throw new IllegalArgumentException("该用户名已被注册，请更换后重试。");
        }
        LocalDateTime now = LocalDateTime.now();
        AccountUser user = new AccountUser();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setStatus(1);
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        mapper.insert(user);
        return user;
    }

    @Transactional
    public AccountUser authenticate(String rawUsername, String rawPassword) {
        String username = normalizeUsername(rawUsername);
        AccountUser user = mapper.selectOne(new LambdaQueryWrapper<AccountUser>()
                .eq(AccountUser::getUsername, username).last("LIMIT 1"));
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())
                || !passwordEncoder.matches(rawPassword == null ? "" : rawPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("用户名或密码错误。");
        }
        user.setLastLoginAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());
        mapper.updateById(user);
        return user;
    }

    public AccountUser findRequired(Long userId) {
        AccountUser user = userId == null ? null : mapper.selectById(userId);
        if (user == null || !Integer.valueOf(1).equals(user.getStatus())) {
            throw new IllegalArgumentException("登录状态已失效，请重新登录。");
        }
        return user;
    }

    @Transactional
    public void delete(Long userId) {
        findRequired(userId);
        mapper.deleteById(userId);
    }

    private String normalizeUsername(String raw) {
        String value = raw == null ? "" : raw.trim();
        if (!USERNAME.matcher(value).matches()) {
            throw new IllegalArgumentException("用户名需为3至32位中文、字母、数字或下划线。");
        }
        return value.toLowerCase(Locale.ROOT);
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8 || password.length() > 72) {
            throw new IllegalArgumentException("密码长度需为8至72位。");
        }
    }
}
