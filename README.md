# Compound Naming Plugin for GraphDB

A GraphDB plugin implemented using the [GraphDB Plugin API](https://graphdb.ontotext.com/documentation/10.2/plug-in-api.html).

See the [Compound Naming Model](https://agldwg.github.io/compound-naming-model/model.html) for more information.

## Plugin details

This plugin adds a [SPARQL property function](https://graphdb.ontotext.com/documentation/10.2/sparql-functions-reference.html#sparql-functions-vs-magic-predicates) (also known as magic predicates) to GraphDB.

The single function has the namespace `https://pid.kurrawong.ai/func/` with the local name `getLiteralComponents` and takes two SPARQL variables as arguments to bind the return values to. One caveat with this particular implementation is that the SPARQL variables passed to the function _must_ be `?componentType` and `?componentValue`, in that order.

See the next section below for an example of running the function within a SPARQL query.

## Running prebuilt GraphDB container image with plugin preloaded

Run a prebuilt GraphDB container image with the plugin preloaded.

```bash
docker run --rm -d --name graphdb-compound-naming -p 7200:7200 ghcr.io/kurrawong/graphdb-compound-naming
```

This image comes preloaded with example data.

To load the preloaded data, first create a [new repository](http://localhost:7200/repository).

Navigate to http://localhost:7200/import#server and import `data.ttl`.

Finally, test out the plugin by running a SPARQL query at http://localhost:7200/sparql.

```sparql
PREFIX func: <https://pid.kurrawong.ai/func/>
SELECT *
WHERE {
    BIND(<https://linked.data.gov.au/dataset/qld-addr/addr-obj-1075435> as ?iri)
    ?iri func:getLiteralComponents (?componentType ?componentValue) .
}
```

And the result set will be something like the following.

| iri                                                          | componentType                                                             | componentValue |
|--------------------------------------------------------------|---------------------------------------------------------------------------|----------------|
| https://linked.data.gov.au/dataset/qld-addr/addr-obj-1075435 | https://w3id.org/profile/anz-address/AnzAddressComponentTypes/numberFirst | 72             |
| https://linked.data.gov.au/dataset/qld-addr/addr-obj-1075435 | https://linked.data.gov.au/def/roads/ct/RoadType                          | ST (Y)         |
| https://linked.data.gov.au/dataset/qld-addr/addr-obj-1075435 | https://w3id.org/profile/anz-address/AnzAddressComponentTypes/locality    | SHORNCLIFFE    |
| https://linked.data.gov.au/dataset/qld-addr/addr-obj-1075435 | https://linked.data.gov.au/def/roads/ct/RoadName                          | Yundah         |

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
