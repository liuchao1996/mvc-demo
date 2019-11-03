package com.study.mvc.controller;

import com.study.mvc.annotation.Controller;
import com.study.mvc.annotation.RequestMapping;

/**
 * @author lc
 * @version 1.0
 * @date 2019-10-31 15:50
 **/
@Controller
@RequestMapping("/study")
public class StudyController {

    @RequestMapping("/login")
    public void login(String userName, String passWord) {
        System.out.println("方法调用了:参数：" + userName + " " + passWord);
    }
}
