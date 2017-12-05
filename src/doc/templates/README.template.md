Gradle WSImport Plugin
======================

This is a plugin to run [wsimport](https://jax-ws.java.net/2.2.10/docs/ch04.html#tools-wsimport) on WSDL files to generate the Java code required to access a web service (aka the really comprehensibly named "JAX-WS portable artifacts" ;-).

With this plugin, you can create builds that do not require a network connection to download WSDL files during build time. Instead, you can use a separate download task to update the local WSDL files, so everything required to build is availably locally.

Applying the plugin
-------------------

```gradle
plugins {
    id "me.seeber.wsimport" version "${project.version}"
}
```

Usage
-----

The plugin will automatically process all WSDL files found in `src/main/wsdl`. This works for all source sets, so files in `src/test/wsdl` and any other source set you define will also be processed.

The package name for the generated Java classes will be determined by the subdirectory of the WSDL file, e.g. for `src/main/wsdl/com/company/boringenterpriseservice.wsdl` the plugin will use the package name `com.company`.

Currently there are no configuration options.

### Downloading the WSDLs

You can use the [download plugin](https://github.com/michel-kraemer/gradle-download-task) to download the WSDLs. Here's an example from the demo projects:

```gradle
plugins {
    id "de.undercouch.download" version "1.2"
}

import de.undercouch.gradle.tasks.download.Download

task downloadWsdl(type: Download) {
    description "Download WSDL"
    src "http://www-inf.int-evry.fr/cours/WebServices/TP_BPEL/files/PingPong.wsdl"
    dest "src/main/wsdl/me/seeber/gradle/wsimport/demo/hello/client/PingPong.wsdl"
}
```

Then you can update the local WSDL file with

```bash
gradle downloadWsdl
```

Examples
--------

See the [demo project](https://github.com/jochenseeber/gradle-wsimport-demo) for an example.

License
-------

This plugin is licensed under the [${projectConfig.license.id}](LICENSE.txt) license.
