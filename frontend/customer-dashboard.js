// customer-dashboard.js
// Production-ready, supports "View More" modals with date filtering for Transactions, Loans, and Support Tickets.

console.log("customer-dashboard.js loaded!");

const API_BASE = "http://localhost:8080";
const ACCESS_TOKEN_KEY = "accessToken";
const REFRESH_TOKEN_KEY = "refreshToken";
const IDLE_TIMEOUT_MS = 2 * 60 * 1000; // 2 min inactivity

// ========== Session and Auth =============
function logout(msg = "Session expired. Please log in again.") {
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
  alert(msg);
  window.location.href = "customer-login.html";
}

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

async function refreshAccessToken() {
  const refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY);
  if (!refreshToken) return false;
  try {
    const res = await fetch(`${API_BASE}/auth/refresh-token`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ refreshToken })
    });
    if (!res.ok) return false;
    const data = await res.json();
    localStorage.setItem(ACCESS_TOKEN_KEY, data.accessToken);
    localStorage.setItem(REFRESH_TOKEN_KEY, data.refreshToken || refreshToken);
    return true;
  } catch {
    return false;
  }
}

async function ensureValidToken() {
  let token = localStorage.getItem(ACCESS_TOKEN_KEY);
  let refreshToken = localStorage.getItem(REFRESH_TOKEN_KEY);
  if (!token || !refreshToken) {
    logout("Please log in!");
    return false;
  }
  let decoded = parseJwt(token);
  const now = Date.now();
  if (!decoded) {
    if (await refreshAccessToken()) {
      token = localStorage.getItem(ACCESS_TOKEN_KEY);
      decoded = parseJwt(token);
      if (!decoded) {
        logout("Session error. Please log in again.");
        return false;
      }
    } else {
      logout("Session expired. Please log in again.");
      return false;
    }
  }
  if (decoded.role !== "CUSTOMER") {
    logout("Access denied.");
    return false;
  }
  if (now >= decoded.exp * 1000) {
    if (await refreshAccessToken()) {
      return true;
    } else {
      logout("Session expired. Please log in again.");
      return false;
    }
  }
  return true;
}

async function fetchWithAuth(url, opts = {}) {
  if (!(await ensureValidToken())) throw new Error("Not authenticated");
  const token = localStorage.getItem(ACCESS_TOKEN_KEY);
  opts.headers = opts.headers || {};
  opts.headers["Authorization"] = `Bearer ${token}`;
  const res = await fetch(url, opts);
  if (res.status === 401) {
    if (await refreshAccessToken()) {
      return fetchWithAuth(url, opts);
    } else {
      logout();
      throw new Error("Session expired");
    }
  }
  return res;
}

// ===== Idle timeout =====
let idleTimer;
function resetIdleTimer() {
  clearTimeout(idleTimer);
  idleTimer = setTimeout(() => {
    logout("Session expired due to inactivity.");
  }, IDLE_TIMEOUT_MS);
}
document.onmousemove = resetIdleTimer;
document.onkeydown = resetIdleTimer;

// ========== Formatting ============
function formatCurrency(amount) {
  if (amount === undefined || amount === null) return "-";
  return new Intl.NumberFormat("en-IN", { style: "currency", currency: "INR" }).format(amount);
}
function formatDate(dt) {
  if (!dt) return "–";
  const d = new Date(dt);
  return d.toLocaleString("en-IN");
}
function getTxnTypeLabel(type) {
  switch ((type || "").toUpperCase()) {
    case "CREDIT": return "Credit (Deposit)";
    case "DEBIT": return "Debit (Withdrawal)";
    case "TRANSFER_OUT": return "Transfer Sent";
    case "TRANSFER_IN": return "Transfer Received";
    case "DEPOSIT": return "Deposit";
    default: return type || "–";
  }
}
function getTxnAmountClass(type) {
  switch ((type || "").toUpperCase()) {
    case "CREDIT":
    case "TRANSFER_IN":
    case "DEPOSIT":
      return "credit";
    case "DEBIT":
    case "TRANSFER_OUT":
      return "debit";
    default:
      return "";
  }
}

// ========== Profile =============
async function fetchProfileData() {
  try {
    const res = await fetchWithAuth(`${API_BASE}/api/customer/dashboard/profile`);
    if (!res.ok) throw new Error("Profile fetch failed");
    const data = await res.json();
    const elements = {
      "custName": data.fullName,
      "custEmail": data.email,
      "custPhone": data.phone,
      "custStatus": data.accountStatus,
      "accountNumber": data.accountNumber,
      "accountBalance": formatCurrency(data.accountBalance),
      "custLastLogin": formatDate(data.lastLogin)
    };
    Object.entries(elements).forEach(([id, value]) => {
      const element = document.getElementById(id);
      if (element) element.innerText = value || "–";
    });
    document.getElementById("custStatus").className = `status-${data.accountStatus?.toLowerCase() || 'unknown'}`;
    ["depositBtn", "transferBtn", "loanBtn", "ticketBtn"].forEach(btnId => {
      const btn = document.getElementById(btnId);
      if (btn) {
        if (String(data.accountStatus).toUpperCase() !== "ACTIVE") {
          btn.disabled = true;
          btn.title = "Only allowed for active accounts.";
        } else {
          btn.disabled = false;
          btn.title = "";
        }
      }
    });
  } catch (err) {
    ["custName","custEmail","custPhone","custStatus","accountNumber","accountBalance","custLastLogin"].forEach(id => {
      const el = document.getElementById(id);
      if (el) el.innerText = "–";
    });
    alert("Failed to load profile.");
  }
}

// ========== Recent Transactions Table ===========
let txnPage = 0, txnPageSize = 10, txnHasMore = true, txnList = [], txnLastFilters = {};

async function fetchRecentTransactions({ reset = false, ...filters } = {}) {
  if (reset) {
    txnPage = 0;
    txnHasMore = true;
    txnList = [];
    document.querySelector("#recentTransactions tbody").innerHTML = "";
  }
  if (!txnHasMore) return;
  let params = [];
  params.push(`page=${txnPage}`);
  params.push(`size=${txnPageSize}`);
  if (filters.fromDate) params.push(`fromDate=${encodeURIComponent(filters.fromDate)}`);
  if (filters.toDate) params.push(`toDate=${encodeURIComponent(filters.toDate)}`);
  if (filters.type) params.push(`type=${encodeURIComponent(filters.type)}`);
  if (filters.status) params.push(`status=${encodeURIComponent(filters.status)}`);
  if (filters.sortBy) params.push(`sortBy=${encodeURIComponent(filters.sortBy)}`);
  let query = params.length ? `?${params.join("&")}` : "";

  try {
    const res = await fetchWithAuth(`${API_BASE}/api/customer/dashboard/transactions${query}`);
    const tbody = document.querySelector("#recentTransactions tbody");
    if (!res.ok) {
      tbody.innerHTML = `<tr><td colspan="6" class="no-data text-danger">Failed to load transactions.</td></tr>`;
      txnHasMore = false;
      return;
    }
    const data = await res.json();
    let txns = Array.isArray(data.content) ? data.content : (Array.isArray(data) ? data : []);
    txnList = reset ? txns : txnList.concat(txns);

    if (txns.length === 0 && txnPage === 0) {
      tbody.innerHTML = `<tr><td colspan="6" class="text-center text-muted">No transactions found.</td></tr>`;
      txnHasMore = false;
      return;
    }
    for (const txn of txns) {
      const tr = document.createElement("tr");
      tr.innerHTML = `
        <td>${txn.id || "–"}</td>
        <td>${getTxnTypeLabel(txn.type)}</td>
        <td>${txn.description || "–"}</td>
        <td class="amount ${getTxnAmountClass(txn.type)}">${formatCurrency(txn.amount)}</td>
        <td>${formatDate(txn.timestamp)}</td>
        <td><span class="status-badge ${txn.status?.toLowerCase()}">${txn.status || "–"}</span></td>
      `;
      tbody.appendChild(tr);
    }
    txnHasMore = !(data.last === true) && txns.length === txnPageSize;
  } catch (err) {
    const tbody = document.querySelector("#recentTransactions tbody");
    if (tbody) {
      tbody.innerHTML = `<tr><td colspan="6" class="no-data text-danger">Failed to load transactions.</td></tr>`;
    }
    txnHasMore = false;
  }
}

// ========== View More Transactions Modal ===========
async function fetchAllTransactionsModal({ fromDate, toDate } = {}) {
  let params = [];
  params.push("page=0");
  params.push("size=100"); // Show up to 100 in modal
  if (fromDate) params.push(`fromDate=${encodeURIComponent(fromDate)}`);
  if (toDate) params.push(`toDate=${encodeURIComponent(toDate)}`);
  let query = params.length ? `?${params.join("&")}` : "";
  const tbody = document.getElementById("txnModalTableBody");
  tbody.innerHTML = `<tr><td colspan="6" class="text-center text-muted">Loading...</td></tr>`;
  try {
    const res = await fetchWithAuth(`${API_BASE}/api/customer/dashboard/transactions${query}`);
    if (!res.ok) {
      tbody.innerHTML = `<tr><td colspan="6" class="text-danger">Failed to load transactions.</td></tr>`;
      return;
    }
    const data = await res.json();
    const txns = Array.isArray(data.content) ? data.content : (Array.isArray(data) ? data : []);
    if (!txns || txns.length === 0) {
      tbody.innerHTML = `<tr><td colspan="6" class="text-muted text-center">No transactions found.</td></tr>`;
      return;
    }
    tbody.innerHTML = "";
    txns.forEach(txn => {
      const tr = document.createElement("tr");
      tr.innerHTML = `
        <td>${txn.id || "–"}</td>
        <td>${getTxnTypeLabel(txn.type)}</td>
        <td>${txn.description || "–"}</td>
        <td class="amount ${getTxnAmountClass(txn.type)}">${formatCurrency(txn.amount)}</td>
        <td>${formatDate(txn.timestamp)}</td>
        <td><span class="status-badge ${txn.status?.toLowerCase()}">${txn.status || "–"}</span></td>
      `;
      tbody.appendChild(tr);
    });
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="6" class="text-danger">Failed to load transactions.</td></tr>`;
  }
}

// ========== Loans and View More ===========
async function fetchOpenLoans() {
  try {
    const res = await fetchWithAuth(`${API_BASE}/api/customer/dashboard/loans`);
    const tbody = document.querySelector("#openLoans tbody");
    tbody.innerHTML = "";
    if (!res.ok) {
      tbody.innerHTML = `<tr><td colspan="8" class="no-data text-danger">Failed to load loans.</td></tr>`;
      return;
    }
    const loans = await res.json();
    if (!Array.isArray(loans) || loans.length === 0) {
      tbody.innerHTML = "";
      return;
    }
    loans.forEach(loan => {
      const row = document.createElement("tr");
      row.innerHTML = `
        <td>${loan.id || "–"}</td>
        <td>${loan.loanType || "–"}</td>
        <td class="amount">${formatCurrency(loan.amountRequested)}</td>
        <td><span class="status-badge ${loan.status?.toLowerCase()}">${loan.status || "–"}</span></td>
        <td>${formatDate(loan.appliedAt)}</td>
        <td>${loan.reviewedBy || "–"}</td>
        <td>${formatDate(loan.reviewedAt)}</td>
        <td>${loan.details || "–"}</td>
      `;
      tbody.appendChild(row);
    });
  } catch (err) {
    const tbody = document.querySelector("#openLoans tbody");
    if (tbody) {
      tbody.innerHTML = `<tr><td colspan="8" class="no-data text-danger">Failed to load loans.</td></tr>`;
    }
  }
}

async function fetchAllLoansModal({ fromDate, toDate } = {}) {
  let params = [];
  params.push("page=0");
  params.push("size=100");
  if (fromDate) params.push(`fromDate=${encodeURIComponent(fromDate)}`);
  if (toDate) params.push(`toDate=${encodeURIComponent(toDate)}`);
  let query = params.length ? `?${params.join("&")}` : "";
  const tbody = document.getElementById("loansModalTableBody");
  tbody.innerHTML = `<tr><td colspan="8" class="text-center text-muted">Loading...</td></tr>`;
  try {
    const res = await fetchWithAuth(`${API_BASE}/api/customer/dashboard/loans${query}`);
    if (!res.ok) {
      tbody.innerHTML = `<tr><td colspan="8" class="text-danger">Failed to load loans.</td></tr>`;
      return;
    }
    const loans = await res.json();
    if (!loans || loans.length === 0) {
      tbody.innerHTML = `<tr><td colspan="8" class="text-center text-muted">No loans found.</td></tr>`;
      return;
    }
    tbody.innerHTML = "";
    loans.forEach(loan => {
      const row = document.createElement("tr");
      row.innerHTML = `
        <td>${loan.id || "–"}</td>
        <td>${loan.loanType || "–"}</td>
        <td class="amount">${formatCurrency(loan.amountRequested)}</td>
        <td><span class="status-badge ${loan.status?.toLowerCase()}">${loan.status || "–"}</span></td>
        <td>${formatDate(loan.appliedAt)}</td>
        <td>${loan.reviewedBy || "–"}</td>
        <td>${formatDate(loan.reviewedAt)}</td>
        <td>${loan.details || "–"}</td>
      `;
      tbody.appendChild(row);
    });
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="8" class="text-danger">Failed to load loans.</td></tr>`;
  }
}

// ========== Tickets and View More ===========
async function fetchOpenTickets() {
  try {
    const res = await fetchWithAuth(`${API_BASE}/api/customer/dashboard/tickets`);
    const tbody = document.querySelector("#openTickets tbody");
    tbody.innerHTML = "";
    if (!res.ok) {
      tbody.innerHTML = `<tr><td colspan="8" class="no-data text-danger">Failed to load tickets.</td></tr>`;
      return;
    }
    const tickets = await res.json();
    if (!Array.isArray(tickets) || tickets.length === 0) {
      tbody.innerHTML = "";
      return;
    }
    tickets.forEach(ticket => {
      const row = document.createElement("tr");
      row.innerHTML = `
        <td>${ticket.id || "–"}</td>
        <td>${ticket.issueType || "–"}</td>
        <td>${ticket.description || "–"}</td>
        <td><span class="status-badge ${ticket.status?.toLowerCase()}">${ticket.status || "–"}</span></td>
        <td>${formatDate(ticket.createdAt)}</td>
        <td>${ticket.handledBy || "–"}</td>
        <td>${formatDate(ticket.handledAt)}</td>
        <td>${ticket.description || "–"}</td>
      `;
      tbody.appendChild(row);
    });
  } catch (err) {
    const tbody = document.querySelector("#openTickets tbody");
    if (tbody) {
      tbody.innerHTML = `<tr><td colspan="8" class="no-data text-danger">Failed to load tickets.</td></tr>`;
    }
  }
}

async function fetchAllTicketsModal({ fromDate, toDate } = {}) {
  let params = [];
  params.push("page=0");
  params.push("size=100");
  if (fromDate) params.push(`fromDate=${encodeURIComponent(fromDate)}`);
  if (toDate) params.push(`toDate=${encodeURIComponent(toDate)}`);
  let query = params.length ? `?${params.join("&")}` : "";
  const tbody = document.getElementById("ticketsModalTableBody");
  tbody.innerHTML = `<tr><td colspan="8" class="text-center text-muted">Loading...</td></tr>`;
  try {
    const res = await fetchWithAuth(`${API_BASE}/api/customer/dashboard/tickets${query}`);
    if (!res.ok) {
      tbody.innerHTML = `<tr><td colspan="8" class="text-danger">Failed to load tickets.</td></tr>`;
      return;
    }
    const tickets = await res.json();
    if (!tickets || tickets.length === 0) {
      tbody.innerHTML = `<tr><td colspan="8" class="text-center text-muted">No tickets found.</td></tr>`;
      return;
    }
    tbody.innerHTML = "";
    tickets.forEach(ticket => {
      const row = document.createElement("tr");
      row.innerHTML = `
        <td>${ticket.id || "–"}</td>
        <td>${ticket.issueType || "–"}</td>
        <td>${ticket.description || "–"}</td>
        <td><span class="status-badge ${ticket.status?.toLowerCase()}">${ticket.status || "–"}</span></td>
        <td>${formatDate(ticket.createdAt)}</td>
        <td>${ticket.handledBy || "–"}</td>
        <td>${formatDate(ticket.handledAt)}</td>
        <td>${ticket.description || "–"}</td>
      `;
      tbody.appendChild(row);
    });
  } catch (err) {
    tbody.innerHTML = `<tr><td colspan="8" class="text-danger">Failed to load tickets.</td></tr>`;
  }
}

// ========== Export (PDF only) ===========
function exportTransactions(type) {
  let params = [];
  params.push(`page=0`);
  params.push(`size=10000`);
  if (txnLastFilters.fromDate) params.push(`fromDate=${encodeURIComponent(txnLastFilters.fromDate)}`);
  if (txnLastFilters.toDate) params.push(`toDate=${encodeURIComponent(txnLastFilters.toDate)}`);
  if (txnLastFilters.type) params.push(`type=${encodeURIComponent(txnLastFilters.type)}`);
  if (txnLastFilters.status) params.push(`status=${encodeURIComponent(txnLastFilters.status)}`);
  if (txnLastFilters.sortBy) params.push(`sortBy=${encodeURIComponent(txnLastFilters.sortBy)}`);
  let query = params.length ? `?${params.join("&")}` : "";

  const url = `${API_BASE}/api/customer/dashboard/transactions/export?format=${type}${query}`;
  const token = localStorage.getItem(ACCESS_TOKEN_KEY);

  fetch(url, {
    method: "GET",
    headers: { "Authorization": `Bearer ${token}` }
  })
    .then(res => {
      if (!res.ok) throw new Error("Export failed");
      return res.blob();
    })
    .then(blob => {
      const filename = `transactions_${new Date().toISOString().slice(0, 10)}.${type}`;
      const link = document.createElement('a');
      link.href = window.URL.createObjectURL(blob);
      link.download = filename;
      link.click();
      window.URL.revokeObjectURL(link.href);
    })
    .catch(err => {
      alert("Export failed. Please try again.");
    });
}

// ========== Filter Bar ===========
function setupFilterBar() {
  const filterForm = document.getElementById("transactionFilterForm");
  if (filterForm) {
    filterForm.addEventListener("submit", function(e) {
      e.preventDefault();
      const fromDate = document.getElementById("filterFromDate").value;
      const toDate = document.getElementById("filterToDate").value;
      const type = document.getElementById("filterType").value;
      const status = document.getElementById("filterStatus").value;
      const sortBy = document.getElementById("filterSort").value;
      txnLastFilters = { fromDate, toDate, type, status, sortBy };
      fetchRecentTransactions({ reset: true, ...txnLastFilters });
    });
  }
  const resetBtn = document.getElementById("resetFiltersBtn");
  if (resetBtn) {
    resetBtn.addEventListener("click", function() {
      filterForm.reset();
      txnLastFilters = {};
      fetchRecentTransactions({ reset: true });
    });
  }
}

// ========== View More Modal Filter Bar Setup ===========
function setupModalFilters() {
  // Transactions modal
  document.getElementById("txnModalFilterForm")?.addEventListener("submit", function(e) {
    e.preventDefault();
    const fromDate = document.getElementById("txnModalFromDate").value;
    const toDate = document.getElementById("txnModalToDate").value;
    fetchAllTransactionsModal({ fromDate, toDate });
  });
  document.getElementById("txnModalResetBtn")?.addEventListener("click", function() {
    document.getElementById("txnModalFilterForm").reset();
    fetchAllTransactionsModal();
  });

  // Loans modal
  document.getElementById("loansModalFilterForm")?.addEventListener("submit", function(e) {
    e.preventDefault();
    const fromDate = document.getElementById("loansModalFromDate").value;
    const toDate = document.getElementById("loansModalToDate").value;
    fetchAllLoansModal({ fromDate, toDate });
  });
  document.getElementById("loansModalResetBtn")?.addEventListener("click", function() {
    document.getElementById("loansModalFilterForm").reset();
    fetchAllLoansModal();
  });

  // Tickets modal
  document.getElementById("ticketsModalFilterForm")?.addEventListener("submit", function(e) {
    e.preventDefault();
    const fromDate = document.getElementById("ticketsModalFromDate").value;
    const toDate = document.getElementById("ticketsModalToDate").value;
    fetchAllTicketsModal({ fromDate, toDate });
  });
  document.getElementById("ticketsModalResetBtn")?.addEventListener("click", function() {
    document.getElementById("ticketsModalFilterForm").reset();
    fetchAllTicketsModal();
  });
}

// ========== Deposit/Transfer/Loan/Ticket Modal Logic (ensure these exist!) ==========

function showDepositModal() {
  const modal = new bootstrap.Modal(document.getElementById('depositModal'));
  modal.show();
  document.getElementById('depositAmount').value = "";
  document.getElementById('depositError').classList.add('d-none');
  document.getElementById('depositError').innerText = "";
}
async function handleDeposit(e) {
  e.preventDefault();
  const amountInput = document.getElementById('depositAmount');
  const errorDiv = document.getElementById('depositError');
  const amount = amountInput.value.trim();

  errorDiv.classList.add('d-none');
  errorDiv.innerText = "";

  if (!amount || isNaN(amount) || parseFloat(amount) <= 0) {
    errorDiv.innerText = "Please enter a valid, positive amount.";
    errorDiv.classList.remove('d-none');
    return;
  }

  try {
    const res = await fetchWithAuth(`${API_BASE}/api/customer/dashboard/deposit?amount=${encodeURIComponent(amount)}`, {
      method: 'POST',
      headers: { 'Accept': 'application/json' }
    });

    if (!res.ok) {
      let errMsg = "Deposit failed.";
      try {
        const result = await res.json();
        errMsg = result.error || result.message || errMsg;
      } catch {}
      errorDiv.innerText = errMsg;
      errorDiv.classList.remove('d-none');
      return;
    }
    const result = await res.json();
    bootstrap.Modal.getInstance(document.getElementById('depositModal')).hide();
    fetchProfileData();
    fetchRecentTransactions({ reset: true });
    alert(result.message || 'Deposit successful!');
  } catch (err) {
    errorDiv.innerText = "Network error. Try again.";
    errorDiv.classList.remove('d-none');
  }
}

function showTransferModal() {
  const modal = new bootstrap.Modal(document.getElementById('transferModal'));
  modal.show();
  document.getElementById('recipientAccount').value = "";
  document.getElementById('transferAmount').value = "";
  document.getElementById('transferError').classList.add('d-none');
  document.getElementById('transferError').innerText = "";
}
async function handleTransfer(e) {
  e.preventDefault();
  const accountInput = document.getElementById('recipientAccount');
  const amountInput = document.getElementById('transferAmount');
  const errorDiv = document.getElementById('transferError');
  const account = accountInput.value.trim();
  const amount = amountInput.value.trim();

  errorDiv.classList.add('d-none');
  errorDiv.innerText = "";

  if (!account || !amount || isNaN(amount) || parseFloat(amount) <= 0) {
    errorDiv.innerText = "Enter valid account and amount.";
    errorDiv.classList.remove('d-none');
    return;
  }

  try {
    const res = await fetchWithAuth(
      `${API_BASE}/api/customer/dashboard/transfer`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          recipientAccountNumber: account,
          amount: amount
        })
      }
    );
    if (!res.ok) {
      let errMsg = "Transfer failed.";
      try { errMsg = (await res.json()).error || errMsg; } catch {}
      errorDiv.innerText = errMsg;
      errorDiv.classList.remove('d-none');
      return;
    }
    const result = await res.json();
    bootstrap.Modal.getInstance(document.getElementById('transferModal')).hide();
    fetchProfileData();
    fetchRecentTransactions({ reset: true });
    alert(result.message || "Transfer successful!");
  } catch (err) {
    errorDiv.innerText = "Network error. Try again.";
    errorDiv.classList.remove('d-none');
  }
}

function showLoanModal() {
  const modal = new bootstrap.Modal(document.getElementById('loanModal'));
  modal.show();
  document.getElementById('loanTypeSelect').value = "";
  document.getElementById('loanAmount').value = "";
  document.getElementById('loanError').classList.add('d-none');
  document.getElementById('loanError').innerText = "";
}
async function handleLoanApply(e) {
  e.preventDefault();
  const typeInput = document.getElementById('loanTypeSelect');
  const amountInput = document.getElementById('loanAmount');
  const errorDiv = document.getElementById('loanError');
  const loanType = typeInput.value;
  const amount = amountInput.value.trim();

  errorDiv.classList.add('d-none');
  errorDiv.innerText = "";

  if (!loanType) {
    errorDiv.innerText = "Please select a loan type.";
    errorDiv.classList.remove('d-none');
    return;
  }
  if (!amount || isNaN(amount) || parseFloat(amount) <= 0) {
    errorDiv.innerText = "Enter a valid loan amount.";
    errorDiv.classList.remove('d-none');
    return;
  }

  try {
    const res = await fetchWithAuth(
      `${API_BASE}/api/customer/dashboard/loan`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          loanType: loanType,
          amountRequested: amount
        })
      }
    );
    if (!res.ok) {
      let errMsg = "Loan application failed.";
      try { errMsg = (await res.json()).error || errMsg; } catch {}
      errorDiv.innerText = errMsg;
      errorDiv.classList.remove('d-none');
      return;
    }
    const result = await res.json();
    bootstrap.Modal.getInstance(document.getElementById('loanModal')).hide();
    fetchOpenLoans();
    alert(result.message || "Loan application submitted!");
  } catch (err) {
    errorDiv.innerText = "Network error. Try again.";
    errorDiv.classList.remove('d-none');
  }
}

function showTicketModal() {
  const modal = new bootstrap.Modal(document.getElementById('ticketModal'));
  modal.show();
  document.getElementById('issueTypeSelect').value = "";
  document.getElementById('ticketDescription').value = "";
  document.getElementById('ticketError').classList.add('d-none');
  document.getElementById('ticketError').innerText = "";
}
async function handleTicketSubmit(e) {
  e.preventDefault();
  const typeInput = document.getElementById('issueTypeSelect');
  const descInput = document.getElementById('ticketDescription');
  const errorDiv = document.getElementById('ticketError');
  const issueType = typeInput.value;
  const description = descInput.value.trim();

  errorDiv.classList.add('d-none');
  errorDiv.innerText = "";

  if (!issueType) {
    errorDiv.innerText = "Please select an issue type.";
    errorDiv.classList.remove('d-none');
    return;
  }

  try {
    const res = await fetchWithAuth(
      `${API_BASE}/api/customer/dashboard/ticket`,
      {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          issueType: issueType,
          description: description
        })
      }
    );
    if (!res.ok) {
      let errMsg = "Ticket submission failed.";
      try { errMsg = (await res.json()).error || errMsg; } catch {}
      errorDiv.innerText = errMsg;
      errorDiv.classList.remove('d-none');
      return;
    }
    const result = await res.json();
    bootstrap.Modal.getInstance(document.getElementById('ticketModal')).hide();
    fetchOpenTickets();
    alert(result.message || "Support ticket submitted!");
  } catch (err) {
    errorDiv.innerText = "Network error. Try again.";
    errorDiv.classList.remove('d-none');
  }
}

// ========== Init ===========
document.addEventListener("DOMContentLoaded", () => {
  resetIdleTimer();
  if (!localStorage.getItem(ACCESS_TOKEN_KEY) || !localStorage.getItem(REFRESH_TOKEN_KEY)) {
    logout("Please log in!");
    return;
  }
  fetchProfileData();
  fetchRecentTransactions({ reset: true });
  fetchOpenLoans();
  fetchOpenTickets();

  document.getElementById("logoutBtn")?.addEventListener("click", () => logout("Logged out."));
  setupFilterBar();
  setupModalFilters();

  // View More: open modal and load all data
  document.getElementById("viewMoreTxnBtn")?.addEventListener("click", () => fetchAllTransactionsModal());
  document.getElementById("viewMoreLoansBtn")?.addEventListener("click", () => fetchAllLoansModal());
  document.getElementById("viewMoreTicketsBtn")?.addEventListener("click", () => fetchAllTicketsModal());

  // Export (PDF only)
  document.getElementById('exportPDFBtn')?.addEventListener("click", function(e) {
    e.preventDefault();
    exportTransactions('pdf');
  });

  // Modal handlers
  document.getElementById("depositBtn")?.addEventListener("click", showDepositModal);
  document.getElementById("depositForm")?.addEventListener("submit", handleDeposit);

  document.getElementById("transferBtn")?.addEventListener("click", showTransferModal);
  document.getElementById("transferForm")?.addEventListener("submit", handleTransfer);

  document.getElementById("loanBtn")?.addEventListener("click", showLoanModal);
  document.getElementById("loanForm")?.addEventListener("submit", handleLoanApply);

  document.getElementById("ticketBtn")?.addEventListener("click", showTicketModal);
  document.getElementById("ticketForm")?.addEventListener("submit", handleTicketSubmit);
});