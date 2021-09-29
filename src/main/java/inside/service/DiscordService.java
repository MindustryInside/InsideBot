package inside.service;

import discord4j.core.GatewayDiscordClient;
import inside.interaction.chatinput.InteractionCommandHandler;

public interface DiscordService extends InteractionCommandHandler{

    GatewayDiscordClient gateway();
}
