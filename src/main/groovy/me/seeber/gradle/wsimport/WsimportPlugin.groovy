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

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.SourceSet

public class WsimportPlugin implements Plugin<Project> {
    void apply(Project project) {
        project.configure(project) {
            apply plugin: 'java'

            configurations.create('wsimport') {
                description = 'The JAX-WS libraries used for the wsimport task'
                extendsFrom configurations.compile
                visible = false
                transitive = true
            }

            dependencies {
                wsimport group: 'com.sun.xml.ws', name: 'jaxws-tools', version: '2.2.10'
            }

            sourceSets.all { SourceSet sourceSet ->
                String taskName = sourceSet.getTaskName('wsimport', '')
                File wsdlDir = file("src/${sourceSet.name}/wsdl")

                if(wsdlDir.directory) {
                    String infix = sourceSet.name == 'main' ? '' : "-${sourceSet.name}"
                    File generatedSourcesDir = new File(project.buildDir, "generated${infix}-src/wsimport")

                    Task wsimportTask = task(taskName, type: WsimportTask) {
                        description = 'Generate JAX-WS code from WSDL'
                        inputDir = wsdlDir
                        outputDir = generatedSourcesDir
                        group = 'generated'
                    }

                    java { srcDir generatedSourcesDir }

                    resources { srcDir wsdlDir }

                    project.tasks[sourceSet.compileJavaTaskName].dependsOn(wsimportTask)
                }
            }
        }
    }
}


