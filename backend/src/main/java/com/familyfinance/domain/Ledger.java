package com.familyfinance.domain;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public final class Ledger {
    public enum Role { ADMIN, MEMBER }
    public enum Type { CREDIT, DEBIT }
    public enum NeedStatus { PENDING, APPROVED, PARTIALLY_FUNDED, FUNDED, REJECTED }

    public record User(UUID id, String username, String displayName, Role role) {}
    public record Account(UUID id, UUID ownerId, String ownerName, long openingBalancePaise, LocalDate openingDate) {}
    public record Transaction(UUID id, UUID accountId, Type type, long amountPaise, LocalDate date, String description, String notes, UUID createdBy, UUID needId, UUID transferId) {}
    public record Funding(UUID id, UUID needId, UUID transactionId, long amountPaise, UUID fundedBy, OffsetDateTime fundedAt, String notes) {}
    public record Need(UUID id, UUID requestedBy, String requesterName, String cause, long amountPaise, LocalDate requiredBy, String notes, NeedStatus status, long fundedPaise, OffsetDateTime createdAt) {
        public long remainingPaise() { return amountPaise - fundedPaise; }
    }
}
