import { useParams } from 'next/navigation';

export default function BookingConfirm() {
  const params = useParams();
  const { id } = params;
  return (
    <div>
      <h1>Booking Confirmed</h1>
      <p>Booking ID: {id}</p>
    </div>
  );
}
//URL: http://localhost:3000/booking/confirm/456