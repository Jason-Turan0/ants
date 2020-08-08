package AiHub;

import hippo.MyBot;
import hippo.Order;

import java.util.ArrayList;
import java.util.List;

public class HippoBotCommandAdapter implements CommandAdapter {
    private MyBot hippoBot;

    public void init(List<String> startData) {
        hippoBot = new MyBot();
        for (String line : startData) {
            hippoBot.processLine(line);
        }
        hippoBot.processLine("ready");
    }

    public String playTurn(List<String> playData) {
        for (String line : playData) {
            hippoBot.processLine(line);
        }
        hippoBot.processLine("go");
        List<Order> orders = hippoBot.getAnts().getOrders();
        List<String> commands = new ArrayList<String>();
        for (Order o : orders) {
            commands.add(o.toString());
        }
        hippoBot.getAnts().getOrders().clear();
        return String.join("\n", commands);

    }
}
