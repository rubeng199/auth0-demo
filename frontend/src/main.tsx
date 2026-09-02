import { StrictMode } from 'react';
import { createRoot } from 'react-dom/client';
import { Auth0Provider } from '@auth0/auth0-react';

import App from './App.tsx';
import { config } from './config.ts';
import './index.css';

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <Auth0Provider
      domain={config.domain}
      clientId={config.clientId}
      authorizationParams={{
        redirect_uri: window.location.origin,
        // Asking for an audience is what makes Auth0 return a JWT access token for the Spring API.
        // Omit it and you get an opaque token that the backend cannot validate.
        audience: config.audience,
      }}
      // Organizations log the user in against a specific tenant. Without refresh tokens the SDK
      // would need a silent iframe re-auth on every page load, which third-party cookie blocking
      // breaks -- so a refresh would bounce the user back to the login screen.
      useRefreshTokens
      cacheLocation="localstorage"
    >
      <App />
    </Auth0Provider>
  </StrictMode>,
);
