package Scent.Danielle.Utils;
public class ChatItem {
    private final int avatarResId;
    private final String name;
    private final String message;

    public ChatItem(int avatarResId, String name, String message) {
        this.avatarResId = avatarResId;
        this.name = name;
        this.message = message;
    }
    public int getAvatarResId() {
        return avatarResId;
    }

    public String getName() {
        return name;
    }

    public String getMessage() {
        return message;
    }
}