package rcpa.project.network;

import rcpa.project.config.Configuration;
import rcpa.project.entity.attacks.SpinAttack;
import rcpa.project.entity.base.*;
import rcpa.project.model.GameState;
import rcpa.project.model.Message;
import rcpa.project.model.PlayerInfo;
import rcpa.project.repository.AttackRepository;
import rcpa.project.repository.CellRepository;
import rcpa.project.repository.EnemyRepository;
import rcpa.project.repository.TowerRepository;
import rcpa.project.util.MapUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static rcpa.project.config.Configuration.CELL_WIDTH;
import static rcpa.project.entity.base.EnemyType.*;

public class GameRoom {
    private String roomId;
    private String roomName;
    private int hostId;
    private GameServer server;
    private Map<Integer, ClientHandler> players;
    private boolean gameStarted;
    private GameState currentState;
    private int waveLevel;
    private long startTime;

    private Map<Integer, Enemy> enemies = new ConcurrentHashMap<>();
    private Map<Integer, Tower> towers = new ConcurrentHashMap<>();
    private Map<Integer, Attack> attacks = new ConcurrentHashMap<>();

    private List<GameState.PlayerData> pendingPlayerDataUpdate = new ArrayList<>();
    private List<GameState.EnemyData> pendingNewEnemies = new ArrayList<>();
    private List<GameState.EnemyMoveData> pendingEnemiesMove = new ArrayList<>();
    private List<Integer> pendingDeadEnemies = new ArrayList<>();
    private List<GameState.TowerData> pendingTowers = new ArrayList<>();
    private List<GameState.AttackData> pendingAttacks = new ArrayList<>();
    private List<GameState.AttackMoveData> pendingAttacksMove = new ArrayList<>();

    private int enemyCountSpawned = 0;
    private int enemiesPerWave = 0;
    private int frameCount = 0;
    private boolean waveActive = false;

    private int enemyRespawn = 100;
    private int nextEnemyId = 0;
    private int nextAttackId = 0;

    private List<List<Byte>> waveEnemyTypes = new ArrayList<>(Arrays.asList(
            new ArrayList<>(Arrays.asList(DEFAULT_ENEMY.id,
                    DEFAULT_ENEMY.id,
                    DEFAULT_ENEMY.id,
                    DEFAULT_ENEMY.id,
                    DEFAULT_ENEMY.id)),
            new ArrayList<>(Arrays.asList(BIG_ENEMY.id,
                    BIG_ENEMY.id,
                    BIG_ENEMY.id,
                    DEFAULT_ENEMY.id,
                    DEFAULT_ENEMY.id,
                    DEFAULT_ENEMY.id,
                    DEFAULT_ENEMY.id,
                    DEFAULT_ENEMY.id,
                    DEFAULT_ENEMY.id)),
            new ArrayList<>(Arrays.asList(BIG_ENEMY.id,
                    BIG_ENEMY.id,
                    SPEEDY_ENEMY.id,
                    SPEEDY_ENEMY.id,
                    SPEEDY_ENEMY.id)),
            new ArrayList<>(Arrays.asList(BIG_ENEMY.id,
                    BIG_ENEMY.id,
                    BIG_ENEMY.id,
                    BIG_ENEMY.id,
                    BIG_ENEMY.id)),
            new ArrayList<>(Arrays.asList(SPEEDY_ENEMY.id,
                    SPEEDY_ENEMY.id,
                    BIG_ENEMY.id,
                    SPEEDY_ENEMY.id,
                    BIG_ENEMY.id,
                    SPEEDY_ENEMY.id,
                    DEFAULT_ENEMY.id,
                    DEFAULT_ENEMY.id,
                    DEFAULT_ENEMY.id,
                    DEFAULT_ENEMY.id))
    ));

    public GameRoom(String roomId, String roomName, int hostId, GameServer server) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.hostId = hostId;
        this.server = server;
        this.players = new ConcurrentHashMap<>();
        this.gameStarted = false;
        this.currentState = new GameState();
        this.waveLevel = 0;

        init();
    }

    private static synchronized void init() {
        MapUtils mapUtils = MapUtils.getMapUtils();
        mapUtils.analyzeMap(Configuration.MAP_1);
    }

    public void addPlayer(int playerId, ClientHandler handler) {
        players.put(playerId, handler);
        PlayerInfo player = new PlayerInfo(playerId, handler.getPlayerName(), null, 0);
        currentState.getPlayers().put(playerId, new GameState.PlayerData(playerId, handler.getPlayerName(), 300));

        Message message = new Message(Message.Type.PLAYER_ASSIGNED);
        message.putData("playerId", playerId);
        message.setSenderId(playerId);
        broadcastPlayer(message);
    }

    public void removePlayer(int playerId) {
        players.remove(playerId);
        currentState.getPlayers().remove(playerId);
        if (playerId == hostId && !players.isEmpty()) {
            hostId = players.keySet().iterator().next();
        }
        if (players.isEmpty()) {
            server.removeRoom(roomId);
        } else {
            broadcastPlayerList();
        }
    }

    public void broadcastPlayerList() {
        Message msg = new Message(Message.Type.PLAYER_LIST);
        List<PlayerInfo> playerInfos = new ArrayList<>();
        players.forEach((id, handler) -> {
            PlayerInfo info = new PlayerInfo(id, handler.getPlayerName(), null, 0);
            info.setHost(id == hostId);
            playerInfos.add(info);
        });
        msg.putData("players", playerInfos);
        msg.putData("hostId", hostId);
        msg.putData("gameStarted", gameStarted);
        broadcast(msg);
    }

    public void startGame() {
        if (players.size() >= 1 && !gameStarted) {
            gameStarted = true;
            startTime = System.currentTimeMillis();
            waveLevel = 1;
            enemiesPerWave = waveEnemyTypes.getFirst().size();
            enemyCountSpawned = 0;
            waveActive = true;
            frameCount = 0;
            sendFullState();

            Message message = new Message(Message.Type.GAME_STARTED);
            message.putData("waveLevel", waveLevel);
            message.putData("enemiesPerWave", enemiesPerWave);
            broadcast(message);
        }
    }

    private void spawnEnemy() {
        List<Enemy> market = EnemyRepository.getEnemyRepository().getEnemiesMarket();
        if (market.isEmpty()) return;
        Enemy template = market.get(waveEnemyTypes.get(waveLevel-1).get(enemyCountSpawned));
        if (template == null) return;
        try {
            Enemy enemy = (Enemy) template.clone();
            enemy.setId(nextEnemyId++);
            enemy.setWay(CellRepository.getCellRepository().buildPath());
            int startX = enemy.getX();
            int startY = enemy.getY();
            enemies.put(enemy.getId(), enemy);
            enemyCountSpawned++;

            GameState.EnemyData enemyData = new GameState.EnemyData(waveEnemyTypes.get(waveLevel-1).get(enemyCountSpawned-1),
                                                                    enemy.getId(),
                                                                    startX,
                                                                    startY);
            pendingNewEnemies.add(enemyData);
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
    }

    public void update() {
        if (!gameStarted || !waveActive) return;
        frameCount++;

        if (frameCount % enemyRespawn == 0 && enemyCountSpawned < enemiesPerWave) {
            spawnEnemy();
        }
        if (enemyCountSpawned >= enemiesPerWave && allEnemiesDead()) {
            if(waveLevel!=5) startNextWave();
            else{
                gameStarted = false;
                Message message = new Message(Message.Type.GAME_OVER);
                message.putData("isVictory", true);
                broadcast(message);
            }
        } else {
            enemyMove();
            towerAttack();
            attackMove();
        }

        if(!pendingAttacks.isEmpty() ||
                !pendingTowers.isEmpty() ||
                !pendingEnemiesMove.isEmpty() ||
                !pendingNewEnemies.isEmpty() ||
                !pendingDeadEnemies.isEmpty() ||
                !pendingAttacksMove.isEmpty() ||
                !pendingPlayerDataUpdate.isEmpty()) {
            GameState state = new GameState();
            state.setUpdateType(GameState.UpdateType.FULL_UPDATE);
            state.setWaveLevel(waveLevel);

            if (!pendingPlayerDataUpdate.isEmpty()) {
                state.setPlayerData(List.copyOf(pendingPlayerDataUpdate));
                pendingPlayerDataUpdate.clear();
            }
            if (!pendingEnemiesMove.isEmpty()) {
                state.setEnemyMoves(List.copyOf(pendingEnemiesMove));
                pendingEnemiesMove.clear();
            }
            if (!pendingTowers.isEmpty()) {
                state.setTowersChanged(List.copyOf(pendingTowers));
                pendingTowers.clear();
            }
            if (!pendingDeadEnemies.isEmpty()) {
                state.setDeadEnemies(List.copyOf(pendingDeadEnemies));
                pendingDeadEnemies.clear();
            }
            if (!pendingAttacks.isEmpty()) {
                state.setNewAttacks(List.copyOf(pendingAttacks));
                pendingAttacks.clear();
            }
            if (!pendingNewEnemies.isEmpty()) {
                state.setNewEnemies(List.copyOf(pendingNewEnemies));
                pendingNewEnemies.clear();
            }
            if (!pendingAttacksMove.isEmpty()) {
                state.setAttackMoves(List.copyOf(pendingAttacksMove));
                pendingAttacksMove.clear();
            }

            broadcastGameState(state);
        }
    }

    private void enemyMove() {
        enemies.values().forEach(e->{
            e.move();

            if(e.isReached()){
                gameStarted = false;

                Message message = new Message(Message.Type.GAME_OVER);
                message.putData("isVictory", false);
                broadcast(message);

                clearRoom();
            }
            else{
                GameState.EnemyMoveData enemyMoveData = new GameState.EnemyMoveData(e.getId(),e.getX(),e.getY(),e.getHealth(),e.getLookOrientation());
                pendingEnemiesMove.add(enemyMoveData);
            }
        });
    }

    private void clearRoom(){
        pendingPlayerDataUpdate.clear();
        pendingAttacks.clear();
        pendingTowers.clear();
        pendingEnemiesMove.clear();
        pendingNewEnemies.clear();
        pendingDeadEnemies.clear();
        pendingAttacksMove.clear();

        towers.clear();
        enemies.clear();
        attacks.clear();
        players.clear();

        CellRepository.getCellRepository().clearCells();
        TowerRepository.getTowerRepository().clearTowers();
        EnemyRepository.getEnemyRepository().clearEnemies();
        AttackRepository.getAttackRepository().clear();
    }

    private void towerAttack() {
        if (towers.isEmpty() || enemies.isEmpty()) return;

        towers.values().forEach(tower -> {
            tower.findTarget(new ArrayList<>(enemies.values()));
            if (tower.canAttack()) {
                try {
                    Attack attack = tower.getAttack();
                    attack.setId(nextAttackId++);
                    switch (attack.getAttackType()) {
                        case SPIN_ATTACK:
                            ((SpinAttack) attack).setOwnTower(tower);
                            attack.setX(tower.getX());
                            attack.setY(tower.getY());
                            break;
                        case SHOOT_ATTACK:
                        case MELEE_ATTACK:
                            attack.setX(tower.getX() + CELL_WIDTH / 2);
                            attack.setY(tower.getY() + CELL_WIDTH / 2);
                            break;
                    }
                    tower.setLastAttackTime(System.currentTimeMillis());
                    Attack newAttack = (Attack) attack.clone();

                    attacks.put(newAttack.getId(), newAttack);
                    tower.setAnimation(true);

                    GameState.AttackData attackData = new GameState.AttackData(newAttack.getId(), newAttack.getAngle(), tower);
                    pendingAttacks.add(attackData);
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }
            }
            if (!tower.isCanAttack() &&
                    System.currentTimeMillis() - tower.getLastAttackTime() >= tower.getAttackCooldown()) {
                tower.setCanAttack();
            }
        });
    }

    private void attackMove() {
        for (Attack attack : attacks.values()) {
            if(attack.getAttackType()==AttackType.SPIN_ATTACK)
                ((SpinAttack) attack).setEnemies(new ArrayList<>(enemies.values()));
            attack.move();
            Enemy target = attack.getTarget();
            GameState.AttackMoveData attackMoveData;

            if (attack.getAttackType() == AttackType.SPIN_ATTACK) {
                attackMoveData = new GameState.AttackMoveData(((SpinAttack) attack).getTimeSpent(), attack.getId(), attack.getX(), attack.getY(), attack.isCompleted());
            } else {
                attackMoveData = new GameState.AttackMoveData(attack.getId(), attack.getX(), attack.getY(), attack.isCompleted());
            }
            pendingAttacksMove.add(attackMoveData);

            if(attack.isCompleted()) attacks.remove(attack.getId());

            if (target.getHealth() <= 0 && enemies.get(target.getId())!=null) {
                enemies.remove(target.getId());
                pendingDeadEnemies.add(target.getId());
                currentState.getPlayers().forEach((id, handler) ->{
                    handler.money+=50;
                    GameState.PlayerData playerData = new GameState.PlayerData(id, handler.money);
                    pendingPlayerDataUpdate.add(playerData);
                });
            }

        }
    }

    private void startNextWave() {
        waveLevel++;
        enemiesPerWave = waveEnemyTypes.get(waveLevel-1).size();
        switch (waveLevel){
            case 1:
                enemyRespawn = 100;
                break;
            case 2:
                enemyRespawn = 75;
                break;
            case 3:
            case 4:
                break;
            case 5:
                enemyRespawn = 60;
                break;
        }


        enemyCountSpawned = 0;
        frameCount = 0;
        towers.values().forEach(t -> { t.setLastAttackTime(0); t.setCanAttack(); });
        attacks.clear();

        Message msg = new Message(Message.Type.WAVE_START);
        msg.putData("waveLevel", waveLevel);
        broadcast(msg);
    }

    private boolean allEnemiesDead() {
        return enemies.isEmpty();
    }

    private void sendFullState() {
        GameState fullState = new GameState();
        fullState.setUpdateType(GameState.UpdateType.FULL_STATE);
        fullState.setWaveLevel(waveLevel);
        fullState.setTimestamp(System.currentTimeMillis());
        enemies.values()
                .forEach(
                        enemy -> fullState.getNewEnemies()
                        .add(new GameState.EnemyData(0,
                                                        enemy.getId(),
                                                        enemy.getX(),
                                                        enemy.getY())));
        towers.values().forEach(tower -> fullState.getTowersChanged().add(new GameState.TowerData(tower.getPlayerId(), tower, GameState.TowerData.Action.PLACED)));
        broadcastGameState(fullState);
    }

    public void handleGameAction(Message message) {
        int playerId = message.getSenderId();
        switch (message.getRequestType()) {
            case PLACE_TOWER:
                handleTowerPlaced(message);
                break;
            case REMOVE_TOWER:
                handleTowerRemoved(message);
                break;
        }
        broadcastExcept(playerId, message);
    }

    private void handleTowerPlaced(Message message) {
        GameState.TowerData towerData = (GameState.TowerData) message.getData("towerData");
        GameState.PlayerData playerD = currentState.getPlayers().get((int)message.getData("playerId"));
        Player player = new Player(playerD.playerName,playerD.money, TowerRepository.getTowerRepository());
        Tower template = TowerRepository.getTowerRepository().getTowerMarket().get(towerData.marketId);
        if (template != null && player.canAfford(template.getCost())) {
            try {
                Tower newTower = (Tower) template.clone();
                newTower.setId(towerData.towerId);
                newTower.setX(towerData.x);
                newTower.setY(towerData.y);
                newTower.setPlayerId(towerData.playerId);
                newTower.setInSlot(false);
                newTower.setCanAttack();
                newTower.setLastAttackTime(0);
                towers.put(newTower.getId(), newTower);
                playerD.money -= template.getCost();

                GameState.TowerData towerData1 = new GameState.TowerData(newTower.getPlayerId(),newTower, GameState.TowerData.Action.PLACED);
                GameState.PlayerData playerData = new GameState.PlayerData(playerD.playerId, playerD.money);
                pendingPlayerDataUpdate.add(playerData);
                pendingTowers.add(towerData1);
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleTowerRemoved(Message message) {
//        int towerId = (int) message.getData("towerId");
//        towers.remove(towerId);
//        GameState.TowerData removeData = new GameState.TowerData(0, null, GameState.TowerData.Action.REMOVED);
//        removeData.towerId = towerId;
//
//        GameState state = new GameState();
//        state.setUpdateType(GameState.UpdateType.TOWER_REMOVED);
//        state.setTowersChanged(List.of(removeData));
//        broadcastGameState(state);
    }

    private void handleTowerUpgraded(Message message) {
        GameState.TowerData towerData = (GameState.TowerData) message.getData("towerData");
        Tower tower = towers.get(towerData.towerId);
        if (tower != null) {
            tower.setLevel(towerData.level);


            GameState.TowerData removeData = new GameState.TowerData(0, tower, GameState.TowerData.Action.UPGRADED);
            pendingTowers.add(removeData);
        }
    }

    public void broadcastPlayer(Message message){
        players.forEach((id, handler) ->{
            if(id == message.getSenderId()) handler.sendMessage(message);
        });
    }

    public void broadcast(Message message) {
        players.values().forEach(handler -> handler.sendMessage(message));
    }

    public void broadcastExcept(int excludeId, Message message) {
        players.forEach((id, handler) -> {
            if (id != excludeId) handler.sendMessage(message);
        });
    }

    public void broadcastGameState(GameState state) {
        players.values().forEach(handler -> handler.sendGameState(state));
    }

    public String getRoomId() { return roomId; }
    public String getRoomName() { return roomName; }
    public boolean isGameStarted() { return gameStarted; }
    public boolean canJoin() { return players.size() < 4 && !gameStarted; }
    public boolean isHost(int playerId) { return playerId == hostId; }
    public int getPlayerCount() { return players.size(); }
    public Map<Integer, ClientHandler> getPlayers() { return players; }
}