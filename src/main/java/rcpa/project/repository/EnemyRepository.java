package rcpa.project.repository;

import rcpa.project.entity.base.Cell;
import rcpa.project.entity.base.Enemy;
import rcpa.project.entity.base.EnemyType;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static rcpa.project.config.Configuration.*;
import static rcpa.project.config.Configuration.SLIME_ENEMY_A3;

/**
 * @author Ivan Monin
 *
 * Класс для хранения врагов
 */
public class EnemyRepository {
    /**
     * Переменная для хранения экземпляра класса EnemyRepository
     *
     * @see EnemyRepository
     */
    private static EnemyRepository instance;


    /**
     * Массив для хранения врагов
     *
     * @see HashMap
     * @see Enemy
     */
    private HashMap<Integer,Enemy> enemies;
    private ArrayList<Enemy> enemiesMarket = new ArrayList<>();
    private byte lastId;
    private ArrayList<Cell> path;

    /**
     * Конструктор класса EnemyRepository
     * Конструктор объявлен private для паттерна Singleton
     *
     * @see EnemyRepository#getEnemyRepository()
     */
    private EnemyRepository() {
        enemies = new HashMap<>();
        init();
    }

    /**
     * Метод для получения единственного экземпляра класса
     * Параметр synchronized необходим для исключения ситуации множественного создания экземляров класса
     *
     * @see EnemyRepository#EnemyRepository()
     * @return EnemyRepository - возвращает единственный экземпляр класса
     */
    public static synchronized EnemyRepository getEnemyRepository() {
        if (instance == null) {
            instance = new EnemyRepository();
        }
        return instance;
    }

    private void init(){
        try {
            ArrayList<BufferedImage> anim = new ArrayList<>(Arrays.asList(
                                                                ImageIO.read(new File(SLIME_ENEMY)),
                                                                ImageIO.read(new File(SLIME_ENEMY_A1)),
                                                                ImageIO.read(new File(SLIME_ENEMY_A2)),
                                                                ImageIO.read(new File(SLIME_ENEMY_A3))));
            enemiesMarket.addAll(Arrays.asList(new Enemy(
                                                        EnemyType.DEFAULT_ENEMY.id,
                                                        30,
                                                        2,
                                                        anim.getFirst(),
                                                        anim,
                                                        "Slime"),
                                                new Enemy(
                                                        EnemyType.BIG_ENEMY.id,
                                                        150,
                                                        1,
                                                        anim.getFirst(),
                                                        anim,
                                                        "Big Smile"),
                                                new Enemy(
                                                        EnemyType.SPEEDY_ENEMY.id,
                                                        35,
                                                        5,
                                                        anim.getFirst(),
                                                        anim,
                                                        "Speedy Slime")));


        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Метод добавления врага в репозиторий {@link #enemies}
     *
     * @param enemy - созданный враг
     */
    public void addNewEnemy(Enemy enemy) {
        if(path==null) path = CellRepository.getCellRepository().buildPath();
        enemy.setWay(path);
        enemies.put(enemy.getId(),enemy);
    }

    /**
     * Метод получения врага по id
     * @param id - идентификтор врага
     * @return Enemy - возвращает врага
     */
    public Enemy getEnemy(int id) {
        return enemies.getOrDefault(id, null);
    }

    /**
     * Метод получения всех врагов
     * @return ArrayList<Enemy> - возвращает список врагов
     */
    public ArrayList<Enemy> getEnemies() {
        return new ArrayList<>(enemies.values());
    }

    /**
     * Метод удаления врага по id
     * @param id - идентификтор врага
     * @return boolean - возвращает удален ли враг
     */
    public boolean deleteEnemy(int id) {
        enemies.remove(id);
        return getEnemy(id) == null;
    }

    /**
     * Метод получения свободного Id {@link #enemies}
     *
     */
    public byte getFreeId() {
        lastId++;
        return lastId;
    }

    public void clearEnemies() {
        enemies.clear();
    }

    public ArrayList<Enemy> getEnemiesMarket() {
        return enemiesMarket;
    }
}
