package rcpa.project.entity.attacks;

import rcpa.project.entity.base.Attack;
import rcpa.project.entity.base.Enemy;
import rcpa.project.repository.AttackRepository;
import rcpa.project.util.MapUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Serializable;

public class SpinAttack extends Attack implements Cloneable {
    int spinDuration;
    int timeSpent;

    public SpinAttack(int id, double damage, BufferedImage image, int x, int y, Enemy target, int spinDuration) {
        super(id, damage, image, x, y, target);
        this.spinDuration = spinDuration;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        Attack newAttack = (Attack) super.clone();
        newAttack.setId(AttackRepository.getAttackRepository().getFreeId());
        return newAttack;
    }

    @Override
    public boolean move(Graphics g){
        timeSpent++;
        if(timeSpent < spinDuration*60){
            MapUtils.rotateImage(this.getImage(),5);
            return true;
        }
        else{
            return false;
        }
    }
}
