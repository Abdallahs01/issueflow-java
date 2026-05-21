package com.att.tdp.issueflow.mention;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MentionRepository extends JpaRepository<Mention, Long> {

    List<Mention> findByCommentId(Long commentId);

    List<Mention> findByMentionedUserIdOrderByCommentCreatedAtDesc(Long userId);

    Page<Mention> findByMentionedUserIdOrderByCommentCreatedAtDesc(Long userId, Pageable pageable);

    void deleteByCommentId(Long commentId);
}
