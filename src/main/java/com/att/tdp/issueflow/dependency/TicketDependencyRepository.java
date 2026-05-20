package com.att.tdp.issueflow.dependency;

import com.att.tdp.issueflow.ticket.TicketStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TicketDependencyRepository extends JpaRepository<TicketDependency, Long> {

    boolean existsByTicketIdAndBlockerId(Long ticketId, Long blockerId);

    boolean existsByTicketIdAndBlockerStatusNot(Long ticketId, TicketStatus status);

    List<TicketDependency> findByTicketId(Long ticketId);

    Optional<TicketDependency> findByTicketIdAndBlockerId(Long ticketId, Long blockerId);
}
