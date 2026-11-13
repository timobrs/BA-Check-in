package de.jscoding.bacheckin.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BCIPinDots(
    modifier: Modifier = Modifier,
    totalDots: Int = 5,
    filledDots: Int = 0,
    isError: Boolean = false,
    dotSize: Dp = 16.dp,
    spacing: Dp = 20.dp,
    filledColor: Color = colorScheme.primary,
    emptyColor: Color = colorScheme.primary,
    errorColor: Color = colorScheme.error,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing)
    ) {
        repeat(filledDots) {
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .background(
                        color = if (isError) errorColor else filledColor,
                        shape = RoundedCornerShape(50)
                    )
            )
        }
        repeat(totalDots - filledDots) {
            Box(
                modifier = Modifier
                    .size(dotSize)
                    .border(
                        width = 2.dp,
                        color = if (isError) errorColor else emptyColor,
                        shape = RoundedCornerShape(50)
                    )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun BCIPinInputBox(
    modifier: Modifier = Modifier,
    pin: String,
    onPinChange: (String) -> Unit,
    isLoading: Boolean = false,
    maxLength: Int = 5,
    bottomLeftIcon: ImageVector? = null,
    bottomLeftIconModifier: Modifier = Modifier.size(32.dp),
    bottomLeftIconTint: Color = colorScheme.primary,
    bottomLeftIconAction: (() -> Unit)? = null,
    numberButtonWidth: Dp = 72.dp,
    rowSpacing: Dp = 36.dp,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val rows = listOf(
            listOf("1" to "", "2" to "A B C", "3" to "D E F"),
            listOf("4" to "D H I", "5" to "J K L", "6" to "M N O"),
            listOf("7" to "P Q R S", "8" to "T U V", "9" to "X Y Z"),
            listOf("C" to "", "0" to "", "D" to "")
        )

        rows.forEachIndexed { rowIndex, row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                row.forEach { (number, letters) ->
                    Column(
                        modifier = Modifier
                            .width(numberButtonWidth)
                            .clickable {
                                if (number == "D") {
                                    if (pin.isNotEmpty()) {
                                        onPinChange(pin.dropLast(1))
                                    }
                                } else if (number.isNotEmpty()) {
                                    if (pin.length < maxLength) {
                                        onPinChange(pin + number)
                                    }
                                }
                            },
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        if (number == "D") {
                            if (isLoading) {
                                LoadingIndicator(
                                    modifier = Modifier.size(32.dp),
                                    color = colorScheme.primary,
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = "Backspace",
                                    tint = colorScheme.primary,
                                    modifier = Modifier
                                        .size(32.dp)
                                )
                            }
                        } else if (number == "C") {
                            bottomLeftIcon?.let {
                                IconButton(
                                    onClick = {
                                        bottomLeftIconAction?.invoke()
                                    },
                                ) {
                                    Icon(
                                        imageVector = it,
                                        contentDescription = "Custom Icon",
                                        tint = bottomLeftIconTint,
                                        modifier = bottomLeftIconModifier
                                    )
                                }
                            }
                        } else if (number.isNotEmpty()) {
                            Text(
                                text = number,
                                color = colorScheme.primary,
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Light,
                            )
                        }

                        if (letters.isNotEmpty()) {
                            Text(
                                text = letters,
                                color = colorScheme.onSurfaceVariant
                                    .copy(alpha = 0.6f),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                letterSpacing = 2.sp,
                                modifier = Modifier.height(16.dp)
                            )
                        }
                    }
                }
            }

            if (rowIndex < rows.lastIndex) {
                Spacer(modifier = Modifier.height(rowSpacing))
            }
        }
    }
}

@Preview
@Composable
private fun BCIPinDotsPreview() {
    BCIPinDots(
        totalDots = 5,
        filledDots = 2,
        isError = false
    )
}

@Preview
@Composable
private fun BCIPinInputBoxPreview() {
    var pin by remember { mutableStateOf("12") }

    BCIPinInputBox(
        pin = pin,
        onPinChange = { newPin ->
            pin = newPin
        },
        bottomLeftIcon  = Icons.AutoMirrored.Filled.ArrowBack,
        bottomLeftIconTint = colorScheme.primary
    )
}
