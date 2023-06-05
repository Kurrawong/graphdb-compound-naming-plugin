import org.eclipse.rdf4j.model.Literal
import org.eclipse.rdf4j.model.Value
import org.eclipse.rdf4j.model.ValueFactory
import org.eclipse.rdf4j.model.util.Values
import org.eclipse.rdf4j.query.algebra.evaluation.TripleSource
import org.eclipse.rdf4j.query.algebra.evaluation.ValueExprEvaluationException
import org.eclipse.rdf4j.query.algebra.evaluation.function.Function

/**
 * An example custom SPARQL function that detects palindromes
 *
 * @author Jeen Broekstra
 */
class PalindromeFunction : Function {
    /**
     * return the URI 'http://example.org/custom-function/palindrome' as a
     * String
     */
    override fun getURI(): String {
        return NAMESPACE + "palindrome"
    }

    @Deprecated("Deprecated in Java")
    override fun evaluate(valueFactory: ValueFactory?, vararg args: Value?): Value {
        TODO("Not yet implemented")
    }

    override fun evaluate(tripleSource: TripleSource, vararg args: Value): Value {
        // our palindrome function expects only a single argument, so throw an error
        // if there's more than one
        if (args.size != 1) {
            throw ValueExprEvaluationException(
                "palindrome function requires"
                        + "exactly 1 argument, got "
                        + args.size
            )
        }
        val arg = args[0]
        // check if the argument is a literal, if not, we throw an error
        if (arg !is Literal) {
            throw ValueExprEvaluationException(
                "invalid argument (literal expected): $arg"
            )
        }

        // get the actual string value that we want to check for palindrome-ness.
        val label = arg.label
        // we invert our string
        var inverted = ""
        for (i in label.length - 1 downTo 0) {
            inverted += label[i]
        }
        // a string is a palindrome if it is equal to its own inverse
        val palindrome = inverted.equals(label, ignoreCase = true)

        // a function is always expected to return a Value object, so we
        // return our boolean result as a Literal
        return Values.literal(palindrome)
    }

    companion object {
        // define a constant for the namespace of our custom function
        const val NAMESPACE = "http://example.org/custom-function/"
    }
}