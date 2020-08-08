package AiHub;

import xathis.Connection;
import xathis.Strategy;

import java.util.Arrays;
import java.util.List;

public class XathisCommandAdapter implements CommandAdapter {

    Connection c;

  //  @Override
    public void init(List<String> data) {
        c = new Connection();
        Strategy s = new Strategy();
        c.setup(data, s);
        s.init(c);
    }

   // @Override
    public String playTurn(List<String> playData) {
        c.update(playData);
        c.doTurn();
        String commands = c.finishTurn();
        return commands;
    }
}
