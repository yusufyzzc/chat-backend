package com.chatapp.chat_backend.repository;

import com.chatapp.chat_backend.entity.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, Long> {
    Page<Conversation> findByParticipants_Id(Long userId, Pageable pageable);

    @Modifying
    @Transactional
    @Query(value = "delete from conversation_participants where user_id = :userId", nativeQuery = true)
    void deleteParticipantRowsByUserId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query(value = "delete from conversation_participants where conversation_id = :conversationId", nativeQuery = true)
    void deleteParticipantRowsByConversationId(@Param("conversationId") Long conversationId);
}