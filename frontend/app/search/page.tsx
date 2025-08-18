'use client';
import { useSearchParams } from 'next/navigation';

export default function SearchPage() {
  const searchParams = useSearchParams();
  const query = searchParams.get('query');
  return (
    <div>
      <h1>Search Results</h1>
      <p>Searching for: {query || 'No query'}</p>
    </div>
  );
}
//URL: http://localhost:3000/search?query=barber