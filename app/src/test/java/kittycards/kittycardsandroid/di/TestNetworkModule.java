package kittycards.kittycardsandroid.di;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import dagger.hilt.components.SingletonComponent;
import dagger.hilt.testing.TestInstallIn;
import kittycards.kittycardsandroid.components.FakeNetworkManager;
import kittycards.kittycardsandroid.components.IGameController;
import kittycards.kittycardsandroid.components.INetworkManager;
import kittycards.kittycardsandroid.components.IProtocolEngine;
import kittycards.kittycardsandroid.logic.GameController;
import kittycards.kittycardsandroid.network.ProtocolEngine;

@Module
@TestInstallIn(
    components = SingletonComponent.class,
    replaces = AppModule.class
)
public class TestNetworkModule {

    @Provides
    @Singleton
    public FakeNetworkManager provideFakeNetworkManager() {
        return new FakeNetworkManager();
    }

    @Provides
    @Singleton
    public INetworkManager provideINetworkManager(FakeNetworkManager fake) {
        return fake;
    }

    @Provides
    @Singleton
    public IProtocolEngine provideProtocolEngine() {
        return new ProtocolEngine();
    }

    @Provides
    @Singleton
    public IGameController provideGameController(GameController controller) {
        return controller;
    }
}
