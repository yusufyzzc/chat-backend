package com.chatapp.chat_backend.repository;

import com.chatapp.chat_backend.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {
    Page<Message> findByConversationIdOrderByCreatedAtAsc(Long conversationId, Pageable pageable);
    Page<Message> findByConversationIdAndContentContainingIgnoreCase(Long conversationId, String keyword, Pageable pageable);

    @Modifying
    @Transactional
    @Query("delete from Message m where m.sender.id = :userId")
    void deleteBySenderId(@Param("userId") Long userId);

    @Modifying
    @Transactional
    @Query(value = "delete from messages where conversation_id = :conversationId", nativeQuery = true)
    void hardDeleteByConversationId(@Param("conversationId") Long conversationId);
}