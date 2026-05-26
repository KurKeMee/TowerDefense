package rcpa.project.entity.base;

import rcpa.project.repository.SlotRepository;
import rcpa.project.repository.TowerRepository;

import java.io.IOException;

public class Player {
    private int id;
    private String name;
    private TowerRepository towerRepository;
    private SlotRepository slotRepository;
    private Slot grabbedSlot=null;
    private int money;

    public Player(String name, int money, TowerRepository towerRepository) {
        this.name = name;
        this.money = money;
        this.towerRepository = towerRepository;
        slotRepository = new SlotRepository();
        fillSlots();
    }


    private void fillSlots(){
        try {
            slotRepository.addNewSlot(new Slot(slotRepository.getFreeId(), towerRepository.getTowerMarket().get(0)));
            slotRepository.addNewSlot(new Slot(slotRepository.getFreeId(), towerRepository.getTowerMarket().get(1)));
            slotRepository.addNewSlot(new Slot(slotRepository.getFreeId(), towerRepository.getTowerMarket().get(2)));
            slotRepository.addNewSlot(new Slot(slotRepository.getFreeId(), towerRepository.getTowerMarket().get(3)));
            slotRepository.addNewSlot(new Slot(slotRepository.getFreeId(), towerRepository.getTowerMarket().get(4)));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean canAfford(int price){
        return money >= price;
    }

    public String getName() {
        return name;
    }

    public void setMoney(int money) {
        this.money = money;
    }
    public int getMoney() {
        return money;
    }

    public void spendMoney(int moneyToSpend) {
        if(!canAfford(moneyToSpend)) return;
        this.money -= moneyToSpend;
    }

    public void earnMoney(int moneyToEarn) {
        this.money += moneyToEarn;
    }

    public SlotRepository getSlotRepository() {
        return slotRepository;
    }

    public TowerRepository getTowerRepository(){
        return towerRepository;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }
}
