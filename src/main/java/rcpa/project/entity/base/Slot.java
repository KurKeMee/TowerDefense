package rcpa.project.entity.base;

import rcpa.project.service.GameMaster;
import rcpa.project.util.MapUtils;
import rcpa.project.view.GamePanel;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionAdapter;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static rcpa.project.config.Configuration.*;

public class Slot extends JComponent {
    private int slotId;
    private BufferedImage slotImage = ImageIO.read(new File(SLOT_IMAGE));
    private double currentTowerX;
    private double currentTowerY;
    private Tower tower;
    private boolean dragTower=false;

    public Slot(int slotId, Tower tower) throws IOException {
        this.slotId = slotId;
        this.tower = tower;
        currentTowerX = MapUtils.getMapUtils().getWidth() - TOWER_SLOT_RESIZING;
        currentTowerY = TOWER_SLOT_RESIZING * 2 + TOWER_SLOT_RESIZING*slotId;

        setPreferredSize(new Dimension(slotImage.getWidth(), slotImage.getHeight()));

        this.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                GameMaster.getGameMaster().renderSlotCostTable(getX()-80,getY()+TOWER_SLOT_RESIZING/3,tower.getCost());
            }
            public void mouseExited(MouseEvent e) {
                GameMaster.getGameMaster().renderSlotCostTable(0,0,0);
            }
            @Override
            public void mousePressed(MouseEvent e) {
                if(!GameMaster.getGameMaster().getPlayer().canAfford(tower.getCost())) return;
                dragTower=true;
                GameMaster.getGameMaster().setIsDragTower(true);
                try {
                    Tower newTower = (Tower) tower.clone();
                    GameMaster.getGameMaster().setDragTower(newTower);

                    Point globalMousePos = e.getLocationOnScreen();
                    SwingUtilities.convertPointFromScreen(globalMousePos, getParent());

                    newTower.setX(globalMousePos.x - CELL_WIDTH/2);
                    newTower.setY(globalMousePos.y - CELL_WIDTH/2);
                    //newTower.setBounds(newTower.getX(),newTower.getY(),newTower.getWidth(),newTower.getHeight());
                } catch (CloneNotSupportedException ex) {
                    throw new RuntimeException(ex);
                }


                SwingUtilities.getAncestorOfClass(GamePanel.class, Slot.this)
                        .dispatchEvent(SwingUtilities.convertMouseEvent(e.getComponent(), e, getParent()));
            }
            @Override
            public void mouseReleased(MouseEvent e) {
                dragTower=false;
                SwingUtilities.getAncestorOfClass(GamePanel.class, Slot.this)
                        .dispatchEvent(SwingUtilities.convertMouseEvent(e.getComponent(), e, getParent()));
            }
        });
        this.addMouseMotionListener(new MouseMotionAdapter() {
           @Override
           public void mouseDragged(MouseEvent e) {
               Tower newTower = GameMaster.getGameMaster().getDragTower();
               if(newTower!=null && !newTower.isCanOccupe()) {
                   Point globalMousePos = e.getLocationOnScreen();
                   SwingUtilities.convertPointFromScreen(globalMousePos, getParent());
                   newTower.setX(globalMousePos.x - CELL_WIDTH / 2);
                   newTower.setY(globalMousePos.y - CELL_WIDTH / 2);
                   newTower.setBounds(newTower.getX(), newTower.getY(), newTower.getWidth(), newTower.getHeight());
                   SwingUtilities.getAncestorOfClass(GamePanel.class, Slot.this)
                           .dispatchEvent(SwingUtilities.convertMouseEvent(e.getComponent(), e, getParent()));
               }
           }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(slotImage,0,0, null);
        g.drawImage(this.tower.getImage()
                .getScaledInstance(96,96,Image.SCALE_DEFAULT),0,0,null);
    }

    public void resetTowerCords(){
        currentTowerX = MapUtils.getMapUtils().getWidth() - TOWER_SLOT_RESIZING;
        currentTowerY = TOWER_SLOT_RESIZING * 2 + TOWER_SLOT_RESIZING*slotId;
    }

    public double getCurrentTowerY() {
        return currentTowerY;
    }

    public void setCurrentTowerY(double inc) {
        this.currentTowerY+=inc;
    }

    public double getCurrentTowerX() {
        return currentTowerX;
    }

    public void setCurrentTowerX(double inc) {
        this.currentTowerX+=inc;
    }

    public int getSlotId(){
        return this.slotId;
    }

    public Tower getTower(){
        return this.tower;
    }

    public boolean isDragTower() {
        return dragTower;
    }

    public void setDragTower(boolean dragTower) {
        this.dragTower = dragTower;
    }
}
