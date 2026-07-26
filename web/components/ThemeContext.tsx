'use client';

import React, { createContext, useContext, useEffect, useState } from 'react';
import { Language, translations } from '@/lib/i18n';

type Theme = 'light' | 'dark' | 'system';

interface ThemeContextType {
  theme: Theme;
  setTheme: (t: Theme) => void;
  lang: Language;
  setLang: (l: Language) => void;
  t: typeof translations['en'];
}

const ThemeContext = createContext<ThemeContextType | undefined>(undefined);

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const [theme, setThemeState] = useState<Theme>('dark');
  const [lang, setLangState] = useState<Language>('en');

  useEffect(() => {
    const savedTheme = (localStorage.getItem('loomora_theme') as Theme) || 'dark';
    const savedLang = (localStorage.getItem('loomora_lang') as Language) || 'en';
    setThemeState(savedTheme);
    setLangState(savedLang);
    applyTheme(savedTheme);
  }, []);

  const setTheme = (t: Theme) => {
    setThemeState(t);
    localStorage.setItem('loomora_theme', t);
    applyTheme(t);
  };

  const setLang = (l: Language) => {
    setLangState(l);
    localStorage.setItem('loomora_lang', l);
  };

  const applyTheme = (t: Theme) => {
    const root = document.documentElement;
    if (t === 'dark') {
      root.classList.add('dark');
      root.classList.remove('light');
    } else if (t === 'light') {
      root.classList.add('light');
      root.classList.remove('dark');
    } else {
      const systemDark = window.matchMedia('(prefers-color-scheme: dark)').matches;
      if (systemDark) {
        root.classList.add('dark');
        root.classList.remove('light');
      } else {
        root.classList.add('light');
        root.classList.remove('dark');
      }
    }
  };

  return (
    <ThemeContext.Provider
      value={{
        theme,
        setTheme,
        lang,
        setLang,
        t: translations[lang],
      }}
    >
      {children}
    </ThemeContext.Provider>
  );
}

export function useTheme() {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error('useTheme must be used within ThemeProvider');
  }
  return context;
}
