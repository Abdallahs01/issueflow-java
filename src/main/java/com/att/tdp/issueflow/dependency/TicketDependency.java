package com.att.tdp.issueflow.dependency;

import com.att.tdp.issueflow.ticket.Ticket;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Getter
@NoArgsConstructor
@Entity
@Table(
        name = "ticket_dependencies",
        uniqueConstraints = @UniqueConstraint(name = "uk_ticket_dependency_pair", columnNames = {"ticket_id", "blocker_id"})
)
public class TicketDependency {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticket;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "blocker_id", nullable = false)
    private Ticket blocker;

    private Instant createdAt;

    public TicketDependency(Ticket ticket, Ticket blocker) {
        this.ticket = ticket;
        this.blocker = blocker;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
