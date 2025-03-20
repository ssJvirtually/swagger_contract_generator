package org.example.git;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

class BranchInfo {
    String branch;
    String creationTime;
    String commitTime;

    BranchInfo(String branch, String creationTime, String commitTime) {
        this.branch = branch;
        this.creationTime = creationTime;
        this.commitTime = commitTime;
    }
}

public class GitBranchFinder {
    public static void main(String[] args) {
//        if (args.length < 2) {
//            System.out.println("Usage: java GitBranchFinder <commit_id_prefix> <repo_path>");
//            return;
//        }

        String commitIdPrefix = "23b847c";
        String repoPath = "E:\\work\\java-concepts";

        try {
            List<BranchInfo> branches = getAllBranches(repoPath);

            // Sort branches by creation time in descending order
            branches.sort(Comparator.comparing((BranchInfo b) -> b.creationTime).reversed());

            // Search commit in all branches
            for (BranchInfo branchInfo : branches) {
                if (isCommitInBranch(commitIdPrefix, branchInfo.branch, repoPath)) {
                    String commitTime = getCommitTime(commitIdPrefix, repoPath);
                    System.out.println(branchInfo.branch + " - Created: " + branchInfo.creationTime + " - Committed: " + commitTime);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<BranchInfo> getAllBranches(String repoPath) throws Exception {
        List<BranchInfo> branches = new ArrayList<>();
        Process process = new ProcessBuilder("git", "for-each-ref", "--sort=-creatordate", "--format=%(refname:short) %(creatordate)", "refs/heads/")
                .directory(new java.io.File(repoPath))
                .start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            String[] parts = line.split(" ", 2);
            if (parts.length == 2) {
                branches.add(new BranchInfo(parts[0], parts[1], ""));
            }
        }
        process.waitFor();
        return branches;
    }

    private static boolean isCommitInBranch(String commitId, String branch, String repoPath) {
        try {
            Process process = new ProcessBuilder("git", "log", branch, "--pretty=%H")
                    .directory(new java.io.File(repoPath))
                    .start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(commitId)) {
                    return true;
                }
            }
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    private static String getCommitTime(String commitId, String repoPath) {
        try {
            Process process = new ProcessBuilder("git", "show", "-s", "--format=%ci", commitId)
                    .directory(new java.io.File(repoPath))
                    .start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String commitTime = reader.readLine();
            process.waitFor();
            return commitTime;
        } catch (Exception e) {
            return "Unknown";
        }
    }
}
