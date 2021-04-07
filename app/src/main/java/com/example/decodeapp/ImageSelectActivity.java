package com.example.decodeapp;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.provider.MediaStore;
import android.view.View;
import android.view.Window;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;

import com.imnjh.imagepicker.SImagePicker;
import com.imnjh.imagepicker.activity.PhotoPickerActivity;
import com.imnjh.imagepicker.util.ImageUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;

/**
 * Created by harishhu on 2017/5/22.
 */

public class ImageSelectActivity extends AppCompatActivity{
    public static final String RETURN_DATA_FILE = "file";
    public static final String KEY_NEED_CROP = "crop";

    public static final String KEY_CROP_WIDTH = "crop_image_width";
    public static final String KEY_NEED_CAMERA = "needCamera";
    public static final String KEY_CROP_HEIGHT = "crop_image_height";
    public static final String KEY_IMAGE_MAX_COUNT = "image_max_count";

    private static final int DEFAULT_IMAGE_MAX_COUNT = 1;

    private static final int DEFAULT_WIDTH = 700;
    private static final int DEFAULT_HEIGHT = 700;

    private static final int RESULT_CAMERA_ONLY = 100;
    private static final int RESULT_SELECT_PHOTO = 101;
    private static final int RESULT_CAMERA_CROP_PATH_RESULT = 102;


    private String cropPath;
    private String cameraPhotoPath;

    private int width = 0;
    private int height = 0;


    boolean needCrop = false;
    boolean showCamera = true;

    static int photoIndex = 0;
    private int maxCount;

    private static final int MAX_CACHE_NUM = 20;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        super.onCreate(savedInstanceState);

        String action = getIntent().getAction();

        needCrop = getIntent().getBooleanExtra(KEY_NEED_CROP, false);
        showCamera = getIntent().getBooleanExtra(KEY_NEED_CAMERA, true);
        cameraPhotoPath = getIntent().getStringExtra("path");
        maxCount = getIntent().getIntExtra(KEY_IMAGE_MAX_COUNT, DEFAULT_IMAGE_MAX_COUNT);
        if (maxCount <= 0) {
            maxCount = DEFAULT_IMAGE_MAX_COUNT;
        }
        width = getIntent().getIntExtra(KEY_CROP_WIDTH, DEFAULT_WIDTH);
        height = getIntent().getIntExtra(KEY_CROP_HEIGHT, DEFAULT_HEIGHT);

        String cacheDir = getExternalCacheDir().getAbsolutePath();
        cropPath = cacheDir + File.separator + "crop_" + System.currentTimeMillis() + ".jpg";
        if(null == cameraPhotoPath) {
            cameraPhotoPath = cacheDir + File.separator + "photo_" + System.currentTimeMillis() + ".jpg";
        }
        photoIndex++;

        if (photoIndex > MAX_CACHE_NUM){
            photoIndex = 0;
        }
        if (action.equals(AppConstant.ACTION_IMAGE_GET_CAPTURE)){
            takePhotos();
        }else if (action.equals(AppConstant.ACTION_IMAGE_GET_GALLERY)||action.equals(AppConstant.ACTION_IMAGE_GET_ALL)){
                    showImagePicker();
        }
    }

    public void takePhotos() {
        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        Uri imageUri = getFileProviderUri(Uri.fromFile(new File(cameraPhotoPath)));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, imageUri);

        startActivityForResult(intent, RESULT_CAMERA_ONLY);
    }

    private Uri getFileProviderUri(Uri uri){
        if (uri.toString().startsWith("file:") && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N){
            try {
                uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", new File(new URI(uri.toString())));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        return uri;
    }


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode != Activity.RESULT_OK){
            finish();
            return;
        }

        File file;
        switch (requestCode) {
            case RESULT_CAMERA_ONLY:
                final ArrayList<String> pathList = new ArrayList<>(1);
                pathList.add(cameraPhotoPath);
                rotatePicture(pathList);
                if (!needCrop){
                    setMyResult(pathList);
                }else{
                    file = new File(pathList.get(0));
                    Uri imageUri = Uri.fromFile(file);
                    cropImg(imageUri);
                }
                break;
            case RESULT_CAMERA_CROP_PATH_RESULT:
                final ArrayList<String> pathList1 = new ArrayList<>(1);
                pathList1.add(cropPath);
                rotatePicture(pathList1);

                setMyResult(pathList1);
                break;
            case RESULT_SELECT_PHOTO:
                final ArrayList<String> pathList2 = data.getStringArrayListExtra(PhotoPickerActivity.EXTRA_RESULT_SELECTION);
                rotatePicture(pathList2);
                setMyResult(pathList2);

                break;
        }
    }

    public  File saveBitmap2File(Bitmap bitmap, String path) {
        File file = new File(path);
        FileOutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
            outputStream.flush();
            outputStream.close();
            return file;
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return null;
    }
    private void rotatePicture(ArrayList<String> pathlist){
        int degree = getBitmapDegree(pathlist.get(0));
        if (degree!=0){
            Bitmap bitmap =   rotateBitmapByDegree(BitmapFactory.decodeFile(pathlist.get(0)),degree);
            String path = getExternalCacheDir().getAbsolutePath()+ File.separator + "photo_rotate" + System.currentTimeMillis() + ".jpg";
            saveBitmap2File(bitmap,path);
            pathlist.clear();
            pathlist.add(path);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState, PersistableBundle outPersistentState) {
        super.onSaveInstanceState(outState, outPersistentState);
    }

    /**
     * 读取图片的旋转的角度
     *
     * @param path
     *            图片绝对路径
     * @return 图片的旋转角度
     */
    private int getBitmapDegree(String path) {
        int degree = 0;
        try {
            // 从指定路径下读取图片，并获取其EXIF信息
            ExifInterface exifInterface = new ExifInterface(path);
            // 获取图片的旋转信息
            int orientation = exifInterface.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    degree = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    degree = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    degree = 270;
                    break;
            }
        } catch ( IOException e) {
            e.printStackTrace();
        }
        return degree;
    }
    /**
     * 将图片按照某个角度进行旋转
     *
     * @param bm
     *            需要旋转的图片
     * @param degree
     *            旋转角度
     * @return 旋转后的图片
     */
    public static Bitmap rotateBitmapByDegree(Bitmap bm, int degree) {
        Bitmap returnBm = null;

        // 根据旋转角度，生成旋转矩阵
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        try {
            // 将原始图片按照旋转矩阵进行旋转，并得到新的图片
            returnBm = Bitmap.createBitmap(bm, 0, 0, bm.getWidth(), bm.getHeight(), matrix, true);
        } catch (OutOfMemoryError e) {
        }
        if (returnBm == null) {
            returnBm = bm;
        }
        if (bm != returnBm) {
            bm.recycle();
        }
        return returnBm;
    }
    private void setMyResult(ArrayList<String> pathList){
        Intent i = new Intent();
        i.putExtra(RETURN_DATA_FILE, pathList);
        setResult(Activity.RESULT_OK, i);

        finish();
    }

    public void cropImg(Uri uri) {
        uri = getFileProviderUri(uri);
        File file = new File(cropPath);
        Intent intent = new Intent("com.android.camera.action.CROP");
        intent.setDataAndType(uri, "image/*");
        intent.putExtra("crop", "true");
        intent.putExtra("aspectX", width);
        intent.putExtra("aspectY", height);
        intent.putExtra("outputX", width);
        intent.putExtra("outputY", height);
        intent.putExtra("return-data", false);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(file));
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.putExtra("outputFormat", Bitmap.CompressFormat.JPEG.toString());
        intent.putExtra("noFaceDetection", true);
        startActivityForResult(intent, RESULT_CAMERA_CROP_PATH_RESULT);
    }



    public void showImagePicker() {
        SImagePicker.from(this)
                .maxCount(maxCount)
                .rowCount(3)
                .pickMode(needCrop ? SImagePicker.MODE_AVATAR : SImagePicker.MODE_IMAGE)
                .showCamera(showCamera)
                .aspectX(0)
                .aspectY(0)
                .cropFilePath(cropPath)
                .photoPath(cameraPhotoPath)
                .forResult(RESULT_SELECT_PHOTO);
    }
}
