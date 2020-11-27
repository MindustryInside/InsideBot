package insidebot;

public class Listener{

    protected void register(){
        // Внутренние ивенты

        /*Events.on(MemberUnmuteEvent.class, event -> {
            Member member = event.userInfo.asMember().block();
            if(DiscordUtil.isBot(member)) return;

            event.userInfo.muteEndDate(null);
            userService.delete(event.userInfo);
            member.removeRole(muteRoleID).block();
            log(e -> {
                e.setTitle(messageService.get("message.unmute"));
                e.setDescription(messageService.format("message.unmute.text", event.userInfo.name()));
                e.setFooter(MessageUtil.zonedFormat(), null);
                e.setColor(userUnmute.color);
            });
        });

        Events.on(MemberMuteEvent.class, event -> {
            Member member = guild.getMemberById(event.user.getId()).block();
            LocalMember userInfo = userService.getById(event.user.getId());
            if(DiscordUtil.isBot(member) || userInfo == null) return;

            Calendar calendar = Calendar.getInstance();
            calendar.roll(Calendar.DAY_OF_YEAR, +event.delay);
            userInfo.muteEndDate(calendar);
            userService.save(userInfo);
            member.addRole(muteRoleID).block();
            log(embedBuilder -> {
                embedBuilder.setTitle(messageService.get("message.mute"));
                embedBuilder.setDescription(messageService.format("message.mute.text", event.user.getMention(), event.delay));
                embedBuilder.setFooter(MessageUtil.zonedFormat(), null);
                embedBuilder.setColor(userMute.color);
            });
        });

        Events.on(MessageClearEvent.class, event -> {
            Flux.fromIterable(event.history).subscribe(m -> {
                buffer.add(m.getId());
                m.delete().block();
            });

            log(embed -> {
                embed.setTitle(messageService.format("message.clear", event.count, event.channel.getName()));
                embed.setDescription(messageService.format("message.clear.text", event.user.getUsername(), event.count, event.channel.getName()));
                embed.setFooter(MessageUtil.zonedFormat(), null);
                embed.setColor(messageClear.color);

                StringBuilder builder = new StringBuilder();
                event.history.forEach(m -> {
                    builder.append('[').append(MessageUtil.dateTime().withZone(ZoneId.systemDefault()).format(m.getTimestamp())).append("] ");
                    builder.append(m.getUserData().username()).append(" > ");
                    builder.append(m.getContent());
                    if(!m.getAttachments().isEmpty()){
                        builder.append("\n---\n");
                        m.getAttachments().forEach(a -> builder.append(a.getUrl()).append('\n'));
                    }
                    builder.append('\n');
                });
                temp.writeString(builder.toString());
            }, true);
        });*/
    }
}
