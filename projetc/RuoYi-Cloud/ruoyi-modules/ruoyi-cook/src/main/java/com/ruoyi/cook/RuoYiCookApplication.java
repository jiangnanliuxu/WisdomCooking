package com.ruoyi.cook;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import com.ruoyi.common.security.annotation.EnableCustomConfig;
import com.ruoyi.common.security.annotation.EnableRyFeignClients;

/**
 * 码上智厨业务模块。
 */
@EnableCustomConfig
@EnableRyFeignClients
@SpringBootApplication
public class RuoYiCookApplication
{
    public static void main(String[] args)
    {
        SpringApplication.run(RuoYiCookApplication.class, args);
        System.out.println("(♥◠‿◠)ﾉﾞ  码上智厨业务模块启动成功   ლ(´ڡ`ლ)ﾞ");
    }
}
