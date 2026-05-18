package com.reps.app.feature.more

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.reps.app.R
import com.reps.app.ui.theme.RepsTheme

@Composable
fun MoreScreen(
    onNavigateToProgress: () -> Unit,
    onNavigateToMealPlan: () -> Unit,
    onNavigateToGrocery: () -> Unit,
    onNavigateToSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(top = 24.dp)
    ) {
        Text(
            text = stringResource(R.string.screen_more),
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
        )
        Spacer(Modifier.height(16.dp))
        MoreItem(
            icon = Icons.Filled.BarChart,
            label = stringResource(R.string.nav_progress),
            onClick = onNavigateToProgress
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        MoreItem(
            icon = Icons.Filled.CalendarMonth,
            label = stringResource(R.string.more_meal_plan),
            onClick = onNavigateToMealPlan
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        MoreItem(
            icon = Icons.Filled.ShoppingCart,
            label = stringResource(R.string.more_grocery),
            onClick = onNavigateToGrocery
        )
        HorizontalDivider(color = MaterialTheme.colorScheme.outline)
        MoreItem(
            icon = Icons.Filled.Settings,
            label = stringResource(R.string.more_settings),
            onClick = onNavigateToSettings
        )
    }
}

@Composable
private fun MoreItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 18.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Icon(
            imageVector = Icons.Filled.ChevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0A0A0A)
@Composable
private fun MoreScreenPreview() {
    RepsTheme {
        MoreScreen(
            onNavigateToProgress = {},
            onNavigateToMealPlan = {},
            onNavigateToGrocery = {},
            onNavigateToSettings = {}
        )
    }
}
