package com.example.monitoringservice.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

@Configuration
public class ConnectionConfig {
    // user name
    public final static String DB_USER = "root";
    // password
    public final static String DB_PW = "root";
    // name of the database
    public final static String DB = "systemdatabase";

    @Bean
    public static Connection con() throws SQLException {
//        Class.forName("com.mysql.jdbc.Driver");
        return DriverManager.getConnection("jdbc:mysql://mysql:3306/" + DB, DB_USER, DB_PW);
    }
}
