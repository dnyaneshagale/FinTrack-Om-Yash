# Local and Google Sheets setup

1. Create a Google Cloud project and enable Google Sheets API.
2. Create a service account and grant its email Editor access to a private spreadsheet named `Family Finance Tracker`.
3. Set `GOOGLE_SHEETS_SPREADSHEET_ID` and provide the service account JSON through a hosting secret or `GOOGLE_SERVICE_ACCOUNT_JSON`; never commit the JSON file.
4. Create tabs named `Users`, `Accounts`, `Transactions`, `Needs`, `NeedFunding`, `AuditLog`, and optionally `Reconciliations`. The adapter must validate exact headers before writes.
5. Configure `backend/.env.example` values in the runtime environment and set `FRONTEND_ORIGIN` to the exact deployed frontend origin.
6. Set `INITIAL_FATHER_PASSWORD`, `INITIAL_SON1_PASSWORD`, and `INITIAL_SON2_PASSWORD` through the hosting secret manager. Change the development fallback values before any deployment.

The current local profile uses an in-memory seed so the UI can be developed without Google credentials. Set `STORAGE_MODE=sheets` only after sharing the private spreadsheet with the service account. In Sheets mode, startup validates/creates the required tabs and user-created transaction writes are appended with their idempotency key. Need/account backfill and recovery workflows must be verified before production use.
