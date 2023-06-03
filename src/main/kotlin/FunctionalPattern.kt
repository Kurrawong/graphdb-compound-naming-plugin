import com.ontotext.trree.sdk.PluginConnection
import com.ontotext.trree.sdk.PluginException
import com.ontotext.trree.sdk.StatementIterator

/**
 * Interface that allows easy implementation of functional patterns that can be registered in [ExampleFunctionalPlugin].
 */
interface FunctionalPattern {
    /**
     * Returns the IRI to use for calling the functional pattern.
     *
     * @return an IRI as as [String]
     */
    fun getIRI(): String

    /**
     * Returns the minimum number of arguments the functional pattern expects or zero if no minimum.
     *
     * @return a number
     */
    fun getMinArguments(): Int

    /**
     * Returns the maximum number of arguments the functional pattern expects or zero if no maximum.
     *
     * @return a number
     */
    fun getMaxArguments(): Int

    /**
     * Verifies the number of arguments for calling the functional pattern. The default implementation should suffice
     * in most cases.
     *
     * @param number the number of arguments that will be passed to the functional pattern
     */
    fun verifyNumberOfArguments(number: Int) {
        if (getMinArguments() > 0 && number < getMinArguments()) {
            throw PluginException("Too few arguments for iri: got number, expected at least ${getMinArguments()}")
        } else if (getMaxArguments() > 0 && number > getMaxArguments()) {
            throw PluginException("Too many arguments for iri: got number, expected at most ${getMaxArguments()}")
        }
    }

    /**
     * Evaluates the functional pattern with the provided arguments.
     *
     * @param arguments        the arguments as entity IDs
     * @param pluginConnection the plugin connection used to call the functional pattern
     * @return a [StatementIterator] that must bind the output of the functional pattern as the subject
     */
    fun evaluate(arguments: LongArray, pluginConnection: PluginConnection): StatementIterator
}
