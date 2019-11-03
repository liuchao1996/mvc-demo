package com.study.mvc.servlet;

import com.study.mvc.annotation.Controller;
import com.study.mvc.annotation.RequestMapping;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * 入口
 *
 * @author lc
 * @version 1.0
 * @date 2019-10-31 15:34
 **/
public class DispatcherServlet extends HttpServlet {

    /**
     * 存放配置
     */
    private Properties properties = new Properties();

    /**
     * 存放扫描到的类
     */
    private List<String> classNames = new ArrayList<>();

    /**
     * 存放实例化对象
     */
    private Map<String, Object> ioc = new HashMap<>();

    /**
     * 存储方法
     */
    private Map<String, Method> handleMapping = new HashMap<>();

    /**
     * 存储控制器
     */
    private Map<String, Object> controllerMap = new HashMap<>();

    @Override
    public void init(ServletConfig config) throws ServletException {
        //1.加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));

        //2.扫描类
        doScanner(properties.getProperty("scanPackage"));

        //3.拿到扫描的类后，通过反射机制实例化，并发到ioc容器中（key -> value） Map
        doInstance();

        //4.初始化handleMapping(url - > method)
        initHandleMapping();
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        doDispatch(req, resp);
    }

    /**
     * 获取servlet初始化的时候配置的文件
     *
     * @param location 配置文件名称
     */
    private void doLoadConfig(String location) {
        //把web.xml中的contextConfigLocation对应value值的文件加载到流里面
        InputStream resourceAsStream = this.getClass().getClassLoader().getResourceAsStream(location);
        try {
            properties.load(resourceAsStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (resourceAsStream != null) {
                try {
                    resourceAsStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

    /**
     * 递归调用
     *
     * @param packageName 扫描的包名
     */
    private void doScanner(String packageName) {
        //把所有.替换成/
        URL url = this.getClass().getClassLoader().getResource("/" + packageName.replaceAll("\\.", "/"));
        File dir = new File(Objects.requireNonNull(url).getFile());
        for (File file : Objects.requireNonNull(dir.listFiles())) {
            if (file.isDirectory()) {
                //递归读取包
                doScanner(packageName + "." + file.getName());
            } else {
                String className = packageName + "." + file.getName().replace(".class", "");
                classNames.add(className);
                System.out.println("扫描到的类有：" + className);
            }
        }
    }

    /**
     * 实例化类
     */
    private void doInstance() {
        classNames.forEach(x -> {
            try {
                Class<?> clazz = Class.forName(x);
                if (clazz.isAnnotationPresent(Controller.class)) {
                    ioc.put(toLowerFirstWord(clazz.getSimpleName()), clazz.newInstance());
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    private String toLowerFirstWord(String name) {
        char[] charArray = name.toCharArray();
        charArray[0] += 32;
        return String.valueOf(charArray);
    }

    /**
     * 初始化handleMapping(url - > method)
     */
    private void initHandleMapping() {

        ioc.forEach((key, val) -> {

            Class<?> clazz = val.getClass();
            if (!clazz.isAnnotationPresent(Controller.class)) {
                return;
            }

            String baseUrl = "";
            if (clazz.isAnnotationPresent(RequestMapping.class)) {
                RequestMapping annotation = clazz.getAnnotation(RequestMapping.class);
                baseUrl = annotation.value();
            }


            try {
                Method[] methods = clazz.getMethods();
                for (Method method : methods) {
                    //判断方法是不是有RequestMapping注解
                    if (!method.isAnnotationPresent(RequestMapping.class)) {
                        continue;
                    }

                    RequestMapping methodAnnotation = method.getAnnotation(RequestMapping.class);
                    String url = methodAnnotation.value();

                    url = (baseUrl + "/" + url).replaceAll("/+", "/");
                    handleMapping.put(url, method);

                    controllerMap.put(url, clazz.newInstance());

                    System.out.println(url + "," + method);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }

        });

    }

    private void doDispatch(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (handleMapping.isEmpty()) {
            return;
        }

        String url = req.getRequestURI();
        String contextPath = req.getContextPath();

        //拼接url并把多个/替换成一个
        url = url.replace(contextPath, "").replaceAll("/+", "/");

        if (!this.handleMapping.containsKey(url)) {
            resp.getWriter().write("404 NOT FOUND");
            return;
        }

        Method method = this.handleMapping.get(url);

        //获取方法的参数列表
        Class<?>[] parameterTypes = method.getParameterTypes();

        //获取请求的参数
        Map<String, String[]> parameterMap = req.getParameterMap();

        //保存参数值
        Object[] paramValues = new Object[parameterTypes.length];

        //方法的参数列表
        for (int i = 0; i < parameterTypes.length; i++) {
            //根据参数名称,做某些处理
            String requestParam = parameterTypes[i].getSimpleName();
            if ("HttpServletRequest".equals(requestParam)) {
                //参数类型已明确，强转类型
                paramValues[i] = req;
                continue;
            }

            if ("HttpServletResponse".equals(requestParam)) {
                paramValues[i] = resp;
                continue;
            }

            System.out.println("参数类型:" + requestParam);
            if ("String".equals(requestParam)) {

                for (Map.Entry<String, String[]> entry : parameterMap.entrySet()) {
                    System.out.println("获取到的参数：" + Arrays.toString(entry.getValue()));

                    String value = Arrays.toString(entry.getValue()).replaceAll("[\\[\\]]", "");
                    paramValues[i] = value;
                    i++;
                }

//                parameterMap.forEach((key, val) -> {
//                    //  \\[|\\]
//                    String value = Arrays.toString(val).replaceAll("[\\[\\]]", "");
//                    paramValues[finalI] = value;
//                });
            }

        }

        //利用反射机制调用 第一个参数是method对应的实例，第二个是参数
        try {
            method.invoke(this.controllerMap.get(url), paramValues);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }

    }


}
