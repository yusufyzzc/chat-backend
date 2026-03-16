package com.chatapp.chat_backend.repository;

import com.chatapp.chat_backend.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Page<Conversation> findByParticipants_Id(Long userId, Pageable pageable);
}