import { create } from 'zustand';
import { persist } from 'zustand/middleware';

const SESSION_DURATION_MS = 7 * 24 * 60 * 60 * 1000; // 7 days

export const useAuthStore = create(
    persist(
        (set, get) => ({
            user: null, // { id, email, name, pictureUrl }
            token: null, // JWT from backend
            loginAt: null,

            login: (authResponse) => set({
                user: authResponse.user,
                token: authResponse.token,
                loginAt: Date.now(),
            }),

            logout: () => set({ user: null, token: null, loginAt: null }),

            isSessionValid: () => {
                const { user, token, loginAt } = get();
                if (!user || !token || !loginAt) return false;
                return Date.now() - loginAt < SESSION_DURATION_MS;
            },
        }),
        {
            name: 'auth-storage',
        }
    )
);
