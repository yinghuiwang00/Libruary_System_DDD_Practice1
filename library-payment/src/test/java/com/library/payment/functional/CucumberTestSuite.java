package com.library.payment.functional;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/payment")
@ConfigurationParameter(key = "cucumber.glue", value = "com.library.payment.functional")
@ConfigurationParameter(key = "cucumber.plugin", value = "pretty")
public class CucumberTestSuite {
}
