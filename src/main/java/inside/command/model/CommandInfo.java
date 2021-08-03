package inside.command.model;

import discord4j.rest.util.*;
import inside.command.CommandCategory;

public record CommandInfo(String[] key, String paramText, String description, CommandParam[] params,
                          PermissionSet permissions, CommandCategory category){

    public CommandInfo(String[] key, String paramText, String description, CommandParam[] params,
                       Permission[] permissions, CommandCategory category){
        this(key, paramText, description, params, PermissionSet.of(permissions), category);
    }
}
