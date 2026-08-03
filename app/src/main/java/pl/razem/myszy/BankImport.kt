package pl.razem.myszy

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.text.PDFTextStripper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.w3c.dom.Element
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.math.absoluteValue

internal fun InputStream.readAtMost(maxBytes: Int, tooLargeMessage: String): ByteArray {
    val output = ByteArrayOutputStream()
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var total = 0
    while (true) {
        val read = read(buffer)
        if (read < 0) return output.toByteArray()
        total += read
        require(total <= maxBytes) { tooLargeMessage }
        output.write(buffer, 0, read)
    }
}

data class BankTransaction(val id: String, val description: String, val amount: Double, val date: String)

class BankImportStore(context: Context) {
    private val prefs = context.getSharedPreferences("razem_bank_import", Context.MODE_PRIVATE)
    fun start(queue: List<BankTransaction>) { prefs.edit().putInt("total", queue.size).apply(); save(queue) }
    fun total(): Int { if (!prefs.contains("total")) { val value = load().size; prefs.edit().putInt("total", value).apply(); return value }; return prefs.getInt("total", load().size) }
    fun save(queue: List<BankTransaction>) { val a = JSONArray(); queue.forEach { a.put(JSONObject().put("id",it.id).put("description",it.description).put("amount",it.amount).put("date",it.date)) }; prefs.edit().putString("queue",a.toString()).apply() }
    fun load(): List<BankTransaction> = runCatching { val a=JSONArray(prefs.getString("queue","[]")); (0 until a.length()).map { a.getJSONObject(it).let { o -> BankTransaction(o.getString("id"),o.getString("description"),o.getDouble("amount"),o.getString("date")) } } }.getOrDefault(emptyList())
    fun clear() = prefs.edit().remove("queue").remove("total").apply()
    fun clearForLogout() = prefs.edit().remove("queue").remove("total").remove("added").apply()
    fun markAdded(id: String) { prefs.edit().putStringSet("added", prefs.getStringSet("added", emptySet()).orEmpty() + id).apply() }
    fun wasAdded(id: String) = prefs.getStringSet("added", emptySet()).orEmpty().contains(id)
}

object BankStatementParser {
    private const val MAX_IMPORT_BYTES = 10 * 1024 * 1024
    private const val MAX_XLSX_ENTRIES = 200
    private const val MAX_XLSX_ENTRY_BYTES = 4 * 1024 * 1024
    private const val MAX_XLSX_UNCOMPRESSED_BYTES = 20 * 1024 * 1024
    suspend fun parse(context: Context, uri: Uri): List<BankTransaction> = withContext(Dispatchers.IO) {
        val name = context.contentResolver.getType(uri).orEmpty().lowercase()
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readAtMost(MAX_IMPORT_BYTES, "Plik importu jest zbyt duży. Maksymalny rozmiar to 10 MB.") } ?: error("Nie udało się odczytać pliku")
        val lines = if (name.contains("pdf") || bytes.take(4).toByteArray().decodeToString().startsWith("%PDF")) pdfLines(context, bytes) else xlsxLines(bytes)
        parseTransactions(lines)
    }
    private fun pdfLines(context: Context, bytes: ByteArray): List<String> { PDFBoxResourceLoader.init(context.applicationContext); return PDDocument.load(bytes).use { PDFTextStripper().getText(it).lines() } }
    private fun xlsxLines(bytes: ByteArray): List<String> {
        val entries = mutableMapOf<String, ByteArray>()
        var uncompressedBytes = 0
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            while (true) {
                val entry = zip.nextEntry ?: break
                if (entry.isDirectory) continue
                require(entries.size < MAX_XLSX_ENTRIES) { "Plik Excel zawiera zbyt wiele elementów." }
                val entryBytes = zip.readAtMost(MAX_XLSX_ENTRY_BYTES, "Element pliku Excel jest zbyt duży.")
                uncompressedBytes += entryBytes.size
                require(uncompressedBytes <= MAX_XLSX_UNCOMPRESSED_BYTES) { "Plik Excel po rozpakowaniu jest zbyt duży." }
                entries[entry.name] = entryBytes
            }
        }
        if (entries.keys.none { it.endsWith(".xml") }) error("Obsługiwany jest plik Excel .xlsx")
        val shared=entries["xl/sharedStrings.xml"]?.let { xml -> val d=doc(xml); val list=d.getElementsByTagName("si"); (0 until list.length).map { i -> (list.item(i) as Element).getElementsByTagName("t").let { ts -> (0 until ts.length).joinToString("") { ts.item(it).textContent } } } }.orEmpty()
        val sheet=entries.entries.firstOrNull { it.key.startsWith("xl/worksheets/sheet") && it.key.endsWith(".xml") }?.value ?: error("Brak arkusza w pliku")
        val rows=doc(sheet).getElementsByTagName("row")
        return (0 until rows.length).map { i -> val cells=(rows.item(i) as Element).getElementsByTagName("c"); (0 until cells.length).joinToString(" | ") { j -> val c=cells.item(j) as Element; val v=c.getElementsByTagName("v").item(0)?.textContent.orEmpty(); if(c.getAttribute("t")=="s") shared.getOrNull(v.toIntOrNull()?:-1).orEmpty() else v } }
    }
    private fun doc(bytes: ByteArray) = DocumentBuilderFactory.newInstance().apply {
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        setFeature("http://xml.org/sax/features/external-general-entities", false)
        setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        isXIncludeAware = false
        isExpandEntityReferences = false
    }.newDocumentBuilder().parse(ByteArrayInputStream(bytes))
    private fun parseTransactions(lines: List<String>): List<BankTransaction> {
        val starts = Regex("^\\d{2}[./-]\\d{2}[./-]\\d{4}\\s+\\d{2}[./-]\\d{2}[./-]\\d{4}")
        val blocks = mutableListOf<StringBuilder>()
        lines.forEach { raw ->
            val line = raw.trim()
            if (starts.containsMatchIn(line)) blocks += StringBuilder(line)
            else if (blocks.isNotEmpty() && line.isNotBlank()) blocks.last().append("\n").append(line)
        }
        val wanted = Regex("(?i)(Transakcja kart|BLIK)")
        val amount = Regex("-\\s*\\d{1,3}(?:[ .]\\d{3})*[,.]\\d{2}")
        val date = Regex("\\d{2}[./-]\\d{2}[./-]\\d{4}")
        return blocks.mapNotNull { builder ->
            val block = builder.toString()
            if (!wanted.containsMatchIn(block)) return@mapNotNull null
            val rawAmount = amount.find(block)?.value ?: return@mapNotNull null
            val value = rawAmount.replace("-", "").replace(" ", "").replace(".", "").replace(",", ".").toDoubleOrNull() ?: return@mapNotNull null
            val merchant = if (block.contains("Transakcja kart", true))
                Regex("(?im)^Opis transakcji:\\s*(.+)$").find(block)?.groupValues?.getOrNull(1)?.trim()
            else Regex("(?im)^Odbiorca:\\s*(.+)$").find(block)?.groupValues?.getOrNull(1)?.trim()
            val displayMerchant = merchant ?: "Transakcja Alior"
            val d = date.find(block)?.value ?: "Brak daty"
            BankTransaction(block.hashCode().toUInt().toString(16), displayMerchant.take(120), value, d)
        }.distinctBy { it.id }
    }}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BankImportReview(transaction: BankTransaction, position: Int, total: Int, yes: () -> Unit, no: () -> Unit, modify: () -> Unit, stop: () -> Unit, discard: () -> Unit) {
    val progress = if (total > 0) position.toFloat() / total.toFloat() else 0f
    Scaffold(containerColor = MouseCream, topBar = { TopAppBar(colors = TopAppBarDefaults.topAppBarColors(containerColor = MouseCream, titleContentColor = MouseInk), title = { Text("Okruszki z banku", fontWeight = FontWeight.Bold) }, navigationIcon = { TextButton(stop) { Text("Wróć", color = MouseTerracotta) } }) }) { padding ->
        Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.TopCenter) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Okruszek $position z $total", color = MouseTerracotta, fontWeight = FontWeight.Bold)
                LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth(), color = MouseTerracotta, trackColor = MouseTerracottaSoft)
                Surface(color = MouseSurface, shape = RoundedCornerShape(24.dp), border = androidx.compose.foundation.BorderStroke(1.dp, MouseLine), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(22.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Czy to wydatek do naszej norki?", color = MouseInk, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Text("Myszy pokażą Ci każdy znaleziony przelew osobno. Ty decydujesz, co zapisujemy.", color = MouseMuted, fontSize = 14.sp)
                        Text(transaction.description, color = MouseInk, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(transaction.date, color = MouseMuted, fontSize = 13.sp)
                        Text(money(transaction.amount), color = MouseTerracotta, style = MaterialTheme.typography.displaySmall, fontWeight = FontWeight.ExtraBold)
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(no, Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("Pomiń") }
                    OutlinedButton(modify, Modifier.weight(1f), shape = RoundedCornerShape(14.dp)) { Text("Dopasuj") }
                    Button(yes, Modifier.weight(1f), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = MouseTerracotta)) { Text("Dodaj") }
                }
                TextButton(stop, Modifier.fillMaxWidth()) { Text("Zatrzymaj import — wrócę później", color = MouseMuted) }
                TextButton(discard, Modifier.fillMaxWidth()) { Text("Porzuć całkowicie ten import", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold) }
            }
        }
    }
}
