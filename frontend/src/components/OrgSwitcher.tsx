import { useAuth0 } from '@auth0/auth0-react';

import { organizations } from '../config';

/**
 * Switches the active tenant.
 *
 * <p>This is the part people usually get wrong: with Auth0 Organizations, changing org is a full
 * re-authentication, not client-side state. The access token is *issued for* one organization, so a
 * new org means a new token, which means a round trip through Auth0. There is no way to "switch org"
 * locally without asking the authorization server -- and that is the point, since otherwise the
 * client would be choosing its own tenancy.
 */
export function OrgSwitcher({ currentOrgId }: { currentOrgId: string }) {
  const { loginWithRedirect } = useAuth0();

  return (
    <div className="org-switcher">
      <span className="label">Organization</span>
      {organizations.map((org) => {
        const active = org.id === currentOrgId;
        return (
          <button
            key={org.id}
            className={active ? 'org-tab active' : 'org-tab'}
            disabled={active}
            onClick={() =>
              loginWithRedirect({
                authorizationParams: { organization: org.id },
              })
            }
          >
            {org.label}
          </button>
        );
      })}
    </div>
  );
}
