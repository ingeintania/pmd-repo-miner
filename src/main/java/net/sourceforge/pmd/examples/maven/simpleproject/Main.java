package net.sourceforge.pmd.examples.maven.simpleproject;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.CheckoutConflictException;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.InvalidRefNameException;
import org.eclipse.jgit.api.errors.NoHeadException;
import org.eclipse.jgit.api.errors.RefAlreadyExistsException;
import org.eclipse.jgit.api.errors.RefNotFoundException;
import org.eclipse.jgit.revwalk.RevCommit;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import net.sourceforge.pmd.PMDConfiguration;
import net.sourceforge.pmd.PmdAnalysis;
import net.sourceforge.pmd.reporting.Report;
import net.sourceforge.pmd.reporting.RuleViolation;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class Main {
    public static void main(String[] args) throws IOException, NoHeadException, GitAPIException {
        // Change your target path on repoPath
        if (args.length < 2) {
            System.out.println("Please input <repoPath> <rulesetPath>");
            return;
        }

        String repoPath = args[0]; // repoPath input
        String rulesetPath = args[1]; // rulesetPath input
        File repoDir = new File(repoPath);

        jsonEachCommit(repoDir, rulesetPath);
        jsonSummary(repoDir, rulesetPath);
    }

    private static int countJavaFiles(File dir) {
        int count = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isDirectory()) {
                    count += countJavaFiles(f);
                } else if (f.getName().endsWith(".java")) {
                    count++;
                }
            }
        }
        return count;
    }

    public static void jsonEachCommit(File repoDir, String rulesetPath)
            throws IOException, NoHeadException, RefAlreadyExistsException, RefNotFoundException,
            InvalidRefNameException, CheckoutConflictException, GitAPIException {
        int commitIndex = 0;

        Git git = Git.open(repoDir);
        for (RevCommit c : git.log().all().call()) {
            // Checkout tiap commit
            git.reset().setMode(org.eclipse.jgit.api.ResetCommand.ResetType.HARD).call();
            git.clean().setCleanDirectories(true).setForce(true).call();
            git.checkout().setName(c.getName()).setForced(true).call();
            commitIndex++;

            // Hitung Java files di commit ini
            int javaFiles = countJavaFiles(repoDir);

            // Run PMD di commit ini
            PMDConfiguration cfg = new PMDConfiguration();
            cfg.addInputPath(repoDir.toPath());
            cfg.addRuleSet(rulesetPath);

            Map<String, Integer> commitRules = new HashMap<>();
            int numWarnings = 0;

            try (PmdAnalysis pmd = PmdAnalysis.create(cfg)) {
                Report report = pmd.performAnalysisAndCollectReport();
                numWarnings = report.getViolations().size();

                for (RuleViolation v : report.getViolations()) {
                    String r = v.getRule().getName();
                    commitRules.put(r, commitRules.getOrDefault(r, 0) + 1);
                }
            }

            // Create JSON per commit
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode rootCommit = mapper.createObjectNode();
            rootCommit.put("commit_hash", c.getName());
            rootCommit.put("commit_index", commitIndex);
            rootCommit.put("num_java_files", javaFiles);
            rootCommit.put("num_warnings", numWarnings);

            ObjectNode statWarnings = rootCommit.putObject("stat_of_warnings");
            for (Map.Entry<String, Integer> entry : commitRules.entrySet()) {
                statWarnings.put(entry.getKey(), entry.getValue());
            }

            // Simpan file JSON
            File outFile = new File("reports/commits/commit_" + commitIndex + ".json");
            outFile.getParentFile().mkdirs(); // bikin folder kalau belum ada
            mapper.writerWithDefaultPrettyPrinter().writeValue(outFile, rootCommit);

            System.out.println("Generated JSON for commit " + commitIndex + ": " + outFile.getPath());
        }
    }

    public static void jsonSummary(File repoDir, String rulesetPath)
            throws IOException, NoHeadException, GitAPIException {
        // Call warnings with PMD
        int commits = 0;
        int javaFiles = 0;
        int totalJavaFiles = 0;
        int totalWarnings = 0;
        Map<String, Integer> allRules = new HashMap<>();

        try (Git git = Git.open(repoDir)) {
            for (RevCommit c : git.log().all().call()) {
                git.reset().setMode(org.eclipse.jgit.api.ResetCommand.ResetType.HARD).call();
                git.clean().setCleanDirectories(true).setForce(true).call();
                git.checkout().setName(c.getName()).setForced(true).call();
                commits++;

                javaFiles = countJavaFiles(repoDir);
                totalJavaFiles += javaFiles;

                PMDConfiguration cfg = new PMDConfiguration();
                cfg.addInputPath(repoDir.toPath());
                cfg.addRuleSet(rulesetPath);

                try (PmdAnalysis pmd = PmdAnalysis.create(cfg)) {
                    Report report = pmd.performAnalysisAndCollectReport();
                    totalWarnings += report.getViolations().size();

                    for (RuleViolation v : report.getViolations()) {
                        String r = v.getRule().getName();
                        allRules.put(r, allRules.getOrDefault(r, 0) + 1);
                    }
                }
            }
        }

        // Create JSON
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        root.put("location", repoDir.getAbsolutePath());

        ObjectNode statRepo = root.putObject("stat_of_repository");
        statRepo.put("number_of_commits", commits);

        Double avg_of_num_java_files = (double) totalJavaFiles / commits;
        statRepo.put("avg_of_num_java_files", Math.round(avg_of_num_java_files * 10) / 10.0);

        Double avg_of_num_warnings = (double) totalWarnings / commits;
        statRepo.put("avg_of_num_warnings", Math.round(avg_of_num_warnings * 10) / 10.0);
        ObjectNode statWarnings = root.putObject("stat_of_warnings");
        for (Map.Entry<String, Integer> entry : allRules.entrySet()) {
            statWarnings.put(entry.getKey(), entry.getValue());
        }

        // Save JSON Summary
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File("reports/summary.json"), root);
        System.out.println("JSON Report generated: summary.json");
    }
}