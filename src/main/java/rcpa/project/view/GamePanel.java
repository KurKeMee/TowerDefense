package rcpa.project.view;

import rcpa.project.entity.base.Tower;
import rcpa.project.model.GameState;
import rcpa.project.network.NetworkUtils;
import rcpa.project.repository.CellRepository;
import rcpa.project.service.GameMaster;
import rcpa.project.util.MapUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;

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

    private GameStatus currentGameStatus;


    /**
     * Переменная для хранения экземпляра класса MapUtils
     *
     * @see MapUtils
     */
    private final MapUtils mapUtils;

    private JFrame frame;

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

        this.frame = frame;

        initPanel();
        timer.start();
    }

    /**
     * Метод инициализации панели
     */
    public void initPanel() {
        resetComponents(frame);
        this.setBounds(0, 0,1024, 800);
        this.setLayout(null);
        frame.setSize(1024,800);
        frame.setLocationRelativeTo(null);

        JButton butLevels = new JButton(LEVEL_BUTTON_TEXT_RU){
            @Override
            protected void paintComponent(Graphics g) {
                try {
                    Image bg = ImageIO.read(new File(LEVELS_BUTTON_IMAGE));
                    g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                g.setColor(Color.ORANGE);
                g.setFont(new Font("Gabriola", Font.BOLD, 50));
                FontMetrics fm = g.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() - fm.getHeight()) + fm.getAscent();
                g.drawString(getText(), x, y+12);
            }
        };
        JButton butExit = new JButton(EXIT_BUTTON_TEXT_RU){
            @Override
            protected void paintComponent(Graphics g) {
                // Рисуем фон
                try {
                    Image bg = ImageIO.read(new File(EXIT_BUTTON_IMAGE));
                    g.drawImage(bg, 0, 0, getWidth(), getHeight(), this);
                } catch (IOException e) {
                    e.printStackTrace();
                }

                // Рисуем текст по центру
                g.setColor(Color.orange);
                g.setFont(new Font("Gabriola", Font.BOLD, 46));
                FontMetrics fm = g.getFontMetrics();
                int x = (getWidth() - fm.stringWidth(getText())) / 2;
                int y = (getHeight() - fm.getHeight()) + fm.getAscent();
                g.drawString(getText(), x, y+10);
            }
        };

        butLevels.setContentAreaFilled(false);  // Убираем фон
        butLevels.setBorderPainted(false);      // Убираем границы
        butLevels.setFocusPainted(false);       // Убираем рамку фокуса
        butLevels.setOpaque(false);             // Делаем кнопку прозрачной

        butExit.setContentAreaFilled(false);
        butExit.setBorderPainted(false);
        butExit.setFocusPainted(false);
        butExit.setOpaque(false);

        butLevels.setIcon(new ImageIcon(LEVELS_BUTTON_IMAGE));
        butExit.setIcon(new ImageIcon(EXIT_BUTTON_IMAGE));

        butLevels.setBounds(100,400,384,96);
        butExit.setBounds(100,600,384,96);

        JButton butHost = new JButton("Создать игру");
        butHost.setBounds(100, 500, 384, 48);
        butHost.addActionListener(_ -> {
            String roomName = gameMaster.createMultiplayerRoom();
            if (roomName != null) {
                JOptionPane.showMessageDialog(frame,
                        "Игра создана!\nНазвание: " + roomName +
                                "\n\nВторой игрок должен ввести это название");
            }
        });
        this.add(butHost);

        JButton butJoin = new JButton("Присоединиться");
        butJoin.setBounds(100, 560, 384, 48);
        butJoin.addActionListener(_ -> {
            String roomName = JOptionPane.showInputDialog(frame, "Введите название комнаты:");
            if (roomName != null && !roomName.isEmpty()) {
                if (gameMaster.connectToServer("127.0.0.1", 8080)) {
                    gameMaster.getGameClient().joinRoom(roomName);
                }
            }
        });
        this.add(butJoin);
        JButton butStartGame = new JButton("Начать игру");
        butStartGame.setBounds(100, 460, 384, 48);
        butStartGame.addActionListener(_ -> {
            if (gameMaster.getGameClient() != null) {
                gameMaster.getGameClient().startGame();
                System.out.println("Запрос на старт игры отправлен");
            }
        });
        this.add(butStartGame);

        butLevels.addActionListener(_ -> gameMaster.setGameStatus(GameStatus.MAIN_MENU_LEVEL));

        butExit.addActionListener(_ -> System.exit(0));


        this.add(butLevels);
        this.add(butExit);
    }

    public void levelLoad(){
        resetComponents(frame);

        drawMap();
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
            //this.setComponentZOrder(cell,0);
            this.add(cell);
        });

        frame.setSize(this.mapUtils.getWidth(), this.mapUtils.getHeight());
        frame.setLocationRelativeTo(null);
    }

    private void resetComponents(JFrame frame){
        this.removeAll();
        this.revalidate();
        this.repaint();

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
    }


    /**
     * Метод отрисовки карты
     */
    private void drawMap(){
        mapUtils.analyzeMap(MAP_1);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        try {
            g.drawImage(ImageIO.read(new File(MENU_BACKGROUND_IMAGE)),0,0,null);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        gameMaster.renderFrame(g);
    }

    /**
     * Метод для перерисовки окна
     * @param e the event to be processed
     */
    @Override
    public void actionPerformed(ActionEvent e) {
        if(currentGameStatus!=gameMaster.getGameStatus()){
            currentGameStatus = gameMaster.getGameStatus();
            switch(currentGameStatus){
                case WAVE_STARTING:
                case LEVEL_ENTER:
                    frame.setIconImage(null);
                    levelLoad();
                    break;
                default:
                    break;
            }
        }
        if(gameMaster.getTowerToUpdate()!=null &&
                (currentGameStatus==GameStatus.WAVE_STARTING ||
                currentGameStatus==GameStatus.WAVE_STARTED ||
                currentGameStatus==GameStatus.WAVE_END)){
            this.add(gameMaster.getTowerToUpdate());
            gameMaster.getPlayer().getTowerRepository().addNewTower(gameMaster.getTowerToUpdate());
            gameMaster.clearTowerToUpdate();
        }
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
            if(newTower.isCanOccupe()){
                newTower.setBounds(newTower.getX(),newTower.getY(),newTower.getWidth(),newTower.getHeight());
                this.add(newTower);
                gameMaster.getPlayer().getTowerRepository().addNewTower(newTower);
                CellRepository.getCellRepository().getCell(newTower.getX()/CELL_WIDTH,newTower.getY()/CELL_WIDTH).occupeCell();

                if (gameMaster.isMultiplayer()) {
                    gameMaster.sendTowerPlaced(newTower);
                }

            }
            gameMaster.setIsDragTower(false);
            gameMaster.setDragTower(null);
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
