package ru.vodnouho.android.atthisdaywidgetapp;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.content.ContextCompat;
import android.text.Html;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static ru.vodnouho.android.atthisdaywidgetapp.BuildConfig.DEBUG;

public class DataAdapter extends BaseAdapter {
    private static final String TAG = "vdnh.DataAdapter";
    static final int TYPE_CATEGORY_NAME = 0;
    static final int TYPE_FACT = 1;



    private Context mContext;
    private final LayoutInflater mInflater;
    private ArrayList<Category> mData;
    private List<DataHolder> mDataHolders;
    private String mLang;
    private int mTextColor;
    private int mBgColor;
    private int mLinkTextColor;
    private String mTheme;

    public DataAdapter(Context context) {
        mContext = context;
        mInflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    }

    public void clear(){
        mContext = null;
        if(mDataHolders != null){

/*
            for(DataHolder viewHolder : mDataHolders){
                viewHolder.clear();
            }
*/

            mDataHolders.clear();
            mDataHolders = null;
        }
    }

    public void setData(ArrayList<Category> data, String lang) {
        mData = data;
        mLang = lang;
        refreshViewHolders();
        notifyDataSetChanged();
    }

    private void refreshViewHolders() {
        if(DEBUG) Log.d(TAG, "updateViews()");

        //once get localized MORE string
        //String localizedMoreString = LocalizationUtils.getLocalizedString(R.string.more, mLang, mContext);


        if (mDataHolders == null) {
            mDataHolders = Collections.synchronizedList(new ArrayList<DataHolder>());
        } else {
            mDataHolders.clear();
        }


        for (Category c : mData) {
            //category name
            DataHolder vh = new DataHolder(TYPE_CATEGORY_NAME);

            vh.categoryName = c.name;
            mDataHolders.add(vh);

            ArrayList<Fact> facts = c.getFavFacts();
            for (int i = 0; i < facts.size(); i++) {
                Fact f = facts.get(i);

                vh = new DataHolder(TYPE_FACT);
                vh.factText = Html.fromHtml(f.text);

                vh.fact = f;
                mDataHolders.add(vh);

                //better start parallel request after mViewsHolder.add()
/*
                if (factViewHolder.mFact != null && factViewHolder.mFact.getThumbnailUrl() != null) {
                    mNetworkFetcher.requestImage(factViewHolder.mFact.getThumbnailUrl(), this);
                } else if (f.mayHasThumbnail()) {
                    String findPictureUrlAt = f.getTitleForPicture();
                    if (findPictureUrlAt != null) {
                        addImageUrlRequest(findPictureUrlAt, f);
                        mNetworkFetcher.requestJsonObject(findPictureUrlAt, this);
                    }
                }
*/

            }


        }


    }


    //implemented methods
    @Override
    public int getCount() {

        if (mDataHolders == null) {
            if(DEBUG)
                Log.d(TAG, "getCount():0");

            return 0;
        }

        if(DEBUG)
            Log.d(TAG, "getCount():"+ mDataHolders.size());


        return mDataHolders.size();
    }

    @Override
    public DataHolder getItem(int position) {
        if (mDataHolders == null || mDataHolders.size() <= position) {
            return null;
        }

        return mDataHolders.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }



    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(DEBUG){
            Log.d(TAG, "getView position:"+position
                    +" convertView:" +convertView
                    +" parent:" + parent
            );
        }



        //TODO сделать вью холдеры отдельно от дата холдеров
        DataHolder dataHolder = mDataHolders.get(position);
        ViewHolder viewHolder;

        //onCreate
        if (convertView == null){
            viewHolder = new ViewHolder();
            if(TYPE_CATEGORY_NAME == dataHolder.mViewType){
                convertView = mInflater.inflate(R.layout.widget_item_category, parent, false);
                viewHolder.setCategoryNameView(convertView);
            }else{
                convertView = mInflater.inflate(R.layout.widget_item, parent, false);
                viewHolder.setFactView(convertView);
            }
            convertView.setTag(viewHolder);
        }else{
            viewHolder = (ViewHolder) convertView.getTag();
        }

        //onBind
        if(TYPE_CATEGORY_NAME == dataHolder.mViewType){
            viewHolder.categoryNameTextView.setText(dataHolder.categoryName);
            viewHolder.categoryNameTextView.setTextColor(mTextColor);
        }else{
            viewHolder.factTextTextView.setText(dataHolder.factText);
            viewHolder.factTextTextView.setTextColor(mTextColor);
            viewHolder.mListItemView.setBackgroundColor(mBgColor);
        }

        return convertView;

    }


    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        DataHolder dataHolder = getItem(position);
        return dataHolder.mViewType;
    }

    public void setTheme(Context cnxt, String theme) {
        mTheme = theme;

        if (SettingsActivity.THEME_LIGHT.equals(theme)) { //mTheme to args
            mBgColor = ContextCompat.getColor(cnxt, R.color.bgColor);
            mTextColor = ContextCompat.getColor(cnxt, R.color.textColor);
        } else {
            mBgColor = ContextCompat.getColor(cnxt, R.color.bgBlackColor);
            mTextColor = ContextCompat.getColor(cnxt, R.color.textBlackColor);
        }

    }

    private class DataHolder{
        int mViewType;

        //category
        public String categoryName;

        //fact
        public Fact fact;
        public CharSequence factText;
        public Bitmap mImageBitmap;


        DataHolder(int viewType) {
            mViewType = viewType;
        }
    }


    private class ViewHolder {
        View mListItemView;

        //category
        TextView categoryNameTextView;

        //fact
        TextView factTextTextView;

        public void clear() {
        }

        public void setCategoryNameView(View convertView) {
            mListItemView = convertView.findViewById(R.id.list_item_ViewGroup);
            categoryNameTextView = convertView.findViewById(R.id.tvItemText);
        }

        public void setFactView(View convertView) {
            mListItemView = convertView.findViewById(R.id.list_item_ViewGroup);
            factTextTextView = convertView.findViewById(R.id.tvItemText);
        }
    }
}
