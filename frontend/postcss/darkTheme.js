import defaultColors from "tailwindcss/colors.js";
import tailwindConfig from "../tailwind.config.js";

/**
 * Dark mode without writing `dark:` on every element.
 *
 * Runs after Tailwind. Every rule that paints a light-theme colour (bg-white, text-charcoal,
 * border-light-grey, hover:bg-navy-50, the .card/.chip components, ...) gets a twin right after it:
 *
 *   .bg-white { background-color: rgb(255 255 255 / ...) }
 *   :where(.dark) .bg-white { background-color: rgb(17 26 43 / ...) }
 *
 * `:where()` adds no specificity, so the twin wins only because it comes straight after the
 * original - the cascade between utilities, variants and components behaves exactly as in light
 * mode. Colours not listed below (brand buttons, white text on them, the navy hero) stay as they are.
 */

// Light colour (palette name) -> dark replacement, per kind of property.
const SURFACES = {
  white: "#111a2b",
  "soft-grey": "#0b1322",
  "light-grey": "#243049",
  "slate-50": "#0f1828",
  "slate-100": "#1a2438",
  "slate-200": "#243049",
  "navy-50": "#15233b",
  "navy-100": "#1b2d4b",
  "navy-200": "#253b60",
  "teal-50": "#0e2a33",
  "teal-100": "#12343f",
  "teal-200": "#185363",
  "gold-50": "#28220f",
  "gold-100": "#352c18",
  "red-50": "#2a1517",
  "red-100": "#3b1a1d",
  "emerald-50": "#0e2a20",
  "emerald-100": "#11382a",
  "green-50": "#0f2a18",
  "amber-50": "#2a2210",
  "amber-100": "#3a2d12",
  "yellow-50": "#2a2610",
  "orange-50": "#2b1d10",
  "sky-50": "#0f2233",
  "blue-50": "#111f38",
  "violet-50": "#1d1833",
  "purple-50": "#21173a",
  "pink-50": "#2a1422",
  "rose-50": "#2a1418",
};

const TEXT = {
  charcoal: "#e2e8f0",
  "navy-950": "#f1f5fb",
  "navy-900": "#eef3fa",
  "navy-800": "#d9e4f3",
  "navy-700": "#dbe6f6", // = uni-navy: headings, links
  "navy-600": "#9db7de",
  "navy-500": "#82a2d2",
  "medium-grey": "#94a3b8",
  "slate-900": "#f1f5f9",
  "slate-800": "#e2e8f0",
  "slate-700": "#cbd5e1",
  "slate-600": "#a3b1c6",
  "slate-500": "#94a3b8",
  "teal-900": "#a6dcea",
  "teal-800": "#a6dcea",
  "teal-700": "#6fc4da",
  "teal-600": "#5bbbd2",
  "gold-700": "#e6d4ae",
  "gold-600": "#d4b982",
  danger: "#f87171",
  "red-900": "#fecaca",
  "red-800": "#fca5a5",
  "red-700": "#f87171",
  "red-600": "#f87171",
  success: "#4ade80",
  "green-700": "#4ade80",
  "emerald-900": "#a7f3d0",
  "emerald-800": "#6ee7b7",
  "emerald-700": "#34d399",
  "emerald-600": "#34d399",
  warning: "#fbbf24",
  "amber-900": "#fde68a",
  "amber-800": "#fcd34d",
  "amber-700": "#fbbf24",
  "amber-600": "#fbbf24",
  "orange-700": "#fdba74",
  "orange-600": "#fb923c",
  "sky-700": "#7dd3fc",
  "sky-600": "#38bdf8",
  info: "#60a5fa",
  "blue-700": "#93c5fd",
  "blue-600": "#60a5fa",
  "violet-700": "#c4b5fd",
  "violet-600": "#a78bfa",
  "pink-700": "#f9a8d4",
  "pink-600": "#f472b6",
};

const LINES = {
  white: "#111a2b",
  "soft-grey": "#1a2438",
  "light-grey": "#243049",
  "slate-100": "#1a2438",
  "slate-200": "#243049",
  "slate-300": "#334155",
  "navy-50": "#1b2d4b",
  "navy-100": "#253b60",
  "navy-200": "#2c4268",
  "teal-100": "#164554",
  "teal-200": "#185363",
  "gold-100": "#3d3219",
  "gold-200": "#4d3f20",
  "red-100": "#4c1d21",
  "red-200": "#5b2328",
  "emerald-100": "#14432f",
  "emerald-200": "#14532d",
  "amber-100": "#4a3610",
  "amber-200": "#5a4310",
  "sky-100": "#0c3a55",
  "violet-100": "#2e2558",
};

function kindOf(prop) {
  if (prop === "background-color" || prop === "background" || prop.startsWith("--tw-gradient-")) return SURFACES;
  if (prop === "color" || prop === "fill" || prop === "stroke" || prop === "caret-color" || prop === "text-decoration-color") return TEXT;
  if (/^border(-[a-z]+)?-color$/.test(prop) || prop === "outline-color" || prop === "--tw-ring-color" || prop === "--tw-ring-offset-color") return LINES;
  return null;
}

function toHex(r, g, b) {
  return "#" + [r, g, b].map((n) => Number(n).toString(16).padStart(2, "0")).join("");
}

function normalizeHex(hex) {
  const h = hex.toLowerCase();
  return h.length === 4 ? "#" + [...h.slice(1)].map((c) => c + c).join("") : h;
}

function rgbOf(hex) {
  const h = normalizeHex(hex);
  return [1, 3, 5].map((i) => parseInt(h.slice(i, i + 2), 16)).join(" ");
}

/** palette name -> hex, from Tailwind's defaults plus this project's brand colours. */
function paletteHex(name) {
  // Look names up one at a time: spreading Tailwind's colour list touches its deprecated
  // aliases (lightBlue, warmGray...), which prints a warning on every build.
  const own = tailwindConfig.theme.extend.colors;
  const lookup = (key) => (key in own ? own[key] : defaultColors[key]);
  const dash = name.lastIndexOf("-");
  const direct = lookup(name);
  if (typeof direct === "string") return direct;
  if (dash > 0) {
    const scale = lookup(name.slice(0, dash));
    if (scale && typeof scale === "object") return scale[name.slice(dash + 1)];
  }
  throw new Error(`darkTheme: unknown colour "${name}"`);
}

/** {light hex -> dark rgb triplet} for each kind. */
function compile(map) {
  const out = new Map();
  for (const [name, dark] of Object.entries(map)) out.set(normalizeHex(paletteHex(name)), rgbOf(dark));
  return out;
}

const COMPILED = new Map([
  [SURFACES, compile(SURFACES)],
  [TEXT, compile(TEXT)],
  [LINES, compile(LINES)],
]);

function swap(value, table) {
  let changed = false;
  const next = value
    .replace(/rgb\(\s*(\d+)[ ,]+(\d+)[ ,]+(\d+)/g, (m, r, g, b) => {
      const dark = table.get(toHex(r, g, b));
      if (!dark) return m;
      changed = true;
      return `rgb(${dark}`;
    })
    .replace(/#(?:[0-9a-fA-F]{6}|[0-9a-fA-F]{3})\b/g, (m) => {
      const dark = table.get(normalizeHex(m));
      if (!dark) return m;
      changed = true;
      return `rgb(${dark})`;
    });
  return changed ? next : null;
}

function darkSelector(selector) {
  const s = selector.trim();
  if (s === "html" || s.startsWith("html ") || s.startsWith("html:") || s.startsWith("html.")) return "html.dark" + s.slice(4);
  if (s === ":root") return ":root.dark";
  return `:where(.dark) ${s}`;
}

export default function darkTheme() {
  return {
    postcssPlugin: "unilinkhub-dark-theme",
    OnceExit(root) {
      root.walkRules((rule) => {
        if (rule.parent?.type === "atrule" && /keyframes$/i.test(rule.parent.name)) return;
        if (rule.selectors.some((s) => s.includes(".dark"))) return;
        const decls = [];
        rule.each((node) => {
          if (node.type !== "decl") return;
          const kind = kindOf(node.prop);
          if (!kind) return;
          const value = swap(node.value, COMPILED.get(kind));
          if (value) decls.push(node.clone({ value }));
        });
        if (decls.length === 0) return;
        const twin = rule.clone({ selectors: rule.selectors.map(darkSelector) });
        twin.removeAll();
        twin.append(decls);
        rule.after(twin);
      });
    },
  };
}
darkTheme.postcss = true;
