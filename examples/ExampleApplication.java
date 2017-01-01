/**
 * Copyright 2017 Netzgut GmbH <info@netzgut.net>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.netzgut.integral.spark.examples;

import javax.inject.Inject;

import org.apache.tapestry5.ioc.annotations.ImportModule;
import org.apache.tapestry5.ioc.annotations.Symbol;

import spark.Spark;
import spark.servlet.SparkApplication;

@ImportModule(ExampleModule.class)
public class ExampleApplication implements SparkApplication {

    @Inject
    @Symbol("welcome-message")
    private String welcomeMessage;

    @Override
    public void init() {
        Spark.get("/hello", (req, res) -> this.welcomeMessage);
    }

}
