package rcpa.project.repository;

import rcpa.project.entity.base.Attack;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Ivan Monin
 *
 * Класс для хранения атак
 */
public class AttackRepository {
    /**
     * Переменная для хранения экземпляра класса AttackRepository
     *
     * @see AttackRepository
     */
    private static AttackRepository instance;


    /**
     * Массив для хранения атак
     *
     * @see HashMap
     * @see Attack
     */
    private HashMap<Integer,Attack> attacks;

    /**
     * Конструктор класса TowerRepository
     * Конструктор объявлен private для паттерна Singleton
     *
     * @see AttackRepository#getAttackRepository()
     */
    private AttackRepository() {
        attacks = new HashMap<>();
    }

    /**
     * Метод для получения единственного экземпляра класса
     * Параметр synchronized необходим для исключения ситуации множественного создания экземляров класса
     *
     * @see AttackRepository#AttackRepository()
     * @return AttackRepository - возвращает единственный экземпляр класса
     */
    public static synchronized AttackRepository getAttackRepository() {
        if (instance == null) {
            instance = new AttackRepository();
        }
        return instance;
    }

    /**
     * Метод добавления атаки в репозиторий {@link #attacks}
     *
     * @param attack - созданная атаки
     */
    public void addNewAttack(Attack attack) {
        attacks.put(attack.getId(),attack);
    }

    /**
     * Метод получения атаки по id
     * @param id - идентификтор атаки
     * @return Tower - возвращает атаку
     */
    public Attack getAttack(int id) {
        return attacks.getOrDefault(id, null);
    }

    public void clear(){
        attacks.clear();
    }

    /**
     * Метод получения всех атак
     * @return ArrayList<Attack> - возвращает список атак
     */
    public ArrayList<Attack> getAttacks() {
        return new ArrayList<>(attacks.values());
    }

    /**
     * Метод удаления атаки по id
     * @param id - идентификтор атаки
     * @return boolean - возвращает удалена ли атака
     */
    public boolean deleteAttack(int id) {
        attacks.remove(id);
        return getAttack(id) == null;
    }

    /**
     * Метод получения свободного Id {@link #attacks}
     *
     */
    public int getFreeId() {
        int random = (int)(Math.random()*Integer.MAX_VALUE-Integer.MAX_VALUE);
        while(attacks.containsKey(random)) {
            random =  (int)(Math.random()*Integer.MAX_VALUE-Integer.MAX_VALUE);
        }
        return random;
    }
}
