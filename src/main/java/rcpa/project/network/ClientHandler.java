package rcpa.project.network;

import rcpa.project.model.GameState;
import rcpa.project.model.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler implements Runnable {
    private int clientId;
    private Socket socket;
    private GameServer server;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private GameRoom currentRoom;
    private boolean running;
    private long lastActivity;
    private String playerName;

    public ClientHandler(int clientId, Socket socket, GameServer server) {
        this.clientId = clientId;
        this.socket = socket;
        this.server = server;
        this.running = true;
        this.lastActivity = System.currentTimeMillis();
        this.playerName = "Player"+clientId;

        try{
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
        } catch (IOException e) {
            System.err.println("Ошибка создания потоков: "+e.getMessage());
        }
    }

    @Override
    public void run() {
        try{
            while(running) {
                Object obj = in.readObject();
                lastActivity = System.currentTimeMillis();

                if(obj instanceof Message){
                    handleMessage((Message)obj);
                }else if(obj instanceof GameState){
                    handleGameState((GameState)obj);
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            if(running){
                System.err.println("Клиент "+clientId+" отключился: "+e.getMessage());
            }
        }
        finally {
            disconnect();
        }
    }

    private void handleMessage(Message message) {
        message.setSenderId(clientId);

        switch (message.getRequestType()){
            case CREATE_ROOM:
                handleCreateRoom(message);
                break;
            case JOIN_ROOM:
                handleJoinRoom(message);
                break;
            case LEAVE_ROOM:
                handleLeaveRoom(message);
                break;
            case START_GAME:
                handleStartGame(message);
                break;
            case PLACE_TOWER:
                handlePlaceTower(message);
                break;
            case REMOVE_TOWER:
                handleRemoveTower(message);
                break;
            case UPGRADE_TOWER:
                handleUpgradeTower(message);
                break;
            case DISCONNECT:
                disconnect();
                break;
        }
    }

    private void handleCreateRoom(Message message){
        String roomName = (String) message.getData("roomName");
        GameRoom room = server.createRoom(roomName,clientId);
        currentRoom = room;

        Message response = new Message(Message.Type.ROOM_CREATED);
        response.putData("roomId", room.getRoomId());
        response.putData("name", room.getRoomName());
        sendMessage(response);

        System.out.println("Комната создана "+roomName+" "+room.getRoomId());
    }

    private void handleJoinRoom(Message message){
        String roomId = (String) message.getData("roomId");
        GameRoom room = server.getRoom(roomId);

        if(room != null && room.canJoin()){
            currentRoom = room;
            room.addPlayer(clientId,this);

            Message response = new Message(Message.Type.ROOM_JOINED);
            response.putData("roomId", roomId);
            sendMessage(response);
        }
        else{
            Message response = new Message(Message.Type.ERROR);
            response.setErrorMessage("Не удалось подключиться к комнате");
            sendMessage(response);
        }
    }

    private void handleLeaveRoom(Message message){
        if(currentRoom != null){
            currentRoom.removePlayer(clientId);
            currentRoom = null;
        }
    }

    private void handleStartGame(Message message){
        if(currentRoom != null && currentRoom.isHost(clientId)){
            currentRoom.startGame();
        }
    }

    private void handlePlaceTower(Message message){
        if(currentRoom != null && currentRoom.isGameStarted()){
            currentRoom.handleGameAction(message);
        }
    }

    private void handleRemoveTower(Message message){
        if(currentRoom != null && currentRoom.isGameStarted()){
            currentRoom.handleGameAction(message);
        }
    }

    private void handleUpgradeTower(Message message){
        if(currentRoom != null && currentRoom.isGameStarted()){
            currentRoom.handleGameAction(message);
        }
    }

    private void handleGameState(GameState gameState){
        if(currentRoom != null){
            currentRoom.broadcastGameState(gameState);
        }
    }

    public void sendMessage(Message message){
        try {
            out.writeObject(message);
            out.flush();
        } catch (IOException e) {
            System.err.println("Ошибка отправки сообщения клиенту "+clientId);
        }
    }

    public void sendGameState(GameState gameState){
        try{
            out.writeObject(gameState);
            out.flush();
        } catch (IOException e) {
            System.err.println("Ошибка отправки состояния клиенту "+clientId);
        }
    }

    public void disconnect(){
        running = false;

        if(currentRoom != null){
            currentRoom.removePlayer(clientId);
        }

        server.disconnectClient(clientId);

        try{
            if(out != null)out.close();
            if(in !=null)in.close();
            if(socket!=null)socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Клиент "+clientId+" отключен");
    }

    public int getClientId() { return clientId; }
    public long getLastActivity() { return lastActivity; }
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    public GameRoom getCurrentRoom() { return currentRoom; }
    public void setCurrentRoom(GameRoom currentRoom) { this.currentRoom = currentRoom; }
}
