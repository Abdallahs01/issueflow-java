package com.att.tdp.issueflow.ticket;

import com.att.tdp.issueflow.ticket.dto.CreateTicketRequest;
import com.att.tdp.issueflow.ticket.dto.TicketImportResponse;
import com.att.tdp.issueflow.ticket.dto.TicketResponse;
import com.att.tdp.issueflow.ticket.dto.UpdateTicketRequest;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/tickets")
public class TicketController {

    private final TicketService ticketService;
    private final TicketCsvService ticketCsvService;

    public TicketController(TicketService ticketService, TicketCsvService ticketCsvService) {
        this.ticketService = ticketService;
        this.ticketCsvService = ticketCsvService;
    }

    @GetMapping("/{ticketId}")
    public TicketResponse getTicketById(@PathVariable Long ticketId) {
        return ticketService.getTicketById(ticketId);
    }

    @GetMapping
    public List<TicketResponse> getTicketsByProject(@RequestParam Long projectId) {
        return ticketService.getTicketsByProject(projectId);
    }

    @GetMapping("/deleted")
    @PreAuthorize("hasRole('ADMIN')")
    public List<TicketResponse> getDeletedTicketsByProject(@RequestParam Long projectId) {
        return ticketService.getDeletedTicketsByProject(projectId);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportTickets(@RequestParam Long projectId) {
        byte[] csv = ticketCsvService.exportTickets(projectId);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("text/csv"))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment().filename("tickets.csv").build().toString())
                .body(csv);
    }

    @PostMapping("/import")
    public TicketImportResponse importTickets(@RequestParam Long projectId, @RequestParam("file") MultipartFile file) {
        return ticketCsvService.importTickets(projectId, file);
    }

    @PostMapping
    public TicketResponse createTicket(@Valid @RequestBody CreateTicketRequest request) {
        return ticketService.createTicket(request);
    }

    @PostMapping("/update/{ticketId}")
    public void updateTicket(@PathVariable Long ticketId, @Valid @RequestBody UpdateTicketRequest request) {
        ticketService.updateTicket(ticketId, request);
    }

    @PatchMapping("/{ticketId}")
    public void patchTicket(@PathVariable Long ticketId, @Valid @RequestBody UpdateTicketRequest request) {
        ticketService.updateTicket(ticketId, request);
    }

    @DeleteMapping("/{ticketId}")
    @PreAuthorize("hasRole('ADMIN')")
    public void deleteTicket(@PathVariable Long ticketId) {
        ticketService.deleteTicket(ticketId);
    }

    @PostMapping("/{ticketId}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public void restoreTicket(@PathVariable Long ticketId) {
        ticketService.restoreTicket(ticketId);
    }
}
