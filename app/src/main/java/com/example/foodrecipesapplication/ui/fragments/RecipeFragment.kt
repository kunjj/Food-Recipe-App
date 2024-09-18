package com.example.foodrecipesapplication.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.widget.SearchView
import androidx.core.view.GravityCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.example.foodrecipesapplication.R
import com.example.foodrecipesapplication.adapters.FoodRecipeAdapter
import com.example.foodrecipesapplication.databinding.DrawerHeaderBinding
import com.example.foodrecipesapplication.databinding.FragmentRecipeBinding
import com.example.foodrecipesapplication.network.NetworkResponse
import com.example.foodrecipesapplication.ui.activities.RecipeActivity
import com.example.foodrecipesapplication.utils.observeOnce
import com.google.android.material.navigation.NavigationView
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class RecipeFragment : BaseFragment(), View.OnClickListener,
    NavigationView.OnNavigationItemSelectedListener {
    private val args by navArgs<RecipeFragmentArgs>()
    private var binding: FragmentRecipeBinding? = null
    private var drawerBinding: DrawerHeaderBinding? = null
    private val foodRecipesViewModel by lazy { (activity as RecipeActivity).foodRecipesViewModel }
    private val recipeViewModel by lazy { (activity as RecipeActivity).recipeViewModel }
    private val foodRecipeAdapter by lazy { FoodRecipeAdapter() }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?,
    ): View {
        this.binding = FragmentRecipeBinding.inflate(inflater)
        this.binding!!.recipesViewModel = this.foodRecipesViewModel
        this.drawerBinding = DrawerHeaderBinding.bind(this.binding!!.navigationView.getHeaderView(0))
        this.binding!!.lifecycleOwner = this
        setHasOptionsMenu(true)
        (activity as RecipeActivity).setSupportActionBar(binding!!.toolbar)
        (activity as RecipeActivity).supportActionBar?.setDisplayShowTitleEnabled(false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setUpRecyclerView()
        setUpDrawer()
        fetchDataFromDatabase()
        binding!!.btnFilterRecipe.setOnClickListener(this)
    }

    private fun setUpRecyclerView() = binding!!.recyclerView.apply {
        adapter = this@RecipeFragment.foodRecipeAdapter
        layoutManager = LinearLayoutManager(requireActivity().applicationContext)
        showShimmerEffect()
    }

    private fun setUpDrawer() {
        this.binding!!.navigationView.setNavigationItemSelectedListener(this)
        val toggle = ActionBarDrawerToggle(
            requireActivity(),
            this.binding!!.drawerLayout,
            this.binding!!.toolbar,
            R.string.open_nav,
            R.string.close_nav
        )
        this.binding!!.drawerLayout.addDrawerListener(toggle)
        this.drawerBinding!!.recipesViewModel = this.foodRecipesViewModel
        toggle.syncState()
    }

    private fun fetchDataFromDatabase() = lifecycleScope.launch {
        delay(750)
        foodRecipesViewModel.readRecipes.observeOnce {
            if (it.isNotEmpty() && !args.fromRecipeFilterFragment) {
                foodRecipeAdapter.recipes.submitList(it[0].foodRecipe.recipes.toList())
                stopShimmerEffect()
            } else getRandomRecipesFromApi()
        }
    }

    private fun loadDataFromCache() = lifecycleScope.launch {
        delay(750)
        foodRecipesViewModel.readRecipes.observe(requireActivity()) {
            if (it.isNotEmpty()) {
                foodRecipeAdapter.recipes.submitList(it[0].foodRecipe.recipes.toList())
                stopShimmerEffect()
            }
        }
    }

    private fun getRandomRecipesFromApi() = apply {
        foodRecipesViewModel.getRandomRecipes(recipeViewModel.queries())
        foodRecipesViewModel.foodRecipesResponse.observe(viewLifecycleOwner) { response ->
            when (response) {
                is NetworkResponse.Success -> {
                    stopShimmerEffect()
                    response.data!!.let { foodRecipeAdapter.recipes.submitList(it.recipes.toList()) }
                }

                is NetworkResponse.Error -> {
                    stopShimmerEffect()
                    loadDataFromCache()
                    if (response.message != requireActivity().getString(R.string.not_connected_to_internet)) response.message?.let {
                        showSnackBar(requireView(), it)
                    }
                }

                is NetworkResponse.Loading -> showShimmerEffect()
            }
        }
    }

    private fun searchFoodRecipes(searchQuery: String) = apply {
        foodRecipesViewModel.searchFoodRecipes(recipeViewModel.queries(searchQuery))
        foodRecipesViewModel.searchRecipesResponse.observe(viewLifecycleOwner) { response ->
            when (response) {
                is NetworkResponse.Success -> {
                    stopShimmerEffect()
                    response.data?.let { data -> foodRecipeAdapter.recipes.submitList(data.recipes) }
                }

                is NetworkResponse.Error -> {
                    stopShimmerEffect()
                    loadDataFromCache()
                    if (response.message != requireActivity().getString(R.string.not_connected_to_internet)) response.message?.let {
                        showSnackBar(requireView(), it)
                    }
                }

                is NetworkResponse.Loading -> showShimmerEffect()
            }
        }
    }

    private fun showShimmerEffect() {
        binding?.progressBar?.visibility = View.VISIBLE
    }

    private fun stopShimmerEffect() {
        binding?.progressBar?.visibility = View.GONE
    }

    override fun onClick(view: View?) {
        when (view!!.id) {
            R.id.btnFilterRecipe -> {
                // There are 2 ways to navigate from one fragment to another.
                //1.findNavController().navigate(RecipeFragmentDirections.actionRecipeFragmentToRecipesFilterFragment())
                //2.findNavController().navigate(R.id.action_recipeFragment_to_recipesFilterFragment)
                if (recipeViewModel.isConnectedToInternet) findNavController().navigate(
                    RecipeFragmentDirections.actionRecipeFragmentToRecipesFilterFragment()
                )
                else showSnackBar(
                    view,
                    requireContext().getString(R.string.not_connected_to_internet)
                )
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, menuInflater: MenuInflater) {
        menuInflater.inflate(R.menu.recipe_search, menu)

        val searchItem = menu.findItem(R.id.search_recipes)
        val searchView = searchItem.actionView as SearchView
        searchView.queryHint = requireActivity().getString(R.string.search_recipes)

//        searchView.isSubmitButtonEnabled = true
        val queryOnTextLister = object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                if (query != null) {
                    searchFoodRecipes(query)
                    return true
                }
                return false
            }

            override fun onQueryTextChange(newText: String?): Boolean = true
        }

        searchView.setOnQueryTextListener(queryOnTextLister)
    }

    override fun onDestroy() {
        super.onDestroy()
        this.binding = null
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.logout -> {
                Firebase.auth.signOut()
                startActivity(Intent(requireActivity(), RecipeActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }
        }
        this.binding!!.drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
}