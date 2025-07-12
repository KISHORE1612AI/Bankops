// customer-login.js

// Clear previous tokens on login page load (prevents stale/invalid sessions)
localStorage.removeItem("accessToken");
localStorage.removeItem("refreshToken");
localStorage.removeItem("role");

document.getElementById("customer-login-form").addEventListener("submit", async function (e) {
  e.preventDefault();

  const email = document.getElementById("customer-email").value.trim();
  const password = document.getElementById("customer-password").value;
  const msg = document.getElementById("login-message");

  // Clear previous message
  msg.textContent = "";
  msg.className = "";

  // Simple client-side validation
  if (!email) {
    msg.textContent = "Email is required.";
    msg.className = "error";
    return;
  }
  if (!password) {
    msg.textContent = "Password is required.";
    msg.className = "error";
    return;
  }

  try {
    const response = await fetch("http://localhost:8080/auth/customer/login", {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({ email, password })
    });

    if (response.ok) {
      const data = await response.json();

      // Store JWT tokens
      localStorage.setItem("accessToken", data.accessToken);
      localStorage.setItem("refreshToken", data.refreshToken);
      localStorage.setItem("role", data.role);

      msg.textContent = "Login successful! Redirecting…";
      msg.className = "success";

      // Redirect to dashboard instantly (or use timeout if you prefer)
      window.location.href = "customer-dashboard.html";
      // setTimeout(() => {
      //   window.location.href = "customer-dashboard.html";
      // }, 1000);
    } else {
      const errorText = await response.text();
      msg.textContent = errorText || "Invalid email or password.";
      msg.className = "error";
    }

  } catch (error) {
    console.error("Login error:", error);
    msg.textContent = "Network error — please try again.";
    msg.className = "error";
  }
});