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

package com.tommihirvonen.exifnotes.screens.frameedit

import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AddPhotoAlternate
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Clear
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.Fluorescent
import androidx.compose.material.icons.outlined.InsertPhoto
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.QuestionMark
import androidx.compose.material.icons.outlined.Rotate90DegreesCcw
import androidx.compose.material.icons.outlined.Rotate90DegreesCw
import androidx.compose.material.icons.outlined.Tungsten
import androidx.compose.material.icons.outlined.WbCloudy
import androidx.compose.material.icons.outlined.WbShade
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.core.entities.Filter
import com.tommihirvonen.exifnotes.core.entities.Frame
import com.tommihirvonen.exifnotes.core.entities.Lens
import com.tommihirvonen.exifnotes.core.entities.LightSource
import com.tommihirvonen.exifnotes.core.entities.Roll
import com.tommihirvonen.exifnotes.core.toShutterSpeedOrNull
import com.tommihirvonen.exifnotes.screens.DateTimeButtonCombo
import com.tommihirvonen.exifnotes.screens.DropdownButton
import com.tommihirvonen.exifnotes.screens.MultiChoiceDialog
import com.tommihirvonen.exifnotes.util.SnackbarMessage
import com.tommihirvonen.exifnotes.util.copy
import com.tommihirvonen.exifnotes.util.description
import com.tommihirvonen.exifnotes.util.mapNonUniqueToNameWithSerial
import com.tommihirvonen.exifnotes.util.readableCoordinates
import kotlinx.coroutines.launch
import java.io.File
import java.time.LocalDateTime
import kotlin.math.roundToInt

@Composable
fun FrameEditScreen(
    frameId: Long,
    rollId: Long,
    previousFrameId: Long = -1,
    frameCount: Int = -1,
    onNavigateUp: () -> Unit,
    onNavigateToLocationPick: () -> Unit,
    onNavigateToFilterEdit: () -> Unit,
    onNavigateToLensEdit: () -> Unit,
    submitHandler: (Frame) -> Unit
) {
    FrameEditScreen(
        rollId = rollId,
        frameId = frameId,
        previousFrameId = previousFrameId,
        frameCount = frameCount,
        onNavigateUp = onNavigateUp,
        onNavigateToLocationPick = onNavigateToLocationPick,
        onAddFilter = onNavigateToFilterEdit,
        onAddLens = onNavigateToLensEdit,
        onSubmit = { frame ->
            submitHandler(frame)
            onNavigateUp()
        }
    )
}

@Composable
private fun FrameEditScreen(
    rollId: Long,
    frameId: Long,
    previousFrameId: Long,
    frameCount: Int,
    onNavigateUp: () -> Unit,
    onAddFilter: () -> Unit,
    onAddLens: () -> Unit,
    onNavigateToLocationPick: () -> Unit,
    onSubmit: (Frame) -> Unit,
    frameViewModel: FrameViewModel = hiltViewModel { factory: FrameViewModel.Factory ->
        factory.create(rollId, frameId, previousFrameId, frameCount)
    }
) {
    val frame = frameViewModel.frame.collectAsState()
    val lens = frameViewModel.lens.collectAsState()
    val lenses = frameViewModel.lenses.collectAsState()
    val apertureValues = frameViewModel.apertureValues.collectAsState()
    val filters = frameViewModel.filters.collectAsState()
    val isResolvingAddress = frameViewModel.isResolvingFormattedAddress.collectAsState()
    val pictureBitmap = frameViewModel.pictureBitmap.collectAsState()
    val pictureRotation = frameViewModel.pictureRotation.collectAsState()
    val snackbarMessage = frameViewModel.snackbarMessage.collectAsState()
    FrameEditContent(
        roll = frameViewModel.roll,
        frame = frame.value,
        lens = lens.value,
        apertureValues = apertureValues.value,
        shutterValues = frameViewModel.shutterValues,
        lenses = lenses.value,
        filters = filters.value,
        exposureCompValues = frameViewModel.exposureCompValues,
        isResolvingAddress = isResolvingAddress.value,
        pictureBitmap = pictureBitmap.value,
        pictureRotation = pictureRotation.value,
        pictureTempFileProvider = frameViewModel::createNewPictureFile,
        onPictureTempFileCommit = frameViewModel::commitPlaceholderPictureFile,
        onPictureSelected = frameViewModel::setPictureFromUri,
        onAddPictureToGallery = frameViewModel::addPictureToGallery,
        onCountChange = frameViewModel::setCount,
        onDateChange = frameViewModel::setDate,
        onNoteChange = frameViewModel::setNote,
        onApertureChange = frameViewModel::setAperture,
        onShutterChange = frameViewModel::setShutter,
        onLensChange = frameViewModel::setLens,
        onFiltersChange = frameViewModel::setFilters,
        onExposureCompChange = frameViewModel::setExposureComp,
        onNoOfExposuresChange = frameViewModel::setNoOfExposures,
        onFlashChange = frameViewModel::setFlashUsed,
        onLightSourceChange = frameViewModel::setLightSource,
        onFocalLengthChange = frameViewModel::setFocalLength,
        onLocationClick = onNavigateToLocationPick,
        onLocationClear = { frameViewModel.setLocation(null, null) },
        onPictureClear = frameViewModel::clearComplementaryPicture,
        onPictureRotateRight = frameViewModel::rotatePictureRight,
        onPictureRotateLeft = frameViewModel::rotatePictureLeft,
        onNavigateUp = onNavigateUp,
        onAddFilter = onAddFilter,
        onAddLens = onAddLens,
        onSubmit = {
            if (frameViewModel.validate()) {
                onSubmit(frameViewModel.frame.value)
            }
        },
        snackbarMessage = snackbarMessage.value
    )
}

@Preview(widthDp = 800)
@Composable
private fun FrameEditContentPreview() {
    val frame = Frame(rollId = 0, count = 5, date = LocalDateTime.now())
    FrameEditContent(
        roll = Roll(),
        frame = frame,
        lens = null,
        apertureValues = emptyList(),
        shutterValues = emptyList(),
        lenses = emptyList(),
        filters = emptyList(),
        exposureCompValues = emptyList(),
        isResolvingAddress = true,
        pictureBitmap = null,
        pictureRotation = 0f,
        pictureTempFileProvider = { Uri.fromFile(File("")) },
        onPictureTempFileCommit = {},
        onPictureSelected = {},
        onAddPictureToGallery = {},
        onCountChange = {},
        onDateChange = {},
        onNoteChange = {},
        onApertureChange = {},
        onShutterChange = {},
        onLensChange = {},
        onFiltersChange = {},
        onExposureCompChange = {},
        onNoOfExposuresChange = {},
        onFlashChange = {},
        onLightSourceChange = {},
        onFocalLengthChange = {},
        onLocationClick = {},
        onLocationClear = {},
        onPictureClear = {},
        onPictureRotateRight = {},
        onPictureRotateLeft = {},
        onNavigateUp = {},
        onAddFilter = {},
        onAddLens = {},
        onSubmit = {},
        snackbarMessage = SnackbarMessage()
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FrameEditContent(
    roll: Roll,
    frame: Frame,
    lens: Lens?,
    apertureValues: List<String>,
    shutterValues: List<String>,
    lenses: List<Lens>,
    filters: List<Filter>,
    exposureCompValues: List<String>,
    isResolvingAddress: Boolean,
    pictureBitmap: Bitmap?,
    pictureRotation: Float,
    pictureTempFileProvider: () -> Uri,
    onPictureTempFileCommit: () -> Unit,
    onPictureSelected: (Uri) -> Unit,
    onAddPictureToGallery: () -> Unit,
    onCountChange: (Int) -> Unit,
    onDateChange: (LocalDateTime) -> Unit,
    onNoteChange: (String) -> Unit,
    onApertureChange: (String?) -> Unit,
    onShutterChange: (String?) -> Unit,
    onLensChange: (Lens?) -> Unit,
    onFiltersChange: (List<Filter>) -> Unit,
    onExposureCompChange: (String) -> Unit,
    onNoOfExposuresChange: (Int) -> Unit,
    onFlashChange: (Boolean) -> Unit,
    onLightSourceChange: (LightSource) -> Unit,
    onFocalLengthChange: (Int) -> Unit,
    onLocationClick: () -> Unit,
    onLocationClear: () -> Unit,
    onPictureClear: () -> Unit,
    onPictureRotateRight: () -> Unit,
    onPictureRotateLeft: () -> Unit,
    onNavigateUp: () -> Unit,
    onAddFilter: () -> Unit,
    onAddLens: () -> Unit,
    onSubmit: () -> Unit,
    snackbarMessage: SnackbarMessage
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var countExpanded by remember { mutableStateOf(false) }
    var apertureExpanded by remember { mutableStateOf(false) }
    var shutterExpanded by remember { mutableStateOf(false) }
    var lensesExpanded by remember { mutableStateOf(false) }
    var exposureCompExpanded by remember { mutableStateOf(false) }
    var noOfExposuresExpanded by remember { mutableStateOf(false) }
    var lightSourceExpanded by remember { mutableStateOf(false) }
    var showFiltersDialog by remember { mutableStateOf(false) }
    var showCustomApertureDialog by remember { mutableStateOf(false) }
    var showCustomShutterDialog by remember { mutableStateOf(false) }
    var showFocalLengthDialog by remember { mutableStateOf(false) }
    var pictureOptionsExpanded by remember { mutableStateOf(false) }
    val lensesWithUniqueNames = remember(lenses) {
        lenses.mapNonUniqueToNameWithSerial()
    }
    val lensName = remember(frame) {
        lensesWithUniqueNames.firstOrNull { it.first.id == frame.lens?.id }?.second
            ?: frame.lens?.name
            ?: ""
    }
    val takePicture = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { result ->
        if (result) {
            onPictureTempFileCommit()
        }
    }
    val selectPicture = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { resultUri ->
        if (resultUri != null) {
            onPictureSelected(resultUri)
        }
    }
    LaunchedEffect(snackbarMessage) {
        if (snackbarMessage.message.isNotEmpty()) {
            snackbarHostState.showSnackbar(snackbarMessage.message)
        }
    }
    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            val title = if (frame.id <= 0)
                stringResource(R.string.AddNewFrame)
            else
                "${stringResource(R.string.EditFrame)}${frame.count}"
            TopAppBar(
                scrollBehavior = scrollBehavior,
                title = { Text(title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "")
                    }
                },
                actions = {
                    val padding = ButtonDefaults.ContentPadding.copy(start = 18.dp)
                    Button(
                        contentPadding = padding,
                        onClick = onSubmit
                    ) {
                        Icon(
                            modifier = Modifier.size(18.dp),
                            imageVector = Icons.Outlined.Check,
                            contentDescription = ""
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.Save))
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp)
                    .widthIn(min = 0.dp, max = 400.dp)
            ) {
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text(
                        text = stringResource(R.string.FrameCount),
                        style = MaterialTheme.typography.bodySmall
                    )
                    ExposedDropdownMenuBox(
                        expanded = countExpanded,
                        onExpandedChange = { countExpanded = it }
                    ) {
                        OutlinedTextField(
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            readOnly = true,
                            value = frame.count.toString(),
                            onValueChange = {},
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = countExpanded)
                            }
                        )
                        ExposedDropdownMenu(
                            expanded = countExpanded,
                            onDismissRequest = { countExpanded = false }
                        ) {
                            (0..100).forEach { count ->
                                DropdownMenuItem(
                                    text = { Text(count.toString()) },
                                    onClick = {
                                        onCountChange(count)
                                        countExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                Row(modifier = Modifier.padding(top = 16.dp)) {
                    Text(
                        text = stringResource(R.string.Date),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    DateTimeButtonCombo(
                        dateTime = frame.date,
                        onDateTimeSet = onDateChange
                    )
                }
                Row(modifier = Modifier.padding(top = 16.dp)) {
                    Text(
                        text = stringResource(R.string.Aperture),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExposedDropdownMenuBox(
                        modifier = Modifier.weight(1f),
                        expanded = apertureExpanded,
                        onExpandedChange = { apertureExpanded = it }
                    ) {
                        OutlinedTextField(
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            readOnly = true,
                            value = frame.aperture ?: "",
                            onValueChange = {},
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = apertureExpanded)
                            }
                        )
                        ExposedDropdownMenu(
                            expanded = apertureExpanded,
                            onDismissRequest = { apertureExpanded = false }
                        ) {
                            apertureValues.forEach { value ->
                                DropdownMenuItem(
                                    text = { Text(value) },
                                    onClick = {
                                        onApertureChange(value)
                                        apertureExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                    ) {
                        FilledTonalIconButton(onClick = { showCustomApertureDialog = true }) {
                            Icon(Icons.Outlined.Edit, "")
                        }
                    }
                    Box(
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        FilledTonalIconButton(onClick = { onApertureChange(null) }) {
                            Icon(Icons.Outlined.Clear, "")
                        }
                    }
                }
                Row(modifier = Modifier.padding(top = 16.dp)) {
                    Text(
                        text = stringResource(R.string.ShutterSpeed),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ExposedDropdownMenuBox(
                        modifier = Modifier.weight(1f),
                        expanded = shutterExpanded,
                        onExpandedChange = { shutterExpanded = it }
                    ) {
                        OutlinedTextField(
                            modifier = Modifier
                                .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                            readOnly = true,
                            value = frame.shutter ?: "",
                            onValueChange = {},
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = shutterExpanded)
                            }
                        )
                        ExposedDropdownMenu(
                            expanded = shutterExpanded,
                            onDismissRequest = { shutterExpanded = false }
                        ) {
                            shutterValues.forEach { value ->
                                DropdownMenuItem(
                                    text = { Text(value) },
                                    onClick = {
                                        onShutterChange(value)
                                        shutterExpanded = false
                                    }
                                )
                            }
                        }
                    }
                    Box(
                        modifier = Modifier.padding(vertical = 4.dp, horizontal = 8.dp)
                    ) {
                        FilledTonalIconButton(onClick = { showCustomShutterDialog = true }) {
                            Icon(Icons.Outlined.Edit, "")
                        }
                    }
                    Box(
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        FilledTonalIconButton(onClick = { onShutterChange(null) }) {
                            Icon(Icons.Outlined.Clear, "")
                        }
                    }
                }
                if (roll.camera?.isNotFixedLens != false) {
                    Row(modifier = Modifier.padding(top = 16.dp)) {
                        Text(
                            text = stringResource(R.string.Lens),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ExposedDropdownMenuBox(
                            modifier = Modifier.weight(1f),
                            expanded = lensesExpanded,
                            onExpandedChange = { lensesExpanded = it }
                        ) {
                            OutlinedTextField(
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                                    .fillMaxWidth(),
                                readOnly = true,
                                value = lensName,
                                onValueChange = {},
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = lensesExpanded)
                                }
                            )
                            ExposedDropdownMenu(
                                expanded = lensesExpanded,
                                onDismissRequest = { lensesExpanded = false }
                            ) {
                                lensesWithUniqueNames.forEach { (lens, lensName) ->
                                    DropdownMenuItem(
                                        text = { Text(lensName) },
                                        onClick = {
                                            onLensChange(lens)
                                            lensesExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            if (frame.lens != null) {
                                FilledTonalIconButton(onClick = { onLensChange(null) }) {
                                    Icon(Icons.Outlined.Clear, "")
                                }
                            } else {
                                FilledTonalIconButton(onClick = onAddLens) {
                                    Icon(Icons.Outlined.Add, "")
                                }
                            }
                        }
                    }
                }
                Row(modifier = Modifier.padding(top = 16.dp)) {
                    Text(
                        text = stringResource(R.string.FilterOrFilters),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val filtersText = frame.filters.joinToString(separator = "\n") { "-${it.name}" }
                    DropdownButton(
                        modifier = Modifier.weight(1f),
                        text = filtersText,
                        maxLines = Int.MAX_VALUE,
                        onClick = { showFiltersDialog = true }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Box(
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        FilledTonalIconButton(onClick = onAddFilter) {
                            Icon(Icons.Outlined.Add, "")
                        }
                    }
                }
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text(
                        text = stringResource(R.string.FocalLengthSingleLine),
                        style = MaterialTheme.typography.bodySmall
                    )
                    DropdownButton(
                        text = frame.focalLength.toString(),
                        onClick = { showFocalLengthDialog = true }
                    )
                }
                Column(modifier = Modifier.padding(top = 16.dp)) {
                    Text(
                        text = stringResource(R.string.Location),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    val locationText = frame.formattedAddress?.ifEmpty { null }
                        ?: frame.location?.readableCoordinates
                            ?.replace("N ", "N\n")
                            ?.replace("S ", "S\n")
                    Box(
                        modifier = Modifier
                            .padding(vertical = 4.dp)
                            .weight(1f)
                    ) {
                        DropdownButton(
                            text = locationText ?: "",
                            maxLines = 2,
                            onClick = onLocationClick
                        )
                        if (isResolvingAddress) {
                            CircularProgressIndicator(
                                modifier = Modifier
                                    .align(Alignment.CenterEnd)
                                    .padding(horizontal = 55.dp)
                                    .size(30.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Box {
                        FilledTonalIconButton(onClick = onLocationClear) {
                            Icon(Icons.Outlined.Clear, "")
                        }
                    }
                }
                Row(modifier = Modifier.padding(top = 16.dp)) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.ExposureComp),
                            style = MaterialTheme.typography.bodySmall
                        )
                        ExposedDropdownMenuBox(
                            expanded = exposureCompExpanded,
                            onExpandedChange = { exposureCompExpanded = it }
                        ) {
                            OutlinedTextField(
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                readOnly = true,
                                value = frame.exposureComp ?: "",
                                onValueChange = {},
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = exposureCompExpanded)
                                }
                            )
                            ExposedDropdownMenu(
                                expanded = exposureCompExpanded,
                                onDismissRequest = { exposureCompExpanded = false }
                            ) {
                                exposureCompValues.forEach { value ->
                                    DropdownMenuItem(
                                        text = { Text(value) },
                                        onClick = {
                                            onExposureCompChange(value)
                                            exposureCompExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.NoOfExposuresSingleLine),
                            style = MaterialTheme.typography.bodySmall
                        )
                        ExposedDropdownMenuBox(
                            expanded = noOfExposuresExpanded,
                            onExpandedChange = { noOfExposuresExpanded = it }
                        ) {
                            OutlinedTextField(
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                readOnly = true,
                                value = frame.noOfExposures.toString(),
                                onValueChange = {},
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = noOfExposuresExpanded)
                                }
                            )
                            ExposedDropdownMenu(
                                expanded = noOfExposuresExpanded,
                                onDismissRequest = { noOfExposuresExpanded = false }
                            ) {
                                (1..10).forEach { value ->
                                    DropdownMenuItem(
                                        text = { Text(value.toString()) },
                                        onClick = {
                                            onNoOfExposuresChange(value)
                                            noOfExposuresExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                Row(modifier = Modifier.padding(top = 16.dp)) {
                    Column(modifier = Modifier.weight(0.4f)) {
                        Text(
                            text = stringResource(R.string.Flash),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Box(
                            modifier = Modifier
                                .align(Alignment.End)
                                .padding(end = 32.dp)
                        ) {
                            Checkbox(
                                checked = frame.flashUsed,
                                onCheckedChange = onFlashChange
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(0.6f)) {
                        Text(
                            text = stringResource(R.string.LightSource),
                            style = MaterialTheme.typography.bodySmall
                        )
                        ExposedDropdownMenuBox(
                            expanded = lightSourceExpanded,
                            onExpandedChange = { lightSourceExpanded = it }
                        ) {
                            OutlinedTextField(
                                modifier = Modifier
                                    .menuAnchor(MenuAnchorType.PrimaryNotEditable),
                                readOnly = true,
                                value = frame.lightSource.description,
                                onValueChange = {},
                                leadingIcon = {
                                    Icon(
                                        modifier = Modifier.size(20.dp),
                                        imageVector = lightSourceImage(frame.lightSource),
                                        contentDescription = ""
                                    )
                                },
                                trailingIcon = {
                                    ExposedDropdownMenuDefaults.TrailingIcon(expanded = lightSourceExpanded)
                                }
                            )
                            ExposedDropdownMenu(
                                expanded = lightSourceExpanded,
                                onDismissRequest = { lightSourceExpanded = false }
                            ) {
                                LightSource.entries.forEach { value ->
                                    DropdownMenuItem(
                                        leadingIcon = { Icon(lightSourceImage(value), "") },
                                        text = { Text(value.description) },
                                        onClick = {
                                            onLightSourceChange(value)
                                            lightSourceExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                Row(modifier = Modifier.padding(top = 16.dp)) {
                    OutlinedTextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = frame.note ?: "",
                        onValueChange = onNoteChange,
                        label = { Text(stringResource(R.string.DescriptionOrNote)) }
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.ComplementaryPicture),
                    style = MaterialTheme.typography.bodySmall
                )
                val cameraErrorMessage = stringResource(R.string.NoCameraFeatureWasFound)
                if (frame.pictureFilename == null) {
                    Row(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TextButton(
                            onClick = {
                                if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(cameraErrorMessage)
                                    }
                                } else {
                                    val uri = pictureTempFileProvider()
                                    takePicture.launch(uri)
                                }
                            }
                        ) {
                            Icon(Icons.Outlined.CameraAlt, "")
                            Text(
                                modifier = Modifier.padding(start = 6.dp),
                                text = stringResource(R.string.TakeNewComplementaryPicture)
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        TextButton(onClick = { selectPicture.launch("image/*") }) {
                            Icon(Icons.Outlined.InsertPhoto, "")
                            Text(
                                modifier = Modifier.padding(start = 6.dp),
                                text = stringResource(R.string.SelectPictureFromGallery)
                            )
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier
                            .padding(top = 16.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        FilledTonalButton(onClick = { pictureOptionsExpanded = true }) {
                            Icon(Icons.Outlined.MoreHoriz, "")
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.Options))
                        }
                        DropdownMenu(
                            expanded = pictureOptionsExpanded,
                            onDismissRequest = { pictureOptionsExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = {
                                    Text(stringResource(R.string.TakeNewComplementaryPicture))
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.CameraAlt, "")
                                },
                                onClick = {
                                    pictureOptionsExpanded = false
                                    if (!context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY)) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(cameraErrorMessage)
                                        }
                                    } else {
                                        val uri = pictureTempFileProvider()
                                        takePicture.launch(uri)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(stringResource(R.string.SelectPictureFromGallery))
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.InsertPhoto, "")
                                },
                                onClick = {
                                    pictureOptionsExpanded = false
                                    selectPicture.launch("image/*")
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(stringResource(R.string.AddPictureToGallery))
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.AddPhotoAlternate, "")
                                },
                                onClick = {
                                    pictureOptionsExpanded = false
                                    onAddPictureToGallery()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(stringResource(R.string.RotateRight90Degrees))
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Rotate90DegreesCw, "")
                                },
                                onClick = {
                                    pictureOptionsExpanded = false
                                    onPictureRotateRight()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(stringResource(R.string.RotateLeft90Degrees))
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Rotate90DegreesCcw, "")
                                },
                                onClick = {
                                    pictureOptionsExpanded = false
                                    onPictureRotateLeft()
                                }
                            )
                            DropdownMenuItem(
                                text = {
                                    Text(stringResource(R.string.Clear))
                                },
                                leadingIcon = {
                                    Icon(Icons.Outlined.Clear, "")
                                },
                                onClick = {
                                    pictureOptionsExpanded = false
                                    onPictureClear()
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .padding(top = 16.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    if (pictureBitmap != null) {
                        val rotation = animateFloatAsState(
                            targetValue = pictureRotation,
                            label = "Image rotation",
                            animationSpec = tween(1000)
                        )
                        Image(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .rotate(rotation.value)
                                .aspectRatio(1f),
                            bitmap = pictureBitmap.asImageBitmap(),
                            contentScale = ContentScale.Fit,
                            contentDescription = ""
                        )
                    }
                }
            }
        }
    }
    if (showFiltersDialog) {
        MultiChoiceDialog(
            initialItems = filters.associateWith { filter -> frame.filters.any { it.id == filter.id } },
            itemText = { it.name },
            sortItemsBy = { it.name },
            onDismiss = { showFiltersDialog = false },
            onConfirm = { selectedFilters ->
                showFiltersDialog = false
                onFiltersChange(selectedFilters)
            }
        )
    }
    if (showCustomApertureDialog) {
        CustomApertureDialog(
            onDismiss = { showCustomApertureDialog = false },
            onConfirm = { value ->
                showCustomApertureDialog = false
                onApertureChange(value)
            }
        )
    }
    if (showCustomShutterDialog) {
        CustomShutterDialog(
            onDismiss = { showCustomShutterDialog = false },
            onConfirm = { value ->
                showCustomShutterDialog = false
                onShutterChange(value)
            }
        )
    }
    if (showFocalLengthDialog) {
        FocalLengthDialog(
            initialValue = frame.focalLength,
            minValue = lens?.minFocalLength ?: 0,
            maxValue = lens?.maxFocalLength ?: 500,
            onDismiss = { showFocalLengthDialog = false },
            onConfirm = { value ->
                showFocalLengthDialog = false
                onFocalLengthChange(value)
            }
        )
    }
}

@Preview
@Composable
fun CustomApertureDialog(
    onDismiss: () -> Unit = {},
    onConfirm: (String) -> Unit = {}
) {
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.Cancel))
            }
        },
        confirmButton = {
            TextButton(
                enabled = value.toFloatOrNull() != null,
                onClick = {
                    if (value.toFloatOrNull() != null) {
                        onConfirm(value)
                    }
                }
            ) {
                Text(stringResource(R.string.OK))
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.EnterCustomerApertureValue))
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    modifier = Modifier.width(100.dp),
                    value = value,
                    onValueChange = {
                        value = if (it.isEmpty()) {
                            it
                        } else {
                            when (it.toFloatOrNull()) {
                                null -> value
                                else -> it
                            }
                        }
                    },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        keyboardType = KeyboardType.Number
                    )
                )
            }
        }
    )
}

@Preview
@Composable
private fun CustomShutterDialog(
    onDismiss: () -> Unit = {},
    onConfirm: (String) -> Unit = {}
) {
    var value by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.Cancel))
            }
        },
        confirmButton = {
            TextButton(
                enabled = value.toShutterSpeedOrNull() != null,
                onClick = {
                    if (value.toShutterSpeedOrNull() != null) {
                        onConfirm(value)
                    }
                }
            ) {
                Text(stringResource(R.string.OK))
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(stringResource(R.string.EnterCustomShutterSpeedValue))
                Spacer(modifier = Modifier.height(16.dp))
                TextField(
                    modifier = Modifier.width(100.dp),
                    value = value,
                    onValueChange = { value = it }
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(stringResource(R.string.AllowedFormatsCustomShutterValue))
            }
        }
    )
}

@Preview
@Composable
private fun FocalLengthDialog(
    initialValue: Int = 0,
    minValue: Int = 0,
    maxValue: Int = 500,
    onDismiss: () -> Unit = {},
    onConfirm: (Int) -> Unit = {}
) {
    var value by remember(initialValue) { mutableFloatStateOf(initialValue.toFloat()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.Cancel))
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(value.roundToInt())
                }
            ) {
                Text(stringResource(R.string.OK))
            }
        },
        title = { Text(stringResource(R.string.ChooseFocalLength)) },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalButton(
                        onClick = {
                            if (value.roundToInt() - 1 >= minValue)
                                value--
                        },
                        enabled = value.roundToInt() > minValue
                    ) {
                        Text("-1")
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .background(
                                color = MaterialTheme.colorScheme.tertiaryContainer,
                                shape = CircleShape
                            )
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(8.dp)
                                .align(Alignment.Center)
                        ) {
                            Text(
                                text = value.roundToInt().toString(),
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    FilledTonalButton(
                        onClick = {
                            if (value.roundToInt() + 1 <= maxValue)
                                value++
                        },
                        enabled = value.roundToInt() < maxValue
                    ) {
                        Text("+1")
                    }
                }
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = minValue.toFloat()..maxValue.toFloat()
                )
            }
        }
    )
}

private fun lightSourceImage(lightSource: LightSource): ImageVector = when (lightSource) {
    LightSource.Unknown -> Icons.Outlined.QuestionMark
    LightSource.Daylight -> Icons.Outlined.WbTwilight
    LightSource.Sunny -> Icons.Outlined.WbSunny
    LightSource.Cloudy -> Icons.Outlined.WbCloudy
    LightSource.Shade -> Icons.Outlined.WbShade
    LightSource.Fluorescent -> Icons.Outlined.Fluorescent
    LightSource.Tungsten -> Icons.Outlined.Tungsten
    LightSource.Flash -> Icons.Outlined.FlashOn
}