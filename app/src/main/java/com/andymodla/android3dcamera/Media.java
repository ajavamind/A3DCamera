package com.andymodla.android3dcamera;

import static java.lang.System.exit;

import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.andymodla.android3dcamera.sketch.PhotoBooth;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import processing.core.PApplet;
import processing.core.PImage;

public class Media {
    private static String TAG = "Media";
    Context context;
    private String BASE_FOLDER = Environment.DIRECTORY_DCIM;
    //private String BASE_FOLDER = Environment.DIRECTORY_PICTURES;
    //private String DOWNLOAD_FOLDER_NAME = Environment.DIRECTORY_DOWNLOADS;
    private String SAVE_FOLDER = "A3DCamera";
    private String SAVE_ANA_FOLDER = "Anaglyph";
    private String SAVE_LR_FOLDER = "LR";
    private String SAVE_AI_EDIT_FOLDER = "AiEdit";

    public static String lastSavedFilePath = null;

    private volatile boolean crossEye = false;  // reverse SBS output to cross eye
    private volatile boolean mirror = false;  // reverse mirror image
    private volatile boolean isAnaglyphDisplayMode = false; //true;

    // Image Save File modes
    // at least one of these booleans must be true;
    private volatile boolean saveAnaglyph = true;
    private volatile boolean saveSBS = true;
    private volatile boolean saveLR = true;

    // save jpg image quality
    private static final int JPEG_QUALITY = 100;

    volatile Bitmap leftBitmap;
    volatile Bitmap rightBitmap;
    //volatile Bitmap sbsBitmap;
    //volatile Bitmap anaglyphBitmap;
    public volatile PImage leftReview;
    public volatile PImage rightReview;

    String timestamp;
    volatile private File reviewSBS;
    volatile private File reviewAnaglyph;
    volatile private File reviewLeft;
    volatile private File reviewRight;

    private String PHOTO_PREFIX = "IMG_";
    public static String APP_REVIEW_PACKAGE = "jp.suto.stereoroidpro"; // Review with StereoRoidPro app default
    public static String APP_AIEDIT_PACKAGE = "com.andymodla.fluxkontext"; // AI edit with itcamera app default
    public static String APP_PHOTO_REVIEW_PACKAGE = "com.google.android.apps.photosgo"; // Review with Gallery
    private String APP_CANON_PRINT_SERVICE_PACKAGE = "jp.co.canon.android.printservice.plugin";
    //APP_REVIEW_PACKAGE = "com.leialoft.leiaplayer"; // Review with Leia Player app default

    private Parameters parameters;
    private AIvision aiVision;
    private PApplet pApplet;

    // Constructor
    public Media(Context context, Parameters parameters, AIvision aiVision) {
        this.context = context;
        this.parameters = parameters;
        this.aiVision = aiVision;
    }

    public void setupApplet(PApplet pApplet) {
        this.pApplet = pApplet;
    }

    /**
     * Recycle bitmaps
     *
     * @return
     */
    public void recycleBitmaps() {
        if (leftBitmap != null) {
            leftBitmap.recycle();
            leftBitmap = null;
        }
        if (rightBitmap != null) {
            rightBitmap.recycle();
            rightBitmap = null;
        }
//        if (sbsBitmap != null) {
//            sbsBitmap.recycle();
//            sbsBitmap = null;
//        }
//        if (anaglyphBitmap != null) {
//            anaglyphBitmap.recycle();
//            anaglyphBitmap = null;
//        }
    }

    public void createMediaFolder() {
        File mediaStorageDir = new File(
                Environment.getExternalStoragePublicDirectory(BASE_FOLDER), SAVE_FOLDER);

        // Create the storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.e(TAG, "Failed to create directory to save photo: " + mediaStorageDir.getAbsolutePath());
                Toast.makeText(context, "Error creating folder " + SAVE_FOLDER, Toast.LENGTH_SHORT).show();
                exit(1); // System exit
            }
        }
        mediaStorageDir = new File(
                Environment.getExternalStoragePublicDirectory(BASE_FOLDER), SAVE_FOLDER + File.separator + SAVE_ANA_FOLDER);

        // Create the Anaglyph storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.e(TAG, "Failed to create directory to save photo: " + mediaStorageDir.getAbsolutePath());
                Toast.makeText(context, "Error creating folder " + SAVE_ANA_FOLDER, Toast.LENGTH_SHORT).show();
                exit(1); // System exit
            }
        }

        mediaStorageDir = new File(
                Environment.getExternalStoragePublicDirectory(BASE_FOLDER), SAVE_FOLDER + File.separator + SAVE_LR_FOLDER);

        // Create the LR storage directory if it does not exist
        if (!mediaStorageDir.exists()) {
            if (!mediaStorageDir.mkdirs()) {
                Log.e(TAG, "Failed to create directory to save photo: " + mediaStorageDir.getAbsolutePath());
                Toast.makeText(context, "Error creating folder " + SAVE_LR_FOLDER, Toast.LENGTH_SHORT).show();
                exit(1); // System exit
            }
        }

    }

    public Bitmap saveImageFile(byte[] bytes, String filename, boolean left) {
        Bitmap bitmap = null;

        if (left) {
            filename += "_l.jpg";
        } else {
            filename += "_r.jpg";
        }

        BitmapFactory.Options options = new BitmapFactory.Options();
        if (((MainActivity)context).isPhotoBooth) {
            options.inSampleSize = 2; // Try reducing image size to avoid out of memory error
        }
        // options.inPreferredConfig = Bitmap.Config.ARGB_8888; // Try specifying a config

        Log.d(TAG, "SaveImageFile " + filename);
        bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length, options);
        if (bitmap == null) {
            Log.e(TAG, "Image decoding failed! " + (left ? "left" : "right"));
            return null;
        } else {
            if (!saveLR) {
                return bitmap;
            }
        }

        File file = new File(Environment.getExternalStoragePublicDirectory(BASE_FOLDER + File.separator + SAVE_FOLDER + File.separator + SAVE_LR_FOLDER), filename);

        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(bytes);
            output.flush();
            output.close();

            // Trigger media scanner to make image visible in gallery
            MediaScannerConnection.scanFile(context, new String[]{file.getAbsolutePath()},
                    new String[]{"image/jpeg"}, null);

            Log.d(TAG, "MediaScannerConnection.scanFile Image saved: " + file.getAbsolutePath());
            if (left) reviewLeft = file;
            else reviewRight = file;
        } catch (IOException e) {
            Log.e(TAG, "Error saving image", e);
            return null;
        }
        //System.gc();
        return bitmap;
    }

    public File createAndSaveAnaglyph(String timestamp, Bitmap leftBitmap, Bitmap rightBitmap) {
        Log.d(TAG, "createAndSaveAnaglyph");
        if (leftBitmap == null || rightBitmap == null) {
            Log.d(TAG, "createAndSaveAnaglyph failed Bitmaps null " + timestamp);
            return null;
        }

        Bitmap anaglyphBitmap = StereoImage.colorAnaglyph(leftBitmap, rightBitmap, parameters.getParallaxOffset(), parameters.getVerticalOffset());

        // Save anaglyph image
        String filename = timestamp + "_ana.jpg";
        File file = new File(Environment.getExternalStoragePublicDirectory(BASE_FOLDER + File.separator + SAVE_FOLDER + File.separator + SAVE_ANA_FOLDER), filename);

        try (FileOutputStream output = new FileOutputStream(file)) {
            anaglyphBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output);
            output.close();
            MediaScannerConnection.scanFile(context, new String[]{file.getAbsolutePath()},
                    new String[]{"image/jpeg"}, null);

            Log.d(TAG, "Anaglyph image saved: " + file.getAbsolutePath());
        } catch (IOException e) {
            Log.e(TAG, "Error saving anaglyph image", e);
            return null;
        }
        anaglyphBitmap.recycle();
        anaglyphBitmap = null;
        //System.gc();
        return file;
    }

    public File createAndSaveSBS(String timestamp, Bitmap leftBitmap, Bitmap rightBitmap) {
        Log.d(TAG, "createAndSaveSBS");
        if (leftBitmap == null || rightBitmap == null) {
            Log.d(TAG, "createAndSaveSBS failed Bitmaps null " + timestamp);
            return null;
        }

        Bitmap sbsBitmap = StereoImage.alignLR(leftBitmap, rightBitmap, parameters.getParallaxOffset(), parameters.getVerticalOffset());
        if (sbsBitmap == null) {
            Log.d(TAG, "createAndSaveSBS failed");
            return null;
        }

        // Save SBS image
        String filename = timestamp + "_2x1.jpg";
        File file = new File(Environment.getExternalStoragePublicDirectory(BASE_FOLDER + File.separator + SAVE_FOLDER), filename);

        try (FileOutputStream output = new FileOutputStream(file)) {
            sbsBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output);
            output.close();
            //sbsBitmap.compress(Bitmap.CompressFormat.PNG, 100, out);
            MediaScannerConnection.scanFile(context, new String[]{file.getAbsolutePath()},
                    new String[]{"image/*"}, null);

            Log.d(TAG, "SBS image saved: " + file.getAbsolutePath());
            if (((MainActivity) context).imageSender != null) {
                String imageUrl = "http://"+((MainActivity) context).hostIpAddr+":"+((MainActivity) context).hostPort+File.separator+filename;
                Log.d(TAG, "imageSender.sendImageUrl " + imageUrl);
                ((MainActivity) context).imageSender.sendImageUrl(imageUrl, parameters.getReceiverIp(), parameters.getReceiverPort());
            }

//            if (((MainActivity) context).imageUrlSender != null) {
//                String imageUrl = "http://"+((MainActivity) context).senderHost+":"+((MainActivity) context).senderPort+File.separator+filename;
//                Log.d(TAG, "imageUrlSender.discoverAndSend " + imageUrl);
//                ((MainActivity) context).imageUrlSender.discoverAndSend(imageUrl);
//            }
        } catch (IOException e) {
            Log.e(TAG, "Error saving SBS image", e);
            return null;
        }
        sbsBitmap.recycle();
        sbsBitmap = null;
        //System.gc();
        return file;
    }

    public void saveImageFiles(byte[] leftBytes, byte[] rightBytes) {
        Log.d(TAG, "saveImageFiles");
        timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());

        leftBitmap = saveImageFile(leftBytes, PHOTO_PREFIX + timestamp, true); // left
        rightBitmap = saveImageFile(rightBytes, PHOTO_PREFIX + timestamp, false); // right

        if (pApplet != null) {  // Processing sketch is present
            if (leftReview != null) {
                ((Bitmap)leftReview.getNative()).recycle();
            }
            if (rightReview != null) {
                ((Bitmap)rightReview.getNative()).recycle();
            }
            leftReview = pApplet.createImage(leftBitmap.getWidth(), leftBitmap.getHeight(), PImage.ARGB);
            rightReview = pApplet.createImage(rightBitmap.getWidth(), rightBitmap.getHeight(), PImage.ARGB);
            leftReview.setNative(leftBitmap);
            rightReview.setNative(rightBitmap);
            leftReview.loadPixels();
            leftReview.updatePixels();
            rightReview.loadPixels();
            rightReview.updatePixels();
        }

        if (aiVision != null) {
            String response = aiVision.getInformationFromImage(leftBitmap, aiVision.getPrompt());
            Log.d(TAG, "AI Vision response: " + response);
            Toast.makeText(context, "AI Vision response: " + response, Toast.LENGTH_SHORT).show();
        }
        // Save Anaglyph image and recycle Anaglyph bitmap
        if (saveAnaglyph) {
            reviewAnaglyph = createAndSaveAnaglyph(PHOTO_PREFIX + timestamp, leftBitmap, rightBitmap);
        }
        if (saveSBS) {
            int counter = ((MainActivity) context).getContinuousCounter();
            Log.d(TAG, "ContinuousCounter=" + counter);
            if (((MainActivity) context).getContinuousMode()) {
                timestamp += "_" + (((MainActivity) context).CONTINUOUS_COUNT - counter + 1);
            }
            // Save SBS image and keep file, and recycle SBS bitmap
            if (crossEye) {
                reviewSBS = createAndSaveSBS(PHOTO_PREFIX + timestamp, rightBitmap, leftBitmap);
            } else {
                reviewSBS = createAndSaveSBS(PHOTO_PREFIX + timestamp, leftBitmap, rightBitmap);
            }
            if (((MainActivity) context).getContinuousMode() && counter > 0) {
                ((MainActivity) context).nextContinuousCapturePhoto();
            }
        }
        //ToastHelper.showToast(context, "Saved " + timestamp);
        Toast.makeText(context, "Saved " + timestamp, Toast.LENGTH_LONG).show();
        if (pApplet != null) {
            ((PhotoBooth) pApplet).setReviewImages(leftReview, rightReview);
            if (((MainActivity) context).reviewPrint) {
                ((MainActivity) context).setReview();
            }
            else {
                ((MainActivity) context).setAiEditReview();
            }
        }
    }

    public void printImageType() {
        if (reviewSBS == null || reviewAnaglyph == null || reviewLeft == null || reviewRight == null) {
            Toast.makeText(context, "Nothing to Print", Toast.LENGTH_SHORT).show();
            return;
        }
        DisplayMode displayMode = ((MainActivity) context).getDisplayMode();
        if (displayMode == DisplayMode.SBS) {
            sharePrintImage(reviewSBS);
        } else if (displayMode == DisplayMode.ANAGLYPH) {
            sharePrintImage(reviewAnaglyph);
        } else if (displayMode == DisplayMode.LEFT){
            sharePrintImage(reviewLeft);
        } else {
            sharePrintImage(reviewRight);
        }

    }

    public File getMediaFile() {
        Log.d(TAG, "getMediaFile()");
        DisplayMode displayMode = ((MainActivity) context).getDisplayMode();
        if (displayMode == DisplayMode.SBS) {
            return reviewSBS;
        } else if (displayMode == DisplayMode.ANAGLYPH) {
            return reviewAnaglyph;
        } else if (displayMode == DisplayMode.LEFT){
            return reviewLeft;
        } else {
            return reviewRight;
        }

    }

    void reviewPhotos(DisplayMode displayMode) {
        Log.d(TAG, "reviewPhotos()");
        if (reviewSBS != null) {
            if (displayMode == DisplayMode.SBS) {
                shareImage2(reviewSBS, APP_REVIEW_PACKAGE);
            } else {
                shareImage2(reviewAnaglyph, APP_AIEDIT_PACKAGE);
            }
        } else {
            Toast.makeText(context, "Nothing to Review", Toast.LENGTH_SHORT).show();
        }
    }

    // For reference not used
    public void reviewImages() {
        Log.d(TAG, "reviewImages()");
        if (reviewSBS != null) {
            shareImage(reviewSBS);
        } else {
            Toast.makeText(context, "Photo Albums Available", Toast.LENGTH_SHORT).show();
            shareImage(reviewSBS);
        }
    }

    public void shareImage(File imageFile) {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        ((MainActivity) context).photoPickerLauncher.launch(intent);
    }

    private Uri getContentUriForFile(String filePath) {
        File file = new File(filePath);
        if (!file.exists()) {
            return null;
        }

        ContentResolver resolver = context.getContentResolver();

        // First try to find existing MediaStore entry
        String[] projection = {MediaStore.Images.Media._ID};
        String selection = MediaStore.Images.Media.DATA + "=?";
        String[] selectionArgs = {file.getAbsolutePath()};

        Cursor cursor = resolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                selectionArgs,
                null
        );

        if (cursor != null && cursor.moveToFirst()) {
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
            long id = cursor.getLong(idColumn);
            cursor.close();
            return ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
        }

        if (cursor != null) {
            cursor.close();
        }

        // If not found, add to MediaStore
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, file.getName());
        contentValues.put(MediaStore.Images.Media.MIME_TYPE, getMimeType(file.getName()));
        //contentValues.put(MediaStore.Images.Media.DATA, file.getAbsolutePath());
        contentValues.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis() / 1000);

        // Use RELATIVE_PATH to tell the system where to put the file in the MediaStore
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            contentValues.put(MediaStore.Images.Media.RELATIVE_PATH, SAVE_FOLDER + File.separator + SAVE_AI_EDIT_FOLDER);
        }

        return resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);
    }

    private String getMimeType(String fileName) {
        if (fileName.toLowerCase().endsWith(".jpg") || fileName.toLowerCase().endsWith(".jpeg")) {
            return "image/jpeg";
        } else if (fileName.toLowerCase().endsWith(".png")) {
            return "image/png";
        }
        return "image/*";
    }

    public boolean shareReviewImage() {
        if (reviewSBS == null) return false;
        return shareImage2(reviewSBS, null);
    }

//    public boolean shareImage2_old2(File imageFile, String appPackage) {
//        boolean success = false;
//        if (imageFile == null || !imageFile.exists()) {
//            Log.d(TAG, "shareImage2 null or invalid imageFile");
//            return success;
//        }
//
//        // Resize the image
//        File resizedImageFile = resizeImage(imageFile, 1080);
//        if (resizedImageFile == null) {
//            Log.e(TAG, "Failed to resize image");
//            return false;
//        }
//
//        success = true;
//        Log.d(TAG, "shareImage2 " + resizedImageFile.getAbsolutePath());
//        Uri contentUri = getContentUriForFile(resizedImageFile.getPath());
//
//        if (contentUri != null) {
//            try {
//                Intent intent = new Intent(Intent.ACTION_SEND);
//                intent.putExtra(Intent.EXTRA_STREAM, contentUri);
//                intent.setType("image/*");
//                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//                if (appPackage != null) {
//                    intent.setPackage(appPackage);
//                }
//                context.startActivity(intent);
//            } catch (ActivityNotFoundException e) {
//                Log.d(TAG, "Failed to launch target app.");
//                Toast.makeText(context, appPackage + " not installed", Toast.LENGTH_SHORT).show();
//                Intent intent = new Intent(Intent.ACTION_SEND);
//                intent.putExtra(Intent.EXTRA_STREAM, contentUri);
//                intent.setType("image/*");
//                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//                context.startActivity(Intent.createChooser(intent, "Share image with:"));
//            }
//        } else {
//            Log.e(TAG, "Failed to create MediaStore entry.");
//            success = false;
//        }
//
//        // Clean up temporary resized file
//        if (resizedImageFile != null && resizedImageFile.exists()) {
//            resizedImageFile.delete();
//        }
//
//        return success;
//    }

//    private File resizeImage(File originalFile, int maxHeight) {
//        try {
//            // Decode image bounds
//            BitmapFactory.Options options = new BitmapFactory.Options();
//            options.inJustDecodeBounds = true;
//            BitmapFactory.decodeFile(originalFile.getAbsolutePath(), options);
//
//            int originalWidth = options.outWidth;
//            int originalHeight = options.outHeight;
//
//            // Calculate new dimensions maintaining aspect ratio
//            int newHeight = maxHeight;
//            int newWidth = (int) ((float) originalWidth * (float) maxHeight / (float) originalHeight);
//
//            // Decode with sample size for performance
//            options.inJustDecodeBounds = false;
//            options.inSampleSize = calculateInSampleSize(options, newWidth, newHeight);
//
//            Bitmap bitmap = BitmapFactory.decodeFile(originalFile.getAbsolutePath(), options);
//            Bitmap resizedBitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
//
//            // Save to temporary file
//            File tempFile = File.createTempFile("", ".jpg");
//            FileOutputStream fos = new FileOutputStream(tempFile);
//            resizedBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, fos);
//            fos.close();
//
//            bitmap.recycle();
//            resizedBitmap.recycle();
//
//            return tempFile;
//        } catch (IOException e) {
//            Log.e(TAG, "Error resizing image", e);
//            return null;
//        }
//    }

    public boolean shareImage2(File imageFile, String appPackage) {
        if (imageFile == null || !imageFile.exists()) return false;
        Log.d(TAG, "shareImage2 " + imageFile.getAbsolutePath() + " appPackage=" + appPackage);
        // 1. Get the Uri from MediaStore first
        Uri contentUri = createMediaStoreUri("" + imageFile.getName());
        Log.d(TAG, "shareImage2 imageFile=" + imageFile.getName());
        if (contentUri != null) {
            try {
                // 2. Resize and Write directly to the Uri
                boolean saved = resizeAndSaveToUri(imageFile, contentUri, 1080);
                if (!saved) return false;

                // 3. Proceed with Intent
                Intent intent = new Intent(Intent.ACTION_SEND);
                intent.putExtra(Intent.EXTRA_STREAM, contentUri);
                intent.putExtra(Intent.EXTRA_TEXT, imageFile.getName());
                intent.setType("image/jpeg");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                if (appPackage != null) {
                    intent.setPackage(appPackage);
                }
                context.startActivity(intent);
                return true;
            } catch (Exception e) {
                Log.e(TAG, "Sharing failed", e);
            }
        }
        return false;
    }

    private Uri createMediaStoreUri(String fileName) {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, fileName);
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, BASE_FOLDER + File.separator + SAVE_FOLDER + File.separator + SAVE_AI_EDIT_FOLDER);
            values.put(MediaStore.Images.Media.IS_PENDING, 1);
        }

        return context.getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
    }

    private boolean resizeAndSaveToUri(File sourceFile, Uri destUri, int maxHeight) {
        try {
            // ... (Keep your existing resizing logic to get a Bitmap) ...
            BitmapFactory.Options options = new BitmapFactory.Options();
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(sourceFile.getAbsolutePath(), options);
            options.inSampleSize = calculateInSampleSize(options, -1, maxHeight);
            options.inJustDecodeBounds = false;
            Bitmap bitmap = BitmapFactory.decodeFile(sourceFile.getAbsolutePath(), options);

            // Write to the MediaStore Uri
            try (OutputStream os = context.getContentResolver().openOutputStream(destUri)) {
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, os);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                ContentValues values = new ContentValues();
                values.put(MediaStore.Images.Media.IS_PENDING, 0);
                context.getContentResolver().update(destUri, values, null, null);
            }

            bitmap.recycle();
            bitmap = null;
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error writing to MediaStore", e);
            return false;
        }
    }
    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

//    public boolean shareImage2_old(File imageFile, String appPackage) {
//        boolean success = false;
//        if (imageFile == null) {
//            Log.d(TAG, "shareImage2 null imageFile");
//            return success;
//        }
//        success = true;
//        Log.d(TAG, "shareImage2 " + imageFile.getAbsolutePath());
//        Uri contentUri = getContentUriForFile(imageFile.getPath());
//
//        if (contentUri != null) {
//            try {
//                Intent intent = new Intent(Intent.ACTION_SEND);
//                intent.putExtra(Intent.EXTRA_STREAM, contentUri);
//                intent.setType("image/*");
//                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//                if (appPackage != null) {
//                    intent.setPackage(appPackage); //  actual package name
//                }
//                context.startActivity(intent);
//            } catch (ActivityNotFoundException e) {
//                // Handle the case where the target app is not installed
//                Log.d(TAG, "Failed to launch 3DSteroid Pro.");
//                Toast.makeText(context, appPackage + " not installed", Toast.LENGTH_SHORT).show();
//                // Toast message or direct the user to the Play Store
//                Intent intent = new Intent(Intent.ACTION_SEND);
//                intent.putExtra(Intent.EXTRA_STREAM, contentUri);
//                intent.setType("image/*");
//                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
//                context.startActivity(Intent.createChooser(intent, "Share image with:"));
//            }
//        } else {
//            Log.e(TAG, "Failed to create MediaStore entry.");
//            success = false;
//        }
//        return success;
//    }

    // For reference not used
    public void shareImage1(File imageFile) {
        findIntent();
        Uri contentUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", imageFile);
        Log.d(TAG, "shareImage1 " + imageFile.getAbsolutePath());
        if (contentUri != null) {

            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("image/*");
            shareIntent.putExtra(Intent.EXTRA_STREAM, contentUri);
            shareIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION); // Required for receiving app
            shareIntent.setPackage(APP_REVIEW_PACKAGE); // Replace with the actual package name

            try {
                context.startActivity(shareIntent);
            } catch (ActivityNotFoundException e) {
                // Handle the case where the target app is not installed
                Log.d(TAG, "Failed to launch stereoroidpro.");
                Toast.makeText(context, "StereoRoidPro not installed", Toast.LENGTH_SHORT).show();
                // Toast message or direct the user to the Play Store
            }
        }
    }

    // For reference not used */
    public void shareImages(File imageFile) {
        //Uri contentUri = FileProvider.getUriForFile(this, this.getPackageName() + ".fileprovider", imageFile);
        ContentResolver resolver = context.getContentResolver();
        ContentValues contentValues = new ContentValues();
        try {
            contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, imageFile.getCanonicalPath());
        } catch (IOException e) {
            contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, imageFile.getPath());
        }
        String fileName = imageFile.getName();
        String path = imageFile.getPath();
        String mimeType = "image/*";

        contentValues.put(MediaStore.Images.Media.MIME_TYPE, mimeType);
        Log.d(TAG, "Filename: " + fileName + " Path=" + path);
        Uri contentUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        if (contentUri != null) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_STREAM, contentUri);
            intent.setType(mimeType); // Or the correct MIME type
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            context.startActivity(Intent.createChooser(intent, "Share using:")); // Allows the user to select the print service
            //this.startActivity(intent); // SEND only
        } else {
            // Handle the case where the insertion failed
            Log.d(TAG, "Failed to share image");
        }
    }

    public void sharePrintImage(File imageFile) {
        Uri contentUri = FileProvider.getUriForFile(context, context.getPackageName() + ".fileprovider", imageFile);
        ContentResolver resolver = context.getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.Images.Media.DISPLAY_NAME, imageFile.getName());

        String fileName = imageFile.getName();
        String mimeType;

        if (fileName.toLowerCase().endsWith(".png")) {
            mimeType = "image/png";
        } else if (fileName.toLowerCase().endsWith(".jpg") || fileName.toLowerCase().endsWith(".jpeg")) {
            mimeType = "image/jpeg";
        } else if (fileName.toLowerCase().endsWith(".gif")) {
            mimeType = "image/gif";
        } else {
            Log.d(TAG, "Unsupported image format: " + fileName);
            return; // Exit if the format is not supported
        }

        contentValues.put(MediaStore.Images.Media.MIME_TYPE, mimeType); // Or the correct MIME type
        //Uri contentUri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues);

        if (contentUri != null) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.putExtra(Intent.EXTRA_STREAM, contentUri);
            intent.setType(mimeType); // Or the correct MIME type
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.setPackage(APP_CANON_PRINT_SERVICE_PACKAGE); // Printer app actual package name
            context.startActivity(Intent.createChooser(intent, "Print using:")); // Allows the user to select the print service
        } else {
            // Handle the case where the insertion failed
            Log.d(TAG, "Failed to share image");
        }
    }

    public void findIntent() {
        // First, find what activities can handle image sharing
        PackageManager pm = context.getPackageManager();
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("image/*");

        List<ResolveInfo> activities = pm.queryIntentActivities(shareIntent, 0);
        for (ResolveInfo activity : activities) {
            String appName = activity.loadLabel(pm).toString();
            String packageName = activity.activityInfo.packageName;
            Log.d(TAG, "App: " + appName + ", Package: " + packageName);

            if (appName.toLowerCase().contains("stereo") ||
                    packageName.toLowerCase().contains("stereo")) {
                Log.d(TAG, "StereoRoidPro: " + packageName + " / " + activity.activityInfo.name);
            }
        }
    }

}
