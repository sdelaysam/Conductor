package com.bluelinelabs.conductor.internal.lifecyclehandler;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Application.ActivityLifecycleCallbacks;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.SparseArray;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.ViewGroup;

import com.bluelinelabs.conductor.ActivityHostedRouter;
import com.bluelinelabs.conductor.Router;
import com.bluelinelabs.conductor.internal.StringSparseArrayParceler;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LifecycleHandler implements ActivityLifecycleCallbacks {

    private static final String ANDROID_X_ACTIVITY = "androidx.appcompat.app.FragmentActivity";
    private static final String SUPPORT_ACTIVITY = "android.support.v4.app.FragmentActivity";

    private static final String KEY_PENDING_PERMISSION_REQUESTS = "LifecycleHelper.pendingPermissionRequests";
    private static final String KEY_PERMISSION_REQUEST_CODES = "LifecycleHelper.permissionRequests";
    private static final String KEY_ACTIVITY_REQUEST_CODES = "LifecycleHelper.activityRequests";
    private static final String KEY_ROUTER_STATE_PREFIX = "LifecycleHelper.routerState";

    private Activity activity;
    private boolean hasRegisteredCallbacks;
    private boolean destroyed;
    private boolean attached;
    private boolean hasPreparedForHostDetach;

    private static final Map<Activity, LifecycleHandler> activeLifecycleHandlers = new HashMap<>();
    private SparseArray<String> permissionRequestMap = new SparseArray<>();
    private SparseArray<String> activityRequestMap = new SparseArray<>();
    private ArrayList<PendingPermissionRequest> pendingPermissionRequests = new ArrayList<>();

    private final Map<Integer, ActivityHostedRouter> routerMap = new HashMap<>();
    private final LifecycleHelper helper;

    @Nullable
    private static LifecycleHandler findInActivity(@NonNull Activity activity) {
        LifecycleHandler lifecycleHandler = activeLifecycleHandlers.get(activity);
        if (lifecycleHandler == null) {
            lifecycleHandler = findHandler(activity);
        }
        if (lifecycleHandler != null) {
            lifecycleHandler.registerActivityListener(activity);
        }
        return lifecycleHandler;
    }

    @NonNull
    public static LifecycleHandler install(@NonNull Activity activity) {
        LifecycleHandler lifecycleHandler = findInActivity(activity);
        if (lifecycleHandler == null) {
            lifecycleHandler = installHandler(activity);
        }
        lifecycleHandler.registerActivityListener(activity);
        return lifecycleHandler;
    }

    LifecycleHandler(@NonNull LifecycleHelper lifecycleHelper) {
        helper = lifecycleHelper;
    }

    @NonNull
    public Router getRouter(@NonNull ViewGroup container, @Nullable Bundle savedInstanceState) {
        ActivityHostedRouter router = routerMap.get(getRouterHashKey(container));
        if (router == null) {
            router = new ActivityHostedRouter();
            router.setHost(this, container);

            if (savedInstanceState != null) {
                Bundle routerSavedState = savedInstanceState.getBundle(KEY_ROUTER_STATE_PREFIX + router.getContainerId());
                if (routerSavedState != null) {
                    router.restoreInstanceState(routerSavedState);
                }
            }
            routerMap.put(getRouterHashKey(container), router);
        } else {
            router.setHost(this, container);
        }

        return router;
    }

    @NonNull
    public List<Router> getRouters() {
        return new ArrayList<Router>(routerMap.values());
    }

    @Nullable
    public Activity getLifecycleActivity() {
        return activity;
    }

    private static int getRouterHashKey(@NonNull ViewGroup viewGroup) {
        return viewGroup.getId();
    }

    private void registerActivityListener(@NonNull Activity activity) {
        this.activity = activity;

        if (!hasRegisteredCallbacks) {
            hasRegisteredCallbacks = true;
            activity.getApplication().registerActivityLifecycleCallbacks(this);

            // Since Fragment transactions are async, we have to keep an <Activity, LifecycleHelper> map in addition
            // to trying to find the LifecycleHelper fragment in the Activity to handle the case of the developer
            // trying to immediately get > 1 router in the same Activity. See issue #299.
            activeLifecycleHandlers.put(activity, this);
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            StringSparseArrayParceler permissionParcel = savedInstanceState.getParcelable(KEY_PERMISSION_REQUEST_CODES);
            permissionRequestMap = permissionParcel != null ? permissionParcel.getStringSparseArray() : new SparseArray<String>();

            StringSparseArrayParceler activityParcel = savedInstanceState.getParcelable(KEY_ACTIVITY_REQUEST_CODES);
            activityRequestMap = activityParcel != null ? activityParcel.getStringSparseArray() : new SparseArray<String>();

            ArrayList<PendingPermissionRequest> pendingRequests = savedInstanceState.getParcelableArrayList(KEY_PENDING_PERMISSION_REQUESTS);
            pendingPermissionRequests = pendingRequests != null ? pendingRequests : new ArrayList<PendingPermissionRequest>();
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        outState.putParcelable(KEY_PERMISSION_REQUEST_CODES, new StringSparseArrayParceler(permissionRequestMap));
        outState.putParcelable(KEY_ACTIVITY_REQUEST_CODES, new StringSparseArrayParceler(activityRequestMap));
        outState.putParcelableArrayList(KEY_PENDING_PERMISSION_REQUESTS, pendingPermissionRequests);
    }

    public void onDestroy() {
        if (activity != null) {
            activity.getApplication().unregisterActivityLifecycleCallbacks(this);
            activeLifecycleHandlers.remove(activity);
            destroyRouters();
            activity = null;
        }
    }

    public void onAttach() {
        destroyed = false;
        setAttached();
    }

    public void onDetach() {
        attached = false;
        destroyRouters();
    }

    private void setAttached() {
        if (!attached) {
            attached = true;

            for (int i = pendingPermissionRequests.size() - 1; i >= 0; i--) {
                PendingPermissionRequest request = pendingPermissionRequests.remove(i);
                requestPermissions(request.instanceId, request.permissions, request.requestCode);
            }
        }

        for (ActivityHostedRouter router : new ArrayList<>(routerMap.values())) {
            router.onContextAvailable();
        }
    }

    private void destroyRouters() {
        if (!destroyed) {
            destroyed = true;

            if (activity != null) {
                for (Router router : getRouters()) {
                    router.onActivityDestroyed(activity);
                }
            }
        }
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        String instanceId = activityRequestMap.get(requestCode);
        if (instanceId != null) {
            for (Router router : getRouters()) {
                router.onActivityResult(instanceId, requestCode, resultCode, data);
            }
        }
    }

    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        String instanceId = permissionRequestMap.get(requestCode);
        if (instanceId != null) {
            for (Router router : getRouters()) {
                router.onRequestPermissionsResult(instanceId, requestCode, permissions, grantResults);
            }
        }
    }

    public Boolean shouldShowRequestPermissionRationale(@NonNull String permission) {
        for (Router router : getRouters()) {
            Boolean handled = router.handleRequestedPermission(permission);
            if (handled != null) {
                return handled;
            }
        }

        return null;
    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        for (Router router : getRouters()) {
            router.onCreateOptionsMenu(menu, inflater);
        }
    }

    public void onPrepareOptionsMenu(Menu menu) {
        for (Router router : getRouters()) {
            router.onPrepareOptionsMenu(menu);
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        for (Router router : getRouters()) {
            if (router.onOptionsItemSelected(item)) {
                return true;
            }
        }

        return false;
    }

    private static boolean useAndroidX(@NonNull Activity activity) {
        Class cls = activity.getClass().getSuperclass();

        while (cls != null) {
            if (cls.getCanonicalName().equals(ANDROID_X_ACTIVITY) || cls.getCanonicalName().equals(SUPPORT_ACTIVITY)) {
                return true;
            }
            cls = cls.getSuperclass();
        }

        return false;
    }

    @Nullable
    private static LifecycleHandler findHandler(@NonNull Activity activity) {
        LifecycleHelper helper;

        if (useAndroidX(activity)) {
            helper = AndroidXHelper.findInActivity(activity);
        } else {
            helper = PlatformHelper.findInActivity(activity);
        }

        return helper != null ? helper.lifecycleHandler() : null;
    }

    private static LifecycleHandler installHandler(@NonNull Activity activity) {
        LifecycleHelper helper = useAndroidX(activity) ? new AndroidXHelper() : new PlatformHelper();
        helper.install(activity);
        return helper.lifecycleHandler();
    }

    public void registerForActivityResult(@NonNull String instanceId, int requestCode) {
        activityRequestMap.put(requestCode, instanceId);
    }

    public void unregisterForActivityResults(@NonNull String instanceId) {
        for (int i = activityRequestMap.size() - 1; i >= 0; i--) {
            if (instanceId.equals(activityRequestMap.get(activityRequestMap.keyAt(i)))) {
                activityRequestMap.removeAt(i);
            }
        }
    }

    public void startActivity(@NonNull Intent intent) {
        helper.startActivity(intent);
    }

    public void startActivityForResult(@NonNull String instanceId, @NonNull Intent intent, int requestCode) {
        registerForActivityResult(instanceId, requestCode);
        helper.startActivityForResult(intent, requestCode);
    }

    public void startActivityForResult(@NonNull String instanceId, @NonNull Intent intent, int requestCode, @Nullable Bundle options) {
        registerForActivityResult(instanceId, requestCode);
        helper.startActivityForResult(intent, requestCode, options);
    }

    @TargetApi(Build.VERSION_CODES.N)
    public void startIntentSenderForResult(@NonNull String instanceId, @NonNull IntentSender intent, int requestCode,
                                           @Nullable Intent fillInIntent, int flagsMask, int flagsValues, int extraFlags,
                                           @Nullable Bundle options) throws IntentSender.SendIntentException {
        registerForActivityResult(instanceId, requestCode);
        helper.startIntentSenderForResult(intent, requestCode, fillInIntent, flagsMask, flagsValues, extraFlags, options);
    }

    @TargetApi(Build.VERSION_CODES.M)
    public void requestPermissions(@NonNull String instanceId, @NonNull String[] permissions, int requestCode) {
        if (attached) {
            permissionRequestMap.put(requestCode, instanceId);
            helper.requestPermissions(permissions, requestCode);
        } else {
            pendingPermissionRequests.add(new PendingPermissionRequest(instanceId, permissions, requestCode));
        }
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        if (this.activity == null && findInActivity(activity) == this) {
            this.activity = activity;

            for (ActivityHostedRouter router : new ArrayList<>(routerMap.values())) {
                router.onContextAvailable();
            }
        }
    }

    @Override
    public void onActivityStarted(Activity activity) {
        if (this.activity == activity) {
            hasPreparedForHostDetach = false;

            for (Router router : getRouters()) {
                router.onActivityStarted(activity);
            }
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (this.activity == activity) {
            for (Router router : getRouters()) {
                router.onActivityResumed(activity);
            }
        }
    }

    @Override
    public void onActivityPaused(Activity activity) {
        if (this.activity == activity) {
            for (Router router : getRouters()) {
                router.onActivityPaused(activity);
            }
        }
    }

    @Override
    public void onActivityStopped(Activity activity) {
        if (this.activity == activity) {
            prepareForHostDetachIfNeeded();

            for (Router router : getRouters()) {
                router.onActivityStopped(activity);
            }
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        if (this.activity == activity) {
            prepareForHostDetachIfNeeded();

            for (Router router : getRouters()) {
                Bundle bundle = new Bundle();
                router.saveInstanceState(bundle);
                outState.putBundle(KEY_ROUTER_STATE_PREFIX + router.getContainerId(), bundle);
            }
        }
    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        activeLifecycleHandlers.remove(activity);
    }

    public void invalidateOptionsMenu() {
        helper.invalidateOptionsMenu();
    }

    private void prepareForHostDetachIfNeeded() {
        if (!hasPreparedForHostDetach) {
            hasPreparedForHostDetach = true;

            for (Router router : getRouters()) {
                router.prepareForHostDetach();
            }
        }
    }

    private static class PendingPermissionRequest implements Parcelable {
        final String instanceId;
        final String[] permissions;
        final int requestCode;

        PendingPermissionRequest(@NonNull String instanceId, @NonNull String[] permissions, int requestCode) {
            this.instanceId = instanceId;
            this.permissions = permissions;
            this.requestCode = requestCode;
        }

        PendingPermissionRequest(Parcel in) {
            instanceId = in.readString();
            permissions = in.createStringArray();
            requestCode = in.readInt();
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel out, int flags) {
            out.writeString(instanceId);
            out.writeStringArray(permissions);
            out.writeInt(requestCode);
        }

        public static final Parcelable.Creator<PendingPermissionRequest> CREATOR = new Parcelable.Creator<PendingPermissionRequest>() {
            @Override
            public PendingPermissionRequest createFromParcel(Parcel in) {
                return new PendingPermissionRequest(in);
            }

            @Override
            public PendingPermissionRequest[] newArray(int size) {
                return new PendingPermissionRequest[size];
            }
        };

    }
}
