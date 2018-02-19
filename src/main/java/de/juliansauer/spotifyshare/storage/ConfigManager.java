package de.juliansauer.spotifyshare.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

public class ConfigManager {

    private final String filePath = System.getProperty("user.dir") + File.separator + "Credentials.cfg";

    private Properties properties;

    public ConfigManager() {
        properties = new Properties();
        if ((new File(filePath)).exists()) {
            try {
                properties.load(new FileInputStream(filePath));
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            properties.put("ClientId", "");
            properties.put("ClientSecret", "");
            try {
                properties.store(new FileOutputStream(filePath), null);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public String getClientId() {
        return properties.getProperty("ClientId");
    }

    public String getClientSecret() {
        return properties.getProperty("ClientSecret");
    }

}
