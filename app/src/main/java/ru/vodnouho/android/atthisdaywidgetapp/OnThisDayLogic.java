package ru.vodnouho.android.atthisdaywidgetapp;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.Nullable;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;

import java.util.ArrayList;
import java.util.concurrent.ConcurrentHashMap;

import static ru.vodnouho.android.atthisdaywidgetapp.OTDWidgetProvider.LOGD;

/**
 * Created by petukhov on 06.04.2017.
 * единый источник данных, который
 * 1. содаёт неизменяемые модели
 * 2. извещает презентера, о новой модели
 * 3. обрабатывает события, которые могут привести к созданию модели
 */

public class OnThisDayLogic implements Loader.OnLoadCompleteListener<Cursor> {
    private static final String TAG = "vdnh.OnThisDayLogic";
    //храним живые сервисы по EXTRA_APPWIDGET_ID
    private static ConcurrentHashMap<String, OnThisDayLogic> mapByWDateLang = new ConcurrentHashMap<String, OnThisDayLogic>(3);
    //TODO убрать костыль. массив поддерживаемых языков для генерации ID лоадера
    private static String[] sLangArray = {"ff", "de","en","es","fr","ru"};

    private ArrayList<ModelChangedListener> mListeners = new ArrayList<>();
    private OnThisDayModel mModel;
    private String mDateString;
    private String mLang;
    private String mCacheFileName;
    private Context mContext;
    private CursorLoader mCategoryLoader;



    public static OnThisDayLogic getInstance(String dateString, String lang, Context context) {
        String key = dateString + "_" + lang;
        OnThisDayLogic onThisDayLogic = mapByWDateLang.get(key);
        if (onThisDayLogic == null) {
            onThisDayLogic = new OnThisDayLogic(dateString, lang, context);
            mapByWDateLang.put(key, onThisDayLogic);
        }
        return onThisDayLogic;
    }



    public OnThisDayLogic(String MMdd, String lang, Context context) {
        mDateString = MMdd;
        mLang = lang;
        mContext = context;
        mCacheFileName = mDateString + "_" + mLang + ".json";
        mModel = new OnThisDayModel(MMdd, lang, null, true, false);
    }


    public void registerModelChangedListener(ModelChangedListener listener) {
        if (!mListeners.contains(listener)) {
            mListeners.add(listener);
        }
    }

    private void notifyListeners() {
        for (ModelChangedListener listener : mListeners) {
            listener.onModelChanged(mModel);
        }
    }

    public void loadData() {
        if(mCategoryLoader == null){
            initCategoryLoader();
        }
        mCategoryLoader.forceLoad();
    }



    @Override
    public void onLoadComplete(Loader<Cursor> loader, Cursor data) {
        if (LOGD)
            Log.d(TAG, "onLoadComplete() id=" + loader.getId());
        int loaderType = loader.getId();

        if (mCategoryLoader.equals(loader)) {
            ArrayList<Category> categories = DataFetcher.fillCategories(data);


            boolean isError = false;
            if (categories == null || categories.size() < 2) {
                isError = true;
            }else{
                filterCategories(categories);
                fillCategoriesByFacts(categories);

            }


            mModel = new OnThisDayModel(mDateString, mLang, categories, false, isError);

            notifyListeners();
        }


    }


    private void fillCategoriesByFacts(ArrayList<Category> categories) {

        //clear favFacts
        for (Category c : categories) {
            c.clearFavFacts();
        }

        //TODO lang must be set by ContentProvider
        DataFetcher.fillCategoriesWithFavoriteFacts(mContext, categories, mLang);

        for (Category c : categories) {

            ArrayList<Fact> facts = c.getFavFacts();
            for (int i = 0; i < facts.size(); ) {
                Fact f = facts.get(i);
                if (isFilteredFacts(f)) {
                    c.removeFavFact(f);
                    continue;
                }
                i++;
            }
        }
    }

    /**
     * Определяем плохие факты
     * @param f
     * @return true if fact is bad
     */
    private boolean isFilteredFacts(Fact f) {
        if (f.text == null
                || f.text.isEmpty()
                || f.text.contains("См. также")
                ) {

            return true;
        }
        return false;
    }

    /**
     * Query Data

     */
    public void initCategoryLoader() {
        if (LOGD)
            Log.d(TAG, "Querying for widget events...");

        int loaderId = generateLoaderId();

        // Search for events from now until some time in the future
        Uri uri = null;
        String[] projection = null;

        uri = DataFetcher.createUriForCategories(mLang, mDateString);
        projection = DataFetcher.createProjectionsForCategories();

        mCategoryLoader = new CursorLoader(mContext, uri, projection, null, null, null);
        mCategoryLoader.registerListener(loaderId, this);
        mCategoryLoader.startLoading();


        if (LOGD)
            Log.d(TAG, "Start loading " + uri);

    }

    private int generateLoaderId(){
        int i=1;
        for(;i<sLangArray.length; i++){
            if(sLangArray[i].equals(mLang)){
                break;
            }
        }
        return Integer.parseInt(i+mDateString);
    }

    @Nullable
    private ArrayList<Category> filterCategories(ArrayList<Category> categories) {
        if (categories == null) {
            return null;
        }
        for (int i = 0; i < categories.size(); ) {
            Category category = categories.get(i);
            if ("Véase también".equals(category.name)
                    || "Enlaces externos".equals(category.name)
                    || "Referencias".equals(category.name)
                    || "Toponymie".equals(category.name)
                    || "Bibliographie".equals(category.name)
                    || "Articles connexes".equals(category.name)
                    ) {
                categories.remove(i);
                continue;
            }
            i++;
        }

        return categories;
    }


    public interface ModelChangedListener {
        public void onModelChanged(OnThisDayModel newModel);
    }

}
