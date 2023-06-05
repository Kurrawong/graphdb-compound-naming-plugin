# Compound Naming Plugin for GraphDB

A GraphDB plugin implemented using the [GraphDB Plugin API](https://graphdb.ontotext.com/documentation/10.2/plug-in-api.html).

See the [Compound Naming Model](https://agldwg.github.io/compound-naming-model/model.html) for more information.

## Running locally

See [build.gradle.kts](build.gradle.kts) for the JDK version used in this project.

Easiest way to install and manage JDK versions is to use [sdkman](https://sdkman.io/install).

Example:

```bash
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 17.0.6-tem
```

### Build the plugin

```bash
./gradlew clean
./gradlew build
```

The JAR file is now available in [build/libs](build/libs).

### Running GraphDB with docker-compose with the Compound Naming Plugin loaded

Start the container.

```bash
docker-compose --profile graphdb up -d
```

Stop the container.

```bash
docker-compose --profile graphdb down
```
