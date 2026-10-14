package com.Barbering.backend.services;

import com.Barbering.backend.dto.MarketplaceDtos.*;
import com.Barbering.backend.entities.*;
import com.Barbering.backend.dto.UserDto;
import com.Barbering.backend.exception.ApiProblem;
import com.Barbering.backend.repositories.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.*;
import java.util.List;

@Service
@Transactional
public class AppointmentService {
    private static final List<AppointmentStatus> BLOCKING_STATUSES = List.of(AppointmentStatus.PENDING, AppointmentStatus.CONFIRMED);
    private static final Duration CUSTOMER_CANCELLATION_NOTICE = Duration.ofHours(24);
    private static final int SLOT_INTERVAL_MINUTES = 15;
    private static final int MAX_BOOKING_DAYS_AHEAD = 90;

    private final BarberProfileRepository profiles;
    private final BarberServiceOfferingRepository offerings;
    private final WorkingHourRepository hours;
    private final BarberBreakRepository breaks;
    private final AppointmentRepository appointments;
    private final UserRepository users;
    private final ServiceCatalogRepository catalog;
    private final ReviewRepository reviews;
    private final FavouriteService favouriteService;

    public AppointmentService(BarberProfileRepository profiles, BarberServiceOfferingRepository offerings,
                              WorkingHourRepository hours, BarberBreakRepository breaks,
                              AppointmentRepository appointments, UserRepository users,
                              ServiceCatalogRepository catalog,
                              ReviewRepository reviews,
                              FavouriteService favouriteService) {
        this.profiles = profiles;
        this.offerings = offerings;
        this.hours = hours;
        this.breaks = breaks;
        this.appointments = appointments;
        this.users = users;
        this.catalog = catalog;
        this.reviews = reviews;
        this.favouriteService = favouriteService;
    }

    @Transactional(readOnly = true)
    public AvailabilityDto availability(Long barberId, Long offeringId, LocalDate date) {
        BarberProfile profile = requireProfile(barberId);
        BarberServiceOffering offering = activeOffering(barberId, offeringId);
        ZoneId zone = ZoneId.of(profile.getTimezoneId());
        validateBookingDate(date, zone);
        WorkingHour working = hours.findByBarberProfileIdAndDayOfWeek(barberId, date.getDayOfWeek().getValue())
                .filter(WorkingHour::isEnabled).orElse(null);
        if (working == null) return new AvailabilityDto(date, zone.getId(), List.of());

        LocalDateTime localOpen = LocalDateTime.of(date, working.getStartTime());
        LocalDateTime localClose = LocalDateTime.of(date, working.getEndTime());
        List<BarberBreak> dayBreaks = breaks.findByBarberProfileIdAndDayOfWeekOrderByStartTime(barberId, date.getDayOfWeek().getValue());
        List<AvailabilitySlot> result = new java.util.ArrayList<>();
        LocalDateTime localStart = nextSlotBoundary(localOpen);
        while (!localStart.plusMinutes(offering.getDurationMinutes()).isAfter(localClose)) {
            LocalDateTime localEnd = localStart.plusMinutes(offering.getDurationMinutes());
            boolean duringBreak = false;
            for (BarberBreak entry : dayBreaks) {
                if (localStart.toLocalTime().isBefore(entry.getEndTime()) && localEnd.toLocalTime().isAfter(entry.getStartTime())) {
                    duringBreak = true;
                    break;
                }
            }
            if (!duringBreak) {
                for (ZoneOffset offset : zone.getRules().getValidOffsets(localStart)) {
                    OffsetDateTime start = localStart.atOffset(offset);
                    OffsetDateTime end = start.plusMinutes(offering.getDurationMinutes());
                    if (!start.toInstant().isAfter(OffsetDateTime.now(ZoneOffset.UTC).toInstant())) continue;
                    if (appointments.findOverlapping(barberId, utc(start), utc(end), BLOCKING_STATUSES, null).isEmpty()) {
                        result.add(new AvailabilitySlot(start, end, localStart.toLocalTime(), localEnd.toLocalTime()));
                    }
                }
            }
            localStart = localStart.plusMinutes(SLOT_INTERVAL_MINUTES);
        }
        return new AvailabilityDto(date, zone.getId(), result);
    }

    public AppointmentDto create(Long customerId, Long barberId, Long offeringId, BookingCreate input) {
        BarberProfile profile = profiles.lockById(barberId)
                .orElseThrow(() -> ApiProblem.notFound("Barber was not found."));
        BarberServiceOffering offering = activeOffering(barberId, offeringId);
        OffsetDateTime start = validateAndNormalizeSlot(profile, offering, input.startAt(), null);
        Appointment appointment = new Appointment();
        appointment.setCustomerId(customerId);
        appointment.setBarberProfileId(barberId);
        appointment.setBarberServiceId(offeringId);
        appointment.setServiceNameSnapshot(serviceName(offering));
        appointment.setPriceAmount(offering.getPriceAmount());
        appointment.setDurationMinutes(offering.getDurationMinutes());
        appointment.setStartAt(start);
        appointment.setEndAt(start.plusMinutes(offering.getDurationMinutes()));
        appointment.setStatus(AppointmentStatus.PENDING);
        appointment.setCustomerNote(clean(input.customerNote()));
        return toDto(appointments.saveAndFlush(appointment));
    }

    @Transactional(readOnly = true)
    public List<AppointmentDto> customerAppointments(Long customerId) {
        return appointments.findByCustomerIdOrderByStartAtDesc(customerId).stream().map(this::toDto).toList();
    }

    @Transactional(readOnly = true)
    public List<AppointmentDto> barberAppointments(Long userId) {
        BarberProfile profile = requireProfileByUser(userId);
        return appointments.findByBarberProfileIdOrderByStartAtAsc(profile.getId()).stream().map(this::toDto).toList();
    }

    public AppointmentDto cancel(Long customerId, Long appointmentId) {
        Appointment appointment = appointments.lockByIdAndCustomerId(appointmentId, customerId)
                .orElseThrow(() -> ApiProblem.notFound("Appointment was not found."));
        if (appointment.getStatus() != AppointmentStatus.PENDING && appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw ApiProblem.conflict("This appointment can no longer be cancelled.");
        }
        if (!appointment.getStartAt().toInstant().isAfter(Instant.now().plus(CUSTOMER_CANCELLATION_NOTICE))) {
            throw ApiProblem.conflict("Appointments can be cancelled up to 24 hours before they start.");
        }
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.touch();
        return toDto(appointment);
    }

    public AppointmentDto reschedule(Long customerId, Long appointmentId, BookingCreate input) {
        Appointment appointment = appointments.lockByIdAndCustomerId(appointmentId, customerId)
                .orElseThrow(() -> ApiProblem.notFound("Appointment was not found."));
        if (appointment.getStatus() != AppointmentStatus.PENDING && appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw ApiProblem.conflict("This appointment can no longer be rescheduled.");
        }
        if (!appointment.getStartAt().toInstant().isAfter(Instant.now().plus(CUSTOMER_CANCELLATION_NOTICE))) {
            throw ApiProblem.conflict("Appointments can be rescheduled up to 24 hours before they start.");
        }
        BarberProfile profile = profiles.lockById(appointment.getBarberProfileId())
                .orElseThrow(() -> ApiProblem.notFound("Barber was not found."));
        BarberServiceOffering offering = activeOffering(profile.getId(), appointment.getBarberServiceId());
        OffsetDateTime start = validateAndNormalizeSlot(profile, offering, input.startAt(), appointment.getId());
        appointment.setStartAt(start);
        appointment.setEndAt(start.plusMinutes(offering.getDurationMinutes()));
        appointment.setDurationMinutes(offering.getDurationMinutes());
        appointment.setPriceAmount(offering.getPriceAmount());
        appointment.setServiceNameSnapshot(serviceName(offering));
        appointment.setCustomerNote(clean(input.customerNote()));
        appointment.setStatus(AppointmentStatus.PENDING);
        appointment.touch();
        return toDto(appointments.saveAndFlush(appointment));
    }

    public AppointmentDto barberTransition(Long userId, Long appointmentId, AppointmentStatus requested) {
        BarberProfile profile = requireProfileByUser(userId);
        Appointment appointment = appointments.lockByIdAndBarberProfileId(appointmentId, profile.getId())
                .orElseThrow(() -> ApiProblem.notFound("Appointment was not found."));
        AppointmentStatus current = appointment.getStatus();
        boolean allowed = (current == AppointmentStatus.PENDING && (requested == AppointmentStatus.CONFIRMED || requested == AppointmentStatus.REJECTED))
                || (current == AppointmentStatus.CONFIRMED && (requested == AppointmentStatus.COMPLETED || requested == AppointmentStatus.NO_SHOW));
        if (!allowed) throw ApiProblem.conflict("That appointment status transition is not allowed.");
        if (current == AppointmentStatus.PENDING && requested == AppointmentStatus.CONFIRMED
                && !appointment.getStartAt().toInstant().isAfter(Instant.now())) {
            throw ApiProblem.conflict("A booking request cannot be confirmed after its appointment has started.");
        }
        if ((requested == AppointmentStatus.COMPLETED || requested == AppointmentStatus.NO_SHOW)
                && appointment.getEndAt().toInstant().isAfter(Instant.now())) {
            throw ApiProblem.conflict("An appointment can only be completed or marked no-show after its scheduled end.");
        }
        appointment.setStatus(requested);
        appointment.touch();
        return toDto(appointment);
    }

    public AppointmentDto barberCancel(Long userId, Long appointmentId) {
        BarberProfile profile = requireProfileByUser(userId);
        Appointment appointment = appointments.lockByIdAndBarberProfileId(appointmentId, profile.getId())
                .orElseThrow(() -> ApiProblem.notFound("Appointment was not found."));
        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw ApiProblem.conflict("Only confirmed appointments can be cancelled by the barber. Decline pending requests instead.");
        }
        if (!appointment.getStartAt().toInstant().isAfter(Instant.now())) {
            throw ApiProblem.conflict("An appointment can only be cancelled before it starts.");
        }
        appointment.setStatus(AppointmentStatus.CANCELLED);
        appointment.touch();
        return toDto(appointment);
    }

    public AppointmentDto barberReschedule(Long userId, Long appointmentId, OffsetDateTime requestedStart) {
        BarberProfile profile = requireProfileByUser(userId);
        Appointment appointment = appointments.lockByIdAndBarberProfileId(appointmentId, profile.getId())
                .orElseThrow(() -> ApiProblem.notFound("Appointment was not found."));
        if (appointment.getStatus() != AppointmentStatus.CONFIRMED) {
            throw ApiProblem.conflict("Only confirmed appointments can be rescheduled by the barber.");
        }
        if (!appointment.getStartAt().toInstant().isAfter(Instant.now())) {
            throw ApiProblem.conflict("An appointment can only be rescheduled before it starts.");
        }
        profiles.lockById(profile.getId()).orElseThrow(() -> ApiProblem.notFound("Barber was not found."));
        BarberServiceOffering offering = activeOffering(profile.getId(), appointment.getBarberServiceId());
        OffsetDateTime start = validateAndNormalizeSlot(profile, offering, requestedStart, appointment.getId());
        appointment.setStartAt(start);
        appointment.setEndAt(start.plusMinutes(offering.getDurationMinutes()));
        appointment.setDurationMinutes(offering.getDurationMinutes());
        appointment.setPriceAmount(offering.getPriceAmount());
        appointment.setServiceNameSnapshot(serviceName(offering));
        appointment.touch();
        return toDto(appointments.saveAndFlush(appointment));
    }

    @Transactional(readOnly = true)
    public CustomerDashboardDto customerDashboard(Long customerId) {
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);
        List<AppointmentDto> all = appointments.findByCustomerIdOrderByStartAtDesc(customerId).stream().map(this::toDto).toList();
        List<AppointmentDto> upcoming = all.stream().filter(a -> a.startAt().isAfter(now)
                && a.status() == AppointmentStatus.CONFIRMED).toList();
        List<AppointmentDto> pending = all.stream().filter(a -> a.status() == AppointmentStatus.PENDING).toList();
        List<AppointmentDto> past = all.stream().filter(a -> a.startAt().isBefore(now)
                || List.of(AppointmentStatus.REJECTED, AppointmentStatus.CANCELLED, AppointmentStatus.COMPLETED, AppointmentStatus.NO_SHOW).contains(a.status())).toList();
        User user = users.findById(customerId).orElseThrow(() -> ApiProblem.notFound("User was not found."));
        return new CustomerDashboardDto(UserDto.from(user), "Client dashboard", upcoming, pending, past, favouriteService.list(customerId));
    }

    @Transactional(readOnly = true)
    public BarberDashboardDto barberDashboard(Long userId) {
        BarberProfile profile = requireProfileByUser(userId);
        ZoneId zone = ZoneId.of(profile.getTimezoneId());
        LocalDate today = LocalDate.now(zone);
        OffsetDateTime startToday = today.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime startTomorrow = today.plusDays(1).atStartOfDay(zone).toOffsetDateTime();
        List<Appointment> all = appointments.findByBarberProfileIdOrderByStartAtAsc(profile.getId());
        List<AppointmentDto> pending = all.stream().filter(a -> a.getStatus() == AppointmentStatus.PENDING).map(this::toDto).toList();
        List<AppointmentDto> todayAppointments = all.stream().filter(a -> !a.getStartAt().isBefore(startToday) && a.getStartAt().isBefore(startTomorrow)).map(this::toDto).toList();
        List<AppointmentDto> upcoming = all.stream().filter(a -> a.getStartAt().isAfter(OffsetDateTime.now(ZoneOffset.UTC))
                && (a.getStatus() == AppointmentStatus.PENDING || a.getStatus() == AppointmentStatus.CONFIRMED)).map(this::toDto).toList();
        BigDecimal revenue = all.stream().filter(a -> a.getStatus() == AppointmentStatus.COMPLETED)
                .map(Appointment::getPriceAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
        User user = users.findById(userId).orElseThrow(() -> ApiProblem.notFound("User was not found."));
        return new BarberDashboardDto(UserDto.from(user), "Barber dashboard", pending, todayAppointments, upcoming, new EarningsDto(revenue, "INR"));
    }

    private OffsetDateTime validateAndNormalizeSlot(BarberProfile profile, BarberServiceOffering offering,
                                                     OffsetDateTime requestedStart, Long excludeAppointmentId) {
        ZoneId zone = ZoneId.of(profile.getTimezoneId());
        OffsetDateTime start = utc(requestedStart);
        ZonedDateTime local = start.atZoneSameInstant(zone);
        validateBookingDate(local.toLocalDate(), zone);
        if (local.getMinute() % SLOT_INTERVAL_MINUTES != 0 || local.getSecond() != 0 || local.getNano() != 0) {
            throw ApiProblem.badRequest("Appointment times must start on a 15-minute boundary.");
        }
        WorkingHour working = hours.findByBarberProfileIdAndDayOfWeek(profile.getId(), local.getDayOfWeek().getValue())
                .filter(WorkingHour::isEnabled).orElseThrow(() -> ApiProblem.conflict("The barber is not working at that time."));
        LocalDateTime localEndDateTime = local.toLocalDateTime().plusMinutes(offering.getDurationMinutes());
        LocalTime endLocal = localEndDateTime.toLocalTime();
        if (local.toLocalTime().isBefore(working.getStartTime()) || localEndDateTime.toLocalDate().isAfter(local.toLocalDate())
                || endLocal.isAfter(working.getEndTime())) {
            throw ApiProblem.conflict("The requested appointment is outside working hours.");
        }
        boolean duringBreak = breaks.findByBarberProfileIdAndDayOfWeekOrderByStartTime(profile.getId(), local.getDayOfWeek().getValue()).stream()
                .anyMatch(b -> local.toLocalTime().isBefore(b.getEndTime()) && endLocal.isAfter(b.getStartTime()));
        if (duringBreak) throw ApiProblem.conflict("The requested appointment overlaps a barber break.");
        if (!start.toInstant().isAfter(Instant.now())) throw ApiProblem.conflict("The requested appointment is in the past.");
        OffsetDateTime end = start.plusMinutes(offering.getDurationMinutes());
        if (!appointments.findOverlapping(profile.getId(), start, end, BLOCKING_STATUSES, excludeAppointmentId).isEmpty()) {
            throw ApiProblem.conflict("That appointment time is no longer available.");
        }
        return start;
    }

    private void validateBookingDate(LocalDate date, ZoneId zone) {
        LocalDate today = LocalDate.now(zone);
        if (date.isBefore(today)) throw ApiProblem.badRequest("Availability is not offered for past dates.");
        if (date.isAfter(today.plusDays(MAX_BOOKING_DAYS_AHEAD))) throw ApiProblem.badRequest("Appointments can be booked up to 90 days ahead.");
    }

    private LocalDateTime nextSlotBoundary(LocalDateTime value) {
        int minuteOfDay = value.toLocalTime().toSecondOfDay() / 60;
        int roundedMinute = ((minuteOfDay + SLOT_INTERVAL_MINUTES - 1) / SLOT_INTERVAL_MINUTES) * SLOT_INTERVAL_MINUTES;
        return value.toLocalDate().atStartOfDay().plusMinutes(roundedMinute);
    }

    private BarberProfile requireProfile(Long id) {
        return profiles.findById(id).orElseThrow(() -> ApiProblem.notFound("Barber was not found."));
    }
    private BarberProfile requireProfileByUser(Long userId) {
        return profiles.findByUserId(userId).orElseThrow(() -> ApiProblem.notFound("Barber profile was not found."));
    }
    private BarberServiceOffering activeOffering(Long barberId, Long offeringId) {
        return offerings.findByIdAndBarberProfileId(offeringId, barberId).filter(BarberServiceOffering::isEnabled)
                .orElseThrow(() -> ApiProblem.notFound("Bookable service was not found."));
    }
    private String serviceName(BarberServiceOffering offering) {
        if (offering.getNameOverride() != null) return offering.getNameOverride();
        return catalog.findById(offering.getServiceId()).map(ServiceCatalogItem::getName)
                .orElseThrow(() -> ApiProblem.notFound("Service was not found."));
    }
    private OffsetDateTime utc(OffsetDateTime value) { return value.withOffsetSameInstant(ZoneOffset.UTC); }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    private AppointmentDto toDto(Appointment a) {
        User customer = users.findById(a.getCustomerId()).orElse(null);
        BarberProfile profile = profiles.findById(a.getBarberProfileId()).orElse(null);
        User barber = profile == null ? null : users.findById(profile.getUserId()).orElse(null);
        return new AppointmentDto(a.getId(), a.getBarberProfileId(), a.getCustomerId(), a.getBarberServiceId(),
                customer == null ? "Customer" : customer.getDisplayName(),
                barber == null ? "Barber" : barber.getDisplayName(), a.getServiceNameSnapshot(), a.getPriceAmount(),
                a.getDurationMinutes(), utc(a.getStartAt()), utc(a.getEndAt()),
                profile == null ? "UTC" : profile.getTimezoneId(), a.getStatus(), a.getCustomerNote(), reviews.existsByAppointmentId(a.getId()));
    }
}
