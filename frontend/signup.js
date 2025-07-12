// customer-signup.js

document.getElementById("signupForm").addEventListener("submit", async function (e) {
  e.preventDefault();

  // Elements
  const fullName   = document.getElementById("fullName").value.trim();
  const email      = document.getElementById("email").value.trim();
  const phone      = document.getElementById("phone").value.trim();
  const password   = document.getElementById("password").value;
  const confirm    = document.getElementById("confirmPwd").value;
  const agree      = document.getElementById("agree").checked;
  const messageEl  = document.getElementById("signupMessage");
  const submitBtn  = this.querySelector("button[type='submit']");

  // Reset message
  messageEl.textContent = "";
  messageEl.className   = "";

  // --- Client-side validation ---
  if (!fullName) return showError("Full name is required.");
  if (!email) return showError("Email is required.");
  if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(email)) {
    return showError("Invalid email format.");
  }
  if (!phone) return showError("Phone number is required.");
  if (!/^[6-9]\d{9}$/.test(phone)) {
    return showError("Enter a valid 10-digit phone number.");
  }
  if (!password) return showError("Password is required.");
  if (password !== confirm) return showError("Passwords do not match.");
  if (!agree) return showError("You must agree to the terms and privacy policy.");

  // Disable button & show loading
  submitBtn.disabled = true;
  submitBtn.textContent = "Signing up...";

  // Build payload — NO accountNumber here
  const payload = { fullName, email, phone, password };

  try {
    const response = await fetch("http://localhost:8080/auth/customer/signup", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });

    if (response.ok) {
      const text = await response.text();
      showSuccess(text || "Account created successfully! Redirecting…");
      this.reset();

      setTimeout(() => {
        window.location.href = "customer-login.html";
      }, 1500);
    } else {
      let errMsg = `Signup failed (status ${response.status}).`;
      const ct = response.headers.get("Content-Type") || "";
      if (ct.includes("application/json")) {
        try {
          const errJson = await response.json();
          errMsg = errJson.message || JSON.stringify(errJson);
        } catch {}
      } else {
        const txt = await response.text();
        if (txt) errMsg = txt;
      }
      showError(errMsg);
    }
  } catch (err) {
    console.error("Signup error:", err);
    showError("Network error — please check your connection and try again.");
  } finally {
    submitBtn.disabled = false;
    submitBtn.textContent = "Sign Up";
  }

  // Helpers
  function showError(msg) {
    messageEl.textContent = msg;
    messageEl.className   = "error";
  }
  function showSuccess(msg) {
    messageEl.textContent = msg;
    messageEl.className   = "success";
  }
});
