    package barter.swapify.core.network;

    import java.util.concurrent.CompletableFuture;

    public interface ConnectionChecker {
        CompletableFuture<Boolean> isConnected();
    }
