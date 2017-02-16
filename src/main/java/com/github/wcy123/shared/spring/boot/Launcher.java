package com.github.wcy123.shared.spring.boot;


import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;

public class Launcher {
    public static void main(String[] args) throws IOException, InterruptedException {

        Config.parseConfig(args);
        final List<Config> configs = Config.getConfigs();

        JarFileExploder exploders[] = new JarFileExploder[configs.size()];
        for (int i = 0; i < configs.size(); ++i) {
            exploders[i] = new JarFileExploder(configs.get(i).getArg(0));
        }
        for (JarFileExploder exploder : exploders) {
            exploder.doIt(Paths.get(getWorkingDirectory()));
        }

        CountDownLatch latch = new CountDownLatch(exploders.length);
        for (int i = 0; i < exploders.length; ++i) {
            createThread(latch, exploders[i], i, configs.get(i));
        }
        latch.await();
    }


    private static void createThread(CountDownLatch latch, JarFileExploder exploder, int index,
            Config config) {
        try {
            final URL[] urls = exploder.getUrls();
            URLClassLoader classLoader = new URLClassLoader(urls);
            final JarFile jarFile = new JarFile(urls[0].getFile());
            final Manifest manifest = jarFile.getManifest();
            String value = manifest.getMainAttributes().getValue("Start-Class");
            if (value == null) {
                System.err.println("cannot find start class in " + urls[0]);
                latch.countDown();
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
                        final String script = "print(java.lang.System.currentTimeMillis());\n" +
                                "org.apache.catalina.webresources.TomcatURLStreamHandlerFactory.disable();";
                        engine.eval(Config.getOwnArg("eval").orElse(script));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    main.invoke(null, new Object[] {config.getArgs()});
                } catch (IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                } finally {
                    latch.countDown();
                }
            });
            runnerThread.setContextClassLoader(classLoader);
            runnerThread.start();
            Thread.sleep(Config.getOwnArg("interval").map(Integer::valueOf).orElse(1000));
        } catch (NoSuchMethodException | IOException | ClassNotFoundException
                | InterruptedException e) {
            e.printStackTrace();
            latch.countDown();
        }
        return;
    }

    private static String getWorkingDirectory() {
        final String work_dir = Config.getOwnArg("work.dir").orElse(
                Optional.ofNullable(System.getenv("WORK_DIR")).orElse(
                        Paths.get(System.getProperty("user.home"), ".your.spring.boot")
                                .toString()));
        Paths.get(work_dir).toFile().mkdirs();
        return work_dir;
    }
}
