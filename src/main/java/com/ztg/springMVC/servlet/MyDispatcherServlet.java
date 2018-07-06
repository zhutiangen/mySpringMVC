package com.ztg.springMVC.servlet;

import com.ztg.springMVC.annotation.MyAutowired;
import com.ztg.springMVC.annotation.MyController;
import com.ztg.springMVC.annotation.MyService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.logging.Logger;

public class MyDispatcherServlet extends HttpServlet {
    private Logger logger = Logger.getLogger("init");

    private Properties properties = new Properties();

    // 保存包中的class类的路径
    private List<String> classNames = new ArrayList<>();

    private Map<String, Object> ioc = new HashMap<>();

    private Map<String, Method> handleMapper = new HashMap<>();

    private Map<String, Object> controllerMap = new HashMap<>();

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        super.init(servletConfig);
        logger.info("初始化MyDisPatcherServlet");
        // 1.加载配置文件，填充properties字段
        String contextConfigLocation = servletConfig.getInitParameter("contextConfigLocation");
        doLoadConfig(contextConfigLocation);

        // 2.根据properties,初始化所有相关的类，扫描用户设定包下的所有类
        doScanner(properties.getProperty("scanPackage"));

        // 3.拿到扫描的类，通过反射机制，实例化，并且放到IOC容器中（key-value beanName-bean)，beanName默认小写字母
        doInstance();

        // 4.自动化注入依赖
        doAutowired();

        // 5.初始HandlerMapping（将URL和method想对应）
        initHandlerMapping();
    }

    /**
     * 根据配置文件路径，加载文件内容加入到properties中
     * @param contextConfigLocation 配置文件路径
     */
    private void doLoadConfig(String contextConfigLocation) {
        // TODO 问题classLoader是什么？？
        InputStream resourceStream = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);

        try {
            logger.info("开始加载配置文件");
            // todo 问题： Properties对象详解
            this.properties.load(resourceStream);
        }catch (Exception e) {
            e.printStackTrace();
        }finally {
            if ( null != resourceStream ) {
                try {
                    resourceStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * 从扫描包中初始化类
     * 将每个类的路径信息加载到classNames
     * @param scanPackage
     */
    private void doScanner(String scanPackage) {
        URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.","/"));
        File dir = new File(url.getFile());

        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                this.doScanner(scanPackage + '.' + file.getName());
            } else {
                String className = scanPackage + "." + file.getName().replace(".class", "");
                this.classNames.add(className);
            }
        }
    }

    /**
     * 根据classNames类信息，进行初始化对象
     * 将其放入到ioc中存储 beanName - bean
     */
    private void doInstance() {
        if (this.classNames.isEmpty()) {
           return;
        }
        this.classNames.forEach((String className) -> {
            // 使用反射，进行实例化，只对于@myController
            try {
                // 使用反射得到类的所有信息
                Class<?> clazz = Class.forName(className);

                // 是用@MyController进行修饰的类
                if (clazz.isAnnotationPresent(MyController.class)) {
                    // 初始化类,需要将类的首字符小写，并放到ioc容器中
                    // clazz.getSimpleName() 得到类的全名  clazz.newInstance()实例化后的对象
                    this.ioc.put(toLocalFirstWord(clazz.getSimpleName()), clazz.newInstance());

                }else if (clazz.isAnnotationPresent(MyService.class)) {
                    // 注意：service是接口 需要初始化对应的实现类 MyService注册在实现类上的
                    MyService myService = clazz.getAnnotation(MyService.class);

                    // 自定义service名
                    String beanName = myService.value();
                    if ("".equals(beanName.trim())) {
                        beanName = this.toLocalFirstWord(beanName);
                    }

                    // 加入beanName - bean 加入到 ioc
                    Object instance = clazz.newInstance();
                    this.ioc.put(beanName, instance);

                    // 得到实现类的所有接口 将interfaceName和实现接口加入
                    // 一个接口有多个实现类 那么接口名对应的接口对象会被覆盖掉
                    Class[] interfaces = clazz.getInterfaces();
                    for (Class<?> i : interfaces) {
                        this.ioc.put(i.getName(), instance);
                    }
                }
                // 其他不作为
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * 自动注入
     * 技术： 反射
     */
    private void doAutowired() {
        // ioc 容器中已经存储了许多bean
        if (this.ioc.isEmpty()) {
            return;
        }

        for (Map.Entry<String, Object> entry : this.ioc.entrySet()) {

            Field[] declaredFields = entry.getValue().getClass().getDeclaredFields();

            for (Field field : declaredFields) {
                if (!field.isAnnotationPresent(MyAutowired.class)) {
                    continue;
                }

                // 属性注入
                MyAutowired myAutowired = field.getAnnotation(MyAutowired.class);
                String value = myAutowired.value();

                // 默认注入
                if ("".equals(value)) {
                    value = field.getType().getName();
                }

                // 自定义注入
                field.setAccessible(true);
                try {
                    field.set(entry.getValue(), this.ioc.get(value));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    continue;
                }

            }
        }
    }

    private void initHandlerMapping() {
    }

    /**
     * 将类的首字母便成小写
     * @param simpleName
     * @return
     */
    private String toLocalFirstWord(String simpleName) {
        char[] chars = simpleName.toCharArray();
        chars[0] = (char) (chars[0] + 32);
        return String.valueOf(chars);
    }
    
    
}
