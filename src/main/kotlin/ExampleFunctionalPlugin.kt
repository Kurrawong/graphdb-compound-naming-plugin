import com.ontotext.trree.sdk.*
import org.eclipse.collections.api.map.primitive.ImmutableLongObjectMap
import org.eclipse.collections.impl.map.mutable.primitive.LongObjectHashMap
import org.eclipse.rdf4j.model.impl.SimpleValueFactory
import java.lang.Exception

/**
 * This plugin defines the predicate http://example.com/getLabel as a multiple-argument functional interface,
 * where the function's arguments are provided as an RDF list in the object and the function output will be bound
 * in the subject. It takes at least three arguments:
 * <pre>
 * ?label <http:></http:>//example.com/getLabel> (?resource ?labelPredicate ?lang1 ...)
</pre> *
 *
 *
 * Where ?resource is the RDF resource to lookup, ?labelPredicate is the predicate whose object will be used,
 * and ?lang1 and the remaining arguments are language tags provided as literals.
 *
 *
 * The plugin will return all labels that match the first language tag that has at least one label, or if none
 * of the language tags match, all labels that are plain literals (i.e. xsd:string literals).
 *
 *
 * The language matching logic is compatible with the SPARQL langMatches() function.
 *
 *
 * It is trivial to add more functional patterns by implementing the [FunctionalPattern] interface and passing
 * the instance to [.registerFunctionalPatterns]
 */
class ExampleFunctionalPlugin : PluginBase(), ListPatternInterpreter {
    private var functionalPatternMap: ImmutableLongObjectMap<FunctionalPattern>? = null

    override fun getName(): String {
        return "ExampleFunctional"
    }

    override fun initialize(reason: InitReason, pluginConnection: PluginConnection) {
        // Register the getLabel functional pattern
        functionalPatternMap = registerFunctionalPatterns(pluginConnection, GetLabelFunctionalPattern())
        logger.info("ExampleFunctional plugin initialized!")
    }

    private fun registerFunctionalPatterns(
        pluginConnection: PluginConnection,
        vararg functionalPatterns: FunctionalPattern
    ): ImmutableLongObjectMap<FunctionalPattern> {
        val map: LongObjectHashMap<FunctionalPattern> = LongObjectHashMap()
        for (functionalPattern in functionalPatterns) {
            val predicateId: Long = pluginConnection.getEntities()
                .put(
                    SimpleValueFactory.getInstance().createIRI(functionalPattern.getIRI()),
                    Entities.Scope.SYSTEM
                )
            map.put(predicateId, functionalPattern)
        }
        return map.toImmutable()
    }

    override fun estimate(
        subject: Long, predicate: Long, objects: LongArray, context: Long,
        pluginConnection: PluginConnection, requestContext: RequestContext?
    ): Double {
        // No need to check the predicate if all of the predicates in this plugin follow the same logic
        for (`object` in objects) {
            if (`object` == Entities.UNBOUND) { // (the constant Entities.UNBOUND is actually zero)
                // Since this plugin receives objects converted from an RDF list it can't bind an object,
                // so it can function only if all members of the RDF list are bound.
                // By returning a very large number we ensure the optimizer won't choose a plan with unbound objects.
                return Double.POSITIVE_INFINITY
            }
        }
        return 1.0
    }

    override fun interpret(
        subject: Long, predicate: Long, objects: LongArray, context: Long,
        pluginConnection: PluginConnection, requestContext: RequestContext?
    ): StatementIterator? {
        if (functionalPatternMap == null) {
            throw Exception("functionPatternMap is null")
        }
        val functionalPattern = functionalPatternMap!!.get(predicate)
        if (functionalPattern != null) {
            functionalPattern.verifyNumberOfArguments(objects.size)
            val debugObjects = objects.map { l -> pluginConnection.entities.get(l) }
            for (`object` in objects) {
                // See note in estimate() method. If we do get evaluated with unbound objects simply return
                // an empty iterator (and besides we must return an iterator to signal we want to handle this pattern)
                if (`object` == 0L) {
                    return StatementIterator.EMPTY
                }
            }
            return functionalPattern.evaluate(objects, pluginConnection)
        }

        // Not interested in handling this triple pattern
        return null
    }
}