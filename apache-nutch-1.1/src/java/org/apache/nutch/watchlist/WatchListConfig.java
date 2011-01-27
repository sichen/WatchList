package org.apache.nutch.watchlist;

public enum WatchListConfig {
    JDBC_DRIVER("watchlist.jdbc.driver", "JDBC Driver to be used"),
    JDBC_URL("watchlist.jdbc.url", "The JDBC URL to access Database"),
    JDBC_USER("watchlist.jdbc.user", "JDBC user name"),
    JDBC_PASSWORD("watchlist.jdbc.password", "The JDBC password"),
    JDBC_ID_GEN_TABLE("watchlist.jdbc.idgenerator.table", "The Table for IdGenerator"),
    NONE("none", "end of configuration");

    /** Attribute string in XML configuration */
    private String attributeString;
    /** Display name for UI */
    private String displayName;

    private WatchListConfig(String attributeString, String displayName) {
        this.attributeString = attributeString;
        this.displayName = displayName;
    }

    public String getAttributeString() {
        return attributeString;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static WatchListConfig fromAttributeString(String attString,
            WatchListConfig defaultType) {
        if (attString != null) {
            for (WatchListConfig conf : WatchListConfig.values()) {
                if (conf.getAttributeString().equalsIgnoreCase(attString)) {
                    return conf;
                }
            }
        }
        return defaultType;
    }
}
