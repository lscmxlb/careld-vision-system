package com.careld.vision.ui.child

import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.careld.vision.R
import com.careld.vision.ui.base.BaseActivity
import com.careld.vision.ui.base.TvFocusHelper
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

/**
 * Child search activity
 * 
 * Allows searching for child profiles from local cache.
 */
@AndroidEntryPoint
class ChildSearchActivity : BaseActivity() {

    override val layoutResId: Int = R.layout.activity_child_search
    
    private val viewModel: ChildSearchViewModel by viewModels()
    
    private lateinit var etSearch: EditText
    private lateinit var btnSearch: Button
    private lateinit var btnBack: Button
    private lateinit var rvChildren: RecyclerView
    private lateinit var tvEmpty: TextView
    
    private lateinit var adapter: ChildAdapter

    override fun initViews() {
        etSearch = findViewById(R.id.etSearch)
        btnSearch = findViewById(R.id.btnSearch)
        btnBack = findViewById(R.id.btnBack)
        rvChildren = findViewById(R.id.rvChildren)
        tvEmpty = findViewById(R.id.tvEmpty)
        
        // Setup RecyclerView
        adapter = ChildAdapter { child ->
            onChildSelected(child)
        }
        rvChildren.layoutManager = LinearLayoutManager(this)
        rvChildren.adapter = adapter
        
        // Setup listeners
        btnSearch.setOnClickListener { performSearch() }
        btnBack.setOnClickListener { finish() }
        
        // Initial focus
        etSearch.requestFocus()
    }

    override fun observeData() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.searchResults.collect { results ->
                    adapter.submitList(results)
                    tvEmpty.visibility = if (results.isEmpty()) View.VISIBLE else View.GONE
                    rvChildren.visibility = if (results.isEmpty()) View.GONE else View.VISIBLE
                }
            }
        }
    }

    private fun performSearch() {
        val keyword = etSearch.text.toString().trim()
        viewModel.search(keyword)
    }

    private fun onChildSelected(child: ChildSearchViewModel.ChildDisplay) {
        // Would navigate to child detail or return result
        // For now, just show a toast or finish with result
        finish()
    }

    override fun setupFocusHandling() {
        etSearch.nextFocusDownId = R.id.btnSearch
        btnSearch.nextFocusUpId = R.id.etSearch
        btnSearch.nextFocusRightId = R.id.btnBack
        btnBack.nextFocusLeftId = R.id.btnSearch
    }
}
