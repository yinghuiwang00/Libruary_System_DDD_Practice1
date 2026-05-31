package com.library.integration.bdd;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/integration")
@ConfigurationParameter(key = "cucumber.glue", value = "com.library.integration.bdd")
@ConfigurationParameter(key = "cucumber.plugin", value = "pretty")
public class CucumberTestSuite {
}
