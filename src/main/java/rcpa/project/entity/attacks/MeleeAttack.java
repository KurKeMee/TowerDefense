package rcpa.project.entity.attacks;

import rcpa.project.entity.base.Attack;
import rcpa.project.entity.base.Enemy;

import java.awt.*;
import java.awt.image.BufferedImage;

public class MeleeAttack extends Attack {

    public MeleeAttack(int id, double damage, BufferedImage image, int x, int y, Enemy target) {
        super(id, damage, image, x, y, target);
    }

    @Override
    public boolean move(Graphics g) {
return false;
    }
}
