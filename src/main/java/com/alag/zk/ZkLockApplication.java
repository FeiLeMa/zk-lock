package com.alag.zk;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ZkLockApplication implements CommandLineRunner {
    @Autowired
    private TicketSell ticketSell;

    public static void main(String[] args) {
        SpringApplication.run(ZkLockApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        ticketSell.start();
    }
}
