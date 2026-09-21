package com.example.filesystem;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class LocalFileSystem {

    private static final LocalFileSystem INSTANCE = new LocalFileSystem();

    private LocalFileSystem() {}

    public static LocalFileSystem getInstance() {
        return INSTANCE;
    }

    public List<FileNode> listDirectory(File directory) {
        return listDirectoryRecursive(directory);
    }

    public List<FileNode> listDirectoryRecursive(File directory) {
        List<FileNode> nodes = new ArrayList<>();
        if (directory == null || !directory.exists() || !directory.isDirectory()) {
            return nodes;
        }

        File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.getName().equals(".DS_Store")) continue;
                FileNode node = FileNode.fromFile(file);
                if (node != null) {
                    if (node.isFolder()) {
                        List<FileNode> subChildren = listDirectoryRecursive(file);
                        node.setChildren(subChildren);
                    }
                    nodes.add(node);
                }
            }
        }

        FileNode dummy = new FileNode("root", directory.getAbsolutePath(), true, 0, 0);
        dummy.setChildren(nodes);
        return dummy.getChildren();
    }

    public File createFile(File parentDir, String fileName) throws Exception {
        if (parentDir == null || !parentDir.exists() || !parentDir.isDirectory()) {
            throw new IllegalArgumentException("Invalid parent directory");
        }
        File newFile = new File(parentDir, fileName);
        if (newFile.exists()) {
            throw new IllegalArgumentException("File already exists: " + fileName);
        }
        boolean created = newFile.createNewFile();
        if (!created) {
            throw new RuntimeException("Could not create file: " + fileName);
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
        File destination = new File(targetFile.getParentFile(), newName);
        if (destination.exists()) {
            throw new IllegalArgumentException("Destination name already exists: " + newName);
        }
        SafUtils.renameInSaf(targetFile, newName);
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
        String baseName;
        String extension;
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex > 0 && !originalName.startsWith(".")) {
            baseName = originalName.substring(0, dotIndex);
            extension = originalName.substring(dotIndex);
        } else {
            baseName = originalName;
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

    public String readFileToString(File file) throws Exception {
        if (file == null || !file.exists() || !file.isFile()) {
            throw new IllegalArgumentException("File does not exist or is a directory");
        }
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8))) {
            String line;
            boolean first = true;
            while ((line = reader.readLine()) != null) {
                if (!first) {
                    builder.append("\n");
                }
                builder.append(line);
                first = false;
            }
        }
        return builder.toString();
    }

    public void writeStringToFile(File file, String content) throws Exception {
        if (file == null) {
            throw new IllegalArgumentException("Target file cannot be null");
        }
        if (!file.exists()) {
            File parent = file.getParentFile();
            if (parent != null && !parent.exists()) {
                parent.mkdirs();
            }
            file.createNewFile();
        }
        try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, false), StandardCharsets.UTF_8))) {
            writer.write(content != null ? content : "");
        }
        SafUtils.syncFileToSaf(file);
    }
}
