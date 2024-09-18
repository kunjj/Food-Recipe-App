package com.example.foodrecipesapplication.di

import androidx.credentials.GetCredentialRequest
import com.example.foodrecipesapplication.utils.Constant.SERVER_CLIENT_ID
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped

@Module
@InstallIn(ActivityComponent::class)
object AuthenticationModule {

    @ActivityScoped
    @Provides
    fun getGoogleIdOptions(): GetGoogleIdOption =
        GetGoogleIdOption.Builder().setFilterByAuthorizedAccounts(false)
            .setServerClientId(serverClientId = SERVER_CLIENT_ID).setAutoSelectEnabled(true).build()

    @ActivityScoped
    @Provides
    fun getCredentialRequest(googleOptionId: GetGoogleIdOption): GetCredentialRequest =
        GetCredentialRequest.Builder().addCredentialOption(credentialOption = googleOptionId)
            .build()
}