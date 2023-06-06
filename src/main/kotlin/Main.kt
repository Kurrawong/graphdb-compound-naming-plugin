import org.eclipse.rdf4j.model.impl.TreeModel
import org.eclipse.rdf4j.model.vocabulary.RDF
import org.eclipse.rdf4j.repository.config.RepositoryConfig
import org.eclipse.rdf4j.repository.config.RepositoryConfigSchema
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager
import org.eclipse.rdf4j.rio.RDFFormat
import org.eclipse.rdf4j.rio.Rio
import org.eclipse.rdf4j.rio.helpers.StatementCollector
import java.io.File
import java.io.InputStream

fun getFileStream(fileName: String): InputStream {
    return object {}.javaClass.getResourceAsStream(fileName)
        ?: throw Exception("Failed to load $fileName")
}

fun main() {
    val configFileName = "config.ttl"
    val repoId = "graphdb-repo"
    val dataFileName = "data.ttl"

    val repositoryManager = LocalRepositoryManager(File("."))
    repositoryManager.init()

    val graph = TreeModel()

    val configInputStream = getFileStream(configFileName)
    val dataInputStream = getFileStream(dataFileName)

    val rdfParser = Rio.createParser(RDFFormat.TURTLE)
    rdfParser.setRDFHandler(StatementCollector(graph))
    rdfParser.parse(configInputStream, RepositoryConfigSchema.NAMESPACE)
    configInputStream.close()

    val repositoryNode = graph.filter(null, RDF.TYPE, RepositoryConfigSchema.REPOSITORY).first().subject
    val repositoryConfig = RepositoryConfig.create(graph, repositoryNode)
    repositoryManager.addRepositoryConfig(repositoryConfig)

    val repository = repositoryManager.getRepository(repoId)
    val repositoryConnection = repository.connection

    repositoryConnection.add(dataInputStream, RDFFormat.TURTLE)

    // Use the repository
    val query = """
        PREFIX rdfs: <http://www.w3.org/2000/01/rdf-schema#>
        PREFIX func: <https://pid.kurrawong.ai/func/>
        PREFIX ex: <https://example.com/>
        PREFIX cfn: <http://example.org/custom-function/>
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
    """.trimIndent()

    val tupleQuery = repositoryConnection.prepareTupleQuery(query)
    val result = tupleQuery.evaluate()

    println("SPARQL result rows")
    result.iterator().asSequence().toList().forEachIndexed { index, bindingSet ->
        print("$index. ")
        for (bindingName in bindingSet.bindingNames) {
            print("$bindingName=${bindingSet.getValue(bindingName)} ")
        }
        println()
    }

    // Clean up
    repositoryConnection.close()
    repository.shutDown()
    repositoryManager.removeRepository(repoId)
    repositoryManager.shutDown()
}