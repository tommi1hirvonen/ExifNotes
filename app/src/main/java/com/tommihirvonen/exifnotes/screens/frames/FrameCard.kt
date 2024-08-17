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

package com.tommihirvonen.exifnotes.screens.frames

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.BrokenImage
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.ShutterSpeed
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
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.core.entities.Frame
import com.tommihirvonen.exifnotes.core.entities.Lens
import com.tommihirvonen.exifnotes.core.sortableDateTime
import java.time.LocalDateTime

@Preview
@Composable
private fun FrameCardSelectedPreview() {
    val lens = Lens(make = "Canon", model = "FD 28mm f/2.8")
    val frame = Frame(
        rollId = 0,
        count = 1,
        date = LocalDateTime.of(2024, 1, 1, 14, 30),
        shutter = "1/250",
        aperture = "2.8",
        lens = lens,
        note = "This is a test note"
    )
    FrameCard(
        frame = frame,
        selected = true,
        onClick = {},
        onLongClick = {}
    )
}

@Preview
@Composable
private fun FrameCardPreview() {
    val lens = Lens(make = "Canon", model = "FD 28mm f/2.8")
    val frame = Frame(
        rollId = 0,
        count = 1,
        date = LocalDateTime.of(2024, 1, 1, 14, 30),
        shutter = "1/250",
        aperture = "2.8",
        lens = lens,
        note = "This is a test note"
    )
    FrameCard(
        frame = frame,
        selected = false,
        onClick = {},
        onLongClick = {}
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FrameCard(
    modifier: Modifier = Modifier,
    frame: Frame,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val cardColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.surfaceVariant else MaterialTheme.colorScheme.surface,
        label = "cardBackgroundColor",
        animationSpec = tween(durationMillis = 400)
    )
    val imageTint by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.surfaceVariant,
        label = "cardBackgroundColor",
        animationSpec = tween(durationMillis = 400)
    )
    Box(modifier = modifier) {
        val cardShape = RoundedCornerShape(12.dp)
        Card(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .fillMaxWidth()
                .clip(cardShape)
                .combinedClickable(
                    onClick = onClick,
                    onLongClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onLongClick()
                    }
                ),
            shape = cardShape,
            colors = CardDefaults.cardColors(containerColor = cardColor)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    Image(
                        modifier = Modifier.size(64.dp),
                        painter = painterResource(R.drawable.background_frame_small),
                        contentDescription = "",
                        colorFilter = ColorFilter.tint(
                            color = imageTint
                        )
                    )
                    Text(
                        text = frame.count.toString(),
                        fontSize = 24.sp
                    )
                }

                val style = MaterialTheme.typography.bodyMedium
                Column {
                    Row(
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        Column(
                            modifier = Modifier.weight(0.7f)
                        ) {
                            Text(
                                text = frame.date.sortableDateTime,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = style
                            )
                            Text(
                                text = frame.lens?.name ?: "",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = style
                            )
                            Text(
                                text = frame.note ?: "",
                                fontStyle = FontStyle.Italic,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                style = style
                            )
                        }
                        Column(
                            modifier = Modifier.weight(0.3f)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    modifier = Modifier.size(14.dp),
                                    imageVector = Icons.Filled.ShutterSpeed,
                                    contentDescription = ""
                                )
                                Text(
                                    text = frame.shutter ?: "",
                                    style = style
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    modifier = Modifier.size(14.dp),
                                    imageVector = Icons.Filled.Camera,
                                    contentDescription = ""
                                )
                                val aperture = frame.aperture?.let { "f/$it" } ?: ""
                                Text(
                                    text = aperture,
                                    style = style
                                )
                            }
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (frame.pictureFilename != null) {
                                    val pictureFileIcon = if (frame.pictureFileExists) {
                                        Icons.Filled.Image
                                    } else {
                                        Icons.Filled.BrokenImage
                                    }
                                    Icon(
                                        modifier = Modifier.size(14.dp),
                                        imageVector = pictureFileIcon,
                                        contentDescription = ""
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Box(modifier = Modifier
            .align(Alignment.TopStart)
            .padding(horizontal = 59.dp)
        ) {
            AnimatedVisibility(
                visible = selected,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                Box(modifier = Modifier
                    .size(64.dp)
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
                                .size(18.dp),
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