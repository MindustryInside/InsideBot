package inside.command.model;

import discord4j.rest.util.*;

public class CommandInfo{
    public final String text;
    public final String paramText;
    public final String description;
    public final CommandParam[] params;
    public final PermissionSet permissions;

    public CommandInfo(String text, String paramText, String description, CommandParam[] params, Permission[] permissions){
        this.text = text;
        this.paramText = paramText;
        this.description = description;
        this.params = params;
        this.permissions = PermissionSet.of(permissions);
    }
}
