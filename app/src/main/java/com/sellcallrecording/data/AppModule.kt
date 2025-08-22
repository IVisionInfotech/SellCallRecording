package com.sellcallrecording.data

import android.content.Context
import android.content.SharedPreferences
import android.preference.PreferenceManager
import com.sellcallrecording.data.network.RetrofitClient
import com.sellcallrecording.util.Session
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Named

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    @Provides
    @Singleton
    fun provideSession(sharedPreferences: SharedPreferences): Session {
        return Session(sharedPreferences)
    }

    @Provides
    @Singleton
    @Named("baseUrl")
    fun provideBaseUrl(session: Session): String {
        return session.getString("baseUrl", "")!!
    }

    @Provides
    @Singleton
    @Named("token")
    fun provideToken(session: Session): String {
        return session.getString("token", "")!!
    }

    @Provides
    @Singleton
    fun provideRetrofitClient(@Named("baseUrl") baseUrl: String): RetrofitClient {
        return RetrofitClient(baseUrl)
    }
}

