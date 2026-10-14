package com.Barbering.backend.controllers;

import com.Barbering.backend.dto.MarketplaceDtos.*;
import com.Barbering.backend.security.UserPrincipal;
import com.Barbering.backend.services.AppointmentService;
import com.Barbering.backend.services.MarketplaceService;
import com.Barbering.backend.services.ReviewService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
public class MarketplaceController {
    private final MarketplaceService marketplace;
    private final AppointmentService appointments;
    private final ReviewService reviews;

    public MarketplaceController(MarketplaceService marketplace, AppointmentService appointments, ReviewService reviews) {
        this.marketplace = marketplace;
        this.appointments = appointments;
        this.reviews = reviews;
    }

    @GetMapping("/barbers")
    public BarberSearchPage search(@RequestParam(required = false) String query,
                                   @RequestParam(required = false) String location,
                                   @RequestParam(required = false) String service,
                                   @RequestParam(defaultValue = "0") int page,
                                   @RequestParam(defaultValue = "12") int size) {
        return marketplace.search(query, location, service, page, size);
    }

    @GetMapping("/barbers/{barberId}")
    public BarberDto profile(@PathVariable Long barberId) { return marketplace.getPublicProfile(barberId); }

    @GetMapping("/barbers/{barberId}/services")
    public List<OfferingDto> publicServices(@PathVariable Long barberId) { return marketplace.publicOfferings(barberId); }

    @GetMapping("/barbers/{barberId}/reviews")
    public List<ReviewDto> publicReviews(@PathVariable Long barberId) { return reviews.listForBarber(barberId); }

    @GetMapping("/barbers/{barberId}/availability")
    public AvailabilityDto availability(@PathVariable Long barberId, @RequestParam Long serviceId,
                                        @RequestParam LocalDate date) {
        return appointments.availability(barberId, serviceId, date);
    }

    @GetMapping("/profile/me")
    public com.Barbering.backend.dto.UserDto myProfile(@AuthenticationPrincipal UserPrincipal user) {
        return marketplace.userProfile(user.id());
    }

    @PutMapping("/profile/me")
    public com.Barbering.backend.dto.UserDto updateMyProfile(@AuthenticationPrincipal UserPrincipal user,
                                                              @Valid @RequestBody UserProfileUpdate update) {
        return marketplace.updateUserProfile(user.id(), update);
    }

    @GetMapping("/barber/me/profile")
    @PreAuthorize("hasRole('BARBER')")
    public BarberDto myBarberProfile(@AuthenticationPrincipal UserPrincipal user) {
        return marketplace.getOwnProfile(user.id());
    }

    @PutMapping("/barber/me/profile")
    @PreAuthorize("hasRole('BARBER')")
    public BarberDto updateBarberProfile(@AuthenticationPrincipal UserPrincipal user,
                                         @Valid @RequestBody BarberProfileUpdate update) {
        return marketplace.updateOwnProfile(user.id(), update);
    }

    @GetMapping("/barber/me/services")
    @PreAuthorize("hasRole('BARBER')")
    public List<OfferingDto> myServices(@AuthenticationPrincipal UserPrincipal user) { return marketplace.ownOfferings(user.id()); }

    @PostMapping("/barber/me/services")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('BARBER')")
    public OfferingDto addService(@AuthenticationPrincipal UserPrincipal user, @Valid @RequestBody OfferingCreate input) {
        return marketplace.createOffering(user.id(), input);
    }

    @PutMapping("/barber/me/services/{offeringId}")
    @PreAuthorize("hasRole('BARBER')")
    public OfferingDto updateService(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long offeringId,
                                     @Valid @RequestBody OfferingUpdate input) {
        return marketplace.updateOffering(user.id(), offeringId, input);
    }

    @DeleteMapping("/barber/me/services/{offeringId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('BARBER')")
    public void removeService(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long offeringId) {
        marketplace.disableOffering(user.id(), offeringId);
    }

    @GetMapping("/barber/me/working-hours")
    @PreAuthorize("hasRole('BARBER')")
    public List<WorkingHourDto> myWorkingHours(@AuthenticationPrincipal UserPrincipal user) {
        return marketplace.ownWorkingHours(user.id());
    }

    @PutMapping("/barber/me/working-hours")
    @PreAuthorize("hasRole('BARBER')")
    public List<WorkingHourDto> updateWorkingHours(@AuthenticationPrincipal UserPrincipal user,
                                                   @Valid @RequestBody List<@NotNull @Valid WorkingHourInput> inputs) {
        if (inputs == null) throw com.Barbering.backend.exception.ApiProblem.badRequest("Working hours must be a list.");
        return marketplace.replaceWorkingHours(user.id(), inputs);
    }

    @GetMapping("/barber/me/breaks")
    @PreAuthorize("hasRole('BARBER')")
    public List<BreakDto> myBreaks(@AuthenticationPrincipal UserPrincipal user) { return marketplace.ownBreaks(user.id()); }

    @PostMapping("/barber/me/breaks")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('BARBER')")
    public BreakDto addBreak(@AuthenticationPrincipal UserPrincipal user, @Valid @RequestBody BreakCreate input) {
        return marketplace.createBreak(user.id(), input);
    }

    @DeleteMapping("/barber/me/breaks/{breakId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('BARBER')")
    public void removeBreak(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long breakId) {
        marketplace.deleteBreak(user.id(), breakId);
    }

    @GetMapping("/barber/me/appointments")
    @PreAuthorize("hasRole('BARBER')")
    public List<AppointmentDto> barberAppointments(@AuthenticationPrincipal UserPrincipal user) {
        return appointments.barberAppointments(user.id());
    }

    @PatchMapping("/barber/me/appointments/{appointmentId}/status")
    @PreAuthorize("hasRole('BARBER')")
    public AppointmentDto transitionAppointment(@AuthenticationPrincipal UserPrincipal user,
                                                @PathVariable Long appointmentId,
                                                @Valid @RequestBody AppointmentStatusUpdate update) {
        return appointments.barberTransition(user.id(), appointmentId, update.status());
    }

    @PatchMapping("/barber/me/appointments/{appointmentId}/cancel")
    @PreAuthorize("hasRole('BARBER')")
    public AppointmentDto cancelBarberAppointment(@AuthenticationPrincipal UserPrincipal user,
                                                  @PathVariable Long appointmentId) {
        return appointments.barberCancel(user.id(), appointmentId);
    }

    @PatchMapping("/barber/me/appointments/{appointmentId}/reschedule")
    @PreAuthorize("hasRole('BARBER')")
    public AppointmentDto rescheduleBarberAppointment(@AuthenticationPrincipal UserPrincipal user,
                                                      @PathVariable Long appointmentId,
                                                      @Valid @RequestBody BarberReschedule input) {
        return appointments.barberReschedule(user.id(), appointmentId, input.startAt());
    }
}
