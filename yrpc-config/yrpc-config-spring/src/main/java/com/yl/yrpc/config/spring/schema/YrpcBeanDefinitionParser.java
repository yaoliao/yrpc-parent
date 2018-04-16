package com.yl.yrpc.config.spring.schema;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;


/**
 * @author 小新
 * @date 2018/3/11
 */

public class YrpcBeanDefinitionParser implements BeanDefinitionParser {

    private static final Logger logger = LoggerFactory.getLogger(YrpcBeanDefinitionParser.class);

    private Class<?> beanClass;
    private Boolean required;

    public YrpcBeanDefinitionParser(Class<?> beanClass, Boolean required) {
        this.beanClass = beanClass;
        this.required = required;
    }

    public BeanDefinition parse(Element element, ParserContext parserContext) {
        return parse(element, parserContext, beanClass, required);
    }

    private static BeanDefinition parse(Element element, ParserContext parserContex, Class<?> beanClass, boolean required) {
        RootBeanDefinition beanDefinition = new RootBeanDefinition();
        beanDefinition.setBeanClass(beanClass);
        beanDefinition.setLazyInit(false);
        String id = element.getAttribute("id");
        if ((id == null || id.length() == 0) && required) {
            String beanName = element.getAttribute("name");
            if (beanName == null || beanName.length() == 0) {
                beanName = element.getAttribute("interface");
            }
            id = beanName;

        }
        if (id != null || id.length() > 0) {
            if (parserContex.getRegistry().containsBeanDefinition(id)) {
                throw new IllegalStateException("Duplicate spring bean id " + id);
            }
            parserContex.getRegistry().registerBeanDefinition(id, beanDefinition);
            beanDefinition.getPropertyValues().addPropertyValue("id", id);
        }
        for (Method setter : beanClass.getMethods()) {
            String name = setter.getName();
            if (name.length() > 3 && name.startsWith("set")
                    && Modifier.isPublic(setter.getModifiers())
                    && setter.getParameterTypes().length == 1) {
                String value = element.getAttribute(name.substring(3).toLowerCase());
                if (value != null) {
                    value = value.trim();
                    if (value.length() > 0) {
                        beanDefinition.getPropertyValues().addPropertyValue(name.substring(3), value);
                    }
                }
            }
        }
        return beanDefinition;
    }
}
