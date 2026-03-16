package com.chatapp.chat_backend.service;

import com.chatapp.chat_backend.entity.Conversation;
import com.chatapp.chat_backend.entity.Message;
import com.chatapp.chat_backend.entity.User;
import com.chatapp.chat_backend.exception.ResourceNotFoundException;
import com.chatapp.chat_backend.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ConversationService conversationService;
    private final UserService userService;

    public Message sendMessage(Long conversationId, Long senderId, String content) {
        Conversation conversation = conversationService.getConversationById(conversationId);
        User sender = userService.getUserById(senderId);

        Message message = new Message();
        message.setConversation(conversation);
        message.setSender(sender);
        message.setContent(content);

        return messageRepository.save(message);
    }

    public List<Message> getMessages(Long conversationId) {
        conversationService.getConversationById(conversationId);
        return messageRepository.findByConversationIdOrderBySentAtAsc(conversationId);
    }

    public void deleteMessage(Long messageId) {
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new ResourceNotFoundException("Message not found with id: " + messageId));
        messageRepository.delete(message);
    }
}