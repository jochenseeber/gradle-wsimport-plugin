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

package me.seeber.gradle.wsimport.demo.weather;

import static java.lang.String.format;

import java.net.MalformedURLException;
import java.net.URL;

import me.seeber.gradle.wsimport.demo.weather.client.Forecast;
import me.seeber.gradle.wsimport.demo.weather.client.ForecastReturn;
import me.seeber.gradle.wsimport.demo.weather.client.Weather;
import me.seeber.gradle.wsimport.demo.weather.client.WeatherSoap;

public class WeatherMain {

    public static void main(String[] arguments) {
        WeatherMain hello = new WeatherMain();
        hello.run();
    }

    public void run() {
        try {
            Weather weather = new Weather(new URL("http://wsf.cdyne.com/WeatherWS/Weather.asmx?WSDL"));
            WeatherSoap weatherPort = weather.getWeatherSoap12();
            ForecastReturn forecast = weatherPort.getCityForecastByZIP("33060");
            Forecast forecastItem = forecast.getForecastResult().getForecast().get(0);
            System.out.println(format("We're expecting %s in %s, %s", forecastItem.getDesciption(), forecast.getCity(),
                    forecast.getState()));
        }
        catch (MalformedURLException e) {
            System.err.println(e.getMessage());
        }
    }
}
