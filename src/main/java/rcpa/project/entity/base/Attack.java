package rcpa.project.entity.base;

import rcpa.project.repository.AttackRepository;
import rcpa.project.repository.EnemyRepository;
import rcpa.project.util.MapUtils;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Serializable;

import static rcpa.project.config.Configuration.CELL_WIDTH;

public abstract class Attack implements Cloneable {
    private int id;
    private double damage;
    private int x;
    private int y;
    private double angle;
    private double speed = 40;
    private boolean completed = false;
    private AttackType attackType;
    private Enemy target;
    private BufferedImage image;

    public Attack(int id, double damage, BufferedImage image, int x, int y, Enemy target, AttackType attackType) {
        this.id = id;
        this.image = image;
        this.damage = damage;
        this.x = x;
        this.y = y;
        this.target = target;
        this.attackType = attackType;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        Attack clone = (Attack) super.clone();
        clone.id = AttackRepository.getAttackRepository().getFreeId();
        if (target != null) {
            double dx = (target.getX() + CELL_WIDTH/2) - x;
            double dy = (target.getY() + CELL_WIDTH/2) - y;
            clone.angle = Math.atan2(dy, dx) + Math.PI / 2;
            clone.target = target;
        }
        return clone;
    }

    public abstract boolean move();

    public void attack(){
        this.getTarget().takeDamage(getDamage());
    }

    public void render(Graphics g) {
        if (image != null) {
            int w = image.getWidth();
            int h = image.getHeight();
            g.drawImage(image, x - w/2, y - h/2, null);
        }
    }

    public BufferedImage getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public int getId() {
        return id;
    }

    public void setId(int id){this.id = id;}

    public Enemy getTarget() {
        return target;
    }
    public void setTarget(Enemy target) {this.target = target;}

    public double getDamage() {
        return damage;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        this.speed = speed;
    }

    public double getAngle() {
        return angle;
    }

    public void setAngle(double angle) {
        this.angle = angle;
    }

    public AttackType getAttackType() {
        return attackType;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }
}
