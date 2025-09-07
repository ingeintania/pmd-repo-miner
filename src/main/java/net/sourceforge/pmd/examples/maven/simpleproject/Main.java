package net.sourceforge.pmd.examples.maven.simpleproject;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.api.errors.NoHeadException;
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
        //Change your target path on repoPath
        String repoPath = "/Users/ingeintania/Documents/code/commons-lang";
        File repoDir = new File(repoPath);

        int commits = 0, javaFiles = 0;
        try (Git git = Git.open(repoDir)) {
            for (RevCommit c : git.log().call()) {
                commits++;
                javaFiles += countJavaFiles(repoDir);
            }
        }

        // Create JSON
        ObjectMapper mapper = new ObjectMapper();
        ObjectNode root = mapper.createObjectNode();
        root.put("location", repoDir.getAbsolutePath());

        ObjectNode statRepo = root.putObject("stat_of_repository");
        statRepo.put("number_of_commits", commits);

        Double avg_of_num_java_files = (double) javaFiles / commits;
        statRepo.put("avg_of_num_java_files", Math.round(avg_of_num_java_files * 10) / 10.0);

        // Call warnings with PMD
        try {
            PMDConfiguration cfg = new PMDConfiguration();
            cfg.addInputPath(repoDir.toPath());
            cfg.addRuleSet("ruleset.xml");

            try (PmdAnalysis pmd = PmdAnalysis.create(cfg)) {
                Report report = pmd.performAnalysisAndCollectReport();

                Map<String, Integer> allRules = new HashMap<>();

                for (RuleViolation v : report.getViolations()) {
                    String r = v.getRule().getName();
                    allRules.put(r, allRules.getOrDefault(r, 0)+1);
                }
                Double avg_of_num_warnings = (double) report.getViolations().size() / commits;
                statRepo.put("avg_of_num_warnings", Math.round(avg_of_num_warnings * 10) / 10.0);
                ObjectNode statWarnings = root.putObject("stat_of_warnings");
                for (Map.Entry<String, Integer> entry : allRules.entrySet()) {
                    statWarnings.put(entry.getKey(), entry.getValue());
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Save to JSON file
        mapper.writerWithDefaultPrettyPrinter().writeValue(new File("report.json"), root);
        System.out.println("JSON Report generated: report.json");
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
}