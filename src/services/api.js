const BASE_URL = '/api/v1';

/**
 * Shared fetch wrapper with JSON headers and error handling.
 * No third-party HTTP library — uses native fetch.
 */
async function request(endpoint, options = {}) {
    const url = `${BASE_URL}${endpoint}`;

    let token = null;
    try {
        const authStorage = localStorage.getItem('auth-storage');
        if (authStorage) {
            const state = JSON.parse(authStorage).state;
            if (state && state.token) {
                token = state.token;
            }
        }
    } catch (e) {
        console.error('Failed to parse auth storage', e);
    }

    const headers = {
        'Content-Type': 'application/json',
        ...options.headers,
    };

    if (token) {
        headers['Authorization'] = `Bearer ${token}`;
    }

    const config = {
        ...options,
        headers,
    };

    const response = await fetch(url, config);

    if (response.status === 401) {
        localStorage.removeItem('auth-storage');
        window.location.href = '/login';
        return;
    }

    if (!response.ok) {
        const error = await response.json().catch(() => ({
            message: `Request failed with status ${response.status}`,
        }));
        throw new Error(error.message || `API error: ${response.status}`);
    }

    // Handle empty responses (204 No Content or empty 200)
    const text = await response.text();
    if (!text) return null;
    return JSON.parse(text);
}

export const api = {
    get: (endpoint) => request(endpoint, { method: 'GET' }),
    post: (endpoint, data) =>
        request(endpoint, { method: 'POST', body: JSON.stringify(data) }),
    put: (endpoint, data) =>
        request(endpoint, { method: 'PUT', body: JSON.stringify(data) }),
    delete: (endpoint) => request(endpoint, { method: 'DELETE' }),
};
