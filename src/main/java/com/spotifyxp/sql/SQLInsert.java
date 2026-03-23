package com.spotifyxp.sql;

import java.sql.Date;

public class SQLInsert {
    private final Object object;
    private final SQLEntryTypes type;

    public SQLInsert(Object object, SQLEntryTypes type) {
        this.object = object;
        this.type = type;
    }

    public Object getObject() {
        return object;
    }

    public String getString() {
        return (String) object;
    }

    public Boolean getBoolean() {
        return (Boolean) object;
    }

    public Date getDate() {
        return (Date) object;
    }

    public Integer getInteger() {
        return (Integer) object;
    }

    public SQLEntryTypes getType() {
        return type;
    }
}
