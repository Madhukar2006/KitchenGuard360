package com.kitchenguard360.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kitchenguard360.app.data.model.SafetyLevel
import com.kitchenguard360.app.ui.theme.DangerRed
import com.kitchenguard360.app.ui.theme.DangerRedContainer
import com.kitchenguard360.app.ui.theme.OnDangerRedContainer
import com.kitchenguard360.app.ui.theme.OnSafeGreenContainer
import com.kitchenguard360.app.ui.theme.OnWarningAmberContainer
import com.kitchenguard360.app.ui.theme.SafeGreen
import com.kitchenguard360.app.ui.theme.SafeGreenContainer
import com.kitchenguard360.app.ui.theme.WarningAmber
import com.kitchenguard360.app.ui.theme.WarningAmberContainer

private data class BadgeConfig(
    val bgColor: Color,
    val textColor: Color,
    val icon: ImageVector,
    val iconColor: Color
)

@Composable
fun StatusBadge(
    level: SafetyLevel,
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    val config = when (level) {
        SafetyLevel.SAFE -> BadgeConfig(
            SafeGreenContainer,
            OnSafeGreenContainer,
            Icons.Default.CheckCircle,
            SafeGreen
        )
        SafetyLevel.WARNING -> BadgeConfig(
            WarningAmberContainer,
            OnWarningAmberContainer,
            Icons.Default.Warning,
            WarningAmber
        )
        SafetyLevel.DANGER -> BadgeConfig(
            DangerRedContainer,
            OnDangerRedContainer,
            Icons.Default.Error,
            DangerRed
        )
        SafetyLevel.SENSOR_FAULT -> BadgeConfig(
            WarningAmberContainer,
            OnWarningAmberContainer,
            Icons.Default.Warning,
            WarningAmber
        )
        SafetyLevel.OFFLINE -> BadgeConfig(
            DangerRedContainer,
            OnDangerRedContainer,
            Icons.Default.WifiOff,
            DangerRed
        )
    }

    val verticalPad = if (compact) 4.dp else 6.dp
    val horizPad = if (compact) 8.dp else 12.dp
    val fontSize = if (compact) 12.sp else 14.sp
    val iconSize = if (compact) 14.dp else 18.dp

    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(config.bgColor)
            .padding(horizontal = horizPad, vertical = verticalPad),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = config.icon,
            contentDescription = level.name,
            tint = config.iconColor,
            modifier = Modifier.size(iconSize)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = level.label,
            color = config.textColor,
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp
        )
    }
}
