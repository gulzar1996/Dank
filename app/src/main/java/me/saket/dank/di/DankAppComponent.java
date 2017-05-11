package me.saket.dank.di;

import com.danikula.videocache.HttpProxyCacheServer;

import javax.inject.Singleton;

import dagger.Component;
import me.saket.dank.data.DankRedditClient;
import me.saket.dank.data.DataStores;
import me.saket.dank.data.ErrorManager;
import me.saket.dank.data.SharedPrefsManager;
import me.saket.dank.data.SubredditSubscriptionManager;
import me.saket.dank.data.UserPrefsManager;
import me.saket.dank.notifs.MessagesNotificationManager;
import me.saket.dank.utils.ImgurManager;
import me.saket.dank.utils.JacksonHelper;
import okhttp3.OkHttpClient;

@Component(modules = DankAppModule.class)
@Singleton
public interface DankAppComponent {
    DankRedditClient dankRedditClient();

    SharedPrefsManager sharedPrefs();

    OkHttpClient okHttpClient();

    HttpProxyCacheServer httpProxyCacheServer();

    DankApi api();

    UserPrefsManager userPrefs();

    ImgurManager imgur();

    SubredditSubscriptionManager subredditSubscriptionManager();

    JacksonHelper jacksonHelper();

    DataStores dataStores();

    ErrorManager errorManager();

    MessagesNotificationManager messagesNotificationManager();
}
