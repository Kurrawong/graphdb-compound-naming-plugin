import com.ontotext.trree.sdk.*
import com.ontotext.trree.sdk.impl.QueryRequestImpl
import com.ontotext.trree.sdk.impl.RequestContextImpl
import org.eclipse.rdf4j.model.impl.SimpleValueFactory
import org.eclipse.rdf4j.query.BindingSet
import org.eclipse.rdf4j.query.impl.MapBindingSet
import java.lang.Exception

class CompoundNamingPlugin : PluginBase(), ListPatternInterpreter, Preprocessor, Postprocessor {
    val GET_LITERAL_COMPONENTS_PREDICATE = "https://pid.kurrawong.ai/func/getLiteralComponents"
    val HAS_ADDRESS = "https://w3id.org/profile/anz-address/hasAddress"
    var getLiteralComponentsId: Long? = null
    var hasAddressId: Long? = null
    val bindingSets = mutableListOf<BindingSet>()
    val bindingVarsToAdd = mutableSetOf<String>()
    val postprocessBindingSets = mutableListOf<BindingSet>()

    override fun getName(): String {
        return "CompoundNaming"
    }

    private fun logInfo(s: String) {
        logger.error("$name plugin - $s")
    }

    private fun logDebug(s: String) {
        logger.error("$name plugin - $s")
    }

    override fun initialize(initReason: InitReason?, pluginConnection: PluginConnection) {
        val getLiteraComponentsPredicateIRI =
            SimpleValueFactory.getInstance().createIRI(GET_LITERAL_COMPONENTS_PREDICATE)
        getLiteralComponentsId = pluginConnection.entities.put(getLiteraComponentsPredicateIRI, Entities.Scope.SYSTEM)

        val hasAddressIRI = SimpleValueFactory.getInstance().createIRI(HAS_ADDRESS)
        hasAddressId = pluginConnection.entities.put(hasAddressIRI, Entities.Scope.DEFAULT)

        logInfo("initialized")
    }

    override fun estimate(
        subjectId: Long,
        predicateId: Long,
        objectIds: LongArray,
        contextId: Long,
        pluginConnection: PluginConnection,
        requestContext: RequestContext?
    ): Double {
        return 1.0
    }

    override fun interpret(
        subjectId: Long,
        predicateId: Long,
        objectIds: LongArray,
        contextId: Long,
        pluginConnection: PluginConnection,
        requestContext: RequestContext?
    ): StatementIterator? {
        logDebug("start of interpret call")
        return if (predicateId == getLiteralComponentsId && subjectId.toInt() != 0) {
            if (requestContext == null) {
                throw Exception("requestContext is null")
            }

            logDebug("getLiteralComponents function called")
            logDebug("subject: ${pluginConnection.entities.get(subjectId)}")
            logDebug("predicate: ${pluginConnection.entities.get(predicateId)}")
            for (o in objectIds) {
                logDebug("object: ${pluginConnection.entities.get(o)}")
            }

            val iter = pluginConnection.statements[subjectId, 0, 0]
            val iterList = mutableListOf<LongArray>()
            val bindingNames = (requestContext.request as QueryRequestImpl).tupleExpr.bindingNames.toList()

            if (bindingNames.size < 3) {
                throw Exception("Must have two variables assigned.")
            }

            val iri = "iri"
            val componentType = "componentType"
            val componentValue = "componentValue"

            while (iter.next()) {
                iterList.add(
                    longArrayOf(
                        iter.subject,
                        iter.subject,
                        iter.subject,
                        0
                    )
                )
                val bindingSet = MapBindingSet()
                bindingSet.addBinding(iri, pluginConnection.entities.get(iter.subject))
                bindingSet.addBinding(componentType, pluginConnection.entities.get(iter.predicate))
                bindingSet.addBinding(componentValue, pluginConnection.entities.get(iter.`object`))
                bindingSets.add(bindingSet)
            }

            StatementIterator.create(iterList.toTypedArray())

        } else if (predicateId == getLiteralComponentsId && subjectId.toInt() == 0) {
            logDebug("subjectId is 0, returning empty iterator")
            StatementIterator.EMPTY
        } else {
            logDebug("Not getLiteralComponents predicate, returning null")
            null
        }
    }

    override fun preprocess(request: Request?): RequestContext? {
        logDebug("preprocess call")
        if (request is QueryRequest) {
            val context = RequestContextImpl()
            context.request = request
            return context
        }

        return null
    }

    override fun shouldPostprocess(requestContext: RequestContext?): Boolean {
        logDebug("shouldPostProcess call")
        return bindingSets.isNotEmpty()
    }

    override fun postprocess(bindingSet: BindingSet, requestContext: RequestContext?): BindingSet? {
        logDebug("postprocess call")
        for (selfBindingSet in bindingSets) {
            val cloneSelfBindingSet = MapBindingSet()
            selfBindingSet.map { cloneSelfBindingSet.addBinding(it.name, it.value) }
            for (bindingName in bindingSet.bindingNames) {
                if (bindingName != "iri" && bindingName != "componentType" && bindingName != "componentValue") {
                    bindingVarsToAdd.add(bindingName)
                }
                if (bindingVarsToAdd.contains(bindingName)) {
                    cloneSelfBindingSet.addBinding(bindingName, bindingSet.getValue(bindingName))
                }
            }
            postprocessBindingSets.add(cloneSelfBindingSet)
        }

        return null
    }

    override fun flush(requestContext: RequestContext?): MutableIterator<BindingSet> {
        logDebug("flush call")
//        if (requestContext != null) {
//            bindingSets.add((requestContext.request as QueryRequestImpl).bindings)
//        }
        return postprocessBindingSets.iterator()
    }
}