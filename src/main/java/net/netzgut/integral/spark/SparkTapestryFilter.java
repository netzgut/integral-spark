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
package net.netzgut.integral.spark;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.apache.tapestry5.ioc.Registry;
import org.apache.tapestry5.ioc.RegistryBuilder;
import org.apache.tapestry5.ioc.annotations.ImportModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import spark.servlet.SparkApplication;
import spark.servlet.SparkFilter;
import spark.utils.StringUtils;

public class SparkTapestryFilter extends SparkFilter {

    private static final Logger          LOG                            =
        LoggerFactory.getLogger(SparkTapestryFilter.class);

    /**
     * If you use the autodicover feature the filter searches all availables class in any
     * available classloaders for Spark applications. This might not be the behaviour you want,
     * because it could impact startup time in a bad way. That's way you can restrict what actually
     * should be scanned by providing a comma-separated list of search spec, see
     * https://github.com/lukehutch/fast-classpath-scanner/wiki/2.-Constructor
     * for more infos about the the spec.
     */
    private static final String          AUTODISCOVER_SEARCH_SPEC_PARAM = "autodiscoverSearchSpec";

    private Registry                     registry;

    private final List<SparkApplication> applications                   = new ArrayList<>();

    @SuppressWarnings("rawtypes")
    @Override
    protected SparkApplication[] getApplications(FilterConfig filterConfig) throws ServletException {

        // We support 2 modes:
        // - Applications configured in web.xml via init-param "applicationClass"
        // - Auto-Discovery (with package restrictions via init-param autodiscoverSearchSpec)

        List<Class<? extends SparkApplication>> applicationClasses = new ArrayList<>();
        List<Class> tapestryModules = new ArrayList<>();

        String servletInitParamApplications = filterConfig.getInitParameter(APPLICATION_CLASS_PARAM);

        // If we don't find an init-param in the web.xml we try to autodiscover.
        // We can restrict the packages searched by an init-param, too.
        if (StringUtils.isBlank(servletInitParamApplications)) {
            LOG.debug("Autodiscover SparkApplications");
            String searchSpec = filterConfig.getInitParameter(AUTODISCOVER_SEARCH_SPEC_PARAM);
            autodiscover(applicationClasses, tapestryModules, searchSpec);
        }
        else {
            LOG.debug("Processing Servlet Init Param: {}", servletInitParamApplications);
            processInitParam(servletInitParamApplications, applicationClasses, tapestryModules);
        }

        this.registry =
            RegistryBuilder.buildAndStartupRegistry(tapestryModules.stream().toArray(size -> new Class[size]));

        for (Class<? extends SparkApplication> application : applicationClasses) {
            LOG.debug("Autobuilding: {}", application);
            SparkApplication app = this.registry.autobuild(application);
            this.applications.add(app);
        }

        return this.applications.stream().toArray(size -> new SparkApplication[size]);
    }

    @SuppressWarnings("rawtypes")
    private void processInitParam(String servletInitParamApplications,
                                  List<Class<? extends SparkApplication>> applicationClasses,
                                  List<Class> tapestryModules) throws ServletException {

        String[] sparkApplications = servletInitParamApplications.split(",");
        if (sparkApplications == null) {
            throw new ServletException("There are no Spark applications configured in the filter.");
        }

        for (String application : sparkApplications) {
            Class<? extends SparkApplication> applicationClass = getApplicationClass(application);
            detectTapestryModules(applicationClass, tapestryModules);
            applicationClasses.add(applicationClass);
        }
    }

    @SuppressWarnings("rawtypes")
    private void autodiscover(List<Class<? extends SparkApplication>> applicationClasses,
                              List<Class> tapestryModules,
                              String searchSpec) {
        FastClasspathScanner scanner = null;
        if (searchSpec == null) {
            scanner = new FastClasspathScanner();
        }
        else {
            LOG.debug("Autodiscover Search Spec: {}", searchSpec);
            scanner = new FastClasspathScanner(searchSpec.split(","));
        }

        scanner.matchClassesImplementing(SparkApplication.class, //
                                         (applicationClass) -> {
                                             detectTapestryModules(applicationClass, tapestryModules);
                                             applicationClasses.add(applicationClass);
                                         })
               .scan();
    }

    @SuppressWarnings("rawtypes")
    private void detectTapestryModules(Class<? extends SparkApplication> applicationClass,
                                       List<Class> tapestryModules) {
        ImportModule importModule = applicationClass.getAnnotation(ImportModule.class);
        if (importModule == null) {
            return;
        }

        LOG.debug("Found tapestry modules: {}", importModule.value().toString());

        Stream.of(importModule.value()).forEach(tapestryModules::add);
    }

    @SuppressWarnings("unchecked")
    private Class<? extends SparkApplication> getApplicationClass(String className) throws ServletException {
        Class<?> clazz = null;
        try {
            clazz = Class.forName(className);
        }
        catch (Exception e) {
            throw new ServletException(e);
        }

        if (clazz.isAssignableFrom(SparkApplication.class) == false) {
            String msg = String.format("Class '%s' is not implementing interface SparkApplication", className);
            throw new ServletException(msg);
        }

        return (Class<? extends SparkApplication>) clazz;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException,
                                                                                              ServletException {
        try {
            super.doFilter(request, response, chain);
        }
        finally {
            this.registry.cleanupThread();
        }
    }

    @Override
    public void destroy() {
        super.destroy();

        this.registry.shutdown();
        this.applications.forEach(SparkApplication::destroy);
    }

}
