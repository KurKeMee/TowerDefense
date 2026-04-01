package rcpa.project.util;

import rcpa.project.entity.base.Cell;
import rcpa.project.repository.CellRepository;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static rcpa.project.config.Configuration.*;

public class MapUtils {

    /**
     * Переменная для хранения экземпляра класса MapUtils
     *
     * @see MapUtils
     */
    private static MapUtils mapUtils;

    private int width;
    private int height;

    /**
     * Конструктор класса MapUtils
     * Конструктор объявлен private для паттерна Singleton
     *
     * @see MapUtils#MapUtils()
     */
    private MapUtils(){}

    /**
     * Метод для получения единственного экземпляра класса
     * Параметр synchronized необходим для исключения ситуации множественного создания экземляров класса
     *
     * @see MapUtils#MapUtils()
     * @return MapUtils - возвращает единственный экземпляр класса
     */
    public static synchronized MapUtils getMapUtils(){
        if(mapUtils == null){
            mapUtils = new MapUtils();
        }
        return mapUtils;
    }

    public boolean analyzeMap(String path){
        CellRepository cellRepository = CellRepository.getCellRepository();
        File file = new File(path);
        try {
            BufferedImage image = ImageIO.read(file);
            this.width = image.getWidth();
            this.height = image.getHeight();
            for(int x=0;x<this.width;x++){
                for(int y=0;y<this.height;y++){
                    Color color = new Color(image.getRGB(x,y));

                    if(color.getRed()==255
                            && color.getGreen()==255
                            && color.getBlue()==255)
                        cellRepository.addNewCell(new Cell(
                                x,
                                y,
                                true,
                                false,
                                false,
                                ImageIO.read(new File(ROAD_CELL))));

                    else if(color.getRed()==0
                                && color.getGreen()==0
                                && color.getBlue()==0)
                        cellRepository.addNewCell(new Cell(x,
                                y,
                                false,
                                false,
                                false,
                                ImageIO.read(new File(GRASS_CELL))));

                    else if(color.getRed()==0
                            && color.getGreen()==0
                            && color.getBlue()==255)
                        cellRepository.addNewCell(new Cell(x,
                                y,
                                true,
                                true,
                                false,
                                ImageIO.read(new File(HOME_PORTAL_CELL))));

                    else if(color.getRed()==255
                            && color.getGreen()==0
                            && color.getBlue()==0)
                        cellRepository.addNewCell(new Cell(x,
                                y,
                                true,
                                false,
                                true,
                                ImageIO.read(new File(ENEMY_PORTAL_CELL))));
                }
            }
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public static BufferedImage rotateImage(BufferedImage image, double angle) {
        int w = image.getWidth();
        int h = image.getHeight();

        BufferedImage rotated = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2d = rotated.createGraphics();
        g2d.setBackground(new Color(0, 0, 0, 0));
        g2d.clearRect(0, 0, w, h);

        g2d.setComposite(AlphaComposite.SrcOver);

        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING,
                RenderingHints.VALUE_RENDER_QUALITY);

        g2d.translate(w / 2, h / 2);
        g2d.rotate(angle);
        g2d.translate(-w / 2, -h / 2);
        g2d.drawImage(image, 0, 0, null);
        g2d.dispose();

        return rotated;
    }

    public int getWidth() {
        return width * CELL_WIDTH;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height * CELL_WIDTH;
    }

    public void setHeight(int height) {
        this.height = height;
    }
}
