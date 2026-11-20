/* ApplyRank - silly-robot.js  (file name kept so every page keeps working)
   Calm helper: a small "Help" button that opens a tips panel. No mascot, no speech, no voice.
   Same API as before, so no page needs to change:
     SillyBot.dock({ greet, tips })  -> small Help button + tips panel (bottom right)
     bot.say('text', { ms })         -> short toast message (used for progress / feedback)
     SillyBot.face(el) / create({parent}) -> a small verified-rank shield emblem inside a circle
     SillyBot.go(url)                -> smooth page navigation                                              */
(function () {
  'use strict';
  var uid = 0;
  var reduce = window.matchMedia && matchMedia('(prefers-reduced-motion: reduce)').matches;
  var QUOTES = [
    'One rejection is an answer for one role, not for your whole career. Keep going.',
    'Learn a little every day. Your verified rank grows one day at a time.',
    'Every fresher starts here. You are not behind, you are just beginning.',
    'Build one project properly and put it on GitHub. Recruiters look at real code.',
    'People with verified skills are still in demand. Keep your rank up to date.'
  ];

  /* remove emojis and extra spaces so every message reads calm and professional */
  function plain(t) { return String(t == null ? '' : t).replace(/[\u{1F000}-\u{1FAFF}\u{2600}-\u{27BF}\u{FE0F}\u{200D}]/gu, '').replace(/\s+/g, ' ').trim(); }

  function emblem() {
    var id = 'hpg' + (++uid);
    return '<svg class="hp-emblem" viewBox="0 0 64 64" aria-hidden="true"><defs><linearGradient id="' + id + '" x1="0" y1="0" x2="1" y2="1"><stop offset="0" stop-color="#6366f1"/><stop offset="1" stop-color="#10b981"/></linearGradient></defs>' +
      '<path d="M32 6 54 14v16c0 14-10 24-22 28C20 54 10 44 10 30V14z" fill="none" stroke="url(#' + id + ')" stroke-width="4" stroke-linejoin="round"/>' +
      '<path d="m22 32 7 7 14-15" fill="none" stroke="url(#' + id + ')" stroke-width="5" stroke-linecap="round" stroke-linejoin="round"/></svg>';
  }

  /* ---------------------------------------------------------------- toast (short messages) */
  var toastEl, toastT;
  function toast(text, ms) {
    text = plain(text); if (!text) return;
    if (!toastEl) { toastEl = document.createElement('div'); toastEl.className = 'hp-toast'; toastEl.setAttribute('role', 'status'); document.body.appendChild(toastEl); }
    toastEl.textContent = text; toastEl.classList.add('hp-on');
    clearTimeout(toastT); toastT = setTimeout(function () { toastEl.classList.remove('hp-on'); }, ms > 0 ? ms : 6000);
  }
  function stub() {
    var noop = function () {}, P = function () { return Promise.resolve(); };
    return { el: document.createElement('div'), set: noop, sit: noop, stand: noop, hush: noop, hearts: noop, destroy: noop, nextTip: noop, walkTo: P,
             say: function (t, o) { toast(t, o && o.ms); return P(); } };
  }

  /* ---------------------------------------------------------------- Help button + tips panel */
  function dock(o) {
    o = o || {};
    var old = document.querySelector('.hp-help'); if (old) old.remove();
    var wrap = document.createElement('div'); wrap.className = 'hp-help';
    var btn = document.createElement('button'); btn.type = 'button'; btn.className = 'hp-btn'; btn.setAttribute('aria-label', 'Help and tips'); btn.setAttribute('aria-expanded', 'false'); btn.textContent = '?';
    var panel = document.createElement('div'); panel.className = 'hp-panel'; panel.setAttribute('role', 'dialog'); panel.setAttribute('aria-label', 'Tips for this page');
    var h = document.createElement('div'); h.className = 'hp-title'; h.textContent = 'Tips for this page'; panel.appendChild(h);
    if (o.greet) { var g = document.createElement('p'); g.className = 'hp-greet'; g.textContent = plain(o.greet); panel.appendChild(g); }
    var tips = (o.tips || []).map(plain).filter(Boolean);
    if (tips.length) { var ul = document.createElement('ul'); tips.forEach(function (t) { var li = document.createElement('li'); li.textContent = t; ul.appendChild(li); }); panel.appendChild(ul); }
    wrap.appendChild(panel); wrap.appendChild(btn); document.body.appendChild(wrap);
    function set(open) { panel.classList.toggle('hp-open', open); btn.setAttribute('aria-expanded', open ? 'true' : 'false'); btn.textContent = open ? '×' : '?'; }
    btn.addEventListener('click', function (e) { e.stopPropagation(); set(!panel.classList.contains('hp-open')); });
    document.addEventListener('click', function (e) { if (!wrap.contains(e.target)) set(false); });
    document.addEventListener('keydown', function (e) { if (e.key === 'Escape') set(false); });
    return stub();
  }

  /* ---------------------------------------------------------------- emblem inside a circle (replaces the robot face) */
  function face(anchor) {
    if (!anchor) return { react: function () {}, set: function () {} };
    anchor.innerHTML = '<div class="hp-face">' + emblem() + '</div>';
    var root = anchor.firstChild, t;
    return { root: root, set: function () {}, react: function (state) {
      if (state !== 'happy' && state !== 'jump' && state !== 'talk') return;
      root.classList.remove('hp-pulse'); void root.offsetWidth; root.classList.add('hp-pulse');
      clearTimeout(t); t = setTimeout(function () { root.classList.remove('hp-pulse'); }, 900);
    } };
  }
  function create(o) {
    o = o || {};
    if (o.parent) o.parent.innerHTML = '<div class="hp-face hp-face-big">' + emblem() + '</div>';
    return stub();
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
  var noop = function () {};

  function boot() { hookNavbar(); spotlight(); wipeIn(); reveal(); }
  if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', boot); else boot();

  window.SillyBot = { create: create, dock: dock, face: face, greet: noop, idleTalk: noop, go: go, speak: noop, speakNow: noop, setVoice: noop, setVoiceName: noop,
    voiceOn: function () { return false; }, voice: function () { return null; }, mountVoicePill: noop, quotes: QUOTES, name: profileName, lastResult: lastResult };
})();
