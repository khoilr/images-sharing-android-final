package com.khoilr.finalproject

import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.annotation.CheckResult
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.search.SearchBar
import com.google.android.material.search.SearchView
import com.google.android.material.transition.MaterialContainerTransform
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart


class ImagesGridFragment(private val images: MutableList<ImageItem>) : Fragment() {
	private lateinit var recyclerView: RecyclerView
	private lateinit var adapter: ImageAdapter
	private lateinit var searchBar: SearchBar
	private lateinit var searchView: SearchView
	private lateinit var recyclerViewSearchResult: RecyclerView
	private lateinit var adapterSearchResult: ImageAdapter
	private lateinit var resultSize: TextView
	private lateinit var context: Context

	override fun onAttach(context: Context) {
		super.onAttach(context)
		this.context = context
	}

	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
//		exitTransition = Hold()
//		reenterTransition = MaterialElevationScale(true)
	}

	override fun onCreateView(
		inflater: LayoutInflater,
		container: ViewGroup?,
		savedInstanceState: Bundle?,
	): View {
		// Inflate the layout for this fragment
		val view = inflater.inflate(R.layout.fragment_images_grid, container, false)

		// Find the RecyclerView and set its layout manager and adapter
		recyclerView = view.findViewById(R.id.recyclerView)
		recyclerViewSearchResult = view.findViewById(R.id.recyclerViewSearchResult)
		resultSize = view.findViewById(R.id.resultSize)

		// Layout
		val layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
		layoutManager.gapStrategy = StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS
		recyclerView.layoutManager = layoutManager

		// Adapter
		adapter = ImageAdapter(context, images, true)
		adapter.onClickItemListener =
			object : ImageAdapter.OnClickItemListener {
				override fun onClickItem(imageItem: ImageItem) {
					val imageFragment = ImageFragment().apply {
						arguments = Bundle().apply {
							putParcelable("imageItem", imageItem)
						}
					}
					imageFragment.sharedElementEnterTransition = MaterialContainerTransform()

//					val transition: Transition = MaterialContainerTransform()

					// Start a fragment transaction and replace the container with the image fragment
					requireActivity().supportFragmentManager
						.beginTransaction()
						.replace(R.id.container, imageFragment)
						.addToBackStack(null)
						.commit()
				}
			}

		recyclerView.adapter = adapter

		// Search result
		val layoutManagerSearchResult =
			StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
		layoutManagerSearchResult.gapStrategy =
			StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS
		recyclerViewSearchResult.layoutManager = layoutManagerSearchResult

		return view
	}

	@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
	override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
		super.onViewCreated(view, savedInstanceState)
		postponeEnterTransition()
		(view.parent as ViewGroup).viewTreeObserver
			.addOnPreDrawListener {
				startPostponedEnterTransition()
				true
			}

		// Search bar
		searchBar = view.findViewById(R.id.search_bar)

		// Search view
		searchView = view.findViewById(R.id.search_view)
		searchView.editText.textChanges().debounce(1000).onEach { it ->
			// filter images with ImageLabel
			val text = it.toString()
			var texts = text.split(" ")
			texts = texts.filter { it != "" }
			searchBar.text = text


			// filter image any images label contain any text in texts
			val newImages = images.filter {
				it.labels?.any { label -> texts.any { text -> label.text.contains(text, true) } }
					?: false
			}

			// update result size
			when (newImages.size) {
				0 -> {
					resultSize.text = getString(R.string.no_image)
				}

				1 -> {
					resultSize.text = getString(R.string.there_is_1_image)
				}

				else -> {
					resultSize.text = getString(R.string.there_are_n_images, newImages.size)
				}
			}

			// update adapter
			if (::adapterSearchResult.isInitialized) adapterSearchResult.updateData(newImages.toMutableList())
			else {
				adapterSearchResult =
					ImageAdapter(context, newImages.toMutableList(), labeling = false)
				recyclerViewSearchResult.adapter = adapterSearchResult
			}
		}.launchIn(lifecycleScope)
	}

	@ExperimentalCoroutinesApi
	@CheckResult
	fun EditText.textChanges(): kotlinx.coroutines.flow.Flow<CharSequence?> {
		return callbackFlow {
			val listener = object : TextWatcher {
				override fun afterTextChanged(s: Editable?) = Unit
				override fun beforeTextChanged(
					s: CharSequence?,
					start: Int,
					count: Int,
					after: Int,
				) = Unit

				override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
					trySend(s)
				}
			}
			addTextChangedListener(listener)
			awaitClose { removeTextChangedListener(listener) }
		}.onStart { emit(text) }
	}
}

