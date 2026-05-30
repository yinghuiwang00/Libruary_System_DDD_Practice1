package com.library.notification.functional;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/notification")
@ConfigurationParameter(key = "cucumber.glue", value = "com.library.notification.functional")
@ConfigurationParameter(key = "cucumber.plugin", value = "pretty")
public class CucumberTestSuite {
}
