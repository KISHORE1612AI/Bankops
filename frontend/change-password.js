document.getElementById("change-password-form").addEventListener("submit", async function (e) {
  e.preventDefault();

  const email = document.getElementById("change-email").value.trim();
  const currentPassword = document.getElementById("current-password").value;
  const newPassword = document.getElementById("new-password").value;
  const changeMsg = document.getElementById("changeMessage");

  changeMsg.textContent = "";
  changeMsg.className = "";

  try {
    const accessToken = localStorage.getItem("accessToken");

    const response = await fetch("http://localhost:8080/users/change-password", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Authorization": `Bearer ${accessToken}`
      },
      body: JSON.stringify({ email, currentPassword, newPassword })
    });

    const message = await response.text();

    if (response.ok) {
      changeMsg.textContent = "Password changed successfully.";
      changeMsg.className = "success";
    } else {
      changeMsg.textContent = message || "Failed to change password.";
      changeMsg.className = "error";
    }
  } catch (err) {
    console.error("Password change error:", err);
    changeMsg.textContent = "Network error — please try again.";
    changeMsg.className = "error";
  }
});
