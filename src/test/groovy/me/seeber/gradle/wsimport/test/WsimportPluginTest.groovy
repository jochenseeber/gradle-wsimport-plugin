/*
 * BSD 2-Clause License
 *
 * Copyright (c) 2016, Jochen Seeber
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

import org.gradle.api.Task

import me.seeber.gradle.wsimport.WsdlSourceSet
import me.seeber.gradle.wsimport.WsimportPlugin
import me.seeber.gradle.wsimport.WsimportTask

public class WsimportPluginSpec extends BaseSpecification {

    def "plugin_applies"() {
        when:
        project { apply plugin: WsimportPlugin }

        then:
        project.plugins.hasPlugin(WsimportPlugin)
    }

    def "creates_wsimport_task_for_java_source_set"() {
        when:
        new File(project.projectDir, "src/main/wsdl").mkdirs()

        project { apply plugin: WsimportPlugin }

        then:
        Task task = project.tasks.findByName("wsimportWsdl")
        task instanceof WsimportTask
    }

    def "creates_wsimport_task_for_wsdl_source_set"() {
        when:
        new File(project.projectDir, "src/main/custom").mkdirs()

        project {
            apply plugin: WsimportPlugin

            model {
                components {
                    wsdlMain {
                        sources { customWsdl(WsdlSourceSet) }
                    }
                }
            }
        }

        then:
        Task task = project.tasks.findByName("wsimportCustomWsdl")
        task instanceof WsimportTask
    }
}
