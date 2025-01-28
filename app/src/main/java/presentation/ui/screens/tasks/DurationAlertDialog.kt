package com.elena.autoplanner.presentation.ui.screens.tasks


import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.elena.autoplanner.presentation.ui.utils.GeneralAlertDialog
import com.elena.autoplanner.domain.models.DurationPlan

@Composable
fun DurationAlertDialog(
    existing: DurationPlan?, // Si hay un valor inicial, se interpreta
    onDismiss: () -> Unit,
    onReady: (DurationPlan?) -> Unit
) {
    // Estado para determinar si son horas o minutos
    var useHours by remember { mutableStateOf(false) }
    // Cadena de texto para el campo numérico
    var numberText by remember { mutableStateOf("") }

    // Si hay un valor previo (existing), lo interpretamos en el init
    LaunchedEffect(Unit) {
        val mins = existing?.totalMinutes
        if (mins != null) {
            if (mins % 60 == 0) {
                // Exactamente múltiplo de 60 => interpretamos como hours
                useHours = true
                numberText = (mins / 60).toString()
            } else {
                // Caso contrario => interpretamos como minutes
                numberText = mins.toString()
            }
        }
    }

    // Título a mostrar en el diálogo
    val titleContent: @Composable () -> Unit = {
        Text(
            text = "Duration",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
    }

    // Cuerpo: un TextField para introducir el número, y un Dropdown o Toggle para Hours/Minutes
    val bodyContent: @Composable () -> Unit = {
        Row (
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween

        ) {
            // Campo de texto para ingresar el valor de la duración
            OutlinedTextField(
                value = numberText,
                onValueChange = { newValue ->
                    // Forzamos que sean solo dígitos (0-9) o vacío
                    if (newValue.all { it.isDigit() } || newValue.isEmpty()) {
                        numberText = newValue
                    }
                },
                label = { Text("Value") },
                placeholder = { Text("E.g. 100") },
                singleLine = true,
                // Uso de teclado numérico
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(0.5f)
            )

            Spacer(Modifier.height(16.dp))

            // Selector: Hours o Minutes
            var expanded by remember { mutableStateOf(false) }
            Box {
                // Botón principal que muestra la opción actual (Hours o Minutes)
                Button(onClick = { expanded = true },) {
                    Text(if (useHours) "Hours" else "Minutes")
                }

                // Menú desplegable
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Hours") },
                        onClick = {
                            useHours = true
                            expanded = false
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Minutes") },
                        onClick = {
                            useHours = false
                            expanded = false
                        }
                    )
                }
            }
        }
    }

    // Finalmente, usamos el diálogo genérico
    GeneralAlertDialog(
        title = titleContent,
        content = bodyContent,
        onDismiss = onDismiss,
        onConfirm = {
            // Botón "Ready": parseamos el texto => int
            val numberInt = numberText.toIntOrNull() ?: 0
            val totalMinutes = if (useHours) numberInt * 60 else numberInt
            onReady(DurationPlan(totalMinutes))
        },
        onNeutral = {
            // Botón "None"
            onReady(null)
        }
    )
}


