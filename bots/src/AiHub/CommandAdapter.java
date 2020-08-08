package AiHub;

import java.util.List;

public interface CommandAdapter {
    void init(List<String> startData);
    String playTurn(List<String> playData);
}
