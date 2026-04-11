import React, { useState } from 'react';
import { useQuery } from '@tanstack/react-query';
import { useNavigate } from 'react-router-dom';
import { api } from '../../../services/api';
import {
    Users, Activity, CheckCircle2, Wallet, ChevronRight, ChevronLeft, X,
    Target, LayoutDashboard, Shield, HelpCircle, LogOut, Download,
    Search, Bell, TrendingUp, Filter, Clock, FileText
} from 'lucide-react';
import ThemeToggle from '../../../components/ui/ThemeToggle';

const fmt = (v) => {
    if (v == null || isNaN(v)) return '₹0';
    const abs = Math.abs(v);
    const sign = v < 0 ? '-' : '';
    if (abs >= 10000000) return `${sign}₹${(abs / 10000000).toFixed(2)} Cr`;
    if (abs >= 100000) return `${sign}₹${(abs / 100000).toFixed(1)}L`;
    if (abs >= 1000) return `${sign}₹${Math.round(abs).toLocaleString('en-IN')}`;
    return `${sign}₹${Math.round(abs)}`;
};

const fmtDate = (d) => d ? new Date(d).toLocaleDateString('en-IN', { day: 'numeric', month: 'short', year: 'numeric' }) : '—';

const timeAgo = (d) => {
    if (!d) return '—';
    const diff = Date.now() - new Date(d).getTime();
    const mins = Math.floor(diff / 60000);
    if (mins < 60) return mins <= 1 ? 'Just now' : `${mins}m ago`;
    const hrs = Math.floor(mins / 60);
    if (hrs < 24) return `${hrs}h ago`;
    const days = Math.floor(hrs / 24);
    return `${days}d ago`;
};

/* ─── Sidebar ─── */
const SIDEBAR_ITEMS = [
    { id: 'overview', label: 'Overview', icon: LayoutDashboard },
    { id: 'audit', label: 'Audit Logs', icon: FileText },
];

const Sidebar = ({ activeTab, setActiveTab, onExport }) => {
    const navigate = useNavigate();
    return (
        <aside className="w-56 shrink-0 bg-surface-dark border-r border-white/5 flex flex-col h-screen sticky top-0">
            {/* Brand */}
            <div className="px-5 py-5 border-b border-white/5">
                <div className="flex items-center gap-2.5">
                    <div className="w-9 h-9 rounded-xl bg-primary/15 flex items-center justify-center">
                        <Shield className="w-5 h-5 text-primary" />
                    </div>
                    <div>
                        <p className="text-sm font-bold text-slate-800 tracking-tight">MyFinancial</p>
                        <p className="text-xs text-slate-500 uppercase tracking-widest font-semibold">Admin Panel</p>
                    </div>
                </div>
            </div>

            {/* Nav */}
            <nav className="flex-1 p-3 space-y-1 pt-5">
                {SIDEBAR_ITEMS.map((item) => {
                    const Icon = item.icon;
                    const isActive = activeTab === item.id;
                    return (
                        <button key={item.id} onClick={() => setActiveTab(item.id)} className={`w-full flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-left transition-all cursor-pointer ${
                            isActive ? 'bg-primary/10 text-primary font-semibold' : 'text-slate-500 hover:text-slate-300 hover:bg-white/5'
                        }`}>
                            <Icon className="w-[18px] h-[18px]" />
                            <span className="text-sm">{item.label}</span>
                        </button>
                    );
                })}
            </nav>

            {/* Bottom */}
            <div className="p-3 space-y-1 border-t border-white/5">
                <button onClick={onExport} className="w-full flex items-center justify-center gap-2 px-4 py-2.5 rounded-xl bg-primary text-background-dark text-sm font-bold cursor-pointer hover:opacity-90 transition-opacity">
                    <Download className="w-4 h-4" /> Export Data
                </button>
                <button onClick={() => navigate('/')} className="w-full flex items-center gap-3 px-3.5 py-2.5 rounded-xl text-slate-500 hover:text-slate-300 hover:bg-white/5 cursor-pointer text-sm">
                    <LogOut className="w-[18px] h-[18px]" /> Back to App
                </button>
            </div>
        </aside>
    );
};

/* ─── Top Bar ─── */
const TopBar = () => (
    <header className="h-14 border-b border-white/5 flex items-center px-6 gap-4 bg-surface-dark/80 backdrop-blur-md sticky top-0 z-30">
        <h2 className="text-base font-bold text-slate-800 mr-auto">Admin Dashboard</h2>
        <div className="relative">
            <Search className="w-4 h-4 absolute left-3 top-1/2 -translate-y-1/2 text-slate-500" />
            <input placeholder="Search users..." className="pl-9 pr-4 py-2 text-sm bg-white/5 border border-white/5 rounded-xl text-slate-300 placeholder:text-slate-600 focus:outline-none focus:border-primary/30 w-64" />
        </div>
        <ThemeToggle />
        <button className="p-2 rounded-lg hover:bg-white/5 text-slate-500 relative cursor-pointer">
            <Bell className="w-[18px] h-[18px]" />
        </button>
    </header>
);

/* ─── Stat Card ─── */
const StatCard = ({ label, value, icon: Icon, color, bg, accent }) => (
    <div className={`rounded-2xl border p-5 flex flex-col gap-3 ${accent ? 'bg-primary border-primary/30 text-background-dark' : 'bg-surface-dark border-white/5'}`}>
        <div className="flex items-center justify-between">
            <div className={`w-10 h-10 rounded-xl flex items-center justify-center ${accent ? 'bg-white/20' : bg}`}>
                <Icon className={`w-5 h-5 ${accent ? 'text-background-dark' : color}`} />
            </div>
        </div>
        <div>
            <p className={`text-xs uppercase tracking-[0.12em] font-semibold ${accent ? 'text-background-dark/60' : 'text-slate-500'}`}>{label}</p>
            <p className={`text-2xl font-bold tabular-nums tracking-tight mt-0.5 ${accent ? 'text-background-dark' : 'text-slate-800'}`}>{value}</p>
        </div>
    </div>
);

/* ─── Goal Progress Ring ─── */
const GoalRing = ({ pct }) => {
    const r = 60, stroke = 8;
    const circ = 2 * Math.PI * r;
    const offset = circ - (pct / 100) * circ;
    return (
        <div className="relative flex items-center justify-center">
            <svg width="150" height="150" viewBox="0 0 150 150" className="-rotate-90">
                <circle cx="75" cy="75" r={r} fill="none" className="stroke-slate-500/10" strokeWidth={stroke} />
                <circle cx="75" cy="75" r={r} fill="none" stroke="#0ab842" strokeWidth={stroke}
                    strokeDasharray={circ} strokeDashoffset={offset} strokeLinecap="round"
                    className="transition-all duration-1000 ease-out" />
            </svg>
            <div className="absolute flex flex-col items-center">
                <span className="text-3xl font-bold text-slate-800 tabular-nums">{pct}%</span>
                <span className="text-xs uppercase tracking-[0.15em] font-semibold text-slate-500">Reached</span>
            </div>
        </div>
    );
};

/* ─── Step Badge ─── */
const StepBadge = ({ steps }) => {
    const color = steps === 6 ? 'bg-emerald-500/15 text-emerald-400 border-emerald-500/20'
        : steps >= 4 ? 'bg-amber-500/15 text-amber-400 border-amber-500/20'
        : 'bg-red-500/15 text-red-400 border-red-500/20';
    return <span className={`text-sm font-bold px-2.5 py-1 rounded-full border ${color}`}>{steps}/6</span>;
};

/* ─── User Detail Panel ─── */
const UserDetailPanel = ({ userId, onClose }) => {
    const { data, isLoading } = useQuery({
        queryKey: ['admin-user-detail', userId],
        queryFn: () => api.get(`/admin/users/${userId}`),
        enabled: !!userId,
    });

    if (isLoading) return (
        <div className="fixed inset-y-0 right-0 w-[440px] bg-surface-dark border-l border-white/5 shadow-2xl z-50 p-6 overflow-y-auto">
            <div className="animate-pulse space-y-4">
                {[...Array(8)].map((_, i) => <div key={i} className="h-8 bg-slate-500/10 rounded-lg" />)}
            </div>
        </div>
    );

    if (!data) return null;
    const { summary: s } = data;

    const steps = [
        { label: 'Profile', done: data.hasProfile },
        { label: 'Cash Flow', done: data.hasCashFlow },
        { label: 'Net Worth', done: data.hasNetWorth },
        { label: 'Goals', done: data.hasGoals },
        { label: 'Insurance', done: data.hasInsurance },
        { label: 'Tax', done: data.hasTax },
    ];

    return (
        <div className="fixed inset-y-0 right-0 w-[440px] bg-surface-dark border-l border-white/5 shadow-2xl z-50 overflow-y-auto">
            <div className="sticky top-0 bg-surface-dark/95 backdrop-blur-md border-b border-white/5 p-5 flex items-center gap-3 z-10">
                <img src={s.pictureUrl || `https://ui-avatars.com/api/?name=${encodeURIComponent(s.name || 'U')}&background=0ab842&color=fff&size=44`}
                    className="w-11 h-11 rounded-full" alt="" />
                <div className="flex-1 min-w-0">
                    <p className="text-base font-bold text-slate-800 truncate">{s.name}</p>
                    <p className="text-xs text-slate-500 truncate">{s.email}</p>
                </div>
                <button onClick={onClose} className="p-2 rounded-lg hover:bg-white/5 text-slate-500 cursor-pointer">
                    <X className="w-5 h-5" />
                </button>
            </div>

            <div className="p-5 space-y-5">
                <div className="flex flex-wrap gap-3 text-xs text-slate-500">
                    {s.city && <span className="px-2.5 py-1 rounded-lg bg-white/5 border border-white/5">{s.city}, {s.state}</span>}
                    {s.age && <span className="px-2.5 py-1 rounded-lg bg-white/5 border border-white/5">Age {s.age}</span>}
                    <span className="px-2.5 py-1 rounded-lg bg-white/5 border border-white/5">Joined {fmtDate(s.createdAt)}</span>
                </div>

                <Section title="Assessment Progress">
                    <div className="grid grid-cols-3 gap-2">
                        {steps.map((st) => (
                            <div key={st.label} className={`flex items-center gap-1.5 text-xs px-2.5 py-2 rounded-xl border ${st.done ? 'bg-emerald-500/10 border-emerald-500/20 text-emerald-400' : 'bg-white/[0.03] border-white/5 text-slate-500'}`}>
                                {st.done ? <CheckCircle2 className="w-3.5 h-3.5" /> : <div className="w-3.5 h-3.5 rounded-full border border-slate-600" />}
                                {st.label}
                            </div>
                        ))}
                    </div>
                </Section>

                <Section title="Financial Summary">
                    <div className="space-y-2.5">
                        <Row label="Net Worth" value={fmt(s.netWorth)} highlight={s.netWorth > 0} />
                        <Row label="Monthly Income" value={fmt(s.monthlyIncome)} />
                        <Row label="Monthly Expenses" value={fmt(s.monthlyExpenses)} />
                        <Row label="Savings Rate" value={`${s.savingsRate?.toFixed(0) || 0}%`} highlight={s.savingsRate >= 20} warn={s.savingsRate < 10} />
                        <Row label="Total Assets" value={fmt(data.totalAssets)} />
                        <Row label="Total Liabilities" value={fmt(data.totalLiabilities)} warn={data.totalLiabilities > 0} />
                        <Row label="EMI/Income Ratio" value={`${data.emiToIncomeRatio?.toFixed(0) || 0}%`} warn={data.emiToIncomeRatio > 40} />
                    </div>
                </Section>

                <Section title="Insurance">
                    <div className="space-y-2.5">
                        <Row label="Term Life Cover" value={data.termLifeCover > 0 ? fmt(data.termLifeCover) : 'No cover'} warn={data.termLifeCover === 0} />
                        <Row label="Health Cover" value={data.healthCover > 0 ? fmt(data.healthCover) : 'No cover'} warn={data.healthCover === 0} />
                    </div>
                </Section>

                {data.taxRegime && (
                    <Section title="Tax">
                        <div className="space-y-2.5">
                            <Row label="Regime" value={data.taxRegime.replace('_', ' ')} />
                            {data.taxSaved > 0 && <Row label="Tax Saved" value={fmt(data.taxSaved)} highlight />}
                        </div>
                    </Section>
                )}

                {data.goals?.length > 0 && (
                    <Section title={`Goals (${data.goals.length})`}>
                        <div className="space-y-2.5">
                            {data.goals.map((g, i) => (
                                <div key={i} className="flex items-center justify-between text-sm">
                                    <div className="flex items-center gap-2">
                                        <Target className="w-4 h-4 text-primary" />
                                        <span className="text-slate-300">{g.name || g.type}</span>
                                    </div>
                                    <span className="text-slate-500 text-xs">{fmt(g.targetAmount)} · {g.horizonYears}yr</span>
                                </div>
                            ))}
                        </div>
                    </Section>
                )}

                {data.riskTolerance && (
                    <Section title="Risk Profile">
                        <div className="flex items-center gap-2">
                            <span className={`text-xs font-bold px-3 py-1.5 rounded-full border ${
                                data.riskTolerance === 'AGGRESSIVE' ? 'bg-red-500/10 border-red-500/20 text-red-400' :
                                data.riskTolerance === 'MODERATE' ? 'bg-amber-500/10 border-amber-500/20 text-amber-400' :
                                'bg-emerald-500/10 border-emerald-500/20 text-emerald-400'
                            }`}>{data.riskTolerance}</span>
                            {data.riskScore != null && <span className="text-sm text-slate-500">Score: {data.riskScore}</span>}
                        </div>
                    </Section>
                )}
            </div>
        </div>
    );
};

const Section = ({ title, children }) => (
    <div>
        <h4 className="text-xs uppercase tracking-[0.15em] font-bold text-slate-500 mb-3">{title}</h4>
        {children}
    </div>
);

const Row = ({ label, value, highlight, warn }) => (
    <div className="flex justify-between items-center text-sm">
        <span className="text-slate-500">{label}</span>
        <span className={`font-mono font-semibold ${warn ? 'text-red-400' : highlight ? 'text-primary' : 'text-slate-300'}`}>{value}</span>
    </div>
);

/* ─── Main Admin Page ─── */
/* ─── Audit Logs View ─── */
const AuditLogsView = () => {
    const { data: logs, isLoading } = useQuery({
        queryKey: ['admin-audit-logs'],
        queryFn: () => api.get('/admin/audit-logs'),
        refetchInterval: 10000,
    });

    const actionColors = {
        LOGIN: 'bg-blue-500/15 text-blue-400 border-blue-500/20',
        SAVE_PROFILE: 'bg-emerald-500/15 text-emerald-400 border-emerald-500/20',
        ADD_INCOME: 'bg-emerald-500/15 text-emerald-400 border-emerald-500/20',
        ADD_EXPENSE: 'bg-amber-500/15 text-amber-400 border-amber-500/20',
        ADD_ASSET: 'bg-emerald-500/15 text-emerald-400 border-emerald-500/20',
        ADD_LIABILITY: 'bg-amber-500/15 text-amber-400 border-amber-500/20',
        ADD_GOAL: 'bg-emerald-500/15 text-emerald-400 border-emerald-500/20',
        SAVE_INSURANCE: 'bg-purple-500/15 text-purple-400 border-purple-500/20',
        SAVE_TAX: 'bg-cyan-500/15 text-cyan-400 border-cyan-500/20',
        DELETE_INCOME: 'bg-red-500/15 text-red-400 border-red-500/20',
        DELETE_EXPENSE: 'bg-red-500/15 text-red-400 border-red-500/20',
        DELETE_ASSET: 'bg-red-500/15 text-red-400 border-red-500/20',
        DELETE_LIABILITY: 'bg-red-500/15 text-red-400 border-red-500/20',
        DELETE_GOAL: 'bg-red-500/15 text-red-400 border-red-500/20',
    };

    return (
        <div className="space-y-6">
            <div>
                <p className="text-xs uppercase tracking-[0.15em] font-semibold text-slate-500 mb-1">System Logs</p>
                <h1 className="text-2xl font-bold text-slate-800 tracking-tight">Audit Logs</h1>
                <p className="text-sm text-slate-500 mt-1">Real-time activity feed — auto-refreshes every 10s</p>
            </div>

            <div className="bg-surface-dark rounded-2xl border border-white/5 overflow-hidden">
                {isLoading ? (
                    <div className="p-6 space-y-3">
                        {[1,2,3,4,5].map(i => <div key={i} className="h-14 bg-slate-500/10 rounded-xl animate-pulse" />)}
                    </div>
                ) : !logs?.length ? (
                    <div className="p-12 text-center text-slate-500">
                        <Clock className="w-10 h-10 mx-auto mb-3 opacity-30" />
                        <p className="text-sm">No audit logs yet. Logs appear as users interact with the app.</p>
                    </div>
                ) : (
                    <div className="divide-y divide-white/[0.03]">
                        {logs.map((log) => (
                            <div key={log.id} className="px-6 py-4 flex items-center gap-4 hover:bg-white/[0.02] transition-colors">
                                <div className="w-10 h-10 rounded-full bg-white/5 flex items-center justify-center shrink-0">
                                    <img src={`https://ui-avatars.com/api/?name=${encodeURIComponent(log.userName || 'U')}&background=0ab842&color=fff&size=40`}
                                        className="w-10 h-10 rounded-full" alt="" />
                                </div>
                                <div className="flex-1 min-w-0">
                                    <div className="flex items-center gap-2">
                                        <span className="text-sm font-semibold text-slate-800">{log.userName || 'Unknown'}</span>
                                        <span className={`text-xs font-bold uppercase px-2 py-0.5 rounded-full border ${actionColors[log.action] || 'bg-slate-500/15 text-slate-400 border-slate-500/20'}`}>
                                            {log.action?.replace('_', ' ')}
                                        </span>
                                    </div>
                                    <p className="text-xs text-slate-500 mt-0.5">
                                        {log.entity && <span className="capitalize">{log.entity}</span>}
                                        {log.entityId && <span> #{log.entityId}</span>}
                                        {log.details && <span> — {log.details}</span>}
                                    </p>
                                </div>
                                <div className="text-right shrink-0">
                                    <p className="text-xs text-slate-500">{timeAgo(log.createdAt)}</p>
                                    <p className="text-xs text-slate-600">{log.userEmail}</p>
                                </div>
                            </div>
                        ))}
                    </div>
                )}
            </div>
        </div>
    );
};

/* ─── Main Admin Page ─── */
export default function AdminDashboard() {
    const [selectedUserId, setSelectedUserId] = useState(null);
    const [activeTab, setActiveTab] = useState('overview');
    const [page, setPage] = useState(1);
    const perPage = 10;

    const { data: stats, isLoading: statsLoading } = useQuery({
        queryKey: ['admin-stats'],
        queryFn: () => api.get('/admin/stats'),
    });

    const { data: users, isLoading: usersLoading } = useQuery({
        queryKey: ['admin-users'],
        queryFn: () => api.get('/admin/users'),
    });

    const { data: activity } = useQuery({
        queryKey: ['admin-activity'],
        queryFn: () => api.get('/admin/activity?days=7'),
    });

    const totalPages = users ? Math.ceil(users.length / perPage) : 1;
    const pagedUsers = users?.slice((page - 1) * perPage, page * perPage);

    const handleExport = () => {
        if (!users) return;
        const csv = [
            ['Name', 'Email', 'City', 'Joined', 'Steps', 'Net Worth', 'Monthly Income', 'Savings Rate'],
            ...users.map(u => [u.name, u.email, u.city || '', fmtDate(u.createdAt), `${u.stepsCompleted}/6`, u.netWorth, u.monthlyIncome, `${u.savingsRate?.toFixed(0) || 0}%`])
        ].map(r => r.join(',')).join('\n');
        const blob = new Blob([csv], { type: 'text/csv' });
        const url = URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url; a.download = 'myfinance-users.csv'; a.click();
        URL.revokeObjectURL(url);
    };

    // Compute assessment completion rate for goal ring
    const completionPct = stats && stats.totalUsers > 0
        ? Math.round((stats.assessmentsCompleted / stats.totalUsers) * 100) : 0;

    return (
        <div className="flex min-h-screen bg-background-dark">
            <Sidebar activeTab={activeTab} setActiveTab={setActiveTab} onExport={handleExport} />

            <div className="flex-1 flex flex-col min-w-0">
                <TopBar />

                <main className="flex-1 p-6 space-y-6 overflow-y-auto">
                    {activeTab === 'audit' ? <AuditLogsView /> : <>
                    {/* Section Header */}
                    <div>
                        <p className="text-xs uppercase tracking-[0.15em] font-semibold text-slate-500 mb-1">Admin Intelligence</p>
                        <h1 className="text-2xl font-bold text-slate-800 tracking-tight">Fiscal Overview</h1>
                    </div>

                    {/* Stats Row */}
                    {statsLoading ? (
                        <div className="grid grid-cols-4 gap-4">
                            {[1,2,3,4].map(i => <div key={i} className="h-28 bg-surface-dark rounded-2xl border border-white/5 animate-pulse" />)}
                        </div>
                    ) : stats && (
                        <div className="grid grid-cols-4 gap-4">
                            <StatCard label="Total Users" value={stats.totalUsers} icon={Users} color="text-blue-400" bg="bg-blue-500/10" />
                            <StatCard label="Active Today" value={stats.activeToday} icon={Activity} color="text-emerald-400" bg="bg-emerald-500/10" />
                            <StatCard label="Assessments Done" value={stats.assessmentsCompleted} icon={CheckCircle2} color="text-amber-400" bg="bg-amber-500/10" />
                            <StatCard label="Total Net Worth" value={fmt(stats.totalNetWorthTracked)} icon={Wallet} accent />
                        </div>
                    )}

                    {/* Middle Row: Placeholder + Goal Ring */}
                    <div className="grid grid-cols-3 gap-4">
                        {/* System Performance placeholder */}
                        <div className="col-span-2 bg-surface-dark rounded-2xl border border-white/5 p-6">
                            <div className="flex items-center justify-between mb-4">
                                <div>
                                    <h3 className="text-base font-bold text-slate-800">User Activity</h3>
                                    <p className="text-xs text-slate-500 mt-0.5">Sign-ups & logins over time</p>
                                </div>
                                <span className="text-xs text-slate-500 border border-white/5 px-3 py-1.5 rounded-lg">Last 7 Days</span>
                            </div>
                            <div className="h-48 flex items-end justify-between gap-2 px-4">
                                {(activity || []).map((day, i) => {
                                    const maxActions = Math.max(...(activity || []).map(d => d.actions || 1), 1);
                                    const pct = Math.max(5, ((day.actions || 0) / maxActions) * 100);
                                    const loginPct = day.actions > 0 ? Math.max(20, (day.logins / day.actions) * 100) : 0;
                                    const label = new Date(day.date + 'T00:00:00').toLocaleDateString('en-IN', { weekday: 'short' });
                                    return (
                                        <div key={i} className="flex-1 flex flex-col items-center gap-2 group relative">
                                            <div className="absolute -top-8 left-1/2 -translate-x-1/2 bg-surface-dark border border-white/10 rounded-lg px-2 py-1 text-xs text-slate-300 opacity-0 group-hover:opacity-100 transition-opacity whitespace-nowrap z-10">
                                                {day.logins} logins · {day.actions} actions
                                            </div>
                                            <div className="w-full rounded-t-lg bg-primary/20 transition-all" style={{ height: `${pct}%` }}>
                                                <div className="w-full rounded-t-lg bg-primary transition-all" style={{ height: `${loginPct}%` }} />
                                            </div>
                                            <span className="text-xs text-slate-500 uppercase font-semibold">{label}</span>
                                        </div>
                                    );
                                })}
                                {!activity?.length && (
                                    <div className="w-full h-full flex items-center justify-center text-xs text-slate-500">
                                        No activity data yet
                                    </div>
                                )}
                            </div>
                        </div>

                        {/* Goal Progress */}
                        <div className="bg-surface-dark rounded-2xl border border-white/5 p-6 flex flex-col items-center justify-center">
                            <div className="flex items-center justify-between w-full mb-4">
                                <h3 className="text-base font-bold text-slate-800">Completion Rate</h3>
                                <TrendingUp className="w-5 h-5 text-primary" />
                            </div>
                            <p className="text-xs text-slate-500 mb-4 text-center">Users who completed all 6 assessment steps.</p>
                            <GoalRing pct={completionPct} />
                            <div className="flex justify-between w-full mt-4 text-xs text-slate-500">
                                <span>Done: <b className="text-slate-300">{stats?.assessmentsCompleted || 0}</b></span>
                                <span>Total: <b className="text-slate-300">{stats?.totalUsers || 0}</b></span>
                            </div>
                        </div>
                    </div>

                    {/* Users Table */}
                    <div className="bg-surface-dark rounded-2xl border border-white/5 overflow-hidden">
                        <div className="px-6 py-4 border-b border-white/5 flex items-center justify-between">
                            <div>
                                <h3 className="text-base font-bold text-slate-800">All Users</h3>
                                <p className="text-xs text-slate-500 mt-0.5">Detailed breakdown of registered accounts</p>
                            </div>
                            <button className="flex items-center gap-2 text-xs font-semibold text-slate-500 border border-white/5 px-3.5 py-2 rounded-xl hover:bg-white/5 cursor-pointer">
                                <Filter className="w-3.5 h-3.5" /> Filter
                            </button>
                        </div>

                        {usersLoading ? (
                            <div className="p-6 space-y-3">
                                {[1,2,3].map(i => <div key={i} className="h-16 bg-slate-500/10 rounded-xl animate-pulse" />)}
                            </div>
                        ) : (
                            <>
                                <div className="overflow-x-auto">
                                    <table className="w-full text-sm">
                                        <thead>
                                            <tr className="border-b border-white/5 text-slate-500 uppercase tracking-wider text-xs">
                                                <th className="text-left px-6 py-3.5 font-semibold">User</th>
                                                <th className="text-left px-4 py-3.5 font-semibold">City</th>
                                                <th className="text-left px-4 py-3.5 font-semibold">Joined</th>
                                                <th className="text-left px-4 py-3.5 font-semibold">Last Login</th>
                                                <th className="text-center px-4 py-3.5 font-semibold">Steps</th>
                                                <th className="text-right px-4 py-3.5 font-semibold">Net Worth</th>
                                                <th className="text-right px-4 py-3.5 font-semibold">Income/mo</th>
                                                <th className="text-right px-4 py-3.5 font-semibold">Savings %</th>
                                                <th className="px-6 py-3.5"></th>
                                            </tr>
                                        </thead>
                                        <tbody>
                                            {pagedUsers?.map((u) => (
                                                <tr key={u.id} className="border-b border-white/[0.03] hover:bg-white/[0.02] transition-colors h-[68px]">
                                                    <td className="px-6 py-3">
                                                        <div className="flex items-center gap-3">
                                                            <img src={u.pictureUrl || `https://ui-avatars.com/api/?name=${encodeURIComponent(u.name || 'U')}&background=0ab842&color=fff&size=40`}
                                                                className="w-10 h-10 rounded-full shrink-0" alt="" />
                                                            <div>
                                                                <p className="font-semibold text-slate-800">{u.name}</p>
                                                                <p className="text-xs text-slate-500">{u.email}</p>
                                                            </div>
                                                        </div>
                                                    </td>
                                                    <td className="px-4 py-3 text-slate-400">{u.city || '—'}</td>
                                                    <td className="px-4 py-3 text-slate-400">{fmtDate(u.createdAt)}</td>
                                                    <td className="px-4 py-3 text-slate-400">{timeAgo(u.lastLoginAt)}</td>
                                                    <td className="px-4 py-3 text-center"><StepBadge steps={u.stepsCompleted} /></td>
                                                    <td className="px-4 py-3 text-right font-mono font-semibold text-slate-300">{fmt(u.netWorth)}</td>
                                                    <td className="px-4 py-3 text-right font-mono text-slate-300">{fmt(u.monthlyIncome)}</td>
                                                    <td className="px-4 py-3 text-right">
                                                        <span className={`font-mono font-bold px-2.5 py-1 rounded-full text-xs ${
                                                            u.savingsRate >= 40 ? 'bg-emerald-500/15 text-emerald-400' :
                                                            u.savingsRate >= 20 ? 'bg-emerald-500/10 text-emerald-400' :
                                                            u.savingsRate >= 10 ? 'bg-amber-500/10 text-amber-400' :
                                                            'bg-red-500/10 text-red-400'
                                                        }`}>
                                                            {u.savingsRate?.toFixed(0) || 0}%
                                                        </span>
                                                    </td>
                                                    <td className="px-6 py-3 text-right">
                                                        <button onClick={() => setSelectedUserId(u.id)}
                                                            className="text-primary hover:underline font-semibold text-xs cursor-pointer flex items-center gap-1 ml-auto">
                                                            View <ChevronRight className="w-3.5 h-3.5" />
                                                        </button>
                                                    </td>
                                                </tr>
                                            ))}
                                        </tbody>
                                    </table>
                                </div>

                                {/* Pagination */}
                                <div className="px-6 py-3 border-t border-white/5 flex items-center justify-between">
                                    <p className="text-xs text-slate-500 uppercase tracking-wider font-semibold">
                                        Showing {pagedUsers?.length || 0} of {users?.length || 0} users
                                    </p>
                                    {totalPages > 1 && (
                                        <div className="flex items-center gap-1">
                                            <button onClick={() => setPage(p => Math.max(1, p - 1))} disabled={page === 1}
                                                className="p-1.5 rounded-lg hover:bg-white/5 text-slate-500 disabled:opacity-30 cursor-pointer">
                                                <ChevronLeft className="w-4 h-4" />
                                            </button>
                                            {[...Array(totalPages)].map((_, i) => (
                                                <button key={i} onClick={() => setPage(i + 1)}
                                                    className={`w-8 h-8 rounded-lg text-xs font-bold cursor-pointer ${page === i + 1 ? 'bg-primary text-background-dark' : 'text-slate-500 hover:bg-white/5'}`}>
                                                    {i + 1}
                                                </button>
                                            ))}
                                            <button onClick={() => setPage(p => Math.min(totalPages, p + 1))} disabled={page === totalPages}
                                                className="p-1.5 rounded-lg hover:bg-white/5 text-slate-500 disabled:opacity-30 cursor-pointer">
                                                <ChevronRight className="w-4 h-4" />
                                            </button>
                                        </div>
                                    )}
                                </div>
                            </>
                        )}
                    </div>
                    </>}
                </main>
            </div>

            {/* Detail Panel */}
            {selectedUserId && (
                <>
                    <div className="fixed inset-0 bg-black/40 z-40" onClick={() => setSelectedUserId(null)} />
                    <UserDetailPanel userId={selectedUserId} onClose={() => setSelectedUserId(null)} />
                </>
            )}
        </div>
    );
}
