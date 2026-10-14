package com.familyfinance.service;

import com.familyfinance.domain.Ledger;
import com.familyfinance.domain.LedgerRepository;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.beans.factory.annotation.Value;

@Service
public class LedgerService {
    public static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");
    private final Map<UUID, Ledger.User> users = new LinkedHashMap<>();
    private final Map<UUID, Ledger.Account> accounts = new LinkedHashMap<>();
    private final List<Ledger.Transaction> transactions = new ArrayList<>();
    private final Map<UUID, Ledger.Need> needs = new LinkedHashMap<>();
    private final Map<UUID, List<Ledger.Funding>> fundings = new LinkedHashMap<>();
    private final Map<String, Ledger.Transaction> idempotentTransactions = new ConcurrentHashMap<>();
    private final ReentrantLock writeLock = new ReentrantLock();
    private GoogleSheetsTransactionWriter sheetsWriter;
    private GoogleSheetsFundingWriter fundingWriter;
    private LedgerRepository repository;

    @Autowired(required = false)
    public void setSheetsWriter(GoogleSheetsTransactionWriter sheetsWriter) { this.sheetsWriter = sheetsWriter; }

    @Autowired(required = false)
    public void setFundingWriter(GoogleSheetsFundingWriter fundingWriter) { this.fundingWriter = fundingWriter; }

    @Autowired(required = false)
    public void setRepository(LedgerRepository repository) { this.repository = repository; }

    @EventListener(ApplicationReadyEvent.class)
    @Order(1)
    public void hydrateFromRepository() {
        if (repository == null) return;
        var persistedUsers = repository.users();
        var persistedAccounts = repository.accounts();
        if (!persistedUsers.isEmpty()) { users.clear(); persistedUsers.forEach(user -> users.put(user.id(), user)); }
        if (!persistedAccounts.isEmpty()) { accounts.clear(); persistedAccounts.forEach(account -> accounts.put(account.id(), account)); }
        if (!persistedAccounts.isEmpty()) { transactions.clear(); persistedAccounts.forEach(account -> transactions.addAll(repository.transactions(account.id()))); }
    }

    public LedgerService() { this("Pappa", "Om", "Yash"); }

    @Autowired
    public LedgerService(@Value("${app.family.father-name:Pappa}") String fatherName, @Value("${app.family.son1-name:Om}") String son1Name, @Value("${app.family.son2-name:Yash}") String son2Name) {
        var father = new Ledger.User(UUID.fromString("00000000-0000-0000-0000-000000000001"), "father", fatherName, Ledger.Role.ADMIN);
        var son1 = new Ledger.User(UUID.fromString("00000000-0000-0000-0000-000000000002"), "son1", son1Name, Ledger.Role.MEMBER);
        var son2 = new Ledger.User(UUID.fromString("00000000-0000-0000-0000-000000000003"), "son2", son2Name, Ledger.Role.MEMBER);
        users.put(father.id(), father); users.put(son1.id(), son1); users.put(son2.id(), son2);
        accounts.put(UUID.fromString("10000000-0000-0000-0000-000000000001"), new Ledger.Account(UUID.fromString("10000000-0000-0000-0000-000000000001"), son1.id(), son1.displayName(), 2500000, LocalDate.of(2026, 1, 1)));
        accounts.put(UUID.fromString("10000000-0000-0000-0000-000000000002"), new Ledger.Account(UUID.fromString("10000000-0000-0000-0000-000000000002"), son2.id(), son2.displayName(), 1800000, LocalDate.of(2026, 1, 1)));
        addSeedTransaction(accounts.keySet().stream().findFirst().orElseThrow(), Ledger.Type.DEBIT, 18500, LocalDate.now(ZONE).minusDays(1), "College lunch", "Lunch and snacks.", son1.id(), null);
        addSeedTransaction(accounts.keySet().stream().skip(1).findFirst().orElseThrow(), Ledger.Type.CREDIT, 500000, LocalDate.now(ZONE).minusDays(3), "Monthly allowance", "Allowance received from Father.", father.id(), null);
    }

    public Collection<Ledger.User> users() { return users.values(); }
    public Collection<Ledger.Account> accounts() { return accounts.values(); }
    public List<Ledger.Transaction> transactions() { return List.copyOf(transactions); }
    public Collection<Ledger.Need> needs() { return needs.values(); }
    public List<Ledger.Funding> fundings(UUID needId) { return List.copyOf(fundings.getOrDefault(needId, List.of())); }
    public Ledger.User userByUsername(String username) { return users.values().stream().filter(u -> u.username().equalsIgnoreCase(username)).findFirst().orElse(null); }
    public Ledger.User user(UUID id) { return users.get(id); }

    public long balance(Ledger.Account account, LocalDate cutoff) {
        return account.openingBalancePaise() + transactions.stream().filter(t -> t.accountId().equals(account.id()) && !t.date().isBefore(account.openingDate()) && !t.date().isAfter(cutoff)).mapToLong(t -> t.type() == Ledger.Type.CREDIT ? t.amountPaise() : -t.amountPaise()).sum();
    }
    public List<Ledger.Transaction> accountTransactions(UUID accountId) { return transactions.stream().filter(t -> t.accountId().equals(accountId)).sorted(Comparator.comparing(Ledger.Transaction::date).reversed()).toList(); }
    public synchronized Ledger.Transaction addTransaction(UUID accountId, Ledger.Type type, long amountPaise, LocalDate date, String description, String notes, UUID actor, UUID needId, String idempotencyKey) {
        return addTransaction(accountId, type, amountPaise, date, description, notes, actor, needId, idempotencyKey, null);
    }
    private synchronized Ledger.Transaction addTransaction(UUID accountId, Ledger.Type type, long amountPaise, LocalDate date, String description, String notes, UUID actor, UUID needId, String idempotencyKey, UUID transferId) {
        if (!accounts.containsKey(accountId) || amountPaise <= 0 || idempotencyKey == null || idempotencyKey.isBlank()) throw new IllegalArgumentException("Invalid transaction");
        var existing = idempotentTransactions.get(idempotencyKey);
        if (existing != null) return existing;
        var transaction = new Ledger.Transaction(UUID.randomUUID(), accountId, type, amountPaise, date, description, notes, actor, needId, transferId);
        if (sheetsWriter != null) sheetsWriter.append(transaction, idempotencyKey);
        transactions.add(transaction);
        idempotentTransactions.put(idempotencyKey, transaction);
        return transaction;
    }

    public List<Ledger.Transaction> transfer(UUID senderAccountId, UUID recipientAccountId, long amountPaise, UUID actor, LocalDate date, String notes, String operationKey) {
        writeLock.lock();
        try {
            if (senderAccountId.equals(recipientAccountId) || amountPaise <= 0 || !accounts.containsKey(senderAccountId) || !accounts.containsKey(recipientAccountId)) throw new IllegalArgumentException("Invalid transfer");
            var transferId = UUID.nameUUIDFromBytes(operationKey.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            var debit = addTransaction(senderAccountId, Ledger.Type.DEBIT, amountPaise, date, "Transfer to family account", notes, actor, null, operationKey + ":debit", transferId);
            var credit = addTransaction(recipientAccountId, Ledger.Type.CREDIT, amountPaise, date, "Transfer from family account", notes, actor, null, operationKey + ":credit", transferId);
            return List.of(debit, credit);
        } finally { writeLock.unlock(); }
    }
    private Ledger.Transaction addSeedTransaction(UUID accountId, Ledger.Type type, long amountPaise, LocalDate date, String description, String notes, UUID actor, UUID needId) {
        var tx = new Ledger.Transaction(UUID.randomUUID(), accountId, type, amountPaise, date, description, notes, actor, needId, null); transactions.add(tx); return tx;
    }
    public Ledger.Need createNeed(UUID requester, String cause, long amountPaise, LocalDate requiredBy, String notes) { if (amountPaise <= 0) throw new IllegalArgumentException("Amount must be positive"); var n = new Ledger.Need(UUID.randomUUID(), requester, users.get(requester).displayName(), cause, amountPaise, requiredBy, notes, Ledger.NeedStatus.PENDING, 0, OffsetDateTime.now(ZONE)); needs.put(n.id(), n); return n; }
    public Ledger.Need updateStatus(UUID id, Ledger.NeedStatus status) { var old = needs.get(id); if (old == null) throw new NoSuchElementException(); if (old.status() == Ledger.NeedStatus.FUNDED || old.status() == Ledger.NeedStatus.REJECTED) throw new IllegalArgumentException("Terminal need status cannot change"); if (status != Ledger.NeedStatus.APPROVED && status != Ledger.NeedStatus.REJECTED) throw new IllegalArgumentException("Only approval or rejection is allowed here"); var updated = new Ledger.Need(old.id(), old.requestedBy(), old.requesterName(), old.cause(), old.amountPaise(), old.requiredBy(), old.notes(), status, old.fundedPaise(), old.createdAt()); needs.put(id, updated); return updated; }
    public Ledger.Need fundNeed(UUID id, long amountPaise, UUID actor) { writeLock.lock(); try { var old = needs.get(id); if (old == null || (old.status() != Ledger.NeedStatus.APPROVED && old.status() != Ledger.NeedStatus.PARTIALLY_FUNDED) || amountPaise <= 0 || amountPaise > old.remainingPaise()) throw new IllegalArgumentException("Funding exceeds remaining need or need is not approved"); var account = accounts.values().stream().filter(a -> a.ownerId().equals(old.requestedBy())).findFirst().orElseThrow(); var tx = addTransaction(account.id(), Ledger.Type.CREDIT, amountPaise, LocalDate.now(ZONE), "Funding: " + old.cause(), "Need funding", actor, id, "need-funding-" + id + "-" + old.fundedPaise()); var funding = new Ledger.Funding(UUID.randomUUID(), id, tx.id(), amountPaise, actor, OffsetDateTime.now(ZONE), "Need funding"); if (fundingWriter != null) fundingWriter.append(funding); fundings.computeIfAbsent(id, ignored -> new ArrayList<>()).add(funding); var funded = old.fundedPaise() + tx.amountPaise(); var status = funded == old.amountPaise() ? Ledger.NeedStatus.FUNDED : Ledger.NeedStatus.PARTIALLY_FUNDED; var updated = new Ledger.Need(old.id(), old.requestedBy(), old.requesterName(), old.cause(), old.amountPaise(), old.requiredBy(), old.notes(), status, funded, old.createdAt()); needs.put(id, updated); return updated; } finally { writeLock.unlock(); } }
}
