package com.zys;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@MapperScan("com.zys.mapper")
@SpringBootApplication
public class PiaoLaiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PiaoLaiApplication.class, args);
    }

}
