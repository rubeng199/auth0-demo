function required(name: string): string {
  const value = import.meta.env[name as keyof ImportMetaEnv] as string | undefined;
  if (!value) {
    throw new Error(
      `Missing ${name}. Copy .env.example to .env.local and fill it in (see README.md).`,
    );
  }
  return value;
}

export const config = {
  domain: required('VITE_AUTH0_DOMAIN'),
  clientId: required('VITE_AUTH0_CLIENT_ID'),
  audience: required('VITE_AUTH0_AUDIENCE'),
  apiBase: required('VITE_API_BASE'),
};

/**
 * The organizations this demo can switch between.
 *
 * <p>Hard-coded because it is a two-tenant demo. A real product would look these up per user --
 * Auth0's Management API can list the organizations a user belongs to.
 */
export const organizations = [
  { id: required('VITE_ORG_ACME'), label: 'Acme Inc' },
  { id: required('VITE_ORG_GLOBEX'), label: 'Globex Corp' },
];
