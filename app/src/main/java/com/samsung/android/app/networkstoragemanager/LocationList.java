package com.samsung.android.app.networkstoragemanager;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class LocationList {


    public static ArrayList<Bundle> loadList(Context context) {
        String json = context.getSharedPreferences("sp", Context.MODE_PRIVATE).getString("locations", null);
        if (json == null) return getDefaultList(true);
        ArrayList<Bundle> list = parseJson(json);
        return list != null ? list : getDefaultList(true);
    }

    public static void saveList(Context context, ArrayList<Bundle> list) {
        context.getSharedPreferences("sp", Context.MODE_PRIVATE).edit().putString("locations", parseList(list)).apply();
    }

    private static ArrayList<Bundle> parseJson(String json) {
        try {
            Log.e("load", json);
            JSONArray jsonArray = new JSONArray(json);
            ArrayList<Bundle> list = new ArrayList<>();
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject jsonObject = jsonArray.getJSONObject(i);
                Bundle bundle = new Bundle();
                bundle.putLong("serverId", jsonObject.getLong("serverId"));
                bundle.putString("serverName", jsonObject.optString("serverName"));
                bundle.putString("serverAddr", jsonObject.optString("serverAddr"));
                bundle.putString("sharedFolder", jsonObject.optString("sharedFolder"));
                bundle.putBoolean("isAnonymousMode", true);
                bundle.putInt("serverPort", 1);
                list.add(bundle);
            }
            return list;
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    private static String parseList(ArrayList<Bundle> list) {
        try {
            JSONArray jsonArray = new JSONArray();
            for (Bundle bundle : list) {
                JSONObject jsonObject = new JSONObject();
                jsonObject.put("serverId", bundle.getLong("serverId"));
                jsonObject.put("serverName", bundle.getString("serverName"));
                jsonObject.put("serverAddr", bundle.getString("serverAddr"));
                jsonObject.put("sharedFolder", bundle.getString("sharedFolder"));
                jsonArray.put(jsonObject);
            }
            Log.e("save", jsonArray.toString());
            return jsonArray.toString();
        } catch (JSONException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static ArrayList<Bundle> getDefaultList(boolean serverIds) {
        ArrayList<Bundle> defaultList = new ArrayList<>();
        Bundle bStorage;

        // Present FTP/SFTP options that Samsung My Files understands, but redirect to SMB/CIFS backend
        
        // 1. "Add Root Location" - disguised as FTP server
        bStorage = new Bundle();
        bStorage.putBoolean("isAnonymousMode", true);
        bStorage.putInt("serverPort", 21);
        if (serverIds) bStorage.putLong("serverId", 1);
        bStorage.putString("serverAddr", "ftp://root.local");
        bStorage.putString("serverName", "🔓 Add Root Location");
        bStorage.putString("sharedFolder", "");
        bStorage.putString("connectionType", "FTP");
        bStorage.putString("serverType", "ROOT_ACCESS");
        bStorage.putString("protocol", "FTP");
        defaultList.add(new Bundle(bStorage));

        // 2. "Add SMB/CIFS Share" - disguised as SFTP server  
        bStorage = new Bundle();
        bStorage.putBoolean("isAnonymousMode", true);
        bStorage.putInt("serverPort", 22);
        if (serverIds) bStorage.putLong("serverId", 2);
        bStorage.putString("serverAddr", "sftp://smb.local");
        bStorage.putString("serverName", "🌐 Add SMB/CIFS Share");
        bStorage.putString("sharedFolder", "");
        bStorage.putString("connectionType", "SFTP");
        bStorage.putString("serverType", "SMB_ACCESS");
        bStorage.putString("protocol", "SFTP");
        defaultList.add(new Bundle(bStorage));

        return defaultList;
    }

private static void addCifsShares(ArrayList<Bundle> defaultList, boolean serverIds) {
    // Add example SMB shares only (FTP/SFTP removed)
    Bundle bStorage = new Bundle();
    bStorage.putBoolean("isAnonymousMode", true);
    bStorage.putInt("serverPort", 445);
    
    // Example SMB Share 1
    if (serverIds) bStorage.putLong("serverId", 101);
    bStorage.putString("serverAddr", "smb://192.168.1.100/shared");
    bStorage.putString("serverName", "  🌐 Home Server (SMB)");
    bStorage.putString("sharedFolder", "");
    bStorage.putString("category", "network_storage");
    bStorage.putString("connectionType", "SMB");
    bStorage.putString("parentId", "100");
    defaultList.add(new Bundle(bStorage));
    
    // Example SMB Share 2
    if (serverIds) bStorage.putLong("serverId", 102);
    bStorage.putString("serverAddr", "smb://192.168.1.200/public");
    bStorage.putString("serverName", "  🌐 NAS Drive (SMB)");
    bStorage.putString("sharedFolder", "");
    bStorage.putString("category", "network_storage");
    bStorage.putString("connectionType", "SMB");
    bStorage.putString("parentId", "100");
    defaultList.add(new Bundle(bStorage));
}
}
