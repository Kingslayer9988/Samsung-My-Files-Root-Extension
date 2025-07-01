package com.samsung.android.app.networkstoragemanager;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.samsung.cifs.storage.manager.StorageClientManager;
import com.samsung.cifs.common.values.StorageType;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class CifsIntegration {
    
    private static final String PREF_CIFS_SHARES = "cifs_shares";
    private static final String TAG = "CifsIntegration";
    
    // CIFS Share configuration class
    public static class CifsShare {
        public String name;
        public String address;
        public String username;
        public String password;
        public String connectionType; // SMB, FTP, SFTP
        public int port;
        public boolean anonymous;
        
        public CifsShare(String name, String address, String connectionType) {
            this.name = name;
            this.address = address;
            this.connectionType = connectionType;
            this.anonymous = true;
            this.port = getDefaultPort(connectionType);
        }
        
        private int getDefaultPort(String type) {
            switch (type.toUpperCase()) {
                case "SMB": return 445;
                case "FTP": return 21;
                case "SFTP": return 22;
                default: return 21;
            }
        }
    }
    
    public static ArrayList<Bundle> getCifsShares(Context context) {
        ArrayList<Bundle> cifsShares = new ArrayList<>();
        List<CifsShare> savedShares = loadCifsShares(context);
        
        int serverId = 101; // Start from 101 for CIFS shares
        for (CifsShare share : savedShares) {
            Bundle bundle = new Bundle();
            bundle.putLong("serverId", serverId++);
            bundle.putString("serverAddr", share.address);
            bundle.putString("serverName", "  📁 " + share.name);
            bundle.putString("sharedFolder", "");
            bundle.putString("category", "cifs_shares");
            bundle.putString("connectionType", share.connectionType);
            bundle.putString("parentId", "100");
            bundle.putBoolean("isAnonymousMode", share.anonymous);
            bundle.putInt("serverPort", share.port);
            if (!share.anonymous) {
                bundle.putString("username", share.username);
                bundle.putString("password", share.password);
            }
            cifsShares.add(bundle);
        }
        
        return cifsShares;
    }
    
    public static void saveCifsShare(Context context, CifsShare share) {
        List<CifsShare> shares = loadCifsShares(context);
        shares.add(share);
        saveCifsShares(context, shares);
    }
    
    public static void removeCifsShare(Context context, String address) {
        List<CifsShare> shares = loadCifsShares(context);
        shares.removeIf(share -> share.address.equals(address));
        saveCifsShares(context, shares);
    }
    
    private static List<CifsShare> loadCifsShares(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("cifs_config", Context.MODE_PRIVATE);
        String json = prefs.getString(PREF_CIFS_SHARES, "[]");
        
        try {
            Type listType = new TypeToken<List<CifsShare>>(){}.getType();
            List<CifsShare> shares = new Gson().fromJson(json, listType);
            return shares != null ? shares : new ArrayList<>();
        } catch (Exception e) {
            Log.e(TAG, "Error loading CIFS shares", e);
            return getDefaultCifsShares();
        }
    }
    
    private static void saveCifsShares(Context context, List<CifsShare> shares) {
        SharedPreferences prefs = context.getSharedPreferences("cifs_config", Context.MODE_PRIVATE);
        String json = new Gson().toJson(shares);
        prefs.edit().putString(PREF_CIFS_SHARES, json).apply();
    }
    
    private static List<CifsShare> getDefaultCifsShares() {
        List<CifsShare> defaultShares = new ArrayList<>();
        // Add example SMB shares only
        defaultShares.add(new CifsShare("Home Server", "smb://192.168.1.100/shared", "SMB"));
        defaultShares.add(new CifsShare("NAS Drive", "smb://192.168.1.200/public", "SMB"));
        return defaultShares;
    }
    
    public static ArrayList<Bundle> accessCifsShare(Context context, long serverId, String serverAddr) {
        Log.i(TAG, "Accessing CIFS share: " + serverAddr);
        
        try {
            // Parse the server address to extract connection details
            CifsShare share = parseServerAddress(serverAddr);
            if (share == null) {
                Log.e(TAG, "Failed to parse server address: " + serverAddr);
                return new ArrayList<>();
            }
            
            // TODO: Implement actual file listing using StorageClientManager
            // For now, return example directory structure
            ArrayList<Bundle> fileList = new ArrayList<>();
            
            Bundle folder1 = new Bundle();
            folder1.putLong("serverId", serverId);
            folder1.putString("filePath", serverAddr + "/Documents");
            folder1.putString("fileName", "Documents");
            folder1.putBoolean("isDirectory", true);
            folder1.putLong("fileSize", 0);
            folder1.putLong("lastModified", System.currentTimeMillis());
            fileList.add(folder1);
            
            Bundle folder2 = new Bundle();
            folder2.putLong("serverId", serverId);
            folder2.putString("filePath", serverAddr + "/Pictures");
            folder2.putString("fileName", "Pictures");
            folder2.putBoolean("isDirectory", true);
            folder2.putLong("fileSize", 0);
            folder2.putLong("lastModified", System.currentTimeMillis());
            fileList.add(folder2);
            
            return fileList;
            
        } catch (Exception e) {
            Log.e(TAG, "Error accessing CIFS share: " + serverAddr, e);
            return new ArrayList<>();
        }
    }
    
    private static CifsShare parseServerAddress(String serverAddr) {
        try {
            if (serverAddr.startsWith("smb://")) {
                String address = serverAddr.substring(6); // Remove "smb://"
                return new CifsShare("SMB Share", serverAddr, "SMB");
            }
            // Add parsing for other protocols as needed
            return null;
        } catch (Exception e) {
            Log.e(TAG, "Error parsing server address: " + serverAddr, e);
            return null;
        }
    }
    
    public static void openCifsManager(Context context) {
        Log.i(TAG, "Opening CIFS Documents Provider for configuration");
        
        try {
            // Try to start the CIFS Documents Provider activity directly
            // Since it's integrated into our app, we can access it via the presentation module
            Intent intent = new Intent();
            intent.setClassName(context.getPackageName(), "com.samsung.cifs.presentation.ui.main.MainActivity");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Log.e(TAG, "Error launching CIFS configuration activity", e);
            // Fallback: try to open the external CIFS Documents Provider if available
            openExternalCifsDocumentsProvider(context);
        }
    }
    
    private static void openExternalCifsDocumentsProvider(Context context) {
        try {
            Intent intent = context.getPackageManager()
                .getLaunchIntentForPackage("com.wa2c.android.cifsdocumentsprovider");
            if (intent != null) {
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } else {
                Log.w(TAG, "External CIFS Documents Provider not installed");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error launching external CIFS Documents Provider", e);
        }
    }
}