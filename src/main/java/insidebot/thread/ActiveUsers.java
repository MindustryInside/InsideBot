package insidebot.thread;

import arc.util.Log;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.Calendar;

import static insidebot.InsideBot.*;

public class ActiveUsers extends Thread{

    public int lastWipe;

    public ActiveUsers(){
        start();
    }

    @Override
    public void run() {
        while (true) {
            try {
                Statement statement = data.getCon().createStatement();
                ResultSet resultSet = statement.executeQuery("SELECT MESSAGES_PER_WEEK, LAST_SENT_MESSAGE_DATE FROM DISCORD.USERS_INFO;");

                while (resultSet.next()) {
                    String lastSendMessage = resultSet.getString("LAST_SENT_MESSAGE_DATE");
                    int messages = resultSet.getInt("MESSAGES_PER_WEEK");
                    PreparedStatement stmt = data.getCon().prepareStatement("SELECT ID FROM DISCORD.USERS_INFO WHERE LAST_SENT_MESSAGE_DATE=? AND MESSAGES_PER_WEEK=?;");

                    stmt.setString(1, lastSendMessage);
                    stmt.setInt(2, messages);

                    ResultSet resultId = stmt.executeQuery();

                    long id = 0;
                    while (resultId.next()) {
                        id = resultId.getLong(1);
                    }

                    if (check(id, lastSendMessage, messages)) {
                        if (listener.guild.getMemberById(id) != null) {
                            listener.guild.addRoleToMember(listener.guild.getMemberById(id), listener.jda.getRolesByName(activeUserRoleName, true).get(0)).queue();
                        }
                    } else {
                        if (listener.guild.getMemberById(id) != null) {
                            listener.guild.removeRoleFromMember(listener.guild.getMemberById(id), listener.jda.getRolesByName(activeUserRoleName, true).get(0)).queue();
                        }
                    }
                }

                sleep(60000);
            } catch (InterruptedException | SQLException e) {
                Log.err(e);
            }
        }
    }

    private boolean check (long id, String time, int messages) {
        try {
            Calendar now = Calendar.getInstance();
            now.setTime(data.format().parse(time));

            if (now.get(Calendar.DAY_OF_YEAR) - lastWipe == 14){
                data.getUserInfo(id).clearQueue();
                lastWipe = LocalDateTime.now().getDayOfYear();
            }

            return (LocalDateTime.now().getDayOfWeek().getValue() - now.get(Calendar.DAY_OF_WEEK) <= 7) && messages >= 20;
        } catch (Exception e) {
            return false;
        }
    }
}
