import { useEffect, useRef, useState, useCallback } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuthStore } from '../../features/auth/store/useAuthStore';
import { AlertTriangle, Clock } from 'lucide-react';

const INACTIVITY_LIMIT_MS = 20 * 60 * 1000; // 20 minutes
const WARNING_AT_MS = 15 * 60 * 1000;       // show warning at 15 minutes
const CHECK_INTERVAL_MS = 10 * 1000;         // check every 10 seconds

export default function InactivityGuard({ children }) {
    const navigate = useNavigate();
    const logout = useAuthStore((s) => s.logout);
    const user = useAuthStore((s) => s.user);

    const lastActivityRef = useRef(0);

    // Initialize on mount (avoids impure call during render)
    useEffect(() => { lastActivityRef.current = Date.now(); }, []);
    const [showWarning, setShowWarning] = useState(false);
    const [remainingSeconds, setRemainingSeconds] = useState(300);

    const resetActivity = useCallback(() => {
        lastActivityRef.current = Date.now();
        setShowWarning(false);
    }, []);

    // Track user activity events
    useEffect(() => {
        if (!user) return;

        const events = ['mousedown', 'keydown', 'scroll', 'touchstart', 'mousemove'];

        // Throttle: only update once per second max
        let lastUpdate = 0;
        const handler = () => {
            const now = Date.now();
            if (now - lastUpdate < 1000) return;
            lastUpdate = now;
            lastActivityRef.current = now;
            if (showWarning) setShowWarning(false);
        };

        events.forEach((e) => window.addEventListener(e, handler, { passive: true }));
        return () => events.forEach((e) => window.removeEventListener(e, handler));
    }, [user, showWarning]);

    // Periodic check for inactivity
    useEffect(() => {
        if (!user) return;

        const interval = setInterval(() => {
            const idle = Date.now() - lastActivityRef.current;

            if (idle >= INACTIVITY_LIMIT_MS) {
                clearInterval(interval);
                logout();
                navigate('/login', { replace: true });
                return;
            }

            if (idle >= WARNING_AT_MS) {
                setShowWarning(true);
                setRemainingSeconds(Math.ceil((INACTIVITY_LIMIT_MS - idle) / 1000));
            }
        }, CHECK_INTERVAL_MS);

        return () => clearInterval(interval);
    }, [user, logout, navigate]);

    // Countdown timer when warning is visible
    useEffect(() => {
        if (!showWarning) return;

        const countdown = setInterval(() => {
            const idle = Date.now() - lastActivityRef.current;
            const remaining = Math.ceil((INACTIVITY_LIMIT_MS - idle) / 1000);

            if (remaining <= 0) {
                clearInterval(countdown);
                logout();
                navigate('/login', { replace: true });
                return;
            }

            setRemainingSeconds(remaining);
        }, 1000);

        return () => clearInterval(countdown);
    }, [showWarning, logout, navigate]);

    const minutes = Math.floor(remainingSeconds / 60);
    const seconds = remainingSeconds % 60;

    return (
        <>
            {children}

            {/* Inactivity Warning Modal */}
            {showWarning && (
                <div className="fixed inset-0 z-[9999] flex items-center justify-center p-4">
                    <div className="absolute inset-0 bg-black/70 backdrop-blur-sm" />
                    <div className="relative bg-surface-dark w-full max-w-sm rounded-2xl p-6 shadow-2xl border border-white/10 text-center animate-scale-in">
                        <div className="w-14 h-14 bg-amber-500/10 rounded-full flex items-center justify-center mx-auto mb-4 border border-amber-500/20">
                            <AlertTriangle className="w-7 h-7 text-amber-500" />
                        </div>

                        <h3 className="text-lg font-bold text-white mb-2">Session Expiring Soon</h3>
                        <p className="text-sm text-slate-400 mb-4">
                            You've been inactive for a while. Your session will expire in:
                        </p>

                        <div className="flex items-center justify-center gap-2 mb-5">
                            <Clock className="w-5 h-5 text-amber-400" />
                            <span className="text-3xl font-black text-amber-400 tabular-nums">
                                {minutes}:{seconds.toString().padStart(2, '0')}
                            </span>
                        </div>

                        <button
                            onClick={resetActivity}
                            className="w-full py-3 bg-primary hover:bg-primary/90 text-background-dark font-bold rounded-xl transition-all"
                        >
                            I'm Still Here
                        </button>
                        <p className="text-xs text-slate-600 mt-3">
                            Any mouse or keyboard activity will also reset the timer
                        </p>
                    </div>
                </div>
            )}
        </>
    );
}
