# Load test

1. Install k6:  `winget install k6`  (Windows).
2. Start the backend (`mvn spring-boot:run`).
3. Run:  `k6 run loadtest/k6-smoke.js`
4. Read the end of the output: `http_req_duration ... p(95)=...` and `http_req_failed`. A tick (✓) next to both thresholds means PASS.

Write your own numbers into the main README, for example:
"Load test (k6, 20 users, 40 s, on my laptop): p95 = ___ ms, errors = ___ %". Real numbers from your own run are what interviewers like.

Two things to know:
- The rate limiter allows 300 requests/minute per IP (`RATE_LIMIT_PER_MINUTE`). All k6 users come from your one computer, so you may see HTTP 429. For the local test start the backend with a higher limit:
  `$env:RATE_LIMIT_PER_MINUTE = "100000"` (PowerShell). Do NOT use this value on the live server.
- Do not hammer the free live server. Run the test locally.
