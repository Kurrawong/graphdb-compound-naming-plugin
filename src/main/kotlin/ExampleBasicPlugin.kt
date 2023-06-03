import com.ontotext.trree.sdk.*
import org.eclipse.rdf4j.model.impl.SimpleValueFactory
import java.util.*

class ExampleBasicPlugin : PluginBase(), PatternInterpreter {
    val NOW_PREDICATE = "https://example.com/now"
    var nowPredicateId: Long? = null

    override fun getName(): String {
        return "ExampleBasic"
    }

    private fun logInfo(s: String) {
        logger.info("$name plugin - $s")
    }

    private fun logDebug(s: String) {
        logger.debug("$name plugin - $s")
    }

    override fun initialize(initReason: InitReason, pluginConnection: PluginConnection) {
        // Create an IRI to represent the now predicate.
        val nowPredicate = SimpleValueFactory.getInstance().createIRI(NOW_PREDICATE)
        // Put the predicate in the entity pool using the SYSTEM scope.
        nowPredicateId = pluginConnection.entities.put(nowPredicate, Entities.Scope.SYSTEM)

        logInfo("initialised")
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
        logDebug("start of interpret call")
        return if (predicate == nowPredicateId) {
            val literalId = createDateTimeLiteral(pluginConnection.entities)
            logDebug("created literal with id $literalId")
            StatementIterator.create(subject, predicate, literalId, 0)
        } else {
            null
        }
    }

    private fun createDateTimeLiteral(entities: Entities): Long {
        val literal = SimpleValueFactory.getInstance().createLiteral(Date())
        logDebug("created literal with value $literal")
        return entities.put(literal, Entities.Scope.REQUEST)
    }
}