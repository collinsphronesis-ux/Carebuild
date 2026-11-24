package com.example.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.VolunteerActivism
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.VolunteerActivism
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.R
import com.example.core.UiState
import com.example.data.auth.authStateFlow
import com.example.data.auth.signOutUser
import com.example.data.model.BeneficiaryCase
import com.example.data.repository.CareBridgeRepository
import com.example.data.repository.OreAssistantRepository
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore

const val ROUTE_DISCOVER = "discover"
const val ROUTE_PORTFOLIO = "portfolio"
const val ROUTE_ORE = "ore"
const val ROUTE_SUBMIT = "submit"
const val ROUTE_CASE_DETAIL = "case_detail"

private data class NavTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val PrimaryTabs = listOf(
    NavTab(
        route = ROUTE_DISCOVER,
        labelRes = R.string.nav_discover,
        selectedIcon = Icons.Filled.Explore,
        unselectedIcon = Icons.Outlined.Explore,
        testTag = "nav_discover"
    ),
    NavTab(
        route = ROUTE_PORTFOLIO,
        labelRes = R.string.nav_portfolio,
        selectedIcon = Icons.Filled.VolunteerActivism,
        unselectedIcon = Icons.Outlined.VolunteerActivism,
        testTag = "nav_portfolio"
    ),
    NavTab(
        route = ROUTE_ORE,
        labelRes = R.string.nav_ore,
        selectedIcon = Icons.Filled.AutoAwesome,
        unselectedIcon = Icons.Outlined.AutoAwesome,
        testTag = "nav_ore"
    ),
    NavTab(
        route = ROUTE_SUBMIT,
        labelRes = R.string.nav_submit,
        selectedIcon = Icons.Filled.AddCircle,
        unselectedIcon = Icons.Outlined.AddCircleOutline,
        testTag = "nav_submit"
    )
)

@Composable
fun CareBridgeRoot(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser
    if (user == null) {
        AuthScreen(onAuthSuccess = { /* Reactive authStateFlow transitions to shell */ })
    } else {
        key(user.uid) {
            AuthenticatedCareBridgeShell(currentUser = user)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthenticatedCareBridgeShell(currentUser: FirebaseUser) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: CareBridgeViewModel = viewModel(
        key = currentUser.uid,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                CareBridgeViewModel(
                    repository = CareBridgeRepository(db),
                    oreRepository = OreAssistantRepository(),
                    currentUserId = currentUser.uid
                )
            }
        }
    )

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: ROUTE_DISCOVER

    val filteredCasesState by viewModel.filteredCasesState.collectAsStateWithLifecycle()
    val allCasesState by viewModel.casesState.collectAsStateWithLifecycle()
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()
    val selectedCaseId by viewModel.selectedCaseId.collectAsStateWithLifecycle()
    val updatesState by viewModel.selectedCaseUpdatesState.collectAsStateWithLifecycle()
    val pledgesState by viewModel.pledgesState.collectAsStateWithLifecycle()
    val oreState by viewModel.oreState.collectAsStateWithLifecycle()
    val caseOreState by viewModel.caseOreState.collectAsStateWithLifecycle()
    val oreNotesState by viewModel.oreNotesState.collectAsStateWithLifecycle()
    val isMutating by viewModel.isMutating.collectAsStateWithLifecycle()

    var pledgeSheetTarget by remember { mutableStateOf<BeneficiaryCase?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.snackbarEvents.collect { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    val allCases = (allCasesState as? UiState.Success)?.data.orEmpty()
    val selectedCase = allCases.firstOrNull { it.id == selectedCaseId }

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp
        val showChrome = currentRoute != ROUTE_CASE_DETAIL

        Scaffold(
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                if (showChrome) {
                    CenterAlignedTopAppBar(
                        title = {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = stringResource(id = R.string.app_name),
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                            }
                        },
                        actions = {
                            IconButton(
                                onClick = {
                                    signOutUser(
                                        credentialManager = credentialManager,
                                        scope = scope
                                    )
                                },
                                modifier = Modifier.testTag("sign_out_button")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Logout,
                                    contentDescription = stringResource(id = R.string.sign_out),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = MaterialTheme.colorScheme.background
                        )
                    )
                }
            },
            bottomBar = {
                if (!useRail && showChrome) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow
                    ) {
                        PrimaryTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            val label = stringResource(id = tab.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail && showChrome) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier
                            .fillMaxHeight()
                            .statusBarsPadding()
                            .navigationBarsPadding()
                    ) {
                        Spacer(modifier = Modifier.size(12.dp))
                        PrimaryTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            val label = stringResource(id = tab.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                modifier = Modifier.testTag(tab.testTag)
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_DISCOVER
                    ) {
                        composable(ROUTE_DISCOVER) {
                            DiscoverScreen(
                                casesState = filteredCasesState,
                                filterState = filterState,
                                onSearchChange = viewModel::updateSearchQuery,
                                onToggleCategory = viewModel::toggleCategory,
                                onToggleUrgent = viewModel::toggleOnlyUrgent,
                                onToggleMicroloans = viewModel::toggleOnlyMicroloans,
                                onClearFilters = viewModel::clearFilters,
                                onOpenCaseDetail = { item ->
                                    viewModel.selectCase(item.id)
                                    navController.navigate(ROUTE_CASE_DETAIL)
                                },
                                onQuickSupport = { item ->
                                    pledgeSheetTarget = item
                                },
                                onAskOreEverything = {
                                    viewModel.askOreEverythingDigest()
                                    navigateToTab(ROUTE_ORE)
                                }
                            )
                        }

                        composable(ROUTE_PORTFOLIO) {
                            PortfolioScreen(
                                pledgesState = pledgesState,
                                isMutating = isMutating,
                                onRecordRepaymentOrFulfill = viewModel::recordRepaymentOrFulfill,
                                onAskOreSummarizePortfolio = {
                                    viewModel.askOrePortfolioDigest()
                                    navigateToTab(ROUTE_ORE)
                                },
                                onBrowseCases = { navigateToTab(ROUTE_DISCOVER) }
                            )
                        }

                        composable(ROUTE_ORE) {
                            OreScreen(
                                oreState = oreState,
                                oreNotesState = oreNotesState,
                                isMutating = isMutating,
                                onAskEverythingDigest = viewModel::askOreEverythingDigest,
                                onAskPortfolioDigest = viewModel::askOrePortfolioDigest,
                                onAskUrgentMedicalDigest = viewModel::askOreUrgentMedicalDigest,
                                onSubmitCustomInput = viewModel::askOreCustomInput,
                                onSaveSummary = { viewModel.saveOreSummary(fromCaseView = false) },
                                onDeleteNote = viewModel::deleteOreNote
                            )
                        }

                        composable(ROUTE_SUBMIT) {
                            SubmitCaseScreen(
                                isSubmitting = isMutating,
                                onCreateCase = { name, region, title, story, plan, cat, type, goal, months, urgency, cover, resetForm ->
                                    viewModel.createCase(
                                        beneficiaryName = name,
                                        region = region,
                                        title = title,
                                        story = story,
                                        allocationPlan = plan,
                                        category = cat,
                                        supportType = type,
                                        goalAmountCents = goal,
                                        repaymentMonths = months,
                                        urgency = urgency,
                                        coverAssetKey = cover,
                                        onCreated = { newId ->
                                            resetForm()
                                            viewModel.selectCase(newId)
                                            navController.navigate(ROUTE_CASE_DETAIL)
                                        }
                                    )
                                }
                            )
                        }

                        composable(ROUTE_CASE_DETAIL) {
                            if (selectedCase != null) {
                                CaseDetailScreen(
                                    beneficiaryCase = selectedCase,
                                    updatesState = updatesState,
                                    caseOreState = caseOreState,
                                    currentUserId = currentUser.uid,
                                    isMutating = isMutating,
                                    onBack = { navController.popBackStack() },
                                    onOpenPledgeSheet = { pledgeSheetTarget = selectedCase },
                                    onAskOreSummarizeCase = viewModel::askOreForCase,
                                    onSaveCaseOreSummary = { viewModel.saveOreSummary(fromCaseView = true) },
                                    onPostUpdate = { headline, message, type, onDone ->
                                        viewModel.postImpactUpdate(
                                            caseId = selectedCase.id,
                                            headline = headline,
                                            message = message,
                                            milestoneType = type,
                                            onSuccess = onDone
                                        )
                                    }
                                )
                            } else {
                                LaunchedEffect(Unit) {
                                    navController.popBackStack()
                                }
                            }
                        }
                    }
                }
            }
        }

        val targetForPledge = pledgeSheetTarget
        if (targetForPledge != null) {
            PledgeBottomSheet(
                beneficiaryCase = targetForPledge,
                isSubmitting = isMutating,
                onDismiss = { pledgeSheetTarget = null },
                onSubmitPledge = { supportType, amountCents, months, note ->
                    viewModel.submitPledge(
                        beneficiaryCase = targetForPledge,
                        supportType = supportType,
                        amountCents = amountCents,
                        repaymentMonths = months,
                        note = note,
                        onSuccess = { pledgeSheetTarget = null }
                    )
                }
            )
        }
    }
}
