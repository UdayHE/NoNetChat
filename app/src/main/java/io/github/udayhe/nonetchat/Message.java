package io.github.udayhe.nonetchat;

public class Message {
    private final String sender;
    private final String recipient;
    private final String content;
    private final long timestamp;

    public Message(String sender, String recipient, String content, long timestamp) {
        this.sender = sender;
        this.recipient = recipient;
        this.content = content;
        this.timestamp = timestamp;
    }

    public String getSender() {
        return sender;
    }

    public String getRecipient() {
        return recipient;
    }

    public String getContent() {
        return content;
    }

    public long getTimestamp() {
        return timestamp;
    }
}
