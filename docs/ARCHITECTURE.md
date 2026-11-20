# ApplyRank architecture

![ApplyRank architecture](architecture.png)

The same diagram as text (GitHub draws it automatically):

```mermaid
flowchart TB
  U([User in browser]) -->|HTML CSS JS| FE[Frontend<br/>static pages<br/>Live Server :5500]
  FE -->|REST + JWT<br/>fetch / SSE stream| API[Spring Boot API :8080]
  subgraph BE[Backend]
    API --> SEC[Security<br/>JwtAuthFilter, rate limit, CORS]
    SEC --> CTL[Controllers<br/>auth, jobs, tests, resume,<br/>ailab, profile, privacy,<br/>notifications, recruiter]
    CTL --> SVC[Services<br/>scoring, resume reader,<br/>code review, scam check,<br/>page audit, resume polish]
    SVC --> REPO[(JPA repositories)]
  end
  REPO --> DB[(H2 default<br/>MySQL profile)]
  SVC -.optional, with consent.-> AI[Google Gemini API]
  SVC -.optional.-> MAIL[SMTP e-mail]
```

## How a request travels
1. The browser loads plain HTML, CSS and JavaScript (the frontend repo, no build step).
2. `js/api.js` calls the backend with `fetch`. The login token (JWT) goes in the `Authorization` header.
3. In the backend, `JwtAuthFilter` checks the token (including its version number, so "log out everywhere" works), then the rate limiter and CORS rules run.
4. A controller in `web/` receives the request and calls a service in `service/`.
5. Services use JPA repositories to read and write the database (H2 for testing, MySQL with the `mysql` profile).
6. AI Lab only: if the user agreed, the service sends hidden-data text to Gemini and streams the answer back with Server-Sent Events.

## Key design choices
- **Stateless JWT with a version claim:** logout and password reset raise the user's token version, so old tokens stop working.
- **Privacy first:** emails, phone numbers and links are hidden before any text goes to the AI, resume files are read in memory and never saved, and recruiters see the full email only after a shortlist.
- **Rules first, AI second:** every AI Lab tool gives a useful answer from plain rules, and the AI is an optional extra with a per-user hourly quota.
- **Secrets only as environment variables:** see `.env.example` in the repo root.
