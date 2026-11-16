
package com.novatwo.assistant.memory;

import android.content.Context;

import org.json.JSONArray;
import org.json.JSONObject;

public class MemoryManager {

    private final ConversationMemory memory;

    public MemoryManager(Context context) {
        memory = new ConversationMemory(context);
    }

    public void saveUserMessage(String message) {
        memory.saveMessage("user", message);
    }

    public void saveAssistantMessage(String message) {
        memory.saveMessage("assistant", message);
    }

    public JSONArray getConversationHistory() {
        return memory.getMessages();
    }

    public int getMessageCount() {
        return memory.getMessageCount();
    }

    public boolean hasMemory() {
        return memory.hasMemory();
    }

    public void clearMemory() {
        memory.clearMemory();
    }

    public String getConversationText() {
        JSONArray messages = memory.getMessages();
        StringBuilder history = new StringBuilder();

        for (int i = 0; i < messages.length(); i++) {
            JSONObject message = messages.optJSONObject(i);

            if (message == null) {
                continue;
            }

            String role = message.optString("role");
            String content = message.optString("content");

            history.append(role)
                    .append(": ")
                    .append(content)
                    .append("\n");
        }

        return history.toString();
    }
}
