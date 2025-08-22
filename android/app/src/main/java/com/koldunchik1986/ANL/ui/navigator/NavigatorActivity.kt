package com.koldunchik1986.ANL.ui.navigator

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.hilt.android.AndroidEntryPoint
import com.koldunchik1986.ANL.ui.theme.ABClientTheme

/**
 * ��������� - ������ ������ FormNavigator �� Windows ������
 * �������� �����, ������������� � ����� �������
 */
@AndroidEntryPoint
class NavigatorActivity : ComponentActivity() {
    
    companion object {
        const val EXTRA_DESTINATION = "extra_destination"
        const val EXTRA_LOCATION = "extra_location"
        const val EXTRA_LOCATION2 = "extra_location2"
        const val EXTRA_NICK = "extra_nick"
        
        fun createIntent(
            context: Context,
            destination: String? = null,
            location: String? = null,
            location2: String? = null,
            nick: String? = null
        ): Intent {
            return Intent(context, NavigatorActivity::class.java).apply {
                putExtra(EXTRA_DESTINATION, destination)
                putExtra(EXTRA_LOCATION, location)
                putExtra(EXTRA_LOCATION2, location2)
                putExtra(EXTRA_NICK, nick)
            }
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val destination = intent.getStringExtra(EXTRA_DESTINATION)
        val location = intent.getStringExtra(EXTRA_LOCATION)
        val location2 = intent.getStringExtra(EXTRA_LOCATION2)
        val nick = intent.getStringExtra(EXTRA_NICK)
        
        setContent {
            ABClientTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavigatorScreen(
                        onBack = { finish() },
                        initialDestination = destination,
                        location = location,
                        location2 = location2,
                        nick = nick
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavigatorScreen(
    onBack: () -> Unit,
    initialDestination: String? = null,
    location: String? = null,
    location2: String? = null,
    nick: String? = null,
    viewModel: NavigatorViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    LaunchedEffect(initialDestination, location, location2, nick) {
        viewModel.initialize(initialDestination, location, location2, nick)
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = uiState.title.ifEmpty { "���������" }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "�����")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.startAutoMoving() },
                        enabled = uiState.canStartMoving
                    ) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "������ ��������")
                    }
                }
            )
        }
    ) { paddingValues ->
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // ����� ������ � ���������
            LocationsPanel(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                uiState = uiState,
                onLocationSelected = viewModel::setDestination,
                onSearchTextChanged = viewModel::setSearchText,
                onFavoriteAdded = viewModel::addToFavorites,
                onFavoritesCleared = viewModel::clearFavorites
            )
            
            // ������ ������ � ������ � �����������
            MapInfoPanel(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                uiState = uiState,
                onDestinationChanged = viewModel::setDestination,
                onCalculateRoute = viewModel::calculateRoute
            )
        }
    }
}

@Composable
private fun LocationsPanel(
    modifier: Modifier = Modifier,
    uiState: NavigatorUiState,
    onLocationSelected: (String) -> Unit,
    onSearchTextChanged: (String) -> Unit,
    onFavoriteAdded: () -> Unit,
    onFavoritesCleared: () -> Unit
) {
    Card(
        modifier = modifier.padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // ����� �� ��������
            OutlinedTextField(
                value = uiState.searchText,
                onValueChange = onSearchTextChanged,
                label = { Text("����� �� ��������") },
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = "�����")
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // ������ ���������� ���������
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onFavoriteAdded,
                    modifier = Modifier.weight(1f),
                    enabled = uiState.currentDestination.isNotEmpty()
                ) {
                    Icon(Icons.Default.Star, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("� ���������")
                }
                
                Button(
                    onClick = onFavoritesCleared,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Clear, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("��������")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // ������ �������
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                uiState.locationGroups.forEach { group ->
                    item {
                        LocationGroupHeader(
                            title = group.title,
                            count = group.locations.size
                        )
                    }
                    
                    items(group.locations) { location ->
                        LocationItem(
                            location = location,
                            isSelected = location.cellNumber == uiState.currentDestination,
                            onClick = { onLocationSelected(location.cellNumber) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MapInfoPanel(
    modifier: Modifier = Modifier,
    uiState: NavigatorUiState,
    onDestinationChanged: (String) -> Unit,
    onCalculateRoute: () -> Unit
) {
    Card(
        modifier = modifier.padding(8.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            // ���� ����������
            OutlinedTextField(
                value = uiState.destinationInput,
                onValueChange = onDestinationChanged,
                label = { Text("���������� (��������: 8-259)") },
                isError = !uiState.isDestinationValid,
                supportingText = {
                    if (!uiState.isDestinationValid && uiState.destinationInput.isNotEmpty()) {
                        Text(
                            text = "�������� ������ �������",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                },
                trailingIcon = {
                    IconButton(
                        onClick = onCalculateRoute,
                        enabled = uiState.isDestinationValid
                    ) {
                        Icon(Icons.Default.Calculate, contentDescription = "���������� �������")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // ���������� � ��������
            RouteInfoCard(uiState = uiState)
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // ����� (�������� ��� ������� ����������)
            MapPlaceholder(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                destination = uiState.currentDestination
            )
        }
    }
}

@Composable
private fun LocationGroupHeader(
    title: String,
    count: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            Icons.Default.Folder,
            contentDescription = null,
            tint = Color(0xFF006064)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF006064)
        )
        if (count > 0) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "($count)",
                style = MaterialTheme.typography.bodySmall,
                color = Color(0xFF006064)
            )
        }
    }
}

@Composable
private fun LocationItem(
    location: LocationInfo,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Text(
                text = location.name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
            if (location.cellNumber.isNotEmpty()) {
                Text(
                    text = location.cellNumber,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun RouteInfoCard(
    uiState: NavigatorUiState
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "���������� � ��������",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            
            RouteInfoRow(
                label = "���������:",
                value = uiState.routeInfo.jumps?.toString() ?: "-"
            )
            
            RouteInfoRow(
                label = "������� �����:",
                value = uiState.routeInfo.botLevel?.toString() ?: "-"
            )
            
            RouteInfoRow(
                label = "���������:",
                value = uiState.routeInfo.tiedPercentage?.let { "~$it%" } ?: "-"
            )
        }
    }
}

@Composable
private fun RouteInfoRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun MapPlaceholder(
    modifier: Modifier = Modifier,
    destination: String
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    Icons.Default.Map,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "�����",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (destination.isNotEmpty()) {
                    Text(
                        text = "����� ����������: $destination",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
