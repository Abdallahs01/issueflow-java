package com.att.tdp.issueflow.ticket;

import com.att.tdp.issueflow.audit.AuditLogService;
import com.att.tdp.issueflow.common.BadRequestException;
import com.att.tdp.issueflow.project.Project;
import com.att.tdp.issueflow.project.ProjectService;
import com.att.tdp.issueflow.ticket.dto.CreateTicketRequest;
import com.att.tdp.issueflow.ticket.dto.TicketImportResponse;
import com.att.tdp.issueflow.user.UserService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class TicketCsvService {

    private static final CSVFormat EXPORT_FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader("id", "title", "description", "status", "priority", "type", "projectId", "assigneeId", "dueDate")
            .build();

    private static final CSVFormat IMPORT_FORMAT = CSVFormat.DEFAULT.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreHeaderCase(true)
            .setTrim(true)
            .build();

    private final TicketRepository ticketRepository;
    private final TicketService ticketService;
    private final ProjectService projectService;
    private final UserService userService;
    private final AuditLogService auditLogService;

    public TicketCsvService(TicketRepository ticketRepository, TicketService ticketService, ProjectService projectService, UserService userService, AuditLogService auditLogService) {
        this.ticketRepository = ticketRepository;
        this.ticketService = ticketService;
        this.projectService = projectService;
        this.userService = userService;
        this.auditLogService = auditLogService;
    }

    @Transactional(readOnly = true)
    public byte[] exportTickets(Long projectId) {
        projectService.findActiveProjectById(projectId);
        List<Ticket> tickets = ticketRepository.findByProjectIdAndDeletedFalse(projectId);

        try (
                ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
                Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8);
                CSVPrinter printer = new CSVPrinter(writer, EXPORT_FORMAT)
        ) {
            for (Ticket ticket : tickets) {
                Long assigneeId = ticket.getAssignee() == null ? null : ticket.getAssignee().getId();
                printer.printRecord(
                        ticket.getId(),
                        ticket.getTitle(),
                        ticket.getDescription(),
                        ticket.getStatus(),
                        ticket.getPriority(),
                        ticket.getType(),
                        ticket.getProject().getId(),
                        assigneeId,
                        ticket.getDueDate()
                );
            }

            printer.flush();
            return outputStream.toByteArray();
        } catch (IOException exception) {
            throw new BadRequestException("Could not export tickets.");
        }
    }

    @Transactional
    public TicketImportResponse importTickets(Long projectId, MultipartFile file) {
        Project project = projectService.findActiveProjectById(projectId);
        validateCsvFile(file);

        int created = 0;
        List<String> errors = new ArrayList<>();

        try (
                Reader reader = new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8);
                CSVParser parser = IMPORT_FORMAT.parse(reader)
        ) {
            for (CSVRecord record : parser) {
                try {
                    CreateTicketRequest request = toCreateTicketRequest(project.getId(), record);
                    ticketService.createTicket(request);
                    created++;
                } catch (RuntimeException exception) {
                    errors.add("Row " + record.getRecordNumber() + ": " + exception.getMessage());
                }
            }
        } catch (IOException exception) {
            throw new BadRequestException("Could not read CSV file.");
        }

        auditLogService.recordCurrentUserAction("TICKET", project.getId(), "IMPORT", "Imported " + created + " tickets into project " + project.getId() + ".");
        return new TicketImportResponse(created, errors.size(), errors);
    }

    private CreateTicketRequest toCreateTicketRequest(Long projectId, CSVRecord record) {
        String title = required(record, "title");
        String description = required(record, "description");
        TicketStatus status = parseEnum(TicketStatus.class, required(record, "status"), "status");
        TicketPriority priority = parseEnum(TicketPriority.class, required(record, "priority"), "priority");
        TicketType type = parseEnum(TicketType.class, required(record, "type"), "type");
        Long assigneeId = optionalLong(record, "assigneeId");

        if (assigneeId != null) {
            userService.findUserById(assigneeId);
        }

        return new CreateTicketRequest(
                title,
                description,
                status,
                priority,
                type,
                projectId,
                assigneeId,
                optionalDate(record, "dueDate")
        );
    }

    private void validateCsvFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("CSV file is required.");
        }

        String fileName = file.getOriginalFilename();
        if (fileName != null && !fileName.toLowerCase().endsWith(".csv")) {
            throw new BadRequestException("Import file must be a CSV file.");
        }
    }

    private String required(CSVRecord record, String column) {
        String value = record.get(column);

        if (value == null || value.isBlank()) {
            throw new BadRequestException(column + " is required.");
        }

        return value;
    }

    private Long optionalLong(CSVRecord record, String column) {
        String value = record.get(column);

        try {
            return value == null || value.isBlank() ? null : Long.valueOf(value);
        } catch (NumberFormatException exception) {
            throw new BadRequestException(column + " must be a number.");
        }
    }

    private LocalDate optionalDate(CSVRecord record, String column) {
        String value = record.get(column);

        try {
            return value == null || value.isBlank() ? null : LocalDate.parse(value);
        } catch (RuntimeException exception) {
            throw new BadRequestException(column + " must use ISO date format yyyy-MM-dd.");
        }
    }

    private <E extends Enum<E>> E parseEnum(Class<E> enumType, String value, String column) {
        try {
            return Enum.valueOf(enumType, value);
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException(column + " has an invalid value.");
        }
    }
}
