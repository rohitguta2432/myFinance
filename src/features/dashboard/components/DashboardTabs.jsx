import React from 'react';

const TABS = [
    { id: 'summary', label: 'Summary' },
    { id: 'actions', label: 'Action Plan' },
    { id: 'insurance', label: 'Insurance' },
    { id: 'tax', label: 'Tax' },
];

const DashboardTabs = ({ activeTab, onTabChange }) => {
    return (
        <div className="flex gap-1 bg-surface-dark rounded-xl p-1 border border-white/5">
            {TABS.map(t => (
                <button
                    key={t.id}
                    onClick={() => onTabChange(t.id)}
                    className={`px-4 py-2 rounded-lg text-xs font-bold tracking-wide transition-all ${activeTab === t.id
                        ? 'bg-primary/15 text-primary shadow-sm'
                        : 'text-slate-500 hover:text-slate-300'
                        }`}
                >
                    {t.label}
                </button>
            ))}
        </div>
    );
};

export { TABS };
export default DashboardTabs;
