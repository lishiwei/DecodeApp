package com.example.decodeapp;

import android.content.Context;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.net.Uri;
import android.widget.ImageView;

import com.facebook.cache.disk.DiskCacheConfig;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.drawee.drawable.ScalingUtils;
import com.facebook.drawee.interfaces.DraweeController;
import com.facebook.drawee.view.DraweeView;
import com.facebook.drawee.view.SimpleDraweeView;
import com.facebook.imagepipeline.backends.okhttp3.OkHttpImagePipelineConfigFactory;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.imnjh.imagepicker.ImageLoader;

import okhttp3.OkHttpClient;

/**
 * Created by za-wanghe on 2017/11/27.
 */

public class FrescoImageLoader implements ImageLoader {
    public static DiskCacheConfig.Builder getDiskCacheConfigBuilder(Context context) {
        return DiskCacheConfig.newBuilder(context)
                .setBaseDirectoryPath(context.getExternalCacheDir().getAbsoluteFile());
    }

    public static ImagePipelineConfig.Builder getImagePipelineConfigBuilder(Context context, DiskCacheConfig diskCacheConfig) {
        return OkHttpImagePipelineConfigFactory.newBuilder(context, new OkHttpClient.Builder().build())
                .setDownsampleEnabled(false)
                .setMainDiskCacheConfig(diskCacheConfig);
    }
    public FrescoImageLoader(Context context) {
        init(context);
    }

    private void init(Context context) {
        DiskCacheConfig diskCacheConfig = getDiskCacheConfigBuilder(context).build();
        ImagePipelineConfig config = getImagePipelineConfigBuilder(context, diskCacheConfig).build();
        Fresco.initialize(context, config);
    }

    @Override
    public void bindImage(ImageView photoImageView, Uri uri, int width, int height) {
        DraweeView draweeView = (DraweeView) photoImageView;
        final ImageRequestBuilder requestBuilder = ImageRequestBuilder.newBuilderWithSource(uri);
        if (width > 0 && height > 0) {
            requestBuilder.setResizeOptions(new ResizeOptions(width, height));
        }
        ImageRequest imageRequest = requestBuilder.build();
        DraweeController controller = Fresco.newDraweeControllerBuilder()
                .setOldController(draweeView.getController())
                .setImageRequest(imageRequest).build();
        draweeView.setController(controller);
    }

    @Override
    public void bindImage(ImageView imageView, Uri uri) {
        bindImage(imageView, uri, 0, 0);
    }

    @Override
    public ImageView createImageView(Context context) {
        SimpleDraweeView simpleDraweeView = new SimpleDraweeView(context);
        return simpleDraweeView;
    }

    @Override
    public ImageView createFakeImageView(Context context) {
        SimpleDraweeView fakeImage = new SimpleDraweeView(context);
        fakeImage.getHierarchy().setActualImageScaleType(ScaleTypeFillCenterInside.INSTANCE);
        return fakeImage;
    }

    public static class ScaleTypeFillCenterInside extends ScalingUtils.AbstractScaleType {

        public static final ScalingUtils.ScaleType INSTANCE = new ScaleTypeFillCenterInside();

        @Override
        public void getTransformImpl(
                Matrix outTransform,
                Rect parentRect,
                int childWidth,
                int childHeight,
                float focusX,
                float focusY,
                float scaleX,
                float scaleY) {
            float scale = Math.min(scaleX, scaleY);
            float dx = parentRect.left + (parentRect.width() - childWidth * scale) * 0.5f;
            float dy = parentRect.top + (parentRect.height() - childHeight * scale) * 0.5f;
            outTransform.setScale(scale, scale);
            outTransform.postTranslate((int) (dx + 0.5f), (int) (dy + 0.5f));
        }
    }
}