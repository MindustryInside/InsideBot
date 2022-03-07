package inside.service;

import discord4j.core.GatewayDiscordClient;

import java.util.Objects;

public abstract class BaseService {

    protected final GatewayDiscordClient client;

    protected BaseService(GatewayDiscordClient client) {
        this.client = Objects.requireNonNull(client, "client");
    }

    public GatewayDiscordClient getClient() {
        return client;
    }
}
