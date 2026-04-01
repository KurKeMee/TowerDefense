package rcpa.project.repository;

import rcpa.project.entity.base.Cell;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Ivan Monin
 * <p>
 * Класс для хранения ячеек
 */
public class CellRepository {
    /**
     * Переменная для хранения экземпляра класса CellRepository
     *
     * @see CellRepository
     */
    private static CellRepository instance;


    /**
     * Массив для хранения ячеек
     *
     * @see HashMap
     * @see Cell
     */
    private HashMap<String, Cell> cells;

    /**
     * Конструктор класса CellRepository
     * Конструктор объявлен private для паттерна Singleton
     *
     * @see CellRepository#getCellRepository()
     */
    private CellRepository() {
        cells = new HashMap<>();
    }

    /**
     * Метод для получения единственного экземпляра класса
     * Параметр synchronized необходим для исключения ситуации множественного создания экземляров класса
     *
     * @return CellRepository - возвращает единственный экземпляр класса
     * @see CellRepository#CellRepository()
     */
    public static synchronized CellRepository getCellRepository() {
        if (instance == null) {
            instance = new CellRepository();
        }
        return instance;
    }

    /**
     * Метод добавления ячейки в репозиторий {@link #cells}
     *
     * @param cell - созданная ячейка
     */
    public void addNewCell(Cell cell) {
        cells.put(String.format("%d;%d", cell.getXCord(), cell.getYCord()), cell);
    }

    /**
     * Метод построения пути для врагов
     *
     * @return ArrayList<Cell> - возвращает список клеток
     */
    public ArrayList<Cell> buildPath() {
        ArrayList<Cell> path = new ArrayList<>();

        Cell startCell = cells.values().stream()
                .filter(Cell::isEnemyPortal)
                .findFirst()
                .orElse(null);

        Cell endCell = null;

        Cell currentCell = startCell;
        Cell lastCell = currentCell;

        while (endCell == null) {
            path.add(currentCell);
            Cell topCell = cells.getOrDefault(String.format("%d;%d", currentCell.getXCord(), currentCell.getYCord()-1), null);
            Cell bottomCell = cells.getOrDefault(String.format("%d;%d", currentCell.getXCord(), currentCell.getYCord()+1), null);
            Cell leftCell = cells.getOrDefault(String.format("%d;%d", currentCell.getXCord()-1, currentCell.getYCord()), null);
            Cell rightCell = cells.getOrDefault(String.format("%d;%d", currentCell.getXCord()+1, currentCell.getYCord()), null);

            if (topCell != null
                    && topCell.isRoad()
                    && !lastCell.cellEquals(topCell)) {
                lastCell = currentCell;
                currentCell = topCell;
            } else if (bottomCell != null
                    && bottomCell.isRoad()
                    && !lastCell.cellEquals(bottomCell)) {
                lastCell = currentCell;
                currentCell = bottomCell;
            } else if (leftCell != null
                    && leftCell.isRoad()
                    && !lastCell.cellEquals(leftCell)) {
                lastCell = currentCell;
                currentCell = leftCell;
            } else if (rightCell != null
                    && rightCell.isRoad()
                    && !lastCell.cellEquals(rightCell)) {
                lastCell = currentCell;
                currentCell = rightCell;
            }

            if (currentCell.isHomePortal())
                endCell = currentCell;
        }

        path.add(endCell);
        return path;
    }

    /**
     * Метод получения ячейки по x и y
     *
     * @param x - положение ячейки по x
     * @param y - положение ячейки по y
     * @return Cell - возвращает ячейку
     */
    public Cell getCell(int x, int y) {
        return cells.getOrDefault(String.format("%d;%d", x, y), null);
    }

    /**
     * Метод получения всех ячеек
     *
     * @return ArrayList<Cells> - возвращает список ячеек
     */
    public ArrayList<Cell> getCells() {
        return new ArrayList<>(cells.values());
    }
}
