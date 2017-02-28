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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;

import org.eclipse.jdt.annotation.Nullable;
import org.gradle.api.GradleException;
import org.gradle.api.file.EmptyFileVisitor;
import org.gradle.api.file.FileTree;
import org.gradle.api.file.FileVisitDetails;
import org.gradle.api.internal.ConventionTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFiles;
import org.gradle.api.tasks.Internal;
import org.gradle.api.tasks.OutputDirectory;
import org.gradle.api.tasks.TaskAction;
import org.gradle.process.ExecResult;
import org.gradle.process.internal.ExecActionFactory;
import org.gradle.process.internal.JavaExecAction;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;

/**
 * Run wsimport on WSDL files
 */
public class WsimportTask extends ConventionTask {

    /**
     * Joiner used to join package name parts
     */
    protected static final Joiner PACKAGE_JOINER = Objects.requireNonNull(Joiner.on('.'));

    /**
     * Factory used to create executor for wsimport
     */
    @Internal
    private final ExecActionFactory actionFactory;

    /**
     * Destination directory for generated code
     */
    private @Nullable File destinationDir;

    /**
     * WSDLs to compile
     */
    private @Nullable FileTree wsdls;

    /**
     * External binding files
     */
    private @Nullable FileTree bindings;

    /**
     * Binding extensions to use
     */
    @Input
    private List<String> xjcExtensions = Collections.emptyList();

    /**
     * Create a new wsimport task
     *
     * @param actionFactory Factory used to create executor for wsimport
     */
    @Inject
    public WsimportTask(ExecActionFactory actionFactory) {
        this.actionFactory = actionFactory;
    }

    /**
     * Run wsimport
     */
    @TaskAction
    protected void wsimport() {
        Objects.requireNonNull(getWsdls()).visit(new EmptyFileVisitor() {
            /**
             * @see org.gradle.api.file.EmptyFileVisitor#visitFile(org.gradle.api.file.FileVisitDetails)
             */
            @Override
            public void visitFile(FileVisitDetails file) {
                Path relativeWsdlFile = Paths.get(file.getPath());
                Path absoluteWsdlFile = file.getFile().toPath();

                if (relativeWsdlFile.isAbsolute() || !absoluteWsdlFile.isAbsolute()) {
                    throw new IllegalArgumentException(String.format("Illegal file visit details %s", file));
                }

                Path baseDir = Objects.requireNonNull(absoluteWsdlFile.getRoot()).resolve(
                        absoluteWsdlFile.subpath(0, absoluteWsdlFile.getNameCount() - relativeWsdlFile.getNameCount()));

                runWsimport(baseDir, relativeWsdlFile);
            }
        });
    }

    /**
     * Run wsimport on a WSDL file
     *
     * @param baseDir Base directory
     * @param wsdlFile WSDL file to compile
     */
    protected void runWsimport(Path baseDir, Path wsdlFile) {
        JavaExecAction action = getActionFactory().newJavaExecAction();
        String packageName = Optional.ofNullable(wsdlFile.getParent()).map(p -> PACKAGE_JOINER.join(p)).orElse(null);

        Multimap<String, Object> options = Multimaps.newListMultimap(new HashMap<>(), () -> new ArrayList<>());

        if (packageName != null) {
            options.put("p", packageName);
        }

        options.put("wsdllocation", wsdlFile.getFileName().toString());
        options.put("s", getDestinationDir());
        options.put("extension", true);
        options.put("Xnocompile", true);
        options.put("B-classpath", getProject().getConfigurations().getAt("xjc").getAsPath());

        for (String extension : getXjcExtensions()) {
            options.put("B-X" + extension, true);
        }

        if (getProject().getLogger().isDebugEnabled()) {
            options.put("Xdebug", true);
        }
        else {
            options.put("quiet", true);
        }

        for (File bindingFile : Objects.requireNonNull(getBindings()).getFiles()) {
            if (bindingFile.isFile()) {
                options.put("b", bindingFile);
            }
        }

        List<String> arguments = createArguments(options);
        arguments.add(wsdlFile.toString());

        getLogger().debug("Running wsimport with arguments {}", Joiner.on(' ').join(arguments));

        action.setArgs(arguments);
        action.setClasspath(getProject().getConfigurations().getAt("jaxws"));
        action.setMain("com.sun.tools.ws.WsImport");
        action.setWorkingDir(baseDir.toFile());

        ExecResult result = action.execute();

        if (result.getExitValue() != 0) {
            throw new GradleException("Error running wsimport");
        }
    }

    /**
     * Create argument list to run command
     *
     * @param argumentValues Argument values
     * @return Argument list
     */
    protected List<String> createArguments(Multimap<String, Object> argumentValues) {
        List<String> arguments = new ArrayList<>();

        argumentValues.entries().forEach(e -> {
            if (e.getValue() != null) {
                arguments.add("-" + e.getKey());

                if (e.getValue() != Boolean.TRUE) {
                    arguments.add(e.getValue().toString());
                }
            }
        });

        return arguments;
    }

    /**
     * Get the destination directory for generated code
     *
     * @return Destination directory
     */
    @OutputDirectory
    public @Nullable File getDestinationDir() {
        return this.destinationDir;
    }

    /**
     * Set the destination directory for generated code
     *
     * @param directory Destination directory
     */
    public void setDestinationDir(File directory) {
        this.destinationDir = directory;
    }

    /**
     * Get the WSDLs to compile
     *
     * @return WSDLs to compile
     */
    @InputFiles
    public @Nullable FileTree getWsdls() {
        return this.wsdls;
    }

    /**
     * Set the WSDLs to compile
     *
     * @param wsdls WSDLs to compile
     */
    public void setWsdls(FileTree wsdls) {
        this.wsdls = wsdls;
    }

    /**
     * Get the external binding files
     *
     * @return External binding files
     */
    @InputFiles
    public @Nullable FileTree getBindings() {
        return this.bindings;
    }

    /**
     * Set the external binding files
     *
     * @param bindings External binding files
     */
    public void setBindings(FileTree bindings) {
        this.bindings = bindings;
    }

    /**
     * Get the binding extensions to use
     *
     * @return Binding extensions to use
     */
    public List<String> getXjcExtensions() {
        return this.xjcExtensions;
    }

    /**
     * Set the binding extensions to use
     *
     * @param extensions Binding extensions to use
     */
    public void setXjcExtensions(List<String> extensions) {
        this.xjcExtensions = Objects.requireNonNull(ImmutableList.copyOf(extensions));
    }

    /**
     * Get the factory used to create executor for wsimport
     *
     * @return Factory used to create executor for wsimport
     */
    protected ExecActionFactory getActionFactory() {
        return this.actionFactory;
    }

}
