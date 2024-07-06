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

// Top-level build file where you can add configuration options common to all sub-projects/modules.

buildscript {
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath(libs.gradle)
        classpath(libs.jetbrains.kotlin.gradle.plugin)
        classpath(libs.jetbrains.kotlin.serialization)
        classpath(libs.androidx.navigation.safe.args.gradle.plugin)
        classpath(libs.gradle.license.plugin)

        // NOTE: Do not place your application dependencies here; they belong
        // in the individual module build.gradle files
    }
}

plugins {
    alias(libs.plugins.jetbrains.kotlin.android) apply false
    alias(libs.plugins.android.hilt) apply false
}

allprojects {
    repositories {
        google()
        mavenCentral()
    }
}
