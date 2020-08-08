package AiHub;

import lazarant.MyBot;
import lazarant.Order;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jason Turan on 7/28/2020.
 */
public class LazarantBotCommandAdapter implements CommandAdapter {
    private MyBot lazarantBot;

    public void init(List<String> startData) {
        lazarantBot = new MyBot();
        for (String line : startData) {
            lazarantBot.processLine(line);
        }
        lazarantBot.processLine("ready");
    }

    public String playTurn(List<String> playData) {
        for (String line : playData) {
            lazarantBot.processLine(line);
        }
        lazarantBot.processLine("go");
        List<Order> orders = lazarantBot.getOrders();
        List<String> commands = new ArrayList<String>();
        for (Order o : orders) {
            commands.add(o.toCommand());
        }
        lazarantBot.clearOrders();
        return String.join("\n", commands);
    }
}
