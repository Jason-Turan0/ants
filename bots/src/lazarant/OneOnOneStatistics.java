package lazarant;
public class OneOnOneStatistics {
    int attackCount = 0;
    int escapeCount = 0;

    public boolean isEscaper() {
        if (attackCount + escapeCount < 10) return false;
        return escapeCount > attackCount * 9;
    }
}
