package lazarant;
/**
* Created by IntelliJ IDEA.
* User: Andrew
* Date: 26.11.11
* Time: 20:42
* To change this template use File | Settings | File Templates.
*/
interface DistEvaluator {
    int worstValue();

    boolean betterOrEqual(int newValue, int oldValue);

    boolean better(int newValue, int oldValue);
}
