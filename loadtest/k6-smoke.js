// Load test with k6 (free tool: https://k6.io).  Install on Windows:  winget install k6
// Run against your own computer first (do NOT load-test a free live server hard; it may get suspended):
//    k6 run loadtest/k6-smoke.js
// Run against another address:
//    k6 run -e BASE=https://your-app.onrender.com/api loadtest/k6-smoke.js
//
// What it does: 20 virtual users, 40 seconds, each one reads the public job list and the health check.
// The test PASSES only if almost nothing fails and 95% of answers are faster than 800 ms.
import http from 'k6/http';
import { check, sleep } from 'k6';

const BASE = __ENV.BASE || 'http://localhost:8080/api';

export const options = {
  stages: [
    { duration: '10s', target: 20 },   // slowly go up to 20 users
    { duration: '20s', target: 20 },   // stay there
    { duration: '10s', target: 0 },    // slowly go down
  ],
  thresholds: {
    http_req_failed: ['rate<0.02'],     // fewer than 2% failed requests
    http_req_duration: ['p(95)<800'],   // 95% of requests under 800 ms
  },
};

export default function () {
  const health = http.get(`${BASE}/health`);
  check(health, { 'health is 200': (r) => r.status === 200 });

  const jobs = http.get(`${BASE}/jobs`);
  check(jobs, { 'jobs is 200': (r) => r.status === 200 });

  // a login with a wrong password must be refused quickly, not crash the server
  const bad = http.post(`${BASE}/auth/login`, JSON.stringify({ email: 'nobody@example.com', password: 'wrong-password' }),
    { headers: { 'Content-Type': 'application/json' } });
  check(bad, { 'wrong login refused (4xx, not 5xx)': (r) => r.status >= 400 && r.status < 500 });

  sleep(1);
}
