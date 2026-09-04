package com.ajrpachon.chatapp.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation

@Composable
fun ChatAppTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    leadingIcon: ImageVector? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    isError: Boolean = false,
    supportingText: String? = null,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    // Tracked as TextFieldValue (not just the plain String Material3 overload) so we can
    // control cursor position. Compose's String-based TextField keeps whatever selection the
    // field already had and just swaps the text in (`.copy(text = value)`), so when `value` is
    // replaced from outside — e.g. pre-filling this field to edit an existing message — the
    // cursor is left wherever it happened to be (often position 0, if the field started empty),
    // and typing prepends instead of appending. Detecting an external change and resetting the
    // cursor to the end matches normal EditText/TextField expectations.
    var textFieldValueState by remember {
        mutableStateOf(TextFieldValue(text = value, selection = TextRange(value.length)))
    }
    val textFieldValue = if (textFieldValueState.text == value) {
        textFieldValueState
    } else {
        TextFieldValue(text = value, selection = TextRange(value.length))
    }
    textFieldValueState = textFieldValue

    OutlinedTextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            textFieldValueState = newValue
            if (newValue.text != value) {
                onValueChange(newValue.text)
            }
        },
        modifier = Modifier.fillMaxWidth().then(modifier),
        label = label?.let { { Text(it) } },
        placeholder = placeholder?.let { { Text(it) } },
        leadingIcon = leadingIcon?.let { icon -> { Icon(icon, contentDescription = null) } },
        trailingIcon = trailingIcon,
        isError = isError,
        supportingText = supportingText?.let { msg -> { Text(msg) } },
        singleLine = singleLine,
        maxLines = maxLines,
        enabled = enabled,
        readOnly = readOnly,
        shape = MaterialTheme.shapes.small,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
    )
}

@Composable
fun ChatAppSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = Modifier.fillMaxWidth().then(modifier),
        placeholder = { Text(placeholder) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
        singleLine = true,
        enabled = enabled,
        shape = MaterialTheme.shapes.small,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
    )
}
