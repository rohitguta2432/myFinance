import React from 'react';
import { Lock } from 'lucide-react';
import { useLockedInsights } from '../../../hooks/useLockedInsights';

const LockedInsightCard = ({ card }) => {
    return (
        <div className="relative border border-white/10 rounded-2xl p-5 overflow-hidden bg-white/[0.02]">
            {/* Title row with lock icon */}
            <div className="flex items-center gap-2 mb-2">
                <h4 className="text-base font-bold text-white">{card.label}</h4>
                <div className="w-7 h-7 rounded-full bg-white/10 flex items-center justify-center flex-shrink-0">
                    <Lock className="w-3.5 h-3.5 text-white/60" />
                </div>
            </div>

            {/* Impact figure — large, prominent, visible */}
            <p className="text-2xl font-bold text-amber-400 mb-3 tabular-nums">
                {card.impactLabel}
            </p>

            {/* CTA button */}
            <button className="px-5 py-2 bg-gradient-to-r from-amber-500 to-orange-500 text-xs font-bold text-black rounded-full uppercase tracking-wider shadow-lg hover:shadow-amber-500/20 transition-all active:scale-95">
                Unlock Full Analysis
            </button>
        </div>
    );
};

const LockedPremiumInsights = () => {
    const { cards, maxFigureFormatted, hiddenCount } = useLockedInsights();

    if (!cards || cards.length === 0) return null;

    return (
        <div>
            <div className="flex items-center justify-between mb-3">
                <h3 className="text-sm font-bold uppercase tracking-[0.2em] text-slate-500">
                    Premium Insights Waiting For You
                </h3>
                <div className="flex items-center gap-1 text-amber-500">
                    <Lock className="w-4 h-4" />
                    <span className="text-xs font-bold uppercase tracking-widest">Pro</span>
                </div>
            </div>
            <div className="grid gap-3 sm:grid-cols-2">
                {cards.map(card => (
                    <LockedInsightCard key={card.id} card={card} />
                ))}
            </div>

            {/* CTA with largest figure */}
            <button className="mt-4 w-full py-3 bg-gradient-to-r from-amber-500 to-orange-500 text-base font-bold text-black rounded-xl shadow-lg hover:shadow-amber-500/30 transition-all active:scale-[0.98]">
                Unlock Full Analysis — {maxFigureFormatted} waiting
                {hiddenCount > 0 && ` (+${hiddenCount} more)`}
            </button>
        </div>
    );
};

export default LockedPremiumInsights;
