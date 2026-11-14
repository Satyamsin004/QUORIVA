import React, { useState, ReactNode } from 'react';
import { AppContext } from './context';

export const AppProvider: React.FC<{ children: ReactNode }> = ({ children }) => {
  const [appName] = useState('QUORIVA');
  const [isReady] = useState(true);

  return (
    <AppContext.Provider value={{ appName, isReady }}>
      {children}
    </AppContext.Provider>
  );
};
