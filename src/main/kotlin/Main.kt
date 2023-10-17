import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import org.jsoup.Jsoup
import java.io.File
import java.net.URI

data class UserData(
    val id: Int,
    val firstName: String,
    val lastName: String,

    val username: String
) {
    override fun toString(): String {
        return "id: $id, firstName: $firstName, lastname: $lastName"
    }
}

class WebScraper(private val url: String) {
    private val domain = URI(url).host.removePrefix("www.")
    private val userList = mutableListOf<UserData>()
    private val scrapeFile = File("$domain.json")

    suspend fun connect() {
        HttpClient(CIO).use { client ->
            val response: HttpResponse = client.get(url)

            val html = response.bodyAsText()

            val document = Jsoup.parse(html)
            val table = document.select(".table-bordered").first()

            table?.select("tbody tr")?.forEach { row ->
                val cells = row.select("td")

                if (cells.size != 4) {
                    return@forEach
                }

                val id = cells[0].text().toInt()
                val firstName = cells[1].text()
                val lastName = cells[2].text()
                val username = cells[3].text()

                userList.add(UserData(id, firstName, lastName, username))
            }
        }
        writeJson(scrapeFile, userList)
    }

    private fun writeJson(jsonFile: File, userList: MutableList<UserData>) {
        if (!jsonFile.exists()) jsonFile.createNewFile()
        val moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()

        val type = Types.newParameterizedType(MutableList::class.java, UserData::class.java)
        val mutableListJsonAdapter = moshi.adapter<MutableList<UserData>>(type)
        val jsonString = mutableListJsonAdapter.indent(" ").toJson(userList)
        jsonFile.writeText(jsonString)
    }

    fun printResult() {
        for (user in userList) {
            println(user.toString())
        }
    }
}

suspend fun main() {
    val webScraper = WebScraper("https://webscraper.io/test-sites/tables/tables-semantically-correct")
    webScraper.connect()
    webScraper.printResult()
}