import { Navigate, Outlet } from 'react-router-dom';
import { useAuthStore } from '../../features/auth/store/useAuthStore';

const AUTH_REQUIRED = import.meta.env.VITE_AUTH_REQUIRED !== 'false';

export default function ProtectedRoute() {
    const user = useAuthStore((s) => s.user);
    const isSessionValid = useAuthStore((s) => s.isSessionValid);
    const logout = useAuthStore((s) => s.logout);

    if (!AUTH_REQUIRED) {
        return <Outlet />;
    }

    if (!user || !isSessionValid()) {
        if (user) logout(); // clear expired session
        return <Navigate to="/login" replace />;
    }

    return <Outlet />;
}
