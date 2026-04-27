package rcpa.project.entity.attacks;

import rcpa.project.entity.base.Attack;
import rcpa.project.entity.base.AttackType;
import rcpa.project.entity.base.Enemy;
import rcpa.project.repository.EnemyRepository;

import java.awt.*;
import java.awt.image.BufferedImage;

import static rcpa.project.config.Configuration.CELL_WIDTH;

public class MeleeAttack extends Attack  implements Cloneable{

    private int ticks=0;

    public MeleeAttack(int id, double damage, BufferedImage image, int x, int y, Enemy target, AttackType attackType) {
        super(id, damage, image, x, y, target, attackType);
    }

    @Override
    public boolean move(Graphics g) {
        ticks++;
        super.render(g);
        if (getTarget() == null) {
            return false;
        }
        double dx = getTarget().getX()+ CELL_WIDTH/2 - getX();
        double dy = getTarget().getY()+ CELL_WIDTH/2 - getY();

        double distance = Math.sqrt(dx*dx + dy*dy);

        if (ticks>5) {
            attack();
            return false; //Означает удалить атаку
        } else {
            setX(getX() + (int) ((dx / distance)*20));
            setY(getY() + (int) ((dy / distance)*20));
        }

        return true;
    }

    @Override
    public void attack() {
        super.attack();
    }
}
