/*
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
package me.seeber.gradle.wsimport.test

import groovy.transform.TypeChecked

import java.nio.file.Paths

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.internal.project.DefaultProject
import org.gradle.model.internal.type.ModelTypes
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Before
import org.junit.Rule
import org.junit.rules.TestName

import spock.lang.Specification

@TypeChecked
abstract class BaseSpecification extends Specification {

    /**
     * Name of the test
     */
    @Rule
    public TestName testName = new TestName()

    /**
     * Test project
     */
    public Project project

    @Before
    public void initializeTest() {
        this.project = createProject()
    }

    protected Project createProject() {
        File testDirectory = getTestDirectory()
        Project project = ProjectBuilder.builder().withProjectDir(testDirectory).build()
        project
    }

    protected File getTestDirectory() {
        Paths.get("build", "tmp", "test files", getClass().simpleName, testName.methodName).toFile()
    }

    /**
     * Create the test project
     *
     * @param closure Project configuration
     */
    protected void project(@DelegatesTo(Project) Closure closure) {
        closure.delegate = project
        closure()

        if(project instanceof DefaultProject) {
            project.bindAllModelRules()
            project.modelRegistry.find("tasks", ModelTypes.modelMap(Task))

            project.allprojects {
                project.projectEvaluationBroadcaster.afterEvaluate(project, project.state)
            }
        }
    }

    /**
     * Create a subproject
     *
     * @param parent Parent project
     * @param closure Project configuration
     */
    protected void subproject(Project parent, String name, @DelegatesTo(Project) Closure closure) {
        File projectDir = new File(parent.projectDir, name)
        Project child = ProjectBuilder.builder().withName(name).withProjectDir(projectDir).withParent(parent).build()

        closure.delegate = child
        closure()
    }
}
