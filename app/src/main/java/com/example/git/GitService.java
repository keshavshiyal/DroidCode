package com.example.git;

import com.example.project.Project;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class GitService {

    private static final GitService INSTANCE = new GitService();

    private GitService() {}

    public static GitService getInstance() {
        return INSTANCE;
    }

    public GitStatus inspectWorkspace(Project project) {
        if (project == null) {
            return GitStatus.notARepo();
        }

        File projectDir = project.getDirectory();
        if (projectDir == null || !projectDir.exists()) {
            return GitStatus.notARepo();
        }

        File gitDir = new File(projectDir, ".git");
        if (!gitDir.exists() || !gitDir.isDirectory()) {
            return GitStatus.notARepo();
        }

        String branch = "main";
        File headFile = new File(gitDir, "HEAD");
        if (headFile.exists() && headFile.isFile()) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(headFile), StandardCharsets.UTF_8))) {
                String line = reader.readLine();
                if (line != null && line.startsWith("ref: refs/heads/")) {
                    branch = line.substring("ref: refs/heads/".length()).trim();
                } else if (line != null && line.length() >= 7) {
                    branch = line.substring(0, 7); // Detached HEAD commit hash
                }
            } catch (Exception e) {
                android.util.Log.w("GitService", "Failed to parse git HEAD ref", e);
            }
        }

        return new GitStatus(true, branch, 0, 0, 0);
    }
}
