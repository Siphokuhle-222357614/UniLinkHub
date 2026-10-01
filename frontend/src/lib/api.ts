import axios, { type InternalAxiosRequestConfig } from "axios";
import { useToastStore } from "@/stores/toast";

export const api = axios.create({
  baseURL: "/api",
});

const TOKEN_KEY = "unilinkhub.token";

export function getToken(): string | null {
  return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token: string | null) {
  if (token) {
    localStorage.setItem(TOKEN_KEY, token);
  } else {
    localStorage.removeItem(TOKEN_KEY);
  }
}

api.interceptors.request.use((config: InternalAxiosRequestConfig) => {
  const token = getToken();
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

const TOAST_TITLES: Record<number, string> = {
  400: "Please check that",
  403: "That isn't allowed",
  404: "We couldn't find that",
  409: "That couldn't be done",
  413: "That file is too big",
};

api.interceptors.response.use(
  (response) => response,
  async (error) => {
    const method = (error.config?.method ?? "get").toLowerCase();
    const url: string = error.config?.url ?? "";
    const status: number | undefined = error.response?.status;

    if (status === 401) {
      const hadSession = !!error.config?.headers?.Authorization;
      setToken(null);
      if (hadSession) {
        // The server says why (session expired, account suspended...) - say it, then go to login.
        const [{ useAuthStore }, { default: router }] = await Promise.all([import("@/stores/auth"), import("@/router")]);
        useAuthStore().clearSession();
        useToastStore().error("You've been logged out", extractErrorMessage(error));
        const current = router.currentRoute.value;
        if (current.meta.requiresAuth) {
          router.push({ name: "login", query: { redirect: current.fullPath } });
        }
      }
    } else if (method !== "get" && !url.startsWith("/auth/")) {
      // Only mutating requests toast on failure - those are always a direct result of something
      // the user just clicked. GETs are excluded because many are silent background fetches that
      // deliberately swallow errors, and a failed page-load GET has its own inline error state.
      // Auth pages (login, register...) show their errors inline instead.
      useToastStore().error(TOAST_TITLES[status ?? 0] ?? "Something went wrong", extractErrorMessage(error));
    }
    return Promise.reject(error);
  },
);

export interface ApiError {
  timestamp: string;
  status: number;
  error: string;
  message: string;
  path: string;
  /** Machine-readable reason the UI can act on, e.g. EMAIL_NOT_VERIFIED, ADMIN_ACCOUNT, RESTRICTED_ITEM. */
  code?: string;
}

export function extractErrorMessage(err: unknown): string {
  if (axios.isAxiosError(err)) {
    const data = err.response?.data as ApiError | undefined;
    if (data?.message) return data.message;
    if (!err.response) return "We couldn't reach UniLinkHub. Check your internet connection and try again.";
  }
  return "Something went wrong. Please try again in a moment.";
}

export function extractErrorCode(err: unknown): string | undefined {
  return axios.isAxiosError(err) ? (err.response?.data as ApiError | undefined)?.code : undefined;
}
