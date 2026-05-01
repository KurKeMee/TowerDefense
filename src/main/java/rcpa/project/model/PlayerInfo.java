package rcpa.project.model;

import java.io.Serializable;
import java.net.InetAddress;

public class PlayerInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    private int playerId;
    private String playerName;
    private InetAddress address;
    private int port;
    private boolean isHost;
    private boolean isReady;
    private long lastPingTime;
    private long ping;

    public PlayerInfo(int playerId, String playerName, InetAddress address, int port) {
        this.playerId = playerId;
        this.playerName = playerName;
        this.address = address;
        this.port = port;
        this.isHost = false;
        this.isReady = false;
        this.lastPingTime = System.currentTimeMillis();
        this.ping = 0;
    }

    public int getPlayerId() { return playerId; }
    public String getPlayerName() { return playerName; }
    public InetAddress getAddress() { return address; }
    public int getPort() { return port; }
    public boolean isHost() { return isHost; }
    public void setHost(boolean host) { isHost = host; }
    public boolean isReady() { return isReady; }
    public void setReady(boolean ready) { isReady = ready; }
    public long getLastPingTime() { return lastPingTime; }
    public void setLastPingTime(long lastPingTime) { this.lastPingTime = lastPingTime; }
    public long getPing() { return ping; }
    public void setPing(long ping) { this.ping = ping; }
}
