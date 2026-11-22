# ApplyRank

[![CI](https://github.com)](https://github.com)

**Live Website:** https://mehersayyad192-source.github.io/ApplyRank/

Static HTML, CSS and JavaScript. No build step.

Pages: home (login), dashboard, jobs, skill test (9 tracks), resume, AI Lab (code review, page auditor, scam detector, resume polish), profile, notifications, recruiter.

## Run
1. Start the backend (see the backend README), it runs on http://localhost:8080.
2. In VS Code open this folder, right click `index.html` and choose **Open with Live Server** (port 5500).

The API address is set in one place: `js/api.js` (the `BASE` line at the top).

## Privacy
No keys or passwords are stored in this folder. Login tokens stay in the browser only.

## Folder layout
```
index.html, dashboard.html, jobs.html ...   the pages
css/    style sheets (style.css is shared by all pages)
js/     scripts (api.js talks to the backend)
.github/workflows/check.yml   checks JS syntax and links on every push
```
How the whole system fits together: see `docs/ARCHITECTURE.md` in the backend repo.

## Production features
- **Docker** image (`Dockerfile`, `docker-compose.yml`), built and checked on every push.
- **CI/CD:** GitHub Actions run all tests and the Docker build (`.github/workflows/ci.yml`); Render redeploys on every push. A daily **live smoke test** checks the deployed site (`smoke.yml`).
- **Safe by default in production:** the `prod` profile refuses to start with a weak `JWT_SECRET`, turns off the H2 console and has no public demo passwords. Real client addresses are read behind the proxy, so the rate limit works per user.
- **Logging:** one line per request with a request id and timing, slow requests flagged, no personal data in the log.
- **Monitoring:** `/api/health` (with uptime) watched by an uptime monitor.
- **Load test:** `loadtest/k6-smoke.js` (k6).
- **Deploy guide:** `docs/DEPLOY.md`.
