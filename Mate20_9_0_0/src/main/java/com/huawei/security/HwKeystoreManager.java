package com.huawei.security;

import android.app.ActivityThread;
import android.app.Application;
import android.content.Context;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ServiceManager;
import android.os.UserHandle;
import android.security.keystore.KeyExpiredException;
import android.security.keystore.KeyNotYetValidException;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.UserNotAuthenticatedException;
import android.util.Log;
import com.huawei.security.IHwKeystoreService.Stub;
import com.huawei.security.keymaster.HwExportResult;
import com.huawei.security.keymaster.HwKeyCharacteristics;
import com.huawei.security.keymaster.HwKeymasterArguments;
import com.huawei.security.keymaster.HwKeymasterBlob;
import com.huawei.security.keymaster.HwKeymasterCertificateChain;
import com.huawei.security.keymaster.HwKeymasterDefs;
import com.huawei.security.keymaster.HwOperationResult;
import com.huawei.security.keystore.ArrayUtils.EmptyArray;
import com.huawei.security.keystore.HwUniversalKeyStoreException;
import java.security.InvalidKeyException;

public class HwKeystoreManager {
    public static final int AUTH_TEMPLATE_ID_BIND_OVERFLOW = 100002;
    public static final int AUTH_TEMPLATE_ID_BIND_REPEAT = 100001;
    public static final int AUTH_TYPE_UNSUPPORT = 100000;
    public static final int FLAG_ENCRYPTED = 1;
    public static final int FLAG_NONE = 0;
    public static final String HW_KEYSTORE_SDK_VERSION = "9.0.0.3";
    public static final int KEY_COUNT_OVERFLOW = 100;
    public static final int KEY_NOT_FOUND = 7;
    public static final int KM_KEY_FORMAT_X509 = 0;
    public static final int LOCKED = 2;
    public static final int NOT_SUPPORT = 101;
    public static final int NO_ERROR = 1;
    public static final int OP_AUTH_NEEDED = 15;
    public static final int PERMISSION_DENIED = 6;
    public static final int PROTOCOL_ERROR = 5;
    public static final int SYSTEM_ERROR = 4;
    private static final String TAG = "HwKeystoreManager";
    public static final int UID_SELF = -1;
    public static final int UNDEFINED_ACTION = 9;
    public static final int UNINITIALIZED = 3;
    public static final int VALUE_CORRUPTED = 8;
    public static final int VERSION_ERROR = -1;
    public static final int WRONG_PASSWORD = 10;
    private static HwKeystoreManager mHwKeystoreManager = null;
    private final IHwKeystoreService mBinder;
    private int mError = 1;
    private IBinder mToken;

    public enum State {
        UNLOCKED,
        LOCKED,
        UNINITIALIZED
    }

    private HwKeystoreManager(IHwKeystoreService binder) {
        this.mBinder = binder;
    }

    public static Context getApplicationContext() {
        Application application = ActivityThread.currentApplication();
        if (application != null) {
            return application;
        }
        throw new IllegalStateException("Failed to obtain application Context from ActivityThread");
    }

    public static HwKeystoreManager getInstance() {
        IHwKeystoreService binder = Stub.asInterface(ServiceManager.getService("com.huawei.security.IHwKeystoreService"));
        if (binder != null) {
            return new HwKeystoreManager(binder);
        }
        Log.e(TAG, "getInstance IHwKeystoreService binder is null");
        return null;
    }

    public static String getHwKeystoreSdkVersion() {
        return HW_KEYSTORE_SDK_VERSION;
    }

    public State state(int userId) {
        return State.UNLOCKED;
    }

    public State state() {
        return state(UserHandle.myUserId());
    }

    public boolean put(String key, byte[] value, int uid, int flags) {
        return insert(key, value, uid, flags) == 1;
    }

    public int insert(String key, byte[] value, int uid, int flags) {
        return set(key, new HwKeymasterBlob(value), uid);
    }

    public boolean delete(String key, int uid) {
        Log.i(TAG, "delete");
        boolean z = false;
        try {
            int ret = this.mBinder.del(key, uid);
            if (ret == 1 || ret == 7) {
                z = true;
            }
            return z;
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to HwKeystoreManager", e);
            return false;
        }
    }

    public boolean delete(String key) {
        return delete(key, -1);
    }

    public boolean contains(String key, int uid) {
        if (key == null || key.isEmpty()) {
            Log.e(TAG, "contains key is null");
            return false;
        }
        HwExportResult result = null;
        if (key.contains(HwCredentials.USER_PRIVATE_KEY)) {
            result = exportKey(key, 0, null, null, uid);
        } else if (key.contains(HwCredentials.CERTIFICATE_CHAIN)) {
            result = get(key, uid);
        }
        if (result == null || result.resultCode != 1) {
            return false;
        }
        Log.i(TAG, "contains return true");
        return true;
    }

    public boolean contains(String key) {
        return contains(key, -1);
    }

    public String[] list(String prefix, int uid) {
        return EmptyArray.STRING;
    }

    public String[] list(String prefix) {
        return list(prefix, -1);
    }

    public int getLastError() {
        return this.mError;
    }

    public int generateKey(String alias, HwKeymasterArguments args, byte[] entropy, int uid, int flags, HwKeyCharacteristics outCharacteristics) {
        Log.i(TAG, "generateKey");
        try {
            return this.mBinder.generateKey(alias, args, entropy, uid, flags, outCharacteristics);
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to HwKeystoreManager", e);
            return 4;
        }
    }

    public int generateKey(String alias, HwKeymasterArguments args, byte[] entropy, int flags, HwKeyCharacteristics outCharacteristics) {
        return generateKey(alias, args, entropy, -1, flags, outCharacteristics);
    }

    public int getKeyCharacteristics(String alias, HwKeymasterBlob clientId, HwKeymasterBlob appId, int uid, HwKeyCharacteristics outCharacteristics) {
        Log.i(TAG, "getKeyCharacteristics");
        try {
            return this.mBinder.getKeyCharacteristics(alias, clientId, appId, uid, outCharacteristics);
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to HwKeystoreManager", e);
            return 4;
        }
    }

    public int getKeyCharacteristics(String alias, HwKeymasterBlob clientId, HwKeymasterBlob appId, HwKeyCharacteristics outCharacteristics) {
        return getKeyCharacteristics(alias, clientId, appId, -1, outCharacteristics);
    }

    public HwExportResult exportKey(String alias, int format, HwKeymasterBlob clientId, HwKeymasterBlob appId, int uid) {
        Log.i(TAG, "exportKey");
        try {
            return this.mBinder.exportKey(alias, format, clientId, appId, uid);
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to HwKeystoreManager", e);
            return null;
        }
    }

    public HwExportResult exportKey(String alias, int format, HwKeymasterBlob clientId, HwKeymasterBlob appId) {
        return exportKey(alias, format, clientId, appId, -1);
    }

    public HwOperationResult begin(String alias, int purpose, boolean pruneable, HwKeymasterArguments args, byte[] entropy, int uid) {
        Log.i(TAG, "begin");
        try {
            return this.mBinder.begin(getToken(), alias, purpose, pruneable, args, entropy, uid);
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to HwKeystoreManager", e);
            return null;
        }
    }

    public HwOperationResult begin(String alias, int purpose, boolean pruneable, HwKeymasterArguments args, byte[] entropy) {
        return begin(alias, purpose, pruneable, args, entropy, -1);
    }

    public HwOperationResult update(IBinder token, HwKeymasterArguments arguments, byte[] input) {
        Log.i(TAG, "update");
        try {
            return this.mBinder.update(token, arguments, input);
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to HwKeystoreManager", e);
            return null;
        }
    }

    public HwOperationResult finish(IBinder token, HwKeymasterArguments arguments, byte[] signature, byte[] entropy) {
        Log.i(TAG, "finish");
        try {
            return this.mBinder.finish(token, arguments, signature, entropy);
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to HwKeystoreManager", e);
            return null;
        }
    }

    public HwOperationResult finish(IBinder token, HwKeymasterArguments arguments, byte[] signature) {
        return finish(token, arguments, signature, null);
    }

    public int abort(IBinder token) {
        Log.i(TAG, "abort");
        try {
            return this.mBinder.abort(token);
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to HwKeystoreManager", e);
            return 4;
        }
    }

    public int attestKey(String alias, int uid, HwKeymasterArguments params, HwKeymasterCertificateChain outChain) {
        Log.i(TAG, "attestKey");
        try {
            return this.mBinder.attestKey(alias, uid, params, outChain);
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to HwKeystoreManager", e);
            return 4;
        }
    }

    public int attestDeviceIds(HwKeymasterArguments params, HwKeymasterCertificateChain outChain) {
        Log.i(TAG, "attestDeviceIds() was called");
        try {
            return this.mBinder.attestDeviceIds(params, outChain);
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to HwKeystoreManager", e);
            return 4;
        }
    }

    public int assetHandleReq(HwKeymasterArguments params, HwKeymasterCertificateChain outResult) {
        try {
            return this.mBinder.assetHandleReq(params, outResult);
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to HwKeystoreManager", e);
            return -1;
        }
    }

    private synchronized IBinder getToken() {
        if (this.mToken == null) {
            this.mToken = new Binder();
        }
        return this.mToken;
    }

    public HwExportResult get(String alias, int uid) {
        try {
            Log.i(TAG, "get");
            return this.mBinder.get(alias, uid);
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to HwKeystoreManager", e);
            return null;
        }
    }

    public int set(String alias, HwKeymasterBlob blob, int uid) {
        try {
            Log.i(TAG, "set");
            return this.mBinder.set(alias, blob, uid);
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to HwKeystoreManager", e);
            return 4;
        }
    }

    public String getHuksServiceVersion() {
        try {
            Log.i(TAG, "getHuksServiceVersion");
            return this.mBinder.getHuksServiceVersion();
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to HwKeystoreManager", e);
            return "Get Huks service version failed!";
        }
    }

    public int exportTrustCert(HwKeymasterCertificateChain outChain) {
        try {
            Log.i(TAG, "exportTrustCert");
            return this.mBinder.exportTrustCert(outChain);
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to HwKeystoreManager", e);
            return 4;
        }
    }

    public int setKeyProtection(String alias, HwKeymasterArguments args) {
        try {
            Log.i(TAG, "setKeyProtection");
            return this.mBinder.setKeyProtection(alias, args);
        } catch (RemoteException e) {
            Log.w(TAG, "Cannot connect to HwKeystoreManager", e);
            return 4;
        }
    }

    public static HwUniversalKeyStoreException getKeyStoreException(int errorCode) {
        if (errorCode > 0) {
            if (errorCode == 15) {
                return new HwUniversalKeyStoreException(errorCode, "Operation requires authorization");
            }
            switch (errorCode) {
                case 1:
                    return new HwUniversalKeyStoreException(errorCode, "OK");
                case 2:
                    return new HwUniversalKeyStoreException(errorCode, "User authentication required");
                case 3:
                    return new HwUniversalKeyStoreException(errorCode, "Keystore not initialized");
                case 4:
                    return new HwUniversalKeyStoreException(errorCode, "System error");
                default:
                    switch (errorCode) {
                        case 6:
                            return new HwUniversalKeyStoreException(errorCode, "Permission denied");
                        case KEY_NOT_FOUND /*7*/:
                            return new HwUniversalKeyStoreException(errorCode, "Key not found");
                        case 8:
                            return new HwUniversalKeyStoreException(errorCode, "Key blob corrupted");
                        default:
                            switch (errorCode) {
                                case KEY_COUNT_OVERFLOW /*100*/:
                                    return new HwUniversalKeyStoreException(errorCode, "Key count is overflowed");
                                case NOT_SUPPORT /*101*/:
                                    return new HwUniversalKeyStoreException(errorCode, "Not support HwPKI");
                                default:
                                    switch (errorCode) {
                                        case AUTH_TYPE_UNSUPPORT /*100000*/:
                                            return new HwUniversalKeyStoreException(errorCode, "Auth type unsupport");
                                        case AUTH_TEMPLATE_ID_BIND_REPEAT /*100001*/:
                                            return new HwUniversalKeyStoreException(errorCode, "Bound auth template ID repeat");
                                        case AUTH_TEMPLATE_ID_BIND_OVERFLOW /*100002*/:
                                            return new HwUniversalKeyStoreException(errorCode, "Bound auth template ID overflow");
                                        default:
                                            return new HwUniversalKeyStoreException(errorCode, String.valueOf(errorCode));
                                    }
                            }
                    }
            }
        } else if (errorCode != -16) {
            return new HwUniversalKeyStoreException(errorCode, HwKeymasterDefs.getErrorMessage(errorCode));
        } else {
            return new HwUniversalKeyStoreException(errorCode, "Invalid user authentication validity duration");
        }
    }

    public InvalidKeyException getInvalidKeyException(String keystoreKeyAlias, int uid, HwUniversalKeyStoreException e) {
        int errorCode = e.getErrorCode();
        if (errorCode == 2) {
            return new UserNotAuthenticatedException();
        }
        if (errorCode != 15) {
            switch (errorCode) {
                case HwKeymasterDefs.KM_ERROR_KEY_USER_NOT_AUTHENTICATED /*-26*/:
                    break;
                case HwKeymasterDefs.KM_ERROR_KEY_EXPIRED /*-25*/:
                    return new KeyExpiredException();
                case HwKeymasterDefs.KM_ERROR_KEY_NOT_YET_VALID /*-24*/:
                    return new KeyNotYetValidException();
                default:
                    return new InvalidKeyException("Keystore operation failed", e);
            }
        }
        HwKeyCharacteristics keyCharacteristics = new HwKeyCharacteristics();
        int getKeyCharacteristicsErrorCode = getKeyCharacteristics(keystoreKeyAlias, null, null, uid, keyCharacteristics);
        if (getKeyCharacteristicsErrorCode != 1) {
            return new InvalidKeyException("Failed to obtained key characteristics", getKeyStoreException(getKeyCharacteristicsErrorCode));
        }
        if (keyCharacteristics.getUnsignedLongs(HwKeymasterDefs.KM_TAG_USER_SECURE_ID).isEmpty()) {
            return new KeyPermanentlyInvalidatedException();
        }
        return new KeyPermanentlyInvalidatedException();
    }

    public InvalidKeyException getInvalidKeyException(String keystoreKeyAlias, int uid, int errorCode) {
        return getInvalidKeyException(keystoreKeyAlias, uid, getKeyStoreException(errorCode));
    }
}
