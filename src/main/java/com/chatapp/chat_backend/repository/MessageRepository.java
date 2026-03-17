package com.chatapp.chat_backend.repository;

import com.chatapp.chat_backend.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    Page<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId, Pageable pageable);
    Page<Message> findByConversationIdAndContentContainingIgnoreCase(Long conversationId, String keyword, Pageable pageable);
}