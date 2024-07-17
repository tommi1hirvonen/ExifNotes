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

package com.tommihirvonen.exifnotes.di.pictures

import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import com.tommihirvonen.exifnotes.R
import com.tommihirvonen.exifnotes.data.repositories.FrameRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.*
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages all complementary pictures attached to frames.
 */
@Singleton
class ComplementaryPicturesManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val complementaryPicturesDirectoryProvider: ComplementaryPicturesDirectoryProvider,
    private val frameRepository: FrameRepository
) {

    companion object {
        /**
         * Constant specifying the maximum allowed length of the complementary picture's longer side.
         */
        private const val MAX_SIZE = 1024
    }

    /**
     * Creates a new placeholder file with a universally unique
     * 128-bit filename in the complementary pictures location.
     *
     * @return File referencing to the newly created placeholder File
     */
    fun createNewPictureFile(): File {
        // Create a unique name for the new picture file
        val pictureFilename = UUID.randomUUID().toString() + ".jpg"
        // Create a reference to the picture file
        val picture = getPictureFile(pictureFilename)
        // Get reference to the destination folder by the file's parent
        val pictureStorageDirectory = picture.parentFile
        // If the destination folder does not exist, create it
        if (pictureStorageDirectory != null && !pictureStorageDirectory.exists()) {
            pictureStorageDirectory.mkdirs() // also create possible non-existing parent directories -> mkdirs()
        }
        // Return the File
        return picture
    }

    /**
     * Method the get reference to a complementary picture file in the complementary pictures
     * location with only the filename.
     *
     * @param fileName the name of the complementary picture file
     * @return reference to the complementary picture file
     */
    fun getPictureFile(fileName: String): File {
        // Get the absolute path to the picture file.
        return File(complementaryPicturesDirectoryProvider.directory, fileName)
    }

    /**
     * Method to copy a complementary picture to a public external storage directory
     * and to notify the gallery application(s), that they should scan that file.
     *
     * @param filename the name of the complementary picture
     * @throws IOException thrown if the file copying failed
     */
    fun addPictureToGallery(filename: String?) {
        if (filename == null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val contentValues = ContentValues()
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/" + context.getString(R.string.app_name))
            contentValues.put(MediaStore.Images.Media.IS_PENDING, true)
            val uri = context.contentResolver
                    .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            if (uri != null) {
                val source = getPictureFile(filename)
                val outputStream = context.contentResolver.openOutputStream(uri)
                val bitmap = BitmapFactory.decodeFile(source.absolutePath)
                if (outputStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    outputStream.close()
                }
                contentValues.put(MediaStore.Images.Media.IS_PENDING, false)
                context.contentResolver.update(uri, contentValues, null, null)
            }
        } else {
            val publicPictureDirectory = File(Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), context.getString(R.string.app_name))
            val copyFromFile = getPictureFile(filename)
            val copyToFile = File(publicPictureDirectory, filename)
            copyFromFile.copyTo(target = copyToFile, overwrite = true)
            @Suppress("DEPRECATION")
            val mediaScanIntent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
            val contentUri = Uri.fromFile(copyToFile)
            mediaScanIntent.data = contentUri
            context.sendBroadcast(mediaScanIntent)
        }
    }

    /**
     * Compresses a complementary picture file to the size specified by MAX_SIZE.
     *
     * @param fileName the name of the complementary picture
     * @throws IOException thrown if saving the bitmap causes an IOException
     */
    @Throws(IOException::class)
    fun compressPictureFile(fileName: String) {
        // Compress the image
        val pictureFile = getPictureFile(fileName)
        if (pictureFile.exists()) {
            // Get the original orientation
            val exif = ExifInterface(pictureFile.absolutePath)
            val orientation = exif
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

            // Replace the file with the compressed bitmap.
            val bitmap = getCompressedBitmap(pictureFile)
            pictureFile.delete()
            saveBitmapToFile(bitmap, pictureFile)

            // Save the orientation to the new file as it may have been lost during compression.
            exif.setAttribute(ExifInterface.TAG_ORIENTATION, orientation.toString())
            exif.saveAttributes()
        }
    }

    /**
     * Saves bitmap to a file
     *
     * @param bitmap bitmap to be saved
     * @param toFile file where the bitmap should be saved
     * @throws IOException if the file was not found or if the output stream could not be flushed/closed
     */
    @Throws(IOException::class)
    fun saveBitmapToFile(bitmap: Bitmap, toFile: File) {
        val out = FileOutputStream(toFile)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
        out.flush()
        out.close()
    }

    /**
     * Get compressed bitmap from Uri.
     * Method first decodes the bitmap using scaling to save memory
     * and then resizes it to exact dimension.
     *
     * @param uri uri to the file
     * @return compressed bitmap
     * @throws FileNotFoundException if no file was found using the given Uri
     */
    @Throws(FileNotFoundException::class)
    fun getCompressedBitmap(uri: Uri): Bitmap? {
        // Get the dimensions of the picture
        val options = BitmapFactory.Options()
        // Setting the inJustDecodeBounds property to true while decoding avoids memory allocation,
        // returning null for the bitmap object but setting outWidth, outHeight and outMimeType.
        options.inJustDecodeBounds = true
        BitmapFactory.decodeStream(context.contentResolver.openInputStream(uri), null, options)
        options.inSampleSize = calculateInSampleSize(options)
        // Set inJustDecodeBounds back to false, so that decoding returns a bitmap object.
        options.inJustDecodeBounds = false
        // Load the bitmap into memory using the inSampleSize option to reduce the excess resolution
        // and to avoid OutOfMemoryErrors.
        val decodedBitmap = BitmapFactory
                .decodeStream(context.contentResolver.openInputStream(uri), null, options)
        // Then resize the bitmap to the exact dimensions specified by MAX_SIZE.
        return decodedBitmap?.let { getResizedBitmap(it) }
    }

    /**
     * Get compressed bitmap from file.
     * Method first decodes the bitmap using scaling to save memory
     * and then resizes it to exact dimension.
     *
     * @param pictureFile file to decoded to bitmap
     * @return compressed bitmap
     */
    private fun getCompressedBitmap(pictureFile: File): Bitmap {
        // Get the dimensions of the picture
        val options = BitmapFactory.Options()
        // Setting the inJustDecodeBounds property to true while decoding avoids memory allocation,
        // returning null for the bitmap object but setting outWidth, outHeight and outMimeType.
        options.inJustDecodeBounds = true
        BitmapFactory.decodeFile(pictureFile.absolutePath, options)
        options.inSampleSize = calculateInSampleSize(options)
        // Set inJustDecodeBounds back to false, so that decoding returns a bitmap object.
        options.inJustDecodeBounds = false
        // Load the bitmap into memory using the inSampleSize option to reduce the excess resolution
        // and to avoid OutOfMemoryErrors.
        val bitmap = BitmapFactory.decodeFile(pictureFile.absolutePath, options)
        // Then resize the bitmap to the exact dimensions specified by MAX_SIZE.
        return getResizedBitmap(bitmap)
    }

    /**
     * Resizes bitmap to exact dimensions and returns a new bitmap object (if resizing was made).
     *
     * @param bitmap the bitmap to be resized
     * @return a new bitmap with exact maximum dimensions
     */
    private fun getResizedBitmap(bitmap: Bitmap): Bitmap {
        val maxSize = MAX_SIZE
        val outWidth: Int
        val outHeight: Int
        val inWidth = bitmap.width
        val inHeight = bitmap.height

        if (maxSize >= maxOf(inHeight, inWidth)) return bitmap
        if (inWidth > inHeight) {
            outWidth = maxSize
            outHeight = inHeight * maxSize / inWidth
        } else {
            outHeight = maxSize
            outWidth = inWidth * maxSize / inHeight
        }
        return Bitmap.createScaledBitmap(bitmap, outWidth, outHeight, false)
    }

    /**
     * Calculates the value of inSampleSize for bitmap decoding.
     *
     * If set to a value > 1, requests the decoder to subsample the original image,
     * returning a smaller image to save memory. The sample size is the number of pixels in either
     * dimension that correspond to a single pixel in the decoded bitmap. For example,
     * inSampleSize == 4 returns an image that is 1/4 the width/height of the original,
     * and 1/16 the number of pixels. Any value <= 1 is treated the same as 1.
     * Note: the decoder uses a final value based on powers of 2,
     * any other value will be rounded down to the nearest power of 2.
     *
     * @param options the BitmapFactory options for the bitmap to be decoded
     * @return scaling value as integer
     */
    private fun calculateInSampleSize(options: BitmapFactory.Options): Int {
        val height = options.outHeight
        val width = options.outWidth
        return if (MAX_SIZE >= maxOf(height, width)) 1 else maxOf(height / MAX_SIZE, width / MAX_SIZE)
    }

    /**
     * Set the picture orientation 90 degrees clockwise using ExifInterface.
     *
     * @param filename the name of the picture file to be rotated
     * @throws IOException if reading/writing exif data caused an exception
     */
    @Throws(IOException::class)
    fun rotatePictureRight(filename: String) {
        val pictureFile = getPictureFile(filename)
        val exifInterface = ExifInterface(pictureFile.absolutePath)
        val orientation = exifInterface
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        var newOrientation = ExifInterface.ORIENTATION_NORMAL
        if (orientation == ExifInterface.ORIENTATION_NORMAL || orientation == ExifInterface.ORIENTATION_UNDEFINED)
            newOrientation = ExifInterface.ORIENTATION_ROTATE_90
        else if (orientation == ExifInterface.ORIENTATION_ROTATE_90)
            newOrientation = ExifInterface.ORIENTATION_ROTATE_180
        else if (orientation == ExifInterface.ORIENTATION_ROTATE_180)
            newOrientation = ExifInterface.ORIENTATION_ROTATE_270
        exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, newOrientation.toString())
        exifInterface.saveAttributes()
    }

    /**
     * Set the picture orientation 90 degrees counterclockwise using ExifInterface.
     *
     * @param filename the name of the picture file to be rotated
     * @throws IOException if reading/writing exif data caused an exception
     */
    @Throws(IOException::class)
    fun rotatePictureLeft(filename: String) {
        val pictureFile = getPictureFile(filename)
        val exifInterface = ExifInterface(pictureFile.absolutePath)
        val orientation = exifInterface
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        var newOrientation = ExifInterface.ORIENTATION_NORMAL
        if (orientation == ExifInterface.ORIENTATION_NORMAL || orientation == ExifInterface.ORIENTATION_UNDEFINED)
            newOrientation = ExifInterface.ORIENTATION_ROTATE_270
        else if (orientation == ExifInterface.ORIENTATION_ROTATE_180)
            newOrientation = ExifInterface.ORIENTATION_ROTATE_90
        else if (orientation == ExifInterface.ORIENTATION_ROTATE_270)
            newOrientation = ExifInterface.ORIENTATION_ROTATE_180
        exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, newOrientation.toString())
        exifInterface.saveAttributes()
    }

    /**
     * Deletes all complementary pictures which are not linked to any frame in the database.
     *
     */
    fun deleteUnusedPictures() {
        // List of all filenames that are being used in the database
        val complementaryPictureFilenames = frameRepository.complementaryPictureFilenames
        // The application private external storage directory, where complementary pictures are stored
        val picturesDirectory = complementaryPicturesDirectoryProvider.directory
        // Create a FileNameFilter using the filenames
        val filter = FilenameFilter { _: File?, s: String? -> !complementaryPictureFilenames.contains(s) }
        // Delete all files, that are not filtered
        if (picturesDirectory != null) {
            val files = picturesDirectory.listFiles(filter)
            if (files != null) {
                for (pictureFile in files) {
                    pictureFile.delete()
                }
            }
        }
    }
}