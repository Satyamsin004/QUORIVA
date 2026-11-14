/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  darkMode: 'class',
  theme: {
    extend: {
      colors: {
        jet: {
          DEFAULT: '#09090B',
          950: '#09090B',
          900: '#111114',
        },
        surface: {
          elevated: '#171719',
          secondary: '#202025',
          tertiary: '#2A2A32',
        },
        hotpink: {
          DEFAULT: '#FF2D8D',
          hover: '#FF479D',
          light: '#FF6AAEC',
          glow: 'rgba(255, 45, 141, 0.25)',
        },
        chartreuse: {
          DEFAULT: '#C7F000',
          hover: '#D4F52B',
          dark: '#A6C800',
          glow: 'rgba(199, 240, 0, 0.25)',
        },
        accent: {
          pink: '#FF2D8D',
          lime: '#C7F000',
        },
        brand: {
          50: '#f0fdfa',
          100: '#ccfbf1',
          500: '#FF2D8D',
          600: '#E01E78',
          700: '#BD0F62',
          800: '#910B4B',
          900: '#680534',
        },
      },
      fontFamily: {
        sans: ['Inter', 'Plus Jakarta Sans', 'system-ui', 'sans-serif'],
        serif: ['Newsreader', 'Georgia', 'serif'],
        editorial: ['Newsreader', 'Georgia', 'serif'],
      },
      boxShadow: {
        'glow-pink': '0 0 35px -5px rgba(255, 45, 141, 0.35)',
        'glow-lime': '0 0 35px -5px rgba(199, 240, 0, 0.35)',
        'glow-subtle': '0 0 50px -10px rgba(255, 45, 141, 0.15)',
        'card-dark': '0 8px 32px 0 rgba(0, 0, 0, 0.37)',
      },
      animation: {
        'orb-float': 'orbFloat 8s ease-in-out infinite',
        'orb-glow': 'orbGlow 4s ease-in-out infinite alternate',
        'fade-in': 'fadeIn 0.5s ease-out forwards',
        'slide-up': 'slideUp 0.6s cubic-bezier(0.16, 1, 0.3, 1) forwards',
      },
      keyframes: {
        orbFloat: {
          '0%, 100%': { transform: 'translateY(0px) scale(1)' },
          '50%': { transform: 'translateY(-10px) scale(1.02)' },
        },
        orbGlow: {
          '0%': { filter: 'drop-shadow(0 0 25px rgba(255, 45, 141, 0.4)) drop-shadow(0 0 60px rgba(199, 240, 0, 0.2))' },
          '100%': { filter: 'drop-shadow(0 0 40px rgba(255, 45, 141, 0.6)) drop-shadow(0 0 80px rgba(199, 240, 0, 0.35))' },
        },
        fadeIn: {
          '0%': { opacity: '0' },
          '100%': { opacity: '1' },
        },
        slideUp: {
          '0%': { opacity: '0', transform: 'translateY(16px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
      },
    },
  },
  plugins: [],
};

