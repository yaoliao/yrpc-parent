package com.yl.yrpc.config;

/**
 * @author 小新
 * @date 2018/3/11
 */

public class ServiceConfig<T> {

    private String id;
    // 接口类型
    private String interfaceName;
    private Class<?> interfaceClass;

    public String getInterface() {
        return interfaceName;
    }

    public void setInterface(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public String getInterfaceName() {
        return interfaceName;
    }

    public void setInterfaceName(String interfaceName) {
        this.interfaceName = interfaceName;
    }

    public Class<?> getInterfaceClass() {
        if (interfaceClass != null) {
            return interfaceClass;
        }
        try {
            interfaceClass = Class.forName(interfaceName, true, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            throw new IllegalStateException(e.getMessage(), e);
        }
        return interfaceClass;
    }

    public void setInterfaceClass(Class<?> interfaceClass) {
        this.interfaceClass = interfaceClass;
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
