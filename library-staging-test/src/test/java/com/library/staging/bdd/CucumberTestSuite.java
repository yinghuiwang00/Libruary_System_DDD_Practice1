package com.library.staging.bdd;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/integration")
@ConfigurationParameter(key = "cucumber.glue", value = "com.library.staging.bdd")
@ConfigurationParameter(key = "cucumber.plugin", value = "pretty")
public class CucumberTestSuite {
}
