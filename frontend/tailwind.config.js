/** @type {import('tailwindcss').Config} */
export default {
  darkMode: 'class',
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      fontFamily: {
        display: ['"Space Grotesk"', 'sans-serif'],
        body: ['"Manrope"', 'sans-serif'],
        mono: ['"JetBrains Mono"', 'monospace']
      },
      colors: {
        paper: 'var(--bg)',
        surface: 'var(--surface)',
        edge: 'var(--border)',
        ink: 'var(--ink)',
        'ink-muted': 'var(--ink-muted)',
        ledger: {
          DEFAULT: 'var(--primary)',
          dark: 'var(--primary-dark)'
        },
        gold: 'var(--gold)',
        danger: 'var(--danger)'
      }
    }
  },
  plugins: []
}
