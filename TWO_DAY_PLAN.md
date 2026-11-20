> **STATUS (Day 2):** Day 1 DONE (token revoke, resend button, 6 tracks, Gemini key tested, MySQL tested). Gmail test is postponed until just before deploy.
> Day 2 DONE in code: AI quota fix, AI Resume Polish, README, .gitignore. LEFT: click-through check, GitHub push, deploy (last).

# 2 Din ka Plan: baaki sab kaam, phir Deploy

Aaj tak: backend + AI Lab ke **57/57 tests green** hain. Ab baaki kaam 2 din me, aur sabse last me deploy.

Har kaam par likha hai **[TUM]** (tumhe karna hai) ya **[MAIN]** (main banaungi).

---

## Sawal: "AI resume paid kyun bola tha, aur API key ka kya hua?"

**API key:** abhi banayi nahi gayi (tumne bataya nahi, aur main khud nahi bana sakti). Ye **free** hai, 10 minute ka kaam hai. Steps neeche Din 1, kaam 1 me.

**"AI resume ke liye paid chahiye" me meri galti:** wahi Gemini key AI resume ke liye bhi chal sakti hai, free me bhi. Asli dikkat paid/free nahi, **privacy** hai:
- Free tier me Google bheja hua text apne products behtar karne me use kar sakta hai.
- Resume me naam, phone, email hota hai.

**Hal (isi se banate hain):** AI ko bhejne se pehle main resume se **phone, email aur links hata dungi**, sirf experience/project ke bullet points jayenge, aur pehle consent checkbox hoga. Phir free key bhi theek hai. Production me bahut log hon to paid tier lena padega, par demo aur interview ke liye free chalega.

---

## DIN 1: backend ko pakka karna

### Kaam 1 [TUM, 10 min]: Gemini API key banao aur asli AI chalao
1. `aistudio.google.com` kholo, Google account se login karo.
2. **Get API key** > **Create API key**. Key copy karo.
3. Backend wala terminal band karo (`Ctrl + C`). Phir:
```
$env:GEMINI_API_KEY="yahan apni key"
mvn spring-boot:run
```
4. Website > AI LAB > Code Review > **Try an example** > **Also get friendly AI advice** tick > agreement tick > **Analyze**. Neeche "AI coach" me text live likhta dikhna chahiye.
5. **Natija mujhe batao:** chala? ya error aaya (HTTP number)? Ye is project ka pehla asli AI test hai.

Key kisi file me, GitHub me ya chat me mat daalna.

### Kaam 2 [TUM, 15 min]: asli email bhejna (password reset / verify)
Abhi links terminal me print hote hain. Asli email ke liye Gmail "App password" chahiye:
1. Google account > **Security** > **2-Step Verification** chalu karo (agar band hai).
2. Phir `myaccount.google.com/apppasswords` kholo, naam do (jaise "applyrank"), **Create**. Ek 16 akshar ka password milega.
3. Terminal me (ek hi baar, har naye terminal me dobara):
```
$env:SPRING_MAIL_HOST="smtp.gmail.com"
$env:SPRING_MAIL_PORT="587"
$env:SPRING_MAIL_USERNAME="tumhara@gmail.com"
$env:SPRING_MAIL_PASSWORD="16-akshar-ka-app-password"
$env:SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH="true"
$env:SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE="true"
$env:APP_MAIL_FROM="tumhara@gmail.com"
mvn spring-boot:run
```
4. Login page par "Forgot password" try karo apne email se. Mail inbox me aani chahiye (spam bhi dekho).
Ye password bhi key jaisa secret hai. Environment variable me hi rakho, `application.properties` me nahi.

### Kaam 3 [TUM, 20-30 min]: MySQL par chalana
Poore steps `BACKEND_GUIDE.md` ke section 6 me hain. Chhota version:
1. MySQL Workbench ya terminal me: `CREATE DATABASE applyrank CHARACTER SET utf8mb4;`
2. Terminal me `$env:DB_PASSWORD="tumhara mysql password"`
3. `mvn spring-boot:run "-Dspring-boot.run.profiles=mysql"` (**quotes zaroori**)
4. Dekho ki tables ban gayi hain aur demo login (`demo@applyrank.dev`) chal raha hai.
Agar error aaye, poora lal text bhej do.

### Kaam 4 [MAIN]: token turant band karna (logout / password reset ke baad)
Abhi token 24 ghante chalta hai, chahe password badal do. Main har user ke saath ek "token version" rakhungi. Password reset ya logout par version badhega aur purana token apne aap bekaar ho jayega. Tests ke saath.

### Kaam 5 [MAIN]: "Resend verification email" button
Backend me endpoint hai, screen par button nahi hai. Main login/profile par button jodungi.

### Kaam 6 [MAIN]: baaki 6 tracks ke skill-test questions
ML, DevOps, Mobile, QA, Cyber, UI/UX. Har track ke **12 sawal** (concept, bug dhundo, output batao, scenario) aur 3 interview sawal. Fullstack ko alag nahi chahiye, wo frontend + backend se bana hai.
**[TUM] ka hissa:** main likh ke dungi, tum thoda padh lena. Agar kisi sawal ka jawab galat lage to bata dena.

**Din 1 ke end me:** `mvn test` dobara, sab green hone chahiye.

---

## DIN 2: nayi cheezein, safai, phir deploy

### Kaam 7 [MAIN]: AI Resume Polish (AI Lab ka 4th tool)
- Resume ke bullet points ko mazboot banata hai ("Responsible for X" se "Built X, cut load time by 30%"), **bina nayi baatein banaye**.
- Pehle rules: kamzor shabd, numbers nahi hain, bahut lamba bullet.
- Phir optional AI advice, live streaming.
- AI ko jaane se pehle phone, email, links hat jate hain. Consent checkbox pehle.

### Kaam 8: Judge0 (code ko server par asli chalake score)
Meri **seedhi salah: ye abhi mat karo.** Wajah:
- Isme code chalane wala alag server chahiye (Docker ya kisi company ki paid/limited service).
- Ye security ka bada khatra hai (koi bhi ulta code bhej sakta hai), isliye ise bahut dhyan se banana padta hai.
- 2 din me baaki sab kaam ke saath ye jaldbaazi me kharab ban sakta hai.

Interview me bolo: *"Abhi tests MCQ + concept based hain. Next version me sandboxed code execution (Judge0) jodna chahti hu."* Ye sachchi roadmap hai. **Final faisla tumhara.**

### Kaam 9 [TUM + MAIN]: poora click-through test
Ek checklist: sign up, verify, login, resume upload, skill test, job apply, recruiter, AI Lab (teeno + resume), password reset, privacy export/delete, phone par dekhna. Main checklist dungi, tum chalaogi, jo toota wo main theek karungi.

### Kaam 10 [MAIN]: README + demo ke liye safai
Project ka README (kya hai, kaise chalaye, screenshots ki jagah, tech stack, security features). Recruiter/interviewer yahi sabse pehle padhta hai.

### Kaam 11 [TUM]: GitHub par push
Secrets (key, passwords) kabhi mat daalna. Main `.gitignore` check karungi.

### Kaam 12: DEPLOY (sabse last)
Backend ke liye hosting, cloud MySQL database, frontend ke liye alag hosting, aur `FRONTEND_URL`, `CORS_ORIGINS`, `GEMINI_API_KEY`, `DB_*` ko settings me daalna. Free hosting ke plans aur shartein aksar badalti hain, isliye us din main **pehle current options dekhungi**, phir step-by-step likhungi.

---

## Samay ka andaza (sach-sach)
| | Tum | Main |
|---|---|---|
| Din 1 | ~1 ghanta (key, email, MySQL) | Kaam 4, 5, 6 |
| Din 2 | click-through + GitHub (~1.5 ghante) | Kaam 7, 9 ke fixes, 10 |
| Deploy | ~2 ghante | guide + config |

Agar kuch atak jaye, deploy se pehle jo kaam adhura rahe, use "next version" bol sakte hain. **Kaam karta hua online project, adhure bade features se zyada achha dikhta hai.**

## Order jo mat badalna
1. Din 1 ke teeno [TUM] kaam (key, email, MySQL), kyunki deploy me yahi sab dobara lagega.
2. Poori testing.
3. Deploy sabse last.
