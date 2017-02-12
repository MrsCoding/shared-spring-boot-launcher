package com.github.wcy123.shared.spring.boot;


import java.io.IOException;
import java.nio.file.Paths;

public class Launcher {
    public static void main(String[] args) throws IOException {
        JarFileExploder exploders[] = new JarFileExploder[args.length];
        for(int i = 0; i < args.length; ++i){
            exploders[i] = new JarFileExploder(args[i]);
        }
        for (JarFileExploder exploder : exploders) {
            exploder.doIt(Paths.get(getWorkingDirectory()));
        }
        for (JarFileExploder exploder : exploders) {
            System.out.println(exploder.getUrls());
        }
    }

    private static String getWorkingDirectory() {
        final String work_dir = System.getenv("WORK_DIR");
        if (work_dir != null) {
            return work_dir;
        }
        return Paths.get(System.getProperty("user.home"), ".your.spring.boot").toString();
    }
}
