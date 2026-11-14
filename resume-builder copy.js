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
    var tipBox = '<div class="tips"><b>Before you send this</b><ul>' + warn.map(function (w) { return '<li>' + esc(w) + '</li>'; }).join('') +
      (missing.length ? '<li>Keywords recruiters look for in your track: <b>' + missing.map(esc).join(', ') + '</b>. Add one <u>only if you really know it</u>.</li>' : '') +
      (!warn.length && !missing.length ? '<li>Looks complete. Check spelling once more.</li>' : '') +
      '</ul><button onclick="window.print()">Print / Save as PDF</button> <a href="profile.html">Edit profile</a></div>';

    return '<!doctype html><html lang="en"><head><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>' + esc(p.name || 'Resume') + ' - Resume</title><style>' +
      '*{box-sizing:border-box}body{font:14px/1.45 Calibri,Arial,sans-serif;color:#111;margin:0;padding:0 12px 40px;background:radial-gradient(circle at 20% 0%,#1b2145 0,#0b1020 55%) fixed}' +
      '.brand{max-width:794px;margin:20px auto 0;font:800 24px "Plus Jakarta Sans",sans-serif;color:#fff}.brand span{color:#6366f1}.brand small{font:600 13px "Plus Jakarta Sans",sans-serif;color:#10b981;margin-left:10px;letter-spacing:.06em;text-transform:uppercase}' +
      '.page{max-width:794px;margin:16px auto;background:#fff;padding:36px 44px;border-radius:6px;box-shadow:0 14px 50px rgba(99,102,241,.35)}.vr{margin-top:5px;font-size:12.5px;font-weight:700;color:#0a7a55}' +
      'h1{font-size:26px;margin:0}.headline{color:#444;margin:2px 0 6px}.contact{font-size:12.5px;color:#333}.contact a{color:#1a56c4}' +
      'h2{font-size:13px;letter-spacing:.08em;text-transform:uppercase;border-bottom:1.5px solid #6366f1;margin:16px 0 6px;padding-bottom:2px}' +
      'p{margin:0}ul{margin:4px 0 6px 18px;padding:0}li{margin:1px 0}.row{display:flex;justify-content:space-between;gap:12px}.tech{font-size:12.5px;color:#444;font-style:italic}' +
      'a{color:#1a56c4}.nonum{background:#fff6c2}' +
      '.tips{max-width:794px;margin:16px auto 0;background:rgba(17,24,52,.92);color:#dfe3f8;border:1px solid rgba(99,102,241,.45);border-left:5px solid #10b981;border-radius:14px;padding:14px 20px;font:500 13.5px/1.5 "Plus Jakarta Sans",sans-serif}.tips b{color:#fff}.tips ul{margin-top:6px}.tips a{color:#9fb0ff}.tips u{text-decoration-color:#10b981}' +
      '.tips button{background:linear-gradient(135deg,#6366f1,#4f46e5);color:#fff;border:0;border-radius:10px;padding:9px 18px;font:700 13px "Plus Jakarta Sans",sans-serif;cursor:pointer;margin:6px 12px 0 0;box-shadow:0 0 18px rgba(99,102,241,.55)}' +
      '@media print{body{background:#fff;padding:0}.tips,.brand{display:none}.page{box-shadow:none;margin:0;padding:0;max-width:none}.nonum{background:none}@page{size:A4;margin:16mm}}' +
      '</style></head><body><div class="brand">Apply<span>Rank</span><small>Resume builder</small></div>' + tipBox + '<div class="page"><h1>' + esc(p.name || 'Your Name') + '</h1>' +
      (p.headline ? '<div class="headline">' + esc(p.headline) + '</div>' : '') + '<div class="contact">' + contact.join(' &nbsp;|&nbsp; ') + '</div>' + verifiedLine() + h + '</div></body></html>';
  }

  window.ApplyRankResume = {
    build: build,
    open: function (analysis) {
      var html = build(loadProfile(), analysis || window.__lastResumeAnalysis);
      var w = window.open(URL.createObjectURL(new Blob([html], { type: 'text/html' })), '_blank');
      if (!w) alert('Your browser blocked the new tab. Please allow pop-ups for this site and click again.');
    }
  };
})();
