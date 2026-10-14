# Recovery and backup

Google Sheets does not provide relational constraints, unique indexes, or full multi-record ACID transactions. Funding a need crosses Transactions and NeedFunding, so the production adapter must use a stable operation ID, a single-writer lock for the first deployment, idempotency checks, and a reconciliation job for partial writes.

Before retrying an uncertain append, search by idempotency key and transaction UUID. Never blindly append a second credit. If only one side of a funding operation exists, mark the operation pending, reconcile the records, and either complete the missing reference or create a documented compensating reversal. Do not report funding complete until both records are verified.

Export the private spreadsheet regularly to an access-controlled backup location. Test restore into a separate spreadsheet, validate headers and UUID uniqueness, and record the restore date. If the service scales beyond one instance, replace the process lock with a distributed lock or move persistence to PostgreSQL.
