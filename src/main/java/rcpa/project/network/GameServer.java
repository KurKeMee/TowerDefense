package rcpa.project.network;

import rcpa.project.model.GameState;
import rcpa.project.model.Message;
import rcpa.project.model.PlayerInfo;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class GameServer {
    private final int PORT = 8080;
    private final int TICK_RATE = 20; // 20 обновлений в секунду

    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private ScheduledExecutorService gameLoop;
    private Map<Integer, ClientHandler> clients;
    private Map<String, GameRoom> rooms;
    private int nextClientId;
    private boolean running;

    public GameServer() {
        this.threadPool = Executors.newCachedThreadPool();
        this.gameLoop = Executors.newSingleThreadScheduledExecutor();
        this.clients = new ConcurrentHashMap<>();
        this.rooms = new ConcurrentHashMap<>();
        this.nextClientId = 1;
        this.running = true;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Сервер запущен на порту " + PORT);

            // Запускаем игровой цикл сервера
            gameLoop.scheduleAtFixedRate(this::gameTick, 0, 1000 / TICK_RATE, TimeUnit.MILLISECONDS);

            while (running) {
                Socket socket = serverSocket.accept();
                int clientId = nextClientId++;

                ClientHandler handler = new ClientHandler(clientId, socket, this);
                clients.put(clientId, handler);
                threadPool.execute(handler);

                System.out.println("Клиент " + clientId + " подключился " + socket.getInetAddress());
            }
        } catch (IOException e) {
            System.err.println("Ошибка сервера: " + e.getMessage());
        }
    }

    /**
     * Главный игровой цикл сервера
     * Вызывает update() для всех активных комнат
     */
    private void gameTick() {
        rooms.values().stream()
                .filter(GameRoom::isGameStarted)
                .forEach(GameRoom::update);

        checkTimeouts();
    }

    private void checkTimeouts() {
        long now = System.currentTimeMillis();
        clients.values().stream()
                .filter(c -> now - c.getLastActivity() > 300000) // 5 минут
                .forEach(c -> {
                    System.out.println("Клиент " + c.getClientId() + " отключен по таймеру");
                    disconnectClient(c.getClientId());
                });
    }

    public void disconnectClient(int clientId) {
        ClientHandler handler = clients.remove(clientId);
        if (handler != null) {
            handler.disconnect();
            rooms.values().forEach(room -> room.removePlayer(clientId));
        }
    }

    public GameRoom createRoom(String roomName, int hostId) {
        String roomId = UUID.randomUUID().toString().substring(0, 6);
        GameRoom room = new GameRoom(roomId, roomName, hostId, this);
        rooms.put(roomId, room);

        ClientHandler hostHandler = clients.get(hostId);
        if (hostHandler != null) {
            room.addPlayer(hostId, hostHandler);
            hostHandler.setCurrentRoom(room);
        }
        return room;
    }

    public GameRoom getRoom(String roomId) {
        return rooms.get(roomId);
    }

    public void removeRoom(String roomId) {
        GameRoom room = rooms.remove(roomId);
        if (room != null) {
            room.stopGameLoop();
        }
    }

    public Map<Integer, ClientHandler> getClients() { return clients; }
    public ClientHandler getClient(int clientId) { return clients.get(clientId); }
    public Map<String, GameRoom> getRooms() { return rooms; }

    public void stop() {
        running = false;
        gameLoop.shutdown();
        threadPool.shutdown();

        // Останавливаем все комнаты
        rooms.values().forEach(GameRoom::stopGameLoop);

        clients.values().forEach(ClientHandler::disconnect);

        try {
            serverSocket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        new GameServer().start();
    }
}