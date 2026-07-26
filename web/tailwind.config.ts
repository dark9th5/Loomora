import type { Config } from 'tailwindcss';

const config: Config = {
  darkMode: 'class',
  content: [
    './app/**/*.{js,ts,jsx,tsx,mdx}',
    './components/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {
      colors: {
        loomora: {
          primary: '#6750A4',
          'on-primary': '#FFFFFF',
          container: '#EADDFF',
          'on-container': '#21005D',
          secondary: '#03DAC6',
          recording: '#B00020',
          darkBg: '#0F0E13',
          darkSurface: '#1C1B20',
          darkBorder: '#2B2930',
        },
      },
    },
  },
  plugins: [],
};

export default config;
