package ru.vodnouho.android.atthisdaywidgetapp;

import android.content.Context;
import android.graphics.Bitmap;
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
    private List<ViewHolder> mViewHolders;
    private String mLang;
    private int mTextColor;
    private int mBgColor;
    private int mLinkTextColor;

    public DataAdapter(Context context) {
        mContext = context;
        mInflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);

    }

    public void clear(){
        mContext = null;
        if(mViewHolders != null){

            for(ViewHolder viewHolder : mViewHolders){
                viewHolder.clear();
            }

            mViewHolders.clear();
            mViewHolders = null;
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


        if (mViewHolders == null) {
            mViewHolders = Collections.synchronizedList(new ArrayList<ViewHolder>());
        } else {
            mViewHolders.clear();
        }


        for (Category c : mData) {
            //category name
            ViewHolder vh = new ViewHolder(TYPE_CATEGORY_NAME);

            vh.categoryName = c.name;
            mViewHolders.add(vh);

            ArrayList<Fact> facts = c.getFavFacts();
            for (int i = 0; i < facts.size(); i++) {
                Fact f = facts.get(i);

                vh = new ViewHolder(TYPE_FACT);
                vh.factText = Html.fromHtml(f.text);

                vh.fact = f;
                mViewHolders.add(vh);

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

        if (mViewHolders == null) {
            if(DEBUG)
                Log.d(TAG, "getCount():0");

            return 0;
        }

        if(DEBUG)
            Log.d(TAG, "getCount():"+mViewHolders.size());


        return mViewHolders.size();
    }

    @Override
    public ViewHolder getItem(int position) {
        if (mViewHolders == null || mViewHolders.size() <= position) {
            return null;
        }

        return mViewHolders.get(position);
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
        ViewHolder viewHolder = mViewHolders.get(position);
        ViewHolder viewHolder1;

        //onCreate
        if (convertView == null){
            viewHolder1 = viewHolder;
            if(TYPE_CATEGORY_NAME == viewHolder.mViewType){
                convertView = mInflater.inflate(R.layout.widget_item_category, parent, false);
                viewHolder.setCategoryNameView(convertView);
            }else{
                convertView = mInflater.inflate(R.layout.widget_item, parent, false);
                viewHolder.setFactView(convertView);
            }
            convertView.setTag(viewHolder);
        }else{
            viewHolder1 = (ViewHolder) convertView.getTag();
        }

        //onBind
        if(TYPE_CATEGORY_NAME == viewHolder.mViewType){
            viewHolder1.categoryNameTextView.setText(viewHolder.categoryName);
        }else{
            viewHolder1.factTextTextView.setText(viewHolder.factText);

        }

        return convertView;

    }


    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        ViewHolder viewHolder = getItem(position);
        return viewHolder.mViewType;
    }


    private class ViewHolder {
        View mView;
        int mViewType;


        //category
        public String categoryName;
        TextView categoryNameTextView;

        //fact
        public Fact fact;
        public CharSequence factText;
        Bitmap mImageBitmap;
        TextView factTextTextView;



        ViewHolder(int viewType) {
            mViewType = viewType;
        }


        public void clear() {
            mView = null;
            mImageBitmap = null;
        }

        public void setCategoryNameView(View convertView) {
            categoryNameTextView = convertView.findViewById(R.id.tvItemText);
        }

        public void setFactView(View convertView) {
            factTextTextView = convertView.findViewById(R.id.tvItemText);
        }
    }
}
