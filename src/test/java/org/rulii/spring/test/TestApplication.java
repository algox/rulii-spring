/*
 * This software is licensed under the Apache 2 license, quoted below.
 *
 * Copyright (c) 1999-2025, Algorithmx Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.rulii.spring.test;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Represents a Spring Boot application class that implements CommandLineRunner interface.
 * This class is used to start the Spring Boot application and handle command-line arguments.
 *
 * @author Max Arulananthan
 *
 */
@SpringBootApplication(scanBasePackages = {"org.rulii.spring.test"})
public class TestApplication implements CommandLineRunner {

    private static final Logger LOG = LoggerFactory.getLogger(TestApplication.class);

    public static void main(String[] args) {
        LOG.info("Starting App");
        SpringApplication.run(TestApplication.class, args);
        LOG.info("Finished App");
    }

    @Override
    public void run(String... args) {
        LOG.info("Command line runner");

        for (int i = 0; i < args.length; ++i) {
            LOG.info("args[{}]: {}", i, args[i]);
        }
    }

}
