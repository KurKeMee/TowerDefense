package rcpa.project.entity.attacks;

import rcpa.project.entity.base.Attack;
import rcpa.project.entity.base.AttackType;
import rcpa.project.entity.base.Enemy;
import rcpa.project.entity.base.Tower;
import rcpa.project.repository.AttackRepository;
import rcpa.project.repository.EnemyRepository;
import rcpa.project.service.GameMaster;
import rcpa.project.util.MapUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Serializable;
import java.util.LinkedList;

import static rcpa.project.config.Configuration.CELL_WIDTH;
import static rcpa.project.config.Configuration.MILLISECONDS_PER_FRAME;

public class SpinAttack extends Attack implements Cloneable {
    int spinDuration;
    int timeSpent;
    double radius;
    Tower ownTower;

    public SpinAttack(int id, double damage, BufferedImage image, int x, int y, Enemy target, int spinDuration, AttackType attackType, double radius) {
        super(id, damage, image, x, y, target, attackType);
        this.spinDuration = spinDuration;
        this.radius = radius;
    }

    @Override
    public SpinAttack clone() throws CloneNotSupportedException {
        SpinAttack newAttack = (SpinAttack) super.clone();
        newAttack.setId(AttackRepository.getAttackRepository().getFreeId());
        return newAttack;
    }

    @Override
    public boolean move(){
        timeSpent++;
        if(timeSpent < spinDuration*30){
            if(timeSpent%2==0) {
                ownTower.playAnimation();
                ownTower.setAnimation(false);
                if(timeSpent%10==0) attack();
            }
            return true;
        }
        else{
            ownTower.setLastAttackTime(System.currentTimeMillis());
            return false;
        }
    }

    @Override
    public void attack() {
        EnemyRepository.getEnemyRepository().getEnemies().stream().filter(enemy -> {
            double distance = Math.sqrt(Math.pow((enemy.getX() + (double) CELL_WIDTH / 2) - (this.getX() + CELL_WIDTH / 2), 2)
                    + Math.pow((enemy.getY() + (double) CELL_WIDTH / 2) -
                    (this.getY() + CELL_WIDTH / 2), 2));
            return distance <= radius;
        }).forEach(enemy -> {
            enemy.takeDamage(this.getDamage());
        });
    }

    @Override
    public void render(Graphics g) {
        g.drawImage(getImage().getScaledInstance((int) (radius*((double) (timeSpent % 20+1) /20)*2),
                                                (int) (radius*((double) (timeSpent % 20+1) /20)*2), Image.SCALE_AREA_AVERAGING),
                                                (int)(ownTower.getX()+CELL_WIDTH/2-radius*((double) (timeSpent % 20+1) /20)),
                                                (int)(ownTower.getY()+CELL_WIDTH/2-radius*((double) (timeSpent % 20+1) /20)),
                                                null);
    }

    public Tower getOwnTower() {
        return ownTower;
    }

    public void setOwnTower(Tower ownTower) {
        this.ownTower = ownTower;
    }
}
