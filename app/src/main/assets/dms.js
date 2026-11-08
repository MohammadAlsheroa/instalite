// Messages-only build (dms flavor). Injected after inject.js.
// Hides navigation that leads out of the DMs. Native code (MainActivity.checkUrl)
// still blocks any non-DM page and sends you back to the inbox.
(function () {
  if (window.__instaLiteDms) return;
  window.__instaLiteDms = true;

  var CSS =
    'a[href="/"],a[href="https://www.instagram.com/"],' +
    'a[href^="/explore"],a[href^="/reels"],a[href^="/create"],' +
    'a[href*="/accounts/activity"],a[href^="/notifications"]{display:none!important}';

  function addStyle() {
    if (document.getElementById('__instalite_dms_css')) return;
    var root = document.head || document.documentElement;
    if (!root) return;
    var s = document.createElement('style');
    s.id = '__instalite_dms_css';
    s.textContent = CSS;
    root.appendChild(s);
  }

  // Bottom/side nav bars: hide every item that isn't the DMs.
  function hideNav() {
    var navs = document.querySelectorAll('nav, div[role="navigation"]');
    for (var i = 0; i < navs.length; i++) {
      var links = navs[i].querySelectorAll('a[href]');
      for (var j = 0; j < links.length; j++) {
        var p = new URL(links[j].getAttribute('href'), location.href).pathname;
        if (p.indexOf('/direct/') !== 0) links[j].style.setProperty('display', 'none', 'important');
      }
    }
  }

  function isInbox() { return /^\/direct\/inbox\/?$/.test(location.pathname); }

  // Inbox: the Back arrow leads to the (blocked) home feed, so hide it.
  // visibility (not display) keeps the header title centered.
  // Inside a chat the Back arrow returns to the inbox, so it stays.
  function tidyBack() {
    var icons = document.querySelectorAll('svg[aria-label="Back"]');
    for (var i = 0; i < icons.length; i++) {
      var btn = icons[i].closest('[role="button"],a') || icons[i];
      if (isInbox()) {
        btn.style.setProperty('visibility', 'hidden', 'important');
        btn.setAttribute('data-il-back', '1');
      } else if (btn.getAttribute('data-il-back')) {
        btn.style.removeProperty('visibility');
        btn.removeAttribute('data-il-back');
      }
    }
  }

  // Inbox: hide the username (account switcher) and the New message icon in the
  // header. visibility keeps their space, so the search bar stays where it was.
  function tidyHeader() {
    var items = [];
    var chev = document.querySelector('svg[aria-label="Down chevron icon"]');
    if (chev) items.push(chev.closest('[role="button"]') || chev);
    var neu = document.querySelector('a[href="/direct/new/"]');
    if (neu) items.push(neu);
    for (var i = 0; i < items.length; i++) {
      if (isInbox()) {
        items[i].style.setProperty('visibility', 'hidden', 'important');
        items[i].setAttribute('data-il-head', '1');
      }
    }
    if (!isInbox()) {
      var done = document.querySelectorAll('[data-il-head]');
      for (var j = 0; j < done.length; j++) {
        done[j].style.removeProperty('visibility');
        done[j].removeAttribute('data-il-head');
      }
    }
  }

  // Inbox: hide the Notes tray (the row of note bubbles above "Messages").
  // Found by behavior: a tall horizontal scroller near the top of the inbox.
  function hideNotes() {
    if (!isInbox()) return;
    var divs = document.querySelectorAll('div');
    for (var i = 0; i < divs.length; i++) {
      var e = divs[i];
      if (e.getAttribute('data-il-notes')) continue;
      var r = e.getBoundingClientRect();
      if (r.height < 100 || r.top > 400 || e.scrollWidth <= r.width + 1) continue;
      var ox = getComputedStyle(e).overflowX;
      if (ox !== 'auto' && ox !== 'scroll') continue;
      var t = e;
      while (t.parentElement && t.parentElement.getBoundingClientRect().height <= r.height + 40) t = t.parentElement;
      t.setAttribute('data-il-notes', '1');
      t.style.setProperty('display', 'none', 'important');
    }
  }

  var pending = false;
  function tick() {
    addStyle();
    if (pending) return;
    pending = true;
    requestAnimationFrame(function () {
      pending = false;
      hideNav();
      tidyBack();
      tidyHeader();
      hideNotes();
    });
  }

  tick();
  new MutationObserver(tick).observe(document.documentElement, { childList: true, subtree: true });
})();
