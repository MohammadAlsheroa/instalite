// Injected into every instagram.com page at document start.
// Hides the Reels tab and swallows taps on it (native code also blocks it).
(function () {
  if (window.__instaLite) return;
  window.__instaLite = true;

  var CSS =
    'a[href="/reels/"],a[href^="/reels/?"],' +
    'a[href="https://www.instagram.com/reels/"]{display:none!important}';

  function addStyle() {
    if (document.getElementById('__instalite_css')) return;
    var root = document.head || document.documentElement;
    if (!root) return;
    var s = document.createElement('style');
    s.id = '__instalite_css';
    s.textContent = CSS;
    root.appendChild(s);
  }

  // Fallback for nav items whose href differs: hide any link to the reels feed
  // that carries the "Reels" icon.
  function hideNavReels() {
    var icons = document.querySelectorAll('svg[aria-label="Reels"]');
    for (var i = 0; i < icons.length; i++) {
      var a = icons[i].closest('a');
      if (!a) continue;
      var p = new URL(a.getAttribute('href') || '', location.href).pathname;
      if (p === '/reels/' || p === '/reels') a.style.setProperty('display', 'none', 'important');
    }
  }

  // ---- Single-reel lock -------------------------------------------------
  // Reels opened from a chat show in an overlay (URL stays /direct/t/...)
  // holding a vertical scroll-snap list of reels. Lock it on the opened reel;
  // any swipe/scroll to another reel closes the overlay -> back to the chat.
  var handled = new WeakSet();

  function isReelScroller(e) {
    var c = getComputedStyle(e);
    return c.scrollSnapType.indexOf('y') >= 0 &&
      (c.overflowY === 'scroll' || c.overflowY === 'auto' || e.dataset.instaliteLocked);
  }

  function findScroller(video) {
    var e = video.parentElement;
    for (var i = 0; e && i < 25; i++, e = e.parentElement) {
      if (isReelScroller(e)) return e;
    }
    return null;
  }

  function closeViewer(scroller) {
    if (scroller.dataset.instaliteClosing) return;
    scroller.dataset.instaliteClosing = '1';
    document.querySelectorAll('video').forEach(function (v) { v.pause(); v.muted = true; });
    var box = scroller;
    while (box && getComputedStyle(box).position !== 'fixed') box = box.parentElement;
    var scope = box || document;
    var icon = scope.querySelector('svg[aria-label="Close"]') || scope.querySelector('svg[aria-label="Back"]');
    var btn = icon && (icon.closest('[role="button"],button,a') || icon);
    if (btn) {
      btn.dispatchEvent(new MouseEvent('click', { bubbles: true, cancelable: true, view: window }));
      // If the overlay is still there shortly after, fall back to reloading the chat.
      setTimeout(function () { if (document.contains(scroller)) location.reload(); }, 700);
    } else {
      location.reload();
    }
  }

  function lockScroller(sc) {
    handled.add(sc);
    var lockTop = sc.scrollTop;
    var settled = false;
    // Instagram may jump to the tapped reel right after opening: follow that first.
    setTimeout(function () {
      lockTop = sc.scrollTop;
      settled = true;
      sc.dataset.instaliteLocked = '1';
      sc.style.setProperty('overflow-y', 'hidden', 'important');
    }, 800);

    sc.addEventListener('scroll', function () {
      if (!settled) { lockTop = sc.scrollTop; return; }
      if (Math.abs(sc.scrollTop - lockTop) > 20) closeViewer(sc);
    }, { passive: true });

    var sx = 0, sy = 0;
    sc.addEventListener('touchstart', function (e) {
      var t = e.touches[0]; sx = t.clientX; sy = t.clientY;
    }, { passive: true, capture: true });
    sc.addEventListener('touchmove', function (e) {
      var t = e.touches[0];
      var dy = t.clientY - sy, dx = t.clientX - sx;
      if (Math.abs(dy) > 60 && Math.abs(dy) > Math.abs(dx) * 1.5) {
        if (e.cancelable) e.preventDefault();
        closeViewer(sc);
      }
    }, { passive: false, capture: true });
  }

  function lockReels() {
    var vids = document.querySelectorAll('video');
    for (var i = 0; i < vids.length; i++) {
      var sc = findScroller(vids[i]);
      if (sc && !handled.has(sc)) lockScroller(sc);
    }
  }

  function tick() { addStyle(); hideNavReels(); lockReels(); }

  // Throttle: Instagram mutates the DOM constantly.
  var pending = false;
  function schedule() {
    if (pending) return;
    pending = true;
    setTimeout(function () { pending = false; tick(); }, 150);
  }

  tick();
  document.addEventListener('DOMContentLoaded', tick);
  new MutationObserver(schedule).observe(document.documentElement, { childList: true, subtree: true });

  document.addEventListener('click', function (e) {
    var a = e.target && e.target.closest && e.target.closest('a[href]');
    if (!a) return;
    var p = new URL(a.href, location.href).pathname;
    if (p === '/reels/' || p === '/reels') {
      e.preventDefault();
      e.stopImmediatePropagation();
    }
  }, true);
})();
