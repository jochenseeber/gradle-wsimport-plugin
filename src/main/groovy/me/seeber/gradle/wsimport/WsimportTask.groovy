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

import javax.inject.Inject

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.incremental.IncrementalTaskInputs
import org.gradle.api.tasks.incremental.InputFileDetails
import org.gradle.process.ExecResult
import org.gradle.process.internal.ExecActionFactory
import org.gradle.process.internal.JavaExecAction

@TypeChecked
public class WsimportTask extends DefaultTask {
    @InputDirectory
    File inputDir

    @OutputDirectory
    File outputDir

    ExecActionFactory actionFactory

    @Inject
    public WsimportTask(ExecActionFactory actionFactory) {
        this.actionFactory = actionFactory
    }

    @TaskAction
    void execute(IncrementalTaskInputs inputs) {
        inputs.outOfDate { InputFileDetails change ->
            if(change.file.name =~ /\.wsdl$/) {
                JavaExecAction action = getActionFactory().newJavaExecAction()

                File relativeSourceFile = new File(inputDir.toURI().relativize(change.file.toURI()).toString())
                String packageName = relativeSourceFile.parent.replace("/", ".")

                Map<String, Object> options = (Map<String, Object>)[
                    "quiet": true,
                    "p": packageName,
                    "wsdllocation": change.file.name,
                    "s": outputDir,
                    "extension": true,
                    "Xnocompile": true
                ]

                List<String> arguments = createArguments(options)
                arguments << relativeSourceFile.toString()

                action.setArgs(arguments)
                action.classpath = project.configurations.getAt("wsimport")
                action.main = "com.sun.tools.ws.WsImport"
                action.workingDir = inputDir

                ExecResult result = action.execute()

                if(result.exitValue != 0) {
                    throw new GradleException("Error running wsimport")
                }
            }
        }

        inputs.removed { InputFileDetails change ->
            File targetFile = new File(outputDir, change.file.path)
            targetFile.delete()
        }
    }

    protected List<String> createArguments(Map<String, Object> argumentValues) {
        List<String> arguments = []

        argumentValues.each { String key, Object value ->
            if(value != null) {
                arguments << "-${key}".toString()

                if(value != true) {
                    arguments << value.toString()
                }
            }
        }

        arguments
    }
}
