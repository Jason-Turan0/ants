package AiHub;

import pkmiec.MyBot;
import pkmiec.Order;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by Jason Turan on 7/28/2020.
 */
public class PkmiecBotCommandAdapter implements CommandAdapter {
    private MyBot pkmiecBot;

    public void init(List<String> startData) {
        pkmiecBot = new MyBot();
        for (String line : startData) {
            pkmiecBot.processLine(line);
        }
        pkmiecBot.processLine("ready");
    }

    public String playTurn(List<String> playData) {
        for (String line : playData) {
            pkmiecBot.processLine(line);
        }
        pkmiecBot.processLine("go");
        Set<Order> orders = pkmiecBot.getAnts().getOrders();
        List<String> commands = new ArrayList<String>();
        for (Order o : orders) {
            commands.add(o.toString());
        }
        return String.join("\n", commands);
    }
}
