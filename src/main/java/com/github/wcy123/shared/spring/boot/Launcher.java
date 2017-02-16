package com.github.wcy123.shared.spring.boot;


import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class Launcher {
    public static void main(String[] args) throws IOException, InterruptedException {
        JarFileExploder exploders[] = new JarFileExploder[args.length];
        for(int i = 0; i < args.length; ++i){
            exploders[i] = new JarFileExploder(args[i]);
        }
        for (JarFileExploder exploder : exploders) {
            exploder.doIt(Paths.get(getWorkingDirectory()));
        }

        CountDownLatch latch = new CountDownLatch(args.length);
        for (int i = 0; i < exploders.length; ++i) {
            createThread(latch, exploders[i], i);
        }
        latch.await();
    }


    private static void createThread(CountDownLatch latch, JarFileExploder exploder, int index) {
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
            System.out.println(main);
            Thread runnerThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        main.invoke(null,
                                new Object[] {new String[] {"--server.port=" + (8090 + index)}});
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                    } finally {
                        latch.countDown();
                    }
                }
            });
            runnerThread.setContextClassLoader(classLoader);
            Thread.sleep(index * 10000);
            runnerThread.start();
        } catch (NoSuchMethodException | IOException | ClassNotFoundException
                | InterruptedException e) {
            e.printStackTrace();
            latch.countDown();
        }
        return;
    }

    private static String getWorkingDirectory() {
        final String work_dir = Optional.ofNullable(System.getenv("WORK_DIR"))
                .orElse(Paths.get(System.getProperty("user.home"), ".your.spring.boot").toString());
        Paths.get(work_dir).toFile().mkdirs();
        return work_dir;
    }
}
