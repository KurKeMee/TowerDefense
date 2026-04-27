package rcpa.project.service;

import rcpa.project.entity.attacks.SpinAttack;
import rcpa.project.entity.base.*;
import rcpa.project.repository.AttackRepository;
import rcpa.project.repository.CellRepository;
import rcpa.project.repository.EnemyRepository;
import rcpa.project.repository.TowerRepository;
import rcpa.project.util.MapUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static rcpa.project.config.Configuration.*;
import static rcpa.project.config.Configuration.GameStatus.MAIN_MENU;

/**
 * @author Ivan Monin
 * <p>
 * Класс для рендеринга окна
 */
public class GameMaster {

    /**
     * Переменная для хранения экземпляра класса GameMaster
     */
    private static GameMaster instance;

    /**
     * Переменная для хранения экземпляра класса EnemyRepository
     *
     * @see EnemyRepository
     */
    private final EnemyRepository enemyRepository;

    /**
     * Переменная для хранения экземпляра класса MapUtils
     *
     * @see MapUtils
     */
    private final MapUtils mapUtils;


    /**
     * Переменная для хранения экземпляра класса CellRepository
     *
     * @see CellRepository
     */
    private final CellRepository cellRepository;


    /**
     * Переменная для хранения экземпляра класса AttackRepository
     *
     * @see AttackRepository
     */
    private final AttackRepository attackRepository;

    /**
     * Статус игры
     */
    private GameStatus gameStatus = MAIN_MENU;

    /**
     * Счетчик кадров
     */
    private int frameCount = 0;
    private int animationLevelLoadingStatus = 0;

    /**
     * Переменная текущей волны
     */
    private int waveLevel = 0;


    /**
     * Переменная количества врагов
     */
    private int enemyCount = 1;


    /**
     * Переменная количества призванных врагов
     */
    private int enemyCountSpawned = 0;

    private Player player;
    private boolean isDragTower = false;
    private Tower dragTower;

    /**
     * Конструктор объявлен как private для паттерна Singleton
     *
     * @see GameMaster#getGameMaster()
     */
    private GameMaster() {
        this.enemyRepository = EnemyRepository.getEnemyRepository();
        this.cellRepository = CellRepository.getCellRepository();
        this.attackRepository = AttackRepository.getAttackRepository();
        this.mapUtils = MapUtils.getMapUtils();
        this.player = new Player("f",200,new TowerRepository());
    }

    /**
     * Метод для получения единственного экземпляра класса
     *
     * @return GameMaster - возвращает экземпляр класса GameMaster
     * @see GameMaster#GameMaster()
     */
    public static synchronized GameMaster getGameMaster() {
        if (instance == null) {
            instance = new GameMaster();
        }
        return instance;
    }

    /**
     * Метод для отрисовки окна
     *
     * @param g - графика передаваемая с панели
     */
    public void renderFrame(Graphics g) {
        if(gameStatus != MAIN_MENU) frameCount++;

        if(isDragTower){
            onDragTowerEvent(g);
        }

        if(gameStatus == MAIN_MENU){

        }
        else if(gameStatus == GameStatus.MAIN_MENU_LEVEL){
            mainMenuLevelState(g);
        }
        else if(gameStatus == GameStatus.LEVEL_ENTER){
            gameStatus = GameStatus.WAVE_STARTING;
        }
        else if (gameStatus == GameStatus.WAVE_STARTING) {
            towersAttack(g);
            waveStarting(g);
        } else if (gameStatus == GameStatus.WAVE_STARTED) {
            towersAttack(g);
            waveStarted(g);
        }
    }

    private void mainMenuLevelState(Graphics g) {
        try {
            animationLevelLoadingStatus+=15;

            BufferedImage bufImg = ImageIO.read(new File(HONEY_OVERGROUND_IMAGE));
            g.drawImage(bufImg.getScaledInstance(bufImg.getWidth(),
                            (int) (bufImg.getHeight()*1.2),
                    Image.SCALE_AREA_AVERAGING),
                    0, (int) (bufImg.getHeight()*(-1.2)+animationLevelLoadingStatus),null);

            if(bufImg.getHeight()*(-1.2)+animationLevelLoadingStatus>=0){
                gameStatus=GameStatus.LEVEL_ENTER;
                animationLevelLoadingStatus=0;
                frameCount=0;

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Метод для отображения сцены начала волны
     * @param g - графика
     */
    private void waveStarting(Graphics g) {
        g.setFont(new Font("Arial", Font.PLAIN, 50));
        g.setColor(Color.WHITE);
        g.drawString(String.valueOf((WAVE_CHANGE_COOLDOWN - getSecondsLeft())), this.mapUtils.getWidth() / 2 - 20, this.mapUtils.getHeight() / 6);
        if (getSecondsLeft() >= WAVE_CHANGE_COOLDOWN) {
            frameCount = 100;
            gameStatus = GameStatus.WAVE_STARTED;
            waveLevel++;
            enemyCount += 2;
            enemyCountSpawned = 0;
            player.getTowerRepository().getTowers().forEach(t->{
                t.setLastAttackTime(0);
                t.setCanAttack();
            });
            attackRepository.getAttacks().forEach(a-> attackRepository.deleteAttack(a.getId()));
        }
    }

    /**
     * Метод для отображения сцены конца волны
     * @param g
     */
    private void waveStarted(Graphics g) {

        g.setFont(new Font("Arial", Font.PLAIN, 50));
        g.setColor(Color.WHITE);
        g.drawString("Wave " + waveLevel, this.mapUtils.getWidth() / 2 - 30, this.mapUtils.getHeight() / 6);

        if (getSecondsLeft() == (DEFAULT_ENEMY_SPAWN_COOLDOWN * (enemyCountSpawned + 1))
                && enemyCount > enemyCountSpawned) {
            enemySpawn();
        }
        else if(enemyCount==enemyCountSpawned && enemyRepository.getEnemies().isEmpty()){
            gameStatus = GameStatus.WAVE_STARTING;
            frameCount = 0;
        }
        enemiesMove(g);
        attacksMove(g);

    }

    /**
     * Отображение передвижения передвигаемой башни
     * @param g - графика
     */
    private void onDragTowerEvent(Graphics g) {
        Graphics2D g2d = (Graphics2D) g;

        AlphaComposite alphaComposite = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.2f);
        g2d.setComposite(alphaComposite);

        if (dragTower.isCanOccupe()) {
            g2d.setColor(Color.GREEN);
        } else {
            g2d.setColor(Color.RED);
        }

        g2d.fillOval((int)(dragTower.getX()+CELL_WIDTH/2-dragTower.getRadius()),
                (int)(dragTower.getY()+CELL_WIDTH/2-dragTower.getRadius()),
                (int)dragTower.getRadius()*2,
                (int)dragTower.getRadius()*2);
        g2d.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f));
        g2d.drawOval((int)(dragTower.getX()+CELL_WIDTH/2-dragTower.getRadius()),
                (int)(dragTower.getY()+CELL_WIDTH/2-dragTower.getRadius()),
                (int)dragTower.getRadius()*2,
                (int)dragTower.getRadius()*2);
        g2d.drawImage(dragTower.getImage(), dragTower.getX(),dragTower.getY(),null);
    }

    /**
     * Метод добавления новых врагов на карту
     */
    private void enemySpawn() {
        try {
            Enemy enemy = (Enemy) enemyRepository.getEnemiesMarket().get(0).clone();
            enemyRepository.addNewEnemy(enemy);
        } catch (CloneNotSupportedException e) {
            throw new RuntimeException(e);
        }
        enemyCountSpawned++;
    }

    /**
     * Метод передвижения врагов на карте (меняет их положение)
     *
     * @param g - графика
     */
    private void enemiesMove(Graphics g) {
        enemyRepository.getEnemies().forEach(e -> {
            int imageType=e.move();
            if(frameCount%8==0) {
                e.playAnimation(imageType);
            }

            g.drawImage(e.getImage(),
                    e.getX(),
                    e.getY(),
                    null);

            g.setColor(Color.red);
            g.fillRect(e.getX()+14,e.getY(), (int)e.getHealth()*100 / (int) e.getMaxHealth(),10);
            g.setColor(Color.black);
            g.drawRect(e.getX()+14,e.getY(),100,10);
            g.setColor(Color.white);
            g.setFont(new Font("Arial", Font.PLAIN, 10));
            g.drawString(Double.toString((double) Math.round(e.getHealth() * 10) /10),e.getX()+14+40,e.getY()+10);
        });
    }

    /**
     * Метод создания атак башен
     * @param g - графика
     */
    private void towersAttack(Graphics g) {
        player.getTowerRepository().getTowers().forEach(t -> {
            BufferedImage towerImage = t.rotateTower();
            g.drawImage(towerImage,
                    t.getX(),
                    t.getY(),
                    null);

            if(t.canAttack()){
                if (getSecondsLeftDouble() - t.getLastAttackTime() >= t.getAttackCooldown()) {
                    t.playAnimation();
                }
            }

            if(t.getIsAnimation()){
                if(frameCount%8==0 && t.playAnimation()){
                    try {
                        t.getAttack().setTarget(t.getTarget());
                        switch(t.getAttack().getAttackType()){
                            case SPIN_ATTACK -> {
                                ((SpinAttack)t.getAttack()).setOwnTower(t);
                                t.getAttack().setX(t.getX());
                                t.getAttack().setY(t.getY());
                            }
                            case SHOOT_ATTACK, MELEE_ATTACK -> {
                                t.getAttack().setX(t.getX()+ CELL_WIDTH/2);
                                t.getAttack().setY(t.getY()+ CELL_WIDTH/2);
                            }

                        }
                        t.setLastAttackTime(getSecondsLeftDouble());
                        Attack newAttack = (Attack)(t.getAttack()).clone();

                        attackRepository.addNewAttack(newAttack);
                    } catch (CloneNotSupportedException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
            if (!t.isCanAttack() && !t.getIsAnimation() &&
                    getSecondsLeftDouble() - t.getLastAttackTime() >= t.getAttackCooldown()) {
                t.setCanAttack();
            }
        });
    }

    /**
     * Метод отображения атак
     * @param g - графика
     */
    private void attacksMove(Graphics g) {
        attackRepository.getAttacks().forEach(a -> {
            Enemy target = a.getTarget();
            if (a.getAttackType()!=AttackType.SPIN_ATTACK && target == null) {
                attackRepository.deleteAttack(a.getId());
                return;
            }
            if(!a.move(g)) {
                attackRepository.deleteAttack(a.getId());
            }
        });
    }

    public int getSecondsLeft() {return (frameCount * MILLISECONDS_PER_FRAME/2) / 1000;}
    public double getSecondsLeftDouble(){return (double) Math.round(getSecondsLeft() * 100) /100;}
    public Tower getDragTower(){return this.dragTower;}
    public void setDragTower(Tower dragTower){this.dragTower = dragTower;}
    public boolean isDragTower() {return isDragTower;}
    public void setIsDragTower(boolean isDragTower) {this.isDragTower = isDragTower;}
    public Player getPlayer(){return this.player;}
    public void setGameStatus(GameStatus gameStatus) {this.gameStatus = gameStatus;}
    public GameStatus getGameStatus() {return this.gameStatus;}
}
