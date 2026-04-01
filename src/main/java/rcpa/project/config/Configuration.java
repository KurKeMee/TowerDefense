package rcpa.project.config;

import javax.imageio.ImageIO;
import javax.print.DocFlavor;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

public interface Configuration {

    String GAME_NAME = "Tower Defense";


    int CELL_WIDTH = 128;
    int TOWER_SLOT_RESIZING = 96;


    ////Путь к изображениям

    //Интерфейс
    String SLOT_IMAGE = "src/main/resources/assets/interface/slot.png";

    //Карты
    String MAP_1 = "src/main/resources/maps/map1.png";

    //Ячейки
    String ROAD_CELL = "src/main/resources/assets/cells/road.png";
    String GRASS_CELL = "src/main/resources/assets/cells/grass.png";
    String HOME_PORTAL_CELL = "src/main/resources/assets/cells/home_portal.png";
    String ENEMY_PORTAL_CELL = "src/main/resources/assets/cells/enemy_portal.png";

    //Враги
    String SLIME_ENEMY = "src/main/resources/assets/enemies/slime_enemy.png";
    String SLIME_ENEMY_A1 = "src/main/resources/assets/enemies/slime_enemy_animation1.png";
    String SLIME_ENEMY_A2 = "src/main/resources/assets/enemies/slime_enemy_animation2.png";
    String SLIME_ENEMY_A3 = "src/main/resources/assets/enemies/slime_enemy_animation3.png";

    //Башни

    String CACTUS_TOWER = "src/main/resources/assets/towers/cactus_tower.png";
    String CACTUS_TOWER_A1 = "src/main/resources/assets/towers/cactus_tower_animation1.png";
    String CACTUS_TOWER_A2 = "src/main/resources/assets/towers/cactus_tower_animation2.png";
    String CACTUS_TOWER_A3 = "src/main/resources/assets/towers/cactus_tower_animation3.png";

    String SKELETONKING_TOWER = "src/main/resources/assets/towers/skeletonking_tower.png";
    String SKELETONKING_TOWER_A1 = "src/main/resources/assets/towers/skeletonking_tower_animation1.png";
    String SKELETONKING_TOWER_A2 = "src/main/resources/assets/towers/skeletonking_tower_animation2.png";
    String SKELETONKING_TOWER_A3 = "src/main/resources/assets/towers/skeletonking_tower_animation3.png";

    String LASER_TOWER = "src/main/resources/assets/towers/laser_tower.png";
    String LASER_TOWER_A1 = "src/main/resources/assets/towers/laser_tower_animation1.png";
    String LASER_TOWER_A2 = "src/main/resources/assets/towers/laser_tower_animation2.png";
    String LASER_TOWER_A3 = "src/main/resources/assets/towers/laser_tower_animation3.png";

    String TRIPLESHOOTER_TOWER = "src/main/resources/assets/towers/droid_tower.png";
    String TRIPLESHOOTER_TOWER_A1 = "src/main/resources/assets/towers/droid_tower_animation1.png";
    String TRIPLESHOOTER_TOWER_A2 = "src/main/resources/assets/towers/droid_tower_animation2.png";
    String TRIPLESHOOTER_TOWER_A3 = "src/main/resources/assets/towers/droid_tower_animation2.png";

    String SABER_TOWER = "src/main/resources/assets/towers/hermit_tower.png";
    String SABER_TOWER_A1 = "src/main/resources/assets/towers/hermit_tower_animation1.png";
    String SABER_TOWER_A2 = "src/main/resources/assets/towers/hermit_tower_animation2.png";
    String SABER_TOWER_A3 = "src/main/resources/assets/towers/hermit_tower_animation3.png";

    //Атаки
    String TRIPLESHOOTER_TOWER_ATTACK = "src/main/resources/assets/attacks/droid_tower_attack.png";
    String LASER_TOWER_ATTACK = "src/main/resources/assets/attacks/laser_tower_attack.png";
    String SABER_TOWER_ATTACK = "src/main/resources/assets/attacks/hermit_tower_attack.png";
    String SKELETONKING_TOWER_ATTACK = "src/main/resources/assets/attacks/bolt.png";
    String CACTUS_TOWER_ATTACK = "src/main/resources/assets/attacks/hermit_tower_attack.png";

    //Статусы игры
    byte WAVE_START = 1;
    byte WAVE_END = 2;

    //Анимация
    int MILLISECONDS_PER_FRAME = 50;

    //Cooldowns
    int DEFAULT_ENEMY_SPAWN_COOLDOWN = 5;
    int WAVE_CHANGE_COOLDOWN = 5;
}
