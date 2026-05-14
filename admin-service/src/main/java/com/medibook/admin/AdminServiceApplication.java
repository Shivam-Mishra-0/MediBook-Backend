package com.medibook.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

// @SpringBootApplication
// @EnableDiscoveryClient
// public class AdminServiceApplication {
//     public static void main(String[] args) {
//         SpringApplication.run(AdminServiceApplication.class, args);
//         System.out.println("Admin-Service is Running on port 8089.....!");
//     }
// }

@SpringBootApplication
@EnableDiscoveryClient
public class AdminServiceApplication {
    public static void main(String[] args) {
        var ctx = SpringApplication.run(AdminServiceApplication.class, args);
        String port = ctx.getEnvironment().getProperty("server.port");
        System.out.println("Admin-Service is Running on port " + port + ".....!"); // This will dynamically call the port which is configured in the application.yml.
    }
}
