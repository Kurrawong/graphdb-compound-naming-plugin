# Compound Naming Plugin for GraphDB

A GraphDB plugin implemented using
the [GraphDB Plugin API](https://graphdb.ontotext.com/documentation/10.2/plug-in-api.html).

See the [Compound Naming Model](https://linked.data.gov.au/def/cn) for more information.

## Plugin details

This plugin was developed and tested with GraphDB version 10.2.1.

This plugin adds
a [SPARQL property function](https://graphdb.ontotext.com/documentation/10.2/sparql-functions-reference.html#sparql-functions-vs-magic-predicates) (
also known as a magic predicate) to GraphDB.

The single function has the namespace `https://linked.data.gov.au/def/cn/func/` with the local
name `getLiteralComponents` and takes two SPARQL variables as arguments to bind the return values to.

### Limitations of the current implementation

#### Hardcoded SPARQL variables

The SPARQL variable of the subject when calling the function _must_ be named `?compoundNameObject` and the SPARQL variables passed to the function _must_ be `?componentType`
and `?componentValue`, in that order.

#### Variables not properly bound

The SPARQL variables used when calling the function are not bound correctly to be used further within the same query context.

For example, these following additions to the same query context don't work.

`?componentType` is not properly bound to be used further.

```sparql
?compoundNameObject func:getLiteralComponents (?componentType ?componentValue) .
?other ?componentType ?t .
```

`?componentValue` is not properly bound to be used further.

```sparql
?compoundNameObject func:getLiteralComponents (?componentType ?componentValue) .
BIND(STRLEN(str(?componentValue)) AS ?length)
```

These limitations can be solved but requires the implementor to have a better understanding of the low-level [GraphDB Plugin API](https://graphdb.ontotext.com/documentation/10.2/plug-in-api.html).

See the next section below for an example of running the function within a SPARQL query.

#### Components must have `rdf:type` and `sdo:additionalType`

The current implementation requires a component type to have an `sdo:additionalType` and an `rdf:type` value. It would make sense to also have a fallback if an `rdf:type` value is not found, for example, `skos:prefLabel`.

Future implementations may consider other predicates and maybe even have a priority order. These same considerations may also be applied to the language tags of the values.

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
PREFIX func: <https://linked.data.gov.au/def/cn/func/>
SELECT *
WHERE {
    VALUES (?compoundNameObject) {
        # Address Compound Name
        (<https://linked.data.gov.au/dataset/qld-addr/addr-1075435>)
        # Road Compound Name - a sub-name of the above Address
        (<https://linked.data.gov.au/dataset/qld-addr/road-QLDRYUN1530828927326621000>)
        # Locality Compound Name - a sub-name of the above Address
        (<https://linked.data.gov.au/dataset/qld-addr/locality-SHORNCLIFFE>)
    }

    ?compoundNameObject func:getLiteralComponents (?componentType ?componentValue) .
}
```

And the result set will be something like the following.

| iri                                                                         | componentType                                                             | componentValue |
| --------------------------------------------------------------------------- | ------------------------------------------------------------------------- | -------------- |
| https://linked.data.gov.au/dataset/qld-addr/addr-obj-1075435                | https://w3id.org/profile/anz-address/AnzAddressComponentTypes/numberFirst | 72             |
| https://linked.data.gov.au/dataset/qld-addr/addr-obj-1075435                | https://linked.data.gov.au/def/roads/ct/RoadType                          | ST (Y)         |
| https://linked.data.gov.au/dataset/qld-addr/addr-obj-1075435                | https://w3id.org/profile/anz-address/AnzAddressComponentTypes/locality    | SHORNCLIFFE    |
| https://linked.data.gov.au/dataset/qld-addr/addr-obj-1075435                | https://linked.data.gov.au/def/roads/ct/RoadName                          | Yundah         |
| https://linked.data.gov.au/dataset/qld-addr/road-QLDRYUN1530828927326621000 | https://linked.data.gov.au/def/roads/ct/RoadType                          | ST (Y)         |
| https://linked.data.gov.au/dataset/qld-addr/road-QLDRYUN1530828927326621000 | https://linked.data.gov.au/def/roads/ct/RoadName                          | Yundah         |
| https://linked.data.gov.au/dataset/qld-addr/locality-SHORNCLIFFE            | https://w3id.org/profile/anz-address/AnzAddressComponentTypes/locality    | SHORNCLIFFE    |

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

## License

[BSD-3-Clause license](LICENSE).
