package de.agrigaia.platform.integration.agrovoc

import de.agrigaia.platform.common.HasLogger
import org.intellij.lang.annotations.Language
import org.springframework.web.reactive.function.client.WebClient

class AgrovocService : HasLogger {
    private val webClient: WebClient = WebClient.create()
    fun fetchAgrovocKeywords(lang: String = "de"): List<String> {
        @Language("SparkSQL") val query: String = """
            PREFIX skosxl: <http://www.w3.org/2008/05/skos-xl#>
            SELECT DISTINCT ?obj
            WHERE
            {
            {
            ?sub skosxl:literalForm ?obj
            Filter(lang(?obj)='${lang}')
            }
            }
        """.trimIndent()

        // TODO: No.
        return listOf("Weichmais", "Zahnmais")
    }
}