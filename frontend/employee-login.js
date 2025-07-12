// employee-login.js

const empIdInput    = document.getElementById("employee-id");
const empIdError    = document.getElementById("employee-id-error");
const msg           = document.getElementById("employeeMessage");

const emailInput    = document.getElementById("employee-email");
const passwordInput = document.getElementById("employee-password");


// 1) Enforce numeric‐only input on the Employee ID field
empIdInput.addEventListener("input", () => {
  const value = empIdInput.value;
  if (/[^0-9]/.test(value)) {
    empIdError.textContent = "Only numbers allowed.";
    empIdInput.classList.add("invalid");
  } else {
    empIdError.textContent = "";
    empIdInput.classList.remove("invalid");
  }
});

// 2) Validate exactly 6 digits on blur
empIdInput.addEventListener("blur", () => {
  const value = empIdInput.value.trim();
  if (value && !/^\d{6}$/.test(value)) {
    empIdError.textContent = "Employee ID must be exactly 6 digits.";
    empIdInput.classList.add("invalid");
  } else {
    empIdError.textContent = "";
    empIdInput.classList.remove("invalid");
  }
});


// 3) Handle form submission
document.getElementById("employee-login-form").addEventListener("submit", async (e) => {
  e.preventDefault();

  const employeeId = empIdInput.value.trim();
  const email      = emailInput.value.trim();
  const password   = passwordInput.value;

  // Clear any prior messages
  msg.textContent = "";
  msg.className   = "";

  // Simple front-end validation:
  if (!/^\d{6}$/.test(employeeId)) {
    empIdError.textContent = "Employee ID must be exactly 6 digits.";
    empIdInput.classList.add("invalid");
    return;
  }

  if (!email) {
    msg.textContent = "Email is required.";
    msg.className   = "error";
    return;
  }

  if (!password) {
    msg.textContent = "Password is required.";
    msg.className   = "error";
    return;
  }

  // 4) Perform the login request
  try {
    const response = await fetch("http://localhost:8080/auth/employee/login", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ 
        employeeId: employeeId,
        email:      email,
        password:   password 
      }),
    });

    // Attempt to parse JSON from the server:
    const data = await response.json();

    if (response.ok && data.accessToken) {
      // 5) We expect { accessToken, refreshToken, role } in the JSON
      localStorage.setItem("token", data.accessToken);
      localStorage.setItem("refreshToken", data.refreshToken);

      msg.textContent = "Login successful! Redirecting...";
      msg.className   = "success";

      // 6) Decode the JWT payload to pull out the role
      try {
        const payload = JSON.parse(atob(data.accessToken.split(".")[1]));
        const role    = payload.role;

        // Short delay so the user sees "Login successful..."
        setTimeout(() => {
          switch (role) {
            case "SUPER_ADMIN":
              window.location.href = "super-admin-dashboard.html";
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
            case "AUDITOR":
              window.location.href = "auditor-dashboard.html";
              break;
            default:
              alert("Unknown role in token. Contact administrator.");
              localStorage.removeItem("token");
              localStorage.removeItem("refreshToken");
          }
        }, 800);

      } catch (decodeError) {
        msg.textContent = "Login failed: Unable to decode token.";
        msg.className   = "error";
        localStorage.removeItem("token");
        localStorage.removeItem("refreshToken");
      }

    } else {
      // If the server returned 401 or some other error, data might be a string or an object
      // For simplicity, we show either data.message or a generic message
      const errMsg = (data && data.message) ? data.message : "Invalid credentials.";
      msg.textContent = errMsg;
      msg.className   = "error";
    }

  } catch (networkError) {
    console.error("Login error:", networkError);
    msg.textContent = "Network error — please try again.";
    msg.className   = "error";
  }
});
