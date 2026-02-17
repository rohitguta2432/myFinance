const BASE_URL = '/api/v1/assessment';

/**
 * Shared fetch wrapper with JSON headers and error handling.
 * No third-party HTTP library — uses native fetch.
 */
async function request(endpoint, options = {}) {
    const url = `${BASE_URL}${endpoint}`;
    const config = {
        headers: {
            'Content-Type': 'application/json',
            ...options.headers,
        },
        ...options,
    };

    const response = await fetch(url, config);

    if (!response.ok) {
        const error = await response.json().catch(() => ({
            message: `Request failed with status ${response.status}`,
        }));
        throw new Error(error.message || `API error: ${response.status}`);
    }

    // Handle 204 No Content
    if (response.status === 204) return null;

    return response.json();
}

export const api = {
    get: (endpoint) => request(endpoint, { method: 'GET' }),
    post: (endpoint, data) =>
        request(endpoint, { method: 'POST', body: JSON.stringify(data) }),
    put: (endpoint, data) =>
        request(endpoint, { method: 'PUT', body: JSON.stringify(data) }),
    delete: (endpoint) => request(endpoint, { method: 'DELETE' }),
};
