package com.spotifyxp.sql;

import java.sql.Date;

public class SQLEntryPair {
    private final String name;
    private SQLEntryTypes types;
    private boolean canBeNull = true;
    private Object value;

    public SQLEntryPair(String name, boolean canBeNull, SQLEntryTypes type) {
        this.name = name;
        this.types = type;
        this.canBeNull = canBeNull;
    }

    public SQLEntryPair(String name, boolean canBeNull, String type) {
        this.name = name;
        for (SQLEntryTypes o : SQLEntryTypes.values()) {
            if (o.getRealType().equals(type)) {
                this.types = o;
            }
        }
        this.canBeNull = canBeNull;
        if (this.types == null) {
            throw new RuntimeException();
        }
    }

    public SQLEntryPair(String name, Object value) {
        this.name = name;
        this.value = value;
    }

    public SQLEntryPair(String name, Object value, SQLEntryTypes type) {
        this.types = type;
        this.name = name;
        this.value = value;
    }

    public String getValueString() {
        return (String) value;
    }

    public Object getValue() {
        return value;
    }

    public Integer getValueInteger() {
        return (Integer) value;
    }

    public Boolean getValueBoolean() {
        return (Boolean) value;
    }

    public Date getValueDate() {
        return (Date) value;
    }

    public String getName() {
        return name;
    }

    public SQLEntryTypes getType() {
        return types;
    }

    public boolean canBeNull() {
        return canBeNull;
    }
}
