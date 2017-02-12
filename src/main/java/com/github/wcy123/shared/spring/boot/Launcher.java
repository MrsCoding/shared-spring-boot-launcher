package com.github.wcy123.shared.spring.boot;


import java.io.IOException;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class Launcher {
    public static void main(String[] args) throws IOException {
        JarFileExploder exploders[] = new JarFileExploder[args.length];
        for(int i = 0; i < args.length; ++i){
            exploders[i] = new JarFileExploder(args[i]);
        }
        for (JarFileExploder exploder : exploders) {
            exploder.doIt(Paths.get("/tmp/exp"));
        }
    }
}
