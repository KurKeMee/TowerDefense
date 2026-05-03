package rcpa.project.entity.base;

import rcpa.project.repository.EnemyRepository;

import java.awt.image.BufferedImage;
import java.util.ArrayList;

import static rcpa.project.config.Configuration.CELL_WIDTH;

public class Enemy implements Cloneable{
    private int id;
    private byte animationStatus=0;
    private double health;
    private double maxHealth;
    private double speed;
    private boolean canMove = true;
    private int wayProgress = 0;
    private String name;
    private BufferedImage image;
    private int lookOrientation;
    private int x;
    private int y;
    private Cell currentPosition;
    private Cell targetPosition;
    private ArrayList<BufferedImage> animation;
    private ArrayList<Cell> way;


    public Enemy(byte id,
                 double maxHealth,
                 double speed,
                 BufferedImage image,
                 ArrayList<BufferedImage> animation,
                 String name) {
        this.id = id;
        this.maxHealth = maxHealth;
        this.health = maxHealth;
        this.speed = speed;
        this.image = image;
        this.name = name;
        this.animation = animation;
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        Enemy enemy = (Enemy) super.clone();
        enemy.id = EnemyRepository.getEnemyRepository().getFreeId();
        return enemy;
    }

    public void move(){
        if(this.currentPosition.cellEquals(this.targetPosition)){
            wayProgress++;
            this.targetPosition = getNextStep();
        }
        double[] orientation = this.currentPosition.cellReach(this.targetPosition);
        orientation[0] *= this.speed;
        orientation[1] *= this.speed;
        x+= (int) orientation[0];
        y+= (int) orientation[1];
        if(targetPosition.getXCord()*CELL_WIDTH%x<speed && targetPosition.getYCord()*CELL_WIDTH%y<speed){
            currentPosition = targetPosition;
            x=currentPosition.getXCord()*CELL_WIDTH;
            y=currentPosition.getYCord()*CELL_WIDTH;
        }

        if (Math.abs(orientation[0]) > Math.abs(orientation[1])) {
            lookOrientation = orientation[0] < 0 ? 1 : 3; // 1:влево, 3:вправо
        } else {
            lookOrientation = orientation[1] < 0 ? 2 : 0; // 2:вверх, 0:вниз
        }
    }

    public boolean playAnimation(){
        if(animationStatus!=animation.size()-1){
            animationStatus++;
            this.image = animation.get(animationStatus);
            return false;
        }
        else {
            this.image = animation.getFirst();
            animationStatus=0;
            return true;
        }
    }



    public double getHealth() {
        return health;
    }

    public void takeDamage(double damage) {
        if (damage > 0 && damage < 100000) this.health -= damage;
        if(this.health<0.1) this.health = 0;
    }

    public int getId() {
        return id;
    }

    public double getSpeed() {
        return speed;
    }

    public void setSpeed(double speed) {
        if(speed >= 0) this.speed = speed;
    }

    public boolean isCanMove() {
        return canMove;
    }

    public void setCanMove(boolean canMove) {
        this.canMove = canMove;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        if(!name.trim().isEmpty()) this.name = name;
    }

    public Cell getNextStep() {
        if (wayProgress < way.size()) return way.get(wayProgress);
        return null;
    }

    public Cell getCurrentPosition() {
        return currentPosition;
    }

    public void setCurrentPosition(Cell currentPosition) {
        this.currentPosition = currentPosition;
    }

    public ArrayList<Cell> getWay(){
        return way;
    }

    public void setWay(ArrayList<Cell> way) {
        this.way = way;
        this.currentPosition = way.getFirst();
        this.targetPosition = way.getFirst();
        x=currentPosition.getXCord() * CELL_WIDTH;
        y=currentPosition.getYCord() * CELL_WIDTH;
    }

    public BufferedImage getImage() {
        return this.image;
    }

    public int getX() {
        return x;
    }
    public void setId(int id) {
        this.id = id;
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

    public double getMaxHealth() {
        return maxHealth;
    }

    public void setMaxHealth(double maxHealth) {
        this.maxHealth = maxHealth;
    }

    public int getLookOrientation() {
        return lookOrientation;
    }

    public void setLookOrientation(int lookOrientation) {
        this.lookOrientation = lookOrientation;
    }
}
