# PMD Repository Miner

Run with

    mvn exec:java -Dexec.mainClass="net.sourceforge.pmd.examples.maven.simpleproject.Main"

This should generate report.json, the format should be as follows:

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