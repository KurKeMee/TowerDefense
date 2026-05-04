package rcpa.project.repository;

import rcpa.project.entity.attacks.MeleeAttack;
import rcpa.project.entity.attacks.ShootAttack;
import rcpa.project.entity.attacks.SpinAttack;
import rcpa.project.entity.attacks.SummonAttack;
import rcpa.project.entity.base.AttackType;
import rcpa.project.entity.base.Tower;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static rcpa.project.config.Configuration.*;
import static rcpa.project.config.Configuration.CACTUS_TOWER_A3;

/**
 * @author Ivan Monin
 *
 * Класс для хранения башен
 */
public class TowerRepository {
    /**
     * Переменная для хранения экземпляра класса TowerRepository
     *
     * @see TowerRepository
     */
    private static TowerRepository instance;


    /**
     * Массив для хранения башен
     *
     * @see HashMap
     * @see Tower
     */
    private HashMap<Integer,Tower> towers;


    /**
     * Массив со всеми башнями
     */
    private static ArrayList<Tower> towerMarket = new ArrayList<>();


    /**
     * Конструктор класса TowerRepository
     *
     */
    private TowerRepository() {
        towers = new HashMap<>();
        init();
    }

    public static TowerRepository getTowerRepository() {
        if (instance == null) {
            instance = new TowerRepository();
        }
        return instance;
    }

    /**
     * Метод инициализации всех башен
     */
    private void init() {
        try {
            ArrayList<BufferedImage> towerAnimationLaser = new ArrayList<>(
                    Arrays.asList(ImageIO.read(new File(LASER_TOWER)),
                            ImageIO.read(new File(LASER_TOWER_A1)),
                            ImageIO.read(new File(LASER_TOWER_A2)),
                            ImageIO.read(new File(LASER_TOWER_A3))));
            ArrayList<BufferedImage> towerAnimationSkeletonKing = new ArrayList<>(
                    Arrays.asList(ImageIO.read(new File(SKELETONKING_TOWER)),
                            ImageIO.read(new File(SKELETONKING_TOWER_A1)),
                            ImageIO.read(new File(SKELETONKING_TOWER_A2)),
                            ImageIO.read(new File(SKELETONKING_TOWER_A3))));
            ArrayList<BufferedImage> towerAnimationSaber = new ArrayList<>(
                    Arrays.asList(ImageIO.read(new File(SABER_TOWER)),
                            ImageIO.read(new File(SABER_TOWER_A1)),
                            ImageIO.read(new File(SABER_TOWER_A2)),
                            ImageIO.read(new File(SABER_TOWER_A3))));
            ArrayList<BufferedImage> towerAnimationTripleShooter = new ArrayList<>(
                    Arrays.asList(ImageIO.read(new File(TRIPLESHOOTER_TOWER)),
                            ImageIO.read(new File(TRIPLESHOOTER_TOWER_A1)),
                            ImageIO.read(new File(TRIPLESHOOTER_TOWER_A2)),
                            ImageIO.read(new File(TRIPLESHOOTER_TOWER_A3))));
            ArrayList<BufferedImage> towerAnimationCactus = new ArrayList<>(
                    Arrays.asList(ImageIO.read(new File(CACTUS_TOWER)),
                            ImageIO.read(new File(CACTUS_TOWER_A1)),
                            ImageIO.read(new File(CACTUS_TOWER_A2)),
                            ImageIO.read(new File(CACTUS_TOWER_A3))));


            towerMarket.addAll(Arrays.asList(new Tower(0,
                                                            200,
                                                            5,
                                                            3 * CELL_WIDTH,
                                                            2,
                                                            new ShootAttack(
                                                                    AttackRepository.getAttackRepository().getFreeId(),
                                                                    5,
                                                                    ImageIO.read(new File(LASER_TOWER_ATTACK)),
                                                                    0,
                                                                    0,
                                                                    null,
                                                                    AttackType.SHOOT_ATTACK),
                                                            towerAnimationLaser.getFirst(),
                                                            towerAnimationLaser,
                                                            "Spiky Bee"),
                                                    new Tower(1,
                                                            200,
                                                            10,
                                                            3 * CELL_WIDTH,
                                                            10,
                                                            new SummonAttack(
                                                                    AttackRepository.getAttackRepository().getFreeId(),
                                                                    10,
                                                                    ImageIO.read(new File(SKELETONKING_TOWER_ATTACK)),
                                                                    0,0,null, AttackType.SUMMON_ATTACK),
                                                            towerAnimationSkeletonKing.getFirst(),
                                                            towerAnimationSkeletonKing,
                                                            "Skeleton King Tower"),
                                                    new Tower(2,
                                                            200,
                                                            3, //TODO Добавить определение пересечения плоскостей
                                                            1.5 * CELL_WIDTH,
                                                            3,
                                                            new MeleeAttack(
                                                                    AttackRepository.getAttackRepository().getFreeId(),
                                                                    3,
                                                                    ImageIO.read(new File(SABER_TOWER_ATTACK)),
                                                                    0,0,null, AttackType.MELEE_ATTACK),
                                                            towerAnimationSaber.getFirst(),
                                                            towerAnimationSaber,
                                                            "Melee bee"),
                                                    new Tower(3,
                                                            200,
                                                            3,
                                                            3 * CELL_WIDTH,
                                                            1,
                                                            new ShootAttack(
                                                                    AttackRepository.getAttackRepository().getFreeId(),
                                                                    3,
                                                                    ImageIO.read(new File(TRIPLESHOOTER_TOWER_ATTACK)),
                                                                    0,0,null, AttackType.SHOOT_ATTACK),
                                                            towerAnimationTripleShooter.getFirst(),
                                                            towerAnimationTripleShooter,
                                                            "TripleShooter Tower"),
                                                    new Tower(4,
                                                            200,
                                                            0.2,
                                                            1.5 * CELL_WIDTH,
                                                            3,
                                                            new SpinAttack(
                                                                    AttackRepository.getAttackRepository().getFreeId(),
                                                                    0.6,
                                                                    ImageIO.read(new File(CACTUS_TOWER_ATTACK)),
                                                                    0,0,null,   3, AttackType.SPIN_ATTACK,CELL_WIDTH*1.5),
                                                            towerAnimationCactus.getFirst(),
                                                            towerAnimationCactus,
                                                            "Spin Bee")));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Метод добавления врага в репозиторий {@link #towers}
     *
     * @param tower - созданная башня
     */
    public void addNewTower(Tower tower) {
        towers.put(tower.getId(),tower);
    }

    /**
     * Метод получения башни по id
     * @param id - идентификтор башни
     * @return Tower - возвращает башню
     */
    public Tower getTower(int id) {
        return towers.getOrDefault(id, null);
    }

    /**
     * Метод получения всех башен
     * @return ArrayList<Tower> - возвращает список башен
     */
    public ArrayList<Tower> getTowers() {
        return new ArrayList<>(towers.values());
    }

    /**
     * Метод удаления башни по id
     * @param id - идентификтор башни
     * @return boolean - возвращает удалена ли башня
     */
    public boolean deleteTower(int id) {
        towers.remove(id);
        return getTower(id) == null;
    }

    /**
     * Метод получения свободного Id {@link #towers}
     *
     */
    public byte getFreeId() {
        return (byte) (towers.size());
    }

    public static ArrayList<Tower> getTowerMarket() {
        return towerMarket;
    }
}
