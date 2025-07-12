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

async function fetchUsers() {
  hideAlert();
  document.getElementById("loader").style.display = "block";
  document.getElementById("tableSection").style.display = "none";

  try {
    const res = await fetch(`${API_BASE}/api/superadmin/users`, {
      headers: { "Authorization": "Bearer " + token }
    });
    if (!res.ok) throw new Error("Failed to fetch users: " + await res.text());
    const users = await res.json();

    const tbody = document.querySelector("#usersTable tbody");
    tbody.innerHTML = "";
    if (!users.length) {
      tbody.innerHTML = `<tr><td colspan="6" class="text-center text-secondary">No users found.</td></tr>`;
    } else {
      users.forEach((u, idx) => {
        tbody.innerHTML += `
          <tr>
            <td>${u.id ?? idx + 1}</td>
            <td>${u.name || "-"}</td>
            <td>${u.email || "-"}</td>
            <td>${u.active ? '<span class="text-success">Active</span>' : '<span class="text-danger">Inactive</span>'}</td>
            <td>${formatDate(u.createdAt)}</td>
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

window.onload = fetchUsers;