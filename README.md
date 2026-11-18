# ApplyRank Backend (Spring Boot 3 + Java 17)

The API address is set in one place: `api.js` (the `BASE` line at the top).

## Run (VS Code)
1. Install **JDK 17+** (Temurin) and **Maven 3.9+**. Check: `java -version`, `mvn -version`.
2. VS Code extensions: *Extension Pack for Java*, *Spring Boot Extension Pack*.
3. `File > Open Folder...` -> this folder (the one with `pom.xml`). Wait for "Java: Ready".
4. Terminal: `mvn spring-boot:run`  (first run downloads dependencies, 2-5 min).
5. Open http://localhost:8080/api/health -> `{"status":"ok"}`.
6. Frontend: right-click `index.html` -> *Open with Live Server* (port 5500). CORS already allows it.

Demo logins: `demo@applyrank.dev / Demo@1234` (candidate), `recruiter@applyrank.dev / Recruiter@123`.
H2 DB viewer: http://localhost:8080/h2-console  (JDBC URL `jdbc:h2:file:./data/applyrank`, user `sa`, empty password).
MySQL instead of H2: create DB `applyrank`, edit `application-mysql.properties`, run `mvn spring-boot:run -Dspring-boot.run.profiles=mysql`.
Tests: `mvn test`.  Everything with MySQL in Docker: `docker compose up --build`.
Questions are loaded per track: a track that has no questions yet gets them on the next start. To change questions of an existing track, stop the app and delete the `data/` folder (H2) (or clear the `questions` table in MySQL).

## API
| Method | Path | Auth | Purpose |
|---|---|---|---|
| POST | /api/auth/register, /login | - | returns `{token,user}` |
| GET | /api/auth/me | user | current user |
| GET/PUT | /api/profile | user | profile (same field names as profile.html) |
| GET | /api/jobs?track=backend, /api/jobs/{id} | - | list / one job |
| POST | /api/jobs/{id}/apply | user | apply (stores rank at apply time) |
| GET | /api/applications | user | my applications |
| GET | /api/tests/domains | user | tracks |
| GET | /api/tests/questions?domain=backend | user | starts an attempt: **5 random questions** from the DB, without answers |
| POST | /api/tests/answer | user | locks ONE answer, returns correct option + explanation |
| POST | /api/tests/interview-score | user | feedback for one interview answer |
| POST | /api/tests/submit | user | needs `sessionId`; **server scores** from its stored answers, saves verified rank (one submit per session) |
| GET | /api/tests/latest, /history | user | results |
| POST | /api/auth/logout | user | ends every login token of this user (token version +1) |
| POST | /api/auth/resend-verification | user | sends the e-mail verification link again |
| GET | /api/ailab/status, /history | user | AI Lab settings, my last checks (only small facts, never the pasted text) |
| POST | /api/ailab/code-review, /scam-check, /page-audit, /resume-polish | user | rules result + optional streamed AI advice (Server-Sent Events) |
| POST | /api/ailab/resume-polish/file (multipart `file`, `useAi`, `consent`) | user | same as /resume-polish but the text is read from a PDF/DOCX in memory (the file is never saved) |
| GET/PUT | /api/notifications, /{id}/read, /read-all, /unread-count | user | notifications |
| POST | /api/resume (multipart `file`,`track`) | user | PDF/DOCX check, score + missing keywords |
| POST/GET | /api/recruiter/jobs | recruiter | create / list my jobs |
| GET | /api/recruiter/jobs/{id}/applicants | recruiter | applicants, best verified rank first |
| PUT | /api/recruiter/applications/{id}/status | recruiter | move stage / outcome -> notifies candidate |

Errors are always `{"error":"...","message":"..."}`.

## Performance & design (talking points)
- **Question bank in the database** (`questions-bank.json` -> table `questions`, 12 per track, 9 tracks). The server picks 5 at random per attempt (ids only are loaded, shuffled in Java -> same code on H2 and MySQL), stores them in a `test_sessions` row and grades from that row. The browser never receives the answer key, and a locked answer can't be changed.
- **Optimistic locking** (`@Version` on `test_sessions`): a double-click / parallel submit cannot be counted twice (HTTP 409).
- **N+1 fixed and tested:** the applicants list uses `@EntityGraph` (one join query) + one query for all applicants' latest results. `QueryCountTest` fails if the number of SQL statements grows with the number of applicants.
- **Indexes** on the columns we really filter by: `applications(job_id)`, `notifications(user_id, created_at)`, `test_results(user_id, created_at)`, `questions(domain)`, `jobs(track, active)`, `test_sessions(user_id)`. Verify with `EXPLAIN` (see below) - don't quote "10x" without measuring.
- **Swagger UI:** http://localhost:8080/swagger-ui.html (login -> copy token -> Authorize).
- **Docker Compose:** `docker compose up --build` starts MySQL 8 + the API (non-root container user). **CI:** `.github/workflows/ci.yml` runs `mvn verify` on every push.

### See the indexes working (H2 console or MySQL)
`EXPLAIN SELECT * FROM applications WHERE job_id = 1;`  -> should mention the index `idx_app_job` instead of a full table scan.
To print every SQL statement the app runs: `mvn spring-boot:run -Dspring-boot.run.arguments=--spring.jpa.show-sql=true`.

## AI Lab (talking points)
- **Rules first, AI second.** Every tool (Code Review, Scam Detector, Page Auditor, Resume Polish) gives an instant result from my own rules. The AI advice is optional and streams in afterwards, so the tool still works without a key or when the AI is down.
- **Consent + privacy:** the user must tick a box before any text goes to Gemini. Resume Polish hides e-mail, phone and links on the server before anything else happens, and shows the user the exact cleaned text. Only small facts (tool, score, line count) are saved, never the pasted text.
- **Prompt-injection defense:** user text is wrapped in marker blocks and the system prompt says to treat it as data. AI text is shown with `textContent`, never as HTML.
- **No SSRF:** the Page Auditor takes pasted HTML, it never fetches a URL for the user.
- **Cost control:** hourly quota per user (default 10), input size limits, a failed AI call does not use up a check. Key only from the `GEMINI_API_KEY` environment variable.

## Environment variables (never put these in files or GitHub)
| Name | What for |
|---|---|
| `JWT_SECRET` | signs login tokens (32+ random characters) |
| `GEMINI_API_KEY`, `GEMINI_MODEL` | optional AI advice |
| `DB_PASSWORD` | MySQL profile |
| `SPRING_MAIL_*`, `APP_MAIL_FROM` | real e-mails (without them, links are printed in the console) |

## Security & privacy (talking points)
- **Passwords:** BCrypt hashes, policy 8+ chars with letter+number; login always runs a hash check so timing doesn't reveal which e-mails exist.
- **Brute-force protection:** 5 wrong logins (same e-mail + IP) -> locked 15 min, HTTP 429 (`LoginAttemptService`). In-memory; use Redis for multi-server.
- **JWT:** signed (HS256), 24h expiry, secret from `JWT_SECRET` env var (startup warning if default). It carries a token version `v`; logout and password reset raise the user's version, so older tokens stop working at once (one extra user lookup per request - the cost of revocation).
- **Role-based access:** `/api/recruiter/**` needs RECRUITER; a recruiter can only touch their OWN jobs' applicants.
- **Privacy by design:** recruiters see a masked e-mail (`a***@x.com`) until they shortlist the candidate.
- **User rights:** `GET /api/privacy/export` (download all my data) and `DELETE /api/privacy/account` (password-confirmed erasure incl. resume files).
- **Password reset & e-mail verification:** one-time links (32 random bytes, only the SHA-256 hash is stored, 30 min / 24 h expiry, single use). `POST /api/auth/forgot-password` always gives the same answer, so e-mails cannot be enumerated. Without SMTP the link is printed in the console (dev mode).
- **Pagination:** `GET /api/jobs?page=0&size=10` and `/api/notifications?page=0&size=20` (total in `X-Total-Count`).
- **Audit log:** table `audit_log` (LOGIN_OK/FAIL/LOCKED, REGISTER, DATA_EXPORT, ACCOUNT_DELETED, APPLICATION_STATUS) - never stores passwords or tokens.
- **Hardening:** security headers (HSTS, no-referrer, CSP frame-ancestors, nosniff, permissions-policy), no stack traces in errors, CORS allow-list, upload type check by magic bytes + 5 MB cap, files saved under random UUID names, SQL-injection-safe (JPA parameter binding).

## Honest limitations (good to mention in interviews)
- Code-round scores are the fraction of tests passed **reported by the browser** (clamped 0..1). Next step: run submissions server-side (Judge0).
- Resume score is a transparent keyword/length/metrics heuristic, not AI.
- Anyone can register as RECRUITER (demo). In production: invite codes or admin approval.
- Resume Polish works on pasted text only (no PDF upload to the AI) and cannot find a person's name or address, so the page asks the user to remove them. On the free Gemini tier Google may use sent text to improve its products, so the demo uses fake data.
- Change `app.jwt.secret` before deploying (use an environment variable).
