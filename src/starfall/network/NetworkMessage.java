package starfall.network;

import java.util.Arrays;

public class NetworkMessage {

    public enum Type {
        HELLO,
        WELCOME,
        CUSTOMIZE,
        LOBBY_CONFIG,
        LOBBY_STATE,
        START_MATCH,
        MOVE,
        STATE_SYNC,
        KILL_REQ,
        KILL_EVENT,
        VENT_REQ,
        SABOTAGE_REQ,
        SABOTAGE_REPAIR,
        TASK_COMPLETE,
        MEETING_REQ,
        MEETING_START,
        CHAT_MSG,
        VOTE_REQ,
        VOTE_UPDATE,
        VOTE_RESULT,
        GAME_OVER,
        RETURN_LOBBY,
        PING,
        PONG,
        DISCONNECT
    }

    private final Type type;
    private final String[] fields;

    public NetworkMessage(Type type, String... fields) {
        this.type = type;
        this.fields = (fields != null) ? fields : new String[0];
    }

    public Type getType() {
        return type;
    }

    public String getField(int index, String defaultValue) {
        if (index < 0 || index >= fields.length || fields[index] == null) {
            return defaultValue;
        }
        return fields[index];
    }

    public int getIntField(int index, int defaultValue) {
        try {
            return Integer.parseInt(getField(index, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public double getDoubleField(int index, double defaultValue) {
        try {
            return Double.parseDouble(getField(index, String.valueOf(defaultValue)));
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    public boolean getBooleanField(int index, boolean defaultValue) {
        String val = getField(index, String.valueOf(defaultValue));
        return Boolean.parseBoolean(val);
    }

    public String serialize() {
        StringBuilder sb = new StringBuilder(type.name());
        for (String f : fields) {
            sb.append('|').append(sanitize(f));
        }
        return sb.toString();
    }

    public static NetworkMessage deserialize(String rawLine) {
        if (rawLine == null || rawLine.isBlank()) return null;
        String[] parts = rawLine.split("\\|", -1);
        try {
            Type msgType = Type.valueOf(parts[0].trim());
            String[] payload = (parts.length > 1) ? Arrays.copyOfRange(parts, 1, parts.length) : new String[0];
            return new NetworkMessage(msgType, payload);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    public static String sanitize(String input) {
        if (input == null) return "";
        return input.replace("|", "/")
                    .replace(";", ",")
                    .replace("\r", "")
                    .replace("\n", " ");
    }
}