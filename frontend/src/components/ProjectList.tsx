import { useCallback, useEffect, useState } from 'react';

import { ApiError, useApi, type Me, type Project } from '../api';

export function ProjectList({ me }: { me: Me }) {
  const api = useApi();
  const [projects, setProjects] = useState<Project[]>([]);
  const [newName, setNewName] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  // Hiding the controls is a UX affordance, not a security boundary. The same check exists in the
  // Spring controller via @PreAuthorize, and that is the one that actually matters -- anyone can
  // edit this condition in devtools, but they cannot forge a role claim in a signed token.
  const isAdmin = me.roles.includes('admin');

  const refresh = useCallback(async () => {
    try {
      setProjects((await api<Project[]>('/api/projects')) ?? []);
      setError(null);
    } catch (e) {
      setError(e instanceof ApiError ? e.message : String(e));
    } finally {
      setLoading(false);
    }
  }, [api]);

  // Re-fetches whenever the identity changes, which includes switching organization.
  useEffect(() => {
    void refresh();
  }, [refresh, me.orgId]);

  async function create(event: React.FormEvent) {
    event.preventDefault();
    if (!newName.trim()) return;
    try {
      await api<Project>('/api/projects', {
        method: 'POST',
        body: JSON.stringify({ name: newName.trim() }),
      });
      setNewName('');
      await refresh();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : String(e));
    }
  }

  async function remove(id: string) {
    try {
      await api<null>(`/api/projects/${id}`, { method: 'DELETE' });
      await refresh();
    } catch (e) {
      setError(e instanceof ApiError ? e.message : String(e));
    }
  }

  return (
    <section className="projects">
      <h2>Projects</h2>

      {error && <p className="error">{error}</p>}

      {loading ? (
        <p className="muted">Loading…</p>
      ) : projects.length === 0 ? (
        <p className="muted">No projects in this organization yet.</p>
      ) : (
        <ul>
          {projects.map((project) => (
            <li key={project.id}>
              <span>{project.name}</span>
              {isAdmin && (
                <button className="danger" onClick={() => remove(project.id)}>
                  Delete
                </button>
              )}
            </li>
          ))}
        </ul>
      )}

      {isAdmin ? (
        <form onSubmit={create} className="create">
          <input
            value={newName}
            onChange={(e) => setNewName(e.target.value)}
            placeholder="New project name"
            aria-label="New project name"
          />
          <button type="submit">Add project</button>
        </form>
      ) : (
        <p className="muted">
          You are a <strong>member</strong> here — read-only. Switch to an organization where you
          are an admin to create or delete projects.
        </p>
      )}
    </section>
  );
}
