# agri-gaia platform backend

This repository contains the backend of the agri-gaia platform application. The backend is a Spring Boot application
written in kotlin.

## Prerequisites

To build and run the application you need to have the following software installed:

* JDK 11
* Maven
* Docker to run a database container or alternatively an installed database (MariaDB recommended).

If you use IntelliJ IDEA, the following plugins are recommended, but not required:

* MapStruct Support (https://plugins.jetbrains.com/plugin/10036-mapstruct-support)
* JPA Buddy (https://plugins.jetbrains.com/plugin/15075-jpa-buddy)

## Database

The easiest way to provide a database is to run a docker container. One way to do this is to use the
provided `env/db/build.sh` script which builds and starts a MariaDB container.

## Build and Test

Since the application uses annotation processing with kapt (https://kotlinlang.org/docs/kapt.html) and kapt ist not yet
supported by IntelliJ IDEA, it is required to run a maven build *at least once* before running the application,
e.g. `mvn clean install`.

> A maven build is also required when you make any changes the MapStruct mappers in the *platform-api* project.

After the maven build, you can build the application normally via the IDE as long as you don't change any mappers or
run `mvn clean`.

## Run

To run the application you need to provide a Spring configuration in `platform-application/src/main/resources`. It is
not recommended editing the provided `application.yml` since it is versioned in git and should only serve as a template.

For local development a file `application-developer.yml` (ignored in .gitignore) can be created in the same folder. The
content of the `application.yml` file can be copied to the new file and customized (remove comments and change values if
necessary).

To use the `application-developer.yml` configuration the application must be started with the Spring profile `developer`
(can be set in the Run/Debug Configurations in IntelliJ IDEA).
