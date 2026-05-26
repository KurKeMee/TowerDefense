package rcpa.project.network;

import rcpa.project.model.GameState;
import rcpa.project.model.Message;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Map;

public class GameClient {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private int playerId;
    private boolean connected;
    private Thread listenerThread;
    private ClientListener listener;

    public interface ClientListener {
        void onConnected(int playerId);
        void onDisconnected();
        void onMessageReceived(Message message);
        void onGameStateReceived(GameState gameState);
    }

    public GameClient() {
        this.playerId = -1;
        this.connected = false;
    }

    public boolean connect(String host, int port) {
        try{
            socket = new Socket(host,port);
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());
            connected = true;

            listenerThread = new Thread(this::listenForMessages);
            listenerThread.setDaemon(true);
            listenerThread.start();

            Message message = new Message(Message.Type.PLAYER_JOIN);
            sendMessage(message);

            System.out.println("Подключен к серверу "+host+":"+port);
            return true;
        } catch (IOException e) {
            System.err.println("Ошибка подключения "+e.getMessage());
            return false;
        }
    }

    private void listenForMessages() {
        try{
            while(connected) {
                Object obj = in.readObject();

                if(obj instanceof Message) {
                    Message message = (Message) obj;
                    handleServerMessage(message);
                }else if(obj instanceof GameState) {
                    GameState gameState = (GameState) obj;
                    if(listener != null) {
                        listener.onGameStateReceived(gameState);
                    }
                }
            }
        } catch (IOException | ClassNotFoundException e) {
            if(connected) {
                Message message = new Message(Message.Type.DISCONNECT);
                sendMessage(message);
                System.err.println("Соединение потеряно "+e.getMessage());
            }
        }
        finally {
            disconnect();
        }
    }

    private void handleServerMessage(Message message) {
        switch (message.getRequestType()){
            case PLAYER_ASSIGNED:
                playerId = (int) message.getData("playerId");
                if (listener != null) {
                    listener.onConnected(playerId);
                }
                System.out.println("Получен ID: " + playerId);
                break;
            case ROOM_CREATED:
                System.out.println("Комната создана: " + message.getData("roomId"));
                break;
            case ROOM_JOINED:
                System.out.println("Подключились к комнате: " + message.getData("roomId"));
                break;
            case GAME_STARTED:
                System.out.println("Игра началась!");
                break;
            case ERROR:
                System.err.println("Ошибка: " + message.getErrorMessage());
                break;
            default:
                break;
        }

        if(listener != null) {
            listener.onMessageReceived(message);
        }
    }

    public void sendMessage(Message message) {
        if(connected && out != null) {
            try {
                message.setSenderId(playerId);
                out.writeObject(message);
                out.flush();
            } catch (IOException e) {
                System.err.println("Ошибка отправки "+e.getMessage());
            }
        }
    }

    public void sendGameState(GameState gameState) {
        if(connected && out != null) {
            try {
                out.writeObject(gameState);
                out.flush();
            } catch (IOException e) {
                System.err.println("Ошибка отправки состояния "+e.getMessage());
            }
        }
    }

    public void sendRequest(Message.Type type, Map<String, Object> data) {
        Message message = new Message(type);
        if(data!=null) data.forEach(message::putData);
        sendMessage(message);
    }

    public void placeTower(GameState.TowerData towerData){
        Message message = new Message(Message.Type.PLACE_TOWER);
        message.putData("playerId", playerId);
        message.putData("towerData", towerData);
        sendMessage(message);
    }

    public void removeTower(int towerId){
        Message message = new Message(Message.Type.REMOVE_TOWER);
        message.putData("towerId", towerId);
        sendMessage(message);
    }

    public void upgradeTower(GameState.TowerData towerData){
        Message message = new Message(Message.Type.UPGRADE_TOWER);
        message.putData("towerData", towerData);
        sendMessage(message);
    }

    public void disconnect() {
        if(connected) {
            sendMessage(new Message(Message.Type.DISCONNECT));
            connected = false;

            try {
                if (out != null) out.close();
                if (in != null) in.close();
                if (socket != null) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            System.out.println("Отключен от сервера");

            if (listener != null) {
                listener.onDisconnected();
            }
        }
    }

    public boolean isConnected() { return connected; }
    public int getPlayerId() { return playerId; }
    public void setListener(ClientListener listener) { this.listener = listener; }
}
