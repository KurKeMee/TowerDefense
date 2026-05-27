package rcpa.project.entity.base;

import rcpa.project.repository.EnemyRepository;
import rcpa.project.repository.TowerRepository;
import rcpa.project.service.GameMaster;
import rcpa.project.util.MapUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Comparator;

import static rcpa.project.config.Configuration.CELL_WIDTH;

public class Tower<T extends Attack> extends JComponent implements Cloneable {
    private int id;
    private int marketId;
    private byte animationStatus=0;
    private double damage;
    private double radius;
    private double attackCooldown;
    private double lastAttackTime;
    private int cost;
    private int x;
    private int y;
    private int level;
    private int playerId;
    private boolean canAttack = false;
    private boolean isAnimation = false;
    private boolean isInSlot = true;
    private boolean canOccupe = false;
    private T attack;
    private Enemy target;
    private BufferedImage image;
    private ArrayList<BufferedImage> animation;
    private String name;

    public Tower(int marketId,
                 int cost,
                 double damage,
                 double radius,
                 double attackSpeed,
                 T attack,
                 BufferedImage image,
                 ArrayList<BufferedImage> animation,
                 String name) {
        this.marketId = marketId;
        this.cost = cost;
        this.damage = damage;
        this.radius = radius;
        this.attackCooldown = attackSpeed;
        this.attack = attack;
        this.image = image;
        this.animation = animation;
        this.name = name;
        setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(this.image, 0, 0, null);
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        Tower clone = (Tower) super.clone();
        clone.id = TowerRepository.getTowerRepository().getFreeId();
        return clone;
    }

    public boolean canAttack() {
        if (canAttack && this.target != null) {
            long currentTime = System.currentTimeMillis();
            if (currentTime - lastAttackTime >= attackCooldown*1000) {
                canAttack = false;
                return true;
            }
        }
        return false;
    }

    public void findTarget(ArrayList<Enemy> enemies){
        this.target = enemies.stream().filter(enemy -> {
            double distance = Math.sqrt(Math.pow((enemy.getX()+(double)CELL_WIDTH/2)-(x+(double)CELL_WIDTH/2),2)
                    +Math.pow((enemy.getY()+ (double) CELL_WIDTH /2)-(y+(double)CELL_WIDTH/2),2));
            return distance<=radius;
        }).max(Comparator.comparingDouble(Enemy::getWayPassed)).orElse(null);

        if(target!=null) attack.setTarget(target);
    }

    public boolean playAnimation(){
        if(animationStatus!=animation.size()-1){
            isAnimation = true;
            animationStatus++;
            this.image = animation.get(animationStatus);
            return false;
        }
        else {
            isAnimation = false;
            this.image = animation.get(0);
            animationStatus=0;
            return true;
        }
    }

    public BufferedImage rotateTower(ArrayList<Enemy> enemies){
        this.findTarget(enemies);

        if(this.getTarget()!=null) {
            double dx = this.getTarget().getX()  - this.getX();
            double dy = this.getTarget().getY()  - this.getY();
            double angle = Math.atan2(dy, dx) + Math.PI/2;
            return MapUtils.rotateImage(getImage(), angle);
        }
        return getImage();
    }

    public void setInSlot(boolean isInSlot) {
        this.isInSlot = isInSlot;
        this.canAttack = !isInSlot;
    }

    public boolean getIsAnimation(){return isAnimation;}
    public void setAnimation(boolean animation) {isAnimation = animation;}
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id=id;
    }
    public double getDamage() {
        return damage;
    }
    public Enemy getTarget() {
        return target;
    }
    public void setTarget(Enemy target) {this.target=target;}
    public boolean isCanAttack(){return canAttack;}
    public void setCanAttack(){
        canAttack=true;
    }
    public void setDamage(double damage) {
        if (damage >= 0) this.damage = damage;
    }
    public double getRadius() {
        return radius;
    }
    public void setRadius(double radius) {
        if(radius > 0) this.radius = radius;
    }
    public double getAttackCooldown() {
        return attackCooldown;
    }
    public void setAttackCooldown(double attackCooldown) {
        if(attackCooldown >0) this.attackCooldown = attackCooldown;
    }
    public BufferedImage getImage() {
        return image;
    }
    public void setImage(BufferedImage image) {
        this.image = image;
    }
    public ArrayList<BufferedImage> getAnimation() {
        return animation;
    }
    public void setAnimation(ArrayList<BufferedImage> animation) {
        this.animation = animation;
    }
    public String getName() {
        return name;
    }
    public void setName(String name) {
        if(!name.trim().isEmpty())this.name = name;
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
    public double getLastAttackTime() {
        return lastAttackTime;
    }
    public void setLastAttackTime(double lastAttackTime) {
        this.lastAttackTime = lastAttackTime;
    }
    public boolean isInSlot() {
        return this.isInSlot;
    }
    public boolean isCanOccupe() {
        return canOccupe;
    }
    public void setCanOccupe(boolean canOccupe) {
        this.canOccupe = canOccupe;
    }
    public T getAttack() {
        return attack;
    }
    public void setAttack(T attack) {
        this.attack = attack;
    }
    public int getLevel() {return level;}
    public void setLevel(int level) {this.level = level;}
    public int getPlayerId() {return playerId;}
    public void setPlayerId(int playerId) {this.playerId = playerId;}

    public int getMarketId() {
        return marketId;
    }

    public void setMarketId(int marketId) {
        this.marketId = marketId;
    }

    public int getCost() {
        return cost;
    }

    public void setCost(int cost) {
        this.cost = cost;
    }
}
