package com.yl.yrpc.config.spring.schema;

import com.yl.yrpc.config.ServiceConfig;
import org.springframework.beans.factory.xml.NamespaceHandlerSupport;

/**
 * @author 小新
 * @date 2018/3/11
 */

public class YrpcNamespaceHandler extends NamespaceHandlerSupport {

    public void init() {
        registerBeanDefinitionParser("service", new YrpcBeanDefinitionParser(ServiceConfig.class, true));
    }
}
