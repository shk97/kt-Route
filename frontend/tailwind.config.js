/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      fontFamily: {
        sans: ['Plus Jakarta Sans', 'Inter', 'ui-sans-serif', 'system-ui', 'sans-serif'],
        display: ['Plus Jakarta Sans', 'Inter', 'sans-serif'],
      },
      colors: {
        navy: {
          950: '#06101f',
          900: '#0c1929',
          800: '#132238',
          700: '#1a2d4a',
          600: '#22395e',
          500: '#2b4a7a',
          400: '#3a5f9a',
          300: '#5a80b8',
          200: '#8aaad4',
          100: '#c4d5ea',
          50:  '#eef4fb',
        },
        gold: {
          700: '#b45309',
          600: '#d97706',
          500: '#f59e0b',
          400: '#fbbf24',
          300: '#fcd34d',
          200: '#fde68a',
          100: '#fef3c7',
          50:  '#fffbeb',
        },
        brand: {
          DEFAULT: '#0c1929',
          sidebar: '#06101f',
          accent:  '#d97706',
          light:   '#2b4a7a',
        },
        surface: {
          DEFAULT: '#f8f7f5',
          card:    '#ffffff',
          muted:   '#f1f0ed',
        },
      },
      boxShadow: {
        'card':   '0 1px 3px rgba(0,0,0,0.06), 0 4px 16px rgba(0,0,0,0.06)',
        'card-hover': '0 4px 12px rgba(0,0,0,0.08), 0 12px 32px rgba(0,0,0,0.1)',
        'stat':   '0 2px 8px rgba(12,25,41,0.08), 0 8px 24px rgba(12,25,41,0.06)',
        'nav':    '0 2px 16px rgba(6,16,31,0.24)',
        'modal':  '0 20px 60px rgba(0,0,0,0.3)',
        'gold':   '0 4px 14px rgba(217,119,6,0.35)',
      },
      backgroundImage: {
        'hero-gradient': 'linear-gradient(135deg, #06101f 0%, #0c1929 40%, #132238 70%, #1a2d4a 100%)',
        'gold-gradient': 'linear-gradient(135deg, #f59e0b 0%, #d97706 100%)',
        'card-gradient': 'linear-gradient(145deg, #ffffff 0%, #f8f7f5 100%)',
        'sidebar-gradient': 'linear-gradient(180deg, #06101f 0%, #0c1929 100%)',
      },
      borderRadius: {
        'xl':  '12px',
        '2xl': '16px',
        '3xl': '24px',
      },
      animation: {
        'spin-slow': 'spin 1.5s linear infinite',
        'fade-in': 'fadeIn 0.3s ease-out',
        'slide-up': 'slideUp 0.3s ease-out',
      },
      keyframes: {
        fadeIn: { from: { opacity: 0 }, to: { opacity: 1 } },
        slideUp: { from: { opacity: 0, transform: 'translateY(12px)' }, to: { opacity: 1, transform: 'translateY(0)' } },
      },
    },
  },
  plugins: [],
}
