import { useAuth0 } from '@auth0/auth0-react';
import { useEffect, useState } from 'react';

import { ApiError, useApi, type Me } from './api';
import { ClaimsPanel } from './components/ClaimsPanel';
import { OrgSwitcher } from './components/OrgSwitcher';
import { ProjectList } from './components/ProjectList';
import { organizations } from './config';
import './App.css';

function Landing() {
  const { loginWithRedirect } = useAuth0();

  return (
    <main className="landing">
      <h1>TeamBoard</h1>
      <p className="muted">A multi-tenant demo built on Auth0 Organizations.</p>
      <div className="landing-actions">
        {organizations.map((org) => (
          <button
            key={org.id}
            onClick={() =>
              loginWithRedirect({ authorizationParams: { organization: org.id } })
            }
          >
            Log in to {org.label}
          </button>
        ))}
      </div>
      <p className="muted small">
        The same demo user is an admin in Acme and a member in Globex.
      </p>
    </main>
  );
}

function Workspace() {
  const { user, logout } = useAuth0();
  const api = useApi();
  const [me, setMe] = useState<Me | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    api<Me>('/api/me')
      .then((result) => {
        setMe(result);
        setError(null);
      })
      .catch((e) => {
        setError(
          e instanceof ApiError && e.status === 401
            ? 'The API rejected your token. If you logged in without picking an organization, log out and use one of the organization buttons.'
            : String(e),
        );
      });
  }, [api]);

  return (
    <>
      <header>
        <h1>TeamBoard</h1>
        <div className="header-right">
          <span className="muted">{user?.email ?? user?.name}</span>
          <button onClick={() => logout({ logoutParams: { returnTo: window.location.origin } })}>
            Log out
          </button>
        </div>
      </header>

      {error && <p className="error">{error}</p>}

      {me && (
        <main>
          <OrgSwitcher currentOrgId={me.orgId} />
          <ProjectList me={me} />
          <ClaimsPanel me={me} />
        </main>
      )}
    </>
  );
}

export default function App() {
  const { isLoading, isAuthenticated, error } = useAuth0();

  if (isLoading) return <p className="muted centered">Loading…</p>;
  if (error) return <p className="error centered">{error.message}</p>;

  return isAuthenticated ? <Workspace /> : <Landing />;
}
