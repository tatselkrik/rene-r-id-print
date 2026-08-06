package com.idphoto.printing.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.idphoto.printing.core.SheetCombination
import com.idphoto.printing.core.SheetLayout

@Composable
fun CombinationScreen(
    selectedCombination: SheetCombination,
    onCombinationSelected: (SheetCombination) -> Unit,
    onBack: () -> Unit,
    onPreview: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasCream)
            .safeDrawingPadding()
            .padding(horizontal = 20.dp, vertical = 18.dp),
    ) {
        Text(
            text = "Choose Combination",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = Ink,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Select one maximum-use combination for this 5 × 7 sheet.",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodyMedium,
            color = Muted,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(16.dp))

        QuantityHeader()
        Spacer(Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(
                items = SheetLayout.combinations,
                key = { it.name },
            ) { combination ->
                CombinationOption(
                    combination = combination,
                    selected = combination == selectedCombination,
                    onClick = { onCombinationSelected(combination) },
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedButton(
                onClick = onBack,
                modifier = Modifier.weight(1f),
            ) {
                Text("Back")
            }
            Button(
                onClick = onPreview,
                modifier = Modifier.weight(1f),
            ) {
                Text("Preview")
            }
        }
    }
}

@Composable
private fun QuantityHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 48.dp, end = 14.dp),
    ) {
        QuantityLabel("2 × 2", Modifier.weight(1f))
        QuantityLabel("Passport", Modifier.weight(1f))
        QuantityLabel("1 × 1", Modifier.weight(1f))
    }
}

@Composable
private fun CombinationOption(
    combination: SheetCombination,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(16.dp)
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = if (selected) 2.dp else 1.dp,
                color = if (selected) Pine else Muted.copy(alpha = 0.22f),
                shape = shape,
            )
            .clickable(onClick = onClick),
        color = if (selected) Mint else WarmWhite,
        shape = shape,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            RadioButton(
                selected = selected,
                onClick = onClick,
            )
            QuantityValue(combination.largeCount, Modifier.weight(1f))
            QuantityValue(combination.passportCount, Modifier.weight(1f))
            QuantityValue(combination.smallCount, Modifier.weight(1f))
        }
    }
}

@Composable
private fun QuantityLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.labelMedium,
        color = Muted,
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun QuantityValue(value: Int, modifier: Modifier = Modifier) {
    Text(
        text = if (value == 0) "—" else value.toString(),
        modifier = modifier,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = Ink,
        textAlign = TextAlign.Center,
    )
}
