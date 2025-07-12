// loan-officer-dashboard.js

document.addEventListener('DOMContentLoaded', async () => {
  // ==== CONFIG ====
  const API_BASE = "http://localhost:8080";

  // ==== AUTHENTICATION & SESSION MANAGEMENT ====
  const MAX_REFRESH_COUNT = 2;
  const IDLE_TIMEOUT_MS = 2 * 60 * 1000; // 2 minutes

  const token = localStorage.getItem('token');
  const refreshToken = localStorage.getItem('refreshToken');
  let refreshCount = parseInt(localStorage.getItem('refreshCount') || '0');
  let lastAccess = parseInt(localStorage.getItem('lastAccessTime') || '0');

  if (!token || !refreshToken) {
    return handleLogout('You must be logged in!');
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

  if (!decoded) {
    const ok = await refreshAccessToken();
    if (!ok) return handleLogout('Session expired. Please log in again.');
    decoded = parseJwt(localStorage.getItem('token'));
    if (!decoded) return handleLogout('Invalid token after refresh. Please log in again.');
  }

  if (decoded.role !== 'LOAN_OFFICER') {
    return handleLogout('Access denied. Only LOAN_OFFICER can access this page.');
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
    if (!decoded || decoded.role !== 'LOAN_OFFICER') {
      return handleLogout('Invalid token after refresh. Please log in again.');
    }
  } else {
    localStorage.setItem('lastAccessTime', now.toString());
  }

  // ==== MAIN DASHBOARD LOGIC ====

  // API Helper
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

  // === 1. Loan Metric Cards ===
  async function updateLoanMetrics() {
    try {
      const counts = await api('/api/loans/dashboard/counts');
      document.getElementById('newLoanRequests').textContent = counts.PENDING || 0;
      document.getElementById('loansInProcess').textContent = counts.UNDER_REVIEW || 0;
      document.getElementById('approvedLoans').textContent = counts.APPROVED || 0;
      document.getElementById('rejectedLoans').textContent = counts.REJECTED || 0;
    } catch { /* error already handled */ }
  }

  // === 2. Loan Applications Over Time Chart ===
  async function renderLoanApplicationsChart() {
    try {
      const data = await api('/api/loans/dashboard/monthly-applications?monthsBack=6');
      const labels = data.map(row => `${row.year}-${row.month.toString().padStart(2, '0')}`);
      const counts = data.map(row => row.count);

      const ctx = document.getElementById('loanApplicationsChart').getContext('2d');
      if (window.loanChart) window.loanChart.destroy(); // Prevent double charts
      window.loanChart = new Chart(ctx, {
        type: 'bar',
        data: {
          labels,
          datasets: [{
            label: 'Loan Applications',
            data: counts,
            backgroundColor: '#3498db'
          }]
        },
        options: {
          responsive: true,
          plugins: {
            title: {
              display: true,
              text: 'Monthly Loan Applications'
            }
          },
          scales: {
            y: { beginAtZero: true }
          }
        }
      });
    } catch {}
  }

  // === 3. Loan Requests Table ===
  async function populateLoanRequests() {
    try {
      const loans = await api('/api/loans?status=PENDING');
      const tbody = document.getElementById('loanRequestsTableBody');
      tbody.innerHTML = '';
      loans.forEach(loan => {
        const row = document.createElement('tr');
        row.innerHTML = `
          <td>${loan.id}</td>
          <td>${loan.customerName || loan.customerFullName || '-'}</td>
          <td>${loan.loanType || '-'}</td>
          <td>${formatCurrency(loan.amountRequested)}</td>
          <td>${loan.status}</td>
          <td>${formatDate(loan.appliedAt)}</td>
          <td>
            <div class="action-buttons">
              <button class="btn-action btn-process" data-id="${loan.id}">Process</button>
              <button class="btn-action btn-approve" data-id="${loan.id}">Approve</button>
              <button class="btn-action btn-reject"  data-id="${loan.id}">Reject</button>
            </div>
          </td>
        `;
        tbody.appendChild(row);
      });
      // Attach event listeners
      tbody.querySelectorAll('.btn-process').forEach(btn => btn.onclick = onProcessLoan);
      tbody.querySelectorAll('.btn-approve').forEach(btn => btn.onclick = onApproveLoan);
      tbody.querySelectorAll('.btn-reject').forEach(btn => btn.onclick = onRejectLoan);
    } catch {}
  }

  // === 4. Assigned Customers Table ===
  async function populateAssignedCustomers() {
    try {
      // Fetch all loans assigned to this officer and not final status
      const officer = decoded.sub || decoded.empId || decoded.email; // adjust as per backend
      const loans = await api('/api/loans?status=UNDER_REVIEW');
      const tbody = document.getElementById('assignedCustomersTableBody');
      tbody.innerHTML = '';
      loans.forEach(loan => {
        // Only show if reviewedBy is this officer
        if (loan.reviewedBy !== officer) return;
        const row = document.createElement('tr');
        row.innerHTML = `
          <td>${loan.customerId || '-'}</td>
          <td>${loan.customerName || loan.customerFullName || '-'}</td>
          <td>${loan.loanType || '-'}</td>
          <td>${loan.status}</td>
          <td>
            <div class="action-buttons">
              <button class="btn-action btn-approve" data-id="${loan.id}">Approve</button>
              <button class="btn-action btn-reject"  data-id="${loan.id}">Reject</button>
            </div>
          </td>
        `;
        tbody.appendChild(row);
      });
      tbody.querySelectorAll('.btn-approve').forEach(btn => btn.onclick = onApproveLoan);
      tbody.querySelectorAll('.btn-reject').forEach(btn => btn.onclick = onRejectLoan);
    } catch {}
  }

  // === 5. Report Submission Form ===
  function setupReportSubmission() {
    const form = document.getElementById('reportForm');
    if (!form) return;
    form.addEventListener('submit', async e => {
      e.preventDefault();
      const content = document.getElementById('reportContent').value.trim();
      if (!content) {
        alert('Please enter report details.');
        return;
      }
      try {
        await api('/api/reports', {
          method: 'POST',
          body: JSON.stringify({
            officer: decoded.sub || decoded.email || decoded.empId,
            content
          })
        });
        document.getElementById('reportSuccess').style.display = 'block';
        setTimeout(() => document.getElementById('reportSuccess').style.display = 'none', 3000);
        form.reset();
      } catch {
        alert('Failed to submit report.');
      }
    });
  }

  // ==== --- ACTION HANDLERS --- ====

  // Disable UI buttons while API is running, re-enable after
  function setButtonsDisabled(disabled) {
    document.querySelectorAll('.btn-action').forEach(btn => btn.disabled = disabled);
  }

  async function handleLoanAction(fn, successMsg) {
    setButtonsDisabled(true);
    try {
      await fn();
      // Immediately update all data for live UI feedback
      await updateLoanMetrics();
      await populateLoanRequests();
      await populateAssignedCustomers();
      if (typeof renderLoanApplicationsChart === 'function') await renderLoanApplicationsChart();
      if (successMsg) alert(successMsg);
    } catch (err) {
      // Error already alerted in api()
    } finally {
      setButtonsDisabled(false);
    }
  }

  async function onProcessLoan(e) {
    const loanId = e.target.dataset.id;
    if (!confirm('Mark this loan as In Process?')) return;
    await handleLoanAction(
      async () => { await api(`/api/loans/${loanId}/process`, { method: 'PUT' }); },
      'Loan marked as In Process.'
    );
  }

  async function onApproveLoan(e) {
    const loanId = e.target.dataset.id;
    if (!confirm('Approve this loan?')) return;
    await handleLoanAction(
      async () => { await api(`/api/loans/${loanId}/approve`, { method: 'PUT' }); },
      'Loan approved.'
    );
  }

  async function onRejectLoan(e) {
    const loanId = e.target.dataset.id;
    if (!confirm('Reject this loan?')) return;
    await handleLoanAction(
      async () => { await api(`/api/loans/${loanId}/reject`, { method: 'PUT' }); },
      'Loan rejected.'
    );
  }

  // ==== --- UTILITIES --- ====

  function formatCurrency(amount) {
    if (!amount) return '-';
    return Number(amount).toLocaleString('en-IN', {
      style: 'currency', currency: 'INR', maximumFractionDigits: 0
    });
  }

  function formatDate(dateStr) {
    if (!dateStr) return '-';
    const d = new Date(dateStr);
    return d.toLocaleDateString('en-IN', { day: '2-digit', month: 'short', year: 'numeric' });
  }

  // ==== INITIAL LOAD ====
  await updateLoanMetrics();
  await renderLoanApplicationsChart();
  await populateLoanRequests();
  await populateAssignedCustomers();
  setupReportSubmission();
});