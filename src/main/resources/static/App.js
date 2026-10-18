const API_BASE = '';
const POLL_INTERVAL_MS = 1000;

const el = {
  statusDot: document.getElementById('statusDot'),
  statusText: document.getElementById('statusText'),
  connError: document.getElementById('connError'),
  bybitBalance: document.getElementById('bybitBalance'),
  kucoinBalance: document.getElementById('kucoinBalance'),
  totalChange: document.getElementById('totalChange'),
  totalNetWorth: document.getElementById('totalNetWorth'),
  candidatesList: document.getElementById('candidatesList'),
  candidatesCount: document.getElementById('candidatesCount'),
  candidatesEmptyState: document.getElementById('candidatesEmptyState'),
  openPositionSection: document.getElementById('openPositionSection'),
  openPositionList: document.getElementById('openPositionList'),
};

function fmtUsdt(n) {
  return n.toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
}

function fmtPrice(n) {
  if (n >= 100) return n.toFixed(2);
  if (n >= 1) return n.toFixed(4);
  return n.toFixed(6);
}

function fmtPercent(n) {
  const sign = n >= 0 ? '+' : '';
  return `${sign}${n.toFixed(2)}%`;
}

async function fetchJson(path) {
  const res = await fetch(API_BASE + path, { cache: 'no-store' });
  if (!res.ok) throw new Error(`${path} -> HTTP ${res.status}`);
  return res.json();
}

function setStatus(state) {
  el.statusDot.classList.remove('live', 'error');
  if (state === 'live') {
    el.statusDot.classList.add('live');
    el.statusText.textContent = 'Подключено';
    el.connError.hidden = true;
  } else if (state === 'error') {
    el.statusDot.classList.add('error');
    el.statusText.textContent = 'Нет связи';
    el.connError.hidden = false;
  } else {
    el.statusText.textContent = 'Подключение…';
  }
}

function escapeHtml(str) {
  const div = document.createElement('div');
  div.textContent = str;
  return div.innerHTML;
}

function pluralRu(n) {
  const mod10 = n % 10;
  const mod100 = n % 100;
  if (mod10 === 1 && mod100 !== 11) return 'ий';
  if ([2, 3, 4].includes(mod10) && ![12, 13, 14].includes(mod100)) return 'ия';
  return 'ий';
}

function renderBalances(snapshot) {
  el.bybitBalance.textContent = fmtUsdt(snapshot.bybitNetWorthUsdt);
  el.kucoinBalance.textContent = fmtUsdt(snapshot.kucoinNetWorthUsdt);

  el.totalChange.textContent = fmtPercent(snapshot.totalChangePercent);
  el.totalChange.classList.remove('positive', 'negative');
  el.totalChange.classList.add(snapshot.totalChangePercent >= 0 ? 'positive' : 'negative');

  el.totalNetWorth.textContent = `${fmtUsdt(snapshot.totalNetWorthUsdt)} USDT всего`;
}

function renderCandidates(candidates) {
  el.candidatesCount.textContent = `${candidates.length} кандидат${pluralRu(candidates.length)}`;

  if (candidates.length === 0) {
    el.candidatesList.innerHTML = '';
    el.candidatesList.appendChild(el.candidatesEmptyState);
    return;
  }

  el.candidatesList.innerHTML = candidates.map(candidate => `
    <div class="candidate-row">
      <span class="candidate-symbol">${escapeHtml(candidate.symbol)}</span>
      <span class="candidate-prices">
        <span class="price-futures">${fmtPrice(candidate.futuresPrice)}</span>
        <span class="price-spot">${fmtPrice(candidate.spotPrice)}</span>
      </span>
      <span class="candidate-spread">${fmtPercent(candidate.currentSpreadPercent)}</span>
    </div>
  `).join('');
}

function renderOpenPositions(positions) {
  const section = el.openPositionSection;
  const list = el.openPositionList;

  if (!positions || positions.length === 0) {
    section.hidden = true;
    list.innerHTML = '';
    return;
  }

  section.hidden = false;
  list.innerHTML = positions.map(pos => `
    <div class="open-position-card">
      <div class="open-position-main">
        <span class="open-position-symbol">${escapeHtml(pos.symbol)}</span>
        <span class="open-position-side">${escapeHtml(pos.side)}</span>
      </div>
      <div class="open-position-grid">
        <div class="open-position-field">
          <span class="label">Фьючерс (шорт)</span>
          <span class="value">${fmtPrice(pos.futuresPrice)}</span>
        </div>
        <div class="open-position-field">
          <span class="label">Спот (лонг)</span>
          <span class="value">${fmtPrice(pos.spotPrice)}</span>
        </div>
        <div class="open-position-field">
          <span class="label">Прибыль</span>
          <span class="value">${fmtUsdt(pos.currentProfitUsdt)}</span>
        </div>
        <div class="open-position-field">
          <span class="label">Прибыль %</span>
          <span class="value">${fmtPercent(pos.currentProfitPercent)}</span>
        </div>
      </div>
    </div>
  `).join('');
}

async function refresh() {
  try {
    const [balances, positions, candidates] = await Promise.all([
      fetchJson('/api/balances'),
      fetchJson('/api/positions'),
      fetchJson('/api/candidates'),
    ]);
    renderBalances(balances);
    renderCandidates(candidates);
    renderOpenPositions(positions);
    setStatus('live');
  } catch (e) {
    console.error(e);
    setStatus('error');
  }
}

refresh();
setInterval(refresh, POLL_INTERVAL_MS);