const API_BASE = "http://localhost:8080";
const token = localStorage.getItem("token");

function showAlert(msg) {
  const alert = document.getElementById("alert");
  alert.textContent = msg;
  alert.style.display = "block";
}

function hideAlert() {
  const alert = document.getElementById("alert");
  alert.style.display = "none";
}

function formatDate(d) {
  if (!d) return "-";
  return new Date(d).toLocaleString("en-IN");
}

async function fetchTickets() {
  hideAlert();
  document.getElementById("loader").style.display = "block";
  document.getElementById("tableSection").style.display = "none";

  try {
    const res = await fetch(`${API_BASE}/api/superadmin/tickets`, {
      headers: { "Authorization": "Bearer " + token }
    });
    if (!res.ok) throw new Error("Failed to fetch tickets: " + await res.text());
    const tickets = await res.json();

    const tbody = document.querySelector("#ticketsTable tbody");
    tbody.innerHTML = "";
    if (!tickets.length) {
      tbody.innerHTML = `<tr><td colspan="7" class="text-center text-secondary">No support tickets found.</td></tr>`;
    } else {
      tickets.forEach((t, idx) => {
        tbody.innerHTML += `
          <tr>
            <td>${t.id ?? idx + 1}</td>
            <td>${t.customerName || "-"}</td>
            <td>${t.issueType || "-"}</td>
            <td>${
              t.status === 'RESOLVED'
                ? '<span class="text-success">Resolved</span>'
                : t.status === 'PENDING'
                ? '<span class="text-warning">Pending</span>'
                : t.status || '-'
            }</td>
            <td>${formatDate(t.createdAt)}</td>
            <td>${t.handledBy || '-'}</td>
            <td class="table-actions">
              <button class="btn btn-outline-primary btn-sm" title="View"><i class="fa fa-eye"></i></button>
              <button class="btn btn-outline-success btn-sm" title="Resolve"><i class="fa fa-check"></i></button>
            </td>
          </tr>
        `;
      });
    }
    document.getElementById("loader").style.display = "none";
    document.getElementById("tableSection").style.display = "block";
  } catch (err) {
    showAlert(err.message);
    document.getElementById("loader").style.display = "none";
  }
}

window.onload = fetchTickets;