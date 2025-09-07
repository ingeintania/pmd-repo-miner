# PMD Repository Miner

Run with

    mvn exec:java -Dexec.mainClass="net.sourceforge.pmd.examples.maven.simpleproject.Main" -Dexec.args="<repoPath> <rulesetPath>"
  
    example:
    mvn exec:java -Dexec.mainClass="net.sourceforge.pmd.examples.maven.simpleproject.Main" -Dexec.args="/Users/ingeintania/Documents/code/commons-lang /Users/ingeintania/Documents/code/pmd-repo-miner/ruleset.xml"

This should generate JSON on each commit commit_[index of commit].json, the format should be as follows:

    {
        "commit_hash" : "d6f6665f263c374e2d49c5e6e54c10dc91e5a398",
        "commit_index" : 1,
        "num_java_files" : 12,
        "num_warnings" : 7,
        "stat_of_warnings" : {
            "UnusedPrivateField" : 4,
            "SystemPrintln" : 3,
            ...
        }
    }

This also should generate summary.json, the format should be as follows:

    {
        "location" : "/a/b/c",
        "stat_of_repository" : {
            "number_of_commits" : 8831,
            "avg_of_num_java_files" : 520.0,
            "avg_of_num_warnings" : 0.5
        },
        "stat_of_warnings" : {
            "LooseCoupling" : 57,
            "UnnecessaryVarargsArrayCreation" : 149,
            "MissingOverride" : 1,
            ...
        }
    }

Requirements
    
    Java Development Kit (JDK) 11+
    Apache Maven 3.9
    A valid Git repository cloned locally

## References

*   <https://docs.pmd-code.org/latest/pmd_userdocs_tools_maven.html>
*   <https://maven.apache.org/plugins/maven-pmd-plugin/index.html>
*   <https://github.com/pmd/pmd-examples/tree/main/maven/simple-project>
*   <https://git-scm.com/book/pl/v2/Appendix-B:-Embedding-Git-in-your-Applications-JGit>