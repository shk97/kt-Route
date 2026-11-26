package com.ktroute.app.entity;

import com.ktroute.app.enums.ChangeType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "schedule_change_history")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ScheduleChangeHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "trip_id", nullable = false)
    private Trip trip;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by_user_id", nullable = false)
    private User changedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ChangeType changeType;

    @Column(length = 1000)
    private String changeDescription;

    @Column(length = 500)
    private String reason;

    @CreationTimestamp
    @Column(updatable = false)
    private LocalDateTime changedAt;
}
