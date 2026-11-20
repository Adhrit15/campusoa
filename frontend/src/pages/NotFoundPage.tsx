import { Link } from 'react-router';

export function NotFoundPage() {
  return (
    <div className="card mx-auto max-w-md text-center">
      <h1 className="text-xl font-semibold">Page not found</h1>
      <Link to="/" className="btn-primary mt-4">Go home</Link>
    </div>
  );
}
