package org.agripedia.app.ui

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import org.agripedia.app.AgriPediaApp
import org.agripedia.app.AppContainer
import org.agripedia.app.data.repo.CatalogState
import org.agripedia.app.data.repo.UserProfile

@Composable
fun appContainer(): AppContainer = (LocalContext.current.applicationContext as AgriPediaApp).container

/** Activity-scoped state shared by every screen: knowledge base, profile, connectivity. */
class AppViewModel(val container: AppContainer, context: Context) : ViewModel() {
    val catalog: StateFlow<CatalogState> = container.catalogRepository.state
    val profile: StateFlow<UserProfile?> = container.userRepository.profile
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    val favorites: StateFlow<List<String>> = container.userRepository.favoritePlantIds
        .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())
    val online: StateFlow<Boolean> = connectivity(context.applicationContext)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun retryCatalog() = container.catalogRepository.reload()
    fun saveProfile(p: UserProfile) = viewModelScope.launch { container.userRepository.saveProfile(p) }
    fun toggleFavorite(plantId: String) = viewModelScope.launch { container.userRepository.toggleFavorite(plantId) }

    private fun connectivity(context: Context) = callbackFlow {
        val cm = context.getSystemService(ConnectivityManager::class.java)
        fun current(): Boolean = cm.getNetworkCapabilities(cm.activeNetwork)?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        trySend(current())
        val cb = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(true) }
            override fun onLost(network: Network) { trySend(current()) }
        }
        cm.registerNetworkCallback(NetworkRequest.Builder().addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET).build(), cb)
        awaitClose { cm.unregisterNetworkCallback(cb) }
    }.distinctUntilChanged()
}
