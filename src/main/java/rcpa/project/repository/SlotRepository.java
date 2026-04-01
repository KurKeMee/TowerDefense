package rcpa.project.repository;

import rcpa.project.entity.base.Slot;

import java.util.ArrayList;

/**
 * @author Ivan Monin
 * <p>
 * Класс для хранения ячеек
 */
public class SlotRepository {
    /**
     * Массив для хранения слотов
     *
     * @see ArrayList
     * @see Slot
     */
    private ArrayList<Slot> slots = new ArrayList<>();

    private int lastId=-1;


    /**
     * Метод добавления слота в репозиторий {@link #slots}
     *
     * @param slot - созданный слот
     */
    public void addNewSlot(Slot slot) {
        slots.add(slot);
    }

    /**
     * Метод получения слота по Id
     *
     * @param slotId - Id слота
     * @return Slot - возвращает слот
     */
    public Slot getSlot(int slotId) {
        return slots.stream()
                .filter(slot -> slotId == slot.getSlotId())
                .findFirst()
                .orElse(null);
    }

    /**
     * Метод получения всех слотов
     *
     * @return ArrayList<Slot> - возвращает слоты
     */
    public ArrayList<Slot> getSlots() {
        return slots;
    }

    /**
     * Метод получения свободного Id {@link #slots}
     *
     */
    public int getFreeId() {
        lastId++;
        return lastId;
    }
}
