package ru.vodnouho.android.atthisdaywidgetapp;

import android.content.Context;
import androidx.annotation.Nullable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;

import static ru.vodnouho.android.atthisdaywidgetapp.OTDWidgetProvider.LOGD;

/**
 * Created by petukhov on 05.04.2017.
 */

public class SaveLoadHelper {
    private static String TAG = "vdnh.SaveLoadHelper";

    public static void writeJson(JSONObject json, String fileName, Context context) throws IOException {
        if(LOGD) Log.d(TAG, "writeJson:"+fileName);
        Writer w = null;
        OutputStream out;

        try {
            out = context.openFileOutput(fileName, Context.MODE_PRIVATE);
            w = new OutputStreamWriter(out);
            w.write(json.toString());
        } finally {
            if (w != null)
                w.close();
        }
    }

    @Nullable
    public static  JSONObject readJson(String fileName, Context context) throws IOException, JSONException {
        if(LOGD) Log.d(TAG, "readJson:"+fileName);

        JSONObject result = null;
        BufferedReader reader = null;
        ArrayList<Category> categoryList = new ArrayList<>();

        try {
            InputStream in = context.openFileInput(fileName);
            reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder jsonStringBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                jsonStringBuilder.append(line);
            }

            if (jsonStringBuilder.length() == 0) {
                return null;
            } else {
                result = new JSONObject(jsonStringBuilder.toString());
            }


        } catch (FileNotFoundException e) {
            Log.d(TAG, "Can't find file:" + fileName);

        } finally {
            if (reader != null)
                reader.close();
        }

        return result;
    }

    public static boolean deleteFile(String fileName, Context context) {
        if(LOGD) Log.d(TAG, "deleteFile:"+fileName);
        return context.deleteFile(fileName);
    }

    public static boolean isFileExist(String cacheFileName, Context context) {
        String[] files = context.fileList();
        for(int i=0; i<files.length; i++){
            if(files[i].equals(cacheFileName)){
                return true;
            }
        }
        return false;
    }

    public static void deleteOldFiles(Context context){
        long oldBorder = System.currentTimeMillis() - 1000*60*60*24*3;
        String[] files = context.fileList();
        for(int i=0; i<files.length; i++){
            File file = new File(files[i]);
            long lastModified = file.lastModified();
            if(lastModified < oldBorder){
                if(file.delete()){
                    Log.d(TAG, "File:" + files[i] + " deleted");
                }
            }
        }
    }
}
