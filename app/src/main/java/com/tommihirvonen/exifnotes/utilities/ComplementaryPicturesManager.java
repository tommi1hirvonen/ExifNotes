package com.tommihirvonen.exifnotes.utilities;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import com.tommihirvonen.exifnotes.R;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

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
    private static File getComplementaryPicturesDirectory(final Context context) {
        return context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);
    }

    /**
     * Creates a new placeholder file with a universally unique
     * 128-bit filename in the complementary pictures location.
     *
     * @param context activity's context
     * @return File referencing to the newly created placeholder File
     */
    public static File createNewPictureFile(final Context context) {
        // Create a unique name for the new picture file
        final String pictureFilename = UUID.randomUUID().toString() + ".jpg";
        // Create a reference to the picture file
        final File picture = getPictureFile(context, pictureFilename);
        // Get reference to the destination folder by the file's parent
        final File pictureStorageDirectory = picture.getParentFile();
        // If the destination folder does not exist, create it
        if (pictureStorageDirectory!= null && !pictureStorageDirectory.exists()) {
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
    public static File getPictureFile(final Context context, final String fileName) {
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
    public static void addPictureToGallery(final Context context, final String fileName) throws IOException {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {

            final ContentValues contentValues = new ContentValues();
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/" + context.getString(R.string.app_name));
            contentValues.put(MediaStore.Images.Media.IS_PENDING, true);
            final Uri uri = context.getContentResolver()
                    .insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
            if (uri != null) {
                final File source = ComplementaryPicturesManager.getPictureFile(context, fileName);
                final OutputStream outputStream = context.getContentResolver().openOutputStream(uri);
                final Bitmap bitmap = BitmapFactory.decodeFile(source.getAbsolutePath());
                if (outputStream != null) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
                    outputStream.close();
                }
                contentValues.put(MediaStore.Images.Media.IS_PENDING, false);
                context.getContentResolver().update(uri, contentValues, null, null);

            }

        } else {

            final File publicPictureDirectory = new File(Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), context.getString(R.string.app_name));
            final File copyFromFile = ComplementaryPicturesManager.getPictureFile(context, fileName);
            final File copyToFile = new File(publicPictureDirectory, fileName);
            Utilities.copyFile(copyFromFile, copyToFile);
            final Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
            final Uri contentUri = Uri.fromFile(copyToFile);
            mediaScanIntent.setData(contentUri);
            context.sendBroadcast(mediaScanIntent);

        }
    }

    /**
     * Compresses a complementary picture file to the size specified by MAX_SIZE.
     *
     * @param context activity's context
     * @param fileName the name of the complementary picture
     * @throws IOException thrown if saving the bitmap causes an IOException
     */
    public static void compressPictureFile(final Context context, final String fileName) throws IOException {
        // Compress the image
        final File pictureFile = getPictureFile(context, fileName);
        if (pictureFile.exists()) {
            final Bitmap bitmap = getCompressedBitmap(pictureFile);
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
    public static void saveBitmapToFile(final Bitmap bitmap, final File toFile) throws IOException {
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
    public static @Nullable Bitmap getCompressedBitmap(final Context context, final Uri uri) throws FileNotFoundException {
        // Get the dimensions of the picture
        final BitmapFactory.Options options = new BitmapFactory.Options();
        // Setting the inJustDecodeBounds property to true while decoding avoids memory allocation,
        // returning null for the bitmap object but setting outWidth, outHeight and outMimeType.
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(context.getContentResolver().openInputStream(uri), null, options);
        options.inSampleSize = calculateInSampleSize(options);
        // Set inJustDecodeBounds back to false, so that decoding returns a bitmap object.
        options.inJustDecodeBounds = false;
        // Load the bitmap into memory using the inSampleSize option to reduce the excess resolution
        // and to avoid OutOfMemoryErrors.
        final Bitmap decodedBitmap = BitmapFactory
                .decodeStream(context.getContentResolver().openInputStream(uri), null, options);
        // Then resize the bitmap to the exact dimensions specified by MAX_SIZE.
        if (decodedBitmap != null) return getResizedBitmap(decodedBitmap);
        else return null;
    }

    /**
     * Get compressed bitmap from file.
     * Method first decodes the bitmap using scaling to save memory
     * and then resizes it to exact dimension.
     *
     * @param pictureFile file to decoded to bitmap
     * @return compressed bitmap
     */
    private static Bitmap getCompressedBitmap(final File pictureFile) {
        // Get the dimensions of the picture
        final BitmapFactory.Options options = new BitmapFactory.Options();
        // Setting the inJustDecodeBounds property to true while decoding avoids memory allocation,
        // returning null for the bitmap object but setting outWidth, outHeight and outMimeType.
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(pictureFile.getAbsolutePath(), options);
        options.inSampleSize = calculateInSampleSize(options);
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
        final int outWidth;
        final int outHeight;
        final int inWidth = bitmap.getWidth();
        final int inHeight = bitmap.getHeight();
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
     * @return scaling value as integer
     */
    private static int calculateInSampleSize(final BitmapFactory.Options options) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        if (ComplementaryPicturesManager.MAX_SIZE >= Math.max(height, width)) return 1;
        else return Math.max(height / ComplementaryPicturesManager.MAX_SIZE, width / ComplementaryPicturesManager.MAX_SIZE);
    }

    /**
     * Set the picture orientation 90 degrees clockwise using ExifInterface.
     *
     * @param context activity's context
     * @param filename the name of the picture file to be rotated
     * @throws IOException if reading/writing exif data caused an exception
     */
    public static void rotatePictureRight(final Context context, final String filename) throws IOException {
        final File pictureFile = getPictureFile(context, filename);
        final ExifInterface exifInterface = new ExifInterface(pictureFile.getAbsolutePath());
        final int orientation = exifInterface
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        int newOrientation = ExifInterface.ORIENTATION_NORMAL;

        if (orientation == ExifInterface.ORIENTATION_NORMAL ||
                orientation == ExifInterface.ORIENTATION_UNDEFINED)
            newOrientation = ExifInterface.ORIENTATION_ROTATE_90;
        else if (orientation == ExifInterface.ORIENTATION_ROTATE_90)
            newOrientation = ExifInterface.ORIENTATION_ROTATE_180;
        else if (orientation == ExifInterface.ORIENTATION_ROTATE_180)
            newOrientation = ExifInterface.ORIENTATION_ROTATE_270;

        exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(newOrientation));
        exifInterface.saveAttributes();
    }

    /**
     * Set the picture orientation 90 degrees counterclockwise using ExifInterface.
     *
     * @param context activity's context
     * @param filename the name of the picture file to be rotated
     * @throws IOException if reading/writing exif data caused an exception
     */
    public static void rotatePictureLeft(final Context context, final String filename) throws IOException {
        final File pictureFile = getPictureFile(context, filename);
        final ExifInterface exifInterface = new ExifInterface(pictureFile.getAbsolutePath());
        final int orientation = exifInterface
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        int newOrientation = ExifInterface.ORIENTATION_NORMAL;

        if (orientation == ExifInterface.ORIENTATION_NORMAL ||
                orientation == ExifInterface.ORIENTATION_UNDEFINED)
            newOrientation = ExifInterface.ORIENTATION_ROTATE_270;
        else if (orientation == ExifInterface.ORIENTATION_ROTATE_180)
            newOrientation = ExifInterface.ORIENTATION_ROTATE_90;
        else if (orientation == ExifInterface.ORIENTATION_ROTATE_270)
            newOrientation = ExifInterface.ORIENTATION_ROTATE_180;

        exifInterface.setAttribute(ExifInterface.TAG_ORIENTATION, String.valueOf(newOrientation));
        exifInterface.saveAttributes();
    }

    /**
     * Deletes all complementary pictures which are not linked to any frame in the database.
     *
     * @param context activity's context
     */
    public static void deleteUnusedPictures(final Context context) {
        // List of all filenames that are being used in the database
        final List<String> complementaryPictureFilenames =
                FilmDbHelper.getInstance(context).getAllComplementaryPictureFilenames();
        // The application private external storage directory, where complementary pictures are stored
        final File picturesDirectory = getComplementaryPicturesDirectory(context);
        // Create a FileNameFilter using the filenames
        final FilenameFilter filter = (file, s) -> {
            // Include filenames, which are NOT included in the list of filenames from the database
            return !complementaryPictureFilenames.contains(s);
        };
        // Delete all files, that are not filtered
        if (picturesDirectory != null) {
            final File[] files = picturesDirectory.listFiles(filter);
            if (files != null) {
                for (final File pictureFile : files) {
                    //noinspection ResultOfMethodCallIgnored
                    pictureFile.delete();
                }
            }
        }
    }

    /**
     * Exports all linked complementary pictures to a zip in the target directory.
     *
     * @param context activity's context
     * @param targetFile the directory where the zip file should be saved
     */
    public static void exportComplementaryPictures(final Context context, final File targetFile,
                                                   final ZipFileCreatorAsyncTask.ProgressListener progressListener) {
        final List<String> complementaryPictureFilenames = FilmDbHelper.getInstance(context)
                .getAllComplementaryPictureFilenames();
        final File picturesDirectory = getComplementaryPicturesDirectory(context);
        final FilenameFilter filter = (file, s) -> complementaryPictureFilenames.contains(s);
        final File[] files = picturesDirectory != null ? picturesDirectory.listFiles(filter) : null;
        // If files is empty, no zip file will be created in ZipFileCreatorAsyncTask
        if (files != null) new ZipFileCreatorAsyncTask(files, targetFile, progressListener).execute();
    }

    /**
     * Imports complementary pictures from a zip file and unzips the to the app's
     * private external storage location.
     *
     * @param context activity's context
     * @param zipFile the zip file to be imported
     */
    public static void importComplementaryPictures(final Context context, final File zipFile,
                                                   final ZipFileReaderAsyncTask.ProgressListener progressListener) {
        final File picturesDirectory = getComplementaryPicturesDirectory(context);
        new ZipFileReaderAsyncTask(zipFile, picturesDirectory, progressListener).execute();
    }

    /**
     * Creates the directory given as parameter if it does not yet exist
     *
     * @param directory the directory to be checked and created if necessary
     */
    private static void directoryChecker(final File directory) {
        if (!directory.isDirectory()) {
            //noinspection ResultOfMethodCallIgnored
            directory.mkdirs();
        }
    }

    /**
     * Asynchronous task used to export complementary pictures to a zip file.
     */
    public static class ZipFileCreatorAsyncTask extends AsyncTask<Void, Void, Boolean> {
        /**
         * Limit the buffer memory size while reading and writing buffer into the zip stream
         */
        private static final int BUFFER = 10240; // 10KB - good buffer size for disk access
        /**
         * The files that should be included in the zip file
         */
        private final File[] files;
        /**
         * The target zip file
         */
        private final File zipFile;
        /**
         * Number of completed entries. Increment by +1 whenever a file has been zipped.
         */
        private int completedEntries = 0;
        /**
         * Reference to the implementing class's listener interface.
         */
        private final ProgressListener delegate;
        /**
         * Interface for the implementing class. Used to send progress changes and to notify,
         * when the AsyncTask has finished.
         */
        public interface ProgressListener {
            /**
             *  TODO: Add JavaDoc
             * @param progressPercentage TODO: Add JavaDoc
             * @param completed TODO: Add JavaDoc
             * @param total TODO: Add JavaDoc
             */
            void onProgressChanged(int progressPercentage, int completed, int total);

            /**
             * TODO: Add JavaDoc
             * @param success TODO: Add JavaDoc
             * @param completedEntries TODO: Add JavaDoc
             * @param zipFile TODO: Add JavaDoc
             */
            void onCompleted(boolean success, int completedEntries, File zipFile);
        }

        /**
         * Constructor
         *
         * @param files files to be zipped
         * @param zipFile target zip file
         * @param delegate implementing class's interface
         */
        ZipFileCreatorAsyncTask(final File[] files, final File zipFile, final ProgressListener delegate) {
            this.files = files;
            this.zipFile = zipFile;
            this.delegate = delegate;
        }

        /**
         * {@inheritDoc}
         *
         * @param voids ignored
         * @return true if the zipping was successful, false if not
         */
        @Override
        protected Boolean doInBackground(final Void... voids) {
            try {
                // If the files array is empty, return true and end here.
                if (files.length == 0) return true;
                // Publish empty progress to tell the interface, that the process has begun.
                publishProgress();
                // Set the ZipOutputStream beginning with FileOutputStream then ZipOutputStream.
                final ZipOutputStream outputStream = new ZipOutputStream(new FileOutputStream(zipFile));
                // byte array where the bytes read from input stream should be stored.
                final byte[] buffer = new byte[BUFFER];
                // Iterate the files from the files array
                for (final File file : files) {
                    // Set the BufferedInputStream using FileInputStream
                    final BufferedInputStream inputStream = new BufferedInputStream(new FileInputStream(file), BUFFER);
                    final ZipEntry entry = new ZipEntry(file.getName());
                    // Begin writing a new zip file entry.
                    outputStream.putNextEntry(entry);
                    int count;
                    // BufferedInputStream.read() returns the number of bytes read
                    // or -1 if the end of stream was reached.
                    while ((count = inputStream.read(buffer, 0, BUFFER)) != -1) {
                        outputStream.write(buffer, 0, count);
                    }
                    inputStream.close();
                    ++completedEntries;
                    publishProgress();
                }
                outputStream.close();
            } catch (final IOException e) {
                return false;
            }
            return true;
        }

        @Override
        protected void onProgressUpdate(final Void... values) {
            // Pass the progress percentage to the implementing class's interface.
            delegate.onProgressChanged((int) ((float) completedEntries / (float) files.length * 100f),
                    completedEntries, files.length);
        }

        /**
         * {@inheritDoc}
         *
         * @param bool true if the unzipping was successful, false if not
         */
        @Override
        protected void onPostExecute(final Boolean bool) {
            // Tell the implementing class, that the task has been finished.
            delegate.onCompleted(bool, completedEntries, zipFile);
        }
    }

    /**
     * Asynchronous task used to import complementary pictures from a zip file.
     */
    public static class ZipFileReaderAsyncTask extends AsyncTask<Void, Void, Boolean> {
        /**
         * Limit the buffer memory size while reading and writing buffer from the zip stream
         */
        private static final int BUFFER = 10240; // 10KB - good buffer size for disk access
        /**
         * The zip file to be unzipped
         */
        private final File zipFile;
        /**
         * Target directory where the files from the zip file should be placed
         */
        private final File targetDirectory;
        /**
         * Number of completed entries. Increment by +1 whenever a file has been unzipped.
         */
        private int completedEntries = 0;
        /**
         * Total number of entries in the zip file.
         */
        private int totalEntries;
        /**
         * Reference to the implementing class's listener interface.
         */
        private final ProgressListener delegate;
        /**
         * Interface for the implementing class. Used to send progress changes and to notify,
         * when the AsyncTask has finished.
         */
        public interface ProgressListener {
            /**
             *  TODO: Add JavaDoc
             * @param progressPercentage TODO: Add JavaDoc
             * @param completed TODO: Add JavaDoc
             * @param total TODO: Add JavaDoc
             */
            void onProgressChanged(int progressPercentage, int completed, int total);

            /**
             *  TODO: Add JavaDoc
             * @param success TODO: Add JavaDoc
             * @param completedEntries TODO: Add JavaDoc
             */
            void onCompleted(boolean success, int completedEntries);
        }

        /**
         * Constructor
         *
         * @param zipFile the file to be unzipped
         * @param targetDirectory the directory where the files from the zip file should be placed
         * @param delegate implementing class's interface
         */
        ZipFileReaderAsyncTask(final File zipFile, final File targetDirectory, final ProgressListener delegate) {
            this.zipFile = zipFile;
            this.targetDirectory = targetDirectory;
            this.delegate = delegate;
        }

        /**
         * {@inheritDoc}
         *
         * @param voids ignore
         * @return true if the unzipping was successful, false if not
         */
        @Override
        protected Boolean doInBackground(final Void... voids) {
            try {
                totalEntries = new ZipFile(zipFile).size();
                // If the zip file was empty, return true and end here.
                if (totalEntries == 0) return true;
                // Publish empty progress to tell the interface, that the process has begun.
                publishProgress();
                // Create target directory if it does not exists
                directoryChecker(targetDirectory);
                final ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(zipFile));
                ZipEntry zipEntry;
                final byte[] buffer = new byte[BUFFER];
                while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                    // Create directory if required while unzipping
                    if (zipEntry.isDirectory()) {
                        directoryChecker(new File(targetDirectory, zipEntry.getName()));
                    } else {
                        // Set the FileOutputStream using File
                        final File targetFile = new File(targetDirectory, zipEntry.getName());
                        if (targetFile.exists()) //noinspection ResultOfMethodCallIgnored
                            targetFile.delete();
                        final FileOutputStream outputStream = new FileOutputStream(targetFile);
                        int count;
                        // ZipInputStream.read() returns the number of bytes read
                        // or -1 if the end of stream was reached.
                        while ((count = zipInputStream.read(buffer, 0, BUFFER)) != -1) {
                            outputStream.write(buffer, 0, count);
                        }
                        zipInputStream.closeEntry();
                        outputStream.close();
                        ++completedEntries;
                        publishProgress();
                    }
                }
                zipInputStream.close();
            } catch (final IOException e) {
                return false;
            }
            return true;
        }

        @Override
        protected void onProgressUpdate(final Void... voids) {
            // Pass the progress percentage to the implementing class's interface.
            delegate.onProgressChanged((int) ((float) completedEntries / (float) totalEntries * 100f),
                    completedEntries, totalEntries);
        }

        /**
         * {@inheritDoc}
         *
         * @param bool true if the unzipping was successful, false if not
         */
        @Override
        protected void onPostExecute(final Boolean bool) {
            // Tell the implementing class, that the task has been finished.
            delegate.onCompleted(bool, completedEntries);
        }
    }

}
