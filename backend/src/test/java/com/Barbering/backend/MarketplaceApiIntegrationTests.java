package com.Barbering.backend;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.Barbering.backend.entities.Appointment;
import com.Barbering.backend.entities.AppointmentStatus;
import com.Barbering.backend.repositories.AppointmentRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class MarketplaceApiIntegrationTests {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @Autowired private AppointmentRepository appointments;

    private String suffix() { return UUID.randomUUID().toString().substring(0, 8); }

    private MockHttpSession signUp(String role, String name) throws Exception {
        MockHttpSession session = new MockHttpSession();
        String email = "market-" + suffix() + "@example.com";
        mockMvc.perform(post("/auth/signup").session(session).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"%s","email":"%s","password":"password123","role":"%s"}
                                """.formatted(name, email, role)))
                .andExpect(status().isCreated());
        return session;
    }

    private long profileId(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(get("/barber/me/profile").session(session))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    private long userId(MockHttpSession session) throws Exception {
        MvcResult result = mockMvc.perform(get("/auth/me").session(session))
                .andExpect(status().isOk()).andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asLong();
    }

    @Test
    void barberCanManageOwnedServiceDetailsAndDiscoveryUsesCurrentDetails() throws Exception {
        MockHttpSession barber = signUp("BARBER", "Service Owner");
        String unique = suffix();
        long profileId = profileId(barber);

        MvcResult created = mockMvc.perform(post("/barber/me/services").session(barber).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Classic Cut %s","description":"Original description","priceAmount":25.00,"durationMinutes":30}
                                """.formatted(unique)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.enabled").value(true))
                .andReturn();
        long offeringId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/barber/me/services/{id}", offeringId).session(barber).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Modern Fade %s","description":"Fresh description","priceAmount":31.50,"durationMinutes":45,"enabled":true}
                                """.formatted(unique)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Modern Fade " + unique))
                .andExpect(jsonPath("$.description").value("Fresh description"))
                .andExpect(jsonPath("$.priceAmount").value(31.50))
                .andExpect(jsonPath("$.durationMinutes").value(45));

        mockMvc.perform(put("/barber/me/profile").session(barber).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"city":"Riverbend %s","timezoneId":"Asia/Kolkata"}
                                """.formatted(unique)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/barbers").param("query", "modern fade " + unique))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(profileId));
        mockMvc.perform(get("/barbers").param("location", "riverbend " + unique))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(profileId));
        mockMvc.perform(get("/barbers/{id}/services", profileId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Modern Fade " + unique));

        mockMvc.perform(put("/barber/me/services/{id}", offeringId).session(barber).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Modern Fade %s","description":"Fresh description","priceAmount":31.50,"durationMinutes":45,"enabled":false}
                                """.formatted(unique)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(false));
        mockMvc.perform(get("/barbers/{id}/services", profileId)).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
        mockMvc.perform(get("/barber/me/services").session(barber))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].enabled").value(false));

        mockMvc.perform(put("/barber/me/services/{id}", offeringId).session(barber).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Modern Fade %s","description":"Fresh description","priceAmount":31.50,"durationMinutes":45,"enabled":true}
                                """.formatted(unique)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.enabled").value(true));
        mockMvc.perform(delete("/barber/me/services/{id}", offeringId).session(barber).with(csrf()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/barber/me/services").session(barber))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].enabled").value(false));
    }

    @Test
    void serviceAndProfileOperationsAreBoundToTheSignedInOwner() throws Exception {
        MockHttpSession firstBarber = signUp("BARBER", "First Barber");
        MockHttpSession secondBarber = signUp("BARBER", "Second Barber");
        long firstProfileId = profileId(firstBarber);
        MvcResult created = mockMvc.perform(post("/barber/me/services").session(firstBarber).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Owner-only service %s","description":"","priceAmount":0,"durationMinutes":15}
                                """.formatted(suffix())))
                .andExpect(status().isCreated()).andReturn();
        long offeringId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(put("/barber/me/services/{id}", offeringId).session(secondBarber).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Hijacked","description":"","priceAmount":1,"durationMinutes":15,"enabled":true}
                                """))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/barber/me/profile").session(secondBarber))
                .andExpect(status().isOk()).andExpect(jsonPath("$.id").value(profileId(secondBarber)))
                .andExpect(jsonPath("$.id").value(org.hamcrest.Matchers.not(firstProfileId)));
        MockHttpSession customer = signUp("CUSTOMER", "Profile Customer");
        mockMvc.perform(get("/barber/me/profile").session(customer)).andExpect(status().isForbidden());
    }

    @Test
    void customerCanViewAndEditOnlyTheirOwnDisplayProfile() throws Exception {
        MockHttpSession customer = signUp("CUSTOMER", "Before Edit");
        mockMvc.perform(get("/profile/me").session(customer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.displayName").value("Before Edit"));
        mockMvc.perform(put("/profile/me").session(customer).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"displayName\":\"After Edit\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.displayName").value("After Edit"));
        mockMvc.perform(get("/profile/me").session(customer))
                .andExpect(status().isOk()).andExpect(jsonPath("$.displayName").value("After Edit"));
    }

    @Test
    void quarterHourAvailabilityCanBeBookedAndBarberCanSafelyRescheduleOrCancelOwnedAppointments() throws Exception {
        MockHttpSession barber = signUp("BARBER", "Schedule Owner");
        long barberId = profileId(barber);
        MvcResult createdService = mockMvc.perform(post("/barber/me/services").session(barber).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Schedule Cut %s","description":"","priceAmount":30,"durationMinutes":30}
                                """.formatted(suffix())))
                .andExpect(status().isCreated()).andReturn();
        long serviceId = objectMapper.readTree(createdService.getResponse().getContentAsString()).get("id").asLong();

        java.time.LocalDate date = java.time.LocalDate.now(java.time.ZoneOffset.UTC).plusDays(3);
        String weekday = date.getDayOfWeek().name();
        mockMvc.perform(put("/barber/me/working-hours").session(barber).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"dayOfWeek\":\"%s\",\"startTime\":\"09:10:00\",\"endTime\":\"17:00:00\",\"enabled\":true}]".formatted(weekday)))
                .andExpect(status().isOk());

        MvcResult availability = mockMvc.perform(get("/barbers/{id}/availability", barberId)
                        .param("serviceId", Long.toString(serviceId)).param("date", date.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.slots[0].localStartTime").value("09:15:00"))
                .andReturn();
        String startAt = objectMapper.readTree(availability.getResponse().getContentAsString()).get("slots").get(0).get("startAt").asText();

        MockHttpSession customer = signUp("CUSTOMER", "Schedule Customer");
        MvcResult booking = mockMvc.perform(post("/appointments").session(customer).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"barberId":%d,"serviceId":%d,"startAt":"%s"}
                                """.formatted(barberId, serviceId, startAt)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING"))
                .andReturn();
        long appointmentId = objectMapper.readTree(booking.getResponse().getContentAsString()).get("id").asLong();

        mockMvc.perform(patch("/barber/me/appointments/{id}/status", appointmentId).session(barber).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CONFIRMED\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONFIRMED"));

        String replacement = java.time.OffsetDateTime.of(date, java.time.LocalTime.of(11, 0), java.time.ZoneOffset.UTC).toString();
        mockMvc.perform(patch("/barber/me/appointments/{id}/reschedule", appointmentId).session(barber).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"startAt\":\"%s\"}".formatted(replacement)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.startAt").value(org.hamcrest.Matchers.containsString("11:00:00")));

        mockMvc.perform(patch("/barber/me/appointments/{id}/cancel", appointmentId).session(barber).with(csrf()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        mockMvc.perform(get("/appointments/mine").session(customer))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("CANCELLED"));

        MockHttpSession otherBarber = signUp("BARBER", "Other Barber");
        mockMvc.perform(patch("/barber/me/appointments/{id}/cancel", appointmentId).session(otherBarber).with(csrf()))
                .andExpect(status().isNotFound());
        mockMvc.perform(patch("/barber/me/appointments/{id}/cancel", appointmentId).session(customer).with(csrf()))
                .andExpect(status().isForbidden());
    }

    @Test
    void malformedSchedulesAndOverlongPhotoDescriptionsReturnValidationErrors() throws Exception {
        MockHttpSession barber = signUp("BARBER", "Validation Barber");
        mockMvc.perform(put("/barber/me/working-hours").session(barber).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("[null]"))
                .andExpect(status().isBadRequest());

        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png",
                new byte[]{(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10});
        MockMultipartFile altText = new MockMultipartFile("altText", "", "text/plain", "x".repeat(161).getBytes());
        mockMvc.perform(multipart("/barber/me/photos").file(file).file(altText).session(barber).with(csrf()))
                .andExpect(status().isBadRequest());
    }

    @Test
    void favouritesAreIdempotentAndReviewsRequireAnOwnedCompletedAppointment() throws Exception {
        MockHttpSession barber = signUp("BARBER", "Review Barber");
        long barberId = profileId(barber);
        MockHttpSession customer = signUp("CUSTOMER", "Review Customer");
        mockMvc.perform(put("/favourites/{id}", barberId).session(customer).with(csrf()))
                .andExpect(status().isNoContent());
        mockMvc.perform(put("/favourites/{id}", barberId).session(customer).with(csrf()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/favourites").session(customer))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isArray()).andExpect(jsonPath("$.length()").value(1));
        mockMvc.perform(get("/favourites")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/favourites/{id}", barberId).session(customer).with(csrf()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/favourites").session(customer)).andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());

        MvcResult createdService = mockMvc.perform(post("/barber/me/services").session(barber).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Review Cut %s","description":"","priceAmount":20,"durationMinutes":30}
                                """.formatted(suffix())))
                .andExpect(status().isCreated()).andReturn();
        long serviceId = objectMapper.readTree(createdService.getResponse().getContentAsString()).get("id").asLong();
        long customerId = userId(customer);
        Appointment pending = historicalAppointment(customerId, barberId, serviceId, AppointmentStatus.PENDING);
        Appointment completed = historicalAppointment(customerId, barberId, serviceId, AppointmentStatus.COMPLETED);

        mockMvc.perform(post("/appointments/{id}/reviews", pending.getId()).session(customer).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":5,\"reviewText\":\"Good visit\"}"))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/appointments/{id}/reviews", completed.getId()).session(customer).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":5,\"reviewText\":\"Good visit\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.rating").value(5));
        mockMvc.perform(post("/appointments/{id}/reviews", completed.getId()).session(customer).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":4}"))
                .andExpect(status().isConflict());
        MockHttpSession otherCustomer = signUp("CUSTOMER", "Other Customer");
        mockMvc.perform(post("/appointments/{id}/reviews", completed.getId()).session(otherCustomer).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"rating\":4}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(get("/barbers/{id}/reviews", barberId))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isArray()).andExpect(jsonPath("$.length()").value(1));
    }

    private Appointment historicalAppointment(long customerId, long barberId, long serviceId, AppointmentStatus status) {
        java.time.OffsetDateTime start = java.time.OffsetDateTime.now(java.time.ZoneOffset.UTC).minusDays(2).withNano(0);
        Appointment appointment = new Appointment();
        appointment.setCustomerId(customerId);
        appointment.setBarberProfileId(barberId);
        appointment.setBarberServiceId(serviceId);
        appointment.setServiceNameSnapshot("Review Cut");
        appointment.setPriceAmount(new java.math.BigDecimal("20.00"));
        appointment.setDurationMinutes(30);
        appointment.setStartAt(start);
        appointment.setEndAt(start.plusMinutes(30));
        appointment.setStatus(status);
        return appointments.saveAndFlush(appointment);
    }
}
