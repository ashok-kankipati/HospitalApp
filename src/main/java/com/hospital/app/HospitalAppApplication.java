package com.hospital.app;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class HospitalAppApplication {

    public static void main(String[] args) {
        SpringApplication.run(HospitalAppApplication.class, args);
        System.out.println("Hospital Management System Started Successfully!");
    }
}
