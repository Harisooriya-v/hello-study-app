package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.viewmodel.StudyViewModel

@Composable
fun CalculatorScreen(viewModel: StudyViewModel) {
    val expression by viewModel.calcExpression.collectAsState()
    val result by viewModel.calcResult.collectAsState()

    val keysGrid = listOf(
        // Row 1: Trigonometry + Parenthesis
        listOf("sin", "cos", "tan", "(", ")"),
        // Row 2: Logs + powers + backspace
        listOf("log", "ln", "^", "√", "⌫"),
        // Row 3: Math Keypad Div
        listOf("7", "8", "9", "÷", "C"),
        // Row 4: Math Keypad Mult
        listOf("4", "5", "6", "×", "π"),
        // Row 5: Math Keypad Sub
        listOf("1", "2", "3", "-", "e"),
        // Row 6: Decimals + Add + Equals
        listOf("0", "00", ".", "+", "=")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .testTag("calculator_screen_root"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Equation / Result Monitor Frame
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.End
            ) {
                // Formula Text
                Text(
                    text = expression.ifEmpty { "0" },
                    fontSize = 32.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth().testTag("calc_expression_view")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Calculated Answer
                if (result.isNotEmpty()) {
                    Text(
                        text = "= $result",
                        fontSize = 38.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End,
                        modifier = Modifier.fillMaxWidth().testTag("calc_result_view")
                    )
                }
            }
        }

        // Tap Controls board
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            keysGrid.forEach { rowKeys ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowKeys.forEach { key ->
                        val isSci = key in listOf("sin", "cos", "tan", "log", "ln", "^", "√", "(", ")")
                        val isConst = key in listOf("π", "e")
                        val isAction = key in listOf("C", "⌫")
                        val isOp = key in listOf("÷", "×", "-", "+", "=")

                        val buttonBg = when {
                            key == "=" -> MaterialTheme.colorScheme.primary
                            isAction -> MaterialTheme.colorScheme.error.copy(alpha = 0.15f)
                            isOp -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            isSci -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.12f)
                            isConst -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.25f)
                            else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        }

                        val contentColor = when {
                            key == "=" -> MaterialTheme.colorScheme.onPrimary
                            isAction -> MaterialTheme.colorScheme.error
                            isOp -> MaterialTheme.colorScheme.primary
                            isSci -> MaterialTheme.colorScheme.secondary
                            isConst -> MaterialTheme.colorScheme.secondary
                            else -> MaterialTheme.colorScheme.onSurface
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1.2f)
                                .clip(RoundedCornerShape(14.dp))
                                .background(buttonBg)
                                .clickable { viewModel.onCalculatorKeyPressed(key) }
                                .testTag("calc_button_$key"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = key,
                                fontSize = if (key.length > 2) 13.sp else 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = contentColor
                            )
                        }
                    }
                }
            }
        }
    }
}
