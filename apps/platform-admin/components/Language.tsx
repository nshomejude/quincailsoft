'use client';
import {createContext,useContext} from 'react';
import type {Locale} from '@/lib/i18n';
const Context=createContext<Locale>('en');
export function LanguageProvider({locale,children}:{locale:Locale;children:React.ReactNode}){return <Context.Provider value={locale}>{children}</Context.Provider>}
export function useLanguage(){return useContext(Context)}
