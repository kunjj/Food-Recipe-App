package com.example.foodrecipesapplication.viewmodels

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.example.foodrecipesapplication.R
import com.example.foodrecipesapplication.models.FoodJoke
import com.example.foodrecipesapplication.models.FoodRecipe
import com.example.foodrecipesapplication.network.NetworkListener
import com.example.foodrecipesapplication.network.NetworkResponse
import com.example.foodrecipesapplication.repositories.FoodRecipesRepository
import com.example.foodrecipesapplication.room.entities.FavoriteRecipe
import com.example.foodrecipesapplication.room.entities.FoodJokeEntity
import com.example.foodrecipesapplication.room.entities.FoodRecipeEntity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import retrofit2.Response
import javax.inject.Inject

@HiltViewModel
class FoodRecipesViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val foodRecipesRepository: FoodRecipesRepository,
) : ViewModel() {
    private val networkListener: NetworkListener = NetworkListener()

    var currentUser: FirebaseUser? = Firebase.auth.currentUser

    //Room Data Base.
    val readRecipes: LiveData<List<FoodRecipeEntity>> =
        foodRecipesRepository.readDatabase().asLiveData()

    val favoriteRecipes: LiveData<List<FavoriteRecipe>> =
        foodRecipesRepository.readFavoriteRecipes().asLiveData()

    val readRandomJoke: LiveData<FoodJokeEntity> = foodRecipesRepository.readFoodJoke().asLiveData()

    private fun insertRecipesToRoomDatabase(foodRecipeEntity: FoodRecipeEntity) =
        viewModelScope.launch(Dispatchers.IO) {
            foodRecipesRepository.insertFoodRecipes(foodRecipeEntity)
        }

    fun saveFavoriteRecipe(favoriteRecipe: FavoriteRecipe) = viewModelScope.launch(Dispatchers.IO) {
        foodRecipesRepository.insertFavoriteRecipes(favoriteRecipe)
    }

    private fun saveRandomJoke(foodJokeEntity: FoodJokeEntity) =
        viewModelScope.launch(Dispatchers.IO) {
            foodRecipesRepository.saveFoodJoke(foodJokeEntity)
        }

    fun deleteFavoriteRecipe(favoriteRecipe: FavoriteRecipe) =
        viewModelScope.launch(Dispatchers.IO) {
            foodRecipesRepository.deleteFavoriteRecipe(favoriteRecipe)
        }

    fun deleteAllFavoriteRecipe() = viewModelScope.launch {
        foodRecipesRepository.deleteAllFavoriteRecipes()
    }

    // Remote Data Source.
    var foodRecipesResponse: MutableLiveData<NetworkResponse<FoodRecipe>> = MutableLiveData()

    var searchRecipesResponse: MutableLiveData<NetworkResponse<FoodRecipe>> = MutableLiveData()

    var randomFoodJoke: MutableLiveData<NetworkResponse<FoodJoke>> = MutableLiveData()

    fun getRandomRecipes(queries: Map<String, String>) = viewModelScope.launch {
        try {
            withTimeout(10000) {
                getSafeRandomRecipesApiCall(queries)
            }
        } catch (e: Exception) {
            foodRecipesResponse.value = NetworkResponse.Error(e.message.toString())
        }
    }

    fun searchFoodRecipes(searchQuery: Map<String, String>) = viewModelScope.launch {
        try {
            withTimeout(10000) {
                getSafeSearchRecipesApiCall(searchQuery)
            }
        } catch (e: Exception) {
            searchRecipesResponse.value = NetworkResponse.Error(e.message.toString())
        }
    }

    fun getFoodJoke(apiKey: String) = viewModelScope.launch {
        try {
            withTimeout(10000) {
                getSafeFoodJokeApiCall(apiKey)
            }
        } catch (e: Exception) {
            randomFoodJoke.value = NetworkResponse.Error(e.message.toString())
        }
    }

    private suspend fun getSafeFoodJokeApiCall(apiKey: String) {
        randomFoodJoke.value = NetworkResponse.Loading()
        networkListener.isConnectedToInternet(context).collect { isConnected ->
            if (isConnected) {
                val response = foodRecipesRepository.getRandomFoodJoke(apiKey)
                if (response.isSuccessful) {
                    randomFoodJoke.value = NetworkResponse.Success(response.body()!!)
                    randomFoodJoke.value!!.data?.let { FoodJokeEntity(foodJoke = it) }
                        ?.let { saveRandomJoke(it) }
                } else {
                    randomFoodJoke.value = NetworkResponse.Error(response.message())
                }
            } else randomFoodJoke.value =
                NetworkResponse.Error(context.getString(R.string.not_connected_to_internet))
        }
    }

    private suspend fun getSafeRandomRecipesApiCall(queries: Map<String, String>) {
        foodRecipesResponse.value = NetworkResponse.Loading()
        networkListener.isConnectedToInternet(context).collect { isConnected ->
            if (isConnected) {
                val response = foodRecipesRepository.getRandomRecipes(queries)
                foodRecipesResponse.value = handleResponse(response)
                val foodRecipes = foodRecipesResponse.value!!.data
                if (foodRecipes != null) insertRecipesToRoomDatabase(FoodRecipeEntity(foodRecipe = foodRecipes))
            } else foodRecipesResponse.value = NetworkResponse.Error(
                context.getString(
                    R.string.not_connected_to_internet
                )
            )
        }
    }

    private suspend fun getSafeSearchRecipesApiCall(queries: Map<String, String>) {
        searchRecipesResponse.value = NetworkResponse.Loading()
        networkListener.isConnectedToInternet(context).collect { isConnected ->
            if (isConnected) {
                val response = foodRecipesRepository.searchRecipes(queries)
                searchRecipesResponse.value = handleResponse(response)
            } else searchRecipesResponse.value = NetworkResponse.Error(
                context.getString(
                    R.string.not_connected_to_internet
                )
            )
        }
    }

    private fun handleResponse(response: Response<FoodRecipe>): NetworkResponse<FoodRecipe> {
        return when {
            response.message().contains("timeout") -> NetworkResponse.Error(
                context.getString(R.string.connection_timeout)
            )

            response.code() == 402 -> NetworkResponse.Error(
                context.getString(R.string.try_after_sometimes)
            )

            response.body()?.recipes.isNullOrEmpty() -> NetworkResponse.Error(
                context.applicationContext.getString(R.string.no_recipes_found)
            )

            response.isSuccessful -> NetworkResponse.Success(response.body()!!)

            else -> NetworkResponse.Error(response.message())
        }
    }
}