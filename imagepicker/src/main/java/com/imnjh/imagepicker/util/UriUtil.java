package com.imnjh.imagepicker.util;

import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import androidx.annotation.Nullable;
import android.text.TextUtils;
import com.google.gson.Gson;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Created by Martin on 2017/1/17.
 */

public class UriUtil {
    public static final String HTTP_SCHEME = "http";
    public static final String HTTPS_SCHEME = "https";
    public static final String LOCAL_FILE_SCHEME = "file";
    public static final String LOCAL_CONTENT_SCHEME = "content";
    private static final String LOCAL_CONTACT_IMAGE_PREFIX;
    public static final String LOCAL_ASSET_SCHEME = "asset";
    public static final String LOCAL_RESOURCE_SCHEME = "res";
    public static final String DATA_SCHEME = "data";

    public UriUtil() {
    }

    public static boolean isNetworkUri(@Nullable Uri uri) {
        String scheme = getSchemeOrNull(uri);
        return "https".equals(scheme) || "http".equals(scheme);
    }

    public static boolean isLocalFileUri(@Nullable Uri uri) {
        String scheme = getSchemeOrNull(uri);
        return "file".equals(scheme);
    }

    public static boolean isLocalContentUri(@Nullable Uri uri) {
        String scheme = getSchemeOrNull(uri);
        return "content".equals(scheme);
    }

    public static boolean isLocalContactUri(Uri uri) {
        return isLocalContentUri(uri) && "com.android.contacts".equals(uri.getAuthority())
                && !uri.getPath().startsWith(LOCAL_CONTACT_IMAGE_PREFIX);
    }

    public static boolean isLocalCameraUri(Uri uri) {
        String uriString = uri.toString();
        return uriString.startsWith(MediaStore.Images.Media.EXTERNAL_CONTENT_URI.toString())
                || uriString.startsWith(MediaStore.Images.Media.INTERNAL_CONTENT_URI.toString());
    }

    public static boolean isLocalAssetUri(@Nullable Uri uri) {
        String scheme = getSchemeOrNull(uri);
        return "asset".equals(scheme);
    }

    public static boolean isLocalResourceUri(@Nullable Uri uri) {
        String scheme = getSchemeOrNull(uri);
        return "res".equals(scheme);
    }

    public static boolean isDataUri(@Nullable Uri uri) {
        return "data".equals(getSchemeOrNull(uri));
    }

    @Nullable
    public static String getSchemeOrNull(@Nullable Uri uri) {
        return uri == null ? null : uri.getScheme();
    }

    public static Uri parseUriOrNull(@Nullable String uriAsString) {
        return uriAsString != null ? Uri.parse(uriAsString) : null;
    }

    static {
        LOCAL_CONTACT_IMAGE_PREFIX =
                Uri.withAppendedPath(ContactsContract.AUTHORITY_URI, "display_photo").getPath();
    }

    private static JSONObject transfer(Gson gson, Object obj) {
        String json = gson.toJson(obj);
        JSONObject object = null;
        try {
            object = new JSONObject(json);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return object;
    }

    private static String getPathFormUri(Uri uri) {
        Gson gson = new Gson();
        JSONObject json = transfer(gson, uri);
        JSONObject pathObj = null;
        String path = null;
        if (json != null && json.has("path")) {
            pathObj = json.optJSONObject("path");
        }
        if (pathObj != null) {
            path = pathObj.optString("decoded");
        }
        return path;
    }

    public static void transfer(List<Uri> uriList, final OnTransferListener listener) {
        int count = uriList == null ? 0 : uriList.size();
        final ThreadPoolExecutor executor = buildExecutor();
        int maxPoolSize = executor.getMaximumPoolSize();
        final ListWrapper<Uri> listWrapper = new ListWrapper<>(uriList, maxPoolSize - 1);
        final ArrayList<String> list = buildEmptyArrayList(count);
        while (listWrapper.hasNext()) {
            final List<EntryWrapper<Uri>> entry = listWrapper.next();
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    transfer(list, entry);
                }
            });
        }

        new CheckThread(list, listener).start();
    }

    private static void transfer(List<String> list, List<EntryWrapper<Uri>> wrappers) {
        if (wrappers == null || wrappers.isEmpty()) {
            return;
        }
        for (EntryWrapper<Uri> wrapper : wrappers) {
            String path = getPathFormUri(wrapper.entry);
            if (!TextUtils.isEmpty(path)) {
                list.set(wrapper.index, path);
            }
        }
    }

    private static ThreadPoolExecutor buildExecutor() {
        int corePoolSize = Runtime.getRuntime().availableProcessors() + 1;
        int maxPoolSize = corePoolSize * 2 + 1;
        ThreadPoolExecutor executor = new ThreadPoolExecutor(corePoolSize, maxPoolSize, 1, TimeUnit.SECONDS, new LinkedBlockingQueue<Runnable>(128));
        return executor;
    }

    private static ArrayList<String> buildEmptyArrayList(int size) {
        ArrayList<String> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            list.add(null);
        }
        return list;
    }

    private static class CheckThread<T> extends Thread {
        private ArrayList<T> list;
        private OnTransferListener<T> onTransferListener;

        public CheckThread(ArrayList<T> list, OnTransferListener<T> onTransferListener) {
            this.onTransferListener = onTransferListener;
            this.list = list;
        }

        @Override
        public void run() {
            while (!isInterrupted()) {
                if (check()) {
                    interrupted();
                    if (onTransferListener != null) {
                        onTransferListener.onComplete(list);
                    }
                    return;
                }
                try {
                    sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }

        private boolean check() {
            if (list == null) {
                return false;
            }
            for (Object o : list) {
                if (o == null) {
                    return false;
                }
            }
            return true;
        }
    }

    public interface OnTransferListener<T> {
        void onComplete(ArrayList<T> list);
    }

    public static class ListWrapper<T> {
        private List<T> list;
        private int current;
        private int maxPoolSize;
        private int step;

        public ListWrapper(List<T> list, int maxPoolSize) {
            this.list = list;
            this.maxPoolSize = maxPoolSize;
            initStep();
        }

        private synchronized EntryWrapper<T> get() {
            int count = getCount();
            EntryWrapper<T> entry = null;
            if (current < count) {
                T obj = list.get(current);
                entry = new EntryWrapper<>();
                entry.entry = obj;
                entry.index = current;
                current++;
            }
            return entry;
        }

        public synchronized boolean hasNext() {
            return current < getCount();
        }

        public synchronized List<EntryWrapper<T>> next() {
            ArrayList<EntryWrapper<T>> subList = new ArrayList<>();
            int count = getCount();
            int target = current + step;
            if (target > count) {
                target = count;
            }
            for (int i = current; i < target; i++) {
                T obj = list.get(i);
                EntryWrapper<T> wrapper = new EntryWrapper<>();
                wrapper.entry = obj;
                wrapper.index = i;
                subList.add(wrapper);
            }
            current += step;
            return subList;
        }

        private synchronized int getCount() {
            int count = list == null ? 0 : list.size();
            return count;
        }

        private void initStep() {
            int count = getCount();
            step = count / maxPoolSize;
           if (count % maxPoolSize != 0) {
               step += 1;
           }
        }
    }

    public static class EntryWrapper<T> {
        public T entry;
        public int index;
    }
}
