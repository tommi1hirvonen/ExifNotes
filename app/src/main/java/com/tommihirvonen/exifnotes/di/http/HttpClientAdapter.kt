/*
 * Exif Notes
 * Copyright (C) 2023  Tommi Hirvonen
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

package com.tommihirvonen.exifnotes.di.http

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton container for getting a common instance of HttpClient.
 * The recommendation is to use the same HttpClient instance for multiple requests,
 * since creating a HttpClient is not a cheap operation.
 */
@Singleton
class HttpClientAdapter @Inject constructor() {
    val client = HttpClient(CIO) { expectSuccess = true }
}