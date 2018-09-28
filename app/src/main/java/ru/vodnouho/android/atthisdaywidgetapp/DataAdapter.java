package ru.vodnouho.android.atthisdaywidgetapp;

import android.content.Context;
import android.graphics.Bitmap;

import androidx.core.content.ContextCompat;

import android.text.Html;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static ru.vodnouho.android.atthisdaywidgetapp.BuildConfig.DEBUG;
import static ru.vodnouho.android.atthisdaywidgetapp.OTDWidgetProvider.LOGD;
import static ru.vodnouho.android.atthisdaywidgetapp.SettingsActivity.TEXT_SIZE_DIFF;

public class DataAdapter extends BaseAdapter implements NetworkFetcher.OnLoadListener {
    private static final String TAG = "vdnh.DataAdapter";
    static final int TYPE_CATEGORY_NAME = 0;
    static final int TYPE_FACT = 1;


    private Context mContext;
    private final LayoutInflater mInflater;
    private NetworkFetcher mNetworkFetcher;

    private final Map<String, ArrayList<Fact>> mImageUrlRequests
            = Collections.synchronizedMap(new HashMap<String, ArrayList<Fact>>());


    private ArrayList<Category> mData;
    private List<DataHolder> mDataHolders;
    private String mLang;
    private int mTextColor;
    private int mBaseBgColor;
    private int mBgColor;
    private int mTransparency;
    private int mLinkTextColor;
    private String mTheme;
    private int mTextSize;


    public DataAdapter(Context context) {
        mContext = context;
        mInflater = (LayoutInflater) mContext
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        mNetworkFetcher = NetworkFetcher.getInstance(mContext);

    }

    public void clear() {
        mContext = null;
        if (mDataHolders != null) {

/*
            for(DataHolder viewHolder : mDataHolders){
                viewHolder.clear();
            }
*/

            mDataHolders.clear();
            mDataHolders = null;
        }
    }

    public void clearData() {
        if(mData != null){
            mData.clear();
        }
        if(mDataHolders != null){
            mDataHolders.clear();
        }
    }


    public void setData(ArrayList<Category> data, String lang) {
        mData = data;
        mLang = lang;
        refreshViewHolders();
        notifyDataSetChanged();
    }

    private void refreshViewHolders() {
        if (DEBUG) Log.d(TAG, "updateViews()");

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
            if (DEBUG)
                Log.d(TAG, "getCount():0");

            return 0;
        }

        if (DEBUG)
            Log.d(TAG, "getCount():" + mDataHolders.size());


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
        if (DEBUG) {
            Log.d(TAG, "getView position:" + position
                    + " convertView:" + convertView
                    + " parent:" + parent
            );
        }


        //TODO сделать вью холдеры отдельно от дата холдеров
        DataHolder dataHolder = mDataHolders.get(position);
        ViewHolder viewHolder;

        //onCreate
        if (convertView == null) {
            viewHolder = new ViewHolder();
            if (TYPE_CATEGORY_NAME == dataHolder.mViewType) {
                convertView = mInflater.inflate(R.layout.widget_item_category, parent, false);
                viewHolder.setCategoryNameView(convertView);
            } else {
                convertView = mInflater.inflate(R.layout.widget_item, parent, false);
                viewHolder.setFactView(convertView);
            }
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }

        //onBind
        if (TYPE_CATEGORY_NAME == dataHolder.mViewType) {
            viewHolder.categoryNameTextView.setText(dataHolder.categoryName);
            viewHolder.categoryNameTextView.setTextColor(mTextColor);
            viewHolder.categoryNameTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, mTextSize + TEXT_SIZE_DIFF);

        } else {
            viewHolder.factTextTextView.setText(dataHolder.factText);
            viewHolder.factTextTextView.setTextColor(mTextColor);
            viewHolder.factTextTextView.setLinkTextColor(mTextColor);
            viewHolder.factTextTextView.setTextSize(TypedValue.COMPLEX_UNIT_SP, mTextSize);
            viewHolder.mListItemView.setBackgroundColor(mBgColor);

            // show/hide year textView
            if (dataHolder.fact == null || dataHolder.fact.getYearsAgoString() == null) {
                viewHolder.yearTextView.setVisibility(View.GONE);

            } else {
                String localizedYearsAgoString = LocalizationUtils.getLocalizedString(
                        R.string.years_ago, mLang, mContext,  dataHolder.fact.getYearsAgoString());

                viewHolder.yearTextView.setText(localizedYearsAgoString);
                viewHolder.yearTextView.setTextColor(mTextColor);
                viewHolder.yearTextView.setVisibility(View.VISIBLE);
            }


            // show/hide image
            if (dataHolder.fact == null || dataHolder.fact.getThumbnailUrl() == null || dataHolder.mImageBitmap == null) {
                viewHolder.mImageView.setVisibility(View.GONE);
            } else {
                viewHolder.mImageView.setImageBitmap(dataHolder.mImageBitmap);
                viewHolder.mImageView.setVisibility( View.VISIBLE);
            }


            //better start parallel request after mViewsHolder.add()
            Fact fact = dataHolder.fact;
            if (fact != null) {
                if (fact.getThumbnailUrl() != null && dataHolder.mImageBitmap == null ) {
                    mNetworkFetcher.requestImage(fact.getThumbnailUrl(), this);
                } else if (fact.mayHasThumbnail()) {
                    String findPictureUrlAt = fact.getTitleForPicture();
                    if (findPictureUrlAt != null) {
                        addImageUrlRequest(findPictureUrlAt, fact);
                        mNetworkFetcher.requestJsonObject(findPictureUrlAt, this);
                    }
                }
            }
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
            mBaseBgColor = ContextCompat.getColor(cnxt, R.color.bgColor);
            mTextColor = ContextCompat.getColor(cnxt, R.color.textColor);
        } else {
            mBaseBgColor = ContextCompat.getColor(cnxt, R.color.bgBlackColor);
            mTextColor = ContextCompat.getColor(cnxt, R.color.textBlackColor);
        }

        mBgColor = Utils.setTransparency(mTransparency, mBaseBgColor);
    }

    public void setTransparency(int transparency) {
        mTransparency = transparency;
        mBgColor = Utils.setTransparency(transparency, mBaseBgColor);
    }

    public void setTextSize(int textSize) {
        mTextSize = textSize;
    }

    @Override
    public void onImageLoaded(String url, Bitmap bitmap) {
        if (LOGD)
            Log.d(TAG, "onImageLoaded() bitmap.size:" + bitmap.getByteCount());

        if (mDataHolders == null || mDataHolders.size() == 0) {
            return;
        }

        for (int i = 0; i < mDataHolders.size(); i++) {
            DataHolder holder = mDataHolders.get(i);
            Fact fact = holder.fact;

            if (fact == null) {
                continue;
            }

            if (holder.mImageBitmap == null
                    && fact.getThumbnailUrl() != null
                    && fact.getThumbnailUrl().equals(url)) {

                holder.mImageBitmap = bitmap;

                notifyDataSetChanged();
            }
        }
    }

    @Override
    public void onError(String url, Object error) {

    }

    @Override
    public void onJsonObjectLoaded(String url, JSONObject jsonResponse) {
        if (LOGD)
            Log.d(TAG, "onJsonObjectLoaded():" + jsonResponse.toString());


        if (jsonResponse == null) {
            return;
        }

        ArrayList<Fact> facts = mImageUrlRequests.get(url);
        if (facts == null || facts.size() == 0) {
            //nobody need this info
            return;
        }
        synchronized (mImageUrlRequests) {
            mImageUrlRequests.remove(url);
        }

        JSONObject thumbnail;
        try {
            if (jsonResponse.isNull("thumbnail")) {
                for (Fact f : facts) {
                    f.setThumbnailUrl("", url);
                    String findPictureUrlAt = f.getTitleForPicture();
                    if (findPictureUrlAt != null) {
                        addImageUrlRequest(findPictureUrlAt, f);
                        mNetworkFetcher.requestJsonObject(findPictureUrlAt, this);
                    }
                }

            } else {
                thumbnail = jsonResponse.getJSONObject("thumbnail");
                String thumbnailUrl = thumbnail.getString("source");

                if (thumbnailUrl != null && !thumbnailUrl.isEmpty()) {
                    for (Fact f : facts) {
                        f.setThumbnailUrl(thumbnailUrl, url);
                    }
                    mNetworkFetcher.requestImage(thumbnailUrl, this);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "onJsonObjectLoaded:", e);
        }
    }

    private void addImageUrlRequest(String titleUrl, Fact f) {
        ArrayList<Fact> prevRequests = mImageUrlRequests.get(titleUrl);
        if (prevRequests == null) {
            prevRequests = new ArrayList<>();
        }
        prevRequests.add(f);
        synchronized (mImageUrlRequests) {
            mImageUrlRequests.put(titleUrl, prevRequests);
        }
    }


    private class DataHolder {
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
        TextView yearTextView;
        TextView factTextTextView;
        ImageView mImageView;


        public void clear() {
        }

        public void setCategoryNameView(View convertView) {
            mListItemView = convertView.findViewById(R.id.list_item_ViewGroup);
            categoryNameTextView = convertView.findViewById(R.id.tvItemText);
        }

        public void setFactView(View convertView) {
            mListItemView = convertView.findViewById(R.id.list_item_ViewGroup);
            factTextTextView = convertView.findViewById(R.id.tvItemText);
            mImageView = convertView.findViewById(R.id.fact_ImageView);
            yearTextView = convertView.findViewById(R.id.yearItemText);
        }
    }
}
