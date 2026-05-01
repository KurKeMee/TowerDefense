package rcpa.project.model;

import rcpa.project.entity.base.AttackType;
import rcpa.project.entity.base.Enemy;
import rcpa.project.entity.base.Player;
import rcpa.project.entity.base.Tower;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameState implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum UpdateType{
        FULL_STATE,        // Полное состояние (при подключении)
        DELTA_UPDATE,      // Только изменения
        ENEMY_SPAWN,       // Новый враг
        ENEMY_DEATH,       // Враг умер
        TOWER_PLACED,      // Башня поставлена
        TOWER_REMOVED,     // Башня убрана
        TOWER_UPGRADED,    // Башня улучшена
        ATTACK_CREATED,    // Новая атака
        PLAYER_STATS       // Обновление статистики игрока
    }

    private UpdateType updateType;
    private int waveLevel;
    private long timestamp;

    private Map<Integer, PlayerData> players;

    private List<EnemyData> newEnemies;
    private List<Integer> deadEnemies;
    private List<TowerData> towersChanged;
    private List<AttackData> newAttacks;

    public GameState() {
        this.players = new HashMap<>();
        this.newEnemies = new ArrayList<>();
        this.deadEnemies = new ArrayList<>();
        this.towersChanged = new ArrayList<>();
        this.newAttacks = new ArrayList<>();
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
            this.money = 200;
            this.isReady = false;
        }
    }

    public static class EnemyData implements Serializable {
        private static final long serialVersionUID = 1L;

        public int enemyId;
        public float x,y;
        public double health;
        public double maxHealth;
        public int pathIndex;

        public EnemyData(Enemy enemy) {
            this.enemyId = enemy.getId();
            this.x = enemy.getX();
            this.y = enemy.getY();
            this.health = enemy.getHealth();
            this.maxHealth = enemy.getMaxHealth();
            this.pathIndex = enemy.getCurrentPosition().getXCord();
        }
    }

    public static class TowerData implements Serializable {
        private static final long serialVersionUID = 1L;

        public enum Action{
            PLACED,REMOVED,UPGRADED
        }

        public int playerId;
        public int towerId;
        public int x,y;
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
        public int ownerId;
        public int targetId;
        public int x,y;
        public int targetX, targetY;
        public AttackType attackType;
        public double damage;

        public AttackData(int attackId, Tower tower){
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
