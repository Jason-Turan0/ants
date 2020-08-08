package lazarant;
public enum AttackIntension {
    ATTACK, SUPPORT, ESCAPE, IGNORE, SKIP;

    public boolean isAttack() {
        return ordinal() <= ATTACK.ordinal();
    }

    public boolean isAggression() {
        return ordinal() <= SUPPORT.ordinal();
    }
}
