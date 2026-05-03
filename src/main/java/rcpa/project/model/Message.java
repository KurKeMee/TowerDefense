package rcpa.project.model;

import rcpa.project.entity.base.Tower;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class Message implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum Type{
        PLAYER_JOIN, PLAYER_ASSIGNED, PLAYER_LEFT,

        CREATE_ROOM, ROOM_CREATED, JOIN_ROOM, ROOM_JOINED, LEAVE_ROOM, PLAYER_LIST,

        WAVE_START, WAVE_END,

        START_GAME, GAME_STARTED, GAME_OVER, PLAYER_READY,

        PLACE_TOWER, REMOVE_TOWER, UPGRADE_TOWER,

        CHAT_MESSAGE, DISCONNECT,

        SUCCESS, ERROR
    }

    private Type requestType;
    private String errorMessage;
    private Tower tower;
    private int senderId;
    private Map<String, Object> data;

    public Message(Type requestType) {
        this.requestType = requestType;
        this.data = new HashMap<>();
    }

    public void putData(String key, Object value) {data.put(key, value);}
    public Object getData(String key) {return data.get(key);}
    public Map<String, Object> getDataMap() {return data;}
    public Type getRequestType() {return requestType;}
    public void setRequestType(Type requestType) {this.requestType = requestType;}
    public String getErrorMessage() {return errorMessage;}
    public void setErrorMessage(String errorMessage) {this.errorMessage = errorMessage;}
    public Tower getTower() {return tower;}
    public void setTower(Tower tower) {this.tower = tower;}
    public int getSenderId() {return senderId;}
    public void setSenderId(int senderId) {this.senderId = senderId;}
}
