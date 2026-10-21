package langchain4j.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import langchain4j.common.ApiResponse;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final AccountService accountService;
    private final AccountDeletionService accountDeletionService;

    public AuthController(AccountService accountService, AccountDeletionService accountDeletionService) {
        this.accountService = accountService;
        this.accountDeletionService = accountDeletionService;
    }

    @PostMapping("/register")
    public ApiResponse<AccountView> register(@RequestBody AuthRequest request, HttpServletRequest httpRequest) {
        AccountUser user = accountService.register(request == null ? null : request.getUsername(),
                request == null ? null : request.getPassword());
        HttpSession session = httpRequest.getSession(true);
        httpRequest.changeSessionId();
        AuthSession.login(session, user);
        return ApiResponse.success(AccountView.from(user));
    }

    @PostMapping("/login")
    public ApiResponse<AccountView> login(@RequestBody AuthRequest request, HttpServletRequest httpRequest) {
        AccountUser user = accountService.authenticate(request == null ? null : request.getUsername(),
                request == null ? null : request.getPassword());
        HttpSession session = httpRequest.getSession(true);
        httpRequest.changeSessionId();
        AuthSession.login(session, user);
        return ApiResponse.success(AccountView.from(user));
    }

    @GetMapping("/me")
    public ApiResponse<AccountView> me(HttpSession session) {
        return ApiResponse.success(AccountView.from(accountService.findRequired(AuthSession.userId(session))));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(HttpSession session) {
        AuthSession.logout(session);
        return ApiResponse.success();
    }

    @DeleteMapping("/account")
    public ApiResponse<Void> deleteAccount(HttpSession session) {
        Long userId = AuthSession.userId(session);
        accountDeletionService.deleteAccount(userId);
        AuthSession.logout(session);
        return ApiResponse.success();
    }
}
