// ==== CONFIGURATION ====
const API_BASE = "http://localhost:8080"; // Adjust as per deployment

// ==== AUTHENTICATION & SESSION MANAGEMENT ====
const MAX_REFRESH_COUNT = 2;
const IDLE_TIMEOUT_MS = 2 * 60 * 1000; // 2 minutes

const token = localStorage.getItem('token');
const refreshToken = localStorage.getItem('refreshToken');
let refreshCount = parseInt(localStorage.getItem('refreshCount') || '0');
let lastAccess = parseInt(localStorage.getItem('lastAccessTime') || '0');

if (!token || !refreshToken) {
  handleLogout('You must be logged in!');
}

function parseJwt(t) {
  try {
    const base64Url = t.split('.')[1];
    const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
    const json = decodeURIComponent(
      atob(base64)
        .split('')
        .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
        .join('')
    );
    return JSON.parse(json);
  } catch {
    return null;
  }
}

async function refreshAccessToken() {
  try {
    const response = await fetch(`${API_BASE}/auth/refresh-token`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ refreshToken })
    });
    if (!response.ok) throw new Error('Refresh failed');
    const data = await response.json();
    localStorage.setItem('token', data.accessToken);
    localStorage.setItem('refreshToken', data.refreshToken);
    refreshCount++;
    localStorage.setItem('refreshCount', refreshCount.toString());
    localStorage.setItem('lastAccessTime', Date.now().toString());
    return true;
  } catch (err) {
    console.error('Refresh error:', err);
    return false;
  }
}

function handleLogout(message) {
  if (message) alert(message);
  localStorage.clear();
  window.location.href = '/employee-login.html';
}

let decoded = parseJwt(token);
const now = Date.now();

(async function authenticateAndLoad() {
  if (!decoded) {
    const ok = await refreshAccessToken();
    if (!ok) return handleLogout('Session expired. Please log in again.');
    decoded = parseJwt(localStorage.getItem('token'));
    if (!decoded) return handleLogout('Invalid token after refresh. Please log in again.');
  }

  if (decoded.role !== 'SUPER_ADMIN') {
    return handleLogout('Access denied. Only SUPER_ADMIN can access this page.');
  }

  const expiryTime = decoded.exp * 1000;
  if (now >= expiryTime) {
    const idleTime = now - expiryTime;
    if (idleTime > IDLE_TIMEOUT_MS || isNaN(lastAccess)) {
      return handleLogout('Session expired due to inactivity. Please log in again.');
    }
    if (refreshCount >= MAX_REFRESH_COUNT) {
      return handleLogout('Session expired. Please log in again.');
    }
    const success = await refreshAccessToken();
    if (!success) {
      return handleLogout('Session refresh failed. Please log in again.');
    }
    decoded = parseJwt(localStorage.getItem('token'));
    if (!decoded || decoded.role !== 'SUPER_ADMIN') {
      return handleLogout('Invalid token after refresh. Please log in again.');
    }
  } else {
    localStorage.setItem('lastAccessTime', now.toString());
  }

  // ==== MAIN DASHBOARD LOGIC ====
  window.loadDashboard = loadDashboard; // Make globally available for refresh button
  loadDashboard();

  // ==== Set User Info ====
  if (decoded && decoded.sub) {
    localStorage.setItem('currentUserName', decoded.sub);
    const userElem = document.getElementById('currentUser');
    if (userElem) userElem.textContent = decoded.sub;
  }
})();

// ==== API Helper ====
async function api(url, opts = {}) {
  const headers = opts.headers || {};
  headers['Authorization'] = 'Bearer ' + localStorage.getItem('token');
  headers['Content-Type'] = headers['Content-Type'] || 'application/json';
  try {
    const res = await fetch(`${API_BASE}${url}`, { ...opts, headers });
    if (res.status === 401) {
      // Try refresh token once, then retry
      const refreshed = await refreshAccessToken();
      if (refreshed) {
        headers['Authorization'] = 'Bearer ' + localStorage.getItem('token');
        const retryRes = await fetch(`${API_BASE}${url}`, { ...opts, headers });
        if (!retryRes.ok) throw new Error('API error after refresh');
        return retryRes.json();
      } else {
        handleLogout('Session expired. Please log in again.');
      }
    }
    if (!res.ok) throw new Error(await res.text());
    return res.json();
  } catch (err) {
    console.error('API error:', err);
    alert('API error: ' + err.message);
    throw err;
  }
}

// ==== INR Currency Formatting ====
function formatCurrency(num) {
  if (typeof num === "number") {
    return num.toLocaleString('en-IN', {style:'currency', currency:'INR', maximumFractionDigits:2});
  }
  if (!num) return "₹0.00";
  // fallback: for string, BigDecimal, etc.
  return "₹" + num.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ",");
}

// ==== Dashboard Loader ====
async function loadDashboard() {
  const loader = document.getElementById('loader');
  const cards = document.getElementById('dashboardCards');
  const refreshBtn = document.getElementById('refreshBtn');
  loader.style.display = 'block';
  cards.style.display = 'none';
  refreshBtn.disabled = true;

  try {
    const data = await api('/api/superadmin/dashboard/summary');
    document.getElementById('totalUsers').textContent = data.totalUsers ?? "-";
    document.getElementById('activeUsers').textContent = data.activeUsers ?? "-";
    document.getElementById('totalEmployees').textContent = data.totalEmployees ?? "-";
    document.getElementById('totalLoans').textContent = data.totalLoans ?? "-";
    document.getElementById('pendingTickets').textContent = data.pendingTickets ?? "-";
    document.getElementById('totalTransactions').textContent = data.totalTransactions ?? "-";
    loader.style.display = 'none';
    cards.style.display = 'block';
    refreshBtn.disabled = false;
  } catch (err) {
    loader.style.display = 'none';
    cards.style.display = 'none';
    refreshBtn.disabled = false;
    alert("Could not load dashboard stats. Please try again.\n" + err);
  }
}

// ==== Modal: Loans ====
window.showLoansModal = async function () {
  const modal = new bootstrap.Modal(document.getElementById('loansModal'));
  document.getElementById('loansModalLoader').style.display = '';
  document.getElementById('loansModalContent').style.display = 'none';
  document.getElementById('loansModalEmpty').classList.add('d-none');
  document.getElementById('loansModalTableBody').innerHTML = '';
  modal.show();
  try {
    const loans = await api('/api/superadmin/loans');
    if (!loans || loans.length === 0) {
      document.getElementById('loansModalEmpty').classList.remove('d-none');
    } else {
      let html = '';
      loans.forEach(loan => {
        html += `<tr>
          <td>${loan.id}</td>
          <td>${loan.customerName || '-'}</td>
          <td>${loan.loanType || '-'}</td>
          <td>${formatCurrency(loan.amountRequested)}</td>
          <td>${loan.status || '-'}</td>
          <td>${formatDate(loan.appliedAt)}</td>
        </tr>`;
      });
      document.getElementById('loansModalTableBody').innerHTML = html;
      document.getElementById('loansModalContent').style.display = '';
    }
  } catch (err) {
    document.getElementById('loansModalEmpty').classList.remove('d-none');
    document.getElementById('loansModalEmpty').textContent = "Failed to load loans.";
  } finally {
    document.getElementById('loansModalLoader').style.display = 'none';
  }
};

// ==== Modal: Transactions ====
window.showTransactionsModal = async function () {
  const modal = new bootstrap.Modal(document.getElementById('transactionsModal'));
  document.getElementById('transactionsModalLoader').style.display = '';
  document.getElementById('transactionsModalContent').style.display = 'none';
  document.getElementById('transactionsModalEmpty').classList.add('d-none');
  document.getElementById('transactionsModalTableBody').innerHTML = '';
  modal.show();
  try {
    const txns = await api('/api/superadmin/transactions');
    if (!txns || txns.length === 0) {
      document.getElementById('transactionsModalEmpty').classList.remove('d-none');
    } else {
      let html = '';
      txns.forEach(txn => {
        html += `<tr>
          <td>${txn.id}</td>
          <td>${txn.customerId || '-'}</td>
          <td>${txn.type || '-'}</td>
          <td>${formatCurrency(txn.amount)}</td>
          <td>${formatDate(txn.timestamp)}</td>
          <td>${txn.status || '-'}</td>
        </tr>`;
      });
      document.getElementById('transactionsModalTableBody').innerHTML = html;
      document.getElementById('transactionsModalContent').style.display = '';
    }
  } catch (err) {
    document.getElementById('transactionsModalEmpty').classList.remove('d-none');
    document.getElementById('transactionsModalEmpty').textContent = "Failed to load transactions.";
  } finally {
    document.getElementById('transactionsModalLoader').style.display = 'none';
  }
};

// ==== Date Formatting ====
function formatDate(dateStr) {
  if (!dateStr) return "-";
  const d = new Date(dateStr);
  return d.toLocaleString('en-IN', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' });
}

// ==== Improved Navigation Handlers ====
window.goToUsers = function () {
  window.location.href = '/super-admin-users.html';
};

window.goToEmployees = function () {
  window.location.href = '/super-admin-employees.html';
};

window.goToTickets = function () {
  window.location.href = '/super-admin-tickets.html';
};

// ==== Logout Handler (used in html) ====
window.logout = function () {
  if (confirm("Are you sure you want to log out?")) {
    localStorage.clear();
    window.location.href = '/employee-login.html';
  }
};