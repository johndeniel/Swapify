package barter.swapify.core.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import java.lang.ref.WeakReference;
import java.util.concurrent.CompletableFuture;

import javax.inject.Inject;

public class ConnectionCheckerImpl implements ConnectionChecker {

    private final Context context;
    private WeakReference<ConnectivityManager> connectivityManager;

    @Inject
    public ConnectionCheckerImpl(Context context) {
        this.context = context.getApplicationContext();
    }

    @Override
    public CompletableFuture<Boolean> isConnected() {
        CompletableFuture<Boolean> future = new CompletableFuture<>();
        ConnectivityManager manager = getConnectivityManager();
        if (manager != null) {
            NetworkInfo networkInfo = manager.getActiveNetworkInfo();
            boolean isConnected = networkInfo != null && networkInfo.isConnected();
            future.complete(isConnected);
        } else {
            future.complete(false);
        }
        return future;
    }

    private ConnectivityManager getConnectivityManager() {
        ConnectivityManager manager = null;
        if (connectivityManager != null) {
            manager = connectivityManager.get();
        }
        if (manager == null) {
            manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            if (manager != null) {
                connectivityManager = new WeakReference<>(manager);
            }
        }
        return manager;
    }
}
