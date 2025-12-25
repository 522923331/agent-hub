export function escapeHtml(s) {
  return String(s)
    .replaceAll('&', '&amp;')
    .replaceAll('<', '&lt;')
    .replaceAll('>', '&gt;')
    .replaceAll('"', '&quot;')
    .replaceAll("'", '&#039;');
}

export function fmtTime(t) {
  if (!t) return '-';
  try { return new Date(t).toLocaleString(); } catch (_) { return String(t); }
}

export function setActiveNav(pathname) {
  document.querySelectorAll('nav a[data-nav]').forEach(a => {
    const p = a.getAttribute('href');
    if (p === pathname) a.classList.add('active');
    else a.classList.remove('active');
  });
}

export function qs(id) { return document.getElementById(id); }


