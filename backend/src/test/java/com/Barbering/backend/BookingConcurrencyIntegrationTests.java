package com.Barbering.backend;

import com.Barbering.backend.dto.MarketplaceDtos.BookingCreate;
import com.Barbering.backend.entities.*;
import com.Barbering.backend.exception.ApiProblem;
import com.Barbering.backend.repositories.*;
import com.Barbering.backend.services.AppointmentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

/** Verifies the barber-row lock protects booking when requests race. */
@SpringBootTest
class BookingConcurrencyIntegrationTests {
    @Autowired private UserRepository users;
    @Autowired private BarberProfileRepository profiles;
    @Autowired private ServiceCatalogRepository catalog;
    @Autowired private BarberServiceOfferingRepository offerings;
    @Autowired private WorkingHourRepository hours;
    @Autowired private AppointmentService appointments;
    @Autowired private AppointmentRepository appointmentRepository;

    @Test
    void concurrentRequestsForOneSlotCreateOnlyOneAppointment() throws Exception {
        User barber = users.save(new User("barber-" + UUID.randomUUID() + "@example.com", "Test Barber", Role.BARBER));
        BarberProfile profile = profiles.save(new BarberProfile(barber.getId()));
        User firstCustomer = users.save(new User("customer-" + UUID.randomUUID() + "@example.com", "Customer One", Role.CUSTOMER));
        User secondCustomer = users.save(new User("customer-" + UUID.randomUUID() + "@example.com", "Customer Two", Role.CUSTOMER));
        ServiceCatalogItem service = catalog.save(new ServiceCatalogItem("Cut " + UUID.randomUUID(), "Test service"));
        BarberServiceOffering offering = offerings.save(new BarberServiceOffering(profile.getId(), service.getId(), new BigDecimal("30.00"), 30));

        LocalDate date = LocalDate.now(ZoneOffset.UTC).plusDays(1);
        hours.save(new WorkingHour(profile.getId(), date.getDayOfWeek().getValue(), LocalTime.of(9, 0), LocalTime.of(17, 0), true));
        OffsetDateTime startAt = date.atTime(10, 0).atOffset(ZoneOffset.UTC);
        BookingCreate request = new BookingCreate(startAt, null);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch fire = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<Throwable> first = pool.submit(() -> book(firstCustomer.getId(), profile.getId(), offering.getId(), request, ready, fire));
            Future<Throwable> second = pool.submit(() -> book(secondCustomer.getId(), profile.getId(), offering.getId(), request, ready, fire));
            ready.await();
            fire.countDown();
            List<Throwable> results = java.util.Arrays.asList(first.get(), second.get());

            assertThat(results.stream().filter(result -> result == null)).hasSize(1);
            assertThat(results.stream().filter(ApiProblem.class::isInstance).map(ApiProblem.class::cast)
                    .map(ApiProblem::getStatus).toList()).containsExactly(org.springframework.http.HttpStatus.CONFLICT);
            assertThat(appointmentRepository.findByBarberProfileIdOrderByStartAtAsc(profile.getId())).hasSize(1);
        } finally {
            pool.shutdownNow();
        }
    }

    private Throwable book(Long customerId, Long barberId, Long offeringId, BookingCreate request,
                           CountDownLatch ready, CountDownLatch fire) {
        ready.countDown();
        try {
            fire.await();
            appointments.create(customerId, barberId, offeringId, request);
            return null;
        } catch (Throwable ex) {
            return ex;
        }
    }
}
