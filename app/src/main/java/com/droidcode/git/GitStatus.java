package com.droidcode.git;

public class GitStatus {

    private final boolean isGitRepo;
    private final String currentBranch;
    private final int modifiedFilesCount;
    private final int stagedFilesCount;
    private final int untrackedFilesCount;

    public GitStatus(boolean isGitRepo, String currentBranch, int modifiedFilesCount, int stagedFilesCount, int untrackedFilesCount) {
        this.isGitRepo = isGitRepo;
        this.currentBranch = currentBranch != null ? currentBranch : "Not a Git repository";
        this.modifiedFilesCount = modifiedFilesCount;
        this.stagedFilesCount = stagedFilesCount;
        this.untrackedFilesCount = untrackedFilesCount;
    }

    public static GitStatus notARepo() {
        return new GitStatus(false, "Not a Git repository", 0, 0, 0);
    }

    public boolean isGitRepo() {
        return isGitRepo;
    }

    public String getCurrentBranch() {
        return currentBranch;
    }

    public int getModifiedFilesCount() {
        return modifiedFilesCount;
    }

    public int getStagedFilesCount() {
        return stagedFilesCount;
    }

    public int getUntrackedFilesCount() {
        return untrackedFilesCount;
    }
}
