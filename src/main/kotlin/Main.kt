import org.eclipse.rdf4j.model.impl.TreeModel
import org.eclipse.rdf4j.model.vocabulary.RDF
import org.eclipse.rdf4j.repository.config.RepositoryConfig
import org.eclipse.rdf4j.repository.config.RepositoryConfigSchema
import org.eclipse.rdf4j.repository.manager.LocalRepositoryManager
import org.eclipse.rdf4j.rio.RDFFormat
import org.eclipse.rdf4j.rio.Rio
import org.eclipse.rdf4j.rio.helpers.StatementCollector
import java.io.File

fun main() {
    val configFileName = "config.ttl"
    val repoId = "graphdb-repo"

    val repositoryManager = LocalRepositoryManager(File("."))
    repositoryManager.init()

    val graph = TreeModel()

    val configInputStream = object {}.javaClass.getResourceAsStream(configFileName)
        ?: throw Exception("Failed to load $configFileName")

    val rdfParser = Rio.createParser(RDFFormat.TURTLE)
    rdfParser.setRDFHandler(StatementCollector(graph))
    rdfParser.parse(configInputStream, RepositoryConfigSchema.NAMESPACE)
    configInputStream.close()

    val repositoryNode = graph.filter(null, RDF.TYPE, RepositoryConfigSchema.REPOSITORY).first().subject
    val repositoryConfig = RepositoryConfig.create(graph, repositoryNode)
    repositoryManager.addRepositoryConfig(repositoryConfig)

    val repository = repositoryManager.getRepository(repoId)

    val repositoryConnection = repository.connection

    // Use the repository
    val query = """
        PREFIX ex: <https://example.com/>
        select *
        where {
            ?s ex:now ?o .
        }
        limit 10
    """.trimIndent()

    val tupleQuery = repositoryConnection.prepareTupleQuery(query)
    val result = tupleQuery.evaluate()

    println("SPARQL result rows")
    for (row in result.iterator()) {
        println(row)
    }

    // Clean up
    repositoryConnection.close()
    repository.shutDown()
    repositoryManager.shutDown()
}