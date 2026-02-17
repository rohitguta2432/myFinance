export default function Home() {
    return (
        <div className="p-4 space-y-6 animate-in fade-in slide-in-from-bottom-4 duration-500">
            <header className="flex justify-between items-center pt-2">
                <div>
                    <h1 className="text-2xl font-bold text-slate-900 tracking-tight">Good Morning</h1>
                    <p className="text-slate-500 font-medium">Welcome back!</p>
                </div>
                <div className="w-10 h-10 bg-blue-100 rounded-full flex items-center justify-center text-blue-600 font-bold border-2 border-white shadow-sm ring-1 ring-blue-50">
                    RR
                </div>
            </header>

            <div className="relative overflow-hidden bg-gradient-to-br from-blue-600 to-indigo-700 text-white p-6 rounded-3xl shadow-lg shadow-blue-200/50 ring-1 ring-white/10">
                <div className="absolute top-0 right-0 w-32 h-32 bg-white/5 rounded-full -mr-16 -mt-16 blur-xl"></div>
                <div className="absolute bottom-0 left-0 w-24 h-24 bg-white/5 rounded-full -ml-12 -mb-12 blur-xl"></div>

                <p className="text-blue-100 text-sm font-medium relative z-10">Total Balance</p>
                <h2 className="text-4xl font-bold mt-1 tracking-tight relative z-10">$12,450.00</h2>

                <div className="mt-6 flex gap-3 relative z-10">
                    <button className="flex-1 bg-white/20 hover:bg-white/30 active:scale-95 backdrop-blur-md py-2.5 px-4 rounded-xl text-sm font-semibold transition-all border border-white/10 shadow-sm">
                        Add Money
                    </button>
                    <button className="flex-1 bg-white/20 hover:bg-white/30 active:scale-95 backdrop-blur-md py-2.5 px-4 rounded-xl text-sm font-semibold transition-all border border-white/10 shadow-sm">
                        Transfer
                    </button>
                </div>
            </div>

            <div>
                <div className="flex items-center justify-between mb-4">
                    <h3 className="text-lg font-bold text-slate-900">Recent Activity</h3>
                    <button className="text-sm font-medium text-blue-600 hover:text-blue-700">See All</button>
                </div>

                <div className="space-y-3">
                    {[
                        { id: 1, name: 'Grocery Store', time: 'Today, 10:23 AM', amnt: '-$45.20', icon: '🛍️', bg: 'bg-orange-100' },
                        { id: 2, name: 'Uber Ride', time: 'Yesterday, 8:45 PM', amnt: '-$24.50', icon: '🚗', bg: 'bg-gray-100' },
                        { id: 3, name: 'Spotify Premium', time: 'Jun 12, 2026', amnt: '-$12.99', icon: '🎵', bg: 'bg-green-100' },
                        { id: 4, name: 'Salary', time: 'Jun 01, 2026', amnt: '+$4,250.00', icon: '💰', bg: 'bg-emerald-100', text: 'text-emerald-600' }
                    ].map((item) => (
                        <div key={item.id} className="group bg-white p-4 rounded-2xl border border-slate-100 flex items-center justify-between shadow-sm active:scale-[0.99] transition-transform">
                            <div className="flex items-center gap-4">
                                <div className={`w-12 h-12 ${item.bg} rounded-2xl flex items-center justify-center text-xl`}>
                                    {item.icon}
                                </div>
                                <div>
                                    <p className="font-semibold text-slate-900">{item.name}</p>
                                    <p className="text-xs text-slate-500 font-medium">{item.time}</p>
                                </div>
                            </div>
                            <span className={`font-bold ${item.text || 'text-slate-900'}`}>{item.amnt}</span>
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
}
