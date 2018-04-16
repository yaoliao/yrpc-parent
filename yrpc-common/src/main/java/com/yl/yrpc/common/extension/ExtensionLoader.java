package com.yl.yrpc.common.extension;

import com.yl.yrpc.common.utils.Holder;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.regex.Pattern;

/**
 * @author 小新
 * @date 2018/4/16
 */

public class ExtensionLoader<T> {

    private static final String RPC_DIRECTORY = "META-INF/yrpc";

    private static final Pattern NAME_SEPARATOR = Pattern.compile("\\s*[,]+\\s*");

    /**
     * 缓存所有的扩展点加载器
     */
    private static final ConcurrentMap<Class<?>, ExtensionLoader<?>> EXTENSION_LOADERS = new ConcurrentHashMap<Class<?>, ExtensionLoader<?>>();

    /**
     * 缓存所有的扩展点
     */
    private static final ConcurrentMap<Class<?>, Object> EXTENSION_INSTANCES = new ConcurrentHashMap<Class<?>, Object>();

    /**
     * 接口的类型
     */
    private final Class<?> type;

    /**
     * 默认的SPI，在@SPI中写的值
     */
    private String cachedDefaultName;

    /**
     * 缓存所有在对应type下的SPI的配置
     * 例如配置： dubbo=com.alibaba.dubbo.registry.dubbo.DubboRegistryFactory
     * key = dubbo
     * value = com.alibaba.dubbo.registry.dubbo.DubboRegistryFactory
     */
    private final Holder<Map<String, Class<?>>> cachedClasses = new Holder<Map<String, Class<?>>>();

    /**
     * 缓存所有在对应type下的,已经实例化的类
     * 例如 dubbo=com.alibaba.dubbo.registry.dubbo.DubboRegistryFactory
     * key = DubboRegistryFactory
     * value = DubboRegistryFactory的实例化后的类
     */
    private final Map<String, Holder<Object>> cachedInstances = new ConcurrentHashMap<String, Holder<Object>>();

    /**
     * 异常记录
     */
    private Map<String, IllegalStateException> exceptions = new ConcurrentHashMap<String, IllegalStateException>();


    public ExtensionLoader(Class<?> type) {
        this.type = type;
    }

    @SuppressWarnings("unchecked")
    public static <T> ExtensionLoader<T> getExtensionLoader(Class<T> type) {
        if (type == null)
            throw new IllegalArgumentException("Extension type == null");
        if (!type.isInterface()) {
            throw new IllegalArgumentException("Extension type(" + type + ") is not interface!");
        }
        if (!withExtensionAnnotation(type)) {
            throw new IllegalArgumentException("Extension type(" + type +
                    ") is not extension, because WITHOUT @" + SPI.class.getSimpleName() + " Annotation!");
        }
        ExtensionLoader<T> loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
        if (loader == null) {
            EXTENSION_LOADERS.putIfAbsent(type, new ExtensionLoader<>(type));
            loader = (ExtensionLoader<T>) EXTENSION_LOADERS.get(type);
        }
        return loader;
    }

    private static <T> boolean withExtensionAnnotation(Class<T> type) {
        return type.isAnnotationPresent(SPI.class);
    }

    /**
     * 获取默认的扩展点加载器
     *
     * @return
     */
    public T getDefaultExtension() {
        getExtensionClasses();
        if (null == cachedDefaultName || cachedDefaultName.length() == 0
                || "true".equals(cachedDefaultName)) {
            return null;
        }
        return getExtension(cachedDefaultName);
    }

    /**
     * 返回指定名字的扩展。如果指定名字的扩展不存在，则抛异常 {@link IllegalStateException}.
     *
     * @param name
     * @return
     */
    private T getExtension(String name) {
        if (name == null || name.length() == 0)
            throw new IllegalArgumentException("Extension name == null");
        if ("true".equals(name)) {
            return getDefaultExtension();
        }
        Holder<Object> holder = cachedInstances.get(name);
        if (holder == null) {
            cachedInstances.putIfAbsent(name, new Holder<Object>());
            holder = cachedInstances.get(name);
        }
        Object instance = holder.getValue();
        if (instance == null) {
            synchronized (holder) {
                instance = holder.getValue();
                if (instance == null) {
                    instance = createExtension(name);
                    holder.setValue(instance);
                }
            }
        }
        return (T) instance;
    }

    /**
     * 根据获取到的拓展点class实例化成对象返回
     *
     * @param name
     * @return
     */
    @SuppressWarnings("unchecked")
    private Object createExtension(String name) {
        Class<?> clazz = getExtensionClasses().get(name);
        if (clazz == null) {
            throw findException(name);
        }

        try {
            T instance = (T) EXTENSION_INSTANCES.get(clazz);
            if (instance == null) {
                EXTENSION_INSTANCES.putIfAbsent(clazz, (T) clazz.newInstance());
                instance = (T) EXTENSION_INSTANCES.get(clazz);
            }
            return instance;
        } catch (Throwable t) {
            throw new IllegalStateException("Extension instance(name: " + name + ", class: " +
                    type + ")  could not be instantiated: " + t.getMessage(), t);
        }
    }

    /**
     * 获取扩展点并缓存
     *
     * @return
     */
    private Map<String, Class<?>> getExtensionClasses() {
        Map<String, Class<?>> classes = cachedClasses.getValue();
        if (classes == null) {
            synchronized (cachedClasses) {
                classes = cachedClasses.getValue();
                if (classes == null) {
                    classes = loadExtensionClasses();
                    cachedClasses.setValue(classes);
                }
            }
        }
        return classes;
    }

    /**
     * 1、设置cachedDefaultName，
     * 2、加载配置文件 WEB-INF/... 下所有符合的扩展点
     *
     * @return
     */
    private Map<String, Class<?>> loadExtensionClasses() {
        final SPI defaultAnnotation = type.getAnnotation(SPI.class);
        if (defaultAnnotation != null) {
            String value = defaultAnnotation.value();
            if (value != null && (value = value.trim()).length() > 0) {
                String[] names = NAME_SEPARATOR.split(value);
                if (names.length > 1) {
                    throw new IllegalStateException("more than 1 default extension name on extension " + type.getName()
                            + ": " + Arrays.toString(names));
                }
                if (names.length == 1) {
                    cachedDefaultName = names[0];
                }
            }
        }

        Map<String, Class<?>> extensionClasses = new HashMap<String, Class<?>>();
        loadFile(extensionClasses, RPC_DIRECTORY);
        return extensionClasses;
    }

    /**
     * 加载解析spi配置文件,然后加入缓存
     *
     * @param extensionClasses
     * @param dir
     */
    private void loadFile(Map<String, Class<?>> extensionClasses, String dir) {
        String fileName = dir + type.getName();

        Enumeration<URL> urls;
        ClassLoader classLoader = findClassLoader();
        try {
            if (classLoader != null) {
                urls = classLoader.getResources(fileName);
            } else {
                urls = ClassLoader.getSystemResources(fileName);
            }
            if (urls != null) {
                while (urls.hasMoreElements()) {
                    URL url = urls.nextElement();
                    try {
                        BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), "UTF-8"));
                        try {
                            String line = null;
                            while ((line = reader.readLine()) != null) {
                                final int ci = line.indexOf("#");
                                if (ci > 0) line = line.substring(0, ci);
                                line = line.trim();
                                if (line.length() > 0) {
                                    try {
                                        String name = null;
                                        int i = line.indexOf("=");
                                        if (i > 0) {
                                            name = line.substring(0, i);
                                            line = line.substring(i + 1);
                                        }
                                        if (line.length() > 0) {
                                            Class<?> clazz = Class.forName(name, true, classLoader);
                                            if (!type.isAssignableFrom(clazz)) {
                                                throw new IllegalStateException("Error when load extension class(interface: " +
                                                        type + ", class line: " + clazz.getName() + "), class "
                                                        + clazz.getName() + "is not subtype of interface.");
                                            }
                                            //加入缓存
                                            extensionClasses.put(name, clazz);
                                        }
                                    } catch (Throwable t) {
                                        IllegalStateException e = new IllegalStateException("Failed to load extension class(interface: " + type + ", class line: " + line + ") in " + url + ", cause: " + t.getMessage(), t);
                                        exceptions.put(line, e);
                                    }
                                }
                            } // end of while read lines
                        } finally {
                            reader.close();
                        }
                    } catch (IOException t) {
                        //logger.error("Exception when load extension class(interface: " +
                        //        type + ", class file: " + url + ") in " + url, t);
                    }
                } // end of while urls
            }
        } catch (Throwable t) {
            //logger.error("Exception when load extension class(interface: " +
            //        type + ", description file: " + fileName + ").", t);
        }
    }

    /**
     * //获取类加载器
     *
     * @return
     */
    private ClassLoader findClassLoader() {
        return ExtensionLoader.class.getClassLoader();
    }

    /**
     * 构建异常提示
     *
     * @param name
     * @return
     */
    private IllegalStateException findException(String name) {
        for (Map.Entry<String, IllegalStateException> entry : exceptions.entrySet()) {
            if (entry.getKey().toLowerCase().contains(name.toLowerCase())) {
                return entry.getValue();
            }
        }
        StringBuilder buf = new StringBuilder("No such extension " + type.getName() + " by name " + name);


        int i = 1;
        for (Map.Entry<String, IllegalStateException> entry : exceptions.entrySet()) {
            if (i == 1) {
                buf.append(", possible causes: ");
            }

            buf.append("\r\n(");
            buf.append(i++);
            buf.append(") ");
            buf.append(entry.getKey());
            buf.append(":\r\n");
            buf.append(entry.getValue().toString());
        }
        return new IllegalStateException(buf.toString());
    }

}
