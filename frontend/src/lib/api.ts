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

api.interceptors.response.use(
  (response) => response,
  (error) => {
    const method = (error.config?.method ?? "get").toLowerCase();
    if (error.response?.status === 401) {
      setToken(null);
    } else if (method !== "get") {
      // Only mutating requests (POST/PATCH/PUT/DELETE) toast on failure - those are always a
      // direct result of something the user just clicked. GET requests are excluded because many
      // of them are silent background/nice-to-have fetches (polling, prefetching stats) that
      // deliberately swallow errors elsewhere; toasting those would spam the user for failures
      // they never asked about. A failed page-load GET already has its own inline error state.
      useToastStore().error("Something went wrong", extractErrorMessage(error));
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
}

export function extractErrorMessage(err: unknown): string {
  if (axios.isAxiosError(err)) {
    const data = err.response?.data as ApiError | undefined;
    if (data?.message) return data.message;
    if (err.message) return err.message;
  }
  return "Something went wrong. Please try again.";
}
