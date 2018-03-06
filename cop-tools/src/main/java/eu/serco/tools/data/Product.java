package eu.serco.tools.data;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class Product {

    final static Logger logger = LogManager.getLogger(Product.class);

    private String identifier;
    private String uuid;
    private String beginposition;
    private String downloadUrl;
    private String mission;

    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getBeginposition() {
        return beginposition;
    }

    public void setBeginposition(String beginposition) {
        this.beginposition = beginposition;
    }

    public String getDownloadUrl() {
        return downloadUrl;
    }

    public void setDownloadUrl(String downloadUrl) {
        this.downloadUrl = downloadUrl;
    }

    public String getMission() {
        return mission;
    }

    public void setMission(String mission) {
        this.mission = mission;
    }
}
