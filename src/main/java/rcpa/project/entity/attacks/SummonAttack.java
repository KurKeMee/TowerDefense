package rcpa.project.entity.attacks;

import rcpa.project.entity.base.Attack;
import rcpa.project.entity.base.Enemy;

import java.awt.image.BufferedImage;

public class SummonAttack extends Attack {

    public SummonAttack(int id, double damage, BufferedImage image, int x, int y, Enemy target) {
        super(id, damage, image, x, y, target);
    }
}
