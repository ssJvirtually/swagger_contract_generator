package org.example.git;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

class BranchInfo {
    String branch;
    String commitTime;

    BranchInfo(String branch, String commitTime) {
        this.branch = branch;
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
            List<String> branches = getAllBranches(repoPath);
            List<BranchInfo> matchingBranches = new ArrayList<>();

            for (String branch : branches) {
                if (isCommitInBranch(commitIdPrefix, branch, repoPath)) {
                    String commitTime = getCommitTime(commitIdPrefix, repoPath);
                    if (commitTime != null) {
                        matchingBranches.add(new BranchInfo(branch, commitTime));
                    }
                }
            }

            // Sort branches by commit time in descending order
            matchingBranches.sort(Comparator.comparing((BranchInfo b) -> b.commitTime).reversed());

            // Print results
            for (BranchInfo branchInfo : matchingBranches) {
                System.out.println(branchInfo.commitTime + " - " + branchInfo.branch);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static List<String> getAllBranches(String repoPath) throws Exception {
        List<String> branches = new ArrayList<>();
        Process process = new ProcessBuilder("git", "branch", "-r")
                .directory(new java.io.File(repoPath))
                .start();
        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            branches.add(line.trim());
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
            return null;
        }
    }
}
