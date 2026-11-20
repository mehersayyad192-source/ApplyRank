/* ApplyRank resume builder: turns the data in the user's PROFILE into a clean, ATS-friendly, single-column resume
   (opens in a new tab -> "Print / Save as PDF"). It does NOT invent anything: it only uses what the user typed,
   and it lists suggestions (missing keywords, lines without numbers) separately, outside the printed resume. */
(function () {
  function esc(s) { return String(s == null ? '' : s).replace(/[&<>"']/g, function (c) { return { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]; }); }
  function url(u) { u = String(u || '').trim(); if (!u) return ''; if (!/^https?:\/\//i.test(u)) u = 'https://' + u; return /^https?:\/\//i.test(u) ? u : ''; }
  function loadProfile() { try { return JSON.parse(localStorage.getItem('applyrank_profile') || '{}'); } catch (e) { return {}; } }

  /* "built a login page. used react" -> ["Built a login page", "Used react"] (max 5 bullets) */
  function bullets(text) {
    return String(text || '').split(/\r?\n|•|;|(?<=[.!?])\s+/).map(function (s) { return s.replace(/^[\s\-*•]+/, '').replace(/[.\s]+$/, '').trim(); })
      .filter(function (s) { return s.length > 2; }).slice(0, 5).map(function (s) { return s.charAt(0).toUpperCase() + s.slice(1); });
  }
  function li(b, hints) {
    var hasNum = /\d/.test(b);
    if (!hasNum) hints.count++;
    return '<li' + (hasNum ? '' : ' class="nonum"') + '>' + esc(b) + '</li>';
  }
  function verifiedLine() {
    try {
      var r = JSON.parse(localStorage.getItem('applyrank_last_result') || 'null');
      if (r && r.verified === true && r.rank) {
        var track = String(r.domain || '').replace(/^./, function (c) { return c.toUpperCase(); });
        return '<div class="vr">ApplyRank verified rank: ' + esc(r.rank) + ' &middot; ' + esc(r.total) + '/100' + (track ? ' (' + esc(track) + ')' : '') + '</div>';
      }
    } catch (e) {}
    return '';
  }
  function csv(s) { return String(s || '').split(',').map(function (x) { return x.trim(); }).filter(Boolean); }

  function build(p, analysis) {
    p = p || {}; var hints = { count: 0 }, warn = [];
    var projects = Array.isArray(p.projects) ? p.projects : [], certs = Array.isArray(p.certs) ? p.certs : [];
    var skills = csv(p.skills);
    if (!p.name) warn.push('Add your name in your profile.');
    if (!skills.length) warn.push('Add your skills in your profile.');
    if (!projects.length) warn.push('Add at least one project in your profile.');
    if (!p.degree && !p.university) warn.push('Add your education in your profile.');
    if (!p.github) warn.push('Add your GitHub link in your profile.');

    var contact = [p.email, p.phone, p.location].filter(Boolean).map(esc);
    [['GitHub', p.github], ['LinkedIn', p.linkedin]].forEach(function (x) { var u = url(x[1]); if (u) contact.push('<a href="' + esc(u) + '">' + x[0] + ': ' + esc(String(x[1]).replace(/^https?:\/\//i, '')) + '</a>'); });

    var h = '';
    if (p.about) h += '<h2>Summary</h2><p>' + esc(p.about) + '</p>';
    if (skills.length) h += '<h2>Skills</h2><p>' + skills.map(esc).join(' &middot; ') + '</p>';
    if (p.status === 'experienced' && (p.company || p.jobRole || p.role)) {
      var role = p.jobRole || p.role || '';
      h += '<h2>Experience</h2><div class="row"><b>' + esc(role) + (p.company ? ', ' + esc(p.company) : '') + '</b>' + (p.expYears ? '<span>' + esc(p.expYears) + ' yr</span>' : '') + '</div><ul>' +
        bullets(p.expDesc).map(function (b) { return li(b, hints); }).join('') + '</ul>';
    }
    if (projects.length) {
      h += '<h2>Projects</h2>' + projects.map(function (it) {
        var links = [['Repo', it.repo], ['Demo', it.demo]].map(function (x) { var u = url(x[1]); return u ? '<a href="' + esc(u) + '">' + x[0] + '</a>' : ''; }).filter(Boolean).join(' | ');
        return '<div class="row"><b>' + esc(it.name || it.title || 'Project') + '</b><span>' + links + '</span></div>' +
          (it.tech ? '<div class="tech">' + csv(it.tech).map(esc).join(', ') + '</div>' : '') +
          '<ul>' + bullets(it.desc).map(function (b) { return li(b, hints); }).join('') + '</ul>';
      }).join('');
    }
    if (p.degree || p.university) {
      h += '<h2>Education</h2><div class="row"><b>' + esc(p.degree || '') + '</b><span>' + esc(p.gradYear || '') + '</span></div><div>' +
        esc(p.university || '') + (p.cgpa ? ' &middot; CGPA ' + esc(p.cgpa) : '') + '</div>';
    }
    if (certs.length) {
      h += '<h2>Certifications</h2><ul>' + certs.map(function (c) { return '<li>' + esc(c.name || c.title || '') + (c.issuer ? ', ' + esc(c.issuer) : '') + (c.year ? ' (' + esc(c.year) + ')' : '') + '</li>'; }).join('') + '</ul>';
    }

    if (hints.count) warn.push(hints.count + ' line(s) have no numbers (yellow on screen). Add a real number where you can, e.g. "reduced load time by 40%".');
    var missing = analysis && analysis.missing && analysis.missing.length ? analysis.missing : [];
    var tipBox = '<div class="tips"><b>Before you send this</b><ul>' + '<li class="must">To save: press <b>Download PDF</b>. It saves a real PDF with selectable text, so recruiters\' systems and ApplyRank can read it. (If you use Print instead, choose Chrome\'s <b>Save as PDF</b>, never "Microsoft Print to PDF": that one saves a picture without text.)</li>' + + warn.map(function (w) { return '<li>' + esc(w) + '</li>'; }).join('') +
      (missing.length ? '<li>Keywords recruiters look for in your track: <b>' + missing.map(esc).join(', ') + '</b>. Add one <u>only if you really know it</u>.</li>' : '') +
      (!warn.length && !missing.length ? '<li>Looks complete. Check spelling once more.</li>' : '') +
      '</ul><button onclick="arDownloadPdf()">Download PDF</button> <button class="ghost" onclick="window.print()">Print</button> <a href="profile.html">Edit profile</a></div>';

    return shell(p.name || 'Resume', tipBox, '<h1>' + esc(p.name || 'Your Name') + '</h1>' +
      (p.headline ? '<div class="headline">' + esc(p.headline) + '</div>' : '') + '<div class="contact">' + contact.join(' &nbsp;|&nbsp; ') + '</div>' + verifiedLine() + h, '');
  }

  /* If the resume fits on ONE page, I remove the page margin right before printing, so the browser has no room left to print
     its own header and footer (date, page address). The same space is given back as padding. A longer resume keeps the normal margin. */
  var PRINTFIX = 'window.addEventListener("beforeprint",function(){var g=document.querySelector(".page");if(!g||g.scrollHeight>1000)return;' +
    'var s=document.createElement("style");s.id="pm";s.textContent="@page{size:A4;margin:0}.page{padding:16mm 18mm!important}";document.head.appendChild(s)});' +
    'window.addEventListener("afterprint",function(){var s=document.getElementById("pm");if(s)s.remove()});';


  /* ---------------------------------------------------------------------------------------------
     DIRECT PDF DOWNLOAD
     Print > "Save as PDF" is not available on every computer, and "Microsoft Print to PDF" saves a picture without text.
     So this small function writes a real PDF file itself (A4, standard Helvetica font, real selectable text).
     It does not need any library or internet. It runs inside the resume window, so it is turned into text with toString().
     --------------------------------------------------------------------------------------------- */
  var PDFFN = function () {
    // width of every letter (from the official Helvetica font tables), for the ASCII characters 32 to 126, per 1000 units
    var WR = [278,278,355,556,556,889,667,222,333,333,389,584,278,333,278,278,556,556,556,556,556,556,556,556,556,556,278,278,584,584,584,556,1015,667,667,722,722,667,611,778,722,278,500,667,556,833,722,778,667,778,722,667,611,722,667,944,667,667,611,278,278,278,469,556,222,556,556,500,556,556,278,556,556,222,222,500,222,833,556,556,556,556,333,500,278,556,500,722,500,500,500,334,260,334,584];
    var WB = [278,333,474,556,556,889,722,278,333,333,389,584,278,333,278,278,556,556,556,556,556,556,556,556,556,556,333,333,584,584,584,611,975,722,722,722,722,667,611,778,722,278,556,722,611,833,722,778,667,778,722,667,611,722,667,944,667,667,611,333,278,333,584,556,278,556,611,556,611,556,333,611,611,278,278,556,278,889,611,611,611,611,389,556,333,611,556,778,556,556,500,389,280,389,584];
    var FONT = { n: { id: 'F1', w: WR }, b: { id: 'F2', w: WB }, i: { id: 'F3', w: WR } };
    // PDF "WinAnsi" codes for a few common characters that are not plain ASCII
    var SPECIAL = { '•': 149, '–': 150, '—': 151, '‘': 145, '’': 146, '“': 147, '”': 148, '…': 133, ' ': 32, '·': 183, '→': 45, '₹': 82 };
    var EXTRA_W = { 149: 350, 150: 556, 151: 1000, 145: 222, 146: 222, 147: 333, 148: 333, 133: 1000 };

    function code(ch) {
      var c = ch.charCodeAt(0);
      if (c >= 32 && c <= 126) return c;
      if (SPECIAL[ch] !== undefined) return SPECIAL[ch];
      if (c >= 160 && c <= 255) return c;
      return 63;                                       // "?" for anything else (for example Hindi letters)
    }
    function charW(ch, f, size) {
      var c = code(ch);
      var w = (c >= 32 && c <= 126) ? FONT[f].w[c - 32] : (EXTRA_W[c] || 556);
      return w * size / 1000;
    }
    function strW(s, f, size) { var t = 0, i; for (i = 0; i < s.length; i++) t += charW(s.charAt(i), f, size); return t; }
    // turn text into a safe PDF string: escape ( ) \ and write every non-ASCII code as an octal number
    function pdfStr(s) {
      var o = '', i, c;
      for (i = 0; i < s.length; i++) {
        c = code(s.charAt(i));
        if (c === 40 || c === 41 || c === 92) o += '\\' + String.fromCharCode(c);
        else if (c < 32 || c > 126) o += '\\' + ('00' + c.toString(8)).slice(-3);
        else o += String.fromCharCode(c);
      }
      return o;
    }
    // cut a text into lines that fit into maxW
    function wrap(text, f, size, maxW) {
      var words = String(text).split(/\s+/).filter(Boolean), lines = [], cur = '', w, tryLine, k;
      for (w = 0; w < words.length; w++) {
        tryLine = cur ? cur + ' ' + words[w] : words[w];
        if (strW(tryLine, f, size) <= maxW) { cur = tryLine; continue; }
        if (cur) { lines.push(cur); cur = ''; }
        if (strW(words[w], f, size) <= maxW) { cur = words[w]; continue; }
        // one very long word (a link): cut it by letters
        for (k = 0; k < words[w].length; k++) {
          if (strW(cur + words[w].charAt(k), f, size) > maxW) { lines.push(cur); cur = ''; }
          cur += words[w].charAt(k);
        }
      }
      if (cur) lines.push(cur);
      return lines.length ? lines : [''];
    }

    function make(blocks) {
      var PW = 595.28, PH = 841.89, MX = 52, TOP = 50, BOT = 50, USABLE = PW - 2 * MX;
      var pages = [], ops = [], y = PH - TOP;
      function newPage() { pages.push(ops.join('\n')); ops = []; y = PH - TOP; }
      function need(h) { if (y - h < BOT) newPage(); }
      function put(x, yy, s, f, size, gray) {
        ops.push((gray === undefined ? '0 g' : gray + ' g') + ' BT /' + FONT[f].id + ' ' + size + ' Tf ' + x.toFixed(2) + ' ' + yy.toFixed(2) + ' Td (' + pdfStr(s) + ') Tj ET');
      }
      blocks.forEach(function (b) {
        var size, f, gray, lines, lh, i, indent = 0;
        if (b.k === 'name') {
          size = 21; lh = 26; need(lh); y -= size; put(MX, y, b.t, 'b', size); y -= (lh - size) + 2; return;
        }
        if (b.k === 'head') {
          size = 10.5; need(34); y -= 12; y -= size; put(MX, y, b.t, 'b', size, 0.1);
          y -= 4; ops.push('0.55 G 0.6 w ' + MX + ' ' + y.toFixed(2) + ' m ' + (MX + USABLE).toFixed(2) + ' ' + y.toFixed(2) + ' l S'); y -= 5; return;
        }
        if (b.k === 'title') {
          size = 10.5; lh = 14; need(lh + 4); y -= 5; y -= size;
          var rw = b.r ? strW(b.r, 'n', 9.5) : 0;
          lines = wrap(b.t, 'b', size, USABLE - rw - (rw ? 10 : 0));
          lines.forEach(function (ln, idx) { if (idx) { need(lh); y -= lh; } put(MX, y, ln, 'b', size); });
          if (b.r) put(MX + USABLE - rw, y, b.r, 'n', 9.5, 0.3);
          y -= 2; return;
        }
        if (b.k === 'tech') { size = 9.5; f = 'i'; gray = 0.3; }
        else if (b.k === 'contact') { size = 9; f = 'n'; gray = 0.3; }
        else if (b.k === 'bul') { size = 10; f = 'n'; gray = 0.1; indent = 14; }
        else { size = 10; f = 'n'; gray = 0.1; }
        lh = size * 1.4;
        lines = wrap(b.t, f, size, USABLE - indent);
        for (i = 0; i < lines.length; i++) {
          need(lh); y -= lh;
          if (b.k === 'bul' && i === 0) put(MX + 3, y, '•', 'n', size, gray);
          put(MX + indent, y, lines[i], f, size, gray);
        }
        y -= (b.k === 'bul' ? 1.5 : 2);
      });
      pages.push(ops.join('\n'));

      // put the pages together into a PDF file (objects 1-5 are fixed, then one content + one page object per page)
      var out = '%PDF-1.4\n', offs = [], n;
      function obj(num, body) { offs[num] = out.length; out += num + ' 0 obj\n' + body + '\nendobj\n'; }
      var kids = [], total = 5 + pages.length * 2;
      pages.forEach(function (_, i) { kids.push((6 + i * 2) + ' 0 R'); });
      obj(1, '<< /Type /Catalog /Pages 2 0 R >>');
      obj(2, '<< /Type /Pages /Kids [' + kids.join(' ') + '] /Count ' + pages.length + ' >>');
      obj(3, '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica /Encoding /WinAnsiEncoding >>');
      obj(4, '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold /Encoding /WinAnsiEncoding >>');
      obj(5, '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Oblique /Encoding /WinAnsiEncoding >>');
      pages.forEach(function (content, i) {
        obj(5 + i * 2 + 1, '<< /Type /Page /Parent 2 0 R /MediaBox [0 0 ' + PW + ' ' + PH + '] /Resources << /Font << /F1 3 0 R /F2 4 0 R /F3 5 0 R >> >> /Contents ' + (7 + i * 2) + ' 0 R >>');
        obj(5 + i * 2 + 2, '<< /Length ' + content.length + ' >>\nstream\n' + content + '\nendstream');
      });
      var xref = out.length;
      out += 'xref\n0 ' + (total + 1) + '\n0000000000 65535 f \n';
      for (n = 1; n <= total; n++) out += ('0000000000' + offs[n]).slice(-10) + ' 00000 n \n';
      out += 'trailer\n<< /Size ' + (total + 1) + ' /Root 1 0 R >>\nstartxref\n' + xref + '\n%%EOF';
      return new Blob([out], { type: 'application/pdf' });
    }

    // read the resume that is on the screen (so a keyword the user ticked is included too)
    function collect(pg) {
      var b = [];
      [].forEach.call(pg.children, function (el) {
        var t = (el.textContent || '').replace(/\s+/g, ' ').trim(), c = el.classList, tag = el.tagName, sp, bo;
        if (tag === 'H1') b.push({ k: 'name', t: t });
        else if (tag === 'H2') b.push({ k: 'head', t: t.toUpperCase() });
        else if (tag === 'UL') [].forEach.call(el.children, function (li) { var s = (li.textContent || '').replace(/\s+/g, ' ').trim(); if (s) b.push({ k: 'bul', t: s }); });
        else if (c.contains('contact')) b.push({ k: 'contact', t: t });
        else if (c.contains('row')) { sp = el.querySelector('span'); bo = el.querySelector('b'); b.push({ k: 'title', t: bo ? bo.textContent.trim() : t, r: sp ? sp.textContent.trim() : '' }); }
        else if (c.contains('ttl')) b.push({ k: 'title', t: t, r: '' });
        else if (c.contains('tech')) b.push({ k: 'tech', t: t });
        else if (t) b.push({ k: 'line', t: t });
      });
      return b;
    }
    return { make: make, collect: collect };
  };
  var PDFSCRIPT = 'var arPdf=(' + PDFFN.toString() + ')();' +
    'function arDownloadPdf(){var pg=document.querySelector(".page");if(!pg)return;var bl=arPdf.collect(pg);' +
    'var nm=((bl[0]&&bl[0].k==="name")?bl[0].t:"Resume").replace(/[^A-Za-z0-9]+/g,"_").replace(/^_+|_+$/g,"")||"Resume";' +
    'var a=document.createElement("a");a.href=URL.createObjectURL(arPdf.make(bl));a.download=nm+"_Resume.pdf";document.body.appendChild(a);a.click();' +
    'setTimeout(function(){URL.revokeObjectURL(a.href);a.remove()},1500);}';

  function shell(title, tipBox, pageHtml, script) {
    return '<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>' + esc(title) + ' - Resume</title><link rel="preconnect" href="https://fonts.googleapis.com"><link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@500;700;800&display=swap" rel="stylesheet"><style>' +
      '*{box-sizing:border-box}body{font:14px/1.45 Calibri,Arial,sans-serif;color:#111;margin:0;padding:0 12px 40px;background:radial-gradient(circle at 20% 0%,#1b2145 0,#0b1020 55%) fixed}' +
      '.brand{max-width:794px;margin:20px auto 0;font:800 24px "Plus Jakarta Sans",sans-serif;color:#fff}.brand span{color:#6366f1}.brand small{font:600 13px "Plus Jakarta Sans",sans-serif;color:#10b981;margin-left:10px;letter-spacing:.06em;text-transform:uppercase}' +
      '.page{max-width:794px;margin:16px auto;background:#fff;padding:36px 44px;border-radius:6px;box-shadow:0 14px 50px rgba(99,102,241,.35)}.vr{margin-top:5px;font-size:12.5px;font-weight:700;color:#0a7a55}' +
      'h1{font-size:26px;margin:0}.headline{color:#444;margin:2px 0 6px}.contact{font-size:12.5px;color:#333}.contact a{color:#1a56c4}' +
      'h2{font-size:13px;letter-spacing:.08em;text-transform:uppercase;border-bottom:1.5px solid #6366f1;margin:16px 0 6px;padding-bottom:2px}' +
      'p{margin:0}ul{margin:4px 0 6px 18px;padding:0}li{margin:1px 0}.row{display:flex;justify-content:space-between;gap:12px}.tech{font-size:12.5px;color:#444;font-style:italic}' +
      'a{color:#1a56c4}.nonum{background:#fff6c2}.ttl{font-weight:700;margin-top:8px}' +
      '.tips{max-width:794px;margin:16px auto 0;background:rgba(17,24,52,.92);color:#dfe3f8;border:1px solid rgba(99,102,241,.45);border-left:5px solid #10b981;border-radius:14px;padding:14px 20px;font:500 13.5px/1.5 "Plus Jakarta Sans",sans-serif}.tips b{color:#fff}.tips ul{margin-top:6px}.tips a{color:#9fb0ff}.tips u{text-decoration-color:#10b981}' +
      '.tips .must{background:rgba(245,158,11,.16);border-radius:8px;padding:8px 10px;margin:8px 0;list-style-position:inside}.tips button.ghost{background:transparent;border:1px solid rgba(99,102,241,.7)}.tips button{background:linear-gradient(135deg,#6366f1,#4f46e5);color:#fff;border:0;border-radius:10px;padding:9px 18px;font:700 13px "Plus Jakarta Sans",sans-serif;cursor:pointer;margin:6px 12px 0 0;box-shadow:0 0 18px rgba(99,102,241,.55)}' +
      '@media print{body{background:#fff;padding:0}.tips,.brand{display:none}.page{box-shadow:none;margin:0;padding:0;max-width:none}.nonum{background:none}@page{size:A4;margin:16mm}}' +
      '.add{background:#d9f7e8}.fix{background:#e0e4ff}mark{color:inherit}.tips label{display:inline-block;margin:4px 14px 0 0}.tips code{color:#9fe8c8}.ln{margin:2px 0}.addrow{margin-top:4px}' +
      '@media print{mark,.add,.fix{background:none}}' +
      '</style></head><body><div class="brand">Apply<span>Rank</span><small>Resume builder</small></div>' + tipBox + '<div class="page">' + pageHtml + '</div>' + '<script>' + PRINTFIX + PDFSCRIPT + (script || '') + '<\/script>' + '</body></html>';
  }


  /* ---------- ENHANCED resume: keeps the user's uploaded resume text, fixes known spelling mistakes, adds skills ---------- */
  var FIX = {
    'javascipt':'JavaScript','javscript':'JavaScript','java script':'JavaScript','javascript':'JavaScript','phyton':'Python','pyhton':'Python','python':'Python',
    'sprintboot':'Spring Boot','springboot':'Spring Boot','spring boot':'Spring Boot','mysql':'MySQL','my sql':'MySQL','postgresql':'PostgreSQL','postgres sql':'PostgreSQL',
    'mongodb':'MongoDB','mongo db':'MongoDB','nodejs':'Node.js','node js':'Node.js','react js':'React.js','reactjs':'React.js','typescipt':'TypeScript','typescript':'TypeScript',
    'github':'GitHub','git hub':'GitHub','linkedin':'LinkedIn','html':'HTML','css':'CSS','sql':'SQL','rest api':'REST API','restapi':'REST API','json':'JSON','jwt':'JWT','aws':'AWS','oops':'OOPs',
    'docker':'Docker','kubernetes':'Kubernetes','junit':'JUnit','hibernate':'Hibernate','tailwind':'Tailwind','bootstrap':'Bootstrap',
    'recieve':'receive','experiance':'experience','experiences':'experiences','managment':'management','devloped':'developed','developement':'development','responsibilty':'responsibility',
    'sucessfully':'successfully','succesfully':'successfully','enviroment':'environment','implimented':'implemented','implemeted':'implemented','applicaton':'application','knowlege':'knowledge',
    'langauge':'language','langauges':'languages','databse':'database','algoritm':'algorithm','algoritms':'algorithms','comunication':'communication','achivement':'achievement','oppurtunity':'opportunity',
    'seperate':'separate','definately':'definitely','occured':'occurred','refrence':'reference','projet':'project','intership':'internship','internhip':'internship','desgined':'designed','buit':'built','wokred':'worked','tecnical':'technical','tecnology':'technology','teamwrok':'teamwork'
  };
  var FIXRE = new RegExp('(^|[^A-Za-z0-9])(' + Object.keys(FIX).sort(function (a, b) { return b.length - a.length; }).map(function (k) { return k.replace(/ /g, '\\s+'); }).join('|') + ')(?![A-Za-z0-9])', 'gi');
  var HEAD = /^(summary|objective|career objective|profile|professional summary|skills?|technical skills|key skills|experience|experience\s*\/\s*internships?|work experience|professional experience|employment|internships?|projects?|academic projects?|education|certifications?|achievements|awards|training|courses|languages|languages known|interests|hobbies|personal details|personal information|personal profile|extracurricular activities|references|declaration)\s*:?$/i;

  function fixLine(line, fixes) {
    var keep = [];
    var masked = line.replace(/\S*(\/|@|https?:|www\.)\S*/g, function (m) { keep.push(m); return '\u0001' + (keep.length - 1) + '\u0002'; });
    masked = masked.replace(FIXRE, function (m, pre, w) {
      var to = FIX[w.toLowerCase().replace(/\s+/g, ' ')];
      if (!to) return m;
      if (/^[A-Z]/.test(w) && /^[a-z]/.test(to)) to = to.charAt(0).toUpperCase() + to.slice(1);
      if (to === w) return m;
      fixes.push(w + ' → ' + to);
      return pre + '\u0003' + to + '\u0004';
    });
    return masked.replace(/\u0001(\d+)\u0002/g, function (m, n) { return keep[+n]; });
  }
  function hasSkill(text, k) { return new RegExp('(^|[^a-z0-9+#])' + k.toLowerCase().replace(/[.*+?^${}()|[\]\\]/g, '\\$&') + '($|[^a-z0-9+#])').test(text.toLowerCase()); }
  function lineHtml(l) { return esc(l).replace(/\u0003/g, '<mark class="fix">').replace(/\u0004/g, '</mark>'); }

  function buildEnhanced(text, p, analysis) {
    p = p || {};
    var fixes = [], lines = String(text || '').split(/\r?\n/).map(function (l) { return l.replace(/\s+/g, ' ').trim(); }).filter(Boolean).map(function (l) { return fixLine(l, fixes); });
    var plain = lines.join('\n').replace(/[\u0003\u0004]/g, '');
    // Nice spelling for keywords. This list is only for keywords I SUGGEST. It is not used to change the user's own text
    // (a word like "rest" in a normal sentence must never be changed).
    var KW = { rest: 'REST API', api: 'API', jpa: 'JPA', etl: 'ETL', sql: 'SQL', git: 'Git', ci: 'CI/CD', pandas: 'Pandas', tableau: 'Tableau', excel: 'Excel', statistics: 'Statistics', dashboard: 'Dashboards', analysis: 'Data analysis', responsive: 'Responsive design', accessibility: 'Accessibility', security: 'Security', testing: 'Testing' };
    function canon(k) { return KW[k.toLowerCase()] || FIX[k.toLowerCase()] || (/^[a-z]/.test(k) ? k.charAt(0).toUpperCase() + k.slice(1) : k); }
    var fromProfile = csv(p.skills).map(canon).filter(function (k) { return !hasSkill(plain, k); });
    var missing = ((analysis && analysis.missing) || []).map(canon).filter(function (k) { return !hasSkill(plain, k) && !fromProfile.some(function (x) { return x.toLowerCase() === k.toLowerCase(); }); });

    var out = '', ul = false, nameDone = false, inHead = true, skillsAt = -1, i, l, pendingBullet = false, inBlock = false;
    function close() { if (ul) { out += '</ul>'; ul = false; } }
    var addHtml = '<p class="addrow" id="ar"></p>', addPlaced = false;
    for (i = 0; i < lines.length; i++) {
      l = lines[i];
      var bare = l.replace(/[\u0003\u0004]/g, '');
      if (!nameDone && bare.length < 50 && !/@/.test(bare)) { out += '<h1>' + lineHtml(l) + '</h1>'; nameDone = true; continue; }
      nameDone = true;
      // A bullet sign alone on a line (PDF text often does this): the text of the bullet is on the next line.
      // Before, this made an empty bullet, so I remember it and use it for the next line.
      if (/^[\u2022\u25aa\u25cf\u25e6\u2023\-*\u2013]$/.test(bare)) { pendingBullet = true; continue; }
      if (HEAD.test(bare)) {
        pendingBullet = false; inBlock = false;
        close(); inHead = false;
        if (skillsAt >= 0 && !addPlaced) { out += addHtml; addPlaced = true; }
        if (/skill/i.test(bare)) skillsAt = i;
        out += '<h2>' + lineHtml(l.replace(/:$/, '')) + '</h2>'; continue;
      }
      if (inHead) { close(); out += '<div class="contact">' + lineHtml(l) + '</div>'; continue; }
      var nextBare = (lines[i + 1] || '').replace(/[\u0003\u0004]/g, '');
      // A line is a "title" (project name, job title) when the next line starts with Technologies: or is a date range
      var isTitle = /^(technologies|technology|tech stack|tools)\s*:/i.test(nextBare) ||
        /^(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*\.?\s+\d{4}\s*(-|\u2013|\u2014|to)/i.test(nextBare) || /^\d{4}\s*(-|\u2013|\u2014)\s*(\d{4}|present)/i.test(nextBare);
      var isBullet = pendingBullet || /^[\u2022\u25aa\u25cf\u25e6\u2023\-*\u2013]\s*/.test(bare);
      var isTech = /^(technologies|technology|tech stack|tools)\s*:/i.test(bare);
      if (isTitle) inBlock = false;
      if (isTech || /^(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*\.?\s+\d{4}\s*(-|\u2013|\u2014|to)/i.test(bare)) inBlock = true;
      if (isBullet || (inBlock && !isTech && !isTitle && !/^(jan|feb|mar|apr|may|jun|jul|aug|sep|oct|nov|dec)[a-z]*\.?\s+\d{4}/i.test(bare))) {
        var btxt = l.replace(/^[\u2022\u25aa\u25cf\u25e6\u2023\-*\u2013]\s*/, '');
        pendingBullet = false;
        if (!btxt.replace(/[\u0003\u0004]/g, '').trim()) continue;       // never print an empty bullet
        if (!ul) { out += '<ul>'; ul = true; }
        out += '<li>' + lineHtml(btxt) + '</li>'; continue;
      }
      close(); out += '<div class="ln' + (isTitle ? ' ttl' : isTech ? ' tech' : '') + '">' + lineHtml(l) + '</div>';
    }
    close();
    if (skillsAt >= 0 && !addPlaced) { out += addHtml; addPlaced = true; }
    if (!addPlaced && (fromProfile.length || missing.length)) {
      // no Skills section in the original: put a new one right after the contact block
      var k = out.lastIndexOf('<div class="contact">'), e = k >= 0 ? out.indexOf('</div>', k) + 6 : out.indexOf('</h1>') + 5;
      out = out.slice(0, e) + '<h2>Skills</h2>' + addHtml + out.slice(e);
    }

    var uniq = fixes.filter(function (f, x) { return fixes.indexOf(f) === x; });
    // Things I do NOT change by myself (I never invent facts), but the student should look at
    var weak = ['responsible for', 'worked on', 'helped in', 'helped with', 'hardworking', 'hard working', 'team player', 'good communication', 'passionate', 'reputed company', 'various']
      .filter(function (w) { return plain.toLowerCase().indexOf(w) >= 0; });
    var personal = /date of birth|\bdob\b|marital status|father'?s name/i.test(plain);
    var noNumbers = (plain.match(/\d+\s*%|\d{2,}\+?\s*(users|students|records|tests|apis?|pages|projects)/gi) || []).length === 0;
    var tip = '<div class="tips"><b>Your enhanced resume</b><ul>' +
      '<li class="must">To save: press <b>Download PDF</b>. It saves a real PDF with selectable text, so recruiters\' systems and ApplyRank can read it. (If you use Print instead, choose Chrome\'s <b>Save as PDF</b>, never "Microsoft Print to PDF": that one saves a picture without text.)</li>' +
      '<li>All your original lines are kept as they were. Layout is simplified to one clean ATS-friendly column (columns/tables from the original may merge).</li>' +
      '<li>' + (uniq.length ? 'Spelling fixed (blue on screen): <code>' + uniq.slice(0, 14).map(esc).join(', ') + '</code>' : 'No known spelling mistakes found. Still read it once yourself.') + '</li>' +
      '<li>' + (fromProfile.length ? 'Skills added from your profile (green): <b>' + fromProfile.map(esc).join(', ') + '</b>' : 'Your profile skills are already in the resume (or your profile has none yet).') + '</li>' +
      (missing.length ? '<li>Missing keywords for your track. Tick <u>only the ones you really know</u>:<br>' + missing.map(function (k) { return '<label><input type="checkbox" data-kw="' + esc(k) + '"> ' + esc(k) + '</label>'; }).join('') + '</li>' : '') +
      (weak.length ? '<li>Wording I did <u>not</u> change (only you know the real facts): <code>' + weak.slice(0, 8).map(esc).join(', ') + '</code>. Say what you built and the result instead. Use <a href="resumepolish.html">Resume Polish</a> for help.</li>' : '') +
      (noNumbers ? '<li>No numbers found (users, percent, tests). Add one real number to each project.</li>' : '') +
      (personal ? '<li>Remove date of birth and similar personal details. They are not needed.</li>' : '') +
      '<button onclick="arDownloadPdf()">Download PDF</button> <button class="ghost" onclick="window.print()">Print</button> <a href="profile.html">Edit profile</a></div>';
    var data = JSON.stringify({ base: fromProfile, opt: missing }).replace(/</g, '\\u003c');
    var script = 'var D=' + data + ';function r(){var a=D.base.slice();document.querySelectorAll("input[data-kw]").forEach(function(c){if(c.checked)a.push(c.getAttribute("data-kw"))});' +
      'var e=document.getElementById("ar");if(e)e.innerHTML=a.map(function(x){return"<span class=add>"+x.replace(/[&<>]/g,"")+"</span>"}).join(" &middot; ")}' +
      'document.addEventListener("change",r);r();';
    return shell((text && lines[0] ? lines[0].replace(/[\u0003\u0004]/g, '') : 'Resume') + ' (enhanced)', tip, out + verifiedLine(), script);
  }

  window.ApplyRankResume = {
    buildEnhanced: buildEnhanced,
    openBest: function () {
      var a = window.__lastResumeAnalysis; var t = a && a.text;
      if (!t) { try { var st = JSON.parse(sessionStorage.getItem('applyrank_resume_ctx') || 'null'); if (st) { a = st; t = st.text; } } catch (e) {} }
      var html = t ? buildEnhanced(t, loadProfile(), a) : build(loadProfile(), a);
      var w = window.open(URL.createObjectURL(new Blob([html], { type: 'text/html' })), '_blank');
      if (!w) alert('Your browser blocked the new tab. Please allow pop-ups for this site and click again.');
    },
    build: build,
    open: function (analysis) {
      var html = build(loadProfile(), analysis || window.__lastResumeAnalysis);
      var w = window.open(URL.createObjectURL(new Blob([html], { type: 'text/html' })), '_blank');
      if (!w) alert('Your browser blocked the new tab. Please allow pop-ups for this site and click again.');
    }
  };
})();
