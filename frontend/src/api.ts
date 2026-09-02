import { useAuth0 } from '@auth0/auth0-react';
import { useCallback } from 'react';

import { config } from './config';

export type Project = {
  id: string;
  orgId: string;
  name: string;
  createdBy: string;
};

export type Me = {
  subject: string;
  orgId: string;
  roles: string[];
};

export class ApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.status = status;
  }
}

/**
 * Wraps fetch with the Auth0 access token.
 *
 * <p>This is the entire client side of the token flow: ask the SDK for a token, put it in the
 * Authorization header. The SDK handles caching and refreshing; we never see or store the token
 * ourselves, and nothing here inspects it -- the backend is what decides whether it is valid.
 */
export function useApi() {
  const { getAccessTokenSilently } = useAuth0();

  return useCallback(
    async <T,>(path: string, init: RequestInit = {}): Promise<T | null> => {
      const token = await getAccessTokenSilently();

      const response = await fetch(`${config.apiBase}${path}`, {
        ...init,
        headers: {
          ...init.headers,
          Authorization: `Bearer ${token}`,
          ...(init.body ? { 'Content-Type': 'application/json' } : {}),
        },
      });

      if (!response.ok) {
        throw new ApiError(
          response.status,
          response.status === 403
            ? 'Forbidden — your role in this organization does not allow that.'
            : `Request failed (${response.status})`,
        );
      }

      return response.status === 204 ? null : ((await response.json()) as T);
    },
    [getAccessTokenSilently],
  );
}
