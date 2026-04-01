package rcpa.project.entity.base;

import rcpa.project.repository.SlotRepository;
import rcpa.project.repository.TowerRepository;

import java.io.IOException;

public class Player {
    private String name;
    private int money;
    private TowerRepository towerRepository; // TODO: сделать для каждого свой
    private SlotRepository slotRepository;
    private Slot grabbedSlot=null;

    public Player(String name, int money, TowerRepository towerRepository) {
        this.name = name;
        this.money = money;
        this.towerRepository = towerRepository;
        slotRepository = new SlotRepository();
        fillSlots();
    }


    public Tower spawnTower(){
        if(grabbedSlot!=null){
            Tower placeTower = grabbedSlot.getTower();
            grabbedSlot = null;
            return placeTower;
        }
        return null;
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

    public String getName() {
        return name;
    }

    public int getMoney() {
        return money;
    }

    public void setMoney(int moneyInc) {
        if(this.money-moneyInc<0) return;
        this.money -= moneyInc;
    }

    public SlotRepository getSlotRepository() {
        return slotRepository;
    }

    public TowerRepository getTowerRepository(){
        return towerRepository;
    }
}
