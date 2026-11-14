import { createContext } from 'react';

export interface AppContextType {
  appName: string;
  isReady: boolean;
}

export const AppContext = createContext<AppContextType | undefined>(undefined);

