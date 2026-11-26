package com.ktroute.app.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "boarding_records")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BoardingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boarding_station_id", nullable = false)
    private Station boardingStation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alighting_station_id", nullable = false)
    private Station alightingStation;

    private LocalDateTime actualBoardingTime;

    private LocalDateTime actualAlightingTime;

    @Column(nullable = false)
    @Builder.Default
    private Integer passengerCount = 1;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime recordedAt;
}
