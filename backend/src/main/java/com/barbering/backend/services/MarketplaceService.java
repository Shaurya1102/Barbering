package com.Barbering.backend.services;

import com.Barbering.backend.dto.MarketplaceDtos.*;
import com.Barbering.backend.dto.UserDto;
import com.Barbering.backend.entities.*;
import com.Barbering.backend.exception.ApiProblem;
import com.Barbering.backend.repositories.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DateTimeException;
import java.time.DayOfWeek;
import java.time.ZoneId;
import java.util.*;

@Service
@Transactional
public class MarketplaceService {
    private final UserRepository users;
    private final BarberProfileRepository profiles;
    private final ServiceCatalogRepository catalog;
    private final BarberServiceOfferingRepository offerings;
    private final WorkingHourRepository hours;
    private final BarberBreakRepository breaks;
    private final ReviewRepository reviews;
    private final BarberPhotoRepository photos;

    public MarketplaceService(UserRepository users, BarberProfileRepository profiles,
                              ServiceCatalogRepository catalog, BarberServiceOfferingRepository offerings,
                              WorkingHourRepository hours, BarberBreakRepository breaks,
                              ReviewRepository reviews, BarberPhotoRepository photos) {
        this.users = users;
        this.profiles = profiles;
        this.catalog = catalog;
        this.offerings = offerings;
        this.hours = hours;
        this.breaks = breaks;
        this.reviews = reviews;
        this.photos = photos;
    }

    public BarberProfile requireBarber(Long userId) {
        return profiles.findByUserId(userId)
                .orElseThrow(() -> ApiProblem.notFound("Barber profile was not found."));
    }

    public UserDto updateUserProfile(Long userId, UserProfileUpdate update) {
        User user = users.findById(userId).orElseThrow(() -> ApiProblem.notFound("User was not found."));
        user.setDisplayName(update.displayName().trim());
        user.touch();
        users.save(user);
        return UserDto.from(user);
    }

    @Transactional(readOnly = true)
    public UserDto userProfile(Long userId) {
        return UserDto.from(users.findById(userId).orElseThrow(() -> ApiProblem.notFound("User was not found.")));
    }

    @Transactional(readOnly = true)
    public BarberDto getPublicProfile(Long profileId) {
        BarberProfile profile = profiles.findById(profileId)
                .orElseThrow(() -> ApiProblem.notFound("Barber was not found."));
        return toBarberDto(profile);
    }

    @Transactional(readOnly = true)
    public BarberDto getOwnProfile(Long userId) {
        return toBarberDto(requireBarber(userId));
    }

    public BarberDto updateOwnProfile(Long userId, BarberProfileUpdate update) {
        BarberProfile profile = lockBarber(userId);
        if (update.timezoneId() != null && !update.timezoneId().isBlank()) {
            try {
                profile.setTimezoneId(ZoneId.of(update.timezoneId().trim()).getId());
            } catch (DateTimeException ex) {
                throw ApiProblem.badRequest("Timezone must be a valid IANA time-zone identifier.");
            }
        }
        profile.setBio(clean(update.bio()));
        profile.setCity(clean(update.city()));
        profile.setRegion(clean(update.region()));
        profile.setStreetAddress(clean(update.streetAddress()));
        profile.setPostalCode(clean(update.postalCode()));
        profile.setCountry(clean(update.country()));
        if (update.latitude() != null && update.longitude() != null) {
            if (Math.abs(update.latitude()) <= 90.0 && Math.abs(update.longitude()) <= 180.0) {
                profile.setLatitude(update.latitude());
                profile.setLongitude(update.longitude());
            }
        }
        profile.touch();
        profiles.save(profile);
        return toBarberDto(profile);
    }

    @Transactional(readOnly = true)
    public BarberSearchPage search(String query, String location, String service, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);
        Page<BarberProfile> result = profiles.search(normalizeFilter(query), normalizeFilter(location),
                normalizeFilter(service), PageRequest.of(safePage, safeSize, Sort.by("id").ascending()));
        List<BarberDto> items = result.getContent().stream().map(this::toBarberDto).toList();
        return new BarberSearchPage(items, result.getNumber(), result.getSize(), result.getTotalElements(), result.getTotalPages());
    }

    @Transactional(readOnly = true)
    public List<OfferingDto> publicOfferings(Long profileId) {
        requireProfile(profileId);
        return offerings.findByBarberProfileIdAndEnabledTrueOrderById(profileId).stream().map(this::toOfferingDto).toList();
    }

    @Transactional(readOnly = true)
    public List<OfferingDto> ownOfferings(Long userId) {
        BarberProfile profile = requireBarber(userId);
        return offerings.findByBarberProfileIdOrderById(profile.getId()).stream().map(this::toOfferingDto).toList();
    }

    public OfferingDto createOffering(Long userId, OfferingCreate input) {
        BarberProfile profile = lockBarber(userId);
        String name = input.name().trim();
        ServiceCatalogItem item = catalog.findByNameIgnoreCase(name)
                .orElseGet(() -> catalog.save(new ServiceCatalogItem(name, clean(input.description()))));
        if (offerings.existsByBarberProfileIdAndServiceId(profile.getId(), item.getId())) {
            throw ApiProblem.conflict("This service is already in your list.");
        }
        BarberServiceOffering offering = new BarberServiceOffering(
                profile.getId(), item.getId(), input.priceAmount(), input.durationMinutes());
        offering.setDescriptionOverride(input.description() == null ? "" : input.description().trim());
        return toOfferingDto(offerings.save(offering));
    }

    public OfferingDto updateOffering(Long userId, Long offeringId, OfferingUpdate input) {
        BarberProfile profile = lockBarber(userId);
        BarberServiceOffering offering = offerings.findByIdAndBarberProfileId(offeringId, profile.getId())
                .orElseThrow(() -> ApiProblem.notFound("Service offering was not found."));
        offering.setNameOverride(input.name().trim());
        offering.setDescriptionOverride(input.description() == null ? "" : input.description().trim());
        offering.setPriceAmount(input.priceAmount());
        offering.setDurationMinutes(input.durationMinutes());
        offering.setEnabled(input.enabled());
        offering.touch();
        return toOfferingDto(offerings.save(offering));
    }

    public void disableOffering(Long userId, Long offeringId) {
        BarberProfile profile = lockBarber(userId);
        BarberServiceOffering offering = offerings.findByIdAndBarberProfileId(offeringId, profile.getId())
                .orElseThrow(() -> ApiProblem.notFound("Service offering was not found."));
        offering.setEnabled(false);
        offering.touch();
        offerings.save(offering);
    }

    @Transactional(readOnly = true)
    public List<WorkingHourDto> ownWorkingHours(Long userId) {
        BarberProfile profile = requireBarber(userId);
        return hours.findByBarberProfileIdOrderByDayOfWeek(profile.getId()).stream().map(this::toHourDto).toList();
    }

    public List<WorkingHourDto> replaceWorkingHours(Long userId, List<WorkingHourInput> inputs) {
        BarberProfile profile = lockBarber(userId);
        Set<DayOfWeek> seen = new HashSet<>();
        for (WorkingHourInput input : inputs) {
            if (!seen.add(input.dayOfWeek())) throw ApiProblem.badRequest("Only one working-hours entry is allowed per day.");
            if (!input.startTime().isBefore(input.endTime())) throw ApiProblem.badRequest("Opening time must be before closing time.");
            List<BarberBreak> dayBreaks = breaks.findByBarberProfileIdAndDayOfWeekOrderByStartTime(profile.getId(), input.dayOfWeek().getValue());
            for (BarberBreak entry : dayBreaks) {
                if (!Boolean.TRUE.equals(input.enabled()) || entry.getStartTime().isBefore(input.startTime()) || entry.getEndTime().isAfter(input.endTime())) {
                    throw ApiProblem.conflict("Working hours cannot be changed while a recurring break is outside them.");
                }
            }
        }
        hours.deleteByBarberProfileId(profile.getId());
        List<WorkingHour> saved = hours.saveAll(inputs.stream().map(i -> new WorkingHour(
                profile.getId(), i.dayOfWeek().getValue(), i.startTime(), i.endTime(), i.enabled())).toList());
        return saved.stream().map(this::toHourDto).toList();
    }

    @Transactional(readOnly = true)
    public List<BreakDto> ownBreaks(Long userId) {
        BarberProfile profile = requireBarber(userId);
        return breaks.findByBarberProfileIdOrderByDayOfWeekAscStartTimeAsc(profile.getId()).stream().map(this::toBreakDto).toList();
    }

    public BreakDto createBreak(Long userId, BreakCreate input) {
        BarberProfile profile = lockBarber(userId);
        if (!input.startTime().isBefore(input.endTime())) throw ApiProblem.badRequest("Break start must be before break end.");
        int day = input.dayOfWeek().getValue();
        WorkingHour working = hours.findByBarberProfileIdAndDayOfWeek(profile.getId(), day)
                .filter(WorkingHour::isEnabled)
                .orElseThrow(() -> ApiProblem.conflict("Set enabled working hours for this day before adding a break."));
        if (input.startTime().isBefore(working.getStartTime()) || input.endTime().isAfter(working.getEndTime())) {
            throw ApiProblem.badRequest("Break must fit inside working hours.");
        }
        boolean overlaps = breaks.findByBarberProfileIdAndDayOfWeekOrderByStartTime(profile.getId(), day).stream()
                .anyMatch(existing -> input.startTime().isBefore(existing.getEndTime()) && input.endTime().isAfter(existing.getStartTime()));
        if (overlaps) throw ApiProblem.conflict("This break overlaps another recurring break.");
        return toBreakDto(breaks.save(new BarberBreak(profile.getId(), day, input.startTime(), input.endTime(), clean(input.reason()))));
    }

    public void deleteBreak(Long userId, Long breakId) {
        BarberProfile profile = lockBarber(userId);
        BarberBreak entry = breaks.findByIdAndBarberProfileId(breakId, profile.getId())
                .orElseThrow(() -> ApiProblem.notFound("Break was not found."));
        breaks.delete(entry);
    }

    @Transactional(readOnly = true)
    public List<BarberPhotoDto> photoDtos(Long profileId) {
        return photos.findByBarberProfileIdOrderByCreatedAtDesc(profileId).stream().map(p ->
                new BarberPhotoDto(p.getId(), "/barber-photos/" + p.getId() + "/content", p.getContentType(), p.getSizeBytes(), p.getAltText())).toList();
    }

    private BarberDto toBarberDto(BarberProfile profile) {
        User user = users.findById(profile.getUserId()).orElseThrow(() -> ApiProblem.notFound("Barber was not found."));
        Double average = reviews.averageRating(profile.getId());
        return new BarberDto(profile.getId(), user.getDisplayName(), profile.getBio(), profile.getCity(),
                profile.getRegion(), profile.getCountry(), profile.getStreetAddress(), profile.getPostalCode(),
                profile.getTimezoneId(), average == null ? 0.0 : average,
                reviews.countByBarberProfileId(profile.getId()), photoDtos(profile.getId()));
    }

    private OfferingDto toOfferingDto(BarberServiceOffering offering) {
        ServiceCatalogItem item = catalog.findById(offering.getServiceId())
                .orElseThrow(() -> ApiProblem.notFound("Service was not found."));
        return new OfferingDto(offering.getId(), item.getId(),
                offering.getNameOverride() == null ? item.getName() : offering.getNameOverride(),
                offering.getDescriptionOverride() == null ? item.getDescription() : offering.getDescriptionOverride(),
                offering.getPriceAmount(), offering.getDurationMinutes(), offering.isEnabled());
    }

    private WorkingHourDto toHourDto(WorkingHour hour) {
        return new WorkingHourDto(hour.getId(), DayOfWeek.of(hour.getDayOfWeek()), hour.getStartTime(), hour.getEndTime(), hour.isEnabled());
    }
    private BreakDto toBreakDto(BarberBreak entry) {
        return new BreakDto(entry.getId(), DayOfWeek.of(entry.getDayOfWeek()), entry.getStartTime(), entry.getEndTime(), entry.getReason());
    }
    private void requireProfile(Long id) {
        if (!profiles.existsById(id)) throw ApiProblem.notFound("Barber was not found.");
    }
    private BarberProfile lockBarber(Long userId) {
        return profiles.lockByUserId(userId)
                .orElseThrow(() -> ApiProblem.notFound("Barber profile was not found."));
    }
    private String clean(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String normalizeFilter(String value) { return value == null || value.isBlank() ? "" : value.trim().toLowerCase(Locale.ROOT); }
}
