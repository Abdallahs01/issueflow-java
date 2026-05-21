package com.att.tdp.issueflow;
import com.att.tdp.issueflow.comment.CommentService;
import com.att.tdp.issueflow.comment.dto.CommentResponse;
import com.att.tdp.issueflow.comment.dto.CreateCommentRequest;
import com.att.tdp.issueflow.comment.dto.UpdateCommentRequest;
import com.att.tdp.issueflow.common.BadRequestException;
import com.att.tdp.issueflow.dependency.TicketDependencyService;
import com.att.tdp.issueflow.dependency.dto.CreateTicketDependencyRequest;
import com.att.tdp.issueflow.mention.MentionPageResponse;
import com.att.tdp.issueflow.mention.MentionService;
import com.att.tdp.issueflow.project.ProjectService;
import com.att.tdp.issueflow.project.dto.CreateProjectRequest;
import com.att.tdp.issueflow.project.dto.ProjectResponse;
import com.att.tdp.issueflow.ticket.TicketPriority;
import com.att.tdp.issueflow.ticket.TicketService;
import com.att.tdp.issueflow.ticket.TicketStatus;
import com.att.tdp.issueflow.ticket.TicketType;
import com.att.tdp.issueflow.ticket.dto.CreateTicketRequest;
import com.att.tdp.issueflow.ticket.dto.TicketResponse;
import com.att.tdp.issueflow.ticket.dto.UpdateTicketRequest;
import com.att.tdp.issueflow.user.UserRole;
import com.att.tdp.issueflow.user.UserService;
import com.att.tdp.issueflow.user.dto.CreateUserRequest;
import com.att.tdp.issueflow.user.dto.UserResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

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

	@Autowired
	IssueFlowApplicationTests(
			UserService userService,
			ProjectService projectService,
			TicketService ticketService,
			CommentService commentService,
			MentionService mentionService,
			TicketDependencyService ticketDependencyService
	) {
		this.userService = userService;
		this.projectService = projectService;
		this.ticketService = ticketService;
		this.commentService = commentService;
		this.mentionService = mentionService;
		this.ticketDependencyService = ticketDependencyService;
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
		assertThat(project.deleted()).isFalse();
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
		return userService.createUser(new CreateUserRequest(
				username,
				username + "@example.com",
				"password123",
				username + " User",
				UserRole.DEVELOPER
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
