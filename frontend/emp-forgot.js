// emp-forgot.js

document.getElementById('empForgotForm')
  .addEventListener('submit', async function (e) {
    e.preventDefault();

    const email     = document.getElementById('empResetEmail').value.trim();
    const empId     = document.getElementById('empId').value.trim();
    const messageEl = document.getElementById('empForgotMessage');

    // Reset any previous messages
    messageEl.textContent = '';
    messageEl.style.color = '';

    // Basic validation
    if (!email) {
      showMessage('Please enter your email address.', 'orange');
      return;
    }

    try {
      const res = await fetch('http://localhost:8080/auth/forgot-password', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ email, employeeId: empId || null })
      });

      // Always show a generic message to avoid user-enumeration
      showMessage(
        'If that email is registered, a reset link has been sent.',
        'green'
      );

    } catch (err) {
      console.error('Employee forgot password error:', err);
      showMessage('Network error — please try again later.', 'red');
    }

    function showMessage(msg, color) {
      messageEl.textContent = msg;
      messageEl.style.color = color;
    }
  });
