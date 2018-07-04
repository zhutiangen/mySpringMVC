package com.ztg.springMVC.annotation;

import java.lang.annotation.*;

@Target(ElementType.FIELD) // 表示使用在属性上
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface myAutowired {

}
