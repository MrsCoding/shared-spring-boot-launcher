package com.github.wcy123.shared.spring.boot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class Config {
    private final static Config ownConfig = new Config();
    private static List<? extends Config> configs;
    private final List<String> args = new ArrayList<>();

    public static List<Config> parseConfig(String[] args) {
        Config c = ownConfig;
        List<Config> ret = new ArrayList<>();
        for (int i = 0; i < args.length; ++i) {
            String arg = args[i];
            if (!arg.equals("-jar")) {
                c.addArg(arg);
            } else {
                c = new Config();
                ret.add(c);
                i = i + 1;
                if (i >= args.length) {
                    throw new IndexOutOfBoundsException();
                }
                c.addArg(args[i]);
                c.addArg("--spring.jmx.enabled=false");
            }
        }
        configs = ret;
        return ret;
    }

    public static Optional<String> getOwnArg(String s) {
        for (String arg : ownConfig.args) {
            final String prefix = "--" + s + "=";
            if (arg.startsWith(prefix)) {
                return Optional.of(arg.substring(prefix.length()));
            }
        }
        return Optional.empty();
    }

    public static List<Config> getConfigs() {
        return Collections.unmodifiableList(configs);
    }

    @Override
    public String toString() {
        return String.join(" ", args);
    }

    private void addArg(String arg) {
        this.args.add(arg);
    }

    public String getArg(int i) {
        return args.get(i);
    }

    public String[] getArgs() {
        final String[] strings = new String[args.size()];
        return args.toArray(strings);
    }
}
