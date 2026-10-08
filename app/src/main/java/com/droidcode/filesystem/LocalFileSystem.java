package com.droidcode.filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LocalFileSystem {

    private static final LocalFileSystem INSTANCE = new LocalFileSystem();

    private static final Set<String> DEFAULT_IGNORED_DIRS = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
            ".git", ".gradle", ".idea", ".cxx", ".externalNativeBuild", "__pycache__"
    )));

    private LocalFileSystem() {}

    public static LocalFileSystem getInstance() {
        return INSTANCE;
    }

    public static boolean isIgnoredDirectory(String dirName) {
        return dirName != null && DEFAULT_IGNORED_DIRS.contains(dirName);
    }

    /**
     * Shallow listing of directory contents (depth = 1).
     * Subdirectories have empty children lists, enabling on-demand lazy expansion.
     */
    public List<FileNode> listDirectory(File directory) {
        return listDirectory(directory, Collections.emptySet());
    }

    /**
     * Lazy hierarchical listing of directory contents.
     * Only subdirectories whose absolute paths are present in expandedPaths
     * will have their children recursively resolved.
     */
    public List<FileNode> listDirectory(File directory, Set<String> expandedPaths) {
        List<FileNode> nodes = new ArrayList<>();
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            return nodes;
        }

        File[] files = directory.listFiles();
        if (files != null) {
            Arrays.sort(files, (a, b) -> {
                if (a.isDirectory() != b.isDirectory()) {
                    return a.isDirectory() ? -1 : 1;
                }
                return a.getName().compareToIgnoreCase(b.getName());
            });

            for (File file : files) {
                if (file.getName().equals(".DS_Store") || isIgnoredDirectory(file.getName())) continue;
                String cleanName = SafUtils.getCleanFileName(file.getName());
                if (!cleanName.equals(file.getName())) {
                    File cleanTarget = new File(file.getParentFile(), cleanName);
                    if (!cleanTarget.exists()) {
                        SafUtils.renameInSaf(file, cleanName);
                        boolean renamed = file.renameTo(cleanTarget);
                        if (renamed) {
                            file = cleanTarget;
                        }
                    }
                }
                FileNode node = FileNode.fromFile(file);
                if (node != null) {
                    if (node.isFolder()) {
                        if (expandedPaths != null && expandedPaths.contains(file.getAbsolutePath())) {
                            List<FileNode> subChildren = listDirectory(file, expandedPaths);
                            node.setChildren(subChildren);
                        } else {
                            node.setChildren(new ArrayList<>());
                        }
                    }
                    nodes.add(node);
                }
            }
        }
        return nodes;
    }

    /**
     * Recursive directory listing for complete tree inspections / exports.
     */
    public List<FileNode> listDirectoryRecursive(File directory) {
        return listDirectoryRecursiveInternal(directory, 0, 30);
    }

    private List<FileNode> listDirectoryRecursiveInternal(File directory, int depth, int maxDepth) {
        List<FileNode> nodes = new ArrayList<>();
        if (directory == null || !directory.exists() || !directory.isDirectory() || depth >= maxDepth) {
            return nodes;
        }

        File[] files = directory.listFiles();
        if (files != null) {
            Arrays.sort(files, (a, b) -> {
                if (a.isDirectory() != b.isDirectory()) {
                    return a.isDirectory() ? -1 : 1;
                }
                return a.getName().compareToIgnoreCase(b.getName());
            });

            for (File file : files) {
                if (file.getName().equals(".DS_Store") || isIgnoredDirectory(file.getName())) continue;
                String cleanName = SafUtils.getCleanFileName(file.getName());
                if (!cleanName.equals(file.getName())) {
                    File cleanTarget = new File(file.getParentFile(), cleanName);
                    if (!cleanTarget.exists()) {
                        SafUtils.renameInSaf(file, cleanName);
                        boolean renamed = file.renameTo(cleanTarget);
                        if (renamed) {
                            file = cleanTarget;
                        }
                    }
                }
                FileNode node = FileNode.fromFile(file);
                if (node != null) {
                    if (node.isFolder()) {
                        List<FileNode> subChildren = listDirectoryRecursiveInternal(file, depth + 1, maxDepth);
                        node.setChildren(subChildren);
                    }
                    nodes.add(node);
                }
            }
        }
        return nodes;
    }

    public File createFile(File parentDir, String fileName) throws Exception {
        if (parentDir == null || !parentDir.exists() || !parentDir.isDirectory()) {
            throw new IllegalArgumentException("Invalid parent directory");
        }
        String cleanFileName = SafUtils.getCleanFileName(fileName.trim());
        File newFile = new File(parentDir, cleanFileName);
        if (newFile.exists()) {
            throw new IllegalArgumentException("File already exists: " + cleanFileName);
        }
        boolean created = newFile.createNewFile();
        if (!created) {
            throw new RuntimeException("Could not create file: " + cleanFileName);
        }
        SafUtils.syncFileToSaf(newFile);
        return newFile;
    }

    public File createDirectory(File parentDir, String dirName) throws Exception {
        if (parentDir == null || !parentDir.exists() || !parentDir.isDirectory()) {
            throw new IllegalArgumentException("Invalid parent directory");
        }
        File newDir = new File(parentDir, dirName);
        if (newDir.exists()) {
            throw new IllegalArgumentException("Directory already exists: " + dirName);
        }
        boolean created = newDir.mkdirs();
        if (!created) {
            throw new RuntimeException("Could not create directory: " + dirName);
        }
        SafUtils.syncFileToSaf(newDir);
        return newDir;
    }

    public File renameFile(File targetFile, String newName) throws Exception {
        if (targetFile == null || !targetFile.exists()) {
            throw new IllegalArgumentException("Target file does not exist");
        }
        String cleanName = SafUtils.getCleanFileName(newName.trim());
        File destination = new File(targetFile.getParentFile(), cleanName);
        if (destination.exists()) {
            throw new IllegalArgumentException("Destination name already exists: " + cleanName);
        }
        SafUtils.renameInSaf(targetFile, cleanName);
        boolean renamed = targetFile.renameTo(destination);
        if (!renamed) {
            throw new RuntimeException("Failed to rename file");
        }
        return destination;
    }

    public File rename(File targetFile, String newName) throws Exception {
        return renameFile(targetFile, newName);
    }

    public File duplicateFile(File source) throws Exception {
        if (source == null || !source.exists()) {
            throw new IllegalArgumentException("Source file does not exist");
        }
        return copyFileOrDirectory(source, source.getParentFile());
    }

    public File copyFileOrDirectory(File source, File targetDir) throws Exception {
        if (source == null || !source.exists()) {
            throw new IllegalArgumentException("Source file does not exist");
        }
        if (targetDir == null || !targetDir.exists() || !targetDir.isDirectory()) {
            throw new IllegalArgumentException("Target directory is invalid");
        }

        File dest = new File(targetDir, source.getName());
        if (dest.exists()) {
            String uniqueName = getUniqueCopyName(targetDir, source.getName());
            dest = new File(targetDir, uniqueName);
        }

        copyRecursive(source, dest);
        SafUtils.syncFileToSaf(dest);
        return dest;
    }

    private void copyRecursive(File src, File dest) throws Exception {
        if (src.isDirectory()) {
            if (!dest.exists()) {
                boolean created = dest.mkdirs();
                if (!created && !dest.exists()) {
                    throw new RuntimeException("Failed to create directory: " + dest.getAbsolutePath());
                }
            }
            File[] children = src.listFiles();
            if (children != null) {
                for (File child : children) {
                    copyRecursive(child, new File(dest, child.getName()));
                }
            }
        } else {
            File parent = dest.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            try (FileInputStream in = new FileInputStream(src);
                 FileOutputStream out = new FileOutputStream(dest)) {
                byte[] buffer = new byte[8192];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
        }
    }

    public File moveFileOrDirectory(File source, File targetDir) throws Exception {
        if (source == null || !source.exists()) {
            throw new IllegalArgumentException("Source file does not exist");
        }
        if (targetDir == null || !targetDir.exists() || !targetDir.isDirectory()) {
            throw new IllegalArgumentException("Target directory is invalid");
        }

        // Prevent moving a folder into itself or its descendant
        if (source.isDirectory() && targetDir.getAbsolutePath().startsWith(source.getAbsolutePath())) {
            throw new IllegalArgumentException("Cannot move a folder into itself or a subfolder");
        }

        File dest = new File(targetDir, source.getName());
        if (dest.exists()) {
            String uniqueName = getUniqueCopyName(targetDir, source.getName());
            dest = new File(targetDir, uniqueName);
        }

        if (source.renameTo(dest)) {
            SafUtils.renameInSaf(source, dest.getName());
            return dest;
        } else {
            copyRecursive(source, dest);
            deleteFile(source);
            SafUtils.syncFileToSaf(dest);
            return dest;
        }
    }

    private String getUniqueCopyName(File dir, String originalName) {
        String cleanOriginalName = SafUtils.getCleanFileName(originalName);
        String baseName;
        String extension;
        int dotIndex = cleanOriginalName.lastIndexOf('.');
        if (dotIndex > 0 && !cleanOriginalName.startsWith(".")) {
            baseName = cleanOriginalName.substring(0, dotIndex);
            extension = cleanOriginalName.substring(dotIndex);
        } else {
            baseName = cleanOriginalName;
            extension = "";
        }

        String candidate = baseName + "_copy" + extension;
        File testFile = new File(dir, candidate);
        int counter = 1;
        while (testFile.exists()) {
            candidate = baseName + "_copy" + counter + extension;
            testFile = new File(dir, candidate);
            counter++;
        }
        return candidate;
    }

    public boolean deleteFile(File targetFile) {
        if (targetFile == null || !targetFile.exists()) {
            return false;
        }
        SafUtils.deleteFromSaf(targetFile);
        if (targetFile.isDirectory()) {
            File[] contents = targetFile.listFiles();
            if (contents != null) {
                for (File child : contents) {
                    deleteFile(child);
                }
            }
        }
        return targetFile.delete();
    }

    public boolean deleteDirectory(File targetDir) {
        return deleteFile(targetDir);
    }

    public static class FileReadResult {
        public final String content;
        public final String encoding;
        public final boolean hasBom;

        public FileReadResult(String content, String encoding, boolean hasBom) {
            this.content = content != null ? content : "";
            this.encoding = encoding != null ? encoding : "UTF-8";
            this.hasBom = hasBom;
        }
    }

    public FileReadResult readFileWithMetadata(File file) throws Exception {
        if (file == null || !file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("File does not exist or is a directory");
        }
        long fileLength = file.length();
        if (fileLength > 100 * 1024 * 1024) {
            throw new IllegalArgumentException("File exceeds maximum supported size (100MB)");
        }

        byte[] bytes;
        try (FileInputStream fis = new FileInputStream(file)) {
            int len = (int) fileLength;
            bytes = new byte[len];
            int totalRead = 0;
            int read;
            while (totalRead < len && (read = fis.read(bytes, totalRead, len - totalRead)) != -1) {
                totalRead += read;
            }
            if (totalRead < len) {
                bytes = Arrays.copyOf(bytes, totalRead);
            }
        }

        // BOM detection
        if (bytes.length >= 3 &&
                (bytes[0] & 0xFF) == 0xEF &&
                (bytes[1] & 0xFF) == 0xBB &&
                (bytes[2] & 0xFF) == 0xBF) {
            String text = new String(bytes, 3, bytes.length - 3, StandardCharsets.UTF_8);
            return new FileReadResult(text, "UTF-8", true);
        } else if (bytes.length >= 2 &&
                (bytes[0] & 0xFF) == 0xFF &&
                (bytes[1] & 0xFF) == 0xFE) {
            String text = new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16LE);
            return new FileReadResult(text, "UTF-16LE", true);
        } else if (bytes.length >= 2 &&
                (bytes[0] & 0xFF) == 0xFE &&
                (bytes[1] & 0xFF) == 0xFF) {
            String text = new String(bytes, 2, bytes.length - 2, StandardCharsets.UTF_16BE);
            return new FileReadResult(text, "UTF-16BE", true);
        }

        String text = new String(bytes, StandardCharsets.UTF_8);
        return new FileReadResult(text, "UTF-8", false);
    }

    public String readFileToString(File file) throws Exception {
        return readFileWithMetadata(file).content;
    }

    public void writeStringToFile(File file, String content) throws Exception {
        writeStringToFile(file, content, "UTF-8", false);
    }

    public void writeStringToFile(File file, String content, String encoding, boolean includeBom) throws Exception {
        if (file == null) {
            throw new IllegalArgumentException("Target file cannot be null");
        }
        File parent = file.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        File tempFile = File.createTempFile(".tmp_", ".tmp", parent != null ? parent : new File("."));
        java.nio.charset.Charset charset;
        try {
            charset = (encoding != null && !encoding.isEmpty()) ? java.nio.charset.Charset.forName(encoding) : StandardCharsets.UTF_8;
        } catch (Exception e) {
            charset = StandardCharsets.UTF_8;
        }

        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            if (includeBom) {
                if (charset.name().equalsIgnoreCase("UTF-8")) {
                    fos.write(new byte[]{(byte) 0xEF, (byte) 0xBB, (byte) 0xBF});
                } else if (charset.name().equalsIgnoreCase("UTF-16LE")) {
                    fos.write(new byte[]{(byte) 0xFF, (byte) 0xFE});
                } else if (charset.name().equalsIgnoreCase("UTF-16BE")) {
                    fos.write(new byte[]{(byte) 0xFE, (byte) 0xFF});
                }
            }
            byte[] bytes = (content != null ? content : "").getBytes(charset);
            fos.write(bytes);
            fos.flush();
            try {
                fos.getFD().sync();
            } catch (Exception ignored) {}
        }

        // Atomic replace via rename
        boolean renamed = tempFile.renameTo(file);
        if (!renamed) {
            if (file.exists()) {
                file.delete();
            }
            renamed = tempFile.renameTo(file);
            if (!renamed) {
                // Fallback copy if filesystem doesn't allow direct atomic rename
                try (FileInputStream in = new FileInputStream(tempFile);
                     FileOutputStream out = new FileOutputStream(file)) {
                    byte[] buf = new byte[8192];
                    int n;
                    while ((n = in.read(buf)) != -1) {
                        out.write(buf, 0, n);
                    }
                    out.flush();
                    try {
                        out.getFD().sync();
                    } catch (Exception ignored) {}
                } finally {
                    tempFile.delete();
                }
            }
        }
        SafUtils.syncFileToSaf(file);
    }
}
