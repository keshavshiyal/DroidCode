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
        boolean renamed = targetFile.renameTo(destination);
        if (!renamed) {
            throw new RuntimeException("Failed to rename file");
        }
        return destination;
    }

    public boolean deleteFile(File targetFile) {
        if (targetFile == null || !targetFile.exists()) {
            return false;
        }
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
    }
}
