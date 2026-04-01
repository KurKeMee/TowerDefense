package rcpa.project.entity.attacks;

import rcpa.project.entity.base.Attack;
import rcpa.project.entity.base.Enemy;

import java.awt.*;
import java.awt.image.BufferedImage;

import static rcpa.project.config.Configuration.CELL_WIDTH;

public class ShootAttack extends Attack {

    public ShootAttack(int id, double damage, BufferedImage image, int x, int y, Enemy target) {
        super(id, damage, image, x, y, target);
    }

    @Override
    public boolean move(Graphics g){
        if (getTarget() == null) {
            return false;
        }
        double dx = getTarget().getX()+CELL_WIDTH/2 - this.getX();
        double dy = getTarget().getY()+CELL_WIDTH/2 - this.getY();

        double distance = Math.sqrt(dx*dx + dy*dy);

        if (distance < getSpeed()*2) {
            setX(getTarget().getX()+CELL_WIDTH/2);
            setY(getTarget().getY()+CELL_WIDTH/2);
            getTarget().takeDamage(getDamage());
            return false; //Означает удалить атаку
        } else {
            setX(getX()+(int) ((dx / distance) * getSpeed()));
            setY(getY()+(int) ((dy / distance) * getSpeed()));
        }

        return true;
    }
}
