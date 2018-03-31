package com.tommihirvonen.exifnotes.utilities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Environment;
import android.support.annotation.NonNull;

import com.tommihirvonen.exifnotes.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

/**
 * Manages all complementary pictures attached to frames.
 * Mimics static class and doesn't allow instantiation.
 */
public final class ComplementaryPicturesManager {

    /**
     * Constant specifying the maximum allowed length of the complementary picture's longer side.
     */
    private static final int MAX_SIZE = 1024;

    /**
     * Private empty constructor to limit instantiation.
     */
    private ComplementaryPicturesManager() {}

    /**
     * Method to get the directory location of complementary pictures.
     *
     * @param context activity's context
     * @return directory File for the location of complementary pictures
     */
    private static File getComplementaryPicturesDirectory(Context context) {
        return context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
    }

    /**
     * Creates a new placeholder file with a universally unique
     * 128-bit filename in the complementary pictures location.
     *
     * @param context activity's context
     * @return File referencing to the newly created placeholder File
     */
    public static File createNewPictureFile(Context context) {
        // Create a unique name for the new picture file
        final String pictureFilename = UUID.randomUUID().toString() + ".jpg";
        // Create a reference to the picture file
        final File picture = getPictureFile(context, pictureFilename);
        // Get reference to the destination folder by the file's parent
        final File pictureStorageDirectory = picture.getParentFile();
        // If the destination folder does not exist, create it
        if (!pictureStorageDirectory.exists()) {
            //noinspection ResultOfMethodCallIgnored
            pictureStorageDirectory.mkdirs(); // also create possible non-existing parent directories -> mkdirs()
        }
        // Return the File
        return picture;
    }

    /**
     * Method the get reference to a complementary picture file in the complementary pictures
     * location with only the filename.
     *
     * @param context activity's location
     * @param fileName the name of the complementary picture file
     * @return reference to the complementary picture file
     */
    public static File getPictureFile(Context context, final String fileName) {
        // Get the absolute path to the picture file.
        return new File(getComplementaryPicturesDirectory(context), fileName);
    }

    /**
     * Method to copy a complementary picture to a public external storage directory
     * and to notify the gallery application(s), that they should scan that file.
     *
     * @param context activity's context
     * @param fileName the name of the complementary picture
     * @throws IOException thrown if the file copying failed
     */
    public static void addPictureToGallery(Context context, String fileName) throws IOException {
        final File publicPictureDirectory = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), context.getString(R.string.app_name));
        final File copyFromFile = ComplementaryPicturesManager.getPictureFile(context, fileName);
        final File copyToFile = new File(publicPictureDirectory, fileName);
        Utilities.copyFile(copyFromFile, copyToFile);
        final Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        final Uri contentUri = Uri.fromFile(copyToFile);
        mediaScanIntent.setData(contentUri);
        context.sendBroadcast(mediaScanIntent);
    }

    /**
     * Compresses a complementary picture file to the size specified by MAX_SIZE.
     *
     * @param context activity's context
     * @param fileName the name of the complementary picture
     * @throws IOException thrown if saving the bitmap causes an IOException
     */
    public static void compressPictureFile(Context context, String fileName) throws IOException {
        // Compress the image
        final File pictureFile = getPictureFile(context, fileName);
        if (pictureFile.exists()) {
            Bitmap bitmap = getCompressedBitmap(pictureFile);
            //noinspection ResultOfMethodCallIgnored
            pictureFile.delete();
            saveBitmapToFile(bitmap, pictureFile);
        }
    }

    /**
     * Saves bitmap to a file
     *
     * @param bitmap bitmap to be saved
     * @param toFile file where the bitmap should be saved
     * @throws IOException if the file was not found or if the output stream could not be flushed/closed
     */
    public static void saveBitmapToFile(Bitmap bitmap, File toFile) throws IOException {
        final FileOutputStream out = new FileOutputStream(toFile);
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
        out.flush();
        out.close();
    }

    /**
     * Get compressed bitmap from Uri.
     * Method first decodes the bitmap using scaling to save memory
     * and then resizes it to exact dimension.
     *
     * @param context activity's context
     * @param uri uri to the file
     * @return compressed bitmap
     * @throws FileNotFoundException if no file was found using the given Uri
     */
    public static Bitmap getCompressedBitmap(Context context, Uri uri) throws FileNotFoundException {
        // Get the dimensions of the picture
        final BitmapFactory.Options options = new BitmapFactory.Options();
        // Setting the inJustDecodeBounds property to true while decoding avoids memory allocation,
        // returning null for the bitmap object but setting outWidth, outHeight and outMimeType.
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(context.getContentResolver().openInputStream(uri), null, options);
        options.inSampleSize = calculateInSampleSize(options, MAX_SIZE);
        // Set inJustDecodeBounds back to false, so that decoding returns a bitmap object.
        options.inJustDecodeBounds = false;
        // Load the bitmap into memory using the inSampleSize option to reduce the excess resolution
        // and to avoid OutOfMemoryErrors.
        final Bitmap decodedBitmap = BitmapFactory.decodeStream(context.getContentResolver().openInputStream(uri), null, options);
        // Then resize the bitmap to the exact dimensions specified by MAX_SIZE.
        return getResizedBitmap(decodedBitmap);
    }

    /**
     * Get compressed bitmap from file.
     * Method first decodes the bitmap using scaling to save memory
     * and then resizes it to exact dimension.
     *
     * @param pictureFile file to decoded to bitmap
     * @return compressed bitmap
     */
    private static Bitmap getCompressedBitmap(File pictureFile) {
        // Get the dimensions of the picture
        final BitmapFactory.Options options = new BitmapFactory.Options();
        // Setting the inJustDecodeBounds property to true while decoding avoids memory allocation,
        // returning null for the bitmap object but setting outWidth, outHeight and outMimeType.
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(pictureFile.getAbsolutePath(), options);
        options.inSampleSize = calculateInSampleSize(options, MAX_SIZE);
        // Set inJustDecodeBounds back to false, so that decoding returns a bitmap object.
        options.inJustDecodeBounds = false;
        // Load the bitmap into memory using the inSampleSize option to reduce the excess resolution
        // and to avoid OutOfMemoryErrors.
        final Bitmap bitmap = BitmapFactory.decodeFile(pictureFile.getAbsolutePath(), options);
        // Then resize the bitmap to the exact dimensions specified by MAX_SIZE.
        return getResizedBitmap(bitmap);
    }

    /**
     * Resizes bitmap to exact dimensions and returns a new bitmap object (if resizing was made).
     *
     * @param bitmap the bitmap to be resized
     * @return a new bitmap with exact maximum dimensions
     */
    private static Bitmap getResizedBitmap(final Bitmap bitmap) {
        final int maxSize = MAX_SIZE;
        int outWidth;
        int outHeight;
        int inWidth = bitmap.getWidth();
        int inHeight = bitmap.getHeight();
        if (maxSize >= Math.max(inHeight, inWidth)) return bitmap;
        if(inWidth > inHeight){
            outWidth = maxSize;
            outHeight = (inHeight * maxSize) / inWidth;
        } else {
            outHeight = maxSize;
            outWidth = (inWidth * maxSize) / inHeight;
        }
        return Bitmap.createScaledBitmap(bitmap, outWidth, outHeight, false);
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
     * @param maxSize the maximum length of the longer side of the bitmap
     * @return scaling value as integer
     */
    private static int calculateInSampleSize(final BitmapFactory.Options options, final int maxSize) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        if (maxSize >= Math.max(height, width)) return 1;
        else return Math.max(height / maxSize, width / maxSize);
    }

    /**
     * Deletes all complementary pictures which are not linked to any frame in the database.
     *
     * @param context activity's context
     */
    public static void deleteUnusedPictures(Context context) {
        // List of all filenames that are being used in the database
        final List<String> complementaryPictureFilenames = FilmDbHelper.getInstance(context).getAllComplementaryPictureFilenames();
        // The application private external storage directory, where complementary pictures are stored
        final File picturesDirectory = getComplementaryPicturesDirectory(context);
        // Create a FileNameFilter using the filenames
        final FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                // Include filenames, which are NOT included in the list of filenames from the database
                return !complementaryPictureFilenames.contains(s);
            }
        };
        // Delete all files, that are not filtered
        if (picturesDirectory != null) {
            for (File pictureFile : picturesDirectory.listFiles(filter)) {
                //noinspection ResultOfMethodCallIgnored
                pictureFile.delete();
            }
        }
    }

}
