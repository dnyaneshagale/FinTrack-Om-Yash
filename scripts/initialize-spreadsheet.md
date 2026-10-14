# Spreadsheet initialization

The production setup command should create missing tabs and validate these headers without deleting rows:

- Users: `userId, username, displayName, role, active, createdAt, updatedAt`
- Accounts: `accountId, ownerUserId, currency, openingBalancePaise, openingDate, active, createdAt, updatedAt`
- Transactions: `transactionId, accountId, type, amountPaise, transactionDate, description, notes, createdBy, createdAt, idempotencyKey, needId, transferId, reversalOfTransactionId`
- Needs: `needId, requestedBy, cause, amountPaise, requiredByDate, notes, status, createdAt, updatedAt, reviewedBy, reviewedAt`
- NeedFunding: `fundingId, needId, transactionId, amountPaise, fundedBy, fundedAt, notes`
- AuditLog: `auditId, actorUserId, action, entityType, entityId, timestamp, details`
- Reconciliations: `reconciliationId, accountId, reconciliationDate, actualBalancePaise, calculatedBalancePaise, differencePaise, submittedBy, createdAt, notes`

This document is intentionally a setup contract rather than a fake connectivity script. The command should fail safely on malformed existing headers and never delete existing financial records.
