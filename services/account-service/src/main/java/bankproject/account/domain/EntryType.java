package bankproject.account.domain;

public enum EntryType {
    DEPOSIT(Direction.CREDIT),
    WITHDRAWAL(Direction.DEBIT),
    TRANSFER_DEBIT(Direction.DEBIT),
    TRANSFER_CREDIT(Direction.CREDIT),
    TRANSFER_REVERSAL(Direction.CREDIT);

    public enum Direction { DEBIT, CREDIT }

    private final Direction direction;

    EntryType(Direction direction) {
        this.direction = direction;
    }

    public Direction direction() {
        return direction;
    }
}
