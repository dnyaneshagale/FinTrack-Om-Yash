package com.familyfinance.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/** Storage boundary for replacing the development ledger with Google Sheets or PostgreSQL. */
public interface LedgerRepository {
    List<Ledger.User> users();
    List<Ledger.Account> accounts();
    List<Ledger.Transaction> transactions(UUID accountId);
    Optional<Ledger.Need> need(UUID needId);
    Ledger.Transaction appendTransaction(Ledger.Transaction transaction, String idempotencyKey);
    Ledger.Need saveNeed(Ledger.Need need);
}
