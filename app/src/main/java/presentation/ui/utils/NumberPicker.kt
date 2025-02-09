package com.elena.autoplanner.presentation.ui.utils

import android.view.ContextThemeWrapper
import android.widget.NumberPicker
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.elena.autoplanner.R

@Composable
fun NumberPicker(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->

            val themedContext = ContextThemeWrapper(context, R.style.MyNumberPickerTheme)

          NumberPicker(themedContext).apply {
                minValue = range.first
                maxValue = range.last
                this.value = value
                setOnValueChangedListener { _, _, newVal ->
                    onValueChange(newVal)
                }
            }
        },
        update = { view ->
            view.value = value
        }
    )
}

