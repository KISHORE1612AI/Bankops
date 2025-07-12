// customer-support-dashboard.js
// PRODUCTION: Efficient, error-tolerant, fast-loading, dynamic dashboard!
// - Chart API failures handled gracefully
// - Metrics and main data fetched only ONCE per load or after ticket actions
// - No duplicate resolution note fields
// - Modal is scrollable, UI is responsive and fast

document.addEventListener('DOMContentLoaded', async () => {
  const MAX_REFRESH_COUNT = 2;
  const IDLE_TIMEOUT_MS   = 2 * 60 * 1000; // 2 minutes
  const API_BASE          = "http://localhost:8080/api/support/tickets";

  // Token & session management
  let token         = localStorage.getItem('token');
  let refreshToken  = localStorage.getItem('refreshToken');
  let refreshCount  = parseInt(localStorage.getItem('refreshCount')) || 0;
  let lastAccess    = parseInt(localStorage.getItem('lastAccessTime')) || 0;

  if (!token || !refreshToken) return handleLogout('You must be logged in!');

  function parseJwt(t) {
    try {
      const base64Url = t.split('.')[1];
      const base64    = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const json      = atob(base64)
        .split('').map(c => '%' + c.charCodeAt(0).toString(16).padStart(2, '0')).join('');
      return JSON.parse(decodeURIComponent(json));
    } catch {
      return null;
    }
  }

  async function refreshAccessToken() {
    try {
      const res = await fetch('http://localhost:8080/auth/refresh-token', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ refreshToken })
      });
      if (!res.ok) throw new Error('Refresh failed');
      const { accessToken, refreshToken: newRefresh } = await res.json();
      localStorage.setItem('token', accessToken);
      localStorage.setItem('refreshToken', newRefresh);
      refreshCount++;
      localStorage.setItem('refreshCount', refreshCount);
      localStorage.setItem('lastAccessTime', Date.now());
      token = accessToken;
      return true;
    } catch (e) {
      console.error('Token refresh error', e);
      return false;
    }
  }

  function handleLogout(msg) {
    if (msg) alert(msg);
    ['token','refreshToken','refreshCount','lastAccessTime']
      .forEach(k => localStorage.removeItem(k));
    window.location.href = '/employee-login.html';
  }

  // Validate & refresh JWT if needed
  let decoded = parseJwt(token);
  const now = Date.now();
  if (!decoded) {
    if (!(await refreshAccessToken())) return handleLogout('Session expired. Please log in.');
    decoded = parseJwt(localStorage.getItem('token'));
    if (!decoded) return handleLogout('Invalid token after refresh.');
  }
  if (decoded.role !== 'CUSTOMER_SUPPORT') {
    return handleLogout('Access denied.');
  }
  const expiry = decoded.exp * 1000;
  if (now >= expiry) {
    // expired access token
    const idle = now - (parseInt(localStorage.getItem('lastAccessTime')) || 0);
    if (idle > IDLE_TIMEOUT_MS || refreshCount >= MAX_REFRESH_COUNT) {
      return handleLogout('Session expired due to inactivity.');
    }
    if (!(await refreshAccessToken())) {
      return handleLogout('Session refresh failed.');
    }
    decoded = parseJwt(localStorage.getItem('token'));
    if (!decoded || decoded.role !== 'CUSTOMER_SUPPORT') {
      return handleLogout('Invalid token after refresh.');
    }
  } else {
    localStorage.setItem('lastAccessTime', now);
  }

  // === State ===
  let tickets            = [];
  let currentPage        = 0;
  const pageSize         = 10;
  let totalPages         = 1;
  let statusFilter       = '';
  let searchTerm         = '';
  const metrics          = { open: '-', resolved: '-', avgResponseTime: '-' };

  // === Helpers ===
  function showToast(msg, type = 'info') {
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    toast.textContent = msg;
    document.body.appendChild(toast);
    setTimeout(() => toast.remove(), 3000);
  }

  function apiFetch(url, opts = {}) {
    opts.headers = {
      ...(opts.headers || {}),
      'Authorization': `Bearer ${localStorage.getItem('token')}`
    };
    return fetch(url, opts);
  }

  // === Metrics ===
  async function fetchMetrics() {
    try {
      const [openRes, resRes] = await Promise.all([
        apiFetch(`${API_BASE}?status=OPEN&page=0&size=1`),
        apiFetch(`${API_BASE}?status=RESOLVED&page=0&size=1`)
      ]);
      const { totalElements: open = 0 } = await openRes.json();
      const { totalElements: resolved = 0 } = await resRes.json();
      metrics.open = open;
      metrics.resolved = resolved;
      metrics.avgResponseTime = '45 mins'; // placeholder or fetched separately
    } catch {
      metrics.open = metrics.resolved = metrics.avgResponseTime = '-';
    }
  }

  function renderMetrics() {
    const openEl = document.getElementById('openTickets');
    const resEl  = document.getElementById('resolvedTickets');
    const avgEl  = document.getElementById('avgResponseTime');
    if (openEl) openEl.textContent = metrics.open;
    if (resEl)  resEl.textContent = metrics.resolved;
    if (avgEl)  avgEl.textContent = metrics.avgResponseTime;
  }

  // === Charts ===
  let chartInst = null;
  async function renderTicketsChart() {
    const canvas = document.getElementById('ticketsChart');
    if (!canvas) {
      console.warn('ticketsChart canvas not found — skipping chart render');
      return;
    }

    const chartData = {
      labels: ['Mon','Tue','Wed','Thu','Fri','Sat','Sun'],
      datasets: [{ label:'Tickets', data: Array(7).fill(0) }]
    };

    try {
      const res = await apiFetch(`${API_BASE}/weekly-stats`);
      if (res.ok) {
        const stats = await res.json();
        const idx = {Mon:0,Tue:1,Wed:2,Thu:3,Fri:4,Sat:5,Sun:6};
        stats.forEach(s => { if (idx[s.day] >=0) chartData.datasets[0].data[idx[s.day]] = s.count; });
      }
    } catch (e) {
      console.warn('Weekly stats fetch failed, using zeros', e);
    }

    if (chartInst) chartInst.destroy();
    chartInst = new Chart(canvas.getContext('2d'), {
      type: 'bar',
      data: chartData,
      options: {
        responsive: true,
        plugins: { title:{ display:true, text:'Weekly Ticket Volume' } },
        scales: { y:{ beginAtZero:true } }
      }
    });
  }

  // === Tickets Table ===
  async function fetchTickets(page=0) {
    const url = new URL(API_BASE);
    url.searchParams.append('page', page);
    url.searchParams.append('size', pageSize);
    if (statusFilter) url.searchParams.append('status', statusFilter);
    if (searchTerm) url.searchParams.append('search', searchTerm);

    const res = await apiFetch(url);
    if (!res.ok) throw new Error('Failed to fetch tickets');
    const data = await res.json();
    tickets     = data.content;
    currentPage = data.number;
    totalPages  = data.totalPages;
  }

  function renderTicketsTable() {
    const tbody = document.getElementById('ticketsTableBody');
    if (!tbody) return;
    tbody.innerHTML = '';

    tickets.forEach(ticket => {
      const tr = document.createElement('tr');
      tr.innerHTML = `
        <td>${ticket.id}</td>
        <td>${ticket.customerId || '-'}</td>
        <td>${ticket.issueType || '-'}</td>
        <td>${ticket.status || '-'}</td>
        <td>${ticket.createdAt ? new Date(ticket.createdAt).toLocaleString() : '-'}</td>
        <td>${ticket.handledBy || '-'}</td>
        <td>${ticket.resolutionNote || '-'}</td>
        <td><button class="action-btn" data-id="${ticket.id}">View</button></td>
      `;
      tbody.appendChild(tr);
    });
    renderPagination();
  }

  function renderPagination() {
    const container = document.getElementById('paginationControls');
    if (!container) return;
    container.innerHTML = '';
    for (let i = 0; i < totalPages; i++) {
      const btn = document.createElement('button');
      btn.textContent = i + 1;
      if (i === currentPage) btn.classList.add('active');
      btn.onclick = () => loadPage(i);
      container.appendChild(btn);
    }
  }

  async function loadPage(page) {
    await fetchTickets(page);
    renderTicketsTable();
  }

  // === Ticket Action Handler ===
  async function handleTicketAction(id, action, note='') {
    const url = `${API_BASE}/${id}/${action}`;
    const opts = {
      method: 'PUT',
      headers: {
        'Content-Type': 'application/json',
        'Authorization': `Bearer ${localStorage.getItem('token')}`
      }
    };
    if (note) opts.body = JSON.stringify({ resolutionNote: note });

    const res = await fetch(url, opts);
    if (!res.ok) {
      let msg = 'Action failed';
      try {
        const text = await res.text();
        const err  = JSON.parse(text || '{}');
        if (err.message) msg = err.message;
      } catch {}
      throw new Error(msg);
    }
    // Handle 204 No Content
    if (res.status === 204) return {};
    try {
      const text = await res.text();
      return text ? JSON.parse(text) : {};
    } catch {
      return {};
    }
  }

  // === Modal & Actions UI ===
  function showModal(ticket) {
    const modal = document.getElementById('ticketModal');
    if (!modal) return;
    modal.style.display = 'block';

    const info = document.getElementById('modalTicketInfo');
    if (info) {
      info.innerHTML = `
        <table class="modal-table">
          <tr><td><b>ID</b></td><td>${ticket.id}</td></tr>
          <tr><td><b>Type</b></td><td>${ticket.issueType}</td></tr>
          <tr><td><b>Status</b></td><td>${ticket.status}</td></tr>
          <tr><td><b>Desc</b></td><td>${ticket.description}</td></tr>
          <tr><td><b>Created</b></td><td>${ticket.createdAt ? new Date(ticket.createdAt).toLocaleString() : '-'}</td></tr>
          <tr><td><b>By</b></td><td>${ticket.handledBy || '-'}</td></tr>
          <tr><td><b>Note</b></td><td>${ticket.resolutionNote || '-'}</td></tr>
        </table>`;
    }

    const actions = document.getElementById('modalActions');
    if (!actions) return;
    actions.innerHTML = '';

    const canEdit = ['OPEN','IN_PROGRESS','ON_HOLD','REOPENED'].includes(ticket.status);
    const canReopen = ['RESOLVED','CLOSED'].includes(ticket.status);

    if (canEdit) {
      actions.appendChild(makeForm('resolve', ticket.id));
      actions.appendChild(makeForm('close', ticket.id));
    }
    if (canReopen) {
      actions.appendChild(makeButton('reopen', ticket.id));
    }
  }

  function makeForm(action, id) {
    const form = document.createElement('form');
    form.className = 'modal-action-form';
    form.innerHTML = `
      <label>Note:</label>
      <textarea name="resolutionNote" rows="2" required></textarea>
      <button type="submit">${action}</button>
    `;
    form.onsubmit = async e => {
      e.preventDefault();
      const note = form.querySelector('textarea').value.trim();
      if (!note) return showToast('Resolution note required', 'warning');
      try {
        await handleTicketAction(id, action, note);
        showToast(`Ticket ${action}d!`, 'success');
        document.getElementById('ticketModal').style.display = 'none';
        await initialLoad();
      } catch (err) {
        console.error(err);
        showToast(`Failed to ${action} ticket`, 'error');
      }
    };
    return form;
  }

  function makeButton(action, id) {
    const btn = document.createElement('button');
    btn.type = 'button';
    btn.textContent = action;
    btn.onclick = async () => {
      try {
        await handleTicketAction(id, action);
        showToast(`Ticket ${action}d!`, 'success');
        document.getElementById('ticketModal').style.display = 'none';
        await initialLoad();
      } catch (err) {
        console.error(err);
        showToast(`Failed to ${action} ticket`, 'error');
      }
    };
    return btn;
  }

  // === Event Listeners ===
  document.getElementById('ticketsTableBody')?.addEventListener('click', async e => {
    if (e.target.matches('.action-btn')) {
      const id = e.target.dataset.id;
      try {
        const res = await apiFetch(`${API_BASE}/${id}`);
        if (!res.ok) throw new Error();
        const ticket = await res.json();
        showModal(ticket);
      } catch {
        showToast('Failed to load ticket details', 'error');
      }
    }
  });

  document.getElementById('closeModal')?.addEventListener('click', () => {
    document.getElementById('ticketModal').style.display = 'none';
  });

  window.addEventListener('click', e => {
    const modal = document.getElementById('ticketModal');
    if (e.target === modal) modal.style.display = 'none';
  });

  document.getElementById('statusFilter')?.addEventListener('change', async e => {
    statusFilter = e.target.value;
    await initialLoad();
  });

  document.getElementById('searchBtn')?.addEventListener('click', async () => {
    const input = document.getElementById('searchInput');
    searchTerm = input ? input.value.trim() : '';
    await initialLoad();
  });

  // === Initial Load ===
  async function initialLoad() {
    try {
      await fetchMetrics();
      renderMetrics();
      await fetchTickets(0);
      renderTicketsTable();
      await renderTicketsChart();
    } catch (err) {
      console.error('Initial load failed', err);
      showToast('Dashboard load error', 'error');
    }
  }

  await initialLoad();
});
