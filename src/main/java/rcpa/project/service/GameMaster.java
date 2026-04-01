package rcpa.project.service;

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
    private byte gameStatus = WAVE_END;

    /**
     * Счетчик секунд
     */
    private double secondsCount = 0;
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
        secondsCount += (double) MILLISECONDS_PER_FRAME / 1000;

        if(isDragTower){
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

        if (gameStatus == WAVE_END) {
            g.setFont(new Font("Arial", Font.PLAIN, 50));
            g.setColor(Color.WHITE);
            g.drawString(String.valueOf((WAVE_CHANGE_COOLDOWN - (int) secondsCount)), this.mapUtils.getWidth() / 2 - 20, this.mapUtils.getHeight() / 6);
            if (secondsCount >= WAVE_CHANGE_COOLDOWN) {
                secondsCount = 1;
                gameStatus = WAVE_START;
                waveLevel++;
                enemyCount += 2;
                enemyCountSpawned = 0;
                player.getTowerRepository().getTowers().forEach(t->{
                    t.setLastAttackTime(0);
                    t.setCanAttack();
                });
            }
        } else if (gameStatus == WAVE_START) {
            g.setFont(new Font("Arial", Font.PLAIN, 50));
            g.setColor(Color.WHITE);
            g.drawString("Wave " + waveLevel, this.mapUtils.getWidth() / 2 - 20, this.mapUtils.getHeight() / 6);

            if ((int) (secondsCount) % (DEFAULT_ENEMY_SPAWN_COOLDOWN * (enemyCountSpawned + 1)) == 0
                    && enemyCount > enemyCountSpawned) {
                enemySpawn();
            }
            else if(enemyCount==enemyCountSpawned && enemyRepository.getEnemies().isEmpty()){
                gameStatus = WAVE_END;
                secondsCount = 0;
            }

            enemiesMove(g);
            towersAttack(g);
            attacksMove(g);
        }

    }

    /**
     * Метод добавления новых врагов на карту
     */
    public void enemySpawn() {
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
    public void enemiesMove(Graphics g) {
        enemyRepository.getEnemies().forEach(e -> {
            int imageType = e.move();
            if(secondsCount%0.1<0.05) e.playAnimation(imageType);

            g.drawImage(e.getImage(),
                    e.getX(),
                    e.getY(),
                    null);

            g.setColor(Color.red);
            g.fillRect(e.getX()+14,e.getY(), (int)e.getHealth()*100 / (int) e.getMaxHealth(),10);
            g.setColor(Color.black);
            g.drawRect(e.getX()+14,e.getY(),100,10);
        });
    }

    /**
     * Метод установки башни
     */
    public void spawnTower(Tower tower) {
        player.getTowerRepository().addNewTower(tower);
    }

    public void towersAttack(Graphics g) {
        player.getTowerRepository().getTowers().forEach(t -> {
            BufferedImage towerImage = t.rotateTower();
            g.drawImage(towerImage,
                    t.getX(),
                    t.getY(),
                    null);

            if (t.canAttack() || t.getIsAnimation()) {
                if (secondsCount%0.3<0.5 && t.playAnimation()) {
                    try {
                        attackRepository.addNewAttack((t.getAttack().getClass())(t.getAttack()).clone());
                    } catch (CloneNotSupportedException e) {
                        throw new RuntimeException(e);
                    }
                    t.setLastAttackTime(secondsCount);
                }
            } else if (!t.isCanAttack() && secondsCount-t.getLastAttackTime() >= t.getAttackCooldown() && !t.getIsAnimation()) {
                t.setCanAttack();
            }
        });
    }

    public void attacksMove(Graphics g) {
        attackRepository.getAttacks().forEach(a -> {
            Enemy target = a.getTarget();
            if (target == null) {
                attackRepository.deleteAttack(a.getId());
                return;
            }
            if(a.move(g)) attackRepository.deleteAttack(a.getId());
            if (target.getHealth() == 0) enemyRepository.deleteEnemy(target.getId());
        });
    }

    public Tower getDragTower(){
        return this.dragTower;
    }

    public void setDragTower(Tower dragTower){
        this.dragTower = dragTower;
    }

    public boolean isDragTower() {
        return isDragTower;
    }

    public void setIsDragTower(boolean isDragTower) {
        this.isDragTower = isDragTower;
    }

    public Player getPlayer(){
        return this.player;
    }
}
