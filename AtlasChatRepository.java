package com.chatapp;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * AtlasChatRepository - a minimal MongoDB-backed implementation of ChatRepository.
 *
 * Usage:
 * - Provide a MongoDB connection string (Atlas URI) and database name.
 * - Collection used: `messages` (documents with fields: messageId, sender, receiver, content, timestamp)
 *
 * Note: this is a simple synchronous implementation intended as a skeleton — production code
 * should handle errors, connection pooling, indexing, and retries.
 */
public class AtlasChatRepository implements ChatRepository {
    private final MongoClient client;
    private final MongoDatabase db;
    private final MongoCollection<Document> coll;


    public AtlasChatRepository(String connectionString, String databaseName) {
        if (connectionString == null || connectionString.isEmpty()) {
            this.client = MongoClients.create();
        } else {
            this.client = MongoClients.create(connectionString);
        }
        this.db = client.getDatabase(databaseName == null || databaseName.isEmpty() ? "chatdb" : databaseName);
        this.coll = db.getCollection("messages");
    }

    @Override
    public void saveMessage(Message message) {
        Document d = new Document();
        d.append("messageId", message.messageId);
        d.append("sender", message.senderUserId);
        d.append("receiver", message.receiverUserId);
        d.append("content", message.content);
        d.append("timestamp", message.timestamp);
        coll.insertOne(d);
    }

    @Override
    public List<Message> getConversation(String userA, String userB) {
        // query for messages where (sender=userA AND receiver=userB) OR (sender=userB AND receiver=userA)
        FindIterable<Document> docs = coll.find(
                Filters.or(
                        Filters.and(Filters.eq("sender", userA), Filters.eq("receiver", userB)),
                        Filters.and(Filters.eq("sender", userB), Filters.eq("receiver", userA))
                )
        ).sort(Sorts.ascending("timestamp"));

        List<Message> out = new ArrayList<>();
        for (Document doc : docs) {
            String messageId = doc.getString("messageId");
            String sender = doc.getString("sender");
            String receiver = doc.getString("receiver");
            String content = doc.getString("content");
            long ts = doc.getLong("timestamp") != null ? doc.getLong("timestamp") : 0L;
            out.add(new Message(messageId, sender, receiver, content, ts));
        }
        return out;
    }

    public void close() {
        client.close();
    }
}
