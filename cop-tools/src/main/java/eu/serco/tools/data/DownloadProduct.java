package eu.serco.tools.data;

import java.sql.Timestamp;

public class DownloadProduct {

    private String id;
    private String name;
    private String mission;
    private String beginPosition;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getMission() {
        return mission;
    }

    public void setMission(String mission) {
        this.mission = mission;
    }

    public String getBeginPosition() {
        return beginPosition;
    }

    public void setBeginPosition(String beginPositoin) {
        this.beginPosition = beginPositoin;
    }
}
