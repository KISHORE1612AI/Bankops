// auditor-dashboard.js
// Production-ready: Robust JWT/session, refresh, expiry, inactivity, and role checks.
// Mirrors best practices from customer-support-dashboard.js.
// Uses explicit backend API URL for local frontend-backend separation.

document.addEventListener('DOMContentLoaded', () => {
  // ==== Config ====
  const ACCESS_TOKEN_KEY   = "token";
  const REFRESH_TOKEN_KEY  = "refreshToken";
  const REFRESH_COUNT_KEY  = "refreshCount";
  const LAST_ACCESS_KEY    = "lastAccessTime";
  const MAX_REFRESH_COUNT  = 2;
  const IDLE_TIMEOUT_MS    = 5 * 60 * 1000; // 5 min inactivity
  const API_BASE           = "http://localhost:8080"; // <--- IMPORTANT: backend base URL

  // ==== Session State ====
  let accessToken   = localStorage.getItem(ACCESS_TOKEN_KEY);
  let refreshToken  = localStorage.getItem(REFRESH_TOKEN_KEY);
  let refreshCount  = parseInt(localStorage.getItem(REFRESH_COUNT_KEY) || "0");
  let lastAccess    = parseInt(localStorage.getItem(LAST_ACCESS_KEY) || "0");

  // ==== JWT Helpers ====
  function parseJwt(token) {
    try {
      const base64Url = token.split('.')[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      const jsonPayload = decodeURIComponent(
        atob(base64)
          .split('')
          .map(c => '%' + ('00' + c.charCodeAt(0).toString(16)).slice(-2))
          .join('')
      );
      return JSON.parse(jsonPayload);
    } catch {
      return null;
    }
  }

  // ==== Logout Helper ====
  function handleLogout(msg = "Session expired. Please log in again.") {
    localStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem(REFRESH_TOKEN_KEY);
    localStorage.removeItem(REFRESH_COUNT_KEY);
    localStorage.removeItem(LAST_ACCESS_KEY);
    alert(msg);
    window.location.href = "employee-login.html";
  }

  // ==== Token Validation and Refresh ====
  async function ensureValidToken() {
    let decoded = parseJwt(accessToken);
    const now = Date.now();

    // (1) If no token or refreshToken, force logout
    if (!accessToken || !refreshToken) {
      handleLogout("You must be logged in as an Auditor!");
      return false;
    }

    // (2) If token is malformed, try refresh
    if (!decoded) {
      const ok = await refreshAccessToken();
      if (!ok) return false;
      accessToken = localStorage.getItem(ACCESS_TOKEN_KEY);
      decoded = parseJwt(accessToken);
      if (!decoded) {
        handleLogout("Invalid token after refresh. Please log in again.");
        return false;
      }
    }

    // (3) Check role
    if (decoded.role !== "AUDITOR") {
      handleLogout("Access denied. Only AUDITOR can access this dashboard.");
      return false;
    }

    // (4) Check expiry (exp is in seconds)
    const expiryTime = decoded.exp * 1000;
    if (now >= expiryTime) {
      const idleTime = now - (lastAccess || expiryTime);
      if (idleTime > IDLE_TIMEOUT_MS || isNaN(lastAccess)) {
        handleLogout("Session expired due to inactivity. Please log in again.");
        return false;
      }
      if (refreshCount >= MAX_REFRESH_COUNT) {
        handleLogout("Session expired. Please log in again.");
        return false;
      }
      const success = await refreshAccessToken();
      if (!success) {
        handleLogout("Session refresh failed. Please log in again.");
        return false;
      }
    } else {
      // Still valid—update lastAccessTime
      localStorage.setItem(LAST_ACCESS_KEY, now.toString());
    }
    return true;
  }

  // ==== Refresh Access Token ====
  async function refreshAccessToken() {
    try {
      const res = await fetch(API_BASE + "/auth/refresh-token", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ refreshToken })
      });
      if (!res.ok) return false;
      const data = await res.json();
      localStorage.setItem(ACCESS_TOKEN_KEY, data.accessToken);
      localStorage.setItem(REFRESH_TOKEN_KEY, data.refreshToken || refreshToken);
      refreshCount++;
      localStorage.setItem(REFRESH_COUNT_KEY, refreshCount.toString());
      localStorage.setItem(LAST_ACCESS_KEY, Date.now().toString());
      accessToken = data.accessToken;
      refreshToken = data.refreshToken || refreshToken;
      return true;
    } catch (err) {
      return false;
    }
  }

  // ==== Idle Session Timeout ====
  let idleTimer;
  function resetIdleTimer() {
    clearTimeout(idleTimer);
    idleTimer = setTimeout(() => {
      handleLogout("Session expired due to inactivity.");
    }, IDLE_TIMEOUT_MS);
    localStorage.setItem(LAST_ACCESS_KEY, Date.now().toString());
  }
  document.onmousemove = resetIdleTimer;
  document.onkeydown = resetIdleTimer;

  // ==== Authenticated API Fetch ====
  async function fetchWithAuth(url, opts = {}) {
    if (!(await ensureValidToken())) return Promise.reject(new Error("Session invalid"));
    opts.headers = opts.headers || {};
    opts.headers["Authorization"] = `Bearer ${accessToken}`;
    try {
      const res = await fetch(url, opts);
      if (res.status === 401) {
        const refreshed = await refreshAccessToken();
        if (refreshed) return fetchWithAuth(url, opts);
        else handleLogout();
      }
      if (!res.ok) throw new Error(await res.text());
      return await res.json();
    } catch (e) {
      throw e;
    }
  }

  // ==== DOM Utilities ====
  const $ = (id) => document.getElementById(id);

  // ==== Dashboard Summary ====
  async function fetchDashboardOverview() {
    try {
      const data = await fetchWithAuth(API_BASE + "/api/auditor/dashboard");
      $("total-customers").textContent = data.totalCustomers ?? 0;
      $("total-deposits").textContent = formatCurrency(data.totalDeposits ?? 0);
      $("total-transactions").textContent = data.totalTransactions ?? 0;
    } catch (e) {
      showError("overview", e);
    }
  }

  // ==== State ====
  let loginActivityList = [];
  let txnList = [];
  let loginPage = 0, loginPageSize = 5, loginHasMore = true, loginFilters = {};
  let txnPage = 0, txnPageSize = 5, txnHasMore = true, txnFilters = {};

  // ==== Filtering Handlers ====
  function getLoginFilters() {
    const filters = {};
    const dateVal = $("loginFilterDate")?.value;
    if (dateVal) filters.date = dateVal;
    const emailVal = $("loginFilterEmail")?.value.trim();
    if (emailVal) filters.email = emailVal;
    return filters;
  }
  function getTxnFilters() {
    // Only include params if set, never send empty string for date/customer/type/status
    const filters = {};
    const dateVal = $("txnFilterDate")?.value;
    if (dateVal) filters.date = dateVal;
    const customerVal = $("txnFilterCustomer")?.value.trim();
    if (customerVal) filters.customer = customerVal;
    const typeVal = $("txnFilterType")?.value;
    if (typeVal) filters.type = typeVal;
    const statusVal = $("txnFilterStatus")?.value;
    if (statusVal) filters.status = statusVal;
    return filters;
  }

  // ==== Fetch & Render: Login Activities ====
  async function fetchLoginActivities({ reset = false } = {}) {
    if (reset) {
      loginPage = 0;
      loginHasMore = true;
      loginActivityList = [];
      $("login-activity-body").innerHTML = "";
    }
    if (!loginHasMore) return;
    const params = new URLSearchParams({
      page: loginPage,
      size: loginPageSize,
      ...getLoginFilters()
    });
    try {
      const data = await fetchWithAuth(`${API_BASE}/api/auditor/login-activities?${params}`);
      if (Array.isArray(data.content)) {
        loginActivityList = reset ? data.content : loginActivityList.concat(data.content);
        renderLoginActivities(data.content, { append: !reset });
        loginHasMore = !data.last;
        $("showMoreLoginBtn").style.display = loginHasMore ? "" : "none";
        if (reset) renderLoginChart(loginActivityList);
      }
    } catch (e) {
      showError("login-activity", e);
    }
  }

  function renderLoginActivities(list, { append = false } = {}) {
    const tbody = $("login-activity-body");
    if (!append) tbody.innerHTML = "";
    for (const activity of list) {
      const tr = document.createElement("tr");
      tr.innerHTML = `
        <td>${activity.customerEmail}</td>
        <td>${formatDate(activity.timestamp)}</td>
        <td>${activity.ipAddress || '-'}</td>
        <td>${activity.device || '-'}</td>
      `;
      tr.addEventListener("click", () => showLogDetailModal(activity, "login"));
      tbody.appendChild(tr);
    }
  }

  // ==== Fetch & Render: Transactions ====
  async function fetchTransactions({ reset = false } = {}) {
    if (reset) {
      txnPage = 0;
      txnHasMore = true;
      txnList = [];
      $("transactions-body").innerHTML = "";
    }
    if (!txnHasMore) return;
    const params = new URLSearchParams({
      page: txnPage,
      size: txnPageSize,
      ...getTxnFilters()
    });
    try {
      const data = await fetchWithAuth(`${API_BASE}/api/auditor/transactions?${params}`);
      if (Array.isArray(data.content)) {
        txnList = reset ? data.content : txnList.concat(data.content);
        renderTransactions(data.content, { append: !reset });
        txnHasMore = !data.last;
        $("showMoreTxnBtn").style.display = txnHasMore ? "" : "none";
        if (reset) renderTxnChart(txnList);
      }
    } catch (e) {
      showError("transactions", e);
    }
  }

  function renderTransactions(list, { append = false } = {}) {
    const tbody = $("transactions-body");
    if (!append) tbody.innerHTML = "";
    for (const txn of list) {
      const tr = document.createElement("tr");
      tr.innerHTML = `
        <td>${txn.id}</td>
        <td>${txn.customerId}</td>
        <td>${txn.type}</td>
        <td>${formatCurrency(txn.amount)}</td>
        <td>${formatDate(txn.timestamp)}</td>
        <td>${txn.status}</td>
      `;
      tr.addEventListener("click", () => showLogDetailModal(txn, "txn"));
      tbody.appendChild(tr);
    }
  }

  // ==== Charts ====
  function renderLoginChart(data) {
    if (!window.loginChartObj) {
      window.loginChartObj = new Chart($("loginChart"), {
        type: "bar",
        data: { labels: [], datasets: [{ label: "Logins", data: [], backgroundColor: "#3498db" }] },
        options: {
          responsive: true,
          plugins: { title: { display: true, text: "Login Activity (by Day)" } },
          scales: { y: { beginAtZero: true } }
        }
      });
    }
    const counts = {};
    data.forEach(d => {
      const day = new Date(d.timestamp).toLocaleDateString();
      counts[day] = (counts[day] || 0) + 1;
    });
    const labels = Object.keys(counts);
    const dataset = Object.values(counts);
    window.loginChartObj.data.labels = labels;
    window.loginChartObj.data.datasets[0].data = dataset;
    window.loginChartObj.update();
  }

  function renderTxnChart(data) {
    if (!window.txnChartObj) {
      window.txnChartObj = new Chart($("txnChart"), {
        type: "doughnut",
        data: { labels: [], datasets: [{ label: "Txn Types", data: [], backgroundColor: ['#2ecc71', '#e74c3c', '#f1c40f'] }] },
        options: {
          responsive: true,
          plugins: { title: { display: true, text: "Transaction Breakdown" } }
        }
      });
    }
    const counts = { DEPOSIT: 0, TRANSFER_IN: 0, TRANSFER_OUT: 0 };
    data.forEach(d => { if (counts[d.type] !== undefined) counts[d.type]++; });
    window.txnChartObj.data.labels = Object.keys(counts);
    window.txnChartObj.data.datasets[0].data = Object.values(counts);
    window.txnChartObj.update();
  }

  // ==== CSV Export ====
  function exportCSV(data, filename, headers, prettyHeaders = null) {
    if (!data.length) return alert("No data to export!");
    const rows = [];
    if (prettyHeaders) rows.push(prettyHeaders.join(","));
    else rows.push(headers.join(","));
    data.forEach(row => {
      rows.push(headers.map(h => `"${row[h] ?? ''}"`).join(","));
    });
    const blob = new Blob([rows.join("\n")], { type: "text/csv" });
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = filename;
    a.click();
    setTimeout(() => URL.revokeObjectURL(url), 200);
  }

  $("exportLoginBtn")?.addEventListener("click", () => {
    exportCSV(
      loginActivityList,
      "login_activities.csv",
      ["customerEmail", "timestamp", "ipAddress", "device"],
      ["Email", "Timestamp", "IP Address", "Device"]
    );
  });
  $("exportTxnBtn")?.addEventListener("click", () => {
    exportCSV(
      txnList,
      "transactions.csv",
      ["id", "customerId", "type", "amount", "timestamp", "status"],
      ["Txn ID", "Customer ID", "Type", "Amount", "Timestamp", "Status"]
    );
  });

  // ==== Filtering ====
  $("loginFilterBtn")?.addEventListener("click", () => {
    loginFilters = getLoginFilters();
    fetchLoginActivities({ reset: true });
  });
  $("loginClearBtn")?.addEventListener("click", () => {
    $("loginFilterDate").value = "";
    $("loginFilterEmail").value = "";
    loginFilters = {};
    fetchLoginActivities({ reset: true });
  });
  $("txnFilterBtn")?.addEventListener("click", () => {
    txnFilters = getTxnFilters();
    fetchTransactions({ reset: true });
  });
  $("txnClearBtn")?.addEventListener("click", () => {
    $("txnFilterDate").value = "";
    $("txnFilterCustomer").value = "";
    $("txnFilterType").value = "";
    $("txnFilterStatus").value = "";
    txnFilters = {};
    fetchTransactions({ reset: true });
  });

  // ==== Pagination (Show More) ====
  $("showMoreLoginBtn")?.addEventListener("click", () => {
    loginPage++;
    fetchLoginActivities();
  });
  $("showMoreTxnBtn")?.addEventListener("click", () => {
    txnPage++;
    fetchTransactions();
  });

  // ==== Audit Log Details (Modal) ====
  function showLogDetailModal(row, type) {
    $("logDetailLabel").textContent = type === "login" ? "Login Activity Details" : "Transaction Details";
    let html = "<table class='table table-sm'>";
    for (const [key, value] of Object.entries(row)) {
      html += `<tr><th>${key}</th><td>${value !== null && value !== undefined ? value : '-'}</td></tr>`;
    }
    html += "</table>";
    $("logDetailBody").innerHTML = html;
    const modal = new bootstrap.Modal(document.getElementById("logDetailModal"));
    modal.show();
  }

  // ==== Formatting ====
  function formatDate(dt) {
    if (!dt) return '-';
    const d = new Date(dt);
    return d.toLocaleString();
  }
  function formatCurrency(amount) {
    if (amount === undefined || amount === null) return "-";
    return new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR" }).format(amount);
  }

  // ==== Error Handling ====
  function showError(section, error) {
    alert(`Error loading ${section}: ${error?.message || error}`);
  }

  $("logoutBtn")?.addEventListener("click", () => handleLogout("Logged out."));

  // ==== INIT ====
  (async function init() {
    resetIdleTimer();
    if (!(await ensureValidToken())) return;
    await fetchDashboardOverview();
    await fetchLoginActivities({ reset: true });
    await fetchTransactions({ reset: true });
  })();
});