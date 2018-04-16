package com.yl.yrpc.demo;

import com.yl.yrpc.config.ServiceConfig;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.io.IOException;

/**
 * @author 小新
 * @date 2018/3/11
 */

public class YrpcProvider {

    public static void main(String[] args) throws IOException {
        ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext("yrpc-provider.xml");
        context.start();

        ServiceConfig bean = (ServiceConfig) context.getBean("com.yl.yrpc.demo.ServiceDemo");
        String interfaceName = bean.getInterfaceName();
        System.out.println("name ===== " + interfaceName);

        Object yl = context.getBean("aa");
        Object dd = context.getBean("com.yl.yrpc.demo.Yldemo");


        System.in.read();


    }

}
