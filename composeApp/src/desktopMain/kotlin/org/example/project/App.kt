package org.example.project

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.AlertDialog
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.isSuccess
import kotlinx.coroutines.Dispatchers
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
    var checking: Boolean = false,
    var host: String,
    var lastCheck: Date? = null,
    var errorSince: Date? = null,
    var lastDuration: Long? = null,
    var durations: MutableList<Long> = mutableListOf(),
    var content: String? = null,

    ) {
    fun toSerializable(): ItemSerializable {
        return ItemSerializable(
            name = name,
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
    var weight: Int = 1000,
    var requiredStatusCode: Int? = null,
    var description: String? = null,
    var host: String,
) {
    fun toItem(): Item {
        return Item(
            name = name,
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
    val now = mutableStateOf(Date())
    var items = mutableListOf<Item>()
    var showInitialHint = mutableStateOf(false)
    var pathToConfigFile = ""

    LaunchedEffect(Unit) {
        Dispatchers.IO.let {
            launch {
                var homeDirectory = System.getProperty("user.home")
                val pathToConfigDirectory = homeDirectory + "/.simplemoni"
                pathToConfigFile = "$pathToConfigDirectory/config.json"
                createDirectory(pathToConfigDirectory)

                if (false == fileExists(pathToConfigFile)) {
                    items.add(
                        Item(
                            name = "example item",
                            host = "https://google.com",
                            weight = 100,
                        )
                    )

                    storeItems(items, pathToConfigFile)
                    showInitialHint.value = true
                }

                items = loadItems(pathToConfigFile)
            }
        }
        while (true) {
            now.value = Date()
            delay(300)
            items.forEach { item ->
                if (item.checking) {
                    return@forEach
                }
                item.lastCheck?.let {
                    if (true == item.ok) {
                        if (Date().time - it.time < 20000) {
                            return@forEach
                        }
                    }
                    if (false == item.ok) {
                        if (Date().time - it.time < 3000) {
                            return@forEach
                        }
                    }

                }
                item.checking = true
                Dispatchers.IO.let {
                    launch {
                        var duration = measureTimeMillis {


                            val client = HttpClient(CIO) {
                                install(HttpTimeout) {
                                    requestTimeoutMillis = 2000  // two seconds
                                }
                            }
                            try {
                                val response: HttpResponse = client.get(item.host)
                                item.content = response.bodyAsText()
                                if (null == item.requiredStatusCode) {
                                    item.ok = response.status.isSuccess()
                                }
                                if (null != item.requiredStatusCode) {
                                    item.ok = response.status.value == item.requiredStatusCode
                                }
                                item.errorSince = null
                                if (false == item.ok) {
                                    item.messages.add(response.status.toString() + " " + response.status.description)
                                }
                            } catch (e: Throwable) {
                                if (null == item.errorSince) {
                                    item.errorSince = Date()
                                }
                                item.messages.add(e.stackTraceToString())
                                item.ok = false
                                println(e.stackTraceToString())
                            }
                        }

                        item.lastDuration = duration
                        item.durations.add(duration)
                        item.lastCheck = Date()
                        item.checking = false
                    }
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
                        }
                    }
                ) {
                    Text("refresh")
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
            Column(

            ) {
                Text(now.value.toString())
                FlowRow(
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {

                    items.sortedWith(compareBy({ it.ok }, { it.weight }, { it.name })).forEach {
                        var borderWidth = 1.dp
                        var borderColor = Color.LightGray
                        var backgroundColor = Color.White
                        it.lastCheck?.let { lastCheck ->
                            if (false == it.ok) {
                                borderWidth = 10.dp
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
                                .background(backgroundColor)
                                .border(borderWidth, borderColor)
                        ) {
                            Text(
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                text = it.name
                            )
                            Text(it.host)
                            if (null == it.requiredStatusCode) {
                                Text("required status code: 2xx")
                            }
                            it.requiredStatusCode?.let {
                                Text("required status code: $it")
                            }
                            it.description?.let {
                                Text(it)
                            }
                            it.lastCheck?.let {
                                Row {
                                    Text("last check: " + it.toString())
                                    val now = Date()
                                    val diff = now.time - it.time
                                    val seconds = diff / 1000
                                    val minutes = seconds / 60
                                    val hours = minutes / 60
                                    val days = hours / 24
                                    Text(" ${days}d ${hours % 24}h ${minutes % 60}m ${seconds % 60}s")
                                }
                            }
                            it.errorSince?.let {
                                Row {
                                    Text("error since: " + it.toString())
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
                                    Text(it.lastDuration?.let { it.toString() + "ms" } ?: "-")
                                    Text(
                                        it.durations.average().let { "%.2f".format(it) + "ms avg" })
                                    Text(it.durations.min().let { it.toString() + "ms min" })
                                    Text(it.durations.max().let { it.toString() + "ms max" })
                                }
                            }
                            /*  val platform = EnumPlatform.getCurrentPlatform()
                              val os = platform.os

  //Create a new CefAppBuilder instance
                              var builder = CefAppBuilder()

  //Configure the builder instance
                              builder.setInstallDir(File("jcef-bundle")); //Default
                              builder.setProgressHandler(ConsoleProgressHandler()); //Default
                              builder.addJcefArgs("--disable-gpu"); //Just an example
                              builder.getCefSettings().windowless_rendering_enabled = true; //Default - select OSR mode

  //Set an app handler. Do not use CefApp.addAppHandler(...), it will break your code on MacOSX!
                              //builder.setAppHandler(MavenCefAppHandlerAdapter());

  //Build a CefApp instance using the configuration above
                              var app = builder.build();
                              app.*/


                            it.content?.let {


                                Column(
                                    modifier = Modifier.heightIn(max = 300.dp)
                                ) {
                                    Text(it)
                                }
                            }



                            if (0 != it.messages.size) {
                                Column(
                                    modifier = Modifier.heightIn(max = 200.dp)
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
    var list = mutableListOf<Item>()
    readFile(pathToConfigFile)?.let {
        jsonTool.decodeFromString<List<ItemSerializable>>(it).forEach {
            list.add(it.toItem())
        }
    }
    return list
}

fun storeItems(items: MutableList<Item>, pathToConfigFile: String) {
    var list = mutableListOf<ItemSerializable>()
    items.forEach {
        list.add(it.toSerializable())
    }
    val content = jsonTool.encodeToString(list)
    writeFile(pathToConfigFile, content, false)
}