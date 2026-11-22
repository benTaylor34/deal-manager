import { Link, Navigate, Route, Routes } from 'react-router-dom';
import { DEMO_USERS, useCurrentUser } from './auth';
import { DealListPage } from './pages/DealListPage';
import { DealFormPage } from './pages/DealFormPage';
import { DealDetailPage } from './pages/DealDetailPage';

export function App() {
  const { user, switchUser } = useCurrentUser();
  return (
    <>
      <header className="app-header">
        <Link to="/" className="brand">
          Deal Manager
        </Link>
        <label className="user-switch">
          Acting as
          <select value={user.username} onChange={(e) => switchUser(e.target.value)}>
            {DEMO_USERS.map((u) => (
              <option key={u.username} value={u.username}>
                {u.label} ({u.username})
              </option>
            ))}
          </select>
        </label>
      </header>
      <main className="container">
        <Routes>
          <Route path="/" element={<DealListPage />} />
          <Route path="/deals/new" element={<DealFormPage />} />
          <Route path="/deals/:id" element={<DealDetailPage />} />
          <Route path="*" element={<Navigate to="/" replace />} />
        </Routes>
      </main>
    </>
  );
}
