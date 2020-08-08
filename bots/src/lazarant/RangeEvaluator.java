package lazarant;
/**
* Created by IntelliJ IDEA.
* User: Andrew
* Date: 26.11.11
* Time: 20:43
* To change this template use File | Settings | File Templates.
*/
class RangeEvaluator implements DistEvaluator {
    private int min;
    private int max;

    RangeEvaluator(int min, int max) {
        this.min = min;
        this.max = max;
    }


    public int worstValue() {
        return min - 1;
    }

    public boolean betterOrEqual(int newValue, int oldValue) {
        if (newValue == Ants.NEVER) return false;
        return newValue >= min && newValue <= max;
    }

    public boolean better(int newValue, int oldValue) {
        if (newValue == Ants.NEVER) return false;
        return false;
    }
}
