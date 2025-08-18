import { useParams } from 'next/navigation';

export default function BarberProfile() {
  const params = useParams();
  const { id } = params;
  return (
    <div>
      <h1>Barber Profile</h1>
      <p>Barber ID: {id}</p>
    </div>
  );
}
//URL: http://localhost:3000/barbers/123