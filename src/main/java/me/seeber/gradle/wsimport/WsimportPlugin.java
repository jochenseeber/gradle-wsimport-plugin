/**
 * BSD 2-Clause License
 *
 * Copyright (c) 2016-2017, Jochen Seeber
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package me.seeber.gradle.wsimport;

import java.io.File;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNull;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.internal.file.FileOperations;
import org.gradle.api.plugins.JavaPlugin;
import org.gradle.api.tasks.SourceSet;
import org.gradle.jvm.JvmBinarySpec;
import org.gradle.language.base.LanguageSourceSet;
import org.gradle.language.base.plugins.ComponentModelBasePlugin;
import org.gradle.language.java.JavaSourceSet;
import org.gradle.language.jvm.JvmResourceSet;
import org.gradle.model.Defaults;
import org.gradle.model.Each;
import org.gradle.model.Finalize;
import org.gradle.model.ModelMap;
import org.gradle.model.Mutate;
import org.gradle.model.Path;
import org.gradle.model.RuleSource;
import org.gradle.model.Validate;
import org.gradle.platform.base.ComponentType;
import org.gradle.platform.base.TypeBuilder;
import org.gradle.plugins.ide.eclipse.GenerateEclipseClasspath;

import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

/**
 * Plugin to run wsimport
 */
public class WsimportPlugin implements Plugin<Project> {

    /**
     * Converter to convert configuration names to task names
     */
    protected static final Converter<String, String> TASK_NAME_CONVERTER = Objects
            .requireNonNull(CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.UPPER_CAMEL));

    /**
     * Converter to convert configuration names to task names
     */
    protected static final Converter<String, String> COMPONENT_NAME_CONVERTER = Objects
            .requireNonNull(CaseFormat.UPPER_CAMEL.converterTo(CaseFormat.LOWER_CAMEL));

    /**
     * Converter to convert configuration names to directory names
     */
    protected static final Converter<String, String> DIRECTORY_NAME_CONVERTER = Objects
            .requireNonNull(CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.LOWER_HYPHEN));

    /**
     * Default source sets
     */
    protected static final Set<@NonNull String> DEFAULT_SOURCE_SET_NAMES = Objects
            .requireNonNull(ImmutableSet.<@NonNull String> of(Objects.requireNonNull(SourceSet.MAIN_SOURCE_SET_NAME),
                    Objects.requireNonNull(SourceSet.TEST_SOURCE_SET_NAME)));

    /**
     * Model rules of the plugin
     */
    public static class PluginRules extends RuleSource {

        /**
         * Register wsimport component
         *
         * @param builder Type builder for the component
         */
        @ComponentType
        public void registerComponent(TypeBuilder<WsimportComponent> builder) {
        }

        /**
         * Register WSDL source set
         *
         * @param builder Builder for the source set
         */
        @ComponentType
        public void registerSourceSet(TypeBuilder<WsdlSourceSet> builder) {
            builder.defaultImplementation(DefaultWsdlSourceSet.class);
        }

        /**
         * Validate a wsimport component
         *
         * @param component Component to validate
         */
        @Validate
        public void validateWsimportComponent(@Each WsimportComponent component) {
            if (!component.getName().startsWith("wsdl")) {
                throw new GradleException("Wsimport component name must start with 'wsdl'");
            }
        }

        /**
         * Initialize a WSDL source set
         *
         * @param wsdlSource Source set to initialize
         */
        @Defaults
        public void initializeWsdlSourceSet(@Each WsdlSourceSet wsdlSource) {
            String componentName = getStandardComponentName(Objects.requireNonNull(wsdlSource.getParentName()));
            File directory = getSourceDirectory(componentName, Objects.requireNonNull(wsdlSource.getName()));

            wsdlSource.getSource().srcDir(directory);
            wsdlSource.getSource().include("**/*.wsdl");

            wsdlSource.getBindings().srcDir(directory);
            wsdlSource.getBindings().include("**/*.xjb");
        }

        /**
         * Finalize the wsimport components
         *
         * @param wsimportComponents Wsimport components to finalize
         * @param files File operations
         */
        @Finalize
        public void createWsdlSourceSets(ModelMap<WsimportComponent> wsimportComponents, FileOperations files) {
            for (String componentName : DEFAULT_SOURCE_SET_NAMES) {
                File sourceDirectory = getSourceDirectory(componentName, "wsdl");
                String wsdlComponentName = getWsdlComponentName(componentName);

                wsimportComponents.create(wsdlComponentName, c -> {
                    if (files.file(sourceDirectory).isDirectory()) {
                        c.getSources().create("wsdl", WsdlSourceSet.class, s -> {
                            s.getSource().setSrcDirs(Collections.singleton(sourceDirectory));
                        });
                    }
                });
            }
        }

        /**
         * Create the wsimport tasks for each component
         *
         * @param tasks Task container to create tasks
         * @param wsimportComponents Wsimport components to create tasls for
         * @param files File operations
         * @param buildDir Build directory
         */
        @Mutate
        public void createWsimportTasks(ModelMap<Task> tasks, ModelMap<WsimportComponent> wsimportComponents,
                FileOperations files, @Path("buildDir") File buildDir) {
            Multimap<String, String> taskNames = Multimaps.newSetMultimap(new HashMap<>(), () -> new HashSet<>());

            for (WsimportComponent wsimport : wsimportComponents) {
                String componentName = getStandardComponentName(Objects.requireNonNull(wsimport.getName()));

                for (LanguageSourceSet source : wsimport.getSources()) {
                    WsdlSourceSet wsdlSource = (WsdlSourceSet) source;
                    String taskName = getWsimportTaskName(componentName, Objects.requireNonNull(wsdlSource.getName()));

                    tasks.create(taskName, WsimportTask.class, t -> {
                        t.setDescription(String.format("Run wsimport on %s", wsdlSource));
                        t.setGroup("generated");
                        t.setDestinationDir(getGeneratedSourcesDirectory(buildDir, componentName,
                                Objects.requireNonNull(wsdlSource.getName())));
                        t.setWsdls(Objects.requireNonNull(wsdlSource.getSource()).getAsFileTree());
                        t.setBindings(wsdlSource.getBindings().getAsFileTree());
                        t.setXjcExtensions(wsdlSource.getXjc().getExtensions());
                    });

                    taskNames.put(wsimport.getName(), taskName);
                }
            }

            Map<String, Collection<String>> taskDependencies = taskNames.asMap();

            for (Entry<String, Collection<String>> taskEntry : taskDependencies.entrySet()) {
                String componentName = getStandardComponentName(Objects.requireNonNull(taskEntry.getKey()));
                String taskName = getWsimportTaskName(componentName, "");

                tasks.create(taskName, t -> {
                    t.setDescription(String.format("Run wsimport on component %s", taskEntry.getKey()));
                    t.setGroup("generated");
                    t.dependsOn(taskEntry.getValue());
                });
            }

            for (String componentName : DEFAULT_SOURCE_SET_NAMES) {
                String wsdlComponentName = getWsdlComponentName(componentName);
                Collection<String> wsimportTasks = taskDependencies.get(wsdlComponentName);

                if (wsimportTasks != null) {
                    String compileTaskName = getCompileTaskName(componentName, "java");
                    tasks.get(compileTaskName).dependsOn(wsimportTasks);
                }
            }
        }

        /**
         * Configure the 'eclipseClasspath' task
         *
         * @param generateEclipseClasspath Task to configure
         * @param components Wsimport components
         */
        @Mutate
        public void configureGenerateEclipseTask(@Each GenerateEclipseClasspath generateEclipseClasspath,
                ModelMap<WsimportComponent> components) {
            for (WsimportComponent wsimport : components) {
                String componentName = getStandardComponentName(Objects.requireNonNull(wsimport.getName()));

                for (LanguageSourceSet source : wsimport.getSources()) {
                    String wsimportTaskName = getWsimportTaskName(componentName,
                            Objects.requireNonNull(source.getName()));
                    generateEclipseClasspath.dependsOn(wsimportTaskName);
                }
            }
        }

        /**
         * Configure the JVM binaries
         *
         * @param jvmSpec JVM binary to configure
         * @param wsimportComponents Wsimport components
         * @param files File operations
         * @param buildDir Build directory
         */
        @Mutate
        public void configureJvmBinary(@Each JvmBinarySpec jvmSpec, ModelMap<WsimportComponent> wsimportComponents,
                FileOperations files, @Path("buildDir") File buildDir) {
            jvmSpec.getInputs().withType(JavaSourceSet.class, java -> {
                if (java.getName().equals("java")) {
                    WsimportComponent wsimportComponent = wsimportComponents
                            .get(getWsdlComponentName(Objects.requireNonNull(jvmSpec.getName())));

                    if (wsimportComponent != null) {
                        for (LanguageSourceSet wsdlSource : wsimportComponent.getSources().values()) {
                            File generatedSourceDirectory = getGeneratedSourcesDirectory(buildDir,
                                    Objects.requireNonNull(java.getParentName()),
                                    Objects.requireNonNull(wsdlSource.getName()));
                            java.getSource().srcDir(generatedSourceDirectory);
                        }
                    }
                }
            });

            jvmSpec.getInputs().withType(JvmResourceSet.class, resources -> {
                if (resources.getName().equals("resources")) {
                    WsimportComponent wsimportComponent = wsimportComponents
                            .get(getWsdlComponentName(Objects.requireNonNull(jvmSpec.getName())));

                    if (wsimportComponent != null) {
                        String componentName = getStandardComponentName(
                                Objects.requireNonNull(wsimportComponent.getName()));

                        for (LanguageSourceSet wsdlSource : wsimportComponent.getSources().values()) {
                            File sourceDirectory = getSourceDirectory(componentName,
                                    Objects.requireNonNull(wsdlSource.getName()));
                            resources.getSource().srcDir(sourceDirectory);
                        }
                    }
                }
            });
        }

    }

    /**
     * @see org.gradle.api.Plugin#apply(java.lang.Object)
     */
    @Override
    public void apply(Project project) {
        project.getPlugins().apply(ComponentModelBasePlugin.class);
        project.getPlugins().apply(JavaPlugin.class);

        project.getConfigurations().create("jaxws", c -> {
            c.setDescription("The JAX-WS libraries used.");
            c.setVisible(false);
            c.setTransitive(true);
            c.extendsFrom(project.getConfigurations().getByName("compileClasspath"));
        });

        project.getConfigurations().create("xjc", c -> {
            c.setDescription("The plugin libraries used for xjc.");
            c.setVisible(false);
            c.setTransitive(true);
        });

        project.getDependencies().add("jaxws", "com.sun.xml.ws:jaxws-tools:2.2.10");
    }

    /**
     * Get the source directory for a source set
     *
     * @param componentName Component name
     * @param sourceName Source name
     * @return Source directory for source set
     */
    protected static File getSourceDirectory(String componentName, String sourceName) {
        java.nio.file.Path directory = Paths.get("src", DIRECTORY_NAME_CONVERTER.convert(componentName),
                DIRECTORY_NAME_CONVERTER.convert(sourceName));
        return directory.toFile();
    }

    /**
     * Get the target directory for a source set
     *
     * @param buildDir Build directory
     * @param componentName Component name
     * @param sourceName Source nme
     * @return Target directory for source set
     */
    protected static File getGeneratedSourcesDirectory(File buildDir, String componentName, String sourceName) {
        java.nio.file.Path dir = buildDir.toPath().resolve(Paths.get("generated/wsimport",
                DIRECTORY_NAME_CONVERTER.convert(componentName), DIRECTORY_NAME_CONVERTER.convert(sourceName)));
        return dir.toFile();
    }

    /**
     * Get the task name for a wsimport source set
     *
     * @param componentName Component name
     * @param sourceName Source name
     * @return Task name for source set
     */
    protected static String getWsimportTaskName(String componentName, String sourceName) {
        StringBuilder name = new StringBuilder("wsimport");

        if (!componentName.equals(SourceSet.MAIN_SOURCE_SET_NAME)) {
            name.append(TASK_NAME_CONVERTER.convert(componentName));
        }

        name.append(TASK_NAME_CONVERTER.convert(sourceName));

        return name.toString();
    }

    /**
     * Get the compile task name for a Java source set
     *
     * @param componentName Component name
     * @param sourceName Source name
     * @return Compile task name for source set
     */
    protected static String getCompileTaskName(String componentName, String sourceName) {
        StringBuilder name = new StringBuilder("compile");

        if (!componentName.equals(SourceSet.MAIN_SOURCE_SET_NAME)) {
            name.append(TASK_NAME_CONVERTER.convert(componentName));
        }

        name.append(TASK_NAME_CONVERTER.convert(sourceName));

        return name.toString();
    }

    /**
     * Get the WSDL component name for a component
     *
     * @param componentName Component name
     * @return WSDL component name
     */
    protected static String getWsdlComponentName(String componentName) {
        StringBuilder name = new StringBuilder("wsdl");

        name.append(TASK_NAME_CONVERTER.convert(componentName));

        return name.toString();
    }

    /**
     * Get the component name for a WSDL component
     *
     * @param componentName WSDL component name
     * @return Standard component name for WSDL component
     */
    protected static String getStandardComponentName(String componentName) {
        if (componentName.startsWith("wsdl")) {
            componentName = Objects.requireNonNull(COMPONENT_NAME_CONVERTER.convert(componentName.substring(4)));
        }

        return componentName;
    }
}
