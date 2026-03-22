import { useDashboardSummary } from './useDashboardSummary';

/** Indian currency formatter */
const fmt = (val) => {
    if (val == null || isNaN(val)) return '₹0';
    const n = Number(val);
    if (Math.abs(n) >= 1e7) return `₹${(n / 1e7).toFixed(2)} Cr`;
    if (Math.abs(n) >= 1e5) return `₹${(n / 1e5).toFixed(2)} L`;
    if (Math.abs(n) >= 1e3) return `₹${(n / 1e3).toFixed(1)} K`;
    return `₹${n.toLocaleString('en-IN')}`;
};

/**
 * Thin API consumer — maps backend lockedInsights data to the shape
 * LockedPremiumInsights component expects.
 *
 * Backend InsightCardDTO: { id, icon, title, teaser, category, score }
 * Component expects card: { id, label, impactLabel }
 * Component destructures: { cards, maxFigureFormatted, hiddenCount }
 */
export const useLockedInsights = () => {
    const { data, isLoading, error } = useDashboardSummary();

    if (isLoading || error || !data) {
        return {
            cards: [],
            maxFigureFormatted: '₹0',
            hiddenCount: 0,
            isLoading,
            error,
        };
    }

    const li = data.lockedInsights || {};
    const rawCards = li.cards ?? [];
    const totalAvailable = li.totalAvailable ?? rawCards.length;

    // Map backend shape → component shape
    const cards = rawCards.map(c => ({
        id: c.id,
        label: c.title ?? c.label ?? 'Insight',
        impactLabel: c.teaser ?? fmt(c.score ?? 0),
        icon: c.icon ?? '🔒',
        category: c.category ?? '',
    }));

    // Find the largest score for the CTA
    const maxScore = rawCards.reduce((max, c) => Math.max(max, c.score ?? 0), 0);
    const visibleCount = Math.min(cards.length, 4);
    const hiddenCount = Math.max(0, totalAvailable - visibleCount);

    return {
        cards: cards.slice(0, 4),
        maxFigureFormatted: fmt(maxScore),
        hiddenCount,
        isLoading: false,
        error: null,
    };
};
