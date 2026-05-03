package rcpa.project.network;

import rcpa.project.entity.attacks.SpinAttack;
import rcpa.project.entity.base.Attack;
import rcpa.project.entity.base.AttackType;
import rcpa.project.entity.base.Enemy;
import rcpa.project.entity.base.Tower;
import rcpa.project.model.GameState;
import rcpa.project.model.Message;
import rcpa.project.model.PlayerInfo;
import rcpa.project.repository.CellRepository;
import rcpa.project.repository.EnemyRepository;
import rcpa.project.repository.TowerRepository;
import rcpa.project.util.MapUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static rcpa.project.config.Configuration.CELL_WIDTH;

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

    private int enemyCountSpawned = 0;
    private int enemiesPerWave = 0;
    private int frameCount = 0;
    private boolean waveActive = false;
    private ScheduledExecutorService gameLoop;

    private int nextEnemyId = 0;
    private int nextAttackId = 0;
    private int nextTowerId = 100;

    private static boolean serverInitialized = false;

    private List<GameState.EnemyData> pendingEnemies = new ArrayList<>();
    private List<Integer> pendingDeadEnemies = new ArrayList<>();
    private List<GameState.TowerData> pendingTowers = new ArrayList<>();
    private List<GameState.AttackData> pendingAttacks = new ArrayList<>();

    // Счетчик вызовов update
    private int updateCount = 0;

    public GameRoom(String roomId, String roomName, int hostId, GameServer server) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.hostId = hostId;
        this.server = server;
        this.players = new ConcurrentHashMap<>();
        this.gameStarted = false;
        this.currentState = new GameState();
        this.waveLevel = 0;
        initializeServerData();
    }

    private static synchronized void initializeServerData() {
        if (serverInitialized) return;
        System.out.println("=== ИНИЦИАЛИЗАЦИЯ СЕРВЕРНЫХ ДАННЫХ ===");
        EnemyRepository.getEnemyRepository();
        TowerRepository.getTowerRepository();
        MapUtils.getMapUtils().analyzeMap(rcpa.project.config.Configuration.MAP_1);
        CellRepository.getCellRepository().buildPath();
        serverInitialized = true;
        System.out.println("=== ИНИЦИАЛИЗАЦИЯ ЗАВЕРШЕНА ===");
    }

    public void addPlayer(int playerId, ClientHandler handler) {
        players.put(playerId, handler);
        currentState.getPlayers().put(playerId, new GameState.PlayerData(playerId, handler.getPlayerName()));
    }

    public void removePlayer(int playerId) {
        players.remove(playerId);
        currentState.getPlayers().remove(playerId);
        if (playerId == hostId && !players.isEmpty()) {
            hostId = players.keySet().iterator().next();
        }
        if (players.isEmpty()) {
            server.removeRoom(roomId);
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
        broadcast(msg);
    }

    public void startGame() {
        if (players.size() >= 1 && !gameStarted) {
            System.out.println("=== ЗАПУСК ИГРЫ ===");
            gameStarted = true;
            waveLevel = 1;
            enemiesPerWave = 5;
            enemyCountSpawned = 0;
            waveActive = true;
            frameCount = 0;
            sendFullState();
            gameLoop = Executors.newSingleThreadScheduledExecutor();
            gameLoop.scheduleAtFixedRate(this::gameTick, 0, 50, TimeUnit.MILLISECONDS);
            Message message = new Message(Message.Type.GAME_STARTED);
            message.putData("waveLevel", waveLevel);
            broadcast(message);
            System.out.println("Игра началась! Волна " + waveLevel);
        }
    }

    private void gameTick() {
        if (!gameStarted || !waveActive) return;
        frameCount++;

        if (frameCount % 100 == 0 && enemyCountSpawned < enemiesPerWave) {
            spawnEnemy();
        }

        if (enemyCountSpawned >= enemiesPerWave && allEnemiesDead()) {
            startNextWave();
        } else {
            enemyMove();
            // Вызываем towerAttack только если есть и башни и враги
            if (!towers.isEmpty() && !enemies.isEmpty()) {
                towerAttack();
            }
            attackMove();
        }
    }

    private void spawnEnemy() {
        List<Enemy> market = EnemyRepository.getEnemyRepository().getEnemiesMarket();
        if (market.isEmpty()) return;
        Enemy template = market.get(0);
        if (template == null) return;
        try {
            Enemy enemy = (Enemy) template.clone();
            enemy.setId(nextEnemyId++);
            ArrayList<rcpa.project.entity.base.Cell> path = CellRepository.getCellRepository().buildPath();
            if (path == null || path.isEmpty()) return;
            enemy.setWay(path);
            int startX = enemy.getX();
            int startY = enemy.getY();
            enemies.put(enemy.getId(), enemy);
            enemyCountSpawned++;
            System.out.println("Сервер: Враг создан! ID=" + enemy.getId() + " X=" + startX + " Y=" + startY);
            pendingEnemies.add(new GameState.EnemyData(0, enemy.getId(), startX, startY));
        } catch (CloneNotSupportedException e) {
            e.printStackTrace();
        }
    }

    public void update() {
        if (!gameStarted) {
            System.out.println("Сервер update: игра не запущена");
            return;
        }

        updateCount++;

        GameState state = new GameState();
        state.setUpdateType(GameState.UpdateType.DELTA_UPDATE);
        state.setWaveLevel(waveLevel);
        state.setTimestamp(System.currentTimeMillis());

        if (!pendingEnemies.isEmpty()) {
            state.getNewEnemies().addAll(pendingEnemies);
            System.out.println("Сервер update #" + updateCount + ": " + pendingEnemies.size() + " новых врагов");
            pendingEnemies.clear();
        }
        if (!pendingDeadEnemies.isEmpty()) {
            state.getDeadEnemies().addAll(pendingDeadEnemies);
            pendingDeadEnemies.clear();
        }
        if (!pendingTowers.isEmpty()) {
            state.getTowersChanged().addAll(pendingTowers);
            System.out.println("Сервер update #" + updateCount + ": " + pendingTowers.size() + " башен");
            pendingTowers.clear();
        }
        if (!pendingAttacks.isEmpty()) {
            state.getNewAttacks().addAll(pendingAttacks);
            System.out.println("Сервер update #" + updateCount + ": " + pendingAttacks.size() + " атак");
            pendingAttacks.clear();
        }

        // ВСЕГДА добавляем позиции врагов
        List<GameState.EnemyMoveData> moves = new ArrayList<>();
        for (Enemy enemy : enemies.values()) {
            moves.add(new GameState.EnemyMoveData(enemy.getId(), enemy.getX(), enemy.getY(), enemy.getHealth(), enemy.getLookOrientation()));
        }
        state.setEnemyMoves(moves);

        // Логируем каждые 20 вызовов
        if (updateCount % 20 == 0) {
            System.out.println("Сервер update #" + updateCount + ": отправка позиций " + moves.size() + " врагов, " + towers.size() + " башен");
            if (!moves.isEmpty()) {
                Enemy first = enemies.values().iterator().next();
                System.out.println("  Первый враг: ID=" + first.getId() + " X=" + first.getX() + " Y=" + first.getY());
            }
        }

        // ВСЕГДА отправляем
        broadcastGameState(state);
    }

    private void enemyMove() {
        enemies.values().forEach(Enemy::move);
    }

    private void towerAttack() {
        long currentTime = System.currentTimeMillis();

        for (Tower tower : towers.values()) {
            long lastAttack = (long) tower.getLastAttackTime();
            long cooldownMs = (long) (tower.getAttackCooldown() * 1000);

            if (currentTime - lastAttack < cooldownMs) {
                continue;
            }

            // Ищем цель
            Enemy target = findTargetForTower(tower);

            if (target != null && target.getHealth() > 0) {
                System.out.println("Сервер: БАШНЯ " + tower.getId() + " АТАКУЕТ ВРАГА " + target.getId() + "! Урон: " + tower.getDamage());

                try {
                    Attack attack = (Attack) tower.getAttack().clone();
                    attack.setId(nextAttackId++);
                    attack.setTarget(target);

                    switch (attack.getAttackType()) {
                        case SPIN_ATTACK:
                            ((SpinAttack) attack).setOwnTower(tower);
                            attack.setX(tower.getX());
                            attack.setY(tower.getY());
                            break;
                        default:
                            attack.setX(tower.getX() + CELL_WIDTH / 2);
                            attack.setY(tower.getY() + CELL_WIDTH / 2);
                            break;
                    }

                    attacks.put(attack.getId(), attack);
                    tower.setLastAttackTime(currentTime);
                    tower.setAnimation(true);

                    pendingAttacks.add(new GameState.AttackData(attack.getId(), tower));

                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private Enemy findTargetForTower(Tower tower) {
        final int tx = tower.getX() + CELL_WIDTH / 2;
        final int ty = tower.getY() + CELL_WIDTH / 2;
        final double radius = tower.getRadius();

        for (Enemy enemy : enemies.values()) {
            if (enemy.getHealth() <= 0) continue;
            double dx = (enemy.getX() + CELL_WIDTH / 2.0) - tx;
            double dy = (enemy.getY() + CELL_WIDTH / 2.0) - ty;
            double dist = Math.sqrt(dx * dx + dy * dy);
            if (dist <= radius) {
                return enemy;
            }
        }
        return null;
    }

    private void attackMove() {
        List<Integer> toRemove = new ArrayList<>();
        for (Attack attack : attacks.values()) {
            if (!attack.move()) {
                Enemy target = attack.getTarget();
                if (target != null && target.getHealth() > 0) {
                    target.takeDamage(attack.getDamage());
                    System.out.println("Сервер: Атака " + attack.getId() + " нанесла " + attack.getDamage() + " урона врагу " + target.getId() + ". HP: " + String.format("%.1f", target.getHealth()));
                    if (target.getHealth() <= 0) {
                        enemies.remove(target.getId());
                        pendingDeadEnemies.add(target.getId());
                        System.out.println("Сервер: ВРАГ " + target.getId() + " УБИТ!");
                    }
                }
                toRemove.add(attack.getId());
            }
        }
        toRemove.forEach(attacks::remove);
    }

    private void startNextWave() {
        waveLevel++;
        enemiesPerWave += 2;
        enemyCountSpawned = 0;
        frameCount = 0;
        towers.values().forEach(t -> {
            t.setLastAttackTime(0);
            t.setCanAttack();
        });
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
        enemies.values().forEach(enemy -> fullState.getNewEnemies().add(new GameState.EnemyData(0, enemy.getId(), enemy.getX(), enemy.getY())));
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

        System.out.println("=== УСТАНОВКА БАШНИ ===");
        System.out.println("Сервер: marketId=" + towerData.towerId + " pos=" + towerData.x + "," + towerData.y);

        List<Tower> market = TowerRepository.getTowerRepository().getTowerMarket();
        if (towerData.towerId < 0 || towerData.towerId >= market.size()) {
            System.err.println("Сервер: ОШИБКА - неверный marketId: " + towerData.towerId);
            return;
        }

        Tower template = market.get(towerData.towerId);
        if (template != null) {
            try {
                Tower newTower = (Tower) template.clone();
                int uniqueId = nextTowerId++;
                newTower.setId(uniqueId);
                newTower.setX(towerData.x);
                newTower.setY(towerData.y);
                newTower.setPlayerId(towerData.playerId);
                newTower.setInSlot(false);
                newTower.setCanAttack();
                newTower.setLastAttackTime(0);

                towers.put(uniqueId, newTower);

                towerData.towerId = uniqueId;
                pendingTowers.add(towerData);

                System.out.println("Сервер: Башня установлена! uniqueId=" + uniqueId + " имя=" + newTower.getName());
                System.out.println("Сервер: Позиция=" + newTower.getX() + "," + newTower.getY() + " радиус=" + newTower.getRadius() + " урон=" + newTower.getDamage());
                System.out.println("Сервер: Всего башен=" + towers.size() + " врагов=" + enemies.size());

            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }
    }

    private void handleTowerRemoved(Message message) {
        int towerId = (int) message.getData("towerId");
        towers.remove(towerId);
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
        for (ClientHandler handler : players.values()) {
            handler.sendGameState(state);
        }
    }

    public void stopGameLoop() {
        if (gameLoop != null && !gameLoop.isShutdown()) {
            gameLoop.shutdown();
            try { gameLoop.awaitTermination(1, TimeUnit.SECONDS); } catch (InterruptedException e) { gameLoop.shutdownNow(); }
        }
    }

    public String getRoomId() { return roomId; }
    public String getRoomName() { return roomName; }
    public boolean isGameStarted() { return gameStarted; }
    public boolean canJoin() { return players.size() < 4 && !gameStarted; }
    public boolean isHost(int playerId) { return playerId == hostId; }
    public int getPlayerCount() { return players.size(); }
    public Map<Integer, ClientHandler> getPlayers() { return players; }
}