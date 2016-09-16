/*******************************************************************************
 * Copyright 2016 Anton Bevza stfalcon.com
 * <p/>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p/>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p/>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *******************************************************************************/

package com.stfalcon.contentmanager;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

public class ContentManager {
    private final static int PERMISSION_REQUEST_CODE = 333;

    /**
     * For save and restore instance state
     */
    private static final String DATE_CAMERA_INTENT_STARTED_STATE = "com.stfalcon.contentmanager.ContentManager.dateCameraIntentStarted";
    private static final String CAMERA_PIC_URI_STATE = "com.stfalcon.contentmanager.ContentManager.CAMERA_PIC_URI_STATE";
    private static final String PHOTO_URI_STATE = "com.stfalcon.contentmanager.ContentManager.PHOTO_URI_STATE";
    private static final String ROTATE_X_DEGREES_STATE = "com.stfalcon.contentmanager.ContentManager.ROTATE_X_DEGREES_STATE";
    /**
     * Request codes
     */
    private static final int CONTENT_PICKER = 15; // request codes
    private static final int CONTENT_TAKE = 16; //
    /**
     * Date and time the camera intent was started.
     */
    private Date dateCameraIntentStarted = null;
    /**
     * Default location where we want the photo to be ideally stored.
     */
    private Uri preDefinedCameraUri = null;

    private Uri photoUri = null;
    /**
     * Potential 3rd location of photo data.
     */
    private Uri photoUriIn3rdLocation = null;
    /**
     * Orientation of the retrieved photo.
     */
    private int rotateXDegrees = 0;
    /**
     * Result target file
     */
    private File targetFile;
    /**
     * Result callback
     */
    private PickContentListener pickContentListener;
    /**
     * For monitor the load process
     */
    private Handler handler;
    private int progressPercent = 0;
    /**
     * Activity, fragment
     */
    private Activity activity;
    private Fragment fragment;

    private int savedTask;
    private Content savedContent;

    public ContentManager(Activity activity, PickContentListener pickContentListener) {
        this.activity = activity;
        this.pickContentListener = pickContentListener;
        handler = new Handler();
    }

    public ContentManager(Activity activity, PickContentListener pickContentListener, Fragment fragment) {
        this(activity, pickContentListener);
        this.fragment = fragment;
    }

    /**
     * Need to call in onSaveInstanceState method of activity
     */
    public void onSaveInstanceState(Bundle savedInstanceState) {
        if (dateCameraIntentStarted != null) {
            savedInstanceState.putString(DATE_CAMERA_INTENT_STARTED_STATE, dateToString(dateCameraIntentStarted));
        }
        if (preDefinedCameraUri != null) {
            savedInstanceState.putString(CAMERA_PIC_URI_STATE, preDefinedCameraUri.toString());
        }
        if (photoUri != null) {
            savedInstanceState.putString(PHOTO_URI_STATE, photoUri.toString());
        }
        savedInstanceState.putInt(ROTATE_X_DEGREES_STATE, rotateXDegrees);
    }

    /**
     * Call to reinitialize the helpers instance state.
     * Need to call in onRestoreInstanceState method of activity
     */
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(DATE_CAMERA_INTENT_STARTED_STATE)) {
                dateCameraIntentStarted = stringToDate(savedInstanceState.getString(DATE_CAMERA_INTENT_STARTED_STATE));
            }
            if (savedInstanceState.containsKey(CAMERA_PIC_URI_STATE)) {
                preDefinedCameraUri = Uri.parse(savedInstanceState.getString(CAMERA_PIC_URI_STATE));
            }
            if (savedInstanceState.containsKey(PHOTO_URI_STATE)) {
                photoUri = Uri.parse(savedInstanceState.getString(PHOTO_URI_STATE));
            }
            if (savedInstanceState.containsKey(ROTATE_X_DEGREES_STATE)) {
                rotateXDegrees = savedInstanceState.getInt(ROTATE_X_DEGREES_STATE);
            }
        }
    }

    /**
     * Need to call in onActivityResult method of activity or fragment
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CONTENT_PICKER) {
            if (resultCode == Activity.RESULT_OK) {
                handleContentData(data);
            } else {
                pickContentListener.onCanceled();
            }
        }
        if (requestCode == CONTENT_TAKE) {
            onCameraIntentResult(requestCode, resultCode, data);
        }
    }

    /**
     * Pick image or video content from storage or google acc
     *
     * @param content image or video
     */
    public void pickContent(Content content) {
        savedTask = CONTENT_PICKER;
        savedContent = content;
        if (isStoragePermissionGranted(activity, fragment)) {
            this.targetFile = createFile(content);
            if (Build.VERSION.SDK_INT < 19) {
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK);
                photoPickerIntent.setType(content.toString());
                if (fragment == null) {
                    activity.startActivityForResult(photoPickerIntent, CONTENT_PICKER);
                } else {
                    fragment.startActivityForResult(photoPickerIntent, CONTENT_PICKER);
                }
            } else {
                Intent photoPickerIntent = new Intent(Intent.ACTION_GET_CONTENT);
                photoPickerIntent.setType(content.toString());
                photoPickerIntent.addCategory(Intent.CATEGORY_OPENABLE);
                if (photoPickerIntent.resolveActivity(activity.getPackageManager()) != null) {
                    if (fragment == null) {
                        activity.startActivityForResult(photoPickerIntent, CONTENT_PICKER);
                    } else {
                        fragment.startActivityForResult(photoPickerIntent, CONTENT_PICKER);
                    }
                }
            }
        }
    }

    public void takePhoto() {
        savedTask = CONTENT_TAKE;
        if (isStoragePermissionGranted(activity, fragment)) {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                try {
                    boolean setPreDefinedCameraUri = isSetPreDefinedCameraUri();

                    dateCameraIntentStarted = new Date();
                    Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    if (setPreDefinedCameraUri) {
                        String filename = System.currentTimeMillis() + ".jpg";
                        ContentValues values = new ContentValues();
                        values.put(MediaStore.Images.Media.TITLE, filename);

                        preDefinedCameraUri = activity.getContentResolver().insert(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                values);
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, preDefinedCameraUri);
                    }
                    if (fragment == null) {
                        activity.startActivityForResult(intent, CONTENT_TAKE);
                    } else {
                        fragment.startActivityForResult(intent, CONTENT_TAKE);
                    }
                } catch (ActivityNotFoundException e) {
                    pickContentListener.onError("");
                }
            } else {
                pickContentListener.onError("");
            }
        }
    }


    /**
     * Check device model and return is need to set predefined camera uri
     */
    private boolean isSetPreDefinedCameraUri() {
        boolean setPreDefinedCameraUri = false;

        // NOTE: Do NOT SET: intent.putExtra(MediaStore.EXTRA_OUTPUT, cameraPicUri)
        // on Samsung Galaxy S2/S3/.. for the following reasons:
        // 1.) it will break the correct picture orientation
        // 2.) the photo will be stored in two locations (the given path and, additionally, in the MediaStore)
        String manufacturer = Build.MANUFACTURER.toLowerCase(Locale.ENGLISH);
        String model = Build.MODEL.toLowerCase(Locale.ENGLISH);
        String buildType = Build.TYPE.toLowerCase(Locale.ENGLISH);
        String buildDevice = Build.DEVICE.toLowerCase(Locale.ENGLISH);
        String buildId = Build.ID.toLowerCase(Locale.ENGLISH);
//				String sdkVersion = android.os.Build.VERSION.RELEASE.toLowerCase(Locale.ENGLISH);

        if (!(manufacturer.contains("samsung")) && !(manufacturer.contains("sony"))) {
            setPreDefinedCameraUri = true;
        }
        if (manufacturer.contains("samsung") && model.contains("galaxy nexus")) { //TESTED
            setPreDefinedCameraUri = true;
        }
        if (manufacturer.contains("samsung") && model.contains("gt-n7000") && buildId.contains("imm76l")) { //TESTED
            setPreDefinedCameraUri = true;
        }

        if (buildType.contains("userdebug") && buildDevice.contains("ariesve")) {  //TESTED
            setPreDefinedCameraUri = true;
        }
        if (buildType.contains("userdebug") && buildDevice.contains("crespo")) {   //TESTED
            setPreDefinedCameraUri = true;
        }
        if (buildType.contains("userdebug") && buildDevice.contains("gt-i9100")) { //TESTED
            setPreDefinedCameraUri = true;
        }

        ///////////////////////////////////////////////////////////////////////////
        // TEST
        if (manufacturer.contains("samsung") && model.contains("sgh-t999l")) { //T-Mobile LTE enabled Samsung S3
            setPreDefinedCameraUri = true;
        }
        if (buildDevice.contains("cooper")) {
            setPreDefinedCameraUri = true;
        }
        if (buildType.contains("userdebug") && buildDevice.contains("t0lte")) {
            setPreDefinedCameraUri = true;
        }
        if (buildType.contains("userdebug") && buildDevice.contains("kot49h")) {
            setPreDefinedCameraUri = true;
        }
        if (buildType.contains("userdebug") && buildDevice.contains("t03g")) {
            setPreDefinedCameraUri = true;
        }
        if (buildType.contains("userdebug") && buildDevice.contains("gt-i9300")) {
            setPreDefinedCameraUri = true;
        }
        if (buildType.contains("userdebug") && buildDevice.contains("gt-i9195")) {
            setPreDefinedCameraUri = true;
        }
        if (buildType.contains("userdebug") && buildDevice.contains("xperia u")) {
            setPreDefinedCameraUri = true;
        }

        ///////////////////////////////////////////////////////////////////////////
        return setPreDefinedCameraUri;
    }

    /**
     * Process result of camera intent
     */
    private void onCameraIntentResult(int requestCode, int resultCode, Intent intent) {
        if (resultCode == Activity.RESULT_OK) {
            Cursor myCursor = null;
            Date dateOfPicture = null;
            try {
                // Create a Cursor to obtain the file Path for the large image
                String[] largeFileProjection = {MediaStore.Images.ImageColumns._ID,
                        MediaStore.Images.ImageColumns.DATA,
                        MediaStore.Images.ImageColumns.ORIENTATION,
                        MediaStore.Images.ImageColumns.DATE_TAKEN};
                String largeFileSort = MediaStore.Images.ImageColumns._ID + " DESC";
                myCursor = activity.getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        largeFileProjection,
                        null, null,
                        largeFileSort);
                myCursor.moveToFirst();
                if (!myCursor.isAfterLast()) {
                    // This will actually give you the file path location of the image.
                    String largeImagePath = myCursor.getString(myCursor
                            .getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATA));
                    photoUri = Uri.fromFile(new File(largeImagePath));
                    if (photoUri != null) {
                        dateOfPicture = new Date(myCursor.getLong(myCursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATE_TAKEN)));
                        if (dateOfPicture != null && dateOfPicture.after(dateCameraIntentStarted)) {
                            rotateXDegrees = myCursor.getInt(myCursor
                                    .getColumnIndexOrThrow(MediaStore.Images.ImageColumns.ORIENTATION));
                        } else {
                            photoUri = null;
                        }
                    }
                    if (myCursor.moveToNext() && !myCursor.isAfterLast()) {
                        String largeImagePath3rdLocation = myCursor.getString(myCursor
                                .getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATA));
                        Date dateOfPicture3rdLocation = new Date(myCursor.getLong(myCursor.getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATE_TAKEN)));
                        if (dateOfPicture3rdLocation != null && dateOfPicture3rdLocation.after(dateCameraIntentStarted)) {
                            photoUriIn3rdLocation = Uri.fromFile(new File(largeImagePath3rdLocation));
                        }
                    }
                }
            } catch (Exception e) {
            } finally {
                if (myCursor != null && !myCursor.isClosed()) {
                    myCursor.close();
                }
            }

            if (photoUri == null) {
                try {
                    photoUri = intent.getData();
                } catch (Exception e) {
                }
            }

            if (photoUri == null) {
                photoUri = preDefinedCameraUri;
            }

            try {
                if (photoUri != null && new File(photoUri.getPath()).length() <= 0) {
                    if (preDefinedCameraUri != null) {
                        Uri tempUri = photoUri;
                        photoUri = preDefinedCameraUri;
                        preDefinedCameraUri = tempUri;
                    }
                }
            } catch (Exception e) {
            }

            photoUri = getFileUriFromContentUri(photoUri);
            preDefinedCameraUri = getFileUriFromContentUri(preDefinedCameraUri);
            try {
                if (photoUriIn3rdLocation != null) {
                    if (photoUriIn3rdLocation.equals(photoUri) || photoUriIn3rdLocation.equals(preDefinedCameraUri)) {
                        photoUriIn3rdLocation = null;
                    } else {
                        photoUriIn3rdLocation = getFileUriFromContentUri(photoUriIn3rdLocation);
                    }
                }
            } catch (Exception e) {
            }

            if (photoUri != null) {
                pickContentListener.onContentLoaded(photoUri, "image");
            } else {
                pickContentListener.onError("");
            }
        } else if (resultCode == Activity.RESULT_CANCELED) {
            pickContentListener.onCanceled();
        } else {
            pickContentListener.onCanceled();
        }
    }

    /**
     * Async load content data
     *
     * @param data result intent
     */
    private void handleContentData(final Intent data) {
        if (data != null) {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        Uri contentVideoUri = data.getData();

                        consumeProgress();

                        FileInputStream in = (FileInputStream) activity.getContentResolver().openInputStream(contentVideoUri);
                        FileOutputStream out = new FileOutputStream(targetFile);
                        FileChannel inChannel = in.getChannel();
                        FileChannel outChannel = out.getChannel();
                        inChannel.transferTo(0, inChannel.size(), outChannel);

                        in.close();
                        out.close();

                        ContentResolver contentResolver = activity.getContentResolver();
                        final String contentType = contentResolver.getType(contentVideoUri);

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                pickContentListener.onContentLoaded(Uri.fromFile(targetFile), savedContent.toString());
                            }
                        });
                    } catch (final Exception e) {
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                pickContentListener.onError(e.getMessage());
                            }
                        });
                    }
                }
            }).start();
        } else {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    pickContentListener.onError("Data null");
                }
            });
        }
    }

    /**
     * For showing load content progress
     */
    private void consumeProgress() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                progressPercent++;
                pickContentListener.onLoadContentProgress(
                        progressPercent);
                if (progressPercent < 100) consumeProgress();
            }
        }, 500);
    }

    /**
     * Create image file in directory of pictures
     *
     * @param content
     * @return
     */
    public static File createFile(Content content) {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String type = content.equals(Content.IMAGE) ? ".jpg" : ".mp4";
        String imageFileName = "IMAGE_" + timeStamp + "_";
        File storageDir = Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_PICTURES);
        File image = null;
        try {
            image = File.createTempFile(
                    imageFileName,  /* prefix */
                    type,         /* suffix */
                    storageDir      /* directory */
            );
        } catch (IOException e) {
            e.printStackTrace();
        }

        return image;
    }

    private Uri getFileUriFromContentUri(Uri cameraPicUri) {
        Cursor cursor = null;
        try {
            if (cameraPicUri != null
                    && cameraPicUri.toString().startsWith("content")) {
                String[] proj = {MediaStore.Images.Media.DATA};
                cursor = activity.getContentResolver().query(cameraPicUri, proj, null, null, null);
                cursor.moveToFirst();
                // This will actually give you the file path location of the image.
                String largeImagePath = cursor.getString(cursor
                        .getColumnIndexOrThrow(MediaStore.Images.ImageColumns.DATA));
                return Uri.fromFile(new File(largeImagePath));
            }
            return cameraPicUri;
        } catch (Exception e) {
            return cameraPicUri;
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }

    /**
     * Result callback
     */
    public interface PickContentListener {
        void onContentLoaded(Uri uri, String contentType);

        void onLoadContentProgress(int loadPercent);

        void onError(String error);

        void onCanceled();
    }

    /**
     * Content type
     */
    public enum Content {
        VIDEO("video/*"),
        IMAGE("image/*");

        private final String text;

        private Content(final String text) {
            this.text = text;
        }

        @Override
        public String toString() {
            return text;
        }
    }

    /**
     * File name date format
     */
    public final static String dateFormat = "yyyy-MM-dd HH:mm:ss.SSSZ";

    public final static TimeZone utc = TimeZone.getTimeZone("UTC");

    /**
     * Converts a Date object to a string representation.
     *
     * @param date
     * @return date as String
     */
    public static String dateToString(Date date) {
        if (date == null) {
            return null;
        } else {
            DateFormat df = new SimpleDateFormat(dateFormat);
            df.setTimeZone(utc);
            return df.format(date);
        }
    }

    /**
     * Converts a string representation of a date to its respective Date object.
     *
     * @param dateAsString
     * @return Date
     */
    public static Date stringToDate(String dateAsString) {
        try {
            DateFormat df = new SimpleDateFormat(dateFormat);
            df.setTimeZone(utc);
            return df.parse(dateAsString);
        } catch (ParseException e) {
            return null;
        } catch (NullPointerException e) {
            return null;
        }
    }


    /**
     * Some devices return wrong rotated image so we can fix it by this method
     */
    public static void fixImageRatation(Uri uri, Bitmap realImage) {
        File pictureFile = new File(uri.getPath());

        try {
            FileOutputStream fos = new FileOutputStream(pictureFile);
            ExifInterface exif = new ExifInterface(pictureFile.toString());

            if (exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("6")) {
                realImage = rotate(realImage, 90);
            } else if (exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("8")) {
                realImage = rotate(realImage, 270);
            } else if (exif.getAttribute(ExifInterface.TAG_ORIENTATION).equalsIgnoreCase("3")) {
                realImage = rotate(realImage, 180);
            }

            boolean bo = realImage.compress(Bitmap.CompressFormat.JPEG, 100, fos);

            fos.close();

            Log.d("Info", bo + "");

        } catch (FileNotFoundException e) {
            Log.d("Info", "File not found: " + e.getMessage());
        } catch (IOException e) {
            Log.d("TAG", "Error accessing file: " + e.getMessage());
        }
    }

    public static Bitmap rotate(Bitmap bitmap, int degree) {
        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        Matrix mtx = new Matrix();
        mtx.postRotate(degree);

        return Bitmap.createBitmap(bitmap, 0, 0, w, h, mtx, true);
    }

    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0) {
                if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    switch (savedTask) {
                        case CONTENT_PICKER:
                            pickContent(savedContent);
                            break;
                        case CONTENT_TAKE:
                            takePhoto();
                            break;
                    }
                }
            }
        }
    }

    //For fragments
    public static boolean isStoragePermissionGranted(Activity activity, Fragment fragment) {
        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(activity, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                if (fragment == null) {
                    ActivityCompat.requestPermissions(activity,
                            new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
                } else {
                    fragment.requestPermissions(
                            new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
                }
                return false;
            }
        } else {
            return true;
        }
    }
}
