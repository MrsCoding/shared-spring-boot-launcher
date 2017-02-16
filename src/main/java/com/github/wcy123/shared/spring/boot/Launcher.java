package com.github.wcy123.shared.spring.boot;


import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Stream;

public class Launcher {
    public static void main(String[] args) throws IOException, InterruptedException {
        JarFileExploder exploders[] = new JarFileExploder[args.length];
        for(int i = 0; i < args.length; ++i){
            exploders[i] = new JarFileExploder(args[i]);
        }
        for (JarFileExploder exploder : exploders) {
            exploder.doIt(Paths.get(getWorkingDirectory()));
        }

        Set<String> commonURLs = new HashSet<>();
        for (int i = 0; i < exploders.length; ++i) {
            JarFileExploder exploder = exploders[i];
            final URL[] urls = exploder.getUrls();
            Stream.of(urls)
                    .forEach(url -> {
                        if (isCommonJar(url)) {
                            commonURLs.add(url.toString());
                        }
                    });
        }

        final URL[] commonURLs2 = new URL[commonURLs.size()];
        int index = 0;
        for (String commonURL : commonURLs) {
            commonURLs2[index++] = new URL(commonURL);
        }
        ClassLoader commonClassLoader = new URLClassLoader(commonURLs2);
        CountDownLatch latch = new CountDownLatch(args.length);
        for (int i = 0; i < exploders.length; ++i) {
            createThread(latch, exploders[i], i, commonURLs, commonClassLoader);
        }
        latch.await();
    }

    private static boolean isCommonJar(URL url) {
        return url.toString().contains("toxxxmcat-embed");
    }

    private static void createThread(CountDownLatch latch, JarFileExploder exploder, int index,
            Set<String> commonURLs, ClassLoader commonClassLoader) {
        try {
            final URL[] urls =
                    Stream.of(exploder.getUrls())
                            .filter(url -> !commonURLs.contains(url.toString()))
                            .toArray(size -> new URL[size]);
            URLClassLoader classLoader = new URLClassLoader(urls, commonClassLoader);
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
        final String work_dir = System.getenv("WORK_DIR");
        if (work_dir != null) {
            return work_dir;
        }
        final Path path = Paths.get(System.getProperty("user.home"), ".your.spring.boot");
        path.toFile().mkdirs();
        return path.toString();
    }
}
