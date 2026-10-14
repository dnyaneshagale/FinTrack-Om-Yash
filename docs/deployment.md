# Deployment

The repository includes a Dockerized backend, Render blueprint, and Netlify SPA configuration. These provide repeatable builds and routing, but external credentials and hosted service settings must still be supplied.

## Backend on Render

Deploy the `backend` directory using the included `Dockerfile`, or apply the root `render.yaml` blueprint. Configure `PORT`, `FRONTEND_ORIGIN`, Google Sheets secrets, session settings, and administrator bootstrap values as Render secrets. Render should probe `/actuator/health`.

## Frontend on Netlify

Create a Netlify site from the repository with base directory `frontend`, build command `npm run build`, and publish directory `frontend/dist`. The included `frontend/netlify.toml` applies the SPA fallback routing. Configure `VITE_API_BASE_URL` to the deployed Render backend `/api/v1` URL, then set backend `FRONTEND_ORIGIN` to the exact Netlify origin.

Do not set Google credentials in any `VITE_` variable. Netlify frontend variables are public at build time.

## Render environment variables

Set `STORAGE_MODE=sheets`, `GOOGLE_SHEETS_SPREADSHEET_ID`, and `GOOGLE_SERVICE_ACCOUNT_JSON` as Render secrets. Set `INITIAL_FATHER_PASSWORD`, `INITIAL_SON1_PASSWORD`, and `INITIAL_SON2_PASSWORD` to strong values. Set `FATHER_DISPLAY_NAME=Pappa`, `SON1_DISPLAY_NAME=Om`, and `SON2_DISPLAY_NAME=Yash`.

Share the private spreadsheet with the service-account `client_email` as Editor. Verify `/actuator/health`, then test login and account loading before using real data.

After deployment, verify `/actuator/health`, login/session behavior, CORS, spreadsheet access, duplicate submission behavior, and a test transaction in a non-production sheet before switching the spreadsheet ID.
