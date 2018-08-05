package com.bluelinelabs.conductor.internal.lifecyclehandler;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

public class PlatformHelper extends Fragment implements LifecycleHelper {

    private final LifecycleHandler lifecycleHandler;

    public PlatformHelper() {
        setRetainInstance(true);
        setHasOptionsMenu(true);
        lifecycleHandler = new LifecycleHandler(this);
    }

    @Override
    public void install(@NonNull Activity activity) {
        activity.getFragmentManager().beginTransaction().add(this, FRAGMENT_TAG).commit();
    }

    @Nullable
    static PlatformHelper findInActivity(@NonNull Activity activity) {
        return (PlatformHelper) activity.getFragmentManager().findFragmentByTag(FRAGMENT_TAG);
    }

    @Override
    public void onCreate(@Nullable Bundle bundle) {
        super.onCreate(bundle);

        lifecycleHandler.onCreate(bundle);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        lifecycleHandler.onSaveInstanceState(outState);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        lifecycleHandler.onDestroy();
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);

        lifecycleHandler.onAttach();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        lifecycleHandler.onAttach();
    }

    @Override
    public void onDetach() {
        super.onDetach();

        lifecycleHandler.onDetach();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        lifecycleHandler.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        lifecycleHandler.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public boolean shouldShowRequestPermissionRationale(@NonNull String permission) {
        Boolean handled = lifecycleHandler.shouldShowRequestPermissionRationale(permission);

        return handled != null ? handled : super.shouldShowRequestPermissionRationale(permission);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        lifecycleHandler.onCreateOptionsMenu(menu, inflater);
    }

    @Override
    public void onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        lifecycleHandler.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        return lifecycleHandler.onOptionsItemSelected(item) || super.onOptionsItemSelected(item);
    }

    @Override
    public LifecycleHandler lifecycleHandler() {
        return lifecycleHandler;
    }

    @Override
    public void invalidateOptionsMenu() {
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager != null) {
            fragmentManager.invalidateOptionsMenu();
        }
    }

}
