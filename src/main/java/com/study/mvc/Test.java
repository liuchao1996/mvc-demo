package com.study.mvc;

import java.lang.reflect.Method;

/**
 * @author lc
 * @version 1.0
 * @date 2019-11-03 10:36
 **/
public class Test {
    public static void main(String[] args) throws ClassNotFoundException, NoSuchMethodException {
        Class<?> clazz = Class.forName("com.study.mvc.Test");
        for (Method declaredMethod : clazz.getDeclaredMethods()) {
            System.out.println(declaredMethod.getParameters().length);
            System.out.println(declaredMethod.getName());

            for (Class<?> parameterType : declaredMethod.getParameterTypes()) {
                System.out.println(parameterType.getSimpleName());
            }

//            for (Parameter parameter : declaredMethod.getParameters()) {
//                System.out.println(parameter.getName());
//            }
        }

    }

    public void login(String userName, String passWord) {
        System.out.println("方法调用了:参数：" + userName + " " + passWord);
    }
}
