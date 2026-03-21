import { Navigate, Outlet } from 'react-router-dom';
import { useAuthStore } from '../../features/auth/store/useAuthStore';

const AUTH_REQUIRED = import.meta.env.VITE_AUTH_REQUIRED !== 'false';

export default function ProtectedRoute() {
    const user = useAuthStore((s) => s.user);

    // In production (VITE_AUTH_REQUIRED=false), skip login enforcement
    if (!AUTH_REQUIRED) {
        return <Outlet />;
    }

    if (!user) {
        return <Navigate to="/login" replace />;
    }

    return <Outlet />;
}
