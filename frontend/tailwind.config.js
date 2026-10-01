/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{vue,js,ts,jsx,tsx}"],
  theme: {
    // Mobile-first: unprefixed classes are for phones, and each breakpoint builds on the one below.
    // Listed smallest to largest - the order matters, later breakpoints win.
    screens: {
      xs: "400px", // large phones
      sm: "640px", // phones in landscape, small tablets
      md: "768px", // tablets
      lg: "1024px", // tablets in landscape, small laptops
      xl: "1280px", // laptops
      "2xl": "1536px", // desktops
      "3xl": "1920px", // large monitors
    },
    extend: {
      colors: {
        // Brand Identity Standards Manual, Section 3.4 - Colour System. The three brand hues are
        // unchanged; the neutrals are nudged for readability (the manual's #8A94A6 grey is only
        // ~3:1 on white, below WCAG AA for body text).
        "uni-navy": "#163D72",
        "campus-teal": "#2A9BB4",
        "academic-gold": "#B89A5E",
        "sky-blue": "#79C7E8",
        "slate-blue": "#5E78A6",
        "soft-grey": "#F4F6FA",
        "light-grey": "#E3E8EF",
        "medium-grey": "#64748B",
        charcoal: "#1F2937",
        success: "#16A34A",
        warning: "#D97706",
        danger: "#DC2626",
        info: "#2563EB",

        // Tonal scales built around the brand hues (700 = uni-navy, 500 = campus-teal,
        // 500 = academic-gold) for hover states, tints and gradients.
        navy: {
          50: "#EEF3FA",
          100: "#D9E4F3",
          200: "#B3C8E6",
          300: "#82A2D2",
          400: "#4F79B8",
          500: "#2C5A9A",
          600: "#1F4A86",
          700: "#163D72",
          800: "#102F59",
          900: "#0B2140",
          950: "#06152B",
        },
        teal: {
          50: "#EDF8FB",
          100: "#D2EEF5",
          200: "#A6DCEA",
          300: "#6FC4DA",
          400: "#44AEC7",
          500: "#2A9BB4",
          600: "#1F7F95",
          700: "#1A6678",
          800: "#185363",
          900: "#164554",
          950: "#0B2C37",
        },
        gold: {
          50: "#FAF6EE",
          100: "#F3EAD6",
          200: "#E6D4AE",
          300: "#D4B982",
          400: "#C5A76C",
          500: "#B89A5E",
          600: "#9A7D45",
          700: "#7B6337",
        },
      },
      fontFamily: {
        display: ["Poppins", "system-ui", "sans-serif"],
        sans: ["Inter", "system-ui", "sans-serif"],
        mono: ["JetBrains Mono", "monospace"],
      },
      borderRadius: {
        card: "16px",
        control: "10px",
        modal: "20px",
      },
      boxShadow: {
        xs: "0 1px 2px rgba(15, 23, 42, 0.04)",
        card: "0 1px 2px rgba(15, 23, 42, 0.04), 0 2px 6px -2px rgba(15, 23, 42, 0.06)",
        lift: "0 18px 40px -18px rgba(22, 61, 114, 0.35), 0 4px 10px -6px rgba(15, 23, 42, 0.08)",
        pop: "0 24px 60px -20px rgba(6, 21, 43, 0.35), 0 0 0 1px rgba(15, 23, 42, 0.05)",
        glow: "0 0 0 4px rgba(42, 155, 180, 0.18)",
      },
      keyframes: {
        "fade-in": { from: { opacity: "0" }, to: { opacity: "1" } },
        "fade-up": {
          from: { opacity: "0", transform: "translateY(8px)" },
          to: { opacity: "1", transform: "translateY(0)" },
        },
        "scale-in": {
          from: { opacity: "0", transform: "scale(0.96)" },
          to: { opacity: "1", transform: "scale(1)" },
        },
        shimmer: { from: { backgroundPosition: "200% 0" }, to: { backgroundPosition: "-200% 0" } },
        float: {
          "0%, 100%": { transform: "translateY(0)" },
          "50%": { transform: "translateY(-6px)" },
        },
      },
      animation: {
        "fade-in": "fade-in 0.25s ease-out both",
        "fade-up": "fade-up 0.4s cubic-bezier(0.22, 1, 0.36, 1) both",
        "scale-in": "scale-in 0.18s cubic-bezier(0.22, 1, 0.36, 1) both",
        shimmer: "shimmer 1.6s linear infinite",
        float: "float 5s ease-in-out infinite",
      },
    },
  },
  plugins: [],
};
