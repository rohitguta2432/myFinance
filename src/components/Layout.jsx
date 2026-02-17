import { Outlet, NavLink } from 'react-router-dom';
// import { Home, PieChart, User } from 'lucide-react';
import { Home as HomeIcon, PieChart, User } from 'lucide-react';

export default function Layout() {
    return (
        <div className="flex flex-col h-dvh bg-slate-50 text-slate-900 font-sans">
            <main className="flex-1 overflow-y-auto pb-24 safe-area-pt safe-area-pb">
                <Outlet />
            </main>

            <nav className="fixed bottom-0 left-0 right-0 bg-white border-t border-slate-200 safe-area-pb z-50">
                <div className="flex justify-around items-center h-16">
                    <NavLink
                        to="/"
                        className={({ isActive }) => `flex flex-col items-center justify-center w-full h-full space-y-1 transition-colors duration-200 ${isActive ? 'text-blue-600' : 'text-slate-400 hover:text-slate-600'}`}
                    >
                        {({ isActive }) => (
                            <>
                                <HomeIcon size={24} strokeWidth={isActive ? 2.5 : 2} />
                                <span className="text-[10px] font-medium">Home</span>
                            </>
                        )}
                    </NavLink>
                    <NavLink
                        to="/transactions"
                        className={({ isActive }) => `flex flex-col items-center justify-center w-full h-full space-y-1 transition-colors duration-200 ${isActive ? 'text-blue-600' : 'text-slate-400 hover:text-slate-600'}`}
                    >
                        {({ isActive }) => (
                            <>
                                <PieChart size={24} strokeWidth={isActive ? 2.5 : 2} />
                                <span className="text-[10px] font-medium">Activity</span>
                            </>
                        )}
                    </NavLink>
                    <NavLink
                        to="/profile"
                        className={({ isActive }) => `flex flex-col items-center justify-center w-full h-full space-y-1 transition-colors duration-200 ${isActive ? 'text-blue-600' : 'text-slate-400 hover:text-slate-600'}`}
                    >
                        {({ isActive }) => (
                            <>
                                <User size={24} strokeWidth={isActive ? 2.5 : 2} />
                                <span className="text-[10px] font-medium">Profile</span>
                            </>
                        )}
                    </NavLink>
                </div>
            </nav>
        </div>
    );
}
