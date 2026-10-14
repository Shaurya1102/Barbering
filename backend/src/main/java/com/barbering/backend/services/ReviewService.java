package com.Barbering.backend.services;

import com.Barbering.backend.dto.MarketplaceDtos.ReviewCreate;
import com.Barbering.backend.dto.MarketplaceDtos.ReviewDto;
import com.Barbering.backend.entities.Appointment;
import com.Barbering.backend.entities.AppointmentStatus;
import com.Barbering.backend.entities.Review;
import com.Barbering.backend.entities.User;
import com.Barbering.backend.exception.ApiProblem;
import com.Barbering.backend.repositories.AppointmentRepository;
import com.Barbering.backend.repositories.ReviewRepository;
import com.Barbering.backend.repositories.BarberProfileRepository;
import com.Barbering.backend.repositories.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.util.List;

@Service
@Transactional
public class ReviewService {
    private final ReviewRepository reviews;
    private final AppointmentRepository appointments;
    private final UserRepository users;
    private final BarberProfileRepository profiles;

    public ReviewService(ReviewRepository reviews, AppointmentRepository appointments, UserRepository users,
                        BarberProfileRepository profiles) {
        this.reviews = reviews;
        this.appointments = appointments;
        this.users = users;
        this.profiles = profiles;
    }

    @Transactional(readOnly = true)
    public List<ReviewDto> listForBarber(Long barberId) {
        if (!profiles.existsById(barberId)) throw ApiProblem.notFound("Barber was not found.");
        return reviews.findByBarberProfileIdOrderByCreatedAtDesc(barberId).stream().map(this::toDto).toList();
    }

    public ReviewDto create(Long customerId, Long appointmentId, ReviewCreate input) {
        Appointment appointment = appointments.lockByIdAndCustomerId(appointmentId, customerId)
                .orElseThrow(() -> ApiProblem.notFound("Appointment was not found."));
        if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
            throw ApiProblem.conflict("Only completed appointments can be reviewed.");
        }
        if (reviews.existsByAppointmentId(appointmentId)) {
            throw ApiProblem.conflict("This appointment already has a review.");
        }
        Review review = reviews.save(new Review(appointmentId, customerId,
                appointment.getBarberProfileId(), input.rating(), input.reviewText()));
        return toDto(review);
    }

    private ReviewDto toDto(Review review) {
        User customer = users.findById(review.getCustomerId())
                .orElseThrow(() -> ApiProblem.notFound("Customer was not found."));
        return new ReviewDto(review.getId(), review.getAppointmentId(), customer.getDisplayName(), review.getRating(),
                review.getReviewText(), review.getCreatedAt().atOffset(ZoneOffset.UTC));
    }
}
