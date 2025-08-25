package com.jiangyang.testservice.tests;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(classes = TestServiceTestApplication.class)
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {
}


