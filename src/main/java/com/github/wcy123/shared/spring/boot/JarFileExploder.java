package com.github.wcy123.shared.spring.boot;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public class JarFileExploder {
    public static final int BUF_SIZE = 8 * 1024;
    private final Path fileName;
    private List<File> libFiles = new ArrayList<>();
    private JarFile jarFile;
    private Path workDir;
    private JarOutputStream ownFile;

    private List<URL> urls = new ArrayList<>();

    public JarFileExploder(String fileName) {
        this.fileName = Paths.get(fileName);
    }

    public static long copy(InputStream from, OutputStream to)
            throws IOException {
        checkNotNull(from, "from");
        checkNotNull(to, "to");
        byte[] buf = new byte[BUF_SIZE];
        long total = 0;
        while (true) {
            int r = from.read(buf);
            if (r == -1) {
                break;
            }
            to.write(buf, 0, r);
            total += r;
        }
        return total;
    }

    public static Path makePath(Path dirName, Path fileName) {
        return Paths.get(dirName.toString(), fileName.getFileName().toString());
    }

    public static Path makePath(Path dirName, String fileName) {
        return Paths.get(dirName.toString(), fileName);
    }

    public static FileOutputStream open(Path filename) throws FileNotFoundException {
        return new FileOutputStream(filename.toFile());
    }

    private static void checkNotNull(Object o, String name) {
        if (o == null)
            throw new NullPointerException("\"" + name + "\" is null!");
    }

    public URL[] getUrls() {
        return urls.toArray(new URL[urls.size()]);
    }

    public void doIt(Path dirName) throws IOException {
        if(!dirName.toFile().isDirectory()) {
            throw new IllegalArgumentException("not a directory " +dirName);
        }
        dirName.toFile().mkdirs();
        this.workDir = dirName;
        final Path ownJarName = makePath(dirName, fileName);
        try(final JarFile jarFile = new JarFile(fileName.toFile());
                final JarOutputStream ownFile =
                        new JarOutputStream(new FileOutputStream(ownJarName.toFile()))
        ){
            urls.add(fileName.toFile().toURI().toURL());
            this.jarFile = jarFile;
            this.ownFile = ownFile;
            final Enumeration<JarEntry> entries = jarFile.entries();
            while(entries.hasMoreElements()){
                final JarEntry jarEntry = entries.nextElement();
                filter(jarEntry);
            }
        }
    }

    private void filter(JarEntry jarEntry) throws IOException {
        if (jarEntry.getName().endsWith(".jar")) {
            final Path targetFileName = makePath(workDir, jarEntry.getName());
            targetFileName.getParent().toFile().mkdirs();
            urls.add(targetFileName.toFile().toURI().toURL());
            if(targetFileName.toFile().exists()) {
                info("already exits. skiping " + targetFileName);
            }else{
                try (final InputStream inputStream = jarFile.getInputStream(jarEntry);
                        final FileOutputStream outputStream = open(targetFileName)) {
                    info("exploading " + targetFileName);
                    copy(inputStream, outputStream);
                }
            }
        } else if(jarEntry.getName().startsWith("org/springframework/boot/loader/")) {
            // ignore spring boot loader
        } else {
            addEnty(jarEntry);
        }
    }

    private void addEnty(JarEntry jarEntry) throws IOException {
        if (jarEntry.isDirectory()) {
            addDirEntry(jarEntry);
        } else {
            addFileEntry(jarEntry);
        }
    }

    private void addDirEntry(JarEntry sourceEntry) throws IOException {
        info("exploding: create directory entry: " + sourceEntry.getName());
        JarEntry entry = new JarEntry(sourceEntry.getName());
        entry.setTime(sourceEntry.getLastModifiedTime().to(TimeUnit.MILLISECONDS));
        ownFile.putNextEntry(entry);
        ownFile.closeEntry();
    }

    private void addFileEntry(JarEntry sourceEntry) throws IOException {
        info("exploding: create file entry: " + sourceEntry.getName());
        JarEntry entry = new JarEntry(sourceEntry.getName());
        entry.setTime(sourceEntry.getLastModifiedTime().to(TimeUnit.MILLISECONDS));
        ownFile.putNextEntry(entry);
        BufferedInputStream in = new BufferedInputStream(jarFile.getInputStream(sourceEntry));
        byte[] buffer = new byte[BUF_SIZE];
        while (true)
        {
            int count = in.read(buffer);
            if (count == -1)
                break;
            ownFile.write(buffer, 0, count);
        }
        ownFile.closeEntry();
    }

    private void info(Object obj) {
        System.out.println(obj);
    }

}
