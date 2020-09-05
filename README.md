CraftservePVP ![Build](https://github.com/Craftserve/CraftservePVP/workflows/Build/badge.svg) ![Deploy](https://github.com/Craftserve/CraftservePVP/workflows/Deploy/badge.svg)
=============

PVP modifications for [Minecraft Java Edition](https://minecraft.net) made by [Craftserve](https://craftserve.pl).

The project consists of few [Maven](https://maven.apache.org) modules:

* `pvp` - the plugin itself, contains almost all the code
* `pvp-<version>` - NMS adapter for specific version of the server, depends on `pvp` and specific server implementation
* `pvp-plugin` - depends on `pvp` and all `pvp-<version>` module(s), compiles the final JAR

We generally follow the [Oracle/Sun](https://www.oracle.com/java/technologies/cc-java-programming-language.html) code conventions.

Compiling
---

We use [Apache Maven](https://maven.apache.org/) to handle our dependencies. Run `mvn clean install` to compile. Your local Maven repository must contain specific server implementation artifacts. Look POM files in NMS adapters (`pvp-<version>`) for details. Final JAR will be located in the `pvp-plugin` module.

