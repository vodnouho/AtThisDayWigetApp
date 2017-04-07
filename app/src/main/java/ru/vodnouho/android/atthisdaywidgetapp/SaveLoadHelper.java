package ru.vodnouho.android.atthisdaywidgetapp;

import android.content.Context;
import android.support.annotation.Nullable;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;

/**
 * Created by petukhov on 05.04.2017.
 */

public class SaveLoadHelper {
    private static String TAG = "vdnh.SaveLoadHelper";

    public static void writeJson(JSONObject json, String fileName, Context context) throws IOException {
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

    public static boolean isFileExist(String cacheFileName, Context context) {
        String[] files = context.fileList();
        for(int i=0; i<files.length; i++){
            if(files[i].equals(cacheFileName)){
                return true;
            }
        }
        return false;
    }
}
