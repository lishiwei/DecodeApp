package com.xys.libzxing.zxing.activity;

import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.PagerAdapter;

import java.util.List;


public class MyPagerAdapter extends PagerAdapter {
    private Context   mContext;
    private List<String>  mData;

    public MyPagerAdapter(Context mContext, List<String> mData) {
        this.mContext = mContext;
        this.mData = mData;
    }

    @Override
    public int getCount() {
        return mData.size();

    }

    @NonNull
    @Override
    public Object instantiateItem(@NonNull ViewGroup container, int position) {
        TextView title =new  TextView(mContext);
        title.setText("");
        container.addView(title);
        return title;
    }

    @Nullable
    @Override
    public CharSequence getPageTitle(int position) {
        return  mData.get(position);
    }

    @Override
    public void destroyItem(@NonNull ViewGroup container, int position, @NonNull Object object) {
        container.removeView((View)object  );

    }

    @Override
    public boolean isViewFromObject(@NonNull View view, @NonNull Object object) {
        return view == object;
    }
}