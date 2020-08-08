package AiHub;

import Memetix.MyBot;
import Memetix.Order;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Jason Turan on 7/28/2020.
 */
public class MemetixBotCommandAdapter implements CommandAdapter {
    private MyBot memetixBot;

    public void init(List<String> startData) {
        memetixBot = new MyBot();
        for (String line : startData) {
            memetixBot.processLine(line);
        }
        memetixBot.processLine("ready");
    }

    public String playTurn(List<String> playData) {
        for (String line : playData) {
            memetixBot.processLine(line);
        }
        memetixBot.processLine("go");
        List<Order> orders = memetixBot.getAnts().issuedOrders;
        List<String> commands = new ArrayList<String>();
        for (Order o : orders) {
            commands.add(o.toString());
        }
        memetixBot.getAnts().issuedOrders.clear();
        return String.join("\n", commands);
    }
}
