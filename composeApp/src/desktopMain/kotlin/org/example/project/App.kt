package org.example.project


import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.timeout
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.jetbrains.compose.ui.tooling.preview.Preview
import java.io.File
import java.util.Date
import kotlin.system.measureTimeMillis


data class Item(
    var name: String,
    var weight: Int = 1000,
    var requiredStatusCode: Int? = null,
    var description: String? = null,
    var messages: MutableList<String> = mutableListOf(),
    var ok: Boolean = false,
    var active: Boolean = true,
    var interval: Int = 20,
    var errorInterval: Int = 5,
    var timeOut: Int = 2,
    var checking: Boolean = false,
    var host: String,
    var lastCheck: Date? = null,
    var errorSince: Date? = null,
    var lastDuration: Long? = null,
    var durations: MutableList<Long> = mutableListOf(),
    var content: String? = null,
    var contentId: String? = null,

    ) {
    fun toSerializable(): ItemSerializable {
        return ItemSerializable(
            name = name,
            active = active,
            interval = interval,
            errorInterval = errorInterval,
            timeOut = timeOut,
            weight = weight,
            requiredStatusCode = requiredStatusCode,
            description = description,
            host = host,
        )
    }
}

@Serializable
data class ItemSerializable(
    var name: String,
    var active: Boolean? = true,
    var interval: Int? = 20,
    var errorInterval: Int? = 5,
    var timeOut: Int? = 2,
    var weight: Int = 1000,
    var requiredStatusCode: Int? = null,
    var description: String? = null,
    var host: String,
) {
    fun toItem(): Item {
        return Item(
            name = name,
            active = active ?: true,
            interval = interval ?: 20,
            errorInterval = errorInterval ?: 5,
            timeOut = timeOut ?: 2,
            weight = weight,
            requiredStatusCode = requiredStatusCode,
            description = description,
            host = host,
        )
    }
}


val jsonTool = Json {
    prettyPrint = true
    encodeDefaults = true
    ignoreUnknownKeys = true
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
@Preview
fun App() {
    val now = mutableStateOf(value = Date())
    var items = mutableListOf<Item>()
    val showInitialHint = mutableStateOf(value = false)
    var homeDirectory = ""
    var pathToConfigFile = ""

    LaunchedEffect(Unit) {
        launch {
            homeDirectory = System.getProperty("user.home")
            val pathToConfigDirectory = "$homeDirectory/.simplemoni"
            pathToConfigFile = "$pathToConfigDirectory/config.json"
            createDirectory(path = pathToConfigDirectory)

            if (false == fileExists(path = pathToConfigFile)) {
                items.add(
                    element = Item(
                        name = "example item",
                        host = "https://google.com",
                        weight = 100,
                    )
                )

                storeItems(items = items, pathToConfigFile = pathToConfigFile)
                showInitialHint.value = true
            }

            items = loadItems(pathToConfigFile = pathToConfigFile)
        }
        val client = HttpClient(engineFactory = CIO) {
            followRedirects = false
            install(plugin = HttpTimeout) {
                requestTimeoutMillis = 2000  // two seconds
            }
        }
        while (true) {
            delay(timeMillis = 500)
            now.value = Date()


            items.forEach { item ->
                if (!item.active) {
                    return@forEach
                }
                if (item.checking) {
                    return@forEach
                }
                item.lastCheck?.let {
                    if (true == item.ok) {
                        if (Date().time - it.time < (item.interval * 1000)) {
                            return@forEach
                        }
                    }
                    if (false == item.ok) {
                        if (Date().time - it.time < (item.errorInterval * 1000)) {
                            return@forEach
                        }
                    }

                }
                item.checking = true
                launch {
                    val duration = measureTimeMillis {
                        try {
                            val response: HttpResponse = client.get(urlString = item.host) {
                                timeout {
                                    requestTimeoutMillis = (item.timeOut * 1000).toLong()
                                }
                            }
                            item.content = response.bodyAsText()
                            if (null == item.requiredStatusCode) {
                                item.ok = response.status.isSuccess()
                            }
                            if (null != item.requiredStatusCode) {
                                item.ok = response.status.value == item.requiredStatusCode
                            }
                            item.errorSince = null
                            if (false == item.ok) {
                                item.messages.add(element = response.status.toString() + " " + response.status.description)
                            }
                        } catch (e: Throwable) {
                            if (null == item.errorSince) {
                                item.errorSince = Date()
                            }
                            item.messages.add(element = e.stackTraceToString())
                            item.ok = false
                            println(message = e.stackTraceToString())
                        }
                    }

                    item.lastDuration = duration
                    item.durations.add(element = duration)
                    item.lastCheck = Date()
                    item.checking = false
                }
            }
        }
    }
    MaterialTheme {
        Scaffold(
            modifier = Modifier.padding(all = 10.dp),
            floatingActionButton = {
                Button(
                    onClick = {
                        items.forEach {
                            it.lastCheck = null
                            it.messages.clear()
                            items = loadItems(pathToConfigFile = pathToConfigFile)
                        }
                    }
                ) {
                    Text(text = "refresh")
                }
            }
        ) {
            if (true == showInitialHint.value) {
                AppAlertDialog(
                    onDismissRequest = {
                        showInitialHint.value = false
                    },
                    onConfirmation = {
                        showInitialHint.value = false
                    },
                    dialogTitle = "created initial config file",
                    dialogText = "you find the config file under $pathToConfigFile",
                )
            }
            Column {
                Text(text = "now: " + now.value.format())
                val itemsList =
                    items.sortedWith(comparator = compareBy({ it.ok }, { it.weight }, { it.name }))
                        .toList()

                LazyVerticalStaggeredGrid(
                    modifier = Modifier.fillMaxWidth(),
                    columns = androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells.Adaptive(
                        minSize = 500.dp
                    ),
                    verticalItemSpacing = 8.dp,
                    horizontalArrangement = Arrangement.spacedBy(space = 8.dp),
                ) {
                    itemsList.forEach {
                        if (!it.active) {
                            return@forEach
                        }
                        item {
                            var borderWidth = 1.dp
                            var borderColor = Color.LightGray
                            var backgroundColor = Color.White
                            it.lastCheck?.let { lastCheck ->
                                if (false == it.ok) {
                                    borderWidth = 3.dp
                                    borderColor = Color.Red
                                    backgroundColor = Color.LightGray
                                }
                                if (true == it.ok) {
                                    borderWidth = 1.dp
                                    borderColor = Color.Green
                                }
                            }
                            if (true == it.checking) {
                                borderWidth = 1.dp
                                borderColor = Color.DarkGray
                            }

                            Column(
                                modifier = Modifier
                                    .background(color = backgroundColor)
                                    .border(width = borderWidth, color = borderColor)
                                    .padding(all = 3.dp)
                            ) {
                                Text(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    text = it.name
                                )
                                if (false == it.ok) {
                                    Text(text = it.host)
                                    if (null == it.requiredStatusCode) {
                                        Text(text = "required status code: 2xx")
                                    }
                                    it.requiredStatusCode?.let {
                                        Text(text = "required status code: $it")
                                    }
                                    it.description?.let {
                                        Text(text = it)
                                    }
                                }

                                it.lastCheck?.let {
                                    Row {
                                        Text(text = "last check: " + it.format())
                                        val now = Date()
                                        val diff = now.time - it.time
                                        val seconds = diff / 1000
                                        val minutes = seconds / 60
                                        Text(text = ", ${minutes % 60}m ${seconds % 60}s ago")
                                    }
                                }
                                it.errorSince?.let {
                                    Row {
                                        Text("error since: " + it.format())
                                        val now = Date()
                                        val diff = now.time - it.time
                                        val seconds = diff / 1000
                                        val minutes = seconds / 60
                                        val hours = minutes / 60
                                        val days = hours / 24
                                        Text(" ${days}d ${hours % 24}h ${minutes % 60}m ${seconds % 60}s")
                                    }
                                }

                                if (0 != it.durations.size) {
                                    Row(
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically,

                                        ) {
                                        Text(it.lastDuration?.let { it.toString() + "ms last" }
                                            ?: "")
                                        Text(
                                            it.durations.average()
                                                .let { "%.2f".format(it) + "ms avg" })
                                        Text(it.durations.min().let { it.toString() + "ms min" })
                                        Text(it.durations.max().let { it.toString() + "ms max" })
                                    }
                                }

                                if (0 != it.messages.size) {
                                    Column(
                                        modifier = Modifier
                                            .heightIn(max = 150.dp)
                                            .verticalScroll(rememberScrollState())


                                    ) {
                                        it.messages.reversed().forEach {
                                            Text(it)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AppAlertDialog(
    onDismissRequest: () -> Unit,
    onConfirmation: () -> Unit,
    dialogTitle: String,
    dialogText: String,
) {
    AlertDialog(
        title = {
            Text(text = dialogTitle)
        },
        text = {
            Text(text = dialogText)
        },
        onDismissRequest = {
            onDismissRequest()
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmation()
                }
            ) {
                Text("kthxbye")
            }
        },
        dismissButton = {

        }
    )
}

fun directoryExists(path: String): Boolean {
    return File(path).isDirectory
}

fun createDirectory(path: String): Boolean {
    if (true == directoryExists(path)) {
        return true
    }
    File(path).mkdir()
    if (false == directoryExists(path)) {
        throw Exception("can not create directory under \"$path\"")
    }
    return true
}

fun createFile(path: String): Boolean {

    val file = File(path)
    if (true == file.exists()) {
        return true
    }
    file.createNewFile()
    if (false == file.exists()) {
        throw Exception("can not create file under \"$path\"")
    }

    return true
}

fun readFile(path: String): String? {
    val file = File(path)
    if (false == file.exists()) {
        throw Exception("file not existing under \"$path\"!")
    }
    return file.readText()
}

fun fileExists(path: String): Boolean {

    val file = File(path)

    return file.exists()
}

fun writeFile(path: String, content: String, append: Boolean): Boolean {
    val exists = fileExists(path)
    if (false == exists) {
        createFile(path)
        val file = File(path)
        file.writeText(content)
        if (content != file.readText()) {
            throw Exception("can not write file under \"$path\"")
        }
        return true
    }

    if (true == append) {
        val file = File(path)
        val before = file.readText()
        file.appendText(content)
        if (before + content != file.readText()) {
            throw Exception("can not append to file under \"$path\"")
        }

        return true
    }

    val file = File(path)
    file.writeText(content)
    if (content != file.readText()) {
        throw Exception("can not write file under \"$path\"")
    }
    return true
}

fun loadItems(pathToConfigFile: String): MutableList<Item> {
    val list = mutableListOf<Item>()
    readFile(pathToConfigFile)?.let {
        try {
            jsonTool.decodeFromString<List<ItemSerializable>>(it).forEach {
                list.add(it.toItem())
            }

        } catch (exception: Throwable) {
            list.clear()
            list.add(
                Item(
                    description = exception.toString(),
                    name = "config file is not ok!",
                    host = "https://1111.comf", weight = 1000
                )
            )
        }
    }
    return list
}

fun storeItems(items: MutableList<Item>, pathToConfigFile: String) {
    val list = mutableListOf<ItemSerializable>()
    items.forEach {
        list.add(it.toSerializable())
    }
    val content = jsonTool.encodeToString(list)
    writeFile(pathToConfigFile, content, false)
}