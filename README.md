# Step to Reporoduce

Run with

    mvn clean verify

This should find the following violations in Main.java:

    [WARNING] PMD Failure: net.sourceforge.pmd.examples.maven.simpleproject.Dummy:4 Rule:AbstractClassWithoutAbstractMethod Priority:3 This abstract class does not have any abstract methods.
    [WARNING] PMD Failure: net.sourceforge.pmd.examples.maven.simpleproject.Main:11 Rule:AvoidReassigningCatchVariables Priority:3 Avoid reassigning caught exception 'e'.


## References

*   <https://docs.pmd-code.org/latest/pmd_userdocs_tools_maven.html>
*   <https://maven.apache.org/plugins/maven-pmd-plugin/index.html>
*   <https://github.com/pmd/pmd-examples/tree/main/maven/simple-project>