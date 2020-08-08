package lazarant;
/**
* Created by IntelliJ IDEA.
* User: Andrew
* Date: 26.11.11
* Time: 20:43
* To change this template use File | Settings | File Templates.
*/
class MaxEvaluator implements DistEvaluator {

    public static MaxEvaluator instance = new MaxEvaluator();

    public int worstValue() {
        return Integer.MIN_VALUE;
    }

    public boolean betterOrEqual(int newValue, int oldValue) {
        if (newValue == Ants.NEVER) return false;
        return newValue >= oldValue;
    }

    public boolean better(int newValue, int oldValue) {
        if (newValue == Ants.NEVER) return false;
        return newValue > oldValue;
    }
}
