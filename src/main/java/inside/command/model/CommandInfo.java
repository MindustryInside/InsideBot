package inside.command.model;

import discord4j.rest.util.*;

public record CommandInfo(String[] text, String paramText, String description, CommandParam[] params, PermissionSet permissions){

    public CommandInfo(String[] text, String paramText, String description, CommandParam[] params, Permission[] permissions){
        this(text, paramText, description, params, PermissionSet.of(permissions));
    }
}
