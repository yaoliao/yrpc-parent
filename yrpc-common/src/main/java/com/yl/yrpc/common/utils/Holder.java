package com.yl.yrpc.common.utils;

/**
 * @author 小新
 * @date 2018/4/16
 */

public class Holder<T> {

    private volatile T value;

    public T getValue() {
        return value;
    }

    public void setValue(T value) {
        this.value = value;
    }
}
