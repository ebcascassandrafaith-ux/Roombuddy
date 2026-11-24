package com.template.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/** Member B: validates and copies chosen photos/documents into the project's uploads/ folder. */
public final class UploadStore {

    public static final String ROOT = "uploads";
    public static final long MAX_BYTES = 5L * 1024 * 1024;   // 5 MB per file
    public static final Set<String> IMAGES = Set.of("jpg", "jpeg", "png");
    public static final Set<String> DOCUMENTS = Set.of("pdf", "jpg", "jpeg", "png");

    private UploadStore() {}

    /** @return null when the file is acceptable, otherwise a message for the user */
    public static String validate(File f, Set<String> allowed) {
        if (f == null || !f.isFile()) return "The file could not be found.";
        if (f.length() > MAX_BYTES) return f.getName() + " is bigger than " + (MAX_BYTES / (1024 * 1024)) + " MB.";
        if (!allowed.contains(extension(f.getName())))
            return f.getName() + " is not allowed. Use: " + String.join(", ", allowed) + ".";
        return null;
    }

    /** Copies into uploads/<folder>/<random>.<ext> and returns that relative path (this is what the DB stores). */
    public static String store(File f, String folder) throws IOException {
        Path dir = Path.of(ROOT, folder);
        Files.createDirectories(dir);
        String name = UUID.randomUUID().toString().replace("-", "") + "." + extension(f.getName());
        Files.copy(f.toPath(), dir.resolve(name), StandardCopyOption.REPLACE_EXISTING);
        return ROOT + "/" + folder + "/" + name;
    }

    public static String extension(String fileName) {
        int i = fileName.lastIndexOf('.');
        return i < 0 ? "" : fileName.substring(i + 1).toLowerCase(Locale.ROOT);
    }
}
