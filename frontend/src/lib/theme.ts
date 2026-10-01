import { ref, watch } from "vue";

export type ThemePreference = "system" | "light" | "dark";

// Keep in sync with the inline script in index.html, which applies the theme before first paint.
const STORAGE_KEY = "unilinkhub.theme";
const THEME_COLORS = { light: "#163D72", dark: "#0b1322" };

function readPreference(): ThemePreference {
  try {
    const saved = localStorage.getItem(STORAGE_KEY);
    return saved === "light" || saved === "dark" ? saved : "system";
  } catch {
    return "system";
  }
}

const media = window.matchMedia("(prefers-color-scheme: dark)");

export const themePreference = ref<ThemePreference>(readPreference());
export const isDark = ref(false);

function apply() {
  isDark.value = themePreference.value === "dark" || (themePreference.value === "system" && media.matches);
  document.documentElement.classList.toggle("dark", isDark.value);
  document.querySelector('meta[name="theme-color"]')?.setAttribute("content", isDark.value ? THEME_COLORS.dark : THEME_COLORS.light);
}

watch(themePreference, (pref) => {
  try {
    if (pref === "system") localStorage.removeItem(STORAGE_KEY);
    else localStorage.setItem(STORAGE_KEY, pref);
  } catch {
    // Private browsing: the choice just lasts for this visit.
  }
  apply();
});

media.addEventListener("change", apply);
apply();

/** The header toggle: switch to the opposite of what's on screen right now (this stops following the device). */
export function toggleTheme() {
  themePreference.value = isDark.value ? "light" : "dark";
}
