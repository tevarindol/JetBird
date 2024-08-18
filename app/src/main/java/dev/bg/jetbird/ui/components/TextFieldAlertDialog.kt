package dev.bg.jetbird.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import dev.bg.jetbird.R

@Composable
fun TextFieldAlertDialog(
    open: Boolean,
    title: String,
    label: String,
    enabled: Boolean = true,
    initialValue: String? = "",
    supportingText: String? = null,
    placeholder: String? = null,
    inputType: OutlinedTextFieldInputType,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    onDismissRequest: () -> Unit,
    onEntered: (String?) -> Unit
) {
    var input by remember { mutableStateOf(initialValue ?: "") }

    fun dismiss() {
        if (initialValue != null) {
            input = initialValue
        }
        onDismissRequest()
    }

    if (open) {
        AlertDialog(
            title = {
                Text(title)
            },
            text = {
                Column {
                    TypedOutlinedTextField(
                        value = input,
                        onValueChange = {
                            input = it
                        },
                        label = { Text(label) },
                        placeholder = { if (placeholder != null) { Text(placeholder) } },
                        inputType = inputType,
                        keyboardOptions = keyboardOptions,
                        maxLines = 1,
                        enabled = enabled
                    )
                    if (supportingText != null) {
                        Spacer(Modifier.height(4.dp))
                        Text(supportingText)
                    }
                }
            },
            onDismissRequest = onDismissRequest,
            dismissButton = {
                if (enabled) {
                    OutlinedButton(
                        onClick = {
                            dismiss()
                        }
                    ) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (!enabled) {
                            dismiss()
                            return@Button
                        }
                        if (input.isEmpty()) {
                            onEntered(null)
                            input = ""
                            dismiss()
                        } else {
                            onEntered(input)
                            input = ""
                            dismiss()
                        }
                    }
                ) {
                    if (enabled) {
                        Text(stringResource(R.string.confirm))
                    } else {
                        Text(stringResource(R.string.close))
                    }
                }
            }
        )
    }
}
