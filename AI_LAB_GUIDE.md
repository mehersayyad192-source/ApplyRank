# AI Lab Guide (Hinglish)

AI Lab = ApplyRank ke andar 3 tools ka ek module: **Code Review Coach**, **Page Auditor**, **Scam Detector**.
Ek kahani: *"Fresher ko 3 cheezein check karni hoti hain: apna code, apni portfolio website, aur mili hui job offer."*

---

## 1. Status: kya HO GAYA aur kya BAAKI hai

| Cheez | Status | Kahan hai |
|---|---|---|
| Shared AI engine (Gemini se baat, live streaming) | HO GAYA | `AiClient.java` |
| Code Review Coach: rules se instant score + problems | HO GAYA | `CodeReviewService.java` (Java, JavaScript, Python) |
| Optional AI advice, live likhta hua | HO GAYA | `AiLabController.java` (`POST /api/ailab/code-review`) |
| API key sirf backend me (browser ko kabhi nahi milti) | HO GAYA | `application.properties` -> `GEMINI_API_KEY` |
| Key na ho to bhi tool chalta hai | HO GAYA | sirf rules wala review chalta hai |
| Har user ke 1 ghante me 10 AI checks (limit) | HO GAYA | `app.ai.hourly-quota` |
| Code save nahi hota, sirf score/time/language | HO GAYA | `AiCheck.java` |
| History (apni, private) | HO GAYA | `GET /api/ailab/history` |
| Privacy export + account delete me AI Lab data | HO GAYA | `PrivacyController` |
| User ki "haan" (consent) AI ko code bhejne se pehle | HO GAYA | `codereview.html` checkbox + server check |
| Hub page + navbar me AI LAB tab (sab pages par) | HO GAYA | `ailab.html` |
| Tests | HO GAYA | `CodeReviewServiceTest`, `AiLabApiTest` |
| **Scam Detector**: offer message paste karo, risk number + warning signs | HO GAYA | `ScamDetectorService.java`, `scamcheck.html`, `POST /api/ailab/scam-check` |
| **Page Auditor**: portfolio ka HTML paste karo (Jsoup se padhta hai) | HO GAYA | `PageAuditService.java`, `pageaudit.html`, `POST /api/ailab/page-audit` |
| Teeno tools ek hi engine par (ek shared `runTool`) | HO GAYA | `AiLabController.java` |
| Page Auditor me URL dena (link se page kholna) | JAAN-BUJHKAR NAHI | SSRF security risk. Abhi sirf HTML paste. Kaise karein, section 6 me |
| Real AI ka test | TUMHARA KAAM | Maine yahan sirf nakli (mock) server se test kiya, kyunki yahan se Google ka internet band hai. Tumhari key se pehli baar chalega |

---

## 2. Wireframe

```
HUB (ailab.html)
+-------------------------------------------------------------+
| ApplyRank  HOME  JOB CORNER  SKILL TEST  [AI LAB]  RESUME ..|
+-------------------------------------------------------------+
|  Check it before you send it                                |
|  +---------------+ +---------------+ +-----------------+    |
|  | Code Review   | | Page Auditor  | | Scam Detector   |    |
|  | READY         | | COMING SOON   | | COMING SOON     |    |
|  | [Open]        | | [Not built]   | | [Not built]     |    |
|  +---------------+ +---------------+ +-----------------+    |
|  Recent checks (tumhare account se)                         |
|  Your privacy in AI Lab                                     |
+-------------------------------------------------------------+

SCAM DETECTOR (scamcheck.html)  - same layout as Code Review
| 1. The offer message [box]  [ ] AI advice  [Check this offer]|
| 2. Result: RISK circle (red = bad!)  "Very risky..."        |
| Warning signs I found (fees, OTP, WhatsApp, no interview...) |
| How to verify any offer yourself (4 steps)                   |

PAGE AUDITOR (pageaudit.html)  - same layout as Code Review
| 1. Your page HTML [box]    [ ] AI advice  [Audit my page]    |
| 2. Result: score circle   findings (title, viewport, alt...) |
| Why paste HTML and not a link? (SSRF note)                   |

CODE REVIEW (codereview.html)
+-------------------------------------------------------------+
| 1. Your code   [ big code box ]       [Try an example]      |
|    [ ] Also get friendly AI advice                          |
|        [ ] I agree to send this code to Gemini (consent)    |
|    [Analyze my code]   7 of 10 AI checks left this hour     |
| 2. Result      (score circle 23/100)  java | 12 lines | 5   |
| What I found   HIGH  Secret in code        line 3           |
|                HIGH  SQL injection risk    line 7           |
| AI coach       live text likhta hua ...|                    |
| Recent checks                                               |
+-------------------------------------------------------------+
```

## 3. Ek check me kya hota hai (flow)

```
Browser (codereview.html)
   | POST /api/ailab/code-review  (JWT token + code + useAi + consent)
   v
RateLimitFilter -> JwtAuthFilter -> AiLabController
   1. Check: code khali? bada? consent hai? 1 ghante ki limit?   (galti ho to normal JSON error)
   2. CodeReviewService: rules se score + findings               (turant, bina AI)
   3. Stream shuru:  event "rules"  -> browser score dikhata hai
   4. Agar AI chahiye: AiClient -> Gemini -> chhote chhote tukde
                       event "ai" -> browser text ek ek tukda jodta hai
   5. AiCheck save (sirf score/time/language, code NAHI)
   6. event "done"
```

Stream ka format (SSE): har event = `event: naam` + `data: {json}` + khali line.

---

## 4. Tumhe VS Code me kya karna hai (step by step)

### Step 1: Files copy karo
Zip kholo. Dono folder ki files apne project me **same naam se replace/add** karo:
- `frontend/` ki files -> tumhara website folder (jahan `dashboard.html` hai)
- `backend/` ki files -> tumhara backend folder (jahan `pom.xml` hai). `src` folder apne aap sahi jagah jayega.

### Step 2: Test chalao (AI key ke bina)
Naya terminal (PowerShell):
```
$env:Path += ";C:\Users\Asus\Downloads\apache-maven-3.10.0-bin (1)\apache-maven-3.10.0\bin"
cd C:\Users\Asus\Downloads\applyrank-backend\applyrank-backend
mvn test
```
Aakhir me `BUILD SUCCESS` chahiye. Agar `FAIL` ya `ERROR` aaye to **poora lal text mujhe bhej do**.

### Step 3: Backend chalao (abhi bina key)
```
mvn spring-boot:run
```
Website (Live Server) kholo -> login -> navbar me **AI LAB** -> **Open Code Review** -> **Try an example** -> **Analyze my code**.
Score 23 ke aas-paas aana chahiye. AI wala checkbox band dikhega ("AI is switched off on this server") - ye theek hai.
Phir **AI LAB** hub se **Scam Detector** (Try an example -> risk 100 ke paas) aur **Page Auditor** (Try an example -> score bahut kam) bhi try karo.

### Step 4: AI chalu karne ke liye Gemini API key banao (free)
1. Browser me `aistudio.google.com` kholo, apne Google account se login karo.
2. **Get API key** (ya "API keys") wale button par jao -> **Create API key**.
3. Key copy karo. **Ye password jaisi hai: kisi ko mat do, GitHub par mat daalo, chat me mat bhejo.**
4. Backend wale terminal me pehle band karo (`Ctrl + C`), phir:
```
$env:GEMINI_API_KEY="yahan apni key paste karo"
mvn spring-boot:run
```
(Ye key sirf isi terminal me rehti hai. Naya terminal khologi to dobara likhni padegi. Isiliye ye file me nahi likhte.)

**Key ke baare me zaroori baatein:**
- Key kisi file me paste mat karo (`application.properties` me bhi nahi), GitHub par mat daalo, chat me mat bhejo. Sirf terminal me `$env:GEMINI_API_KEY=...`.
- Agar galti se kahin dikh gayi, AI Studio me jaake us key ko **delete** karo aur nayi banao. Purani key bekaar ho jati hai.
- Key bina bhi poora AI Lab chalta hai (rules wala hissa). Key sirf "friendly AI advice" ke liye hai. Tum isse baad me bhi laga sakti ho.

Menu ke naam Google thoda badal sakta hai. Agar button alag naam ka ho to screenshot bhej do.

Free tier ke baare me: Google ne abhi (Nov 2026) model `gemini-3.8-flash` ko free list me rakha tha (tumhari key par ye naam nahi chala, `gemini-3.5-flash-lite` chala, isliye ab wahi default hai), par **limits ke numbers publish nahi kiye** (account me login karke dikhte hain), aur free tier me **Google tumhara bheja hua text apne products behtar karne me use kar sakta hai**. Isiliye page par consent checkbox hai aur AI ko code bhejne se pehle poochha jata hai. Model ka naam badalna ho to `$env:GEMINI_MODEL="naya-naam"`.

### Step 5: Ab AI ke saath chalao
Code Review page -> example daalo -> **Also get friendly AI advice** tick -> agreement tick -> **Analyze**. Neeche "AI coach" me text live likhta dikhega.

Agar AI wala hissa error de to page par likha aayega ("The AI part could not finish: ... HTTP 403/429/..."). **Rules wala result phir bhi sahi rahta hai.** HTTP number mujhe bata do.

---

## 5. Interview me kya bol sakti ho (concepts jo yahan use hue)

| Concept | Is project me kahan |
|---|---|
| Streaming (Server-Sent Events) | `AiLabController.send()` + `api.js stream()` |
| API key ko secret rakhna (backend-only, env variable) | `AiClient`, `application.properties` |
| Rate limiting / quota | `RateLimitFilter` (IP) + `hourlyQuota` (user) |
| Prompt injection se bachav | `SYSTEM` instruction: code ko sirf data maano, andar likhe order mat maano; code `<<<CODE ... CODE>>>` ke beech |
| XSS se bachav | AI ka text `textContent` se dikhta hai, HTML ki tarah nahi |
| Graceful fallback | key na ho ya AI fail ho to rules wala result phir bhi aata hai |
| Data minimisation / privacy | code save nahi hota, sirf metadata; export + delete me shamil |
| Consent | AI ko bhejne se pehle checkbox, server bhi check karta hai |
| Validation pehle, stream baad me | galti hone par normal JSON error (400/429), stream ke beech nahi |

## 6. Limits (sach-sach)
- **Scam Detector** sirf warning signs dikhata hai, kabhi "pakka asli/nakli" nahi bol sakta. Naye tarike ke scam ke rules me nahi honge.
- **Page Auditor** sirf HTML padhta hai. Colours, speed, asli phone layout aur JavaScript se bane content ko nahi dekhta. Pasted HTML me link se bane CSS/JS bhi nahi padhe jate.
- Page Auditor ko URL dene ka feature jaan-bujhkar nahi diya: agar server koi bhi link khole to koi use private jagah (jaise server ke andar ke addresses) kholne par majboor kar sakta hai (SSRF). Baad me karna ho to allow-list, private IP block, timeout aur size limit sab chahiye. Ye alag bada kaam hai.
- Page Auditor me AI ko poora HTML nahi jata, sirf chhota summary + problems ki list.
- Rules sirf **common galtiyan** pakadte hain (text pattern se). Ye compiler nahi hai. Kabhi kabhi galat alarm bhi aa sakta hai.
- Language ka andaza lagta hai (Java / JavaScript / Python). HTML/CSS ke rules abhi nahi.
- AI ki salah kabhi galat bhi ho sakti hai. Isliye score rules se aata hai, AI se nahi.
- Teeno tools ka AI limit milake gina jata hai (1 ghante me 10). Hourly limit abhi memory me nahi, database (`ai_checks`) se ginti hoti hai, isliye backend restart par bhi sahi rahti hai.
