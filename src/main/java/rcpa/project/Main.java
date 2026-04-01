package rcpa.project;

import rcpa.project.config.Configuration;
import rcpa.project.view.GamePanel;

import javax.swing.*;

/**
 * @author Ivan Monin
 * Главный метод программы
 * Создаётся окно {@link JFrame} и в него добавляется панель {@link GamePanel}
 */
public class Main {
    public static void main(String[] args) {
        JFrame frame = new JFrame(Configuration.GAME_NAME);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setResizable(false);
        frame.setSize(10, 10);
        frame.setLayout(null);
        frame.add(new GamePanel(frame));
        frame.setVisible(true);
    }
}