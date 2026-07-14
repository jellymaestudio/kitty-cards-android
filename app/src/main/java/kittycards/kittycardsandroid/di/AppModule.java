package kittycards.kittycardsandroid.di;

import javax.inject.Singleton;

import dagger.Binds;
import dagger.Module;
import dagger.hilt.InstallIn;
import dagger.hilt.components.SingletonComponent;
import kittycards.kittycardsandroid.components.IGameController;
import kittycards.kittycardsandroid.components.INetworkManager;
import kittycards.kittycardsandroid.components.IProtocolEngine;
import kittycards.kittycardsandroid.logic.GameController;
import kittycards.kittycardsandroid.network.NetworkManager;
import kittycards.kittycardsandroid.network.ProtocolEngine;

@Module
@InstallIn(SingletonComponent.class)
public abstract class AppModule {

    @Binds
    @Singleton
    public abstract IGameController bindGameController(GameController gameController);

    @Binds
    @Singleton
    public abstract INetworkManager bindNetworkManager(NetworkManager networkManager);

    @Binds
    @Singleton
    public abstract IProtocolEngine bindProtocolEngine(ProtocolEngine protocolEngine);
}
