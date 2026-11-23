package com.zys;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

@MapperScan("com.zys.mapper")
@SpringBootApplication
@EnableAspectJAutoProxy(exposeProxy = true)
public class PiaoLaiApplication {

    public static void main(String[] args) {
        SpringApplication.run(PiaoLaiApplication.class, args);
    }

}
