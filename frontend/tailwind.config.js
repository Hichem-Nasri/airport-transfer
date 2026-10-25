/** @type {import('tailwindcss').Config} */
module.exports = {
  content: ['./src/**/*.{html,ts}'],
  theme: {
    extend: {
      colors: {
        brand: { DEFAULT: '#D2001A', dark: '#A10015', soft: '#FFF6F6', line: '#EBCFD2' },
        ink: '#0B1B33',
        muted: '#4A5B73'
      },
      fontFamily: { sans: ['"Plus Jakarta Sans"', 'system-ui', 'sans-serif'] }
    }
  },
  plugins: []
};
