/*
 * Copyright (c) 2015, Jochen Seeber
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package me.seeber.gradle.wsimport

import java.nio.file.Files

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder

import spock.lang.Specification

public class WsimportPluginSpec extends Specification {
    
    private File projectDir
    
    private File wsdlDir
    
    private ProjectBuilder projectBuilder
    
    private Project project
    
    public void setup() {
        projectDir = Files.createTempDirectory("project").toFile()
        wsdlDir = new File(projectDir, "src/main/wsdl")
        projectBuilder = ProjectBuilder.builder().withName("test").withProjectDir(projectDir)
        project = projectBuilder.build()
    }
    
    public void cleanup() {
        projectDir.deleteDir()
    }
    
    protected void copyProjectFile(String name) {
        File targetFile = new File(projectDir, name)
        targetFile.getParentFile().mkdirs()
        
        getClass().getResourceAsStream("test-files/${name}").withStream { InputStream input ->
            new FileOutputStream(targetFile).withStream { OutputStream output -> output << input }
        }
    }
    
    def "wsimport task is added to project if WSDL source directory exists"() {
        when:
        wsdlDir.mkdirs()
        project.pluginManager.apply WsimportPlugin
        
        then:
        project.tasks.wsimport instanceof WsimportTask
    }
    
    def "wsimport task is not added to project if WSDL source directory is absent"() {
        when:
        project.pluginManager.apply WsimportPlugin
        
        then:
        project.tasks.findByName('wsimport') == null
    }
    
    
    def "wsimportTest task is added to project if WSDL source directory exists"() {
        when:
        new File(projectDir, "src/test/wsdl").mkdirs()
        project.pluginManager.apply WsimportPlugin
        
        then:
        project.tasks.wsimportTest instanceof WsimportTask
    }
    
    def "wsimportTest task is not added to project if WSDL source directory is absent"() {
        when:
        project.pluginManager.apply WsimportPlugin
        
        then:
        project.tasks.findByName('wsimportTest') == null
    }
    
    def "wsimport configuration is added to project"() {
        when:
        project.pluginManager.apply WsimportPlugin
        
        then:
        project.configurations.findByName('wsimport') != null
    }
}
