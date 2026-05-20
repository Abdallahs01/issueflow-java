package com.att.tdp.issueflow.mention;

import com.att.tdp.issueflow.comment.Comment;
import com.att.tdp.issueflow.user.User;
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
        name = "mentions",
        uniqueConstraints = @UniqueConstraint(name = "uk_comment_mentioned_user", columnNames = {"comment_id", "mentioned_user_id"})
)
public class Mention {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "mentioned_user_id", nullable = false)
    private User mentionedUser;

    private Instant createdAt;

    public Mention(Comment comment, User mentionedUser) {
        this.comment = comment;
        this.mentionedUser = mentionedUser;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
    }
}
