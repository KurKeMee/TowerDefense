package rcpa.project.entity.attacks;

import rcpa.project.entity.base.Attack;
import rcpa.project.entity.base.AttackType;
import rcpa.project.entity.base.Enemy;
import rcpa.project.repository.EnemyRepository;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Serializable;

import static rcpa.project.config.Configuration.CELL_WIDTH;

public class ShootAttack extends Attack implements Cloneable {

    public ShootAttack(int id, double damage, BufferedImage image, int x, int y, Enemy target, AttackType attackType) {
        super(id, damage, image, x, y, target, attackType);
    }

    @Override
    public boolean move(){
        if (getTarget() == null) {
            return false;
        }
        double dx = getTarget().getX()+ CELL_WIDTH/2 - getX();
        double dy = getTarget().getY()+ CELL_WIDTH/2 - getY();

        double distance = Math.sqrt(dx*dx + dy*dy);

        if (distance < getSpeed()*2) {
            setX(getTarget().getX()+ CELL_WIDTH/2);
            setY(getTarget().getY()+ CELL_WIDTH/2);
            attack();
            return false; //Означает удалить атаку
        } else {
            setX(getX()+(int) ((dx / distance) * getSpeed()));
            setY(getY()+(int) ((dy / distance) * getSpeed()));
        }

        return true;
    }

    @Override
    public void attack() {
        super.attack();
    }
}
