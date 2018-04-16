package com.yl.yrpc.common.extension;

import java.lang.annotation.*;

/**
 * @author 小新
 * @date 2018/4/16
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface SPI {

    String value() default "";

}
