package semo.back.service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication(exclude = {DataSourceAutoConfiguration.class})
@EnableFeignClients(basePackages = "auth.common.core.client")
@EnableDiscoveryClient
@EnableScheduling
public class SemoBackServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SemoBackServiceApplication.class, args);
    }

}
