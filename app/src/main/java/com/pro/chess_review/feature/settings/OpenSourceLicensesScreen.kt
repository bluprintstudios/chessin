package com.pro.chess_review.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pro.chess_review.ui.designsystem.HudPanel
import com.pro.chess_review.ui.designsystem.HudScreenHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private const val STOCKFISH_SOURCE_URL =
    "https://github.com/official-stockfish/Stockfish/tree/edb0d9db6731067ec50ce619ff372b463bc4dd5d"
private const val STOCKFISH_NETWORK_URL =
    "https://github.com/official-stockfish/networks/blob/master/nn-1a298aa575a0.nnue"

@Composable
fun OpenSourceLicensesScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    var stockfishLicense by remember { mutableStateOf<String?>(null) }
    var networkLicense by remember { mutableStateOf<String?>(null) }
    var apacheLicense by remember { mutableStateOf<String?>(null) }
    var stockfishLicenseLoadFailed by remember { mutableStateOf(false) }
    var networkLicenseLoadFailed by remember { mutableStateOf(false) }
    var apacheLicenseLoadFailed by remember { mutableStateOf(false) }

    LaunchedEffect(context) {
        val texts = withContext(Dispatchers.IO) {
            Triple(
                runCatching {
                    context.assets.open("licenses/stockfish/COPYING.txt")
                        .bufferedReader()
                        .use { it.readText() }
                },
                runCatching {
                    context.assets.open("licenses/stockfish/CC0-1.0.txt")
                        .bufferedReader()
                        .use { it.readText() }
                },
                runCatching {
                    context.assets.open("pieces/licenses/Apache-2.0.txt")
                        .bufferedReader()
                        .use { it.readText() }
                }
            )
        }
        stockfishLicense = texts.first.getOrNull()
        networkLicense = texts.second.getOrNull()
        apacheLicense = texts.third.getOrNull()
        stockfishLicenseLoadFailed = texts.first.isFailure
        networkLicenseLoadFailed = texts.second.isFailure
        apacheLicenseLoadFailed = texts.third.isFailure
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background,
    ) { insets ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(insets)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                HudScreenHeader(title = "OPEN-SOURCE LICENSES", routeLabel = "licenses")
                Text("Open-source licenses", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Text(
                    "Engine version and license information for this build.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            HudPanel(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Stockfish 19", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Stockfish is developed by the Stockfish contributors and distributed under the GNU General Public License, version 3 or later. Its source is pinned to commit edb0d9db6731067ec50ce619ff372b463bc4dd5d.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        onClick = { uriHandler.openUri(STOCKFISH_SOURCE_URL) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Open Stockfish source and release")
                    }
                }
            }

            Text("GNU GPL v3", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            when {
                stockfishLicense != null -> SelectionContainer {
                    Text(stockfishLicense.orEmpty(), style = MaterialTheme.typography.bodySmall)
                }
                stockfishLicenseLoadFailed -> Text(
                    "The bundled GPL text could not be opened. The source release link above remains available.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                else -> Text("Loading license…", style = MaterialTheme.typography.bodyMedium)
            }

            Text("Embedded NNUE network · CC0-1.0", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(
                "The Stockfish 19 neural-network file nn-1a298aa575a0.nnue is embedded in the engine executable and released under CC0-1.0.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedButton(
                onClick = { uriHandler.openUri(STOCKFISH_NETWORK_URL) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Open NNUE network source")
            }
            when {
                networkLicense != null -> SelectionContainer {
                    Text(networkLicense.orEmpty(), style = MaterialTheme.typography.bodySmall)
                }
                networkLicenseLoadFailed -> Text(
                    "The bundled CC0-1.0 text could not be opened.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                else -> Text("Loading license…", style = MaterialTheme.typography.bodyMedium)
            }

            HudPanel(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Android Libraries · Apache-2.0", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(
                        "Chessin' includes the following open-source Android libraries licensed under the Apache License, Version 2.0:\n" +
                                "• chesslib (v1.3.7) - PGN parsing & board logic\n" +
                                "• AndroidSVG (v1.4) - SVG piece rendering\n" +
                                "• OkHttp (v4.12.0) - HTTP client",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text("Apache License 2.0", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            when {
                apacheLicense != null -> SelectionContainer {
                    Text(apacheLicense.orEmpty(), style = MaterialTheme.typography.bodySmall)
                }
                apacheLicenseLoadFailed -> Text(
                    "The bundled Apache-2.0 license text could not be opened.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                else -> Text("Loading license…", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(Modifier.height(12.dp))
        }
    }
}
