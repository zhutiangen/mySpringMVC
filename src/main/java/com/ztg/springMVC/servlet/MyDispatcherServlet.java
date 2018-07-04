package com.ztg.springMVC.servlet;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
     * 开始初始化对象
     */
    private void doInstance() {
        if (this.classNames.isEmpty()) {
           return;
        }
    }

    private void doAutowired() {
    }

    private void initHandlerMapping() {
    }
}
