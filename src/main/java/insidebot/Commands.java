package insidebot;

public class Commands{

    public Commands(){

        // handler.register("warn", "<@user> [reason...]", messageService.get("command.warn.description"), args -> {
        //     try{
        //         LocalMember info = userService.getById(MessageUtil.parseUserId(args[0]));
        //         User user = info.asUser().block();
        //
        //         if(isAdmin(listener.guild.getMemberById(user.getId()).block())){
        //             listener.err(messageService.get("command.user-is-admin"));
        //             return;
        //         }
        //
        //         if(user.isBot()){
        //             listener.err(messageService.get("command.user-is-bot"));
        //             return;
        //         }
        //
        //         if(listener.lastUser == user){
        //             listener.err(messageService.get("command.warn.self-user"));
        //             return;
        //         }
        //
        //         int warnings = info.addWarn();
        //
        //         listener.text(messageService.format("message.warn", user.getUsername(),
        //                                             warningStrings[Mathf.clamp(warnings - 1, 0, warningStrings.length - 1)]));
        //
        //         if(warnings >= 3){
        //             listener.guild.ban(user.getId(), b -> b.setDeleteMessageDays(0)).block();
        //         }else{
        //             userService.save(info);
        //         }
        //     }catch(Exception e){
        //         listener.err(messageService.get("command.incorrect-name"));
        //     }
        // });

        // handler.register("warnings", "<@user>", messageService.get("command.warnings.description"), args -> {
        //     try{
        //         LocalMember info = userService.getById(MessageUtil.parseUserId(args[0]));
        //         int warnings = info.warns();
        //
        //         listener.text(messageService.format("command.warnings", info.name(), warnings,
        //                                             warnings == 1 ? messageService.get("command.warn") : messageService.get("command.warns")));
        //     }catch(Exception e){
        //         listener.err(messageService.get("command.incorrect-name"));
        //     }
        // });

        // handler.register("unwarn", "<@user> [count]", messageService.get("command.unwarn.description"), args -> {
        //     if(args.length > 1 && !MessageUtil.canParseInt(args[1])){
        //         listener.text(messageService.get("command.incorrect-number"));
        //         return;
        //     }
        //
        //     int warnings = args.length > 1 ? Strings.parseInt(args[1]) : 1;
        //
        //     try{
        //         LocalMember info = userService.getById(MessageUtil.parseUserId(args[0]));
        //         info.warns(info.warns() - warnings);
        //
        //         listener.text(messageService.format("command.unwarn", info.name(), warnings,
        //                                             warnings == 1 ? messageService.get("command.warn") : messageService.get("command.warns")));
        //         userService.save(info);
        //     }catch(Exception e){
        //         listener.err(messageService.get("command.incorrect-name"));
        //     }
        // });
    }
}
