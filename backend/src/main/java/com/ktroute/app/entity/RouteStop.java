package com.ktroute.app.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "route_stops",
    uniqueConstraints = @UniqueConstraint(columnNames = {"route_id", "stop_order"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RouteStop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "route_id", nullable = false)
    private Route route;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "station_id", nullable = false)
    private Station station;

    @Column(nullable = false)
    private Integer stopOrder;

    // Minutes from departure of first stop
    @Column(nullable = false)
    @Builder.Default
    private Integer arrivalOffsetMinutes = 0;

    @Column(nullable = false)
    @Builder.Default
    private Integer departureOffsetMinutes = 0;

    // Distance from the start station in km
    private Double distanceFromStartKm;
}
