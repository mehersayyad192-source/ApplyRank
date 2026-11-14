 /* ApplyRank \u2014 silly-robot.js
   Silly the robot, shared across every page.
   Needs: silly-robot.css (+ style.css). No libraries.
   API:
     const bot = SillyBot.create({ parent, size, left, bottom, bubble:'top'|'left', state })
     bot.set('walk'|'wave'|'talk'|'happy'|'watch'|'sit'|'shy'|'idle'[, ms])
     bot.say('text', { ms, speak })      -> typewriter bubble
     bot.walkTo(leftPx, ms)              -> Promise
     SillyBot.dock({ page, tips, hovers, clicks })   -> sits bottom-right and talks
     SillyBot.face(anchorEl)             -> robot face inside a circle (.react('happy'|'sad'|'talk'|'jump'))
     SillyBot.go(url)                    -> page-wipe navigation
   Backend / voice: localStorage 'applyrank_voice' = '1' lets Silly speak out loud.                       */
(function () {
  'use strict';
  let uid = 0;
  let lastBubble = 0;
  const reduce = window.matchMedia && matchMedia('(prefers-reduced-motion: reduce)').matches;

  function svg(id, withBody) {
    const g = (n) => n + id;
    return strip('' +
'<svg class="sb-svg" viewBox="0 0 200 290" xmlns="http://www.w3.org/2000/svg" role="img" aria-label="Silly the robot">' +
'<defs>' +
 '<linearGradient id="' + g('hel') + '" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#eef2ff"/><stop offset=".45" stop-color="#a5b4fc"/><stop offset="1" stop-color="#6366f1"/></linearGradient>' +
 '<linearGradient id="' + g('vis') + '" x1="0" y1="0" x2="0" y2="1"><stop offset="0" stop-color="#2e2a6b"/><stop offset="1" stop-color="#0f172a"/></linearGradient>' +
 '<linearGradient id="' + g('bod') + '" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#e0e7ff"/><stop offset="1" stop-color="#818cf8"/></linearGradient>' +
 '<linearGradient id="' + g('lim') + '" x1="0" y1="0" x2="1" y2="0"><stop offset="0" stop-color="#c7d2fe"/><stop offset="1" stop-color="#6366f1"/></linearGradient>' +
 '<radialGradient id="' + g('iri') + '" cx=".5" cy=".4" r=".6"><stop offset="0" stop-color="#c084fc"/><stop offset=".55" stop-color="#6d28d9"/><stop offset="1" stop-color="#2e1065"/></radialGradient>' +
 '<radialGradient id="' + g('flo') + '"><stop offset="0" stop-color="#fffbe0"/><stop offset="1" stop-color="#fde047" stop-opacity="0"/></radialGradient>' +
 '<filter id="' + g('glw') + '" x="-50%" y="-50%" width="200%" height="200%"><feGaussianBlur stdDeviation="3.2" result="b"/><feMerge><feMergeNode in="b"/><feMergeNode in="SourceGraphic"/></feMerge></filter>' +
 '<path id="' + g('hrt') + '" d="M0 -2 C-3 -9 -13 -6 -9 2 C-7 6 -2 9 0 12 C2 9 7 6 9 2 C13 -6 3 -9 0 -2Z"/>' +
'</defs>' +
'<ellipse cx="100" cy="279" rx="52" ry="7" fill="#6366f1" opacity=".25"/>' +
'<g class="sb-all">' +
 /* legs */
 '<g class="sb-legs">' +
  '<g class="sb-legL"><rect x="74" y="210" width="20" height="40" rx="9" fill="url(#' + g('lim') + ')"/><rect x="64" y="244" width="36" height="20" rx="10" fill="#a5b4fc" stroke="#4f46e5" stroke-opacity=".5"/><rect x="68" y="256" width="28" height="4" rx="2" fill="#34d399" opacity=".8"/></g>' +
  '<g class="sb-legR"><rect x="106" y="210" width="20" height="40" rx="9" fill="url(#' + g('lim') + ')"/><rect x="100" y="244" width="36" height="20" rx="10" fill="#a5b4fc" stroke="#4f46e5" stroke-opacity=".5"/><rect x="104" y="256" width="28" height="4" rx="2" fill="#34d399" opacity=".8"/></g>' +
 '</g>' +
 '<g class="sb-upper">' +
  /* left arm (with watch) */
  '<g class="sb-armL"><rect x="54" y="152" width="20" height="50" rx="10" fill="url(#' + g('lim') + ')"/><circle cx="62" cy="208" r="11" fill="#a5b4fc" stroke="#4f46e5" stroke-opacity=".5"/>' +
   '<circle cx="62" cy="197" r="9" fill="#0f172a" stroke="#34d399" stroke-width="2"/><circle class="sb-watchglow" cx="62" cy="197" r="13" fill="#34d399" opacity=".25" filter="url(#' + g('glw') + ')"/>' +
   '<line x1="62" y1="197" x2="62" y2="191" stroke="#e0e7ff" stroke-width="1.6" stroke-linecap="round"/><line class="sb-minute" x1="62" y1="197" x2="62" y2="190" stroke="#34d399" stroke-width="1.4" stroke-linecap="round"/></g>' +
  /* torso */
  '<rect x="68" y="146" width="64" height="76" rx="26" fill="url(#' + g('bod') + ')" stroke="#6366f1" stroke-opacity=".35"/>' +
  '<rect x="80" y="156" width="40" height="46" rx="16" fill="#1e1b4b" opacity=".18"/>' +
  '<g class="sb-chest" filter="url(#' + g('glw') + ')"><use href="#' + g('hrt') + '" x="100" y="180" fill="#34d399" transform="translate(100 182) scale(1.35) translate(-100 -180)"/></g>' +
  /* right arm + flower */
  '<g class="sb-armR"><rect x="126" y="152" width="20" height="48" rx="10" fill="url(#' + g('lim') + ')"/><circle cx="136" cy="206" r="11" fill="#a5b4fc" stroke="#4f46e5" stroke-opacity=".5"/>' +
   '<path d="M136 206 Q148 180 152 158" stroke="#10b981" stroke-width="3.4" fill="none" stroke-linecap="round"/><path d="M144 188 Q158 180 164 186 Q156 194 144 188Z" fill="#34d399"/><path d="M142 192 Q130 184 128 176 Q138 176 142 192Z" fill="#10b981"/>' +
   '<g transform="translate(153 150)"><circle class="sb-flower-glow" r="20" fill="url(#' + g('flo') + ')"/>' +
   '<g fill="#fffbe0" stroke="#fde68a" stroke-width=".8"><ellipse cx="0" cy="-10" rx="6" ry="8"/><ellipse cx="9.5" cy="-3" rx="6" ry="8" transform="rotate(72 9.5 -3)"/><ellipse cx="6" cy="8.5" rx="6" ry="8" transform="rotate(144 6 8.5)"/><ellipse cx="-6" cy="8.5" rx="6" ry="8" transform="rotate(216 -6 8.5)"/><ellipse cx="-9.5" cy="-3" rx="6" ry="8" transform="rotate(288 -9.5 -3)"/></g>' +
   '<circle r="5" fill="#fde047"/><circle r="2.4" fill="#fff"/></g></g>' +
  /* head */
  '<g class="sb-head">' +
   '<path d="M80 44 Q74 26 66 16" stroke="#94a3b8" stroke-width="3" fill="none" stroke-linecap="round"/><path d="M120 44 Q126 26 134 16" stroke="#94a3b8" stroke-width="3" fill="none" stroke-linecap="round"/>' +
   '<use class="sb-heartA" href="#' + g('hrt') + '" x="66" y="12" fill="#a78bfa" filter="url(#' + g('glw') + ')"/><use class="sb-heartB" href="#' + g('hrt') + '" x="134" y="12" fill="#34d399" filter="url(#' + g('glw') + ')"/>' +
   '<ellipse cx="32" cy="92" rx="14" ry="21" fill="#818cf8" stroke="#4f46e5" stroke-opacity=".5"/><ellipse cx="32" cy="92" rx="7" ry="13" fill="none" stroke="#34d399" stroke-width="2.6" filter="url(#' + g('glw') + ')"/>' +
   '<ellipse cx="168" cy="92" rx="14" ry="21" fill="#818cf8" stroke="#4f46e5" stroke-opacity=".5"/><ellipse cx="168" cy="92" rx="7" ry="13" fill="none" stroke="#34d399" stroke-width="2.6" filter="url(#' + g('glw') + ')"/>' +
   '<ellipse cx="100" cy="84" rx="69" ry="61" fill="url(#' + g('hel') + ')" stroke="#e0e7ff" stroke-opacity=".7" stroke-width="2"/>' +
   '<path d="M52 56 Q70 30 104 28" stroke="#fff" stroke-opacity=".75" stroke-width="5" fill="none" stroke-linecap="round"/><circle cx="120" cy="32" r="3" fill="#fff" opacity=".8"/>' +
   '<rect x="42" y="54" width="116" height="66" rx="32" fill="url(#' + g('vis') + ')" stroke="#a5b4fc" stroke-opacity=".7" stroke-width="2.5"/>' +
   '<g class="sb-eyes-open">' +
    '<g class="sb-look"><g class="sb-eye"><ellipse cx="76" cy="86" rx="17" ry="19" fill="#0b0720" stroke="#c084fc" stroke-width="3" filter="url(#' + g('glw') + ')"/><ellipse cx="76" cy="87" rx="12.5" ry="14.5" fill="url(#' + g('iri') + ')"/><circle cx="70" cy="79" r="4.6" fill="#fff"/><circle cx="82" cy="94" r="2.2" fill="#fff" opacity=".85"/></g>' +
    '<g class="sb-eye"><ellipse cx="124" cy="86" rx="17" ry="19" fill="#0b0720" stroke="#c084fc" stroke-width="3" filter="url(#' + g('glw') + ')"/><ellipse cx="124" cy="87" rx="12.5" ry="14.5" fill="url(#' + g('iri') + ')"/><circle cx="118" cy="79" r="4.6" fill="#fff"/><circle cx="130" cy="94" r="2.2" fill="#fff" opacity=".85"/></g></g>' +
   '</g>' +
   '<g class="sb-eyes-happy" fill="none" stroke="#c4b5fd" stroke-width="5" stroke-linecap="round" filter="url(#' + g('glw') + ')"><path d="M62 92 Q76 72 90 92"/><path d="M110 92 Q124 72 138 92"/></g>' +
   '<g class="sb-eyes-shy" fill="none" stroke="#c4b5fd" stroke-width="4" stroke-linecap="round"><path d="M62 88 Q76 98 90 88"/><path d="M110 88 Q124 98 138 88"/></g>' +
   '<ellipse cx="58" cy="106" rx="8" ry="4.5" fill="#f472b6" opacity=".4"/><ellipse cx="142" cy="106" rx="8" ry="4.5" fill="#f472b6" opacity=".4"/>' +
   '<path class="sb-mouth-smile" d="M89 104 Q100 114 111 104" stroke="#e0e7ff" stroke-width="3.2" fill="none" stroke-linecap="round"/>' +
   '<ellipse class="sb-mouth-open" cx="100" cy="108" rx="8" ry="6.5" fill="#f9a8d4" stroke="#e0e7ff" stroke-width="2"/>' +
  '</g>' +
 '</g>' +
'</g></svg>');
  }
  function strip(h) { return h.replace(/ filter="url\(#[^)]*\)"/g, ''); }

  /* ------------------------------------------------------------------ actor */
  function create(o) {
    o = o || {};
    const id = 'sb' + (++uid) + '_';
    const size = o.size || 170;
    const el = document.createElement('div');
    el.className = 'sb-actor' + (o.inline ? ' sb-inline' : '');
    el.style.width = size + 'px';
    if (!o.inline) { el.style.left = (o.left == null ? 24 : o.left) + 'px'; el.style.bottom = (o.bottom == null ? 18 : o.bottom) + 'px'; }
    el.innerHTML = svg(id) + '<div class="sb-bubble sb-' + (o.bubble || 'top') + '" role="status" aria-live="polite"></div>';
    (o.parent || document.body).appendChild(el);
    const root = el.querySelector('.sb-svg'), bub = el.querySelector('.sb-bubble');
    const STATES = ['walk', 'wave', 'talk', 'happy', 'watch', 'sit', 'shy'];
    let stT, sayT, typeT, cur = 'idle', sitting = false;
    const inst = { el, root, bubble: bub };

    function apply(s) {
      STATES.forEach(x => root.classList.remove('sb-' + x));
      s.split(' ').forEach(x => x && x !== 'idle' && root.classList.add('sb-' + x));
      if (sitting && s.indexOf('walk') < 0 && s.indexOf('sit') < 0) root.classList.add('sb-sit');
    }
    inst.set = function (s, ms) {
      clearTimeout(stT);
      if (s === 'sit') sitting = true; else if (s === 'walk' || s === 'stand') sitting = false;
      cur = s === 'stand' ? 'idle' : s; apply(cur);
      if (ms) stT = setTimeout(() => { cur = sitting ? 'sit' : 'idle'; apply(cur); }, ms);
      return inst;
    };
    inst.sit = () => inst.set('sit');
    inst.stand = () => inst.set('stand');
    inst.say = function (text, opt) {
      opt = opt || {}; clearTimeout(sayT); clearInterval(typeT);
      bub.classList.add('sb-show'); bub.textContent = '';
      const prev = cur; if (!opt.keepState) { const s = sitting ? 'sit talk' : 'talk'; apply(s); }
      let i = 0; const caret = document.createElement('span'); caret.className = 'sb-caret';
      const span = document.createElement('span'); bub.appendChild(span); bub.appendChild(caret);
      const step = reduce ? text.length : 1;
      typeT = setInterval(() => {
        i = Math.min(text.length, i + step); span.textContent = text.slice(0, i);
        if (i >= text.length) { clearInterval(typeT); caret.remove(); if (!opt.keepState) apply(cur === 'talk' ? (sitting ? 'sit' : 'idle') : cur); }
      }, 24);
      if (opt.speak === true) speak(text); else if (opt.speak === 'auto') speakAuto(text);
      lastBubble = Date.now();
      if (opt.ms !== 0) sayT = setTimeout(() => bub.classList.remove('sb-show'), (opt.ms || 4200 + text.length * 45));
      return new Promise(r => setTimeout(r, text.length * 24 + 200));
    };
    inst.hush = () => { clearTimeout(sayT); bub.classList.remove('sb-show'); };
    inst.walkTo = function (left, ms) {
      return new Promise(res => {
        ms = ms || 2600; sitting = false; apply('walk');
        el.style.transition = reduce ? 'none' : 'left ' + ms + 'ms linear';
        requestAnimationFrame(() => { el.style.left = left + 'px'; });
        setTimeout(() => { el.style.transition = ''; apply('idle'); res(); }, reduce ? 0 : ms);
      });
    };
    inst.hearts = function () {
      const r = root.getBoundingClientRect();
      for (let k = 0; k < 6; k++) {
        const h = document.createElement('div'); h.className = 'sb-pop'; h.textContent = ['\ud83d\udc9c', '\ud83d\udc9a', '\u2728', '\ud83d\udc96'][k % 4];
        h.style.left = (r.left + r.width * (.25 + Math.random() * .5)) + 'px'; h.style.top = (r.top + r.height * .25) + 'px'; h.style.animationDelay = (k * 90) + 'ms';
        document.body.appendChild(h); setTimeout(() => h.remove(), 1500);
      }
    };
    root.addEventListener('click', () => {
      inst.hearts(); inst.set(sitting ? 'sit happy' : 'happy', 1600);
      inst.say(QUOTES[Math.floor(Math.random() * QUOTES.length)], { speak: true });
    });
    inst.destroy = () => { el.remove(); };
    if (o.state) inst.set(o.state);
    return inst;
  }

  const QUOTES = [
    'One "no" is an answer for one role, not for your whole career. Keep going! \ud83d\udc9c',
    'AI is trending, but that does not mean there are no jobs. People with verified skills are still in demand! \ud83d\ude80',
    'Learn a little every day. Your verified rank grows one day at a time. \u26a1',
    'Every fresher starts right here. You are not behind, you are just beginning! \ud83c\udf31',
    'Build one project at scale and put it on GitHub. That is what recruiters look at. \ud83d\udc40',
    'I am right here. You code, and I will cheer for you! \ud83e\udd16\ud83d\udc9a'
  ];

  /* ------------------------------------------------------------------ voice (English only, best available cute voice) */
  const PREF = [/aria/i, /jenny/i, /sonia/i, /natasha/i, /ava\b/i, /emma/i, /michelle/i, /libby/i, /samantha/i, /google us english/i, /google uk english female/i, /zira/i, /karen/i, /moira/i, /tessa/i, /serena/i, /female/i];
  const AVOID = /\b(david|mark|guy|george|james|ryan|alex|daniel|fred|male)\b/i;
  let voiceList = [], pending = null;
  function loadVoices() { try { voiceList = (speechSynthesis.getVoices() || []).filter(v => /^en/i.test(v.lang)); } catch (e) { voiceList = []; } return voiceList; }
  if ('speechSynthesis' in window) { loadVoices(); try { speechSynthesis.addEventListener('voiceschanged', function () { loadVoices(); renderVoicePill(); }); } catch (e) {} }
  function score(v) {
    let sc = 0; const n = v.name + ' ' + v.lang;
    if (/natural|online/i.test(n)) sc += 100;
    for (let i = 0; i < PREF.length; i++) if (PREF[i].test(n)) { sc += 60 - i; break; }
    if (AVOID.test(v.name) && !/female/i.test(v.name)) sc -= 80;
    if (/en[-_]US/i.test(v.lang)) sc += 6; else if (/en[-_]GB/i.test(v.lang)) sc += 5; else if (/en[-_]IN/i.test(v.lang)) sc += 4;
    return sc;
  }
  function savedName() { try { return localStorage.getItem('applyrank_voice_name') || ''; } catch (e) { return ''; } }
  function bestVoice() {
    const list = voiceList.length ? voiceList : loadVoices(); if (!list.length) return null;
    const name = savedName(); if (name) { const f = list.find(v => v.name === name); if (f) return f; }
    return list.slice().sort((a, b) => score(b) - score(a))[0];
  }
  function voiceOn() { try { return localStorage.getItem('applyrank_voice') !== '0'; } catch (e) { return true; } }
  function cleanForSpeech(t) {
    return String(t)
      .replace(/[\u{1F000}-\u{1FAFF}\u2600-\u27BF\uFE0F]/gu, '')
      .replace(/ApplyRank/g, 'Apply Rank').replace(/\bATS\b/g, 'A T S').replace(/\bJD\b/g, 'job description').replace(/\bGitHub\b/g, 'Git Hub')
      .replace(/(\d+)\s*\/\s*100/g, '$1 out of 100').replace(/(\d+)%/g, '$1 percent').replace(/\bAI\b/g, 'A I')
      .replace(/[\u201c\u201d"]/g, '').replace(/\s*[\u2014\u2013]\s*/g, ', ').replace(/\u2026/g, '...').replace(/\s+/g, ' ').trim();
  }
  function speakNow(text) {
    if (!('speechSynthesis' in window)) return;
    const clean = cleanForSpeech(text); if (!clean) return;
    try {
      speechSynthesis.cancel();
      const v = bestVoice(), natural = v && /natural|online/i.test(v.name);
      const parts = clean.match(/[^.!?]+[.!?]*/g) || [clean];
      parts.forEach(function (p) {
        p = p.trim(); if (!p) return;
        const u = new SpeechSynthesisUtterance(p);
        u.lang = (v && v.lang) || 'en-US'; if (v) u.voice = v;
        u.rate = natural ? 1 : 0.96; u.pitch = natural ? 1 : 1.12; u.volume = 1;
        u.onerror = function (e) { if (e && e.error === 'not-allowed') pending = text; };
        speechSynthesis.speak(u);
      });
    } catch (e) {}
  }
  function speak(text) { if (voiceOn()) { lastSpoke = Date.now(); speakNow(text); } }
  let lastSpoke = 0, blockedCb = null;
  /* Try to speak without a click. If the browser blocks it, keep the line and say it on the first mouse move / click / key. */
  function speakAuto(text) {
    if (!voiceOn() || !('speechSynthesis' in window)) return;
    lastSpoke = Date.now(); speakNow(text);
    setTimeout(function () {
      var ok = false; try { ok = speechSynthesis.speaking || speechSynthesis.pending; } catch (e) {}
      if (!ok) { pending = text; hintPill(true); if (blockedCb) blockedCb(); }
    }, 700);
  }
  var flushing = false;
  function flushPending() {
    if (!pending || !voiceOn() || flushing) return;
    flushing = true; setTimeout(function () { flushing = false; }, 1200);
    var t = pending; speakNow(t);
    setTimeout(function () { var ok = false; try { ok = speechSynthesis.speaking || speechSynthesis.pending; } catch (e) {} if (ok) { pending = null; hintPill(false); lastSpoke = Date.now(); } else pending = t; }, 600);
  }
  function hintPill(on) { if (pill) pill.classList.toggle('sb-hint', !!on); }
  /* After the visitor has been idle, the next mouse move makes Silly say ONE short line. */
  function idleTalk(tips, sayFn, idleMs) {
    if (!tips || !tips.length) return;
    var last = Date.now(), i = 0; idleMs = idleMs || 18000;
    document.addEventListener('mousemove', function () {
      var now = Date.now(), gap = now - last; last = now;
      if (document.hidden || !voiceOn() || pending) return;
      if (gap > idleMs && now - lastSpoke > 25000) { var t = tips[i++ % tips.length]; if (sayFn) sayFn(t); else speak(t); }
    }, { passive: true });
  }
  function greet(text) { speakAuto(text); }
  function setVoice(on) { try { localStorage.setItem('applyrank_voice', on ? '1' : '0'); } catch (e) {} if (!on && 'speechSynthesis' in window) speechSynthesis.cancel(); renderVoicePill(); }
  function setVoiceName(n) { try { localStorage.setItem('applyrank_voice_name', n); } catch (e) {} }
  // Browsers block speech until the visitor interacts: remember the line and say it on the first click/key.
  ['mousemove', 'pointerdown', 'keydown', 'touchstart', 'click'].forEach(function (ev) {
    document.addEventListener(ev, flushPending, { passive: true });
  });

  /* small voice control (bottom-left): on/off + choose an English voice */
  let pill;
  function renderVoicePill() {
    if (!pill) return;
    const on = voiceOn(), list = voiceList, cur = bestVoice();
    pill.innerHTML = '<span class="sb-vhint">Click anywhere to hear Silly</span><button type="button" class="sb-vbtn" aria-label="Turn Silly voice ' + (on ? 'off' : 'on') + '" title="Silly voice">' + (on ? '\ud83d\udd0a' : '\ud83d\udd07') + '</button>' +
      (on && list.length > 1 ? '<select class="sb-vsel" aria-label="Choose Silly voice">' + list.map(function (v) { return '<option value="' + v.name.replace(/"/g, '') + '"' + (cur && v.name === cur.name ? ' selected' : '') + '>' + v.name.replace(/^(Microsoft|Google)\s+/i, '').slice(0, 26) + '</option>'; }).join('') + '</select>' : '');
    pill.querySelector('.sb-vbtn').onclick = function () { setVoice(!voiceOn()); if (voiceOn()) { pending = null; hintPill(false); speakNow('Hi, I am Silly. This is my voice.'); } };
    const sel = pill.querySelector('.sb-vsel');
    if (sel) sel.onchange = function () { setVoiceName(sel.value); speakNow('Hi, I am Silly. How does this voice sound?'); };
  }
  function mountVoicePill() {
    if (pill || !('speechSynthesis' in window) || document.getElementById('sbIntro')) return;
    pill = document.createElement('div'); pill.className = 'sb-voice'; document.body.appendChild(pill); renderVoicePill();
  }

  /* ------------------------------------------------------------------ dock (sits bottom-right on a page) */
  function dock(o) {
    o = o || {};
    const small = window.innerWidth < 640;
    const size = small ? 110 : (o.size || 150);
    const bot = create({ size, left: window.innerWidth - size - (small ? 12 : 26), bottom: small ? 8 : 16, bubble: 'left', state: 'sit' });
    window.addEventListener('resize', () => { bot.el.style.left = (window.innerWidth - size - 20) + 'px'; });
    const tips = o.tips || [];
    let ti = 0, tt;
    function nextTip() { if (!tips.length || document.hidden || Date.now() - lastBubble < 20000) return; bot.say(tips[ti++ % tips.length]); }
    var pageKey = 'sb_greeted_' + (location.pathname.split('/').pop() || 'index'), first = true;
    try { first = !sessionStorage.getItem(pageKey); sessionStorage.setItem(pageKey, '1'); } catch (e) {}
    function arrive() {
      bot.el.style.left = '-200px';
      bot.walkTo(window.innerWidth - size - (small ? 12 : 26), 1800).then(() => { bot.set('wave', 1400); setTimeout(() => { bot.sit(); if (o.greet) bot.say(o.greet, { speak: 'auto' }); }, 1300); });
    }
    if (!reduce) arrive(); else if (o.greet) bot.say(o.greet, { speak: 'auto' });
    blockedCb = function () { bot.say('Click anywhere and I will say hello \ud83d\udd0a', { ms: 4000 }); };
    idleTalk(tips, function (t) { bot.say(t, { speak: true, ms: 5000 }); });
    const hov = o.hovers || {};
    Object.keys(hov).forEach(sel => {
      document.addEventListener('mouseover', e => {
        const t = e.target.closest && e.target.closest(sel);
        if (t && t !== hov['_last_' + sel]) { hov['_last_' + sel] = t; if (Date.now() - lastBubble > 6000) bot.say(hov[sel], { ms: 3000 }); }
        else if (!t) hov['_last_' + sel] = null;
      });
    });
    const clk = o.clicks || {};
    Object.keys(clk).forEach(sel => {
      document.addEventListener('click', e => { if (e.target.closest && e.target.closest(sel)) { bot.set('sit happy', 1600); bot.say(clk[sel], { ms: 3000 }); } }, true);
    });
    bot.nextTip = nextTip;
    return bot;
  }

  /* ------------------------------------------------------------------ face inside a circle */
  function face(anchor, opt) {
    opt = opt || {};
    if (!anchor) return { react: function () {} };
    const id = 'sbf' + (++uid) + '_';
    anchor.innerHTML = '<div class="sb-face-wrap" style="width:100%;height:100%;overflow:hidden;border-radius:50%">' + svg(id).replace('class="sb-svg"', 'class="sb-svg sb-face-svg" style="width:134%;margin-left:-17%;margin-top:-7%;filter:none;pointer-events:none"') + '</div>';
    const root = anchor.querySelector('.sb-svg'); let t;
    function set(c, ms) { ['talk', 'happy', 'shy'].forEach(x => root.classList.remove('sb-' + x)); if (c) root.classList.add('sb-' + c); clearTimeout(t); if (ms) t = setTimeout(() => set(null), ms); }
    return { root, set,
      react: function (state, msg) {
        if (opt.speak && msg) speak(msg);
        if (state === 'happy' || state === 'jump') set('happy', 2200);
        else if (state === 'talk') set('talk', 2200);
        else if (state === 'sad') set('shy', 2200);
        else set(null);
      } };
  }

  /* ------------------------------------------------------------------ page wipe + navigation */
  function go(url) {
    if (reduce) { location.href = url; return; }
    const b = document.createElement('div'); b.className = 'sb-topbar'; document.body.appendChild(b);
    document.documentElement.classList.add('sb-leaving');
    setTimeout(function () { location.href = url; }, 240);
  }
  function wipeIn() { document.documentElement.classList.remove('sb-leaving'); }
  /* gentle reveal of cards as they scroll into view (opacity/transform only, so it stays smooth) */
  var RV = '.silly-hero-card,.utility-action-card,.track-item-cyber-card,.job-state-panel-card,.resume-drop-zone-card,.pf-card,.nt-card,.nt-stat,.nt-reality';
  function reveal() {
    if (reduce || !('IntersectionObserver' in window)) return;
    var io = new IntersectionObserver(function (es) {
      es.forEach(function (e) {
        if (!e.isIntersecting) return; io.unobserve(e.target);
        var el = e.target; el.classList.add('sb-rise');
        el.addEventListener('animationend', function () { el.classList.remove('sb-rv', 'sb-rise'); }, { once: true });
      });
    }, { threshold: 0.08 });
    document.querySelectorAll(RV).forEach(function (el, i) { el.classList.add('sb-rv'); el.style.animationDelay = Math.min(i * 60, 360) + 'ms'; io.observe(el); });
    setTimeout(function () { document.querySelectorAll('.sb-rv').forEach(function (el) { el.classList.remove('sb-rv'); }); }, 2500);
  }
  function hookNavbar() {
    document.addEventListener('click', e => {
      const a = e.target.closest && e.target.closest('.nav-menu-btn, .nav-profile-right-zone'); if (!a) return;
      const oc = a.getAttribute('onclick') || ''; const m = oc.match(/location\.href\s*=\s*['"]([^'"]+)['"]/); const url = m ? m[1] : (a.getAttribute('href') && a.getAttribute('href') !== '#' ? a.getAttribute('href') : null);
      if (!url || url === location.pathname.split('/').pop()) return;
      e.preventDefault(); e.stopImmediatePropagation(); go(url);
    }, true);
  }

  /* ------------------------------------------------------------------ cursor spotlight */
  function spotlight() {
    if (reduce || matchMedia('(pointer: coarse)').matches) return;
    const s = document.createElement('div'); s.className = 'sb-spot'; document.body.appendChild(s);
    let raf; document.addEventListener('mousemove', e => { s.classList.add('sb-on'); cancelAnimationFrame(raf); raf = requestAnimationFrame(() => { s.style.transform = 'translate(' + e.clientX + 'px,' + e.clientY + 'px)'; }); });
    document.addEventListener('mouseleave', () => s.classList.remove('sb-on'));
  }

  function profileName() { try { return (JSON.parse(localStorage.getItem('applyrank_profile') || '{}').name || '').trim().split(/\s+/)[0] || ''; } catch (e) { return ''; } }
  function lastResult() { try { return JSON.parse(localStorage.getItem('applyrank_last_result') || 'null'); } catch (e) { return null; } }

  function boot() { hookNavbar(); spotlight(); wipeIn(); reveal(); mountVoicePill(); }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot); else boot();

  window.SillyBot = { create, dock, face, greet, idleTalk, go, speak, speakNow, setVoice, voiceOn, voice: bestVoice, mountVoicePill, quotes: QUOTES, name: profileName, lastResult };
})();