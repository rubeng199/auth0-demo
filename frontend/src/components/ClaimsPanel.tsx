import type { Me } from '../api';
import { organizations } from '../config';

/**
 * Shows what the *backend* derived from the access token.
 *
 * <p>Deliberately sourced from GET /api/me rather than from the SDK's local user object, so what
 * you see is the server's conclusion after validating signature, issuer, audience and org -- not
 * the browser's opinion about itself.
 */
export function ClaimsPanel({ me }: { me: Me }) {
  const orgLabel =
    organizations.find((org) => org.id === me.orgId)?.label ?? 'Unknown organization';

  return (
    <section className="claims">
      <h2>What the API sees in your token</h2>
      <dl>
        <dt>sub</dt>
        <dd>
          <code>{me.subject}</code>
        </dd>

        <dt>org_id</dt>
        <dd>
          <code>{me.orgId}</code> <span className="muted">({orgLabel})</span>
        </dd>

        <dt>roles</dt>
        <dd>
          {me.roles.length === 0 ? (
            <span className="muted">none — is the post-login Action deployed?</span>
          ) : (
            me.roles.map((role) => (
              <span key={role} className="badge">
                {role}
              </span>
            ))
          )}
        </dd>
      </dl>
    </section>
  );
}
