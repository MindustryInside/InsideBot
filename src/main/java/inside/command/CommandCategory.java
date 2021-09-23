package inside.command;

public enum CommandCategory{
    common,
    admin,
    owner;

    public static final CommandCategory[] all = values();
}
