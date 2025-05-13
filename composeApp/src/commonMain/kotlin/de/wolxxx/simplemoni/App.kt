package de.wolxxx.simplemoni

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import org.jetbrains.compose.ui.tooling.preview.Preview

@Composable
@Preview
fun App() {
    MaterialTheme {
        var foo = remember {
            mutableStateOf(false)
        }
        var bar = remember {
            mutableStateOf(0)
        }

        LaunchedEffect(Unit) {
            while (true) {
                bar.value = bar.value + 1
                println("bar: " + bar.value)
                delay(1000)
            }
        }

        Column {
            Text(
                modifier = Modifier.clickable {
                    foo.value = !foo.value
                },
                text = if (true == foo.value) "hello" else "world"
            )

            Text(text = "foo: " + bar.value)
            Button(
                onClick = {
                    bar.value = 0
                }
            ) {
                Text("fds")
            }
        }
    }
}