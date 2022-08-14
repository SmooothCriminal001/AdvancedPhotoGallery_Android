package com.bignerdranch.android.advancedphotogallery

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import androidx.appcompat.widget.AppCompatImageView
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.*
import coil.load
import com.bignerdranch.android.advancedphotogallery.api.GalleryItem
import com.bignerdranch.android.advancedphotogallery.databinding.FragmentPhotoGalleryBinding
import com.bignerdranch.android.advancedphotogallery.databinding.ListItemGalleryBinding
import com.bignerdranch.android.advancedphotogallery.databinding.ListItemHistoryBinding
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.util.*


private const val TAG = "PhotoGalleryFragment"
class PhotoGalleryFragment : Fragment() {

    private var searchView: SearchView? = null
    private val viewModel: PhotoGalleryViewModel by viewModels()
    private var _binding: FragmentPhotoGalleryBinding? = null
    private val binding
        get() = checkNotNull(_binding) {
            "Cannot access binding because it is null. Is the view visible?"
        }

    private var presentQuery = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentPhotoGalleryBinding.inflate(inflater, container, false)
        binding.galleryGrid.layoutManager = GridLayoutManager(context, 3)
        binding.galleryGrid.adapter = null

        binding.searchedList.layoutManager = LinearLayoutManager(context)
        binding.searchedList.adapter = null
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        hideSearchHistory()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.galleryItems.collect{
                    Log.d(TAG, "Gallery Items collected")
                    binding.progressbar.visibility = View.GONE
                    searchView?.isEnabled = true
                    binding.galleryGrid.adapter = PhotoListAdapter().apply { submitData(lifecycle, it) }
                }
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
        	viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect {
                    Log.d(TAG, "Collecting ${it.history}")
                    binding.searchedList.adapter = QueryHistoryAdapter{
                        searchView?.setQuery(it, true)
                    }.apply { submitList(it.history) }
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
    
    private inner class PhotoViewHolder(private val binding: ListItemGalleryBinding) : RecyclerView.ViewHolder(binding.root){
    	
    	//fun bind(galleryItem : GalleryItem, OnClickLambda: (UUID) -> Unit){
        fun bind(galleryItem : GalleryItem){
    		//Assign binding instances with data here
    		binding.itemImageView.load(galleryItem.url){
    		    //optionalLambda
    		    placeholder(R.drawable.flower_design)
    		}
    		/*
    		binding.root.setOnClickListener{
    			OnClickLambda()
    		}
    		
    		 */
    	}
    }
    
    
    object DiffUtilInstance : DiffUtil.ItemCallback<GalleryItem>() {
    	override fun areItemsTheSame(oldItem: GalleryItem, newItem: GalleryItem): Boolean {
    		return oldItem.id == newItem.id		//Or any way to check the items are same
    		//return false
    	}
    
    	override fun areContentsTheSame(oldItem: GalleryItem, newItem: GalleryItem): Boolean {
    		return (oldItem.toString() == newItem.toString())
    		//return false
    	}
    }
    
    //private inner class PhotoListAdapter(private val OnClickLambda: (UUID) -> Unit): ListAdapter<GalleryItem, PhotoViewHolder>(DiffUtilInstance){
    private inner class PhotoListAdapter(): PagingDataAdapter<GalleryItem, PhotoViewHolder>(DiffUtilInstance){
    	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
    		val inflater = LayoutInflater.from(parent.context)
    		val binding = ListItemGalleryBinding.inflate(inflater, parent, false)
    		return PhotoViewHolder(binding)
    	}
    
    	override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
    		//holder.bind(getItem(position), OnClickLambda)
            getItem(position)?.let { holder.bind(it) }
        }
    
    	override fun getItemViewType(position: Int): Int {
    		
    		return 0
    	}
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
    	super.onCreateOptionsMenu(menu, inflater)
    	inflater.inflate(R.menu.fragment_photo_gallery, menu)

        val searchItem: MenuItem = menu.findItem(R.id.menu_item_search)
        searchView = searchItem.actionView as? SearchView

        searchView?.apply {

            setOnQueryTextFocusChangeListener(object: View.OnFocusChangeListener{
            	override fun onFocusChange(queryView: View?, hasFocus: Boolean) {

                    queryView?.apply {
            			if(hasFocus){
            				showSearchHistory()
            			}
            			else{
            				hideSearchHistory()
            			}
            		}
            	}
            })

            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    searchView?.clearFocus()
                    query?.let{
                        if(it != presentQuery){
                            hideSearchHistory()
                            binding.progressbar.visibility = View.VISIBLE
                            viewModel.searchItems(it)
                            searchView?.isEnabled = false
                        }
                    }
                    hideKeyboardFrom(requireContext(), requireView())
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    newText?.let {
                        binding.progressbar.visibility = View.GONE
                    }
                    return true
                }
            })
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                Log.d(TAG, "to collect query")
                viewModel.uiState.collectLatest {
                    val query = if(it.history.isNotEmpty()) it.history.first() else ""
                    Log.d(TAG, "Collecting while 'Start' : $query")
                    presentQuery = query
                    searchView?.setQuery(query, false)
                }
            }
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
    	return when (item.itemId) {
    		R.id.menu_item_clear -> {
    			viewModel.searchItems("")
                binding.progressbar.visibility = View.VISIBLE
    			true
    		}
    		else -> super.onOptionsItemSelected(item)
    	}
    }

    override fun onDestroyOptionsMenu() {
        super.onDestroyOptionsMenu()
        searchView = null
    }

    fun hideKeyboardFrom(context: Context, view: View) {
    	val imm: InputMethodManager = context.getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
    	imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    fun showSearchHistory(){
        Log.d(TAG, "History on")
        binding.galleryGrid.visibility = View.GONE
        binding.searchedList.visibility = View.VISIBLE
    }

    fun hideSearchHistory(){
        Log.d(TAG, "History off")
        binding.searchedList.visibility = View.GONE
        binding.galleryGrid.visibility = View.VISIBLE
    }

    private inner class QueryHistoryViewHolder(private val binding: ListItemHistoryBinding) : RecyclerView.ViewHolder(binding.root){

    	fun bind(query : String, selectQuery: (String) -> Unit){
            Log.d(TAG, "Binding $query")
            binding.searchQuery.text = query

    		binding.root.setOnClickListener{
    			selectQuery(query)
    		}
    	}
    }


    object DiffUtilHistory : DiffUtil.ItemCallback<String>() {
    	override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
    		return oldItem == newItem		//Or any way to check the items are same
    	}

    	override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
    		return (oldItem == newItem)
    	}
    }

    private inner class QueryHistoryAdapter(private val selectQuery: (String) -> Unit): ListAdapter<String, QueryHistoryViewHolder>(DiffUtilHistory){
    	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): QueryHistoryViewHolder {
    		val inflater = LayoutInflater.from(parent.context)
    		val binding = ListItemHistoryBinding.inflate(inflater, parent, false)
    		return QueryHistoryViewHolder(binding)
    	}

    	override fun onBindViewHolder(holder: QueryHistoryViewHolder, position: Int) {
    		holder.bind(getItem(position), selectQuery)
    	}

    	override fun getItemViewType(position: Int): Int {

    		return 0
    	}
    }
}