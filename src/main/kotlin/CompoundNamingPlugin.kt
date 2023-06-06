import com.ontotext.trree.sdk.*
import com.ontotext.trree.sdk.impl.RequestContextImpl
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.model.impl.SimpleValueFactory
import org.eclipse.rdf4j.query.BindingSet
import org.eclipse.rdf4j.query.impl.MapBindingSet
import java.lang.Exception

class CompoundNamingPlugin : PluginBase(), ListPatternInterpreter, Preprocessor, Postprocessor {
    val componentNameObject = "compoundNameObject"
    val componentType = "componentType"
    val componentValue = "componentValue"
    val bindingSets = mutableListOf<BindingSet>()
    val bindingVarsToAdd = mutableSetOf<String>()
    val postprocessBindingSets = mutableListOf<BindingSet>()
    var focusId: Long? = null
    var focusIRI: Value? = null

    var getLiteralComponentsId: Long? = null
    var hasAddressId: Long? = null
    var valueId: Long? = null
    var nameId: Long? = null
    var hasPartId: Long? = null
    var additionalTypeId: Long? = null

    override fun getName(): String {
        return "compound-naming"
    }

    private fun logInfo(s: String) {
        logger.info("$name plugin - $s")
    }

    private fun createIRI(
        iri: String,
        pluginConnection: PluginConnection,
        scope: Entities.Scope = Entities.Scope.DEFAULT
    ): Long {
        val value = SimpleValueFactory.getInstance().createIRI(iri)
        return pluginConnection.entities.put(value, scope)
    }

    override fun initialize(initReason: InitReason?, pluginConnection: PluginConnection) {
        // Create entities here where while there is an active transaction
        getLiteralComponentsId =
            createIRI("https://pid.kurrawong.ai/func/getLiteralComponents", pluginConnection, Entities.Scope.SYSTEM)
        hasAddressId = createIRI("https://w3id.org/profile/anz-address/hasAddress", pluginConnection)
        valueId = createIRI("http://www.w3.org/1999/02/22-rdf-syntax-ns#value", pluginConnection)
        nameId = createIRI("https://schema.org/name", pluginConnection)
        hasPartId = createIRI("https://schema.org/hasPart", pluginConnection)
        additionalTypeId = createIRI("https://schema.org/additionalType", pluginConnection)

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

    override fun preprocess(request: Request?): RequestContext? {
        logInfo("preprocess call")
        if (request is QueryRequest) {
            return RequestContextImpl(request)
        }

        return null
    }

    override fun interpret(
        subjectId: Long,
        predicateId: Long,
        objectIds: LongArray,
        contextId: Long,
        pluginConnection: PluginConnection,
        requestContext: RequestContext?
    ): StatementIterator? {
        logInfo("start of interpret call")
        return if (predicateId == getLiteralComponentsId && subjectId.toInt() != 0) {
            // Check if we actually need the requestContext object
            if (requestContext == null) {
                throw Exception("requestContext is null")
            }

            focusId = subjectId
            focusIRI = pluginConnection.entities.get(focusId!!)
            val componentQueue = mutableListOf<Long>()
            componentQueue.add(subjectId)

            val compoundNaming = CompoundNaming(
                pluginConnection, componentQueue, this,
                getLiteralComponentsId!!, hasAddressId!!, valueId!!, nameId!!, hasPartId!!, additionalTypeId!!
            )

            val iterList = mutableListOf<LongArray>()
            for (pair in compoundNaming.data.iterator()) {
                iterList.add(longArrayOf(subjectId, pair.first.id, pair.second.id, contextId))

                val bindingSet = MapBindingSet()
                bindingSet.addBinding("compoundNameObject", focusIRI)
                bindingSet.addBinding("componentType", pair.first.value)
                bindingSet.addBinding("componentValue", pair.second.value)
                bindingSets.add(bindingSet)
            }

            StatementIterator.create(iterList.toTypedArray())

        } else if (predicateId == getLiteralComponentsId && subjectId.toInt() == 0) {
            logInfo("subjectId is 0, returning empty iterator")
            StatementIterator.EMPTY
        } else {
            logInfo("Not getLiteralComponents predicate, returning null")
            null
        }
    }

    override fun shouldPostprocess(requestContext: RequestContext?): Boolean {
        logInfo("shouldPostProcess call")
        return bindingSets.isNotEmpty() || postprocessBindingSets.isNotEmpty()
    }

    override fun postprocess(bindingSet: BindingSet, requestContext: RequestContext?): BindingSet? {
        logInfo("postprocess call")
        if (bindingSets.isEmpty()) {
            return null
        }

        var newBindingSetsAdded = false
        for (selfBindingSet in bindingSets) {
            val cloneSelfBindingSet = MapBindingSet()
            selfBindingSet.map { cloneSelfBindingSet.addBinding(it.name, it.value) }
            for (bindingName in bindingSet.bindingNames) {
                if (bindingName != componentNameObject && bindingName != componentType && bindingName != componentValue) {
                    bindingVarsToAdd.add(bindingName)
                }
                if (bindingVarsToAdd.contains(bindingName)) {
                    cloneSelfBindingSet.addBinding(bindingName, bindingSet.getValue(bindingName))
                }
            }
            if (!postprocessBindingSets.contains(cloneSelfBindingSet)) {
                postprocessBindingSets.add(cloneSelfBindingSet)
                newBindingSetsAdded = true
            }
        }

        if (newBindingSetsAdded) {
            bindingSets.clear()
        }

        return null
    }

    override fun flush(requestContext: RequestContext?): MutableIterator<BindingSet> {
        logInfo("flush call ${postprocessBindingSets.size} rows")

        // Make a copy
        val currentBindingSets = postprocessBindingSets.toMutableList()

        // Cleanup
        bindingVarsToAdd.clear()
        bindingSets.clear()
        postprocessBindingSets.clear()

        return currentBindingSets.iterator()
    }
}