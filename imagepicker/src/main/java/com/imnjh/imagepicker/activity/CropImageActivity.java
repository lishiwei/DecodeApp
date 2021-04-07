package com.imnjh.imagepicker.activity;

import com.image.picker.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.os.AsyncTask;
import android.os.Bundle;
import androidx.fragment.app.Fragment;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;


import com.imnjh.imagepicker.util.ImageUtil;
import com.imnjh.imagepicker.util.SystemUtil;
import com.imnjh.imagepicker.widget.ClipImageLayout;


/**
 * Created by Martin on 2017/1/17.
 */
public class CropImageActivity extends BasePickerActivity {

    public static final String RESULT_PATH = "crop_image";
    private static final String PARAM_ORIGIN_PATH = "param_origin_path";
    public static final String PARAM_AVATAR_PATH = "param_path";
    public static final String PARAM_ASPECTX = "param_aspectX";
    public static final String PARAM_ASPECTY = "param_aspectY";

    private static final int SIZE_LIMIT = 2048;

    ClipImageLayout clipImageLayout;
    View cancel;
    View confirm;

    private String sourcePath;
    private int sampleSize;
    private String filePath;
    private int aspectX;
    private int aspectY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        init();
    }

    private void init() {
        confirm = findViewById(R.id.confirm);
        cancel = findViewById(R.id.cancel);
        clipImageLayout = (ClipImageLayout) findViewById(R.id.clip_layout);
        sourcePath = getIntent().getStringExtra(PARAM_ORIGIN_PATH);

        filePath = getIntent().getStringExtra(PARAM_AVATAR_PATH);
        aspectX = getIntent().getIntExtra(PARAM_ASPECTX, 1);
        aspectY = getIntent().getIntExtra(PARAM_ASPECTY, 1);
        clipImageLayout.setAspectX(aspectX);
        clipImageLayout.setAspectY(aspectY);
        cancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        confirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                clipImage();
            }
        });
        rotatePicture(sourcePath);
        setSourceUri(sourcePath);
    }

    private void rotatePicture(String pathlist){
        int degree = getBitmapDegree(pathlist);
        if (degree!=0){
            Bitmap bitmap =   rotateBitmapByDegree(BitmapFactory.decodeFile(pathlist),degree);
            saveBitmap2File(bitmap,pathlist);
        }
    }
    public  File saveBitmap2File(Bitmap bitmap, String dir) {
        File file = new File(dir);
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
        Log.d("ImageSelectActivity", "getBitmapDegree: "+degree);
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
    public  Bitmap rotateBitmapByDegree(Bitmap bm, int degree) {
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

    @Override
    protected int getLayoutResId() {
        return R.layout.activity_crop_image;
    }

    public static void startImageCrop(Activity activity, String originPhotoPath, int requestCode,
                                      String dstFilePath) {
        Intent intent = new Intent();
        intent.putExtra(PARAM_ORIGIN_PATH, originPhotoPath);
        intent.putExtra(PARAM_AVATAR_PATH, dstFilePath);
        intent.setClass(activity, CropImageActivity.class);
        activity.startActivityForResult(intent, requestCode);
    }

    public static void startImageCrop(Activity activity, String originPhotoPath, int requestCode,
                                      String dstFilePath, int aspectX, int aspectY) {
        Intent intent = new Intent();
        intent.putExtra(PARAM_ORIGIN_PATH, originPhotoPath);
        intent.putExtra(PARAM_AVATAR_PATH, dstFilePath);
        intent.putExtra(PARAM_ASPECTX, aspectX);
        intent.putExtra(PARAM_ASPECTY, aspectY);
        intent.setClass(activity, CropImageActivity.class);
        activity.startActivityForResult(intent, requestCode);
    }

    public static void startImageCrop(Fragment fragment, String originPhotoPath, int requestCode,
                                      String dstFilePath) {
        Intent intent = new Intent();
        intent.putExtra(PARAM_ORIGIN_PATH, originPhotoPath);
        intent.putExtra(PARAM_AVATAR_PATH, dstFilePath);
        intent.setClass(fragment.getActivity(), CropImageActivity.class);
        fragment.startActivityForResult(intent, requestCode);
    }

    private void setSourceUri(final String sourcePath) {
        this.sourcePath = sourcePath;
        this.sampleSize = 0;
        if (!TextUtils.isEmpty(sourcePath)) {
            new AsyncTask<Void, Void, Void>() {

                @Override
                protected Void doInBackground(Void... params) {
                    try {
                        sampleSize = calculateBitmapSampleSize(sourcePath);
                        final Bitmap bitmap = ImageUtil.loadBitmap(sourcePath, sampleSize);
                        if (bitmap != null) {
                            SystemUtil.runOnUIThread(new Runnable() {
                                @Override
                                public void run() {
                                    startCrop(bitmap);
                                }
                            });
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return null;
                }
            }.execute();
        }
    }

    private void startCrop(Bitmap bitmap) {
        clipImageLayout.setImageBitmap(bitmap);
    }


    private int calculateBitmapSampleSize(String originPath) throws IOException {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(originPath, options);
        int maxSize = SIZE_LIMIT;
        int sampleSize = 1;
        if (options.outHeight > 0 && options.outWidth > 0) {
            while (options.outHeight / sampleSize > maxSize || options.outWidth / sampleSize > maxSize) {
                sampleSize = sampleSize << 1;
            }
        } else {
            sampleSize = sampleSize << 2;
        }
        return sampleSize;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void clipImage() {
        final Bitmap bitmap = clipImageLayout.clip();
        new AsyncTask<Void, Void, Boolean>() {

            @Override
            protected Boolean doInBackground(Void... params) {
                return ImageUtil.saveBitmap(bitmap, filePath, Bitmap.CompressFormat.JPEG, 85);
            }

            @Override
            protected void onPostExecute(Boolean success) {
                if (success) {
                    Intent intent = new Intent();
                    intent.putExtra(RESULT_PATH, filePath);
                    setResult(RESULT_OK, intent);
                }
                finish();
            }
        }.execute();
    }
}
