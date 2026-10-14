package com.Barbering.backend.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalTime;

@Entity
@Table(name = "working_hours")
@Getter @Setter @NoArgsConstructor
public class WorkingHour {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "barber_profile_id", nullable = false)
    private Long barberProfileId;
    @Column(name = "day_of_week", nullable = false)
    private int dayOfWeek;
    @Column(name = "start_time", nullable = false)
    private LocalTime startTime;
    @Column(name = "end_time", nullable = false)
    private LocalTime endTime;
    @Column(nullable = false)
    private boolean enabled = true;

    public WorkingHour(Long barberProfileId, int dayOfWeek, LocalTime startTime, LocalTime endTime, boolean enabled) {
        this.barberProfileId = barberProfileId;
        this.dayOfWeek = dayOfWeek;
        this.startTime = startTime;
        this.endTime = endTime;
        this.enabled = enabled;
    }
}
