 # ApplyRank — Full-Stack Job Readiness & Skill Verification Platform

Full-stack web application built with **Java 17, Spring Boot 3, Spring Security (JWT), and Vanilla HTML5/CSS3/JS** — featuring server-side skill assessments, ATS-friendly PDF resume generation, notifications, and role-based recruiter workflows.


## ApplyRank Backend (Spring Boot 3 + Java 17)

REST API for the ApplyRank site: login (JWT), profile, jobs & applications, server-side skill-test scoring
(the verified rank can't be faked from the browser), notifications, recruiter ranking, resume check.

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
If you edit `questions-bank.json` later, stop the app and delete the `data/` folder (H2) so the table is re-seeded.

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
| GET/PUT | /api/notifications, /{id}/read, /read-all, /unread-count | user | notifications |
| POST | /api/resume (multipart `file`,`track`) | user | PDF/DOCX check, score + missing keywords |
| POST/GET | /api/recruiter/jobs | recruiter | create / list my jobs |
| GET | /api/recruiter/jobs/{id}/applicants | recruiter | applicants, best verified rank first |
| PUT | /api/recruiter/applications/{id}/status | recruiter | move stage / outcome -> notifies candidate |

Errors are always `{"error":"...","message":"..."}`.

## Performance & design (talking points)
- **Question bank in the database** (`questions-bank.json` -> table `questions`, 10 per track). The server picks 5 at random per attempt (ids only are loaded, shuffled in Java -> same code on H2 and MySQL), stores them in a `test_sessions` row and grades from that row. The browser never receives the answer key, and a locked answer can't be changed.
- **Optimistic locking** (`@Version` on `test_sessions`): a double-click / parallel submit cannot be counted twice (HTTP 409).
- **N+1 fixed and tested:** the applicants list uses `@EntityGraph` (one join query) + one query for all applicants' latest results. `QueryCountTest` fails if the number of SQL statements grows with the number of applicants.
- **Indexes** on the columns we really filter by: `applications(job_id)`, `notifications(user_id, created_at)`, `test_results(user_id, created_at)`, `questions(domain)`, `jobs(track, active)`, `test_sessions(user_id)`. Verify with `EXPLAIN` (see below) - don't quote "10x" without measuring.
- **Swagger UI:** http://localhost:8080/swagger-ui.html (login -> copy token -> Authorize).
- **Docker Compose:** `docker compose up --build` starts MySQL 8 + the API (non-root container user). **CI:** `.github/workflows/ci.yml` runs `mvn verify` on every push.

### See the indexes working (H2 console or MySQL)
`EXPLAIN SELECT * FROM applications WHERE job_id = 1;`  -> should mention the index `idx_app_job` instead of a full table scan.
To print every SQL statement the app runs: `mvn spring-boot:run -Dspring-boot.run.arguments=--spring.jpa.show-sql=true`.

## Security & privacy (talking points)
- **Passwords:** BCrypt hashes, policy 8+ chars with letter+number; login always runs a hash check so timing doesn't reveal which e-mails exist.
- **Brute-force protection:** 5 wrong logins (same e-mail + IP) -> locked 15 min, HTTP 429 (`LoginAttemptService`). In-memory; use Redis for multi-server.
- **JWT:** stateless, signed (HS256), 24h expiry, secret from `JWT_SECRET` env var (startup warning if default).
- **Role-based access:** `/api/recruiter/**` needs RECRUITER; a recruiter can only touch their OWN jobs' applicants.
- **Privacy by design:** recruiters see a masked e-mail (`a***@x.com`) until they shortlist the candidate.
- **User rights:** `GET /api/privacy/export` (download all my data) and `DELETE /api/privacy/account` (password-confirmed erasure incl. resume files).
- **Audit log:** table `audit_log` (LOGIN_OK/FAIL/LOCKED, REGISTER, DATA_EXPORT, ACCOUNT_DELETED, APPLICATION_STATUS) - never stores passwords or tokens.
- **Hardening:** security headers (HSTS, no-referrer, CSP frame-ancestors, nosniff, permissions-policy), no stack traces in errors, CORS allow-list, upload type check by magic bytes + 5 MB cap, files saved under random UUID names, SQL-injection-safe (JPA parameter binding).

## Honest limitations (good to mention in interviews)
- Code-round scores are the fraction of tests passed **reported by the browser** (clamped 0..1). Next step: run submissions server-side (Judge0).
- Resume score is a transparent keyword/length/metrics heuristic, not AI.
- Anyone can register as RECRUITER (demo). In production: invite codes or admin approval.
- Change `app.jwt.secret` before deploying (use an environment variable).
