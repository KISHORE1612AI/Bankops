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

async function fetchEmployees() {
  hideAlert();
  document.getElementById("loader").style.display = "block";
  document.getElementById("tableSection").style.display = "none";

  try {
    const res = await fetch(`${API_BASE}/api/superadmin/employees`, {
      headers: { "Authorization": "Bearer " + token }
    });
    if (!res.ok) throw new Error("Failed to fetch employees: " + await res.text());
    const employees = await res.json();

    const tbody = document.querySelector("#employeesTable tbody");
    tbody.innerHTML = "";
    if (!employees.length) {
      tbody.innerHTML = `<tr><td colspan="7" class="text-center text-secondary">No employees found.</td></tr>`;
    } else {
      employees.forEach((e, idx) => {
        tbody.innerHTML += `
          <tr>
            <td>${e.id ?? idx + 1}</td>
            <td>${e.name || "-"}</td>
            <td>${e.email || "-"}</td>
            <td>${e.role || "-"}</td>
            <td>${e.active ? '<span class="text-success">Active</span>' : '<span class="text-danger">Inactive</span>'}</td>
            <td>${formatDate(e.joinedAt)}</td>
            <td class="table-actions">
              <button class="btn btn-outline-primary btn-sm" title="View"><i class="fa fa-eye"></i></button>
              <button class="btn btn-outline-danger btn-sm" title="Delete"><i class="fa fa-trash"></i></button>
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

window.onload = fetchEmployees;