// Login API URL
const API_URL = '/api/auth/login';

// DOM Elements
const loginForm = document.getElementById('loginForm');
const errorMessage = document.getElementById('errorMessage');
const successMessage = document.getElementById('successMessage');
const usernameInput = document.getElementById('username');
const passwordInput = document.getElementById('password');
const loginBtn = document.querySelector('.btn-login');

// Event Listeners
loginForm.addEventListener('submit', handleLogin);

/**
 * Handle login form submission
 */
async function handleLogin(event) {
    event.preventDefault();

    // Clear previous messages
    clearMessages();

    // Validate inputs
    if (!usernameInput.value.trim()) {
        showError('Please enter your username');
        return;
    }

    if (!passwordInput.value.trim()) {
        showError('Please enter your password');
        return;
    }

    // Disable button and show loading state
    loginBtn.disabled = true;
    loginBtn.textContent = 'Logging in...';

    try {
        // Create login request
        const loginRequest = {
            username: usernameInput.value.trim(),
            password: passwordInput.value
        };

        // Make API call
        const response = await fetch(API_URL, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(loginRequest)
        });

        const data = await response.json();
        if (!response.ok) throw new Error(data.message || 'Login failed');
        if (data.mfaRequired) {
            localStorage.removeItem('user');
            window.location.assign(data.redirectUrl);
            return;
        }

        if (data.success) {
            showSuccess(`Welcome, ${data.username}! Redirecting...`);
            
            // Store user info in localStorage
            localStorage.setItem('user', JSON.stringify({
                username: data.username,
                email: data.email,
                role: data.role,
                loginTime: new Date().toISOString()
            }));

            // Redirect after 1.5 seconds
            setTimeout(() => {
                window.location.href = 'dashboard.html';
            }, 1500);
        } else {
            showError(data.message || 'Login failed');
        }
    } catch (error) {
        console.error('Error:', error);
        showError('An error occurred: ' + error.message);
    } finally {
        // Re-enable button
        loginBtn.disabled = false;
        loginBtn.textContent = 'Login';
    }
}

/**
 * Show error message
 */
function showError(message) {
    errorMessage.textContent = message;
    errorMessage.classList.add('show');
    successMessage.classList.remove('show');
}

/**
 * Show success message
 */
function showSuccess(message) {
    successMessage.textContent = message;
    successMessage.classList.add('show');
    errorMessage.classList.remove('show');
}

/**
 * Clear all messages
 */
function clearMessages() {
    errorMessage.classList.remove('show');
    successMessage.classList.remove('show');
    errorMessage.textContent = '';
    successMessage.textContent = '';
}

// Clear error message when user starts typing
usernameInput.addEventListener('focus', clearMessages);
passwordInput.addEventListener('focus', clearMessages);

// Check if user is already logged in
window.addEventListener('load', () => {
    const user = localStorage.getItem('user');
    if (user) {
        // Optional: redirect to dashboard if already logged in
        // window.location.href = 'dashboard.html';
    }
});

// Complete the browser profile only after the server verifies Duo.
window.addEventListener('load', async () => {
    const result = new URLSearchParams(window.location.search).get('duo');
    if (!result) return;
    history.replaceState(null, '', window.location.pathname);
    localStorage.removeItem('user');
    if (result !== 'complete') {
        showError('Duo verification failed or expired. Please log in again.');
        return;
    }
    try {
        const response = await fetch('/api/auth/session');
        if (!response.ok) throw new Error('Your login session has expired.');
        const user = await response.json();
        localStorage.setItem('user', JSON.stringify(user));
        window.location.replace('dashboard.html');
    } catch (error) { showError(error.message); }
});
