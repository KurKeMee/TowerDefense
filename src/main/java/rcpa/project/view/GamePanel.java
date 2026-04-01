package rcpa.project.view;

import rcpa.project.entity.base.Tower;
import rcpa.project.repository.CellRepository;
import rcpa.project.service.GameMaster;
import rcpa.project.util.MapUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import static rcpa.project.config.Configuration.*;

/**
 * @author Ivan Monin
 *
 * Класс для отрисовки панели на окне
 */
public class GamePanel extends JPanel implements ActionListener, MouseMotionListener, MouseListener {

    /**
     * Переменная хранящая экземпляр класса GameMaster
     *
     * @see GameMaster
     */
    private final GameMaster gameMaster;


    /**
     * Переменная для хранения экземпляра класса MapUtils
     *
     * @see MapUtils
     */
    private final MapUtils mapUtils;

    private JPanel glassPane;

    /**
     * Конструктор с инициализацией элементов на панели
     *
     * @param frame - передаваемый параметр окна
     */
    public GamePanel(JFrame frame) {
        Timer timer = new Timer(MILLISECONDS_PER_FRAME,this);
        this.gameMaster = GameMaster.getGameMaster();
        this.mapUtils = MapUtils.getMapUtils();
        this.addMouseListener(this);
        this.addMouseMotionListener(this);

        drawMap();
        initPanel(frame);
        timer.start();
    }

    /**
     * Метод инициализации панели
     * @param frame - передаваемый параметр окна
     */
    private void initPanel(JFrame frame){
        glassPane = new JPanel(null) {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                gameMaster.renderFrame(g);
            }
        };
        glassPane.setOpaque(false);
        frame.setGlassPane(glassPane);
        glassPane.setVisible(true);

        this.setBounds(0, 0, this.mapUtils.getWidth(), this.mapUtils.getHeight());
        setLayout(null);

        gameMaster.getPlayer().getSlotRepository().getSlots().forEach(slot -> {
            slot.setBounds(
                    MapUtils.getMapUtils().getWidth() - TOWER_SLOT_RESIZING,
                    TOWER_SLOT_RESIZING * 2 + TOWER_SLOT_RESIZING * slot.getSlotId(),
                    slot.getPreferredSize().width,
                    slot.getPreferredSize().height
            );
            this.add(slot);
        });

        CellRepository.getCellRepository().getCells().forEach(cell ->{
            cell.setBounds(cell.getXCord()*CELL_WIDTH,cell.getYCord()*CELL_WIDTH,CELL_WIDTH,CELL_WIDTH);
            this.setComponentZOrder(cell,0);
            this.add(cell);
        });

        frame.setSize(this.mapUtils.getWidth(), this.mapUtils.getHeight());
        frame.setLocationRelativeTo(null);
    }


    /**
     * Метод отрисовки карты
     */
    private void drawMap(){
        mapUtils.analyzeMap(MAP_1);
    }

    /**
     * Метод для перерисовки окна
     * @param e the event to be processed
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        repaint();
    }


    @Override
    public void mouseDragged(MouseEvent e) {

    }

    @Override
    public void mouseMoved(MouseEvent e) {}

    @Override
    public void mouseClicked(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {
        Tower newTower = gameMaster.getDragTower();
        if(newTower != null) {
            newTower.setBounds(newTower.getX(),newTower.getY(),newTower.getWidth(),newTower.getHeight());
            this.add(newTower);
            gameMaster.setIsDragTower(false);
            gameMaster.setDragTower(null);
            gameMaster.getPlayer().getTowerRepository().addNewTower(newTower);

        }
        this.revalidate();
        this.repaint();
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }
}
