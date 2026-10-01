import tailwindcss from "tailwindcss";
import autoprefixer from "autoprefixer";
import darkTheme from "./postcss/darkTheme.js";

export default {
  // darkTheme must run after Tailwind: it reads the colours Tailwind wrote out.
  plugins: [tailwindcss(), darkTheme(), autoprefixer()],
};
