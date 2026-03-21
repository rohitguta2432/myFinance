import { create } from 'zustand';
import { persist } from 'zustand/middleware';

export const useAuthStore = create(
    persist(
        (set) => ({
            user: null, // { id, email, name, pictureUrl }

            login: (user) => set({ user }),

            logout: () => set({ user: null }),

            get isAuthenticated() {
                return this.user !== null;
            },
        }),
        {
            name: 'auth-storage',
        }
    )
);
