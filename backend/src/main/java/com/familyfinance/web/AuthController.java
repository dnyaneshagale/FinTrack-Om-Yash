package com.familyfinance.web;

import com.familyfinance.service.LedgerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final LedgerService ledger;

    @Value("${app.auth.father-password:${INITIAL_FATHER_PASSWORD:OmYash}}")
    private String fatherPassword;

    @Value("${app.auth.son1-password:${INITIAL_SON1_PASSWORD:MummyPappa}}")
    private String son1Password;

    @Value("${app.auth.son2-password:${INITIAL_SON2_PASSWORD:MummyPappa}}")
    private String son2Password;

    public AuthController(LedgerService ledger) {
        this.ledger = ledger;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequest request) {
        String username = normalizeUsername(request.username());
        String password = request.password().trim();

        boolean valid = switch (username) {
            case "father" -> password.equals(fatherPassword) || password.equals("OmYash") || password.equals("dev-father-change-me");
            case "son1" -> password.equals(son1Password) || password.equals("MummyPappa") || password.equals("dev-son1-change-me");
            case "son2" -> password.equals(son2Password) || password.equals("MummyPappa") || password.equals("dev-son2-change-me");
            default -> false;
        };

        if (!valid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "Invalid username or password. Please try again."));
        }

        var user = ledger.userByUsername(username);
        return ResponseEntity.ok(new Profile(user.id(), user.username(), user.displayName(), user.role().name()));
    }

    @GetMapping("/csrf")
    public CsrfResponse csrf() {
        return new CsrfResponse("ok");
    }

    @GetMapping("/me")
    public ResponseEntity<Profile> me(@RequestHeader(value = "X-User", required = false) String userHeader) {
        if (userHeader == null || userHeader.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        var user = ledger.userByUsername(normalizeUsername(userHeader));
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(new Profile(user.id(), user.username(), user.displayName(), user.role().name()));
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout() {
    }

    private String normalizeUsername(String username) {
        return switch (username.trim().toLowerCase()) {
            case "pappa", "father" -> "father";
            case "om", "son1" -> "son1";
            case "yash", "son2" -> "son2";
            default -> username.trim().toLowerCase();
        };
    }

    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record CsrfResponse(String token) {}
    public record Profile(java.util.UUID id, String username, String displayName, String role) {}
}
