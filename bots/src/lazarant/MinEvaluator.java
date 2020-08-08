package lazarant;
/**
* Created by IntelliJ IDEA.
* User: Andrew
* Date: 26.11.11
* Time: 20:43
* To change this template use File | Settings | File Templates.
*/
class MinEvaluator implements DistEvaluator {

    public static MinEvaluator instance = new MinEvaluator();

    public int worstValue() {
        return Integer.MAX_VALUE;
    }

    public boolean betterOrEqual(int newValue, int oldValue) {
        if (newValue == Ants.NEVER) return false;
        return newValue <= oldValue;
    }

    public boolean better(int newValue, int oldValue) {
        if (newValue == Ants.NEVER) return false;
        return newValue < oldValue;
    }
}
