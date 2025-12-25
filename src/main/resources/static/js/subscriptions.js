import { escapeHtml, setActiveNav, qs } from '/js/common.js';

setActiveNav('/subscriptions.html');

const listEl = qs('list');
const errEl = qs('err');
const metaEl = qs('meta');
const qEl = qs('q');
const enabledEl = qs('enabled');
const btnNew = qs('btnNew');
const btnRefresh = qs('btnRefresh');
const prev = qs('prev');
const next = qs('next');
const pageEl = qs('page');
const sizeEl = qs('size');
const go = qs('go');
const pagerMeta = qs('pagerMeta');

const dlg = qs('dlg');
const dlgTitle = qs('dlgTitle');
const dlgClose = qs('dlgClose');
const dlgCancel = qs('dlgCancel');
const dlgSave = qs('dlgSave');

const fName = qs('fName');
const fType = qs('fType');
const fZhName = qs('fZhName');
const fCap = qs('fCap');
const fUrl = qs('fUrl');
const fTags = qs('fTags');
const fLimit = qs('fLimit');
const fEnabled = qs('fEnabled');

let pageIdx = 0;
let totalPages = 1;
let totalElements = 0;
let rows = [];
let mode = 'create';
let editingId = null;

function pillEnabled(v){
  return v ? `<span class="pill on">启用</span>` : `<span class="pill off">禁用</span>`;
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
  for (const s of list){
    const div = document.createElement('div');
    div.className = 'card';
    div.innerHTML = `
      <div class="row">
        <div class="main">
          <div class="title">#${escapeHtml(s.id)} · ${escapeHtml(s.zhName || '')} <span class="muted">(${escapeHtml(s.name || '')})</span></div>
          <div class="muted" style="margin-top:.15rem; white-space:nowrap; overflow:hidden; text-overflow:ellipsis;">${escapeHtml(s.capabilitySummary || '')}</div>
          <div class="muted url">${escapeHtml(s.feedUrl || '')}</div>
          <div class="meta">
            ${pillEnabled(!!s.enabled)}
            <span class="pill">${escapeHtml(s.sourceType || '')}</span>
            <span class="pill">limit ${escapeHtml(s.fetchLimit ?? 20)}</span>
            ${s.tags ? `<span class="pill">${escapeHtml(s.tags)}</span>` : ''}
          </div>
        </div>
        <div class="actions">
          <button class="contrast" data-act="edit" data-id="${s.id}">编辑</button>
          <button class="secondary" data-act="toggle" data-id="${s.id}">${s.enabled ? '禁用' : '启用'}</button>
          <button class="secondary" data-act="del" data-id="${s.id}">删除</button>
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
    const ev = (enabledEl.value || '').trim();
    if (qv) params.set('q', qv);
    if (ev === 'true' || ev === 'false') params.set('enabled', ev);

    const r = await fetch('/api/subscriptions?' + params.toString());
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

function openDialog(m, item){
  mode = m;
  editingId = item ? item.id : null;
  dlgTitle.textContent = m === 'create' ? '新增订阅' : `编辑订阅 #${editingId}`;
  fName.value = item?.name || '';
  fZhName.value = item?.zhName || '';
  fCap.value = item?.capabilitySummary || '';
  fType.value = item?.sourceType || 'RSS';
  fUrl.value = item?.feedUrl || '';
  fTags.value = item?.tags || '';
  fLimit.value = String(item?.fetchLimit ?? 10);
  fEnabled.checked = item?.enabled ?? true;
  dlg.showModal();
}

function closeDialog(){ dlg.close(); }

async function save(){
  errEl.textContent = '';
  const payload = {
    name: (fName.value || '').trim(),
    zhName: (fZhName.value || '').trim(),
    capabilitySummary: (fCap.value || '').trim(),
    sourceType: (fType.value || 'RSS').trim(),
    feedUrl: (fUrl.value || '').trim(),
    tags: (fTags.value || '').trim(),
    fetchLimit: parseInt(fLimit.value || '10', 10),
    enabled: !!fEnabled.checked
  };
  if (!payload.name || !payload.zhName || !payload.capabilitySummary || !payload.sourceType || !payload.feedUrl){
    errEl.textContent = '保存失败：英文名/中文名/能力总结/sourceType/feedUrl 都不能为空';
    return;
  }
  try{
    const isCreate = mode === 'create';
    const url = isCreate ? '/api/subscriptions' : `/api/subscriptions/${editingId}`;
    const method = isCreate ? 'POST' : 'PUT';
    const r = await fetch(url, { method, headers: {'Content-Type':'application/json'}, body: JSON.stringify(payload) });
    const j = await r.json();
    if (!r.ok) throw new Error(j && j.error ? j.error : ('HTTP ' + r.status));
    closeDialog();
    pageIdx = 0;
    await load();
  }catch(e){
    errEl.textContent = '保存失败：' + (e && e.message ? e.message : String(e));
  }
}

async function toggle(id){
  const item = rows.find(x => String(x.id) === String(id));
  if (!item) return;
  try{
    const r = await fetch(`/api/subscriptions/${id}`, { method:'PUT', headers:{'Content-Type':'application/json'}, body: JSON.stringify({ enabled: !item.enabled })});
    const j = await r.json();
    if (!r.ok) throw new Error(j && j.error ? j.error : ('HTTP ' + r.status));
    await load();
  }catch(e){
    errEl.textContent = '更新失败：' + (e && e.message ? e.message : String(e));
  }
}

async function del(id){
  if (!confirm(`确认删除订阅 #${id} ?`)) return;
  try{
    const r = await fetch(`/api/subscriptions/${id}`, { method:'DELETE' });
    const j = await r.json();
    if (!r.ok) throw new Error(j && j.error ? j.error : ('HTTP ' + r.status));
    await load();
  }catch(e){
    errEl.textContent = '删除失败：' + (e && e.message ? e.message : String(e));
  }
}

// events
qEl.addEventListener('input', () => { pageIdx = 0; load(); });
enabledEl.addEventListener('change', () => { pageIdx = 0; load(); });
btnRefresh.addEventListener('click', () => { pageIdx = 0; load(); });
btnNew.addEventListener('click', () => openDialog('create', null));

prev.addEventListener('click', () => { if (pageIdx <= 0) return; pageIdx -= 1; load(); });
next.addEventListener('click', () => { if (pageIdx + 1 >= totalPages) return; pageIdx += 1; load(); });
go.addEventListener('click', () => { const want = parseInt(pageEl.value || '1', 10); if (!Number.isFinite(want)) return; pageIdx = Math.max(0, want - 1); load(); });
sizeEl.addEventListener('change', () => { pageIdx = 0; load(); });

dlgClose.addEventListener('click', (e) => { e.preventDefault(); closeDialog(); });
dlgCancel.addEventListener('click', (e) => { e.preventDefault(); closeDialog(); });
dlgSave.addEventListener('click', (e) => { e.preventDefault(); save(); });

listEl.addEventListener('click', (ev) => {
  const act = ev.target?.getAttribute?.('data-act');
  const id = ev.target?.getAttribute?.('data-id');
  if (!act || !id) return;
  if (act === 'edit'){
    const item = rows.find(x => String(x.id) === String(id));
    if (item) openDialog('edit', item);
  }
  if (act === 'toggle') toggle(id);
  if (act === 'del') del(id);
});

load();


