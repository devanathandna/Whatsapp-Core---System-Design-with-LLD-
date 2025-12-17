package com.chatapp;

import java.util.List;

/**
 * Persistence abstraction for messages.
 * Implement this interface using your Atlas (MongoDB) repository.
 */
public interface ChatRepository {
    // persist a message
    void saveMessage(Message message);

    // return the conversation between two userIds in chronological order
    List<Message> getConversation(String userA, String userB);
}
