package AiHub;

import SpeedyBot.Field;
import SpeedyBot.Map;
import SpeedyBot.MySpeedyBot;

import java.util.List;
import java.util.Optional;

public class SpeedyBotCommandAdapter implements CommandAdapter {
    private MySpeedyBot bot;
    private Map map;


    public void init(List<String> startData) {
        this.map = extractMap(startData);
        this.bot = new MySpeedyBot(this.map);
        for(String data:startData){
            parseLine(bot, data);
        }
    }

    public String playTurn(List<String> playData) {
        for(String data:playData){
            parseLine(bot, data);
        }
        List<String> commands = bot.doTurn();
        return String.join("\n", commands);
    }

    private Map extractMap(List<String> startData){
      return new Map(
              ExtractIntValueByKey("rows", startData),
              ExtractIntValueByKey("cols", startData),
              ExtractIntValueByKey("viewradius2", startData),
              ExtractIntValueByKey("attackradius2", startData)
              );
    }

    private int ExtractIntValueByKey(String key, List<String> data) {
        for(String line : data){
            if(line.split(" ")[0].equalsIgnoreCase(key)){
                return Integer.parseInt(line.split(" ")[1]);
            }
        }
        throw new RuntimeException("Failed to find key " + key);
    }

    private void parseLine(MySpeedyBot bot, String line) {
            String[] words = line.split(" ");
            String keyWord = words[0];
            if (keyWord.equalsIgnoreCase("loadtime"))
                bot.setLoadTime(Integer.parseInt(words[1]));
            else if (keyWord.equalsIgnoreCase("turntime"))
                bot.setTurnTime(Integer.parseInt(words[1]));
            else if (keyWord.equalsIgnoreCase("turns"))
                bot.setTurns(Integer.parseInt(words[1]));
            else if (keyWord.equalsIgnoreCase("viewradius2"))
                bot.setViewRadius2(Integer.parseInt(words[1]));
            else if (keyWord.equalsIgnoreCase("attackradius2"))
                bot.setAttackRadius2(Integer.parseInt(words[1]));
            else if (keyWord.equalsIgnoreCase("spawnradius2"))
                bot.setSpawnRadius2(Integer.parseInt(words[1]));
            else if (keyWord.equalsIgnoreCase("w"))
                map.setWater(Integer.parseInt(words[2]), Integer.parseInt(words[1]));
            else if (keyWord.equalsIgnoreCase("f"))
                bot.setFood(Integer.parseInt(words[2]), Integer
                        .parseInt(words[1]));
            else if (keyWord.equalsIgnoreCase("a"))
                bot.setAnt(Integer.parseInt(words[2]),
                        Integer.parseInt(words[1]), Integer.parseInt(words[3]));
            else if (keyWord.equalsIgnoreCase("h"))
                bot.setHill(Integer.parseInt(words[2]), Integer
                        .parseInt(words[1]), Integer.parseInt(words[3]));
            else if (keyWord.equalsIgnoreCase("d"))
                bot.antDead(Integer.parseInt(words[2]), Integer
                        .parseInt(words[1]), Integer.parseInt(words[3]));
        }


}
