package com.quincaisoft;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
class ModuleBoundaryTest {
 @Test void businessModulesHaveNoDependencyCycles(){ApplicationModules.of(QuincaiSoftApplication.class).verify();}
}
