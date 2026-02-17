/**
 * Currency and number formatting utilities for the Indian market.
 */

/**
 * Format a number as Indian Rupees (₹).
 * @param {number} amount - The amount to format
 * @param {boolean} [compact=false] - Use compact notation (e.g., ₹1.5L)
 * @returns {string}
 */
export const formatCurrency = (amount, compact = false) => {
    if (amount == null || isNaN(amount)) return '₹0';

    if (compact) {
        if (amount >= 10000000) return `₹${(amount / 10000000).toFixed(1)}Cr`;
        if (amount >= 100000) return `₹${(amount / 100000).toFixed(1)}L`;
        if (amount >= 1000) return `₹${(amount / 1000).toFixed(1)}K`;
    }

    return new Intl.NumberFormat('en-IN', {
        style: 'currency',
        currency: 'INR',
        minimumFractionDigits: 0,
        maximumFractionDigits: 0,
    }).format(amount);
};

/**
 * Format a percentage value.
 * @param {number} value - The percentage value
 * @param {number} [decimals=1] - Decimal places
 * @returns {string}
 */
export const formatPercentage = (value, decimals = 1) => {
    if (value == null || isNaN(value)) return '0%';
    return `${Number(value).toFixed(decimals)}%`;
};

/**
 * Format a date string to locale display.
 * @param {string} dateStr - ISO date string
 * @returns {string}
 */
export const formatDate = (dateStr) => {
    if (!dateStr) return '';
    return new Date(dateStr).toLocaleDateString('en-IN', {
        day: 'numeric',
        month: 'short',
        year: 'numeric',
    });
};
