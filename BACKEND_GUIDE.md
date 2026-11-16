# ApplyRank Backend Guide (Hinglish)

Ye file tumhare liye hai: kya ban chuka hai, kya baaki hai, kaunsa concept kahan use hua hai, aur VS Code me ab kya karna hai.

---

## 1. Status: kya HO GAYA aur kya BAAKI hai

| Cheez | Status | Kahan hai / kyu baaki hai |
|---|---|---|
| Login, register, JWT token | HO GAYA | `AuthController`, `JwtService`, `JwtAuthFilter` |
| Password BCrypt hash | HO GAYA | `SecurityConfig.passwordEncoder` |
| 5 galat try = 15 min lock | HO GAYA | `LoginAttemptService` |
| Password rule (8-72, letter+number) | HO GAYA | `Dtos.RegisterRequest` |
| Audit log, security headers | HO GAYA | `AuditService`, `SecurityConfig` |
| Skill test (server session, answers hidden) | HO GAYA | `TestController`, `QuestionService`, `TestSession` |
| Jobs, apply, applications | HO GAYA | `JobController` |
| Notifications | HO GAYA | `NotificationController` |
| Resume analysis (PDF/DOCX) | HO GAYA | `ResumeController`, `ResumeAnalyzer` |
| Enhanced resume (purana text + spelling + skills) | HO GAYA | `resume-builder.js` |
| Recruiter: job post, applicants, shortlist/hire | HO GAYA | backend `RecruiterController`, screen `recruiter.html` (NAYA) |
| Privacy: export data, delete account | HO GAYA | `PrivacyController` |
| **Password reset (forgot password)** | HO GAYA (NAYA) | `AuthController`, `TokenService`, `forgot-password.html`, `reset-password.html` |
| **Email verification** | HO GAYA (NAYA) | `verify-email.html`; apply ke liye zaroori karna ho to `REQUIRE_VERIFIED_EMAIL=true` |
| **Job search + filters** | HO GAYA (NAYA) | `JobSearch`, `GET /api/jobs?q=java pune&location=Pune&location=Delhi&remote=true&maxRank=60&sort=...&page=0` |
| **Job Corner screen** (search bar, multi-city chips, track dropdown, sort, "jobs I qualify for", Previous/Next) | HO GAYA (NAYA) | `jobs.html`, `jobs.css` |
| **Saved jobs** | HO GAYA (NAYA) | `SavedJobController`, `/api/saved-jobs` |
| **Why not selected feedback** (job close hone par rank gap + missing skills) | HO GAYA (NAYA) | `RecruiterController.feedbackFor` |
| **API rate limit** (300 requests/minute/IP, phir 429) | HO GAYA (NAYA) | `RateLimitFilter` |
| **10 IT tracks** (jobs ke liye) | HO GAYA (NAYA) | `tracks.js` |
| **Pagination** | HO GAYA (NAYA) | jobs aur notifications me, `X-Total-Count` header |
| **AI Lab: Code Review Coach** (rules + optional live AI, history, hourly limit) | HO GAYA (NAYA) | `AiLabController`, `CodeReviewService`, `AiClient`, `codereview.html`. Poori guide: `AI_LAB_GUIDE.md` |
| **AI Lab: Scam Detector aur Page Auditor** | HO GAYA (NAYA) | `ScamDetectorService`, `PageAuditService`, `scamcheck.html`, `pageaudit.html`. Guide: `AI_LAB_GUIDE.md` |
| MySQL support | HO GAYA | `application-mysql.properties` (neeche steps) |
| Judge0 (server par code run) | BAAKI | Judge0 ka alag server chahiye + coding problems ke hidden tests frontend se server me le jaane padenge. Ye alag bada kaam hai |
| AI resume rewrite | BAAKI | Paid LLM API key chahiye. Free plan me nahi ho sakta |
| Real email bhejna (Gmail etc.) | Setup baaki | Code ready hai. Bina SMTP ke link backend terminal me print hota hai |
| Deployment (online) | BAAKI | Neeche section 6 |
| Password reset / logout ke baad purana login token | BAAKI (jaan-bujhkar) | JWT stateless hai, isliye 24 ghante tak chalta hai. Turant band karne ke liye har request par database/Redis check lagana padta hai. Ye stateless ki keemat hai, interview me bata sakti ho |
| Baaki tracks ke skill test (AI/ML, DevOps, Mobile, QA, Cyber, UI/UX) | BAAKI | Sirf Frontend, Backend, Data ka question bank hai. Baaki ke liye har track ke 10 sawal `questions-bank.json` me likhne honge |
| Verified email na ho to screen par "Resend email" button | BAAKI (chhota) | Backend `POST /api/auth/resend-verification` ready hai, bas button lagana hai |

---

## 2. VS Code me abhi kya karna hai (step by step)

1. Zip extract karo.
2. **Backend files copy:** zip ke `backend` folder ka `src` aur `pom.xml`, `README.md`, `BACKEND_GUIDE.md` apne `applyrank-backend\applyrank-backend` folder me purani files ke upar paste karo (Replace).
3. **Frontend files copy:** zip ke `frontend` folder ki saari files apne frontend folder me paste karo (Replace).
4. Backend terminal kholo aur ye 2 lines chalao:
   ```
   $env:Path += ";C:\Users\Asus\Downloads\apache-maven-3.10.0-bin (1)\apache-maven-3.10.0\bin"
   cd C:\Users\Asus\Downloads\applyrank-backend\applyrank-backend
   ```
5. Pehle test chalao (naya `spring-boot-starter-mail` download hoga, thoda time lagega):
   ```
   mvn test
   ```
   `BUILD SUCCESS` aana chahiye. Error aaye to poora error mujhe bhej dena.
6. Backend chalao:
   ```
   mvn spring-boot:run
   ```
   "Started ApplyRankApplication" aane tak wait karo. Terminal band mat karna.
7. Frontend: `index.html` par right-click, "Open with Live Server".
8. Forgot password test:
   - Login page par "Forgot?" dabao, email likho, "Send reset link".
   - **Backend terminal me** `DEV MODE ... open this link: http://127.0.0.1:5500/reset-password.html?token=...` line aayegi. Us link ko browser me kholo.
   - Naya password do. Phir login karo.
9. Recruiter test: login me `recruiter@applyrank.dev` / `Recruiter@123` dalo. Recruiter desk khulega. Job post karo. Candidate (demo@applyrank.dev) se job corner me wo job dikhegi.

---

## 3. Tumhare project ke backend concepts (Zone A) aur unka code

| Concept | Matlab (simple) | Tumhare project me kahan |
|---|---|---|
| REST API + Controller | Frontend URL par request bhejta hai, Java method jawab deta hai | `web/*Controller.java` |
| Spring Boot | Tomcat server + sab setup ek saath | `ApplyRankApplication`, `application.properties` |
| Dependency Injection | Spring khud objects banake constructor me deta hai | Har controller ka constructor |
| JPA / Hibernate (ORM) | Java class = table, object = row, SQL khud nahi likhna | `model/*.java`, `repo/*.java` |
| Entity relations | `@ManyToOne` (job -> recruiter) | `Job.postedBy`, `JobApplication` |
| Bean Validation | Galat input pehle hi rok do | `@Valid`, `@NotBlank`, `@Size` in `Dtos` |
| DTO (record) | Database object seedha bahar nahi dete, sirf zaroori fields dete hain | `dto/Dtos.java`, `Mapper` |
| JWT (stateless auth) | Server session yaad nahi rakhta, har request me signed token aata hai | `JwtService`, `JwtAuthFilter` |
| BCrypt | Password ka one-way hash (wapas asli password nahi nikal sakte) | `SecurityConfig` |
| Role based access | RECRUITER hi `/api/recruiter/**` khol sakta hai | `SecurityConfig` |
| CORS | Browser ko batana ki port 5500 wala frontend port 8080 se baat kar sakta hai | `SecurityConfig.corsConfigurationSource` |
| Multipart file upload | PDF/DOCX binary file server tak lana | `ResumeController` |
| Global error handling | Har error ek jaisa JSON `{error,message}` | `ApiExceptionHandler` |
| Brute-force protection | Galat password ki limit | `LoginAttemptService` |
| One-time token | Reset link sirf ek baar chalta hai, hash save hota hai | `TokenService` |
| Indexing | Table me jaldi dhundhne ka B-tree | `@Index` in `model/*.java` |
| N+1 query fix | 1 query me sab laana, 101 nahi | `@EntityGraph` in `repo/*` |
| `@Transactional` | Multi-step kaam ya to poora ho ya poora cancel (ACID) | `RecruiterController`, `PrivacyController` |
| Optimistic locking | Do log ek saath badlen to ek ko rok do | `TestSession` (`@Version`) |
| Pagination | Ek baar me 10 rows, saari nahi | `JobController`, `NotificationController` |
| Concurrency basics | Kai users ek saath, isliye thread-safe map | `ConcurrentHashMap` in `LoginAttemptService` |
| Unit/Integration tests | Code apne aap check hota hai | `src/test/java` |
| Docker + CI | Kahin bhi same tarah chalna, push par auto test | `Dockerfile`, `docker-compose.yml`, `.github/workflows` |
| API docs | Swagger se saare endpoints dikhte hain | `/swagger-ui.html` |

Tumhari list me `@CrossOrigin` likha tha. Hum alag se har controller par nahi lagate, ek jagah (`SecurityConfig`) se poore project ke liye CORS set hai. Ye behtar tareeka hai.

---

## 4. Jo concepts project me NAHI hain (Zone B), par interview me poochhe jate hain

| Concept | Simple matlab | Tumhare project ko zarurat? |
|---|---|---|
| **Redis cache** | Bar bar wahi data database se na maangke RAM me rakhna | Abhi nahi. Jobs list pe baad me laga sakte ho. Interview me bolo: "traffic badhne par jobs list cache karunga" |
| **Microservices** | App ko chhote alag servers me todna (login alag, resume alag) | Nahi. Is size par ek monolith sahi hai. Microservices extra complexity laate hain |
| **Kafka / RabbitMQ** | Message queue: kaam queue me daalo, background me ho | Abhi nahi. Example: apply karte hi email/notification background me |
| **`@Async` / thread pool** | Kaam alag thread me, user ko wait nahi | Chhota sa email bhejne me laga sakte ho. Tomcat ka thread pool already chalta hai |
| **Spring Cloud, Eureka, API Gateway, Feign** | Microservices ke aapas ke raste | Nahi (microservices ke saath hi aate hain) |
| **Flyway / Liquibase** | Database badlav ka version control | Production me chahiye. Abhi `ddl-auto=update` hai |
| **Refresh tokens / httpOnly cookie** | Token chori hone par zyada safe tareeka | Aage ka security step |
| **Rate limiting (poora API)** | Har endpoint par request limit | Abhi sirf login/forgot par hai |
| **Judge0 / sandbox** | Unknown user code safely chalana | Baaki hai (section 1) |

Interview ke liye ek line yaad rakho: "Maine monolith banaya kyunki team/traffic chhota hai. Bada hone par pehle cache (Redis), phir queue (Kafka), phir zarurat par services alag karunga."

---

## 5. Spring Boot API aur MySQL: kab use hota hai?

**Spring Boot API:** wo already use ho raha hai. Jab tum `mvn spring-boot:run` chalati ho, Spring Boot API (port 8080) chal raha hota hai. Tumhari website uske endpoints (`/api/auth/login`, `/api/jobs`, ...) ko bulati hai.

**MySQL:** abhi data **H2** me jata hai (ek file `./data/applyrank`). H2 seekhne aur testing ke liye accha hai. MySQL tab lagao jab:
- tum Workbench me apni tables dekhna chahti ho (interview demo ke liye accha lagta hai), ya
- online deploy karna ho (real database chahiye).

MySQL se chalane ke steps (Windows):
1. MySQL Community Server install karo (installer me root password yaad rakho). MySQL Workbench bhi install kar lo.
2. Workbench me ye chalao: `CREATE DATABASE applyrank CHARACTER SET utf8mb4;`
3. Backend terminal me (PATH wali line ke baad):
   ```
   $env:DB_PASSWORD = "tumhara_mysql_password"
   mvn spring-boot:run "-Dspring-boot.run.profiles=mysql"
   ```
   **Quotes zaroori hain**, warna PowerShell command tod deta hai.
4. Workbench me `applyrank` database refresh karo. `users`, `jobs`, `applications` jaisi tables dikhengi.

Pehli baar MySQL naya khali database hota hai, isliye demo users aur jobs apne aap dubara ban jayenge.

---

## 6. Online deploy (jab tayyar ho)

1. Backend `Dockerfile` se kisi host par chalao (Render, Railway, Fly.io jaisi service; unka free plan abhi kya hai wo unki site par dekho).
2. Ek MySQL database (host ka ya alag) lo. Environment variables set karo:
   - `SPRING_PROFILES_ACTIVE=mysql`
   - `DB_URL`, `DB_USER`, `DB_PASSWORD`
   - `JWT_SECRET` = lambi random string (32+ characters). **Zaroori.**
   - `CORS_ORIGINS` = tumhari frontend site ka URL
   - `FRONTEND_URL` = wahi frontend URL (email ke link ke liye)
   - `REQUIRE_VERIFIED_EMAIL=true`
   - Gmail ya kisi SMTP ki `spring.mail.*` settings (real email ke liye; `application.properties` me example likha hai)
3. Frontend (Netlify / GitHub Pages) par daalo. `api.js` ki pehli line me `http://localhost:8080/api` ki jagah apne backend ka URL likho.
4. Online jaane se pehle H2 console band rakho (MySQL profile me wo band hai).
