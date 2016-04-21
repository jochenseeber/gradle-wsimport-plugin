/*
 * BSD 2-Clause License
 *
 * Copyright (c) 2015, Jochen Seeber
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
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
package me.seeber.gradle.wsimport

import groovy.transform.TypeChecked

import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.junit.rules.TemporaryFolder

import spock.lang.Specification

@TypeChecked
public class WsimportPluginSpec extends Specification {

    @Rule
    TemporaryFolder projectDir

    protected Project createProject(Map options = [:]) {
        if(!(options["wsdl"] == false)) {
            projectDir.newFolder("src", "main", "wsdl")
        }

        if(!(options["testWsdl"] == false)) {
            projectDir.newFolder("src", "test", "wsdl")
        }

        ProjectBuilder builder = ProjectBuilder.builder().withName("test").withProjectDir(projectDir.folder)
        Project project = builder.build()
        project.pluginManager.apply(WsimportPlugin)
        project
    }

    def "wsimport task is added to project if WSDL source directory exists"() {
        when:
        Project project = createProject()
        Task task = project.tasks.findByName("wsimport")

        then:
        task instanceof WsimportTask
    }

    def "wsimport task is not added to project if WSDL source directory is absent"() {
        when:
        Project project = createProject(wsdl: false)
        Task task = project.tasks.findByName("wsimport")

        then:
        task == null
    }

    def "wsimportTest task is added to project if WSDL source directory exists"() {
        when:
        Project project = createProject()
        Task task = project.tasks.findByName("wsimportTest")

        then:
        task instanceof WsimportTask
    }

    def "wsimportTest task is not added to project if WSDL source directory is absent"() {
        when:
        Project project = createProject(testWsdl: false)
        Task task = project.tasks.findByName("wsimportTest")

        then:
        task == null
    }

    def "wsimport configuration is added to project"() {
        when:
        Project project = createProject()
        Task task = project.tasks.findByName("wsimport")

        then:
        project.configurations.findByName("wsimport") != null
    }
}
