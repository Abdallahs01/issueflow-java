package com.att.tdp.issueflow;
import com.att.tdp.issueflow.auth.AuthService;
import com.att.tdp.issueflow.auth.dto.LoginRequest;
import com.att.tdp.issueflow.auth.dto.LoginResponse;
import com.att.tdp.issueflow.comment.CommentService;
import com.att.tdp.issueflow.comment.dto.CommentResponse;
import com.att.tdp.issueflow.comment.dto.CreateCommentRequest;
import com.att.tdp.issueflow.comment.dto.UpdateCommentRequest;
import com.att.tdp.issueflow.common.BadRequestException;
import com.att.tdp.issueflow.common.ResourceNotFoundException;
import com.att.tdp.issueflow.dependency.TicketDependencyService;
import com.att.tdp.issueflow.dependency.dto.CreateTicketDependencyRequest;
import com.att.tdp.issueflow.mention.MentionPageResponse;
import com.att.tdp.issueflow.mention.MentionService;
import com.att.tdp.issueflow.project.ProjectService;
import com.att.tdp.issueflow.project.dto.CreateProjectRequest;
import com.att.tdp.issueflow.project.dto.ProjectResponse;
import com.att.tdp.issueflow.project.dto.WorkloadResponse;
import com.att.tdp.issueflow.project.WorkloadService;
import com.att.tdp.issueflow.ticket.TicketCsvService;
import com.att.tdp.issueflow.ticket.TicketEscalationService;
import com.att.tdp.issueflow.ticket.TicketPriority;
import com.att.tdp.issueflow.ticket.TicketService;
import com.att.tdp.issueflow.ticket.TicketStatus;
import com.att.tdp.issueflow.ticket.TicketType;
import com.att.tdp.issueflow.ticket.dto.CreateTicketRequest;
import com.att.tdp.issueflow.ticket.dto.TicketImportResponse;
import com.att.tdp.issueflow.ticket.dto.TicketResponse;
import com.att.tdp.issueflow.ticket.dto.UpdateTicketRequest;
import com.att.tdp.issueflow.user.UserRole;
import com.att.tdp.issueflow.user.UserService;
import com.att.tdp.issueflow.user.dto.CreateUserRequest;
import com.att.tdp.issueflow.user.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class IssueFlowApplicationTests {

	private final UserService userService;
	private final ProjectService projectService;
	private final TicketService ticketService;
	private final CommentService commentService;
	private final MentionService mentionService;
	private final TicketDependencyService ticketDependencyService;
	private final WorkloadService workloadService;
	private final TicketEscalationService ticketEscalationService;
	private final TicketCsvService ticketCsvService;
	private final AuthService authService;

	@Autowired
	IssueFlowApplicationTests(
			UserService userService,
			ProjectService projectService,
			TicketService ticketService,
			CommentService commentService,
			MentionService mentionService,
			TicketDependencyService ticketDependencyService,
			WorkloadService workloadService,
			TicketEscalationService ticketEscalationService,
			TicketCsvService ticketCsvService,
			AuthService authService
	) {
		this.userService = userService;
		this.projectService = projectService;
		this.ticketService = ticketService;
		this.commentService = commentService;
		this.mentionService = mentionService;
		this.ticketDependencyService = ticketDependencyService;
		this.workloadService = workloadService;
		this.ticketEscalationService = ticketEscalationService;
		this.ticketCsvService = ticketCsvService;
		this.authService = authService;
	}

	@Test
	void contextLoads() {
	}

	@Test
	void createProjectStoresRequiredOwner() {
		UserResponse owner = createUser("owner");

		ProjectResponse project = projectService.createProject(new CreateProjectRequest(
				"Owner Project",
				"Project with an owner",
				owner.id()
		));

		assertThat(project.ownerId()).isEqualTo(owner.id());
		assertThat(project.developerIds()).containsExactly(owner.id());
		assertThat(project.deleted()).isFalse();
	}

	@Test
	void loginReturnsBearerTokenForCreatedUser() {
		createUser("login-user");

		LoginResponse response = authService.login(new LoginRequest("login-user", "password123"));

		assertThat(response.token()).isNotBlank();
		assertThat(response.tokenType()).isEqualTo("Bearer");
	}

	@Test
	void workloadAndAutoAssignmentUseOnlyProjectDevelopers() {
		UserResponse admin = createUser("work-admin", UserRole.ADMIN);
		UserResponse firstDeveloper = createUser("work-dev-one");
		UserResponse secondDeveloper = createUser("work-dev-two");
		UserResponse outsideDeveloper = createUser("work-outside");
		ProjectResponse project = projectService.createProject(new CreateProjectRequest(
				"Workload Project",
				"Project with linked developers",
				admin.id(),
				Set.of(firstDeveloper.id(), secondDeveloper.id())
		));

		createTicket(project.id(), firstDeveloper.id(), "Already assigned");
		TicketResponse autoAssigned = createTicket(project.id(), null, "Needs assignment");

		assertThat(autoAssigned.assigneeId()).isEqualTo(secondDeveloper.id());

		List<WorkloadResponse> workload = workloadService.getWorkload(project.id());
		assertThat(workload).extracting(WorkloadResponse::userId)
				.containsExactlyInAnyOrder(firstDeveloper.id(), secondDeveloper.id())
				.doesNotContain(outsideDeveloper.id());
		assertThat(workload).extracting(WorkloadResponse::openTicketCount)
				.containsExactly(1L, 1L);
	}

	@Test
	void softDeletedTicketsAreHiddenUntilRestored() {
		UserResponse owner = createUser("soft-owner");
		ProjectResponse project = createProject("Soft Delete Project", owner.id());
		TicketResponse ticket = createTicket(project.id(), owner.id(), "Soft Deleted Ticket");

		ticketService.deleteTicket(ticket.id());

		assertThatThrownBy(() -> ticketService.getTicketById(ticket.id()))
				.isInstanceOf(ResourceNotFoundException.class);
		assertThat(ticketService.getDeletedTicketsByProject(project.id()))
				.extracting(TicketResponse::id)
				.containsExactly(ticket.id());

		ticketService.restoreTicket(ticket.id());

		assertThat(ticketService.getTicketById(ticket.id()).deleted()).isFalse();
		assertThat(ticketService.getDeletedTicketsByProject(project.id())).isEmpty();
	}

	@Test
	void escalationPromotesOverdueTicketsOncePerDay() {
		UserResponse owner = createUser("escalation-owner");
		ProjectResponse project = createProject("Escalation Project", owner.id());
		TicketResponse ticket = ticketService.createTicket(new CreateTicketRequest(
				"Overdue Ticket",
				"Needs escalation",
				TicketStatus.TODO,
				TicketPriority.LOW,
				TicketType.BUG,
				project.id(),
				owner.id(),
				Instant.now().minus(2, ChronoUnit.DAYS)
		));

		ticketEscalationService.escalateOverdueTickets();

		TicketResponse escalated = ticketService.getTicketById(ticket.id());
		assertThat(escalated.priority()).isEqualTo(TicketPriority.MEDIUM);
		assertThat(escalated.lastAutoEscalatedAt()).isNotNull();

		ticketEscalationService.escalateOverdueTickets();

		assertThat(ticketService.getTicketById(ticket.id()).priority()).isEqualTo(TicketPriority.MEDIUM);
	}

	@Test
	void csvImportCreatesValidRowsAndReportsInvalidRows() {
		UserResponse owner = createUser("csv-owner");
		ProjectResponse project = createProject("CSV Project", owner.id());
		String csv = """
				title,description,status,priority,type,assigneeId,dueDate
				Imported ticket,Created from CSV,TODO,HIGH,BUG,,2026-06-01
				Bad ticket,Invalid priority,TODO,WRONG,BUG,,2026-06-01
				""";
		MockMultipartFile file = new MockMultipartFile(
				"file",
				"tickets.csv",
				"text/csv",
				csv.getBytes(StandardCharsets.UTF_8)
		);

		TicketImportResponse response = ticketCsvService.importTickets(project.id(), file);

		assertThat(response.created()).isEqualTo(1);
		assertThat(response.failed()).isEqualTo(1);
		assertThat(response.errors()).singleElement().asString().contains("priority has an invalid value");
		assertThat(ticketService.getTicketsByProject(project.id()))
				.extracting(TicketResponse::title)
				.contains("Imported ticket");
	}

	@Test
	void mentionsAreCaseInsensitiveAndRefreshOnCommentUpdate() {
		UserResponse mentioned = createUser("alice");
		UserResponse author = createUser("author");
		ProjectResponse project = createProject("Mentions Project", author.id());
		TicketResponse ticket = createTicket(project.id(), null, "Mention Ticket");

		CommentResponse comment = commentService.createComment(ticket.id(), new CreateCommentRequest(
				author.id(),
				"Please check this @ALICE"
		));

		assertThat(comment.mentionedUsers())
				.singleElement()
				.extracting("username")
				.isEqualTo("alice");

		MentionPageResponse firstPage = mentionService.getMentionsForUser(mentioned.id(), 1, 10);
		assertThat(firstPage.total()).isEqualTo(1);
		assertThat(firstPage.page()).isEqualTo(1);
		assertThat(firstPage.data()).extracting(CommentResponse::id).containsExactly(comment.id());

		commentService.updateComment(ticket.id(), comment.id(), new UpdateCommentRequest("No mention now"));

		MentionPageResponse refreshedPage = mentionService.getMentionsForUser(mentioned.id(), 1, 10);
		assertThat(refreshedPage.total()).isZero();
		assertThat(refreshedPage.data()).isEmpty();
	}

	@Test
	void commentUpdateMustUseTheTicketThatOwnsTheComment() {
		UserResponse author = createUser("commenter");
		ProjectResponse project = createProject("Comment Ownership Project", author.id());
		TicketResponse firstTicket = createTicket(project.id(), null, "First Ticket");
		TicketResponse secondTicket = createTicket(project.id(), null, "Second Ticket");
		CommentResponse comment = commentService.createComment(firstTicket.id(), new CreateCommentRequest(author.id(), "Original"));

		assertThatThrownBy(() -> commentService.updateComment(secondTicket.id(), comment.id(), new UpdateCommentRequest("Wrong path")))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("does not belong");
	}

	@Test
	void ticketLifecycleRejectsBackwardMovesDoneUpdatesAndUnresolvedBlockers() {
		UserResponse owner = createUser("ticket-owner");
		ProjectResponse project = createProject("Ticket Rules Project", owner.id());
		TicketResponse blocked = createTicket(project.id(), owner.id(), "Blocked Ticket");
		TicketResponse blocker = createTicket(project.id(), owner.id(), "Blocker Ticket");

		ticketService.updateTicket(blocked.id(), new UpdateTicketRequest(null, null, TicketStatus.IN_PROGRESS, null, null, null, null));

		assertThatThrownBy(() -> ticketService.updateTicket(blocked.id(), new UpdateTicketRequest(null, null, TicketStatus.TODO, null, null, null, null)))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("cannot move backward");

		ticketDependencyService.addDependency(blocked.id(), new CreateTicketDependencyRequest(blocker.id()));

		assertThatThrownBy(() -> ticketService.updateTicket(blocked.id(), new UpdateTicketRequest(null, null, TicketStatus.DONE, null, null, null, null)))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("unresolved blockers");

		ticketService.updateTicket(blocker.id(), new UpdateTicketRequest(null, null, TicketStatus.DONE, null, null, null, null));
		ticketService.updateTicket(blocked.id(), new UpdateTicketRequest(null, null, TicketStatus.DONE, null, null, null, null));

		assertThatThrownBy(() -> ticketService.updateTicket(blocked.id(), new UpdateTicketRequest("After done", null, null, null, null, null, null)))
				.isInstanceOf(BadRequestException.class)
				.hasMessageContaining("DONE ticket");
	}

	private UserResponse createUser(String username) {
		return createUser(username, UserRole.DEVELOPER);
	}

	private UserResponse createUser(String username, UserRole role) {
		return userService.createUser(new CreateUserRequest(
				username,
				username + "@example.com",
				"password123",
				username + " User",
				role
		));
	}

	private ProjectResponse createProject(String name, Long ownerId) {
		return projectService.createProject(new CreateProjectRequest(name, "Description", ownerId));
	}

	private TicketResponse createTicket(Long projectId, Long assigneeId, String title) {
		return ticketService.createTicket(new CreateTicketRequest(
				title,
				"Description",
				TicketStatus.TODO,
				TicketPriority.LOW,
				TicketType.BUG,
				projectId,
				assigneeId,
				null
		));
	}

}
