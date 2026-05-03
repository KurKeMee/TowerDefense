package rcpa.project.service;

import rcpa.project.entity.attacks.SpinAttack;
import rcpa.project.entity.base.*;
import rcpa.project.model.GameState;
import rcpa.project.model.Message;
import rcpa.project.network.GameClient;
import rcpa.project.network.GameServer;
import rcpa.project.repository.AttackRepository;
import rcpa.project.repository.CellRepository;
import rcpa.project.repository.EnemyRepository;
import rcpa.project.repository.TowerRepository;
import rcpa.project.util.MapUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static rcpa.project.config.Configuration.*;
import static rcpa.project.config.Configuration.GameStatus.MAIN_MENU;

public class GameMaster implements GameClient.ClientListener {

    private GameClient gameClient;
    private boolean isMultiplayer = false;
    private boolean isHost = false;
    private int playerId = -1;
    private GameServer gameServer;

    // ВАЖНО: Единое хранилище для отрисовки
    private final Map<Integer, Enemy> enemiesToRender = new ConcurrentHashMap<>();
    private final Map<Integer, Attack> attacksToRender = new ConcurrentHashMap<>();

    private static GameMaster instance;
    private final EnemyRepository enemyRepository;
    private final MapUtils mapUtils;
    private final CellRepository cellRepository;
    private final AttackRepository attackRepository;

    private GameStatus gameStatus = MAIN_MENU;
    private int frameCount = 0;
    private int animationLevelLoadingStatus = 0;
    private int waveLevel = 0;

    private Player player;
    private boolean isDragTower = false;
    private Tower dragTower;
    private Tower towerToUpdate;

    // Флаг для отслеживания, что игра запущена сервером
    private boolean serverGameStarted = false;

    private GameMaster() {
        this.enemyRepository = EnemyRepository.getEnemyRepository();
        this.cellRepository = CellRepository.getCellRepository();
        this.attackRepository = AttackRepository.getAttackRepository();
        this.mapUtils = MapUtils.getMapUtils();
        this.player = new Player("f", 200, TowerRepository.getTowerRepository());
    }

    public static synchronized GameMaster getGameMaster() {
        if (instance == null) {
            instance = new GameMaster();
        }
        return instance;
    }

    public void renderFrame(Graphics g) {
        if (gameStatus != MAIN_MENU) frameCount++;

        if (isDragTower) {
            onDragTowerEvent(g);
        }

        if (gameStatus == MAIN_MENU) {
            // Главное меню
        } else if (gameStatus == GameStatus.MAIN_MENU_LEVEL) {
            mainMenuLevelState(g);
        } else if (gameStatus == GameStatus.LEVEL_ENTER) {
            gameStatus = GameStatus.WAVE_STARTING;
        } else if (gameStatus == GameStatus.WAVE_STARTING) {
            towersAttack(g);
            waveStarting(g);
        } else if (gameStatus == GameStatus.WAVE_STARTED) {
            towersAttack(g);
            waveStarted(g);
        }
    }

    private void mainMenuLevelState(Graphics g) {
        try {
            animationLevelLoadingStatus += 15;
            BufferedImage bufImg = ImageIO.read(new File(HONEY_OVERGROUND_IMAGE));
            g.drawImage(bufImg.getScaledInstance(bufImg.getWidth(),
                            (int) (bufImg.getHeight() * 1.2),
                            Image.SCALE_AREA_AVERAGING),
                    0, (int) (bufImg.getHeight() * (-1.2) + animationLevelLoadingStatus), null);

            if (bufImg.getHeight() * (-1.2) + animationLevelLoadingStatus >= 0) {
                gameStatus = GameStatus.LEVEL_ENTER;
                animationLevelLoadingStatus = 0;
                frameCount = 0;
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void waveStarting(Graphics g) {
        g.setFont(new Font("Arial", Font.PLAIN, 50));
        g.setColor(Color.WHITE);

        if (isMultiplayer && serverGameStarted) {
            // В мультиплеере ждем врагов с сервера
            g.drawString("Ожидание врагов...", this.mapUtils.getWidth() / 2 - 100, this.mapUtils.getHeight() / 6);
            g.drawString("Врагов на карте: " + enemiesToRender.size(), this.mapUtils.getWidth() / 2 - 100, this.mapUtils.getHeight() / 6 + 60);

            // Переключаем на WAVE_STARTED если есть враги или прошло время
            if (!enemiesToRender.isEmpty() || getSecondsLeft() >= WAVE_CHANGE_COOLDOWN) {
                frameCount = 100;
                gameStatus = GameStatus.WAVE_STARTED;
            }
        } else {
            // Одиночная игра - старая логика
            g.drawString(String.valueOf((WAVE_CHANGE_COOLDOWN - getSecondsLeft())),
                    this.mapUtils.getWidth() / 2 - 20, this.mapUtils.getHeight() / 6);
            if (getSecondsLeft() >= WAVE_CHANGE_COOLDOWN) {
                frameCount = 100;
                gameStatus = GameStatus.WAVE_STARTED;
                waveLevel++;
            }
        }
    }

    private void waveStarted(Graphics g) {
        g.setFont(new Font("Arial", Font.PLAIN, 50));
        g.setColor(Color.WHITE);
        g.drawString("Wave " + waveLevel, this.mapUtils.getWidth() / 2 - 30, this.mapUtils.getHeight() / 6);
        g.drawString("Врагов: " + enemiesToRender.size(), this.mapUtils.getWidth() / 2 - 60, this.mapUtils.getHeight() / 6 + 60);

        enemiesMove(g);
        attacksMove(g);
    }

    private void onDragTowerEvent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;
        AlphaComposite alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f);
        g2d.setComposite(alphaComposite);

        if (dragTower.isCanOccupe()) {
            g2d.setColor(Color.GREEN);
        } else {
            g2d.setColor(Color.RED);
        }

        g2d.fillOval((int) (dragTower.getX() + CELL_WIDTH / 2 - dragTower.getRadius()),
                (int) (dragTower.getY() + CELL_WIDTH / 2 - dragTower.getRadius()),
                (int) dragTower.getRadius() * 2,
                (int) dragTower.getRadius() * 2);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        g2d.drawOval((int) (dragTower.getX() + CELL_WIDTH / 2 - dragTower.getRadius()),
                (int) (dragTower.getY() + CELL_WIDTH / 2 - dragTower.getRadius()),
                (int) dragTower.getRadius() * 2,
                (int) dragTower.getRadius() * 2);
        g2d.drawImage(dragTower.getImage(), dragTower.getX(), dragTower.getY(), null);
    }

    private void towersAttack(Graphics g) {
        TowerRepository.getTowerRepository().getTowers().stream()
                .filter(t -> !t.isInSlot())
                .forEach(t -> {
                    BufferedImage towerImage = t.rotateTower();
                    g.drawImage(towerImage, t.getX(), t.getY(), null);

                    if (frameCount % 8 == 0 && (t.getIsAnimation() || t.canAttack())) {
                        t.playAnimation();
                    }
                });
    }

    private void enemiesMove(Graphics g) {
        // Отладочный вывод КАЖДЫЕ 60 кадров (раз в ~3 секунды)
        if (frameCount % 60 == 0) {
            System.out.println("=== ОТРИСОВКА ВРАГОВ (кадр " + frameCount + ") ===");
            System.out.println("Всего врагов: " + enemiesToRender.size());

            enemiesToRender.values().stream()
                    .sorted((a, b) -> Integer.compare(a.getId(), b.getId()))
                    .forEach(e -> {
                        System.out.println("  Враг " + e.getId() +
                                ": X=" + e.getX() + " Y=" + e.getY() +
                                " HP=" + String.format("%.1f", e.getHealth()) +
                                " Ячейка: " + (e.getX()/CELL_WIDTH) + "," + (e.getY()/CELL_WIDTH) +
                                " Ориентация: " + e.getLookOrientation());
                    });
            System.out.println("===========================================");
        }

        // Отрисовка врагов
        enemiesToRender.values().forEach(e -> {
            // Анимация
            if (frameCount % 8 == 0) {
                e.playAnimation();
            }

            // Рисуем врага
            g.drawImage(e.getImage(), e.getX(), e.getY(), null);

            // Полоска здоровья
            g.setColor(Color.red);
            g.fillRect(e.getX() + 14, e.getY() - 5,
                    (int)(e.getHealth() * 100 / e.getMaxHealth()), 5);
            g.setColor(Color.black);
            g.drawRect(e.getX() + 14, e.getY() - 5, 100, 5);

            // ID врага для отладки
            g.setColor(Color.white);
            g.setFont(new Font("Arial", Font.BOLD, 10));
            g.drawString("ID:" + e.getId(), e.getX() + 14, e.getY() + 20);
        });
    }

    private void attacksMove(Graphics g) {
        // Отрисовка атак
        attacksToRender.values().forEach(a -> {
            a.render(g);
            // Отладка
            if (frameCount % 60 == 0) {
                System.out.println("GameMaster: Отрисовка атаки " + a.getId() +
                        " на позиции " + a.getX() + "," + a.getY());
            }
        });
    }

    // ============ СЕТЕВЫЕ МЕТОДЫ ============

    public String createMultiplayerRoom() {
        if (!isMultiplayer) {
            connectToServer("127.0.0.1", 8080);
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
            }
        }

        String roomName = "Game_" + System.currentTimeMillis() % 10000;
        gameClient.sendRequest(Message.Type.CREATE_ROOM, Map.of("roomName", roomName));
        isHost = true;
        return roomName;
    }

    public void startServer() {
        if (gameServer != null) return;

        new Thread(() -> {
            gameServer = new GameServer();
            gameServer.start();
        }).start();

        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public boolean connectToServer(String host, int port) {
        gameClient = new GameClient();
        gameClient.setListener(this);
        boolean connected = gameClient.connect(host, port);

        if (connected) {
            isMultiplayer = true;
        }
        return connected;
    }

    public void sendTowerPlaced(Tower tower) {
        if (isMultiplayer && gameClient != null) {
            // ВАЖНО: Используем ID из market, а не сгенерированный clone()
            // Найдите оригинальный индекс в market
            int marketId = findTowerMarketId(tower);

            System.out.println("GameMaster: Отправка башни. marketId=" + marketId +
                    " tower.getId()=" + tower.getId());

            GameState.TowerData data = new GameState.TowerData(playerId, tower, GameState.TowerData.Action.PLACED);
            data.towerId = marketId; // Используем market ID
            data.x = tower.getX();
            data.y = tower.getY();
            gameClient.placeTower(data);
        }
    }

    // Новый метод для поиска ID в market
    private int findTowerMarketId(Tower tower) {
        ArrayList<Tower> market = TowerRepository.getTowerRepository().getTowerMarket();
        for (int i = 0; i < market.size(); i++) {
            if (market.get(i).getName().equals(tower.getName())) {
                return i;
            }
        }
        return 0;
    }

    // ============ ОБРАБОТЧИКИ СООБЩЕНИЙ ============

    @Override
    public void onConnected(int playerId) {
        this.playerId = playerId;
        System.out.println("Подключен как игрок " + playerId);
    }

    @Override
    public void onDisconnected() {
        isMultiplayer = false;
        System.out.println("Отключен от сервера");
    }

    @Override
    public void onMessageReceived(Message message) {
        System.out.println("GameMaster: Получено сообщение " + message.getRequestType());

        switch (message.getRequestType()) {
            case GAME_STARTED:
                System.out.println("GameMaster: Игра началась! Волна: " + message.getData("waveLevel"));
                serverGameStarted = true;
                waveLevel = (int) message.getData("waveLevel");

                // Загружаем карту
                SwingUtilities.invokeLater(() -> {
                    MapUtils.getMapUtils().analyzeMap(MAP_1);
                    CellRepository.getCellRepository().buildPath();
                    System.out.println("GameMaster: Карта загружена на клиенте");
                });

                setGameStatus(GameStatus.LEVEL_ENTER);
                break;

            case PLACE_TOWER:
                GameState.TowerData towerData = (GameState.TowerData) message.getData("towerData");
                // placeRemoteTower(towerData);
                break;

            case REMOVE_TOWER:
                int towerId = (int) message.getData("towerId");
                TowerRepository.getTowerRepository().deleteTower(towerId);
                break;

            case WAVE_START:
                waveLevel = (int) message.getData("waveLevel");
                System.out.println("GameMaster: Началась волна " + waveLevel);
                break;

            case ROOM_JOINED:
                System.out.println("Успешно подключились к комнате: " + message.getData("roomId"));
                JOptionPane.showMessageDialog(null,
                        "Подключены к комнате!\nОжидайте начала игры.");
                break;

            case ERROR:
                System.err.println("Ошибка: " + message.getErrorMessage());
                JOptionPane.showMessageDialog(null,
                        "Ошибка: " + message.getErrorMessage(),
                        "Ошибка", JOptionPane.ERROR_MESSAGE);
                break;
        }
    }

    @Override
    public void onGameStateReceived(GameState gameState) {
        System.out.println("GameMaster: Получено состояние игры: " + gameState.getUpdateType());

        switch (gameState.getUpdateType()) {
            case ENEMY_SPAWN:
                handleEnemySpawn(gameState);
                break;
            case ENEMY_DEATH:
                handleEnemyDeath(gameState);
                break;
            case FULL_STATE:
                handleFullState(gameState);
                break;
            case ENEMY_MOVED:
                handleEnemyMoved(gameState);
                break;
            case DELTA_UPDATE:
                handleDeltaUpdate(gameState);
                break;
            case ATTACK_CREATED:
                handleAttackCreated(gameState);
                break;
        }
    }

    private void handleEnemyMoved(GameState state) {
        if (state.getEnemyMoves() != null) {
            for (GameState.EnemyMoveData moveData : state.getEnemyMoves()) {
                Enemy enemy = enemiesToRender.get(moveData.enemyId);
                if (enemy != null) {
                    enemy.setX(moveData.x);
                    enemy.setY(moveData.y);
                    enemy.setLookOrientation(moveData.lookOrientation);
                }
            }
        }
    }

    private void handleEnemySpawn(GameState state) {
        System.out.println("GameMaster: Спавн " + state.getNewEnemies().size() + " врагов");

        for (GameState.EnemyData enemyData : state.getNewEnemies()) {
            // Проверяем, нет ли уже такого врага
            if (enemiesToRender.containsKey(enemyData.enemyId)) {
                System.out.println("GameMaster: Враг " + enemyData.enemyId + " уже существует");
                continue;
            }

            try {
                Enemy template = enemyRepository.getEnemiesMarket().get(enemyData.enemyMarketId);
                if (template != null) {
                    Enemy enemy = (Enemy) template.clone();
                    enemy.setId(enemyData.enemyId);

                    // Строим путь
                    ArrayList<rcpa.project.entity.base.Cell> path =
                            CellRepository.getCellRepository().buildPath();
                    enemy.setWay(path);

                    // Устанавливаем начальную позицию
                    enemy.setX(enemyData.x);
                    enemy.setY(enemyData.y);

                    enemiesToRender.put(enemy.getId(), enemy);
                    enemyRepository.addNewEnemy(enemy);

                    System.out.println("GameMaster: Враг создан ID=" + enemy.getId() +
                            " на позиции " + enemy.getX() + "," + enemy.getY());
                }
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }

        // Переключаем статус, если есть враги
        if (!enemiesToRender.isEmpty() && gameStatus == GameStatus.WAVE_STARTING) {
            gameStatus = GameStatus.WAVE_STARTED;
        }
    }

    private void handleEnemyDeath(GameState state) {
        System.out.println("GameMaster: Смерть " + state.getDeadEnemies().size() + " врагов");
        for (int enemyId : state.getDeadEnemies()) {
            enemiesToRender.remove(enemyId);
            enemyRepository.deleteEnemy(enemyId);
            System.out.println("GameMaster: Враг удален ID=" + enemyId);
        }
    }

    private void handleFullState(GameState state) {
        System.out.println("GameMaster: Получено полное состояние");

        // Очищаем текущее состояние
        enemiesToRender.clear();
        enemyRepository.getEnemies().clear();

        // Загружаем врагов
        for (GameState.EnemyData enemyData : state.getNewEnemies()) {
            try {
                Enemy template = enemyRepository.getEnemiesMarket().get(enemyData.enemyMarketId);
                if (template != null) {
                    Enemy enemy = (Enemy) template.clone();
                    enemy.setId(enemyData.enemyId);
                    enemy.setWay(CellRepository.getCellRepository().buildPath());
                    enemy.setX(enemyData.x);
                    enemy.setY(enemyData.y);

                    enemiesToRender.put(enemy.getId(), enemy);
                    enemyRepository.addNewEnemy(enemy);
                }
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }

        // Загружаем башни
        for (GameState.TowerData towerData : state.getTowersChanged()) {
            if (towerData.action == GameState.TowerData.Action.PLACED) {
                Tower template = TowerRepository.getTowerRepository().getTowerMarket().get(towerData.towerId);
                if (template != null) {
                    try {
                        Tower tower = (Tower) template.clone();
                        tower.setId(towerData.towerId);
                        tower.setX(towerData.x);
                        tower.setY(towerData.y);
                        tower.setPlayerId(towerData.playerId);
                        tower.setInSlot(false);
                        tower.setCanAttack();
                        TowerRepository.getTowerRepository().addNewTower(tower);
                    } catch (CloneNotSupportedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }

        System.out.println("GameMaster: Загружено врагов: " + enemiesToRender.size() +
                ", башен: " + TowerRepository.getTowerRepository().getTowers().size());

        // Если есть враги, переключаем статус
        if (!enemiesToRender.isEmpty() && gameStatus == GameStatus.WAVE_STARTING) {
            gameStatus = GameStatus.WAVE_STARTED;
        }
    }

    private void handleDeltaUpdate(GameState state) {
        // 1. Обновляем позиции врагов
        if (state.getEnemyMoves() != null && !state.getEnemyMoves().isEmpty()) {
            for (GameState.EnemyMoveData moveData : state.getEnemyMoves()) {
                Enemy enemy = enemiesToRender.get(moveData.enemyId);
                if (enemy != null) {
                    enemy.setX(moveData.x);
                    enemy.setY(moveData.y);
                    enemy.setLookOrientation(moveData.lookOrientation);
                    // Remove damage application from here
                }
            }
        }

        // 2. Обновляем позиции атак
        if (state.getAttackMoves() != null && !state.getAttackMoves().isEmpty()) {
            for (GameState.AttackMoveData moveData : state.getAttackMoves()) {
                Attack attack = attacksToRender.get(moveData.attackId);
                if (attack != null) {
                    attack.setX(moveData.x);
                    attack.setY(moveData.y);
                    if (moveData.completed) {
                        attacksToRender.remove(moveData.attackId);
                    }
                }
            }
        }

        // 3. Добавляем новых врагов
        for (GameState.EnemyData enemyData : state.getNewEnemies()) {
            if (enemiesToRender.containsKey(enemyData.enemyId)) {
                continue;
            }

            try {
                Enemy template = enemyRepository.getEnemiesMarket().get(enemyData.enemyMarketId);
                if (template != null) {
                    Enemy enemy = (Enemy) template.clone();
                    enemy.setId(enemyData.enemyId);

                    ArrayList<rcpa.project.entity.base.Cell> path =
                            CellRepository.getCellRepository().buildPath();
                    enemy.setWay(path);

                    enemy.setX(enemyData.x);
                    enemy.setY(enemyData.y);

                    enemiesToRender.put(enemy.getId(), enemy);
                }
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }

        // 4. Удаляем мертвых врагов
        for (int enemyId : state.getDeadEnemies()) {
            enemiesToRender.remove(enemyId);
        }

        // 5. Обрабатываем новые атаки
        for (GameState.AttackData attackData : state.getNewAttacks()) {
            Tower tower = TowerRepository.getTowerRepository().getTower(attackData.ownerId);
            if (tower != null) {
                try {
                    Attack attack = (Attack) tower.getAttack().clone();
                    attack.setId(attackData.attackId);
                    attack.setX(attackData.x);
                    attack.setY(attackData.y);

                    Enemy target = enemiesToRender.get(attackData.targetId);
                    attack.setTarget(target);

                    attacksToRender.put(attack.getId(), attack);
                    tower.setAnimation(true);
                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }
            }
        }

        for (GameState.TowerData towerData : state.getTowersChanged()) {
            switch (towerData.action) {
                case PLACED:
                    // Проверяем, есть ли уже башня с таким ID
                    if (TowerRepository.getTowerRepository().getTower(towerData.towerId) != null) {
                        System.out.println("GameMaster: Башня " + towerData.towerId + " уже существует");
                        break;
                    }

                    // Ищем ЛЮБОЙ шаблон башни (используем marketId если есть, иначе 0)
                    int marketId = 0;
                    if (towerData.towerId >= 0 && towerData.towerId < TowerRepository.getTowerRepository().getTowerMarket().size()) {
                        marketId = towerData.towerId;
                    }

                    Tower template = TowerRepository.getTowerRepository().getTowerMarket().get(marketId);
                    if (template != null) {
                        try {
                            Tower tower = (Tower) template.clone();
                            tower.setId(towerData.towerId); // Используем серверный ID
                            tower.setX(towerData.x);
                            tower.setY(towerData.y);
                            tower.setPlayerId(towerData.playerId);
                            tower.setInSlot(false);
                            tower.setCanAttack();
                            TowerRepository.getTowerRepository().addNewTower(tower);
                            System.out.println("GameMaster: Башня добавлена! ID=" + towerData.towerId + " marketId=" + marketId);
                        } catch (CloneNotSupportedException e) {
                            e.printStackTrace();
                        }
                    } else {
                        System.out.println("GameMaster: Шаблон башни не найден для marketId=" + marketId);
                    }
                    break;
                case REMOVED:
                    TowerRepository.getTowerRepository().deleteTower(towerData.towerId);
                    break;
            }
        }
    }

    private void handleAttackCreated(GameState state) {
        System.out.println("GameMaster: Получено " + state.getNewAttacks().size() + " атак");

        for (GameState.AttackData attackData : state.getNewAttacks()) {
            Tower tower = TowerRepository.getTowerRepository().getTower(attackData.ownerId);
            if (tower != null) {
                try {
                    Attack attack = (Attack) tower.getAttack().clone();
                    attack.setId(attackData.attackId);
                    attack.setX(attackData.x);
                    attack.setY(attackData.y);

                    // Находим цель среди отрисовываемых врагов
                    Enemy target = enemiesToRender.get(attackData.targetId);
                    attack.setTarget(target);

                    attacksToRender.put(attack.getId(), attack);

                    // Запускаем анимацию башни
                    tower.setAnimation(true);

                    System.out.println("GameMaster: Атака создана ID=" + attack.getId() +
                            " от башни " + attackData.ownerId +
                            " на врага " + attackData.targetId);

                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("GameMaster: Башня " + attackData.ownerId + " не найдена для атаки");
            }
        }
    }

    private void placeRemoteTower(GameState.TowerData towerData) {
        Tower tower = TowerRepository.getTowerRepository().getTowerMarket().get(towerData.towerId);
        if (tower != null) {
            try {
                this.towerToUpdate = (Tower) tower.clone();
                this.towerToUpdate.setX(towerData.x);
                this.towerToUpdate.setY(towerData.y);
                this.towerToUpdate.setInSlot(false);
                this.towerToUpdate.setCanAttack();
                this.towerToUpdate.setBounds(towerData.x, towerData.y, CELL_WIDTH, CELL_WIDTH);

                TowerRepository.getTowerRepository().addNewTower(this.towerToUpdate);

                Cell cell = CellRepository.getCellRepository().getCell(
                        towerData.x / CELL_WIDTH,
                        towerData.y / CELL_WIDTH
                );
                if (cell != null) {
                    cell.occupeCell();
                }
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        } else {
            System.out.println("ERROR: Tower template not found for id " + towerData.towerId);
        }
    }

    // ============ ГЕТТЕРЫ И СЕТТЕРЫ ============

    public int getSecondsLeft() {
        return (frameCount * MILLISECONDS_PER_FRAME / 2) / 1000;
    }

    public Tower getDragTower() { return this.dragTower; }
    public void setDragTower(Tower dragTower) { this.dragTower = dragTower; }
    public boolean isDragTower() { return isDragTower; }
    public void setIsDragTower(boolean isDragTower) { this.isDragTower = isDragTower; }
    public Player getPlayer() { return this.player; }
    public void setGameStatus(GameStatus gameStatus) { this.gameStatus = gameStatus; }
    public GameStatus getGameStatus() { return this.gameStatus; }
    public boolean isMultiplayer() { return isMultiplayer; }
    public int getPlayerId() { return playerId; }
    public GameClient getGameClient() { return gameClient; }
    public Tower getTowerToUpdate() { return towerToUpdate; }
    public void clearTowerToUpdate() { this.towerToUpdate = null; }
}