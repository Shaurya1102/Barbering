package com.Barbering.backend.controllers;

import com.Barbering.backend.dto.MarketplaceDtos.BarberDashboardDto;
import com.Barbering.backend.dto.MarketplaceDtos.CustomerDashboardDto;
import com.Barbering.backend.security.UserPrincipal;
import com.Barbering.backend.services.AppointmentService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


/**
 * Minimal role-protected dashboard endpoints. The client dashboard is open to
 * any authenticated user; the barber dashboard requires ROLE_BARBER and is
 * enforced server-side (403 otherwise) — frontend visibility alone is not
 * authorization.
 */
@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    private final AppointmentService appointments;

    public DashboardController(AppointmentService appointments) { this.appointments = appointments; }

    @GetMapping("/client")
    @PreAuthorize("hasRole('CUSTOMER')")
    public CustomerDashboardDto clientDashboard(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return appointments.customerDashboard(principal.id());
    }

    @GetMapping("/barber")
    @PreAuthorize("hasRole('BARBER')")
    public BarberDashboardDto barberDashboard(Authentication authentication) {
        UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();
        return appointments.barberDashboard(principal.id());
    }
}
