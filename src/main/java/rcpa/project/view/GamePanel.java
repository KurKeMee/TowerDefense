package rcpa.project.view;

import rcpa.project.entity.base.Tower;
import rcpa.project.model.Message;
import rcpa.project.repository.CellRepository;
import rcpa.project.repository.TowerRepository;
import rcpa.project.service.GameMaster;
import rcpa.project.util.MapUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.Map;

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

    private boolean isMultiplayerGame = false;

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

        butLevels.addActionListener(_ -> gameMaster.setGameStatus(GameStatus.MAIN_MENU_LEVEL));

        butExit.addActionListener(_ -> System.exit(0));


        this.add(butLevels);
        this.add(butExit);

        JButton butSinglePlayer = new JButton("Одиночная игра");
        butSinglePlayer.setBounds(100, 300, 384, 48);
        butSinglePlayer.addActionListener(_ -> {
            isMultiplayerGame = false;
            gameMaster.setGameStatus(GameStatus.MAIN_MENU_LEVEL);
        });
        this.add(butSinglePlayer);

        JButton butHost = new JButton("Создать игру");
        butHost.setBounds(100, 200, 384, 48);
        butHost.addActionListener(_ -> {
            // Запускаем сервер локально
            gameMaster.startServer();

            // Подключаемся к своему серверу
            if (gameMaster.connectToServer("127.0.0.1", 8080)) {
                String roomName = gameMaster.createMultiplayerRoom();
                if (roomName != null) {
                    isMultiplayerGame = true;
                    JOptionPane.showMessageDialog(frame,
                            "Игра создана!\nНазвание комнаты: " + roomName +
                                    "\n\nОжидание второго игрока...");
                }
            }
        });
        this.add(butHost);

        JButton butJoin = new JButton("Присоединиться");
        butJoin.setBounds(100, 260, 384, 48);
        butJoin.addActionListener(_ -> {
            String serverIP = JOptionPane.showInputDialog(frame,
                    "Введите IP сервера:", "127.0.0.1");
            if (serverIP != null && !serverIP.isEmpty()) {
                if (gameMaster.connectToServer(serverIP, 8080)) {
                    String roomId = JOptionPane.showInputDialog(frame,
                            "Введите ID комнаты:");
                    if (roomId != null && !roomId.isEmpty()) {
                        gameMaster.getGameClient().sendRequest(
                                Message.Type.JOIN_ROOM,
                                Map.of("roomId", roomId)
                        );
                        isMultiplayerGame = true;
                    }
                }
            }
        });
        this.add(butJoin);

        JButton butStartGame = new JButton("Начать игру");
        butStartGame.setBounds(100, 320, 384, 48);
        butStartGame.addActionListener(_ -> {
            if (gameMaster.getGameClient() != null && gameMaster.isMultiplayer()) {
                gameMaster.getGameClient().sendRequest(Message.Type.START_GAME, null);
                System.out.println("Запрос на старт игры отправлен");
            }
        });
        this.add(butStartGame);
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

        CellRepository.getCellRepository().getCells().forEach(cell -> {
            cell.setBounds(cell.getXCord() * CELL_WIDTH,
                    cell.getYCord() * CELL_WIDTH,
                    CELL_WIDTH, CELL_WIDTH);
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
        GameStatus newStatus = gameMaster.getGameStatus();
        if (currentGameStatus != newStatus) {
            currentGameStatus = newStatus;

            switch (currentGameStatus) {
                case WAVE_STARTING:
                case LEVEL_ENTER:
                    frame.setIconImage(null);
                    levelLoad();
                    break;
                default:
                    break;
            }
        }

        if (gameMaster.getTowerToUpdate() != null &&
                (currentGameStatus == GameStatus.WAVE_STARTING ||
                        currentGameStatus == GameStatus.WAVE_STARTED ||
                        currentGameStatus == GameStatus.WAVE_END)) {
            Tower tower = gameMaster.getTowerToUpdate();
            this.add(tower);
            TowerRepository.getTowerRepository().addNewTower(tower);
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
        if (newTower != null) {
            if (newTower.isCanOccupe()) {
                final Tower towerToPlace = newTower;
                // Process tower placement asynchronously
                SwingUtilities.invokeLater(() -> {
                    towerToPlace.setBounds(towerToPlace.getX(), towerToPlace.getY(),
                            towerToPlace.getWidth(), towerToPlace.getHeight());
                    this.add(towerToPlace);
                    TowerRepository.getTowerRepository().addNewTower(towerToPlace);
                    CellRepository.getCellRepository()
                            .getCell(towerToPlace.getX() / CELL_WIDTH,
                                    towerToPlace.getY() / CELL_WIDTH)
                            .occupeCell();

                    if (gameMaster.isMultiplayer()) {
                        gameMaster.sendTowerPlaced(towerToPlace);
                    }

                    this.revalidate();
                    this.repaint();
                });
            }
            gameMaster.setIsDragTower(false);
            gameMaster.setDragTower(null);
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }
}
