package com.pay2own.ui.nav

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.pay2own.domain.EnrollLink
import com.pay2own.domain.Pay2OwnRepository
import com.pay2own.ui.components.LoadingState
import com.pay2own.ui.components.Pay2OwnScaffold
import com.pay2own.ui.contact.ContactRoute
import com.pay2own.ui.history.HistoryRoute
import com.pay2own.ui.home.HomeRoute
import com.pay2own.ui.pay.PayRoute
import com.pay2own.ui.receipt.ReceiptRoute
import com.pay2own.ui.schedule.ScheduleRoute
import com.pay2own.ui.setup.SetupRoute
import com.pay2own.ui.support.FaqRoute
import com.pay2own.ui.support.SupportRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

object Routes {
    const val Setup = "setup"
    const val Contact = "contact"
    const val Home = "home"
    const val Pay = "pay"
    const val History = "history"
    const val Receipt = "receipt/{reference}"
    const val Schedule = "schedule"
    const val Support = "support"
    const val Faq = "faq"

    fun receipt(reference: String) = "receipt/$reference"
}

@HiltViewModel
class AppViewModel @Inject constructor(
    repository: Pay2OwnRepository,
) : ViewModel() {
    private val _start = MutableStateFlow<String?>(null)
    val start: StateFlow<String?> = _start.asStateFlow()

    init {
        viewModelScope.launch {
            _start.value = if (repository.hasCredentials() && repository.isConfirmed()) {
                Routes.Home
            } else {
                Routes.Setup
            }
        }
    }
}

@Composable
fun AppRoot(enroll: EnrollLink?, onEnrollConsumed: () -> Unit) {
    val appViewModel: AppViewModel = hiltViewModel()
    val start by appViewModel.start.collectAsStateWithLifecycle()
    if (start == null) {
        Pay2OwnScaffold(title = "") { LoadingState(it) }
        return
    }
    val nav = rememberNavController()
    NavHost(navController = nav, startDestination = start!!) {
        composable(Routes.Setup) {
            SetupRoute(
                enroll = enroll,
                onConfirmed = { nav.openHome() },
                onRejected = { nav.openContact() },
                onConsumed = onEnrollConsumed,
            )
        }
        composable(Routes.Contact) {
            ContactRoute(onEnterToken = { nav.openSetup() })
        }
        composable(Routes.Home) {
            HomeRoute(
                onPay = { nav.navigate(Routes.Pay) },
                onHistory = { nav.navigate(Routes.History) },
                onSchedule = { nav.navigate(Routes.Schedule) },
                onSupport = { nav.navigate(Routes.Support) },
                onSignedOut = { nav.openContact() },
            )
        }
        composable(Routes.Pay) {
            PayRoute(
                onSignedOut = { nav.openContact() },
                onDone = { nav.popBackStack() },
                onReceipt = { reference -> nav.navigate(Routes.receipt(reference)) },
            )
        }
        composable(Routes.History) {
            HistoryRoute(
                onBack = { nav.popBackStack() },
                onReceipt = { reference -> nav.navigate(Routes.receipt(reference)) },
                onSignedOut = { nav.openContact() },
            )
        }
        composable(
            route = Routes.Receipt,
            arguments = listOf(navArgument("reference") { type = NavType.StringType }),
        ) { entry ->
            ReceiptRoute(
                reference = entry.arguments?.getString("reference").orEmpty(),
                onBack = { nav.popBackStack() },
                onSignedOut = { nav.openContact() },
            )
        }
        composable(Routes.Schedule) {
            ScheduleRoute(onBack = { nav.popBackStack() }, onSignedOut = { nav.openContact() })
        }
        composable(Routes.Support) {
            SupportRoute(
                onBack = { nav.popBackStack() },
                onFaq = { nav.navigate(Routes.Faq) },
                onCleared = { nav.openSetup() },
            )
        }
        composable(Routes.Faq) {
            FaqRoute(onBack = { nav.popBackStack() })
        }
    }
}

private fun NavHostController.openHome() {
    navigate(Routes.Home) { popUpTo(graph.id) { inclusive = true } }
}

private fun NavHostController.openSetup() {
    navigate(Routes.Setup) { popUpTo(graph.id) { inclusive = true } }
}

private fun NavHostController.openContact() {
    navigate(Routes.Contact) { popUpTo(graph.id) { inclusive = true } }
}
