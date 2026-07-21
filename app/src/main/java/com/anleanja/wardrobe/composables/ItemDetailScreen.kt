package com.anleanja.wardrobe.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.anleanja.wardrobe.database.entities.WardrobeItem
import com.anleanja.wardrobe.view_models.AddItemEvent
import com.anleanja.wardrobe.view_models.AddOutfitEvent
import com.anleanja.wardrobe.view_models.AddOutfitUiState
import com.anleanja.wardrobe.view_models.AddOutfitViewModel
import com.anleanja.wardrobe.view_models.CalendarEvent
import com.anleanja.wardrobe.view_models.ItemDetailUiState
import com.anleanja.wardrobe.view_models.OutfitDetailEvent
import com.anleanja.wardrobe.view_models.OutfitDetailUiState
import com.anleanja.wardrobe.view_models.WardrobeScreenEvent
import com.kizitonwose.calendar.compose.HorizontalCalendar
import com.kizitonwose.calendar.compose.rememberCalendarState
import com.kizitonwose.calendar.core.DayPosition
import com.kizitonwose.calendar.core.firstDayOfWeekFromLocale
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.format.TextStyle
import java.util.Date
import java.util.Locale


@Composable
fun ItemDetailsContent(
    uiState: ItemDetailUiState,
    onOutfitClick: (Int) -> Unit
) {
    when {
        uiState.isLoading -> ModernLoadingState()

        uiState.errorMessage != null -> ModernErrorState(message = uiState.errorMessage)

        else -> {
            uiState.item?.let { item ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    ModernMediaCard(
                        imageUri = item.imageUri,
                        contentDescription = item.modernTitle(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(0.75f),
                        contentScale = ContentScale.Fit
                    )
                    ModernSectionCard(title = "Details") {
                        val categoryDescription = listOfNotNull(item.category, item.subcategory)
                            .filter { it.isNotBlank() }
                            .joinToString(" / ")
                        ModernDetailRow(label = "Category", value = categoryDescription)
                        ModernDetailRow(label = "Seasons", value = item.seasons)
                        ModernDetailRow(label = "Rating", value = item.rating?.takeIf { it > 0 }?.let { "$it/5" })
                        ModernDetailRow(label = "Price", value = item.price?.let { "$it €" })
                        ModernDetailRow(label = "Times worn", value = item.timesWorn.toString())
                        val daysText = when (uiState.daysSinceLastWear) {
                            null -> "Never worn"
                            0 -> "Today"
                            1 -> "Yesterday"
                            else -> "${uiState.daysSinceLastWear} days ago"
                        }
                        ModernDetailRow(label = "Last worn", value = daysText)
                        val formattedDate = item.purchaseDate?.let {
                            SimpleDateFormat("dd MMMM yyyy", Locale.getDefault()).format(Date(it))
                        } ?: "Not set"
                        ModernDetailRow(label = "Purchase date", value = formattedDate)
                    }
                    if (uiState.outfits.isNotEmpty()) {
                        Column {
                            Text(
                                "Part of Outfits",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Card(shape = RoundedCornerShape(12.dp)) {
                                Column {
                                    uiState.outfits.forEach { outfit ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { onOutfitClick(outfit.id) }
                                                .padding(16.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            AsyncImage(
                                                model = outfit.displayImageUri(),
                                                contentDescription = "Outfit image",
                                                contentScale = ContentScale.Fit,
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                            )
                                            Text(
                                                text = "Outfit ID: ${outfit.id}",
                                                style = MaterialTheme.typography.bodyLarge,
                                                modifier = Modifier.weight(1f)
                                            )
                                            Icon(
                                                imageVector = Icons.Filled.ChevronRight,
                                                contentDescription = "View outfit details",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } ?: ModernEmptyState("No item found", "This item is no longer available.")
        }
    }
}
