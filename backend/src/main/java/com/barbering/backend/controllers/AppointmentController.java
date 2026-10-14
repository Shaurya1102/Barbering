package com.Barbering.backend.controllers;

import com.Barbering.backend.dto.MarketplaceDtos.*;
import com.Barbering.backend.security.UserPrincipal;
import com.Barbering.backend.services.AppointmentService;
import com.Barbering.backend.services.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/appointments")
@PreAuthorize("hasRole('CUSTOMER')")
public class AppointmentController {
    private final AppointmentService appointments;
    private final ReviewService reviews;

    public AppointmentController(AppointmentService appointments, ReviewService reviews) {
        this.appointments = appointments;
        this.reviews = reviews;
    }

    @GetMapping("/mine")
    public List<AppointmentDto> mine(@AuthenticationPrincipal UserPrincipal user) {
        return appointments.customerAppointments(user.id());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppointmentDto book(@AuthenticationPrincipal UserPrincipal user, @Valid @RequestBody BookingSubmission input) {
        return appointments.create(user.id(), input.barberId(), input.serviceId(),
                new BookingCreate(input.startAt(), input.customerNote()));
    }

    @PatchMapping("/{appointmentId}/cancel")
    public AppointmentDto cancel(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long appointmentId) {
        return appointments.cancel(user.id(), appointmentId);
    }

    @PatchMapping("/{appointmentId}/reschedule")
    public AppointmentDto reschedule(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long appointmentId,
                                      @Valid @RequestBody BookingCreate input) {
        return appointments.reschedule(user.id(), appointmentId, input);
    }

    @PostMapping("/{appointmentId}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    public ReviewDto review(@AuthenticationPrincipal UserPrincipal user, @PathVariable Long appointmentId,
                            @Valid @RequestBody ReviewCreate input) {
        return reviews.create(user.id(), appointmentId, input);
    }
}
