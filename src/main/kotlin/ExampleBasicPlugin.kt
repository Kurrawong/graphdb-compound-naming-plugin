import com.ontotext.trree.sdk.*
import org.eclipse.rdf4j.model.impl.SimpleValueFactory
import java.util.*

class ExampleBasicPlugin : PluginBase(), PatternInterpreter {
    val NOW_PREDICATE = "https://example.com/now"
    var nowPredicateId: Long? = null

    override fun getName(): String {
        return "ExampleBasicPlugin"
    }

    override fun initialize(initReason: InitReason, pluginConnection: PluginConnection) {
        // Create an IRI to represent the now predicate.
        val nowPredicate = SimpleValueFactory.getInstance().createIRI(NOW_PREDICATE)
        // Put the predicate in the entity pool using the SYSTEM scope.
        nowPredicateId = pluginConnection.entities.put(nowPredicate, Entities.Scope.SYSTEM)

        logger.info("ExampleBasic plugin initialized!")
    }

    override fun estimate(
        subject: Long,
        predicate: Long,
        `object`: Long,
        context: Long,
        pluginConnection: PluginConnection,
        requestContext: RequestContext?
    ): Double {
        return 1.0
    }

    override fun interpret(
        subject: Long,
        predicate: Long,
        `object`: Long,
        context: Long,
        pluginConnection: PluginConnection,
        requestContext: RequestContext?
    ): StatementIterator? {
        logger.info("ExampleBasic plugin - start of interpret call")
        return if (predicate == nowPredicateId) {
            val literalId = createDateTimeLiteral(pluginConnection.entities)
            logger.info("ExampleBasic plugin - created literal with id $literalId")
            return StatementIterator.create(subject, predicate, literalId, 0)
        } else {
            null
        }
    }

    private fun createDateTimeLiteral(entities: Entities): Long {
        val literal = SimpleValueFactory.getInstance().createLiteral(Date())
        logger.info("ExampleBasic plugin - created literal with value $literal")
        return entities.put(literal, Entities.Scope.REQUEST)
    }
}