import { defineStore } from "pinia";
import { api, getToken, setToken } from "@/lib/api";
import { useSavedListingsStore } from "@/stores/savedListings";
import { useFollowedProvidersStore } from "@/stores/followedProviders";
import { useNotificationsStore } from "@/stores/notifications";
import { useMessagesStore } from "@/stores/messages";
import { useToastStore } from "@/stores/toast";
import type { AuthResponse, UserResponse } from "@/lib/types";

export const useAuthStore = defineStore("auth", {
  state: () => ({
    user: null as UserResponse | null,
    token: getToken(),
    initialized: false,
  }),
  getters: {
    isAuthenticated: (state) => !!state.token && !!state.user,
    isSeller: (state) => state.user?.seller ?? false,
    isAdmin: (state) => state.user?.role === "ADMIN",
  },
  actions: {
    async login(email: string, password: string) {
      const { data } = await api.post<AuthResponse>("/auth/login", { email, password });
      this.applySession(data);
      useToastStore().success(`Welcome back, ${data.user.firstName}!`);
      return data.user;
    },
    async register(payload: {
      studentNumber: string;
      firstName: string;
      lastName: string;
      email: string;
      password: string;
    }) {
      await api.post("/auth/register", payload);
      useToastStore().success("Account created!", "We've emailed you a link to verify your student email.");
    },
    async fetchCurrentUser() {
      if (!this.token) {
        this.initialized = true;
        return;
      }
      try {
        const { data } = await api.get<UserResponse>("/users/me");
        this.user = data;
      } catch {
        // The API interceptor already explained a 401 (expired session, suspension...).
        this.clearSession();
      } finally {
        this.initialized = true;
      }
    },
    async becomeSeller(acceptedRules: boolean) {
      const { data } = await api.post<UserResponse>("/users/me/become-seller", { acceptedRules });
      this.user = data;
    },
    async updateProfile(payload: { firstName: string; lastName: string; phoneNumber: string; campus?: string }) {
      const { data } = await api.patch<UserResponse>("/users/me", payload);
      this.user = data;
    },
    applySession(auth: AuthResponse) {
      this.token = auth.token;
      this.user = auth.user;
      setToken(auth.token);
    },
    /** Forget the session without announcing it (the caller explains why, if needed). */
    clearSession() {
      this.token = null;
      this.user = null;
      setToken(null);
      useSavedListingsStore().reset();
      useFollowedProvidersStore().reset();
      useNotificationsStore().reset();
      useMessagesStore().reset();
    },
    logout() {
      this.clearSession();
      useToastStore().info("Signed out", "See you next time!");
    },
  },
});
