package inside.command.admin;

import inside.command.Command;
import inside.command.model.CommandEnvironment;
import inside.service.AdminService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import reactor.core.publisher.Mono;

public abstract class AdminCommand extends Command{
    @Lazy
    @Autowired
    protected AdminService adminService;

    @Override
    public Mono<Boolean> filter(CommandEnvironment env){
        return adminService.isAdmin(env.member());
    }
}
