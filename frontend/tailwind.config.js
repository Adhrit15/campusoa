/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  // "class" strategy: dark mode is on when <html class="dark">. ThemeToggle sets it from the
  // user's choice, falling back to the OS setting (prefers-color-scheme).
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        brand: {
          50: '#eef5ff',
          100: '#d9e8ff',
          500: '#3b6ef5',
          600: '#2a55d8',
          700: '#2343ad',
        },
      },
      fontFamily: {
        sans: ['Inter', 'ui-sans-serif', 'system-ui', 'Segoe UI', 'Roboto', 'sans-serif'],
      },
    },
  },
  plugins: [],
};
