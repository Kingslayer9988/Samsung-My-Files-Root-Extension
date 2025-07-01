package com.samsung.android.app.networkstoragemanager;

import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.os.RemoteException;
import android.util.Log;

import com.samsung.android.app.networkstoragemanager.libsupport.IProgressCallback;
import com.samsung.android.app.networkstoragemanager.libsupport.IRequestInterface;
import com.samsung.android.app.networkstoragemanager.libsupport.IResultCallback;
import com.samsung.android.app.networkstoragemanager.libsupport.RequestCode;
import com.topjohnwu.superuser.Shell;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

public class MainService extends Service implements RequestCode {

    private static final String TAG = "MainService";
    
    private ArrayList<Bundle> storageLocations = new ArrayList<>();
    private IResultCallback mCallback;
    private IProgressCallback mProgressCallback;
    private Map<Long, RequestInfo> mRequestInfoMap = new HashMap();

    private final IRequestInterface.Stub mBinder = new IRequestInterface.Stub() {

        public void asyncRequest(long serverId, String type, int reqCode, Bundle extras) {
            (new Thread(() -> syncRequest(serverId, type, reqCode, extras))).start();
        }

        public boolean cancel(long serverId) {
            RequestInfo requestInfo = mRequestInfoMap.get(serverId);
            if (requestInfo != null) {
                requestInfo.mCanceled.set(true);
                return true;
            } else {
                return false;
            }
        }

        public boolean registerProgressCallback(IProgressCallback var1) {
            MainService.this.mProgressCallback = var1;
            return true;
        }

        public boolean registerResultCallback(IResultCallback var1) {
            MainService.this.mCallback = var1;
            return true;
        }

        public void retryRequest(long serverId) {
            RequestInfo requestInfo = mRequestInfoMap.get(serverId);
            if (requestInfo != null) {
                asyncRequest(requestInfo.mServerId, requestInfo.mType, requestInfo.mReqCode, requestInfo.mExtras);
            }
        }

        public Bundle syncRequest(long serverId, String type, int reqCode, Bundle extras) {
            RequestInfo requestInfo = new RequestInfo(serverId, type, reqCode, extras);
            mRequestInfoMap.put(serverId, requestInfo);
            Bundle result = new Bundle();
            handleRequest(requestInfo, result);
            return result;
        }

        public boolean unregisterProgressCallback(IProgressCallback var1) {
            MainService.this.mProgressCallback = null;
            return true;
        }

        public boolean unregisterResultCallback(IResultCallback var1) {
            MainService.this.mCallback = null;
            return true;
        }
    };

    private void handleRequest(RequestInfo requestInfo, Bundle result) {
        result.putBoolean("isSuccess", true);
        result.putBoolean("isValidRequest", true);

        Log.e("handleRequest", requestInfo.mReqCode + " " + requestInfo.mType);
        Bundle extras = requestInfo.mExtras;
        for (String s : extras.keySet()) Log.i(s, String.valueOf(extras.get(s)));

        switch (requestInfo.mReqCode) {
            case CONNECT:
                //0
                break;
            case GET_SERVER_LIST:
                //1 - Return servers based on protocol type Samsung My Files requests
                Log.i(TAG, "GET_SERVER_LIST request for type: " + requestInfo.mType);
                if (requestInfo.mType != null) {
                    if (requestInfo.mType.equals("FTP")) {
                        // Return root access options disguised as FTP
                        ArrayList<Bundle> ftpServers = new ArrayList<>();
                        for (Bundle location : storageLocations) {
                            if ("FTP".equals(location.getString("connectionType")) || 
                                "ROOT_ACCESS".equals(location.getString("serverType"))) {
                                ftpServers.add(location);
                            }
                        }
                        result.putParcelableArrayList("serverList", ftpServers);
                    } else if (requestInfo.mType.equals("SFTP")) {
                        // Return SMB/CIFS options disguised as SFTP
                        ArrayList<Bundle> sftpServers = new ArrayList<>();
                        for (Bundle location : storageLocations) {
                            if ("SFTP".equals(location.getString("connectionType")) || 
                                "SMB_ACCESS".equals(location.getString("serverType"))) {
                                sftpServers.add(location);
                            }
                        }
                        result.putParcelableArrayList("serverList", sftpServers);
                    } else {
                        // For any other type or generic request, return all servers
                        result.putParcelableArrayList("serverList", storageLocations);
                    }
                } else {
                    // No type specified, return all servers
                    result.putParcelableArrayList("serverList", storageLocations);
                }
                result.putBoolean("result", true);
                break;
            case ADD_SERVER:
                //2 - Detect special server addresses and route to appropriate functionality
                String connectionType = extras.getString("connectionType", "");
                String serverType = extras.getString("serverType", "");
                String serverAddr = extras.getString("serverAddr", "");
                String protocol = extras.getString("protocol", "");
                
                Log.i(TAG, "ADD_SERVER request - connectionType: " + connectionType + 
                     ", serverType: " + serverType + ", serverAddr: " + serverAddr + 
                     ", protocol: " + protocol);
                
                // Check if this is one of our special server addresses
                if ("ROOT_ACCESS".equals(serverType) || "ftp://root.local".equals(serverAddr)) {
                    // Route to root access functionality
                    Log.i(TAG, "Routing ADD_SERVER to root access functionality");
                    try {
                        Intent rootIntent = new Intent(this, com.samsung.android.app.networkstoragemanager.activity.AddRootLocationActivity.class);
                        rootIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(rootIntent);
                        result.putBoolean("result", true);
                        result.putLong("serverId", System.currentTimeMillis());
                    } catch (Exception e) {
                        Log.e(TAG, "Error launching AddRootLocationActivity", e);
                        result.putBoolean("result", false);
                    }
                } else if ("SMB_ACCESS".equals(serverType) || "sftp://smb.local".equals(serverAddr)) {
                    // Route to SMB/CIFS functionality
                    Log.i(TAG, "Routing ADD_SERVER to SMB/CIFS functionality");
                    try {
                        CifsIntegration.openCifsManager(this);
                        result.putBoolean("result", true);
                        result.putLong("serverId", System.currentTimeMillis());
                    } catch (Exception e) {
                        Log.e(TAG, "Error launching CIFS manager", e);
                        result.putBoolean("result", false);
                    }
                } else if (serverAddr != null && !serverAddr.isEmpty()) {
                    // Real server configuration - store it
                    Bundle newServer = new Bundle(extras);
                    newServer.putLong("serverId", System.currentTimeMillis());
                    storageLocations.add(newServer);
                    result.putBoolean("result", true);
                    result.putLong("serverId", newServer.getLong("serverId"));
                } else {
                    // Generic ADD_SERVER request - show SMB/CIFS manager as default
                    Log.i(TAG, "Generic ADD_SERVER request, routing to SMB/CIFS functionality");
                    try {
                        CifsIntegration.openCifsManager(this);
                        result.putBoolean("result", true);
                        result.putLong("serverId", System.currentTimeMillis());
                    } catch (Exception e) {
                        Log.e(TAG, "Error launching CIFS manager for generic ADD_SERVER", e);
                        result.putBoolean("result", false);
                    }
                }
                break;
            case UPDATE_SERVER:
                //4
                result.putBoolean("result", false);
                for (Bundle location : storageLocations) {
                    if (location.getLong("serverId") == extras.getLong("serverId")) {
                        location.putAll(extras);
                        result.putBoolean("result", true);
                        break;
                    }
                }
                break;
            case DELETE_SERVER:
                //6
                result.putBoolean("result", false);
                for (Bundle location : storageLocations) {
                    if (location.getLong("serverId") == extras.getLong("serverId")) {
                        storageLocations.remove(location);
                        result.putBoolean("result", true);
                        break;
                    }
                }
                break;
            case FIND_SERVER:
                //7 (when opening + dialog) - Return only SMB/CIFS servers
                Log.i(TAG, "FIND_SERVER request");
                result.putParcelableArrayList("serverList", LocationList.getDefaultList(false));
                result.putBoolean("result", true);
                break;
            case GET_SHARED_FOLDER:
                //8 (aka root folder) - Route special addresses to appropriate handlers
                String folderServerAddr = extras.getString("serverAddr");
                Log.i(TAG, "GET_SHARED_FOLDER request for serverAddr: " + folderServerAddr);
                
                if (folderServerAddr != null) {
                    if (folderServerAddr.equals("ftp://root.local")) {
                        // Route to root file system access
                        result.putParcelableArrayList("sharedFolderList", FileManager.getSharedFolderRootDir(extras.getLong("serverId")));
                    } else if (folderServerAddr.equals("sftp://smb.local")) {
                        // Route to SMB/CIFS share listing
                        result.putParcelableArrayList("sharedFolderList", CifsIntegration.getCifsShares(this));
                    } else if (folderServerAddr.startsWith("cifs://") || folderServerAddr.startsWith("smb://")) {
                        // Handle actual SMB/CIFS share access
                        result.putParcelableArrayList("sharedFolderList", handleCifsRequest(extras.getLong("serverId"), folderServerAddr));
                    } else {
                        // Default file manager behavior
                        result.putParcelableArrayList("sharedFolderList", FileManager.getSharedFolderRootDir(extras.getLong("serverId")));
                    }
                } else {
                    // No server address provided, return default
                    result.putParcelableArrayList("sharedFolderList", FileManager.getSharedFolderRootDir(extras.getLong("serverId")));
                }
                result.putBoolean("result", true);
                break;
            case GET_FILE_LIST:
                //9
                //result.putParcelableArrayList("fileList", FileManager.getFileList(extras.getString("filePath"), extras.getLong("serverId")));
                result.putParcelableArrayList("fileList", FileManager.getFileListWithCache(extras.getString("filePath"), extras.getLong("serverId")));
                result.putBoolean("result", true);
                break;
            case GET_FILE_OBJECT:
                //10
                result.putParcelable("fileObject", FileManager.getFileObject(extras.getString("filePath"), extras.getLong("serverId")));
                result.putBoolean("result", true);
                break;
            case GET_STRING_MAP:
                //11
                Field[] fields = R.string.class.getDeclaredFields();
                Bundle bFiled = new Bundle();
                for (Field field : fields) {
                    try {
                        bFiled.putString(field.getName(), getResources().getString(field.getInt(null)));
                    } catch (Exception e) {
                        e.printStackTrace();
                        bFiled.putString(field.getName(), "");
                    }
                }
                result.putBundle("result", bFiled);
                break;
            case GET_RESOURCE:
                //12
                break;
            case VERIFY_SERVER_INFO:
                //13 (init server info)
                break;
            case CHECK_PERMISSION:
                //14 (before opening)
                Shell.getShell(); //request root
                break;
            case GET_SERVER_COUNT:
                //15
                break;
            case REMOVE_MONITOR:
                //16 (delete request-info)
                /*mRequestInfoMap.remove(requestInfo.mServerId);
                result.putBoolean("result", true);*/
                break;
            case REMOVE_CACHED_FILE_LIST:
                //17
                CachedFileList.clear();
                result.putBoolean("result", true);
                break;
            case CREATE_FOLDER:
                //121
                result.putBoolean("isSuccess", FileManager.newFolder(extras.getString("parentPath"), extras.getString("newName")));
                result.putBoolean("result", true);
                break;
            case RENAME:
                //122
                result.putBoolean("isSuccess", FileManager.renameFile(extras.getString("sourcePath"), extras.getString("newName")));
                result.putBoolean("result", true);
                break;
            case UPLOAD:
                //123 (copy)
                result.putBoolean("isSuccess", FileManager.copy((ParcelFileDescriptor) extras.getParcelable("fileDescriptor"), extras.getString("dstFolderPath"), extras.getString("dstFileName"), mProgressCallback, requestInfo.mServerId, requestInfo.mReqCode));
                result.putBoolean("result", true);
                break;
            case GET_FILE_DESCRIPTOR:
                //124 (click, copy, move)
                result.putParcelable("fileDescriptor", FileManager.getFileDescriptor(extras.getString("sourcePath")));
                result.putBoolean("result", true);
                break;
            case DELETE:
                //125
                result.putBoolean("isSuccess", FileManager.deleteFile(extras.getString("sourcePath")));
                result.putBoolean("result", true);
                break;
            case INTERNAL_COPY:
                //126
                result.putBoolean("isSuccess", FileManager.copy(extras.getString("sourcePath"), extras.getString("dstFolderPath"), extras.getString("dstFileName"), mProgressCallback, requestInfo.mServerId, requestInfo.mReqCode, 0).isSuccess);
                result.putBoolean("result", true);
                break;
            case INTERNAL_MOVE:
                //127
                boolean isSuccess = FileManager.copy(extras.getString("sourcePath"), extras.getString("dstFolderPath"), extras.getString("dstFileName"), mProgressCallback, requestInfo.mServerId, requestInfo.mReqCode, 0).isSuccess;
                result.putBoolean("isSuccess", isSuccess && FileManager.deleteFile(extras.getString("sourcePath")));
                result.putBoolean("result", true);
                break;
            case EXTERNAL_COPY:
                //128
                break;
            case EXTERNAL_MOVE:
                //129
                break;
            case EXIST:
                //130
                result.putBoolean("result", FileManager.exists(extras.getString("sourcePath")));
                break;
        }

        mRequestInfoMap.remove(requestInfo.mServerId);
        if (requestInfo.mCanceled.get()) return;

        try {
            this.mCallback.onSuccess(requestInfo.mServerId, requestInfo.mReqCode, result);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public IBinder onBind(Intent var1) {
        return this.mBinder;
    }

    public void onCreate() {
        if (Shell.getCachedShell() == null) {
            Shell.setDefaultBuilder(Shell.Builder.create().setFlags(Shell.FLAG_MOUNT_MASTER));
        }
        storageLocations = LocationList.loadList(this);
    }

    public void onDestroy() {
        super.onDestroy();
        LocationList.saveList(this, storageLocations);
    }

    public static class RequestInfo {
        public final AtomicBoolean mCanceled;
        public final Bundle mExtras;
        public final int mReqCode;
        public final long mServerId;
        public final String mType;

        private RequestInfo(long serverId, String type, int requestCode, Bundle extras) {
            this.mCanceled = new AtomicBoolean(false);
            this.mServerId = serverId;
            this.mType = type;
            this.mReqCode = requestCode;
            this.mExtras = extras;
        }
    }

    // This method handles SMB/CIFS requests and integrates with the CifsIntegration class.
    private ArrayList<Bundle> handleCifsRequest(long serverId, String serverAddr) {
        if (serverAddr.startsWith("cifs://")) {
            if (serverAddr.equals("cifs://add_new")) {
                // Open SMB configuration
                CifsIntegration.openCifsManager(this);
                return new ArrayList<>();
            } else {
                // Handle CIFS share access
                return CifsIntegration.accessCifsShare(this, serverId, serverAddr);
            }
        } else if (serverAddr.startsWith("smb://")) {
            // Handle SMB protocol directly
            return CifsIntegration.accessCifsShare(this, serverId, serverAddr);
        } else {
            // Fallback to default file manager behavior
            return FileManager.getSharedFolderRootDir(serverId);
        }
    }
}
    