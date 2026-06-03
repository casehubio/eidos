package io.casehub.eidos.graph;

import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@AnalyzeClasses(packages = {"io.casehub.eidos.graph", "io.casehub.eidos.api"})
class GraphArchitectureTest {

    @ArchTest
    static final ArchRule entities_must_not_be_referenced_from_api =
        noClasses().that().resideInAPackage("io.casehub.eidos.api..")
            .should().dependOnClassesThat()
            .resideInAPackage("io.casehub.eidos.graph.entity..");
}
