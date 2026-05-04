package rcpa.project.entity.base;

import rcpa.project.service.GameMaster;
import rcpa.project.view.GamePanel;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.util.Arrays;

import static rcpa.project.config.Configuration.CELL_WIDTH;

public class Cell extends JComponent {
    private int xCord;
    private int yCord;
    private boolean occupied;
    private boolean isRoad;
    private boolean isHomePortal;
    private boolean isEnemyPortal;
    private BufferedImage image;

    public Cell(int xCord,
                int yCord,
                boolean isRoad,
                boolean isHomePortal,
                boolean isEnemyPortal,
                BufferedImage image) {
        this.xCord = xCord;
        this.yCord = yCord;
        this.isRoad = isRoad;
        this.isHomePortal = isHomePortal;
        this.isEnemyPortal = isEnemyPortal;
        this.image = image;
        if(isEnemyPortal || isHomePortal || isRoad) {
            this.isRoad = true;
            occupied = true;
        }

        this.addMouseListener(new MouseAdapter() {
           @Override
           public void mouseEntered(MouseEvent e) {
               Tower newTower = GameMaster.getGameMaster().getDragTower();
                if(newTower != null && !occupied) {
                    newTower.setX(xCord*CELL_WIDTH);
                    newTower.setY(yCord*CELL_WIDTH);
                    newTower.setBounds(newTower.getX(),newTower.getY(), image.getWidth(), image.getHeight());
                    newTower.setCanOccupe(true);
                }
           }

            @Override
            public void mouseExited(MouseEvent e) {
                Tower newTower = GameMaster.getGameMaster().getDragTower();
                if(newTower != null && newTower.isCanOccupe()) {
                    GameMaster.getGameMaster().getDragTower().setCanOccupe(false);

                    SwingUtilities.getAncestorOfClass(GamePanel.class, Cell.this)
                            .dispatchEvent(SwingUtilities.convertMouseEvent(e.getComponent(), e, getParent()));

                    Slot ownSlot = Arrays.stream(getParent().getComponents())
                            .filter(c -> c instanceof Slot && ((Slot) c).isDragTower())
                            .map(s -> ((Slot) s))
                            .findFirst()
                            .orElse(null);

                    ownSlot.dispatchEvent(SwingUtilities.convertMouseEvent(e.getComponent(), new MouseEvent(
                            ownSlot,
                            MouseEvent.MOUSE_DRAGGED,
                            e.getWhen(),
                            e.getModifiersEx(),
                            e.getX(),
                            e.getY(),
                            e.getClickCount(),
                            e.isPopupTrigger()
                    ), ownSlot));
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        g.drawImage(image, 0,0, null);
    }

    public boolean cellEquals(Cell cell) {
        return this.xCord == cell.xCord && this.yCord == cell.yCord;
    }

    public double[] cellReach(Cell cell) {
        double[] orientation = new double[2];
        if(this.xCord < cell.xCord) orientation[0] = 1;
        else if(this.xCord > cell.xCord) orientation[0] = -1;
        else if(this.yCord < cell.yCord) orientation[1] = 1;
        else if(this.yCord > cell.yCord) orientation[1] = -1;
        return orientation;
    }

    public int getXCord() {
        return xCord;
    }

    public int getYCord() {
        return yCord;
    }

    public boolean isRoad() {
        return isRoad;
    }

    public boolean isHomePortal() {
        return isHomePortal;
    }

    public Image getImage() {
        return image;
    }

    public void setImage(BufferedImage image) {
        this.image = image;
    }

    public boolean isEnemyPortal() {
        return isEnemyPortal;
    }

    public boolean isOccupied() {
        return occupied;
    }

    public void occupeCell() {
        this.occupied = true;
    }

    public void freeCell() {
        this.occupied = false;
    }
}
