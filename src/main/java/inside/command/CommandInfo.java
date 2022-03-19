package inside.command;

import discord4j.rest.util.Permission;
import discord4j.rest.util.PermissionSet;

public record CommandInfo(String[] key, String paramText, String description, CommandParam[] params,
                          PermissionSet permissions, CommandCategory category) {

    public CommandInfo(String[] key, String paramText, String description, CommandParam[] params,
                       Permission[] permissions, CommandCategory category) {
        this(key, paramText, description, params, PermissionSet.of(permissions), category);
    }
}
