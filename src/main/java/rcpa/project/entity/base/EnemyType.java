package rcpa.project.entity.base;

public enum EnemyType {
    DEFAULT_ENEMY((byte) 0),
    BIG_ENEMY((byte) 1),
    SPEEDY_ENEMY((byte) 2);

    public byte id;

    EnemyType(byte i) {id =i;}
}
