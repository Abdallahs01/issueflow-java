package com.att.tdp.issueflow.ticket;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

    List<Ticket> findByProjectIdAndDeletedFalse(Long projectId);

    List<Ticket> findByProjectIdAndDeletedTrue(Long projectId);

    Optional<Ticket> findByIdAndDeletedFalse(Long ticketId);

    List<Ticket> findByDeletedFalseAndStatusNotAndDueDateBefore(TicketStatus status, LocalDate dueDate);

    long countByProjectIdAndAssigneeIdAndDeletedFalseAndStatusNot(Long projectId, Long assigneeId, TicketStatus status);
}
