import React from 'react';
import { Lock } from 'lucide-react';
import { useLockedInsights } from '../../../hooks/useLockedInsights';

const LockedInsightCard = ({ card }) => {
    return (
        <div className="relative border border-white/10 rounded-2xl p-5 overflow-hidden bg-white/[0.02]">
            {/* Blur overlay with lock + CTA */}
            <div className="absolute inset-0 backdrop-blur-[2px] z-10 flex items-center justify-center">
                <div className="flex flex-col items-center gap-2">
                    <div className="w-10 h-10 rounded-full bg-white/10 flex items-center justify-center backdrop-blur-md">
                        <Lock className="w-5 h-5 text-white/60" />
                    </div>
                    <button className="px-4 py-1.5 bg-gradient-to-r from-amber-500 to-orange-500 text-[10px] font-bold text-black rounded-full uppercase tracking-wider shadow-lg hover:shadow-amber-500/20 transition-all active:scale-95">
                        Unlock Full Analysis
                    </button>
                </div>
            </div>

            {/* Content behind blur */}
            <div className="relative z-0">
                <h4 className="text-sm font-bold text-white mb-2">{card.label}</h4>

                {/* Impact figure — large, prominent, blurred */}
                <p className="text-2xl font-black text-amber-400 mb-1 blur-[5px] select-none tabular-nums">
                    {card.impactLabel}
                </p>
            </div>
        </div>
    );
};

const LockedPremiumInsights = () => {
    const { cards, maxFigureFormatted, hiddenCount } = useLockedInsights();

    if (!cards || cards.length === 0) return null;

    return (
        <div>
            <div className="flex items-center justify-between mb-3">
                <h3 className="text-xs font-bold uppercase tracking-[0.2em] text-slate-500">
                    Premium Insights Waiting For You
                </h3>
                <div className="flex items-center gap-1 text-amber-500">
                    <Lock className="w-3 h-3" />
                    <span className="text-[10px] font-bold uppercase tracking-widest">Pro</span>
                </div>
            </div>
            <div className="grid gap-3 sm:grid-cols-2">
                {cards.map(card => (
                    <LockedInsightCard key={card.id} card={card} />
                ))}
            </div>

            {/* CTA with largest figure */}
            <button className="mt-4 w-full py-3 bg-gradient-to-r from-amber-500 to-orange-500 text-sm font-bold text-black rounded-xl shadow-lg hover:shadow-amber-500/30 transition-all active:scale-[0.98]">
                Unlock Full Analysis — {maxFigureFormatted} waiting
                {hiddenCount > 0 && ` (+${hiddenCount} more)`}
            </button>
        </div>
    );
};

export default LockedPremiumInsights;
