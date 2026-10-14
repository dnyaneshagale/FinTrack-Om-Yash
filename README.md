# Fintrack Om Yash

A mobile-responsive family ledger for Pappa, Om, and Yash. Money is represented as integer paise in the backend and formatted as INR only at the API/UI boundary.

## Current implementation

- Spring Boot 3.5.6 / Java 21 backend with account, transaction, notes, needs, partial funding, balance calculation, CORS, actuator health, and focused JUnit coverage.
- React + TypeScript + Vite frontend with responsive family overview, combined balances, activity history, needs progress, and an add-transaction flow.
- Production storage is Google Sheets when `STORAGE_MODE=sheets`; automated tests force memory mode and never require Google credentials.
- `LedgerRepository` is the persistence seam for a Google Sheets adapter; the included service is deliberately usable without production credentials.

## Run locally

Backend prerequisites: Java 21 and Maven.

```powershell
cd backend
mvn test
mvn spring-boot:run
```

Frontend prerequisites: Node.js 20+.

```powershell
cd frontend
npm install
npm run dev
```

Open the Vite URL shown in the terminal, normally `http://localhost:5173`. Copy `frontend/.env.example` to `.env` when the API is hosted elsewhere.

## Production setup

See [docs/setup.md](docs/setup.md), [docs/deployment.md](docs/deployment.md), [docs/security.md](docs/security.md), and [docs/recovery-and-backup.md](docs/recovery-and-backup.md).

Deployment manifests are included: `backend/Dockerfile` and `render.yaml` for Render, plus `frontend/netlify.toml` for Netlify. Set `VITE_API_BASE_URL` in Netlify and `FRONTEND_ORIGIN` in Render to the deployed URLs.

## Verification

Verified in this workspace: `backend/mvn test`, `frontend/npm run build`, and `frontend/npm run lint`. Google credentials, deployed persistence, Playwright/mobile browser checks, and production authentication were not executed because no external services or credentials were provided.
