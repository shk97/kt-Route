package com.ktroute.app.entity;

import com.ktroute.app.enums.BookingStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

// orphanRemoval ensures removed passengers are deleted from DB, not just disassociated

@Entity
@Table(name = "bookings")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String bookingRef;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @Column(nullable = false)
    private Integer passengerCount;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private BookingStatus status = BookingStatus.PENDING;

    @Column(length = 500)
    private String cancellationReason;

    private LocalDateTime cancelledAt;

    // Origin and destination for this particular booking (may differ from route endpoints)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "boarding_station_id")
    private Station boardingStation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alighting_station_id")
    private Station alightingStation;

    @OneToMany(mappedBy = "booking", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<BookingPassenger> passengers;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    private void generateRef() {
        if (bookingRef == null) {
            bookingRef = "BK" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        }
    }
}
