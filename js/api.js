/* ApplyRank API client - talks to the Spring Boot backend.
   Fail-soft: if the backend is not running, every call rejects with {offline:true} and the pages
   keep working in demo mode from localStorage. */
(function () {
  // After you deploy the backend (Render), paste its address here. Only this one line needs to change.
  var LIVE_API = 'https://YOUR-APP-NAME.onrender.com/api';
  // On my own computer the site talks to localhost; on the internet it talks to LIVE_API.
  var isLocal = /^(localhost|127\.0\.0\.1|\[::1\])$/.test(location.hostname) || location.protocol === 'file:';
  var BASE = window.APPLYRANK_API || (isLocal ? 'http://localhost:8080/api' : LIVE_API);
  // A free server falls asleep after ~15 minutes and needs up to a minute to wake up, so live requests may wait longer.
  var TIMEOUT = isLocal ? 8000 : 75000;
  var TK = 'applyrank_token';

  /* "Server is waking up" message. If any request is still waiting after 3.5 seconds, a small banner explains why. */
  var waiting = 0, wakeTimers = [];
  function banner() {
    var b = document.getElementById('arWake');
    if (!b) {
      b = document.createElement('div'); b.id = 'arWake'; b.setAttribute('role', 'status');
      b.style.cssText = 'position:fixed;left:50%;bottom:18px;transform:translateX(-50%);z-index:99999;max-width:92vw;' +
        'background:#111a2b;color:#e5e7eb;border:1px solid #6366f1;border-radius:12px;padding:10px 16px;font:600 13px system-ui,sans-serif;' +
        'box-shadow:0 0 24px rgba(99,102,241,.45);display:none;text-align:center';
      b.textContent = 'The server is waking up (free hosting sleeps when nobody uses it). This can take up to a minute. Please wait...';
      (document.body || document.documentElement).appendChild(b);
    }
    return b;
  }
  function waitStart() {                       // call when a request begins; returns a function to call when it ends
    var t = setTimeout(function () { waiting++; banner().style.display = 'block'; t = null; }, 3500);
    return function () { if (t) clearTimeout(t); else { waiting = Math.max(0, waiting - 1); if (!waiting) banner().style.display = 'none'; } t = null; };
  }
  // On the internet, wake the server as soon as any page opens (so it is ready when the user clicks something).
  if (!isLocal && BASE.indexOf('YOUR-APP-NAME') < 0) { try { fetch(BASE + '/health').catch(function () {}); } catch (e) {} }

  function token() { try { return localStorage.getItem(TK) || ''; } catch (e) { return ''; } }
  function setToken(t) { try { t ? localStorage.setItem(TK, t) : localStorage.removeItem(TK); } catch (e) {} }

  function request(method, path, body, isForm, meta) {
    var headers = {};
    if (token()) headers['Authorization'] = 'Bearer ' + token();
    var opt = { method: method, headers: headers };
    if (body !== undefined && body !== null) {
      if (isForm) opt.body = body;
      else { headers['Content-Type'] = 'application/json'; opt.body = JSON.stringify(body); }
    }
    var ctl = ('AbortController' in window) ? new AbortController() : null;
    var timer = ctl ? setTimeout(function () { ctl.abort(); }, TIMEOUT) : null;
    if (ctl) opt.signal = ctl.signal;
    var endWait = waitStart();
    return fetch(BASE + path, opt).then(function (res) {
      endWait();
      if (timer) clearTimeout(timer);
      return res.text().then(function (txt) {
        var data = null; try { data = txt ? JSON.parse(txt) : null; } catch (e) {}
        if (!res.ok) {
          if (res.status === 401 && token() && path.indexOf('/auth/') !== 0) setToken('');   // expired token
          var err = new Error((data && data.message) || ('Request failed (' + res.status + ')'));
          err.status = res.status; err.offline = false; throw err;
        }
        if (meta) return { data: data, total: parseInt(res.headers.get('X-Total-Count') || '0', 10), pages: parseInt(res.headers.get('X-Total-Pages') || '1', 10) };
        return data;
      });
    }, function () {
      endWait();
      if (timer) clearTimeout(timer);
      var err = new Error('Backend is not reachable'); err.offline = true; err.status = 0; throw err;
    });
  }

  /* Streaming helper for the AI Lab.
     A normal fetch waits for the whole answer. Here I read the answer piece by piece while it is still coming,
     so the user sees the text appear live. The server sends "events": a line "event: name", a line "data: {json}",
     then a blank line. (The browser's built-in EventSource cannot send a POST body or a login header, so I read the stream by hand.) */
  function stream(path, body, onEvent) {
    var headers = {};
    // a FormData body (a file upload) must NOT get a Content-Type from me: the browser adds it with the right boundary
    var isForm = (typeof FormData !== 'undefined') && body instanceof FormData;
    if (!isForm) headers['Content-Type'] = 'application/json';
    if (token()) headers['Authorization'] = 'Bearer ' + token();
    var endWait = waitStart();
    return fetch(BASE + path, { method: 'POST', headers: headers, body: isForm ? body : JSON.stringify(body) }).then(function (res) {
      endWait();
      if (!res.ok) {   // mistakes (empty code, too many checks...) come back as normal JSON, before any streaming
        return res.text().then(function (txt) {
          var d = null; try { d = JSON.parse(txt); } catch (e) {}
          if (res.status === 401 && token()) setToken('');
          var err = new Error((d && d.message) || ('Request failed (' + res.status + ')'));
          err.status = res.status; err.offline = false; throw err;
        });
      }
      var reader = res.body.getReader(), decoder = new TextDecoder(), buffer = '';
      function handle(block) {            // one finished event block -> call onEvent(name, data)
        var name = 'message', data = '';
        block.split('\n').forEach(function (line) {
          if (line.indexOf('event:') === 0) name = line.slice(6).trim();
          else if (line.indexOf('data:') === 0) data += line.slice(5).trim();
        });
        if (!data) return;
        try { onEvent(name, JSON.parse(data)); } catch (e) { /* ignore a broken piece */ }
      }
      function pump() {
        return reader.read().then(function (r) {
          if (r.done) { if (buffer.trim()) handle(buffer); return; }
          buffer += decoder.decode(r.value, { stream: true }).replace(/\r\n/g, '\n');
          var blocks = buffer.split('\n\n');   // events are separated by a blank line
          buffer = blocks.pop();                 // the last piece may be unfinished, keep it for the next round
          blocks.forEach(handle);
          return pump();
        });
      }
      return pump();
    }, function () { endWait(); var err = new Error('Backend is not reachable'); err.offline = true; err.status = 0; throw err; });
  }

  function remember(auth) {
    setToken(auth.token);
    try {
      localStorage.setItem('applyrank_email', auth.user.email);
      localStorage.setItem('applyrank_role', auth.user.role);
      var p = JSON.parse(localStorage.getItem('applyrank_profile') || '{}');
      p.name = p.name || auth.user.name; p.email = auth.user.email;
      localStorage.setItem('applyrank_profile', JSON.stringify(p));
    } catch (e) {}
    return auth;
  }

  window.ApplyRankAPI = {
    base: BASE,
    isLoggedIn: function () { return !!token(); },
    /* Log out: first tell the server (it makes this token and every other token of mine invalid), then forget the token here.
       If the server cannot be reached I still log out in this browser. Returns a promise so a page can wait before it redirects. */
    logout: function () {
      var p = token() ? request('POST', '/auth/logout').catch(function () {}) : Promise.resolve();
      setToken('');
      return p;
    },
    ping: function () { return request('GET', '/health'); },
    /* login; if the account does not exist yet, create it (a friendly "continue" flow) */
    loginOrRegister: function (email, password, name) {
      return request('POST', '/auth/login', { email: email, password: password }).then(remember, function (e) {
        if (e.status === 401) {
          return request('POST', '/auth/register', { email: email, password: password, name: name || email.split('@')[0] }).then(remember, function (e2) {
            if (e2.status === 409) { var w = new Error('Wrong password for this email.'); w.status = 401; throw w; }
            throw e2;
          });
        }
        throw e;
      });
    },
    role: function () { try { return localStorage.getItem('applyrank_role') || ''; } catch (e) { return ''; } },
    /* account safety */
    forgotPassword: function (email) { return request('POST', '/auth/forgot-password', { email: email }); },
    resetPassword: function (tokenValue, password) { return request('POST', '/auth/reset-password', { token: tokenValue, password: password }); },
    verifyEmail: function (tokenValue) { return request('POST', '/auth/verify-email', { token: tokenValue }); },
    resendVerification: function () { return request('POST', '/auth/resend-verification'); },
    me: function () { return request('GET', '/auth/me'); },
    /* paging: returns {data, total, pages} */
    /* Job Corner search: {q, track, locations:[..], remote, maxRank, sort, page, size} -> {data, total, pages} */
    searchJobs: function (p) {
      var q = [];
      function add(k, v) { if (v !== undefined && v !== null && v !== '') q.push(k + '=' + encodeURIComponent(v)); }
      add('q', p.q); add('track', p.track);
      (p.locations || []).forEach(function (l) { add('location', l); });
      if (p.remote) add('remote', 'true');
      add('maxRank', p.maxRank); add('sort', p.sort); add('page', p.page || 0); add('size', p.size || 8);
      return request('GET', '/jobs?' + q.join('&'), null, false, true);
    },
    jobLocations: function () { return request('GET', '/jobs/locations'); },
    savedJobs: function () { return request('GET', '/saved-jobs'); },
    saveJob: function (id) { return request('PUT', '/saved-jobs/' + id); },
    unsaveJob: function (id) { return request('DELETE', '/saved-jobs/' + id); },
    jobsPage: function (track, page, size) { return request('GET', '/jobs?page=' + (page || 0) + '&size=' + (size || 10) + (track ? '&track=' + encodeURIComponent(track) : ''), null, false, true); },
    notificationsPage: function (page, size) { return request('GET', '/notifications?page=' + (page || 0) + '&size=' + (size || 20), null, false, true); },
    /* recruiter */
    postJob: function (job) { return request('POST', '/recruiter/jobs', job); },
    myJobs: function () { return request('GET', '/recruiter/jobs'); },
    applicants: function (jobId) { return request('GET', '/recruiter/jobs/' + jobId + '/applicants'); },
    setApplication: function (appId, body) { return request('PUT', '/recruiter/applications/' + appId + '/status', body); },
    getProfile: function () { return request('GET', '/profile'); },
    saveProfile: function (p) { return request('PUT', '/profile', p); },
    jobs: function (track) { return request('GET', '/jobs' + (track ? '?track=' + encodeURIComponent(track) : '')); },
    apply: function (jobId) { return request('POST', '/jobs/' + jobId + '/apply'); },
    applications: function () { return request('GET', '/applications'); },
    notifications: function () { return request('GET', '/notifications'); },
    markRead: function (id) { return request('PUT', '/notifications/' + id + '/read'); },
    markAllRead: function () { return request('PUT', '/notifications/read-all'); },
    interviewScore: function (domain, question, answer) { return request('POST', '/tests/interview-score', { domain: domain, question: question, answer: answer }); },
    questions: function (domain) { return request('GET', '/tests/questions?domain=' + encodeURIComponent(domain)); },
    answerMcq: function (sessionId, questionId, choice) { return request('POST', '/tests/answer', { sessionId: sessionId, questionId: questionId, choice: choice }); },
    submitTest: function (payload) { return request('POST', '/tests/submit', payload); },
    exportMyData: function () { return request('GET', '/privacy/export'); },
    deleteMyAccount: function (password) { return request('DELETE', '/privacy/account', { password: password }).then(function (r) { setToken(''); return r; }); },
    ailabStatus: function () { return request('GET', '/ailab/status'); },
    ailabHistory: function () { return request('GET', '/ailab/history'); },
    codeReview: function (code, useAi, consent, onEvent) { return stream('/ailab/code-review', { code: code, useAi: useAi, consent: consent }, onEvent); },
    scamCheck: function (text, useAi, consent, onEvent) { return stream('/ailab/scam-check', { text: text, useAi: useAi, consent: consent }, onEvent); },
    pageAudit: function (text, useAi, consent, onEvent) { return stream('/ailab/page-audit', { text: text, useAi: useAi, consent: consent }, onEvent); },
    resumePolish: function (text, useAi, consent, onEvent) { return stream('/ailab/resume-polish', { text: text, useAi: useAi, consent: consent }, onEvent); },
    resumePolishFile: function (file, useAi, consent, onEvent) {
      var fd = new FormData(); fd.append('file', file); fd.append('useAi', useAi ? 'true' : 'false'); fd.append('consent', consent ? 'true' : 'false');
      return stream('/ailab/resume-polish/file', fd, onEvent);
    },
    testHistory: function () { return request('GET', '/tests/history'); },
    latestTest: function () { return request('GET', '/tests/latest'); },
    uploadResume: function (file, track) {
      var fd = new FormData(); fd.append('file', file); fd.append('track', track || 'fullstack');
      return request('POST', '/resume', fd, true);
    }
  };
})();
