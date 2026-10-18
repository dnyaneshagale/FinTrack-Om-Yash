package com.familyfinance.web;

import com.familyfinance.service.LedgerService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final SecurityContextRepository contextRepository;
    private final LedgerService ledger;

    public AuthController(AuthenticationManager authenticationManager, SecurityContextRepository contextRepository, LedgerService ledger) { this.authenticationManager = authenticationManager; this.contextRepository = contextRepository; this.ledger = ledger; }

    @PostMapping("/login")
    public Profile login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        var authentication = authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(normalizeUsername(request.username()), request.password()));
        SecurityContext context = SecurityContextHolder.createEmptyContext(); context.setAuthentication(authentication); SecurityContextHolder.setContext(context); contextRepository.saveContext(context, httpRequest, httpResponse);
        return profile(authentication);
    }

    @GetMapping("/csrf") public CsrfResponse csrf(CsrfToken token) { return new CsrfResponse(token.getToken()); }

    @GetMapping("/me")
    public ResponseEntity<Profile> me(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated() || authentication instanceof org.springframework.security.authentication.AnonymousAuthenticationToken) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        return ResponseEntity.ok(profile(authentication));
    }

    @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT) public void logout() { SecurityContextHolder.clearContext(); }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<java.util.Map<String, String>> handleAuthException(org.springframework.security.core.AuthenticationException ex) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(java.util.Map.of("message", "Invalid username or password. Please try again."));
    }

    private Profile profile(Authentication authentication) { var user = ledger.userByUsername(authentication.getName()); return new Profile(user.id(), user.username(), user.displayName(), user.role().name()); }
    private String normalizeUsername(String username) { return switch (username.trim().toLowerCase()) { case "pappa", "father" -> "father"; case "om", "son1" -> "son1"; case "yash", "son2" -> "son2"; default -> username.trim(); }; }
    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record CsrfResponse(String token) {}
    public record Profile(java.util.UUID id, String username, String displayName, String role) {}
}
