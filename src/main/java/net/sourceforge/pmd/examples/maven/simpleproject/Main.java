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
        if (args.length < 2) {
            System.out.println("Please input <repoPath> <rulesetPath>");
            return;
        }

        String repoPath = args[0]; // repoPath input
        String rulesetPath = args[1]; // rulesetPath input
        File repoDir = new File(repoPath);

        createJSON(repoDir, rulesetPath);
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

    public static void createJSON(File repoDir, String rulesetPath)
            throws IOException, NoHeadException, GitAPIException {
        int commits = 0;
        int javaFiles = 0;
        int numWarnings = 0;
        int totalJavaFiles = 0;
        int totalWarnings = 0;
        Map<String, Integer> allRules = new HashMap<>();

        Git git = Git.open(repoDir);
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

            PmdAnalysis pmd = PmdAnalysis.create(cfg);
            Report report = pmd.performAnalysisAndCollectReport();
            numWarnings = report.getViolations().size();
            totalWarnings += report.getViolations().size();

            for (RuleViolation v : report.getViolations()) {
                String r = v.getRule().getName();
                allRules.put(r, allRules.getOrDefault(r, 0) + 1);
            }

            // Create JSON on each commit
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode rootCommit = mapper.createObjectNode();
            rootCommit.put("commit_hash", c.getName());
            rootCommit.put("commit_index", commits);
            rootCommit.put("num_java_files", javaFiles);
            rootCommit.put("num_warnings", numWarnings);

            ObjectNode statWarnings = rootCommit.putObject("stat_of_warnings");
            for (Map.Entry<String, Integer> entry : allRules.entrySet()) {
                statWarnings.put(entry.getKey(), entry.getValue());
            }

            // Save JSON on each commit
            File outFile = new File("reports/commits/commit_" + commits + ".json");
            outFile.getParentFile().mkdirs();
            mapper.writerWithDefaultPrettyPrinter().writeValue(outFile, rootCommit);
            System.out.println("Generated JSON for commit " + commits + ": " + outFile.getPath());
        }

        // Create JSON Summary
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
        File outFile = new File("reports/summary.json");
        outFile.getParentFile().mkdirs();
        mapper.writerWithDefaultPrettyPrinter().writeValue(outFile, root);
        System.out.println("Generated JSON summary: " + outFile.getPath());
    }
}