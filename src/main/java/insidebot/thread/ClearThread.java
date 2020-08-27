package insidebot.thread;

import static insidebot.InsideBot.listener;

public class ClearThread extends Thread{
    public ClearThread(){
        start();
    }

    @Override
    public void run() {
        while (true){
            try {
                listener.messages.clear();
                sleep(43200000); // 12 часов
            }catch (InterruptedException ignored){}
        }
    }
}
