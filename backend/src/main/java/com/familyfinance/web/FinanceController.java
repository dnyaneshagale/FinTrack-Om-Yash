package com.familyfinance.web;

import com.familyfinance.domain.Ledger;
import com.familyfinance.service.LedgerService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/v1")
public class FinanceController {
    private final LedgerService ledger;
    public FinanceController(LedgerService ledger) { this.ledger = ledger; }

    @GetMapping("/accounts")
    public List<AccountView> accounts(@RequestHeader(value = "X-User", required = false) String userHeader) {
        var actor = actor(userHeader);
        return ledger.accounts().stream()
            .filter(account -> actor.role() == Ledger.Role.ADMIN || account.ownerId().equals(actor.id()))
            .map(a -> new AccountView(a.id(), a.ownerName(), ledger.balance(a, LocalDate.now(LedgerService.ZONE)), a.openingBalancePaise()))
            .toList();
    }

    @GetMapping("/accounts/{accountId}/transactions")
    public List<TransactionView> transactions(@PathVariable UUID accountId, @RequestHeader(value = "X-User", required = false) String userHeader) {
        var actor = actor(userHeader);
        checkAccountAccess(accountId, actor, false);
        return ledger.accountTransactions(accountId).stream().map(this::view).toList();
    }

    @PostMapping("/accounts/{accountId}/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    public TransactionView addTransaction(@PathVariable UUID accountId, @Valid @RequestBody TransactionRequest request, @RequestHeader(value = "X-User", required = false) String userHeader) {
        var actor = actor(userHeader);
        if (actor.role() != Ledger.Role.ADMIN) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Members cannot create transactions");
        checkAccountAccess(accountId, actor, true);
        if (request.type() != Ledger.Type.CREDIT) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrators may only record credits");
        return view(ledger.addTransaction(accountId, request.type(), request.amountPaise(), request.date(), request.description(), request.notes(), actor.id(), request.needId(), request.idempotencyKey()));
    }

    @PostMapping("/accounts/{accountId}/adjustments")
    @ResponseStatus(HttpStatus.CREATED)
    public AccountView adjustBalance(@PathVariable UUID accountId, @Valid @RequestBody AdjustmentRequest request, @RequestHeader(value = "X-User", required = false) String userHeader) {
        var actor = actor(userHeader);
        if (actor.role() != Ledger.Role.MEMBER) throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only members may submit personal balance adjustments");
        checkAccountAccess(accountId, actor, true);
        if (request.deltaPaise() == 0) throw new IllegalArgumentException("Adjustment cannot be zero");
        var type = request.deltaPaise() > 0 ? Ledger.Type.CREDIT : Ledger.Type.DEBIT;
        ledger.addTransaction(accountId, type, Math.abs(request.deltaPaise()), LocalDate.now(LedgerService.ZONE), "Balance adjustment", request.reason(), actor.id(), null, request.idempotencyKey());
        var account = ledger.accounts().stream().filter(item -> item.id().equals(accountId)).findFirst().orElseThrow();
        return new AccountView(account.id(), account.ownerName(), ledger.balance(account, LocalDate.now(LedgerService.ZONE)), account.openingBalancePaise());
    }

    @GetMapping("/needs")
    public Collection<NeedView> needs(@RequestHeader(value = "X-User", required = false) String userHeader) {
        var actor = actor(userHeader);
        return ledger.needs().stream()
            .filter(n -> actor.role() == Ledger.Role.ADMIN || n.requestedBy().equals(actor.id()))
            .map(this::needView)
            .toList();
    }

    @PostMapping("/needs")
    @ResponseStatus(HttpStatus.CREATED)
    public NeedView createNeed(@Valid @RequestBody NeedRequest request, @RequestHeader(value = "X-User", required = false) String userHeader) {
        return needView(ledger.createNeed(actor(userHeader).id(), request.cause(), request.amountPaise(), request.requiredBy(), request.notes()));
    }

    @PatchMapping("/needs/{id}/status")
    public NeedView updateStatus(@PathVariable UUID id, @RequestBody StatusRequest request, @RequestHeader(value = "X-User", required = false) String userHeader) {
        requireAdmin(userHeader);
        return needView(ledger.updateStatus(id, request.status()));
    }

    @PostMapping("/needs/{id}/fundings")
    public NeedView fund(@PathVariable UUID id, @Valid @RequestBody FundRequest request, @RequestHeader(value = "X-User", required = false) String userHeader) {
        requireAdmin(userHeader);
        return needView(ledger.fundNeed(id, request.amountPaise(), actor(userHeader).id()));
    }

    @GetMapping("/needs/{id}/fundings")
    public List<FundingView> fundings(@PathVariable UUID id, @RequestHeader(value = "X-User", required = false) String userHeader) {
        var need = ledger.needs().stream().filter(item -> item.id().equals(id)).findFirst().orElseThrow();
        if (actor(userHeader).role() != Ledger.Role.ADMIN && !need.requestedBy().equals(actor(userHeader).id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Need access denied");
        }
        return ledger.fundings(id).stream().map(funding -> new FundingView(funding.id(), funding.needId(), funding.transactionId(), funding.amountPaise(), funding.fundedAt(), funding.notes())).toList();
    }

    @GetMapping("/reports/overview")
    public OverviewReport overview(@RequestHeader(value = "X-User", required = false) String userHeader) {
        requireAdmin(userHeader);
        var cutoff = LocalDate.now(LedgerService.ZONE);
        var credits = ledger.transactions().stream().filter(tx -> tx.type() == Ledger.Type.CREDIT && !tx.date().isAfter(cutoff)).mapToLong(Ledger.Transaction::amountPaise).sum();
        var debits = ledger.transactions().stream().filter(tx -> tx.type() == Ledger.Type.DEBIT && !tx.date().isAfter(cutoff)).mapToLong(Ledger.Transaction::amountPaise).sum();
        return new OverviewReport(ledger.accounts().stream().mapToLong(account -> ledger.balance(account, cutoff)).sum(), credits, debits, ledger.needs().stream().filter(need -> need.status() != Ledger.NeedStatus.FUNDED && need.status() != Ledger.NeedStatus.REJECTED).mapToLong(Ledger.Need::remainingPaise).sum());
    }

    @PostMapping("/transfers")
    @ResponseStatus(HttpStatus.CREATED)
    public List<TransactionView> transfer(@Valid @RequestBody TransferRequest request, @RequestHeader(value = "X-User", required = false) String userHeader) {
        requireAdmin(userHeader);
        var actor = actor(userHeader);
        checkAccountAccess(request.senderAccountId(), actor, true);
        return ledger.transfer(request.senderAccountId(), request.recipientAccountId(), request.amountPaise(), actor.id(), request.date(), request.notes(), request.operationKey()).stream().map(this::view).toList();
    }

    @GetMapping("/reports/monthly")
    public MonthlyReport monthly(@RequestParam int year, @RequestParam int month, @RequestParam(required = false) UUID accountId, @RequestHeader(value = "X-User", required = false) String userHeader) {
        requireAdmin(userHeader);
        var start = LocalDate.of(year, month, 1);
        var end = start.withDayOfMonth(start.lengthOfMonth());
        var accounts = ledger.accounts().stream().filter(account -> accountId == null || account.id().equals(accountId)).toList();
        var opening = accounts.stream().mapToLong(account -> ledger.balance(account, start.minusDays(1))).sum();
        var credits = ledger.transactions().stream().filter(tx -> accounts.stream().anyMatch(account -> account.id().equals(tx.accountId())) && tx.type() == Ledger.Type.CREDIT && !tx.date().isBefore(start) && !tx.date().isAfter(end)).mapToLong(Ledger.Transaction::amountPaise).sum();
        var debits = ledger.transactions().stream().filter(tx -> accounts.stream().anyMatch(account -> account.id().equals(tx.accountId())) && tx.type() == Ledger.Type.DEBIT && !tx.date().isBefore(start) && !tx.date().isAfter(end)).mapToLong(Ledger.Transaction::amountPaise).sum();
        return new MonthlyReport(year, month, opening, credits, debits, opening + credits - debits);
    }

    @GetMapping("/reports/yearly")
    public List<MonthlyReport> yearly(@RequestParam int year, @RequestHeader(value = "X-User", required = false) String userHeader) {
        requireAdmin(userHeader);
        return java.util.stream.IntStream.rangeClosed(1, 12).mapToObj(month -> monthly(year, month, null, userHeader)).toList();
    }

    private Ledger.User actor(String userHeader) {
        String username = normalizeUsername(userHeader);
        var user = ledger.userByUsername(username);
        return user != null ? user : ledger.userByUsername("father");
    }

    private String normalizeUsername(String username) {
        if (username == null || username.isBlank()) return "father";
        return switch (username.trim().toLowerCase()) {
            case "pappa", "father" -> "father";
            case "om", "son1" -> "son1";
            case "yash", "son2" -> "son2";
            default -> username.trim().toLowerCase();
        };
    }

    private void requireAdmin(String userHeader) {
        if (actor(userHeader).role() != Ledger.Role.ADMIN) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Administrator access required");
        }
    }

    private void checkAccountAccess(UUID accountId, Ledger.User actor, boolean write) {
        var account = ledger.accounts().stream().filter(a -> a.id().equals(accountId)).findFirst().orElseThrow();
        if (actor.role() == Ledger.Role.MEMBER && !account.ownerId().equals(actor.id())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Account access denied");
        }
    }

    private TransactionView view(Ledger.Transaction t) { var creator = ledger.user(t.createdBy()); return new TransactionView(t.id(), t.accountId(), t.type().name(), t.amountPaise(), t.date(), t.description(), t.notes(), t.needId(), creator == null ? "Unknown member" : creator.displayName()); }
    private NeedView needView(Ledger.Need n) { return new NeedView(n.id(), n.requesterName(), n.cause(), n.amountPaise(), n.fundedPaise(), n.remainingPaise(), n.requiredBy(), n.status().name(), n.notes()); }
    public record AccountView(UUID id, String ownerName, long balancePaise, long openingBalancePaise) {}
    public record TransactionView(UUID id, UUID accountId, String type, long amountPaise, LocalDate date, String description, String notes, UUID needId, String createdByName) {}
    public record NeedView(UUID id, String requesterName, String cause, long amountPaise, long fundedPaise, long remainingPaise, LocalDate requiredBy, String status, String notes) {}
    public record TransactionRequest(Ledger.Type type, @Positive long amountPaise, LocalDate date, @NotBlank String description, String notes, UUID actorId, UUID needId, @NotBlank String idempotencyKey) {}
    public record AdjustmentRequest(long deltaPaise, @NotBlank String reason, @NotBlank String idempotencyKey) {}
    public record NeedRequest(UUID requestedBy, @NotBlank String cause, @Positive long amountPaise, LocalDate requiredBy, String notes) {}
    public record FundRequest(@Positive long amountPaise, UUID actorId) {}
    public record FundingView(UUID id, UUID needId, UUID transactionId, long amountPaise, java.time.OffsetDateTime fundedAt, String notes) {}
    public record OverviewReport(long combinedBalancePaise, long creditsPaise, long debitsPaise, long outstandingNeedsPaise) {}
    public record TransferRequest(UUID senderAccountId, UUID recipientAccountId, @Positive long amountPaise, LocalDate date, String notes, @NotBlank String operationKey) {}
    public record MonthlyReport(int year, int month, long openingBalancePaise, long creditsPaise, long debitsPaise, long closingBalancePaise) {}
    public record StatusRequest(Ledger.NeedStatus status) {}
}
