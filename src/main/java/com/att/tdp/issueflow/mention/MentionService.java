package com.att.tdp.issueflow.mention;

import com.att.tdp.issueflow.comment.Comment;
import com.att.tdp.issueflow.comment.dto.CommentResponse;
import com.att.tdp.issueflow.common.BadRequestException;
import com.att.tdp.issueflow.user.User;
import com.att.tdp.issueflow.user.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class MentionService {

    private static final Pattern MENTION_PATTERN = Pattern.compile("@([A-Za-z0-9_]{1,50})");

    private final MentionRepository mentionRepository;
    private final UserRepository userRepository;

    public MentionService(MentionRepository mentionRepository, UserRepository userRepository) {
        this.mentionRepository = mentionRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public void refreshMentions(Comment comment) {
        mentionRepository.deleteByCommentId(comment.getId());
        mentionRepository.flush();

        for (String username : extractUsernames(comment.getBody())) {
            User user = userRepository.findByUsername(username)
                    .orElseThrow(() -> new BadRequestException("Mentioned user @" + username + " does not exist."));
            mentionRepository.save(new Mention(comment, user));
        }
    }

    @Transactional(readOnly = true)
    public List<MentionedUserResponse> getMentionedUsers(Long commentId) {
        return mentionRepository.findByCommentId(commentId)
                .stream()
                .map(mention -> MentionedUserResponse.from(mention.getMentionedUser()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CommentResponse> getMentionsForUser(Long userId) {
        userRepository.findById(userId)
                .orElseThrow(() -> new BadRequestException("User was not found."));

        return mentionRepository.findByMentionedUserIdOrderByCommentCreatedAtDesc(userId)
                .stream()
                .map(mention -> CommentResponse.from(mention.getComment(), getMentionedUsers(mention.getComment().getId())))
                .toList();
    }

    private Set<String> extractUsernames(String body) {
        Set<String> usernames = new LinkedHashSet<>();
        Matcher matcher = MENTION_PATTERN.matcher(body);

        while (matcher.find()) {
            usernames.add(matcher.group(1));
        }

        return usernames;
    }
}
