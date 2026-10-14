package com.familyfinance;

import com.familyfinance.domain.Ledger;
import com.familyfinance.service.LedgerService;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;
import static org.junit.jupiter.api.Assertions.*;

class LedgerServiceTest {
    @Test void creditAndDebitUseExactPaiseAndRespectCutoff() {
        var service = new LedgerService();
        var account = service.accounts().iterator().next();
        var before = service.balance(account, LocalDate.of(2025, 12, 31));
        assertEquals(account.openingBalancePaise(), before);
        service.addTransaction(account.id(), Ledger.Type.CREDIT, 12345, LocalDate.of(2026, 2, 1), "Allowance", "", account.ownerId(), null, "test-credit");
        service.addTransaction(account.id(), Ledger.Type.DEBIT, 2345, LocalDate.of(2026, 2, 2), "Bus", "", account.ownerId(), null, "test-debit");
        assertEquals(account.openingBalancePaise() + 10000, service.balance(account, LocalDate.of(2026, 2, 2)));
        assertEquals(account.openingBalancePaise(), service.balance(account, LocalDate.of(2026, 1, 31)));
    }

    @Test void fundingCreatesOneCreditAndMovesStatus() {
        var service = new LedgerService();
        var son = service.users().stream().filter(u -> u.username().equals("son1")).findFirst().orElseThrow();
        var need = service.createNeed(son.id(), "Exam fee", 10000, LocalDate.now(), "Urgent");
        service.updateStatus(need.id(), Ledger.NeedStatus.APPROVED);
        var funded = service.fundNeed(need.id(), 4000, service.users().iterator().next().id());
        assertEquals(Ledger.NeedStatus.PARTIALLY_FUNDED, funded.status());
        assertEquals(6000, funded.remainingPaise());
        assertEquals(1, service.transactions().stream().filter(t -> need.id().equals(t.needId())).count());
    }

    @Test void repeatedIdempotencyKeyDoesNotDuplicateLedgerEntry() {
        var service = new LedgerService();
        var account = service.accounts().iterator().next();
        var first = service.addTransaction(account.id(), Ledger.Type.CREDIT, 100, LocalDate.now(), "Test", "", account.ownerId(), null, "same-request");
        var second = service.addTransaction(account.id(), Ledger.Type.CREDIT, 100, LocalDate.now(), "Test", "", account.ownerId(), null, "same-request");
        assertEquals(first.id(), second.id());
        assertEquals(1, service.transactions().stream().filter(t -> t.id().equals(first.id())).count());
    }

    @Test void transferCreatesEqualOpposingEntries() {
        var service = new LedgerService();
        var accounts = service.accounts().stream().toList();
        var entries = service.transfer(accounts.get(0).id(), accounts.get(1).id(), 5000, accounts.get(0).ownerId(), LocalDate.now(), "Family transfer", "transfer-test");
        assertEquals(2, entries.size());
        assertEquals(entries.get(0).transferId(), entries.get(1).transferId());
        assertEquals(Ledger.Type.DEBIT, entries.get(0).type());
        assertEquals(Ledger.Type.CREDIT, entries.get(1).type());
        assertEquals(5000, entries.get(0).amountPaise());
        assertEquals(5000, entries.get(1).amountPaise());
    }
}
