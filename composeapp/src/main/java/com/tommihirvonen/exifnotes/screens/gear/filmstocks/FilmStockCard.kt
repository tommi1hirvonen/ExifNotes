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

package com.tommihirvonen.exifnotes.screens.gear.filmstocks

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.core.entities.FilmProcess
import com.tommihirvonen.exifnotes.core.entities.FilmStock
import com.tommihirvonen.exifnotes.core.entities.FilmType
import com.tommihirvonen.exifnotes.util.description

@Preview
@Composable
private fun FilmStockCardPreview() {
    val filmStock = FilmStock(
        make = "Tommi's Lab",
        model = "Rainbow 400",
        iso = 400,
        type = FilmType.BW_NEGATIVE,
        process = FilmProcess.C41
    )
    FilmStockCard(
        filmStock = filmStock,
        onClick = {}
    )
}

@Composable
fun FilmStockCard(
    filmStock: FilmStock,
    onClick: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Card(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 6.dp)
                .fillMaxWidth(),
            onClick = onClick,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(6.dp)
            ) {
                Text(filmStock.name, style = MaterialTheme.typography.titleMedium)
                Row {
                    Text(
                        modifier = Modifier.weight(0.3f),
                        text = stringResource(R.string.ISO) + ":"
                    )
                    Text(
                        modifier = Modifier.weight(0.7f),
                        text = filmStock.iso.toString()
                    )
                }
                Row {
                    Text(
                        modifier = Modifier.weight(0.3f),
                        text = stringResource(R.string.FilmType) + ":"
                    )
                    Text(
                        modifier = Modifier.weight(0.7f),
                        text = filmStock.type.description ?: ""
                    )
                }
                Row {
                    Text(
                        modifier = Modifier.weight(0.3f),
                        text = stringResource(R.string.FilmProcess) + ":"
                    )
                    Text(
                        modifier = Modifier.weight(0.7f),
                        text = filmStock.process.description ?: ""
                    )
                }
            }
        }
    }
}