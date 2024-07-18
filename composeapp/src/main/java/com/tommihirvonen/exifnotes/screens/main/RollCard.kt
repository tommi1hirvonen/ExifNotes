/*
 * Exif Notes
 * Copyright (C) 2024  Tommi Hirvonen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.tommihirvonen.exifnotes.screens.main

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CameraRoll
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Theaters
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.core.entities.Camera
import com.tommihirvonen.exifnotes.core.entities.FilmStock
import com.tommihirvonen.exifnotes.core.entities.Frame
import com.tommihirvonen.exifnotes.core.entities.Roll
import com.tommihirvonen.exifnotes.core.sortableDateTime
import java.time.LocalDateTime

@Preview(showBackground = true)
@Composable
private fun RollCardPreview() {
    val filmStock = FilmStock(make = "Tommi's Lab", model = "Rainbow 400", iso = 400)
    val camera = Camera(make = "TomCam Factory", model = "Pocket 9000")
    val roll = Roll(
        name = "Placeholder roll",
        date = LocalDateTime.of(2024, 1, 1, 0, 0),
        unloaded = LocalDateTime.of(2024, 2, 1, 0, 0),
        developed = LocalDateTime.of(2024, 3, 1, 0, 0),
        camera = camera,
        filmStock = filmStock,
        note = "Test note ".repeat(10)
    )
    val frame1 = Frame(roll = roll, count = 1)
    val frame2 = Frame(roll = roll, count = 2)
    roll.frames = listOf(frame1, frame2)
    RollCard(
        roll = roll,
        selected = true
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RollCard(
    roll: Roll,
    selected: Boolean = false,
    onClick: () -> Unit = {},
    onLongClick: () -> Unit = {}
) {
    val filmStock = roll.filmStock
    val camera = roll.camera
    val note = roll.note ?: ""
    val developed = roll.developed
    val unloaded = roll.unloaded
    val (date, state) = when {
        developed != null -> developed.sortableDateTime to stringResource(R.string.Developed)
        unloaded != null -> unloaded.sortableDateTime to stringResource(R.string.Unloaded)
        else -> roll.date.sortableDateTime to stringResource(R.string.Loaded)
    }
    val cardColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        label = "cardBackgroundColor",
        animationSpec = tween(durationMillis = 400)
    )
    Box(modifier = Modifier.fillMaxWidth()) {
        val cardShape = RoundedCornerShape(12.dp)
        Card(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .fillMaxWidth()
                .clip(cardShape)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = onLongClick
                ),
            shape = cardShape,
            colors = CardDefaults.cardColors(containerColor = cardColor)
        ) {
            Box(modifier = Modifier.padding(6.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                    Row {
                        Text(
                            text = roll.name ?: "",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                    if (filmStock != null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                modifier = Modifier.size(14.dp),
                                imageVector = Icons.Filled.CameraRoll,
                                contentDescription = ""
                            )
                            Text(
                                text = filmStock.name,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                    Row {
                        Row(
                            modifier = Modifier.weight(0.7f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                modifier = Modifier.size(14.dp),
                                imageVector = Icons.Filled.CameraAlt,
                                contentDescription = ""
                            )
                            if (camera != null) {
                                Text(camera.name, fontSize = 13.sp)
                            } else {
                                Text(stringResource(R.string.NoCamera), fontSize = 13.sp)
                            }
                        }
                        Row(
                            modifier = Modifier.weight(0.3f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                modifier = Modifier.size(16.dp),
                                imageVector = Icons.Filled.Theaters,
                                contentDescription = ""
                            )
                            Text(
                                pluralStringResource(
                                    R.plurals.PhotosAmount, roll.frames.size, roll.frames.size
                                ),
                                fontSize = 13.sp
                            )
                        }
                    }
                    Row {
                        Row(
                            modifier = Modifier.weight(0.7f),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                modifier = Modifier.size(16.dp),
                                imageVector = Icons.Filled.CalendarToday,
                                contentDescription = ""
                            )
                            Text(date, fontSize = 13.sp)
                        }
                        Box (modifier = Modifier.weight(0.3f)){
                            Text(state, fontSize = 13.sp)
                        }
                    }
                    if (note.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                modifier = Modifier.size(14.dp),
                                imageVector = Icons.AutoMirrored.Filled.Notes,
                                contentDescription = ""
                            )
                            Text(
                                text = note,
                                fontSize = 12.sp,
                                fontStyle = FontStyle.Italic,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
        Box(modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(horizontal = 24.dp)
        ) {
            AnimatedVisibility(
                visible = selected,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Box(modifier = Modifier
                    .size(70.dp)
                    .padding(18.dp)
                    .align(Alignment.Center)
                    .shadow(
                        elevation = 6.dp,
                        shape = CircleShape
                    )
                    .background(
                        color = MaterialTheme.colorScheme.tertiary,
                        shape = CircleShape
                    )
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        Icon(
                            modifier = Modifier
                                .align(Alignment.Center)
                                .size(20.dp),
                            imageVector = Icons.Filled.Check,
                            contentDescription = "",
                            tint = MaterialTheme.colorScheme.onTertiary
                        )
                    }
                }
            }
        }
    }
}