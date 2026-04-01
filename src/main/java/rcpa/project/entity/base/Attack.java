package rcpa.project.entity.base;

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
    private double speed = 20;
    private Enemy target;
    private BufferedImage image;

    public Attack(int id, double damage, BufferedImage image, int x, int y, Enemy target) {
        this.id = id;
        this.image = image;
        this.damage = damage;
        this.x = x;
        this.y = y;
        this.target = target;

        if (target != null) {
            double dx = target.getX()+CELL_WIDTH/2 - x;
            double dy = target.getY()+CELL_WIDTH/2 - y;
            this.angle = Math.atan2(dy, dx) - Math.PI / 2;
            this.image = MapUtils.rotateImage(image, angle);
        }
    }

    public boolean move(Graphics g){
//        else if(getAttackType()==AttackType.MELEE){
//            setX(getTarget().getX()+CELL_WIDTH/2);
//            setY(getTarget().getY()+CELL_WIDTH/2);
//        }


        render(g);
        return false;
    }

    public void render(Graphics g) {
        if (image != null) {
            int w = image.getWidth();
            int h = image.getHeight();
            g.drawImage(image, (int)x - w/2, (int)y - h/2, null);
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
}
