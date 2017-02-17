package com.github.wcy123.shared.spring.boot;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

public class JarRunner {
    private JarFileExploder exploder;
    private MyURLClassLoader classLoader;
    private Config config;
    private String name;
    private String jarFileName;
    private List<Thread> threads = new ArrayList<>();

    public JarRunner(Config config) {
        this.config = config;
        jarFileName = config.getArg(0);
        name = Paths.get(jarFileName).getFileName().toString();
        exploder = new JarFileExploder(jarFileName);
    }

    private static String getWorkingDirectory() {
        final String work_dir = Config.getOwnArg("work.dir").orElse(
                Optional.ofNullable(System.getenv("WORK_DIR")).orElse(
                        Paths.get(System.getProperty("user.home"), ".your.spring.boot")
                                .toString()));
        Paths.get(work_dir).toFile().mkdirs();
        return work_dir;
    }

    public void explodeJar() {
        try {
            exploder.doIt(Paths.get(getWorkingDirectory()));
        } catch (IOException e) {
            System.err.println("cannot explored " + jarFileName);
            e.printStackTrace();
        }
        return;
    }

    public void createThread() {
        try {
            final URL[] urls = exploder.getUrls();
            classLoader = new MyURLClassLoader(urls, name);
            final JarFile jarFile = new JarFile(urls[0].getFile());
            final Manifest manifest = jarFile.getManifest();
            String value = manifest.getMainAttributes().getValue("Start-Class");
            if (value == null) {
                System.err.println("cannot find start class in " + urls[0]);
                return;
            }
            jarFile.close();
            final Class<?> aClass = Class.forName(value, true, classLoader);
            final Method main = aClass.getMethod("main", String[].class);
            Thread runnerThread = new Thread(() -> {
                try {
                    ScriptEngineManager engineManager =
                            new ScriptEngineManager();
                    ScriptEngine engine = engineManager.getEngineByName("nashorn");
                    try {
                        // language=Nashorn JS
                        final String script =
                                "org.apache.catalina.webresources.TomcatURLStreamHandlerFactory.disable();";
                        engine.eval(Config.getOwnArg("eval").orElse(script));
                    } catch (Exception e) {
                        // e.printStackTrace();
                    }
                    main.invoke(null, new Object[] {config.getArgs()});
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
                System.out.println("Thread " + Thread.currentThread().getName() + " ended");
            });
            runnerThread.setName(aClass.getName());
            runnerThread.setContextClassLoader(classLoader);
            runnerThread.start();
            Thread.sleep(Config.getOwnArg("interval").map(Integer::valueOf).orElse(1000));
            return;
        } catch (NoSuchMethodException | IOException | ClassNotFoundException
                | InterruptedException e) {
            e.printStackTrace();
        }
        return;
    }

    public boolean isMyThread(Thread thread) {
        return !thread.isDaemon() && thread.getContextClassLoader() == classLoader;
    }

    public void clearThreads() {
        threads.clear();
    }

    public void maybeAddThread(Thread thread) {
        if (isMyThread(thread)) {
            threads.add(thread);
        }
    }

    public boolean isAlive() {
        return !threads.isEmpty();
    }

    public boolean isNotAlive() {
        return !isAlive();
    }

    public String getName() {
        return name;
    }

    public void dumpThreads() {
        System.out.println("Running (" + name + ")");
        int i = 0;
        for (Thread thread : threads) {
            if (i < 15) {
                System.out.println("    " + i + ":" + thread.getName());
            } else if (i == 15) {
                System.out.println("    ....");
            }
            i++;
        }
    }
}
