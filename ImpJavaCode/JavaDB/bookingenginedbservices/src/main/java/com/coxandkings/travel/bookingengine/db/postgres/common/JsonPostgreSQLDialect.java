package com.coxandkings.travel.bookingengine.db.postgres.common;

import org.hibernate.dialect.PostgreSQL9Dialect;

import java.sql.Types;

//extending the PostgreSQL dialect to tell it about the json type
public class JsonPostgreSQLDialect extends PostgreSQL9Dialect {

    public JsonPostgreSQLDialect() {

        super();

        this.registerColumnType(Types.JAVA_OBJECT, "json");
    }
}