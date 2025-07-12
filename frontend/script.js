document.getElementById("login-form").addEventListener("submit", async (e) => {
  e.preventDefault();

  const email = document.getElementById("email").value.trim();
  const password = document.getElementById("password").value;

  try {
    const response = await fetch("http://localhost:8080/auth/login", {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "Accept": "application/json"
      },
      body: JSON.stringify({ email, password })
    });

    if (!response.ok) {
      const errorText = await response.text();
      alert("Login failed: " + errorText);
      return;
    }

    const data = await response.json();

    if (!data.token || !data.role) {
      throw new Error("Invalid response from server. Missing token or role.");
    }

    localStorage.setItem("jwt", data.token);
    localStorage.setItem("role", data.role);

    // ✅ Redirect based on role
    switch (data.role) {
      case "CUSTOMER":
        window.location.href = "customer-dashboard.html";
        break;
      case "AUDITOR":
        window.location.href = "auditor-dashboard.html";
        break;
      case "BRANCH_MANAGER":
        window.location.href = "branch-manager-dashboard.html";
        break;
      case "LOAN_OFFICER":
        window.location.href = "loan-officer-dashboard.html";
        break;
      case "CUSTOMER_SUPPORT":
        window.location.href = "customer-support-dashboard.html";
        break;
      case "SUPER_ADMIN":
        alert("Redirect to super admin panel (not implemented yet)");
        break;
      default:
        alert("Unknown role. Access denied.");
    }

  } catch (err) {
    console.error("Login error:", err);
    alert("An error occurred during login.");
  }
});
