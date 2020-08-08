package AiHub;
import py4j.GatewayServer;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class Main {

    private HashMap<String, CommandAdapter> _players = new HashMap<String, CommandAdapter>();

    public void createPlayer(String gameIdentifier, String playerName, String playerType, String startData) {
        String playerKey = gameIdentifier +"_" +playerName;
        List<String> data =  Arrays.asList(startData.split("\n"));
        if(playerType.equalsIgnoreCase( "xathis")){
            CommandAdapter adapter = new XathisCommandAdapter();
            adapter.init(data);
            _players.put(playerKey, adapter);
        }else if(playerType.equalsIgnoreCase("speedyBot")){
            CommandAdapter adapter = new SpeedyBotCommandAdapter();
            adapter.init(data);
            _players.put(playerKey, adapter);
        }
        else if(playerType.equalsIgnoreCase("hippo")){
            CommandAdapter adapter = new HippoBotCommandAdapter();
            adapter.init(data);
            _players.put(playerKey, adapter);
        }
        else if(playerType.equalsIgnoreCase("lazarant")){
            CommandAdapter adapter = new LazarantBotCommandAdapter();
            adapter.init(data);
            _players.put(playerKey, adapter);
        }
        else if(playerType.equalsIgnoreCase("memetix")){
            CommandAdapter adapter = new MemetixBotCommandAdapter();
            adapter.init(data);
            _players.put(playerKey, adapter);
        }
        else if(playerType.equalsIgnoreCase("pkmiec")){
            CommandAdapter adapter = new PkmiecBotCommandAdapter();
            adapter.init(data);
            _players.put(playerKey, adapter);
        }
        else {
            throw new RuntimeException("Invalid playertype " + playerType);
        }
    }

    public String playTurn(String gameIdentifier, String playerName, String playData){
        String playerKey = gameIdentifier +"_" +playerName;
        CommandAdapter c = _players.get(playerKey);
        List<String> data =  Arrays.asList(playData.split("\n"));
        try{
            String commands  = c.playTurn(data);
            return commands;
        }catch (Exception ex){
            return "";
        }
    }

    public void endGame(String gameIdentifier){
        //TODO Delete game instances
    }



    public static void main(String[] args) {
        Main app = new Main();
        // app is now the gateway.entry_point
        GatewayServer server = new GatewayServer(app);
        server.start();
    }
}
