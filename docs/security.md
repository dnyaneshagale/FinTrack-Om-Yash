# Security notes

Production must use BCrypt password hashes, server-side sessions, HttpOnly/Secure cookies, SameSite configuration compatible with the deployment topology, CSRF protection, exact CORS origins, generic login failures, rate limiting, session expiry, and server-side role checks on every write. Password hashes, cookies, service credentials, and secrets must never be returned by APIs or written to audit logs.

The current security configuration remains intentionally permissive for local API development and disables CSRF because the authentication/session workflow is not implemented yet. The deployment manifests must therefore be treated as infrastructure-ready, not fully security-ready: do not connect real family data until BCrypt authentication, server sessions, CSRF, rate limiting, and backend role checks are implemented. Google Sheets is not an authentication database and hidden tabs are not access control; a dedicated identity store is recommended for production.
