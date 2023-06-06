import com.ontotext.trree.sdk.Entities
import com.ontotext.trree.sdk.PluginBase
import com.ontotext.trree.sdk.PluginConnection
import com.ontotext.trree.sdk.StatementIterator
import com.ontotext.trree.sdk.Statements
import org.eclipse.rdf4j.model.Value

class Statement(statement: LongArray, entities: Entities) {
    val subjectId = statement[0]
    val predicateId = statement[1]
    val objectId = statement[2]
    val contextId = statement[3]
    val subject = entities.get(subjectId)
    val predicate = entities.get(predicateId)
    val `object` = entities.get(objectId)
    val context = entities.get(contextId)
}

data class Entity(val id: Long, val value: Value)

class CompoundNaming(
    pluginConnection: PluginConnection,
    private var componentQueue: List<Long>,
    private val plugin: PluginBase,
    private val getLiteralComponentsId: Long,
    private val hasAddressId: Long,
    private val valueId: Long,
    private val nameId: Long,
    private val hasPartId: Long,
    private val additionalType: Long
) {
    private val SUBJECT = 0
    private val PREDICATE = 1
    private val OBJECT = 2
    private val CONTEXT = 3
    private val entities: Entities = pluginConnection.entities
    private val statements: Statements = pluginConnection.statements
    private val ANY: Long = 0

    val data = mutableSetOf<Pair<Entity, Entity>>()

    init {
        while (componentQueue.isNotEmpty()) {
            val startingNode = this.componentQueue[0]
            componentQueue = this.componentQueue.drop(1)

            val value = getComponentLiteral(startingNode)
            if (value != null) {
                data.add(value)
            }

            if (this.componentQueue.isEmpty()) {
                break
            }
        }
    }

    private fun toList(iter: StatementIterator): List<LongArray> {
        val values = mutableListOf<LongArray>()
        while (iter.next()) {
            values.add(longArrayOf(iter.subject, iter.predicate, iter.`object`, iter.context))
        }
        return values.toList()
    }

    private fun getComponentLiteral(focusNodeId: Long): Pair<Entity, Entity>? {
        val focusNode = entities.get(focusNodeId)
        val result = toList(statements[focusNodeId, valueId, 0])
        var sdoNames = toList(statements[focusNodeId, nameId, ANY]).map { statement -> statement[OBJECT] }
        var hasParts = toList(statements[focusNodeId, hasPartId, 0]).map { statement -> statement[OBJECT] }

        if (hasParts.isNotEmpty()) {
            val hasPartNode = hasParts[0]
            if (hasParts.size > 1) {
                hasParts = hasParts.drop(1)
                this.componentQueue = componentQueue + hasParts
            }
            return getComponentLiteral(hasPartNode)
        }

        if (sdoNames.isNotEmpty()) {
            val sdoNameNode = sdoNames[0]
            if (sdoNames.size > 1) {
                sdoNames = sdoNames.drop(1)
                this.componentQueue = componentQueue + sdoNames
            }
            return getComponentLiteral(sdoNameNode)
        }

        if (result.isEmpty()) {
            plugin.logger.info("Focus node $focusNode with id $focusNodeId does not have any values for sdo:value.")
            return null
        }

        val value = Statement(result[0], entities)

        if (value.`object`.isIRI) {
            return getComponentLiteral(value.objectId)
        }

        val componentTypes = toList(statements[focusNodeId, additionalType, ANY])

        if (componentTypes.isEmpty()) {
            plugin.logger.info("Focus node $focusNode with id $focusNodeId does not have a component type.")
            return null
        }

        val componentType = Statement(componentTypes[0], entities)
        return Entity(componentType.objectId, componentType.`object`) to Entity(value.objectId, value.`object`)
    }
}