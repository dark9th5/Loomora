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

const readStoredTheme = (): Theme => {
  if (typeof window === 'undefined') return 'dark';
  return (window.localStorage.getItem('loomora_theme') as Theme | null) ?? 'dark';
};

const readStoredLanguage = (): Language => {
  if (typeof window === 'undefined') return 'en';
  return (window.localStorage.getItem('loomora_lang') as Language | null) ?? 'en';
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

export function ThemeProvider({ children }: { children: React.ReactNode }) {
  const [theme, setThemeState] = useState<Theme>(readStoredTheme);
  const [lang, setLangState] = useState<Language>(readStoredLanguage);

  useEffect(() => {
    localStorage.setItem('loomora_theme', theme);
    applyTheme(theme);
  }, [theme]);

  useEffect(() => {
    localStorage.setItem('loomora_lang', lang);
  }, [lang]);

  const setTheme = (t: Theme) => {
    setThemeState(t);
  };

  const setLang = (l: Language) => {
    setLangState(l);
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
