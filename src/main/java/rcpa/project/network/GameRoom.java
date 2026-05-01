package rcpa.project.network;

import rcpa.project.model.GameState;
import rcpa.project.model.Message;
import rcpa.project.model.PlayerInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class GameRoom {
    private String roomId;
    private String roomName;
    private int hostId;
    private GameServer server;
    private Map<Integer,ClientHandler> players;
    private boolean gameStarted;
    private GameState currentState;
    private int waveLevel;
    private long startTime;

    public GameRoom(String roomId, String roomName, int hostId, GameServer server) {
        this.roomId = roomId;
        this.roomName = roomName;
        this.hostId = hostId;
        this.server = server;
        this.players = new ConcurrentHashMap<>();
        this.gameStarted = false;
        this.currentState = new GameState();
        this.waveLevel = 0;
    }

    public void addPlayer(int playerId, ClientHandler handler) {
        players.put(playerId, handler);
        PlayerInfo player = new PlayerInfo(playerId,handler.getPlayerName(),null,0);
        currentState.getPlayers().put(playerId,new GameState.PlayerData(playerId,handler.getPlayerName()));
    }

    public void removePlayer(int playerId) {
        players.remove(playerId);
        currentState.getPlayers().remove(playerId);

        if(playerId == hostId && !players.isEmpty()) {
            hostId = players.keySet().iterator().next();
        }

        if(players.isEmpty()) {
            server.removeRoom(roomId);
        }
        else{
            broadcastPlayerList();
        }
    }

    public void startGame(){
        if(players.size() >= 1 && !gameStarted) {
            gameStarted = true;
            startTime = System.currentTimeMillis();
            waveLevel = 1;


            Message message = new Message(Message.Type.GAME_STARTED);
            message.putData("waveLevel", waveLevel);
            broadcast(message);

            System.out.println("Игра началась в комнате "+roomName);
        }
    }

    public void update(){
        if(!gameStarted) return;

        GameState state = new GameState();
        state.setWaveLevel(waveLevel);
        state.setTimestamp(System.currentTimeMillis());

        if(!state.isEmpty()){
            broadcastGameState(state,-1);
        }
    }

    public void handleGameAction(Message message) {
        int playerId = message.getSenderId();

        switch(message.getRequestType()){
            case PLACE_TOWER:
                handleTowerPlaced(message);
                break;
            case REMOVE_TOWER:
                handleTowerRemoved(message);
                break;
            case UPGRADE_TOWER:
                handleTowerUpgraded(message);
                break;
        }

        broadcastExcept(playerId,message);
    }

    private void handleTowerPlaced(Message message) {
        GameState.TowerData towerData = (GameState.TowerData) message.getData("towerData");
        currentState.getTowersChanged().add(towerData);
        broadcastExcept(message.getSenderId(), message);
    }

    private void handleTowerRemoved(Message message) {
        int towerId = (int) message.getData("towerId");
        currentState.getTowersChanged().removeIf(t->t.towerId == towerId);
    }

    private void handleTowerUpgraded(Message message) {
        GameState.TowerData towerData = (GameState.TowerData) message.getData("towerData");
        currentState.getTowersChanged().stream()
                .filter(t->t.towerId == towerData.towerId)
                .findFirst()
                .ifPresent(t->t.level = towerData.level);
    }

    public void broadcast(Message message) {
        players.values().forEach(handler -> handler.sendMessage(message));
    }

    public void broadcastExcept(int excludeId, Message message) {
        players.forEach((id, handler) ->{
            if(id != excludeId){
                handler.sendMessage(message);
            }
        });
    }

    public void broadcastGameState(GameState state, int senderId) {
        players.forEach((id, handler) -> {
            if (id != senderId) {
                handler.sendGameState(state);
            }
        });
    }

    public void broadcastPlayerList(){
        Message msg = new Message(Message.Type.PLAYER_LIST);
        List<String> playerNames =  new ArrayList<>();
        players.forEach((id, handler)-> playerNames.add(handler.getPlayerName()));
        msg.putData("players", playerNames);
        msg.putData("hostId", hostId);
        broadcast(msg);
    }

    public void setPlayerReady(int playerId){
        GameState.PlayerData playerData = currentState.getPlayers().get(playerId);
        if(playerData != null){
            playerData.isReady = true;

            boolean allReady = currentState.getPlayers().values().stream().allMatch(p->p.isReady);

            if(allReady && players.size() >=2){
                startGame();
            }
        }
    }

    public String getRoomId() { return roomId; }
    public String getRoomName() { return roomName; }
    public boolean isGameStarted() { return gameStarted; }
    public boolean canJoin() { return players.size() < 4 && !gameStarted; }
    public boolean isHost(int playerId) { return playerId == hostId; }
    public int getPlayerCount() { return players.size(); }
    public Map<Integer, ClientHandler> getPlayers() { return players; }
}
