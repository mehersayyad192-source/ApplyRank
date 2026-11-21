# Deploying ApplyRank for free

Order: **database -> backend -> website -> monitoring -> smoke test**.
Screens and button names on these sites change from time to time. If a name is different, look for the closest one.

## What the free plans give (checked November 2026, check again before you start)
| Part | Service | Free plan facts |
|---|---|---|
| Database | **TiDB Cloud Starter** (MySQL-compatible) | 5 GiB row storage and 50 million Request Units per month, up to 5 free instances. When the quota is used up, new connections are refused until next month. ([docs](https://docs.pingcap.com/tidbcloud/select-cluster-tier)) |
| Backend | **Render** web service (Docker) | 750 free hours per month. Sleeps after **15 minutes** without traffic, wakes in about **1 minute**. **No persistent disk**: files are lost on restart. ([docs](https://render.com/docs/free)) |
| Website | **GitHub Pages** | Free for public repositories. |
| Monitor | **UptimeRobot** (free plan) | Pings your health URL on a timer and e-mails you when it is down. |

Why not the other options: Aiven has retired its free MySQL plan, and Render's free Postgres database expires after 30 days.

Things to accept on a free plan:
- The first request after a pause is slow (the website shows a "server is waking up" message).
- Resume **files** uploaded on the dashboard are kept on the server disk, so they disappear when Render restarts. The analysis result stays in the database.
- H2 (the default local database) must NOT be used live, because its file would disappear too.

---

## Step 0. Clean the repository (do this once, before anything else)
`target/` (compiled files) and `data/` (your local H2 database file) were pushed by mistake. They contain only demo data, but they do not belong on GitHub.
```
git rm -r --cached target data
git add .gitignore
git commit -m "Stop tracking build output and local database"
git push
```
Check that nothing else is tracked by mistake: `git ls-files | findstr /i ".env .pem .key"` must show only `.env.example`.

## Step 1. Database (TiDB Cloud)
1. Sign up at TiDB Cloud and create a **Starter** instance (the free one). Pick the region nearest to you.
2. Open its **SQL editor** and run: `CREATE DATABASE applyrank CHARACTER SET utf8mb4;`
3. Click **Connect**. Note the **host**, **port (4000)**, **user** and set a **password**. The user looks like `abc123.root`.
4. Your connection address is:
   `jdbc:mysql://<host>:4000/applyrank?sslMode=VERIFY_IDENTITY&serverTimezone=UTC`
5. **Test it from your own computer first** (PowerShell, same window):
```
$env:SPRING_PROFILES_ACTIVE = "prod,mysql"
$env:DB_URL = "jdbc:mysql://<host>:4000/applyrank?sslMode=VERIFY_IDENTITY&serverTimezone=UTC"
$env:DB_USER = "<user>"
$env:DB_PASSWORD = "<password>"
$b = New-Object byte[] 48; [System.Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b)
$env:JWT_SECRET = [Convert]::ToBase64String($b)
mvn spring-boot:run
```
   You should see `Startup checks passed (production mode: true)` and no database error. Open http://localhost:8080/api/health.
   (I could not test against TiDB from my side. If the connection fails, send the first red error line.)

## Step 2. Backend on Render
1. Render -> **New** -> **Web Service** -> connect your GitHub repository `applyrank`.
2. **Runtime: Docker** (the `Dockerfile` in the repository root is used). **Instance type: Free**.
3. **Health Check Path:** `/api/health`. Leave **Auto-Deploy** on: every push to `main` deploys by itself (this is your CD).
4. **Environment variables** (Environment tab):

| Name | Value |
|---|---|
| `SPRING_PROFILES_ACTIVE` | `prod,mysql` |
| `JWT_SECRET` | a new random value (see the PowerShell line above). Never reuse the local one. |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | from TiDB (Step 1) |
| `GEMINI_API_KEY` | your key (optional, AI Lab then also gives AI advice) |
| `GEMINI_MODEL` | `gemini-3.5-flash-lite` |
| `CORS_ORIGINS` | `https://<your-github-name>.github.io` (no slash and no path at the end) |
| `FRONTEND_URL` | `https://<your-github-name>.github.io/applyrank` |
| `DEMO_RECRUITER_PASSWORD` | a strong password only you know |
| `DEMO_CANDIDATE_PASSWORD` | optional. Set one only if you want to put a demo login in your README. Everyone shares that account, so tell visitors not to upload a private resume. |
| `JAVA_OPTS` | `-Xss512k` (optional, saves a little memory) |

5. Deploy. The first build takes several minutes. When it is live, open `https://<service-name>.onrender.com/api/health`. It should show `"status":"ok"`.
6. If the log says **out of memory** or the service keeps restarting, the free instance is too small for this app. Tell me, we will reduce memory use (this is a known risk for Java apps on 512 MB hosts; I did not verify the exact memory of the free plan).

## Step 3. Website on GitHub Pages
1. Open `js/api.js` and change this one line to your Render address (keep `/api` at the end):
   `var LIVE_API = 'https://<service-name>.onrender.com/api';`
2. Commit and push.
3. GitHub -> repository **Settings** -> **Pages** -> Source: **Deploy from a branch** -> Branch `main`, folder `/ (root)` -> Save.
4. After a minute the site is at `https://<your-github-name>.github.io/applyrank/`.
5. On your computer the site still uses `localhost:8080` automatically, so you can keep developing.

## Step 4. Monitoring
- **Logs:** Render -> your service -> **Logs**. Every API call is one line: `time level [request-id] ... GET /api/jobs -> 200 in 35 ms`. A slow call (2 seconds or more) is shown as `SLOW`. Query strings, tokens, passwords and IP addresses are never logged. Every response has an `X-Request-Id` header, so a user's problem can be found by that id.
- **Uptime:** create a free UptimeRobot account -> **Add monitor** -> HTTP(s) -> URL `https://<service-name>.onrender.com/api/health` -> interval 5 minutes (use the shortest free interval) -> add your e-mail.
  Bonus: the pings stop Render from sleeping. One service running all month uses about 744 of the 750 free hours, so this is fine while it is your only free service on that account.
- **Daily smoke test:** GitHub -> Settings -> Secrets and variables -> Actions -> **Variables** -> New: `LIVE_API_URL` = `https://<service-name>.onrender.com`. The workflow `Live smoke test` then checks the live site every day, and you can also run it by hand after each deploy (Actions tab -> Live smoke test -> Run workflow).

## Step 5. Load test
Run it on your own computer, not on the free live server. See `loadtest/README.md`. Write your real numbers into the README.

## Step 6. Click-through on the live site
Register a new account, log in, log out, forgot password (the reset link is printed in the Render **Logs** until you set up e-mail), a skill test, the AI Lab, Resume Polish with a PDF, Profile -> Download my data, Delete account. Then open the site on your phone.

## Step 7. E-mail (later)
Set these in Render: `SPRING_MAIL_HOST=smtp.gmail.com`, `SPRING_MAIL_PORT=587`, `SPRING_MAIL_USERNAME`, `SPRING_MAIL_PASSWORD` (a Gmail **App Password**), `SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH=true`, `SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE=true`. Then set `REQUIRE_VERIFIED_EMAIL=true`.
Never put the App Password in a file, only in Render's Environment tab.

## Safety checklist before you share the link
- [ ] `JWT_SECRET` set on Render (the server refuses to start without a strong one)
- [ ] No real key or password in any file or in the Git history
- [ ] `DEMO_RECRUITER_PASSWORD` is set by you
- [ ] `CORS_ORIGINS` is only your GitHub Pages address
- [ ] Privacy and Terms pages have your contact e-mail instead of `[PUT YOUR CONTACT E-MAIL HERE]`
- [ ] Any key that was ever pasted into a chat or file has been revoked and replaced
