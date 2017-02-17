package com.github.wcy123.shared.spring.boot;


import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Launcher {
    public static void main(String[] args) throws IOException, InterruptedException {
        Config.parseConfig(args);
        final List<Config> configs = Config.getConfigs();
        JarRunner runners[] =
                configs.stream().map(JarRunner::new).toArray(size -> new JarRunner[size]);
        Stream.of(runners).forEach(JarRunner::explodeJar);
        Stream.of(runners).forEach(JarRunner::createThread);
        int total = configs.size();
        while (true) {
            Stream.of(runners).forEach(JarRunner::clearThreads);
            Thread.getAllStackTraces().keySet().forEach(
                    thread -> Stream.of(runners).forEach(r -> r.maybeAddThread(thread)));
            final List<JarRunner> runningList =
                    Stream.of(runners).filter(JarRunner::isAlive).collect(Collectors.toList());
            final List<JarRunner> endedList =
                    Stream.of(runners).filter(JarRunner::isNotAlive).collect(Collectors.toList());
            final long ended = endedList.size();
            final long running = runningList.size();
            System.out.println("running = " + running + "; ended = " + ended);
            final String endedThreads =
                    endedList.stream().map(JarRunner::getName).collect(Collectors.joining(","));
            final String runningThreads = runningList.stream().filter(JarRunner::isAlive)
                    .map(JarRunner::getName).collect(Collectors.joining(","));
            if (running > 0) {
                System.out.println("runing thread: " + runningThreads);
                runningList.forEach(JarRunner::dumpThreads);
            }
            if (ended > 0) {
                System.out.println("ended thread: " + endedThreads);
                endedList.forEach(JarRunner::dumpThreads);
            }
            Thread.sleep(3000);
            if (ended == total) {
                break;
            }
        }
    }

}
