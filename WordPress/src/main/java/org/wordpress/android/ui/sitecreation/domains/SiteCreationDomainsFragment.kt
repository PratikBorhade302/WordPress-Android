package org.wordpress.android.ui.sitecreation.domains

import android.content.Context
import android.os.Bundle
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.site_creation_domains_screen.*
import org.wordpress.android.R
import org.wordpress.android.WordPress
import org.wordpress.android.ui.accounts.HelpActivity
import org.wordpress.android.ui.sitecreation.SiteCreationBaseFormFragment
import org.wordpress.android.ui.sitecreation.domains.SiteCreationDomainsViewModel.DomainsUiState.DomainsUiContentState
import org.wordpress.android.ui.sitecreation.misc.OnHelpClickedListener
import org.wordpress.android.ui.sitecreation.misc.SearchInputWithHeader
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.DisplayUtilsWrapper
import javax.inject.Inject

class SiteCreationDomainsFragment : SiteCreationBaseFormFragment() {
    private lateinit var nonNullActivity: FragmentActivity
    private var searchInputWithHeader: SearchInputWithHeader? = null
    private lateinit var viewModel: SiteCreationDomainsViewModel

    @Inject internal lateinit var viewModelFactory: ViewModelProvider.Factory
    @Inject internal lateinit var uiHelpers: UiHelpers
    @Inject internal lateinit var displayUtils: DisplayUtilsWrapper

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context !is DomainsScreenListener) {
            throw IllegalStateException("Parent activity must implement DomainsScreenListener.")
        }
        if (context !is OnHelpClickedListener) {
            throw IllegalStateException("Parent activity must implement OnHelpClickedListener.")
        }
    }

    @LayoutRes
    override fun getContentLayout(): Int {
        return R.layout.site_creation_domains_screen
    }

    override fun setupContent(rootView: ViewGroup) {
        searchInputWithHeader = SearchInputWithHeader(
                uiHelpers = uiHelpers,
                rootView = rootView,
                onClear = { viewModel.onClearTextBtnClicked() }
        )
        rootView.findViewById<AppCompatButton>(R.id.create_site_button).setOnClickListener {
            viewModel.createSiteBtnClicked()
        }
        initRecyclerView()
        initViewModel()
    }

    override fun getScreenTitle(): String {
        return arguments?.getString(EXTRA_SCREEN_TITLE)
                ?: throw IllegalStateException("Required argument screen title is missing.")
    }

    override fun onHelp() {
        viewModel.onHelpClicked()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nonNullActivity = requireActivity()
        (nonNullActivity.application as WordPress).component().inject(this)
    }

    override fun onViewStateRestored(savedInstanceState: Bundle?) {
        super.onViewStateRestored(savedInstanceState)
        // we need to set the `onTextChanged` after the viewState has been restored otherwise the viewModel.updateQuery
        // is called when the system sets the restored value to the EditText which results in an unnecessary request
        searchInputWithHeader?.onTextChanged = { viewModel.updateQuery(it) }
    }

    private fun initRecyclerView() {
        recycler_view.layoutManager = LinearLayoutManager(activity, RecyclerView.VERTICAL, false)
        initAdapter()
    }

    private fun initAdapter() {
        val adapter = SiteCreationDomainsAdapter(uiHelpers)
        recycler_view.adapter = adapter
    }

    private fun initViewModel() {
        viewModel = ViewModelProviders.of(this, viewModelFactory)
                .get(SiteCreationDomainsViewModel::class.java)

        viewModel.uiState.observe(this, Observer { uiState ->
            uiState?.let {
                searchInputWithHeader?.updateHeader(nonNullActivity, uiState.headerUiState)
                searchInputWithHeader?.updateSearchInput(nonNullActivity, uiState.searchInputUiState)
                updateContentUiState(uiState.contentState)
                uiHelpers.updateVisibility(create_site_button_container, uiState.createSiteButtonContainerVisibility)
                uiHelpers.updateVisibility(create_site_button_shaddow, uiState.createSiteButtonContainerVisibility)
                updateTitleVisibility(uiState.headerUiState == null)
            }
        })
        viewModel.clearBtnClicked.observe(this, Observer {
            searchInputWithHeader?.setInputText("")
        })
        viewModel.createSiteBtnClicked.observe(this, Observer { domain ->
            domain?.let { (requireActivity() as DomainsScreenListener).onDomainSelected(domain) }
        })
        viewModel.onHelpClicked.observe(this, Observer {
            (requireActivity() as OnHelpClickedListener).onHelpClicked(HelpActivity.Origin.SITE_CREATION_DOMAINS)
        })
        viewModel.start(getSegmentIdFromArguments())
    }

    private fun updateContentUiState(contentState: DomainsUiContentState) {
        uiHelpers.updateVisibility(domain_list_empty_view, contentState.emptyViewVisibility)
        if (contentState.items.isNotEmpty()) {
            view?.announceForAccessibility(getString(R.string.suggestions_updated_content_description))
        }
        (recycler_view.adapter as SiteCreationDomainsAdapter).update(contentState.items)
    }

    private fun updateTitleVisibility(visible: Boolean) {
        val actionBar = (requireActivity() as? AppCompatActivity)?.supportActionBar
        actionBar?.setDisplayShowTitleEnabled(displayUtils.isLandscape() || visible) // Always visible in landscape
    }

    private fun getSegmentIdFromArguments(): Long? = arguments?.getLong(EXTRA_SEGMENT_ID)

    override fun onDestroyView() {
        super.onDestroyView()
        searchInputWithHeader = null
    }

    companion object {
        const val TAG = "site_creation_domains_fragment_tag"
        private const val EXTRA_SEGMENT_ID = "extra_segment_id"

        fun newInstance(screenTitle: String, segmentId: Long?): SiteCreationDomainsFragment {
            val fragment = SiteCreationDomainsFragment()
            val bundle = Bundle()
            bundle.putString(EXTRA_SCREEN_TITLE, screenTitle)
            if (segmentId != null) {
                bundle.putLong(EXTRA_SEGMENT_ID, segmentId)
            }
            fragment.arguments = bundle
            return fragment
        }
    }
}
