import { create } from 'zustand';
import { persist } from 'zustand/middleware';

const SESSION_DURATION_MS = 7 * 24 * 60 * 60 * 1000; // 7 days

export const useAuthStore = create(
    persist(
        (set, get) => ({
            user: null, // { id, email, name, pictureUrl }
            loginAt: null,

            login: (user) => set({ user, loginAt: Date.now() }),

            logout: () => set({ user: null, loginAt: null }),

            isSessionValid: () => {
                const { user, loginAt } = get();
                if (!user || !loginAt) return false;
                return Date.now() - loginAt < SESSION_DURATION_MS;
            },
        }),
        {
            name: 'auth-storage',
        }
    )
);
