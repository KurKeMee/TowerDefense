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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static rcpa.project.config.Configuration.*;
import static rcpa.project.config.Configuration.GameStatus.MAIN_MENU;

public class GameMaster implements GameClient.ClientListener {

    private GameClient gameClient;
    private boolean isMultiplayer = false;
    private int playerId = -1;
    private GameServer gameServer;

    private static GameMaster instance;
    private final EnemyRepository enemyRepository;
    private final MapUtils mapUtils;
    private final CellRepository cellRepository;
    private final AttackRepository attackRepository;

    private GameStatus gameStatus = MAIN_MENU;
    private String roomId;
    private int frameCount = 0;
    private int animationLevelLoadingStatus = 0;
    private int waveLevel = 0;

    private Player player;
    private boolean isDragTower = false;
    private Tower dragTower;
    private Tower towerToUpdate;

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
            mainMenu(g);
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

    private void mainMenu(Graphics g){
        if(roomId!=null) {
            g.setColor(Color.ORANGE);
            g.setFont(new Font("Gabriola", Font.BOLD, 50));
            FontMetrics fm = g.getFontMetrics();
            try {
                Image bg = ImageIO.read(new File(EXIT_BUTTON_IMAGE));
                g.drawImage(bg, 750, 0, 80+fm.stringWidth(roomId), 96, null);
            } catch (IOException e) {
                e.printStackTrace();
            }


            int y = (96 - fm.getHeight()) + fm.getAscent();
            g.drawString(roomId, 790, y + 16);
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
            g.drawString(String.valueOf((WAVE_CHANGE_COOLDOWN - getSecondsLeft())),
                    this.mapUtils.getWidth() / 2 - 20, this.mapUtils.getHeight() / 6);
            if (getSecondsLeft() >= WAVE_CHANGE_COOLDOWN) {
                frameCount = 100;
                gameStatus = GameStatus.WAVE_STARTED;
                attackRepository.clear();
            }
        } else {
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
                .forEach(t -> {
                    BufferedImage towerImage = t.rotateTower(enemyRepository.getEnemies());
                    g.drawImage(towerImage, t.getX(), t.getY(), null);

                    if (frameCount % 8 == 0 && (t.getIsAnimation() || t.canAttack())) {
                        t.playAnimation();
                    }
                });
    }

    private void enemiesMove(Graphics g) {
        enemyRepository.getEnemies().forEach(e -> {
            if (frameCount % 8 == 0) {
                e.playAnimation();
            }

            g.drawImage(e.getImage(), e.getX(), e.getY(), null);

            g.setColor(Color.red);
            g.fillRect(e.getX() + 14, e.getY() - 5,
                    (int) (e.getHealth() * 100 / e.getMaxHealth()), 5);
            g.setColor(Color.black);
            g.drawRect(e.getX() + 14, e.getY() - 5, 100, 5);

            g.setColor(Color.white);
            g.setFont(new Font("Arial", Font.BOLD, 10));
            g.drawString(e.getName(), e.getX() + 14, e.getY() + 20);
        });
    }

    private void attacksMove(Graphics g) {
        System.out.println(attackRepository.getAttacks().size());
        attackRepository.getAttacks().forEach(attack -> {
            attack.render(g);
        });
    }


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
            GameState.TowerData data = new GameState.TowerData(playerId, tower, GameState.TowerData.Action.PLACED);
            data.marketId = tower.getMarketId();
            data.x = tower.getX();
            data.y = tower.getY();
            gameClient.placeTower(data);
        }
    }


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
                serverGameStarted = true;
                waveLevel = (int) message.getData("waveLevel");

                SwingUtilities.invokeLater(() -> {
                    MapUtils.getMapUtils().analyzeMap(MAP_1);
                    CellRepository.getCellRepository().buildPath();
                });

                setGameStatus(GameStatus.LEVEL_ENTER);
                break;

            case PLACE_TOWER:
                GameState.TowerData towerData = (GameState.TowerData) message.getData("towerData");
                placeRemoteTower(towerData);
                break;

            case REMOVE_TOWER:
                int towerId = (int) message.getData("towerId");
                TowerRepository.getTowerRepository().deleteTower(towerId);
                break;

            case WAVE_START:
                waveLevel = (int) message.getData("waveLevel");
                attackRepository.clear();
                System.out.println("GameMaster: Началась волна " + waveLevel);
                break;

            case ROOM_CREATED:
                roomId = (String) message.getData("roomId");
                break;

            case ROOM_JOINED:
                roomId = (String) message.getData("roomId");
                System.out.println("Успешно подключились к комнате: " + message.getData("roomId"));
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
            case FULL_UPDATE:
                if(!gameState.getNewEnemies().isEmpty()) handleEnemySpawn(gameState);
                //if(!gameState.getTowersChanged().isEmpty()) han
                if(!gameState.getDeadEnemies().isEmpty()) handleEnemyDeath(gameState);
                if(!gameState.getEnemyMoves().isEmpty()) handleEnemyMoved(gameState);
                if(!gameState.getNewAttacks().isEmpty()) handleAttackCreated(gameState);
                if(!gameState.getAttackMoves().isEmpty()) handleAttackMove(gameState);
                break;
            case ENEMY_MOVED:
                handleEnemyMoved(gameState);
                break;
            case ATTACK_CREATED:
                handleAttackCreated(gameState);
                break;
        }
    }

    private void handleEnemyMoved(GameState state) {
        if (!enemyRepository.getEnemies().isEmpty()) {
            for (GameState.EnemyMoveData moveData : state.getEnemyMoves()) {
                Enemy enemy = enemyRepository.getEnemies()
                        .stream().filter(e->e.getId() == moveData.enemyId).findFirst().orElse(null);
                if (enemy != null) {
                    enemy.setX(moveData.x);
                    enemy.setY(moveData.y);
                    enemy.setHealth(moveData.health);
                    enemy.setLookOrientation(moveData.lookOrientation);
                }
            }
        }
    }

    private void handleEnemySpawn(GameState state) {
        for (GameState.EnemyData enemyData : state.getNewEnemies()) {
            try {
                Enemy template = enemyRepository.getEnemiesMarket().get(enemyData.marketId);
                if (template != null) {
                    Enemy enemy = (Enemy) template.clone();
                    enemy.setId(enemyData.enemyId);

                    ArrayList<rcpa.project.entity.base.Cell> path =
                            CellRepository.getCellRepository().buildPath();
                    enemy.setWay(path);

                    enemy.setX(enemyData.x);
                    enemy.setY(enemyData.y);

                    enemyRepository.addNewEnemy(enemy);
                }
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }

        if (!enemyRepository.getEnemies().isEmpty() && gameStatus == GameStatus.WAVE_STARTING) {
            gameStatus = GameStatus.WAVE_STARTED;
        }
    }

    private void handleEnemyDeath(GameState state) {
        for (int enemyId : state.getDeadEnemies()) {
            enemyRepository.deleteEnemy(enemyId);
        }
    }

    private void handleFullState(GameState state) {
        enemyRepository.getEnemies().clear();

        for (GameState.EnemyData enemyData : state.getNewEnemies()) {
            try {
                Enemy template = enemyRepository.getEnemiesMarket().get(enemyData.marketId);
                if (template != null) {
                    Enemy enemy = (Enemy) template.clone();
                    enemy.setId(enemyData.enemyId);
                    enemy.setWay(CellRepository.getCellRepository().buildPath());
                    enemy.setX(enemyData.x);
                    enemy.setY(enemyData.y);

                    enemyRepository.addNewEnemy(enemy);
                }
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        }

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

        if (!enemyRepository.getEnemies().isEmpty() && gameStatus == GameStatus.WAVE_STARTING) {
            gameStatus = GameStatus.WAVE_STARTED;
        }
    }

    private void handleAttackCreated(GameState state) {
        if(state.getNewAttacks().isEmpty() || enemyRepository.getEnemies().isEmpty()) return;
        for (GameState.AttackData attackData : state.getNewAttacks()) {
            Tower tower = player.getTowerRepository().getTowers()
                    .stream().filter(t->t.getId() == attackData.ownerId).findFirst().orElse(null);
            if (tower != null) {
                try {
                    Enemy target = enemyRepository.getEnemy(attackData.targetId);
                    tower.getAttack().setTarget(target);
                    Attack attack = (Attack) tower.getAttack().clone();
                    attack.setId(attackData.attackId);
                    switch (attack.getAttackType()) {
                        case SPIN_ATTACK:
                            ((SpinAttack) attack).setOwnTower(tower);
                            attack.setX(attackData.x);
                            attack.setY(attackData.y);
                            break;
                        case SHOOT_ATTACK:
                        case MELEE_ATTACK:
                            attack.setX(attackData.x + CELL_WIDTH / 2);
                            attack.setY(attackData.y + CELL_WIDTH / 2);
                            break;
                    }

                    attack.setImage(MapUtils.rotateImage(attack.getImage(),attackData.angle));
                    attack.setAngle(attackData.angle);
                    attackRepository.addNewAttack(attack);

                    tower.setAnimation(true);

                } catch (CloneNotSupportedException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void handleAttackMove(GameState state) {
        for (GameState.AttackMoveData attackData : state.getAttackMoves()) {
            if(attackData.completed) {
                attackRepository.deleteAttack(attackData.attackId);
            }
            else {
                Attack attack = attackRepository.getAttack(attackData.attackId);
                if(attack!=null) {
                    attack.setX(attackData.x);
                    attack.setY(attackData.y);
                    if (attack.getAttackType() == AttackType.SPIN_ATTACK)
                        ((SpinAttack) attack).setTimeSpent(attackData.timeSpent);
                }
            }
        }
    }

    private void placeRemoteTower(GameState.TowerData towerData) {
        Tower tower = TowerRepository.getTowerRepository().getTowerMarket().get(towerData.marketId);
        if (tower != null) {
            try {
                this.towerToUpdate = (Tower) tower.clone();
                this.towerToUpdate.setId(towerData.towerId);
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
        }
    }


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