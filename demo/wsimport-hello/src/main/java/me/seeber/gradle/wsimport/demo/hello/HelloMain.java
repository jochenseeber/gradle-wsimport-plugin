/**
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

package me.seeber.gradle.wsimport.demo.hello;

import java.net.MalformedURLException;
import java.net.URL;

import me.seeber.gradle.wsimport.demo.hello.client.PingPong;
import me.seeber.gradle.wsimport.demo.hello.client.PingPongService;

public class HelloMain {

    public static void main(String[] arguments) {
        HelloMain hello = new HelloMain();
        hello.run();
    }

    public void run() {
        try {
            PingPongService service = new PingPongService(new URL(
                    "http://www-inf.int-evry.fr/cours/WebServices/TP_BPEL/files/PingPong.wsdl"));
            PingPong pingPong = service.getPingPong();
            String echoInput = pingPong.echoInput("Hello world!");
            System.out.println(echoInput);
        }
        catch (MalformedURLException e) {
            System.err.println(e.getMessage());
        }
    }
}
