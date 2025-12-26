import { escapeHtml, fmtTime, setActiveNav, qs } from '/js/common.js';

setActiveNav('/articles.html');

const listEl = qs('list');
const errEl = qs('err');
const metaEl = qs('meta');
const qEl = qs('q');
const statusEl = qs('status');
const pullBtn = qs('pull');
const scoreBtn = qs('score');
const refreshBtn = qs('refresh');
const prev = qs('prev');
const next = qs('next');
const pageEl = qs('page');
const sizeEl = qs('size');
const go = qs('go');
const pagerMeta = qs('pagerMeta');

const dlg = qs('dlg');
const dlgTitle = qs('dlgTitle');
const dlgMeta = qs('dlgMeta');
const dlgUrl = qs('dlgUrl');
const dlgClose = qs('dlgClose');
const dlgCancel = qs('dlgCancel');
const dlgSave = qs('dlgSave');
const fZhTitle = qs('fZhTitle');
const fStatus = qs('fStatus');
const fZhContent = qs('fZhContent');
const fReflection = qs('fReflection');

let pageIdx = 0;
let totalPages = 1;
let totalElements = 0;
let rows = [];
let editingId = null;

function statusPill(st){
  const s = (st || '').toUpperCase();
  if (s === 'DONE') return `<span class="pill on">DONE</span>`;
  if (s === 'ERROR') return `<span class="pill off">ERROR</span>`;
  return `<span class="pill">NEEDS_REVIEW</span>`;
}

function scorePill(v){
  if (v === null || v === undefined || v === '') return '';
  const n = Number(v);
  if (!Number.isFinite(n)) return '';
  return `<span class="pill">score ${escapeHtml(n.toFixed(2))}</span>`;
}

function render(list){
  listEl.innerHTML = '';
  if (!list || list.length === 0){
    const div = document.createElement('div');
    div.className = 'card';
    div.textContent = '暂无数据';
    listEl.appendChild(div);
    return;
  }
  for (const a of list){
    const div = document.createElement('div');
    div.className = 'card';
    const title = a.zhTitle || a.title || '(no title)';
    div.innerHTML = `
      <div class="row">
        <div class="main">
          <div class="title">#${escapeHtml(a.id)} · ${escapeHtml(title)}</div>
          <div class="muted url">${escapeHtml(a.url || '')}</div>
          <div class="meta">
            ${statusPill(a.status)}
            ${scorePill(a.attractivenessScore)}
            ${a.sourceName ? `<span class="pill">${escapeHtml(a.sourceName)}</span>` : ''}
            ${a.publishedAt ? `<span class="pill">发布 ${escapeHtml(fmtTime(a.publishedAt))}</span>` : ''}
            ${a.fetchedAt ? `<span class="pill">抓取 ${escapeHtml(fmtTime(a.fetchedAt))}</span>` : ''}
          </div>
        </div>
        <div class="actions">
          <button class="contrast" data-act="detail" data-id="${a.id}">详情/编辑</button>
        </div>
      </div>
    `;
    listEl.appendChild(div);
  }
}

async function load(){
  errEl.textContent = '';
  metaEl.textContent = '';
  listEl.innerHTML = '<div class="card">加载中…</div>';
  try{
    const params = new URLSearchParams();
    params.set('page', String(pageIdx));
    params.set('size', String(parseInt(sizeEl.value || '20', 10)));
    const qv = (qEl.value || '').trim();
    const st = (statusEl.value || '').trim();
    if (qv) params.set('q', qv);
    if (st) params.set('status', st);

    const r = await fetch('/api/articles?' + params.toString());
    const j = await r.json();
    if (!r.ok) throw new Error(j && j.error ? j.error : ('HTTP ' + r.status));
    rows = j.content || [];
    totalPages = j.totalPages || 1;
    totalElements = j.totalElements || rows.length;
    pageEl.value = String(pageIdx + 1);
    metaEl.textContent = `第 ${pageIdx + 1} / ${Math.max(totalPages, 1)} 页 · 总条数 ${totalElements}`;
    if (pagerMeta) pagerMeta.textContent = `共 ${totalElements} 条`;
    render(rows);
  }catch(e){
    rows = [];
    totalPages = 1;
    totalElements = 0;
    render([]);
    errEl.textContent = '加载失败：' + (e && e.message ? e.message : String(e));
  }
}

async function pullNow(){
  errEl.textContent = '';
  metaEl.textContent = '正在拉取文章…（可能需要几十秒）';
  try{
    // 既遵守 last_ingested_at，也遵守 fetch_limit
    const r = await fetch('/api/agents/knowledge-ingest/run?minIntervalMinutes=1440', { method:'POST' });
    const j = await r.json();
    if (!r.ok) throw new Error(j && j.error ? j.error : ('HTTP ' + r.status));
    metaEl.textContent = `拉取完成：${j.message || ''}`;
    pageIdx = 0;
    await load();
  }catch(e){
    errEl.textContent = '拉取失败：' + (e && e.message ? e.message : String(e));
  }
}

async function scoreNow(){
  errEl.textContent = '';
  metaEl.textContent = '正在计算吸引力评分…';
  try{
    const r = await fetch('/api/agents/article-attractiveness-analyst/run', { method:'POST' });
    const j = await r.json();
    if (!r.ok) throw new Error(j && j.error ? j.error : ('HTTP ' + r.status));
    metaEl.textContent = `评分完成：${j.message || ''}`;
    await load();
  }catch(e){
    errEl.textContent = '评分失败：' + (e && e.message ? e.message : String(e));
  }
}

async function openDetail(id){
  errEl.textContent = '';
  try{
    const r = await fetch(`/api/articles/${id}`);
    const j = await r.json();
    if (!r.ok) throw new Error(j && j.error ? j.error : ('HTTP ' + r.status));
    editingId = j.id;
    dlgTitle.textContent = `文章 #${j.id}`;
    dlgMeta.textContent = `${j.sourceName || ''} · 状态 ${j.status || ''} · 发布 ${fmtTime(j.publishedAt)} · 抓取 ${fmtTime(j.fetchedAt)}`;
    dlgUrl.href = j.url || '#';
    fZhTitle.value = j.zhTitle || '';
    fStatus.value = j.status || 'NEEDS_REVIEW';
    fZhContent.value = j.zhContent || '';
    fReflection.value = j.reflection || '';
    dlg.showModal();
  }catch(e){
    errEl.textContent = '加载详情失败：' + (e && e.message ? e.message : String(e));
  }
}

function closeDlg(){ dlg.close(); }

async function saveDetail(){
  if (!editingId) return;
  errEl.textContent = '';
  const payload = {
    zhTitle: (fZhTitle.value || '').trim(),
    zhContent: (fZhContent.value || '').trim(),
    reflection: (fReflection.value || '').trim(),
    status: (fStatus.value || '').trim()
  };
  try{
    const r = await fetch(`/api/articles/${editingId}`, { method:'PUT', headers:{'Content-Type':'application/json'}, body: JSON.stringify(payload) });
    const j = await r.json();
    if (!r.ok) throw new Error(j && j.error ? j.error : ('HTTP ' + r.status));
    closeDlg();
    await load();
  }catch(e){
    errEl.textContent = '保存失败：' + (e && e.message ? e.message : String(e));
  }
}

// events
qEl.addEventListener('input', () => { pageIdx = 0; load(); });
statusEl.addEventListener('change', () => { pageIdx = 0; load(); });
refreshBtn.addEventListener('click', () => { pageIdx = 0; load(); });
pullBtn.addEventListener('click', pullNow);
scoreBtn.addEventListener('click', scoreNow);

prev.addEventListener('click', () => { if (pageIdx <= 0) return; pageIdx -= 1; load(); });
next.addEventListener('click', () => { if (pageIdx + 1 >= totalPages) return; pageIdx += 1; load(); });
go.addEventListener('click', () => { const want = parseInt(pageEl.value || '1', 10); if (!Number.isFinite(want)) return; pageIdx = Math.max(0, want - 1); load(); });
sizeEl.addEventListener('change', () => { pageIdx = 0; load(); });

dlgClose.addEventListener('click', (e) => { e.preventDefault(); closeDlg(); });
dlgCancel.addEventListener('click', (e) => { e.preventDefault(); closeDlg(); });
dlgSave.addEventListener('click', (e) => { e.preventDefault(); saveDetail(); });

listEl.addEventListener('click', (ev) => {
  const act = ev.target?.getAttribute?.('data-act');
  const id = ev.target?.getAttribute?.('data-id');
  if (!act || !id) return;
  if (act === 'detail') openDetail(id);
});

load().then(() => {
  // 支持深链：/articles.html?id=123 自动打开详情
  try{
    const id = new URLSearchParams(location.search).get('id');
    if (id) openDetail(id);
  }catch(e){}
});


