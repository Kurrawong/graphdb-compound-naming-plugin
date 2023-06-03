import com.ontotext.trree.sdk.PluginConnection
import com.ontotext.trree.sdk.StatementIterator
import org.eclipse.rdf4j.model.Literal
import org.eclipse.rdf4j.model.util.Literals
import org.eclipse.rdf4j.model.vocabulary.XSD

/**
 * Implements the http://example.com/getLabel functional pattern.
 */
class GetLabelFunctionalPattern : FunctionalPattern {
    override fun getIRI(): String {
        return "http://example.com/getLabel"
    }

    override fun getMinArguments(): Int {
        return 3
    }

    override fun getMaxArguments(): Int {
        return 0
    }

    override fun evaluate(arguments: LongArray, pluginConnection: PluginConnection): StatementIterator {
        return object : StatementIterator() {
            val subjectId = arguments[0]
            val labelPredicateId = arguments[1]
            var languageIndex = 0
            var language: String? = null
            var lockLanguage = false
            var iter: StatementIterator? = null

            private fun languageMatches(label: Literal): Boolean {
                return if (language!!.isEmpty() && label.datatype == XSD.STRING) {
                    true
                } else label.language.isPresent && Literals.langMatches(label.language.get(), language!!)
            }

            override fun next(): Boolean {
                if (language == null) {
                    language = if (languageIndex + 2 < arguments.size) {
                        // one of the languages in the list
                        pluginConnection.entities[arguments[languageIndex + 2]].stringValue()
                    } else if (languageIndex + 2 <= arguments.size) {
                        // last resort, an xsd:string literal - represented simply as an empty language tag
                        ""
                    } else {
                        // no more languages to try
                        return false
                    }
                    iter?.close()
                    iter = pluginConnection.statements[subjectId, labelPredicateId, 0]
                    languageIndex++
                }
                while (iter!!.next()) {
                    val label = pluginConnection.entities[iter!!.`object`]
                    if (label is Literal) {
                        if (languageMatches(label)) {
                            // Found a matching language, bind the matching label as the subject of this iterator
                            subject = iter!!.`object`
                            // Raise flag to lock language so the iterator will loop through all remaining labels
                            // that have the same language.
                            lockLanguage = true
                            return true
                        }
                    }
                }
                if (!lockLanguage) {
                    // Didn't find any label for the currently requested language, reset and retry with next language
                    language = null
                    return next()
                }
                return false
            }

            override fun close() {
                iter?.close()
            }
        }
    }
}
