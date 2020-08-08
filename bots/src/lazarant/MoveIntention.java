package lazarant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MoveIntention {
    int priority;
    List<Direction> moves;

    public MoveIntention(int priority, List<Direction> moves) {
        this.priority = priority;
        this.moves = moves;
    }

    public void updateMovesPriority(List<Direction> moveDirections) {
        Set<Direction> priorityDirections = new HashSet<Direction>(moveDirections);
        List<Direction> result = new ArrayList<Direction>();

        for (Direction move : moves) {
            if (priorityDirections.contains(move)) result.add(move);
        }

        for (Direction move : moves) {
            if (!priorityDirections.contains(move)) result.add(move);
        }

        moves = result;
    }
}
