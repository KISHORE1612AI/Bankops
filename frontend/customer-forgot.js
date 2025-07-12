// customer-forgot.js

document.getElementById('customerForgotForm')
  .addEventListener('submit', async function (e) {
    e.preventDefault();

    const email = document.getElementById('customerResetEmail').value.trim();
    const messageEl = document.getElementById('customerForgotMessage');

    // Reset state
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
        body: JSON.stringify({ email })
      });

      // We always want to show a generic message (no info leakage)
      const userMsg = res.ok
        ? 'If that email is registered, a reset link has been sent.'
        : 'If that email is registered, a reset link has been sent.';

      showMessage(userMsg, 'green');

    } catch (err) {
      console.error('Failed to send reset link:', err);
      showMessage('Network error — please try again later.', 'red');
    }

    function showMessage(msg, color) {
      messageEl.textContent = msg;
      messageEl.style.color = color;
    }
  });
