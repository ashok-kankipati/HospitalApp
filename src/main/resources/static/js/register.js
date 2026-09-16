// Register API URL
const API_URL = '/api/auth/register';

// DOM Elements
const registerForm = document.getElementById('registerForm');
const errorMessage = document.getElementById('errorMessage');
const successMessage = document.getElementById('successMessage');
const usernameInput = document.getElementById('username');
const emailInput = document.getElementById('email');
const passwordInput = document.getElementById('password');
const confirmPasswordInput = document.getElementById('confirmPassword');
const roleInput = document.getElementById('role');
const registerBtn = document.querySelector('.btn-login');

// Event Listeners
registerForm.addEventListener('submit', handleRegister);

/**
 * Handle registration form submission
 */
async function handleRegister(event) {
    event.preventDefault();

    // Clear previous messages
    clearMessages();

    // Validate inputs
    const validation = validateForm();
    if (!validation.valid) {
        showError(validation.message);
        return;
    }

    // Disable button and show loading state
    registerBtn.disabled = true;
    registerBtn.textContent = 'Registering...';

    try {
        // Create registration request
        const registerRequest = {
            username: usernameInput.value.trim(),
            email: emailInput.value.trim(),
            password: passwordInput.value,
            role: roleInput.value
        };

        // Make API call
        const response = await fetch(API_URL, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(registerRequest)
        });

        if (!response.ok) {
            throw new Error(`HTTP error! status: ${response.status}`);
        }

        const data = await response.json();

        if (data.success) {
            showSuccess(`Registration successful! Redirecting to login...`);
            
            // Clear form
            registerForm.reset();

            // Redirect to login page after 2 seconds
            setTimeout(() => {
                window.location.href = 'login.html';
            }, 2000);
        } else {
            showError(data.message || 'Registration failed');
        }
    } catch (error) {
        console.error('Error:', error);
        showError('An error occurred: ' + error.message);
    } finally {
        // Re-enable button
        registerBtn.disabled = false;
        registerBtn.textContent = 'Register';
    }
}

/**
 * Validate registration form
 */
function validateForm() {
    const username = usernameInput.value.trim();
    const email = emailInput.value.trim();
    const password = passwordInput.value;
    const confirmPassword = confirmPasswordInput.value;
    const role = roleInput.value;

    if (!username) {
        return { valid: false, message: 'Please enter a username' };
    }

    if (username.length < 3) {
        return { valid: false, message: 'Username must be at least 3 characters long' };
    }

    if (!email) {
        return { valid: false, message: 'Please enter an email address' };
    }

    if (!isValidEmail(email)) {
        return { valid: false, message: 'Please enter a valid email address' };
    }

    if (!password) {
        return { valid: false, message: 'Please enter a password' };
    }

    if (password.length < 6) {
        return { valid: false, message: 'Password must be at least 6 characters long' };
    }

    if (password !== confirmPassword) {
        return { valid: false, message: 'Passwords do not match' };
    }

    if (!role) {
        return { valid: false, message: 'Please select a role' };
    }

    return { valid: true, message: '' };
}

/**
 * Validate email format
 */
function isValidEmail(email) {
    const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
    return emailRegex.test(email);
}

/**
 * Show error message
 */
function showError(message) {
    errorMessage.textContent = message;
    errorMessage.classList.add('show');
    successMessage.classList.remove('show');
    window.scrollTo(0, 0);
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
emailInput.addEventListener('focus', clearMessages);
passwordInput.addEventListener('focus', clearMessages);
confirmPasswordInput.addEventListener('focus', clearMessages);
roleInput.addEventListener('focus', clearMessages);
