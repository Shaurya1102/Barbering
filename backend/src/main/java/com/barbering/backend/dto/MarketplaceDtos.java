package com.Barbering.backend.dto;

import com.Barbering.backend.entities.AppointmentStatus;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.List;

/** API request and response contracts for the barber marketplace. */
public final class MarketplaceDtos {
    private MarketplaceDtos() {}

    public record UserProfileUpdate(
            @NotBlank @Size(max = 100) String displayName) {}

    public record BarberProfileUpdate(
            @Size(max = 2000) String bio,
            @Size(max = 120) String city,
            @Size(max = 120) String region,
            @Size(max = 255) String streetAddress,
            @Size(max = 32) String postalCode,
            @Size(max = 120) String country,
            @Size(max = 80) String timezoneId,
            Double latitude,
            Double longitude) {
        public BarberProfileUpdate(String bio, String city, String region, String streetAddress, String postalCode, String country, String timezoneId) {
            this(bio, city, region, streetAddress, postalCode, country, timezoneId, null, null);
        }
    }

    public record OfferingCreate(
            @NotBlank @Size(max = 100) String name,
            @Size(max = 1000) String description,
            @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal priceAmount,
            @NotNull @Min(5) @Max(480) Integer durationMinutes) {}

    public record OfferingUpdate(
            @NotBlank @Size(max = 100) String name,
            @Size(max = 1000) String description,
            @NotNull @DecimalMin("0.00") @Digits(integer = 8, fraction = 2) BigDecimal priceAmount,
            @NotNull @Min(5) @Max(480) Integer durationMinutes,
            @NotNull Boolean enabled) {}

    public record OfferingDto(Long id, Long serviceId, String name, String description,
                              BigDecimal priceAmount, int durationMinutes, boolean enabled) {}

    public record WorkingHourInput(@NotNull DayOfWeek dayOfWeek, @NotNull LocalTime startTime,
                                   @NotNull LocalTime endTime, @NotNull Boolean enabled) {}
    public record WorkingHourDto(Long id, DayOfWeek dayOfWeek, LocalTime startTime,
                                 LocalTime endTime, boolean enabled) {}

    public record BreakCreate(@NotNull DayOfWeek dayOfWeek, @NotNull LocalTime startTime,
                              @NotNull LocalTime endTime, @Size(max = 120) String reason) {}
    public record BreakDto(Long id, DayOfWeek dayOfWeek, LocalTime startTime,
                           LocalTime endTime, String reason) {}

    public record BarberPhotoDto(Long id, String url, String contentType, long sizeBytes, String altText) {}

    public record BarberDto(Long id, String displayName, String bio, String city, String region,
                            String country, String streetAddress, String postalCode, String timezoneId,
                            double averageRating, long reviewCount, List<BarberPhotoDto> photos) {}

    public record BarberSearchPage(List<BarberDto> items, int page, int size, long totalItems, int totalPages) {}

    public record AvailabilityDto(LocalDate date, String timezoneId, List<AvailabilitySlot> slots) {}
    public record AvailabilitySlot(OffsetDateTime startAt, OffsetDateTime endAt,
                                   LocalTime localStartTime, LocalTime localEndTime) {}

    public record BookingCreate(@NotNull OffsetDateTime startAt, @Size(max = 1000) String customerNote) {}
    public record BarberReschedule(@NotNull OffsetDateTime startAt) {}
    public record BookingSubmission(@NotNull Long barberId, @NotNull Long serviceId,
                                    @NotNull OffsetDateTime startAt, @Size(max = 1000) String customerNote) {}
    public record AppointmentDto(Long id, Long barberId, Long customerId, Long serviceId, String customerName,
                                 String barberName, String serviceName, BigDecimal priceAmount,
                                 int durationMinutes, OffsetDateTime startAt, OffsetDateTime endAt,
                                 String timezoneId, AppointmentStatus status, String customerNote, boolean reviewed) {}
    public record AppointmentStatusUpdate(@NotNull AppointmentStatus status) {}
    public record EarningsDto(BigDecimal completedRevenue, String currency) {}
    public record CustomerDashboardDto(UserDto user, String message, List<AppointmentDto> upcoming, List<AppointmentDto> pending,
                                       List<AppointmentDto> past, List<FavouriteDto> favourites) {}
    public record BarberDashboardDto(UserDto user, String message, List<AppointmentDto> pending, List<AppointmentDto> today,
                                     List<AppointmentDto> upcoming, EarningsDto earnings) {}

    public record ReviewCreate(@NotNull @Min(1) @Max(5) Integer rating,
                               @Size(max = 2000) String reviewText) {}
    public record ReviewDto(Long id, Long appointmentId, String customerName,
                            int rating, String reviewText, OffsetDateTime createdAt) {}

    public record FavouriteDto(Long barberId, String barberName, String city,
                               double averageRating, long reviewCount) {}

    public record RecommendationRequest(@NotBlank @Size(max = 80) String hairTexture,
                                        @NotBlank @Size(max = 80) String hairLength,
                                        @NotBlank @Size(max = 500) String preferences,
                                        @NotBlank @Size(max = 80) String maintenanceTolerance,
                                        @Size(max = 120) String context) {}
    public record RecommendationItem(String name, String explanation, String maintenance, List<Long> matchingBarberIds) {}
    public record RecommendationResponse(List<RecommendationItem> recommendations) {}
    public record TryOnRequest(@NotBlank @Size(max = 36) String imageId,
                               @NotBlank @Size(max = 40) String category,
                               @NotBlank @Size(max = 500) String desiredStyle) {}
    public record TryOnResponse(String status, String message, String resultUrl) {}
}
