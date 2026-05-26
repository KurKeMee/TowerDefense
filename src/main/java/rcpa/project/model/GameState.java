package rcpa.project.model;

import rcpa.project.entity.base.AttackType;
import rcpa.project.entity.base.Tower;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameState implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum UpdateType {
        FULL_STATE,
        FULL_UPDATE,
        ENEMY_SPAWN,
        ENEMY_DEATH,
        ENEMY_MOVED,
        TOWER_PLACED,
        TOWER_REMOVED,
        TOWER_UPGRADED,
        ATTACK_CREATED,
        ATTACK_MOVED,
        PLAYER_STATS
    }

    private UpdateType updateType;
    private int waveLevel;
    private long timestamp;

    private Map<Integer, PlayerData> players;
    private List<EnemyData> newEnemies;
    private List<Integer> deadEnemies;
    private List<EnemyMoveData> enemyMoves;
    private List<TowerData> towersChanged;
    private List<AttackData> newAttacks;
    private List<AttackMoveData> attackMoves;
    private List<PlayerData> playerData;

    public GameState() {
        this.players = new HashMap<>();
        this.newEnemies = new ArrayList<>();
        this.deadEnemies = new ArrayList<>();
        this.enemyMoves = new ArrayList<>();
        this.towersChanged = new ArrayList<>();
        this.newAttacks = new ArrayList<>();
        this.attackMoves = new ArrayList<>();
        this.playerData = new ArrayList<>();
        this.timestamp = System.currentTimeMillis();
    }

    public static class PlayerData implements Serializable {
        private static final long serialVersionUID = 1L;

        public int playerId;
        public String playerName;
        public int money;
        public boolean isReady;

        public PlayerData(int playerId, String playerName) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.money = 300;
            this.isReady = false;
        }

        public PlayerData(int playerId, String playerName, int money) {
            this.playerId = playerId;
            this.playerName = playerName;
            this.money = money;
            this.isReady = false;
        }

        public PlayerData(int playerId, int money) {
            this.playerId = playerId;
            this.money = money;
        }
    }

    public static class EnemyData implements Serializable {
        private static final long serialVersionUID = 1L;

        public int marketId;
        public int enemyId;
        public int x, y;

        public EnemyData(int marketId, int enemyId, int x, int y) {
            this.marketId = marketId;
            this.enemyId = enemyId;
            this.x = x;
            this.y = y;
        }
    }

    public static class TowerData implements Serializable {
        private static final long serialVersionUID = 1L;

        public enum Action { PLACED, REMOVED, UPGRADED }

        public int playerId;
        public int towerId;
        public int marketId;
        public int x, y;
        public int level;
        public Action action;

        public TowerData(int playerId, Tower tower, Action action) {
            this.playerId = playerId;
            this.towerId = tower.getId();
            this.x = tower.getX();
            this.y = tower.getY();
            this.level = tower.getLevel();
            this.action = action;
        }
    }

    public static class AttackData implements Serializable {
        private static final long serialVersionUID = 1L;

        public int attackId;
        public double angle;
        public int ownerId;
        public int targetId;
        public int x,y;
        public int targetX, targetY;
        public AttackType attackType;
        public double damage;

        public AttackData(int attackId, double angle, Tower tower){
            this.angle = angle;
            this.attackId = attackId;
            this.ownerId = tower.getId();
            this.targetId = tower.getTarget().getId();
            this.x = tower.getX();
            this.y = tower.getY();
            this.targetX = tower.getTarget().getX();
            this.targetY = tower.getTarget().getY();
            this.attackType = tower.getAttack().getAttackType();
            this.damage = tower.getAttack().getDamage();
        }
    }

    public static class EnemyMoveData implements Serializable {
        private static final long serialVersionUID = 1L;

        public int enemyId;
        public int x, y;
        public double health;
        public int lookOrientation;

        public EnemyMoveData(int enemyId, int x, int y, double health, int lookOrientation) {
            this.enemyId = enemyId;
            this.x = x;
            this.y = y;
            this.health = health;
            this.lookOrientation = lookOrientation;
        }
    }

    public static class AttackMoveData implements Serializable {
        private static final long serialVersionUID = 1L;

        public int timeSpent;
        public int attackId;
        public int x, y;
        public boolean completed;

        public AttackMoveData(int attackId, int x, int y, boolean completed) {
            this.attackId = attackId;
            this.x = x;
            this.y = y;
            this.completed = completed;
        }
        public AttackMoveData(int timeSpent,int attackId, int x, int y, boolean completed) {
            this.timeSpent = timeSpent;
            this.attackId = attackId;
            this.x = x;
            this.y = y;
            this.completed = completed;
        }
    }

    public List<EnemyMoveData> getEnemyMoves() { return enemyMoves; }
    public void setEnemyMoves(List<EnemyMoveData> enemyMoves) { this.enemyMoves = enemyMoves; }
    public List<PlayerData> getPlayersData() { return playerData; }
    public void setPlayerData(List<PlayerData> playerData) { this.playerData = playerData; }
    public List<AttackMoveData> getAttackMoves() { return attackMoves; }
    public void setAttackMoves(List<AttackMoveData> attackMoves) { this.attackMoves = attackMoves; }

    public UpdateType getUpdateType() { return updateType; }
    public void setUpdateType(UpdateType updateType) { this.updateType = updateType; }

    public int getWaveLevel() { return waveLevel; }
    public void setWaveLevel(int waveLevel) { this.waveLevel = waveLevel; }

    public long getTimestamp() { return timestamp; }
    public void setTimestamp(long timestamp) { this.timestamp = timestamp; }

    public Map<Integer, PlayerData> getPlayers() { return players; }
    public void setPlayers(Map<Integer, PlayerData> players) { this.players = players; }

    public List<EnemyData> getNewEnemies() { return newEnemies; }
    public void setNewEnemies(List<EnemyData> newEnemies) { this.newEnemies = newEnemies; }

    public List<Integer> getDeadEnemies() { return deadEnemies; }
    public void setDeadEnemies(List<Integer> deadEnemies) { this.deadEnemies = deadEnemies; }

    public List<TowerData> getTowersChanged() { return towersChanged; }
    public void setTowersChanged(List<TowerData> towersChanged) { this.towersChanged = towersChanged; }

    public List<AttackData> getNewAttacks() { return newAttacks; }
    public void setNewAttacks(List<AttackData> newAttacks) { this.newAttacks = newAttacks; }

    public boolean isEmpty() {
        if (enemyMoves != null && !enemyMoves.isEmpty()) return false;
        if (attackMoves != null && !attackMoves.isEmpty()) return false;

        return newEnemies.isEmpty() && deadEnemies.isEmpty() &&
                towersChanged.isEmpty() && newAttacks.isEmpty();
    }

    public void clear() {
        newEnemies.clear();
        deadEnemies.clear();
        towersChanged.clear();
        newAttacks.clear();
    }

}
