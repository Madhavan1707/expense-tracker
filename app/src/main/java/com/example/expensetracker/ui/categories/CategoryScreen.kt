package com.example.expensetracker.ui.categories

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.expensetracker.AppViewModelProvider
import com.example.expensetracker.R
import com.example.expensetracker.data.Category
import com.example.expensetracker.data.DefaultCategories
import com.example.expensetracker.data.EmojiCatalog
import com.example.expensetracker.ui.components.CategoryAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CategoryViewModel = viewModel(factory = AppViewModelProvider.Factory),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<Category?>(null) }
    var creating by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.categories_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { creating = true }) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = stringResource(R.string.category_new),
                )
            }
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize(),
        ) {
            items(items = uiState.active, key = { it.id }) { category ->
                CategoryRow(
                    category = category,
                    canMoveUp = category != uiState.active.first(),
                    canMoveDown = category != uiState.active.last(),
                    onEdit = { editing = category },
                    onMoveUp = { viewModel.moveUp(category) },
                    onMoveDown = { viewModel.moveDown(category) },
                    onArchive = { viewModel.setArchived(category, true) },
                    onRestore = { viewModel.setArchived(category, false) },
                )
            }

            if (uiState.archived.isNotEmpty()) {
                item(key = "archived-header") {
                    Text(
                        text = stringResource(R.string.category_archived_header),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, top = 24.dp, bottom = 8.dp),
                    )
                }
                item(key = "archived-note") {
                    Text(
                        text = stringResource(R.string.category_archived_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 8.dp),
                    )
                }
                items(items = uiState.archived, key = { it.id }) { category ->
                    CategoryRow(
                        category = category,
                        canMoveUp = false,
                        canMoveDown = false,
                        onEdit = { editing = category },
                        onMoveUp = {},
                        onMoveDown = {},
                        onArchive = { viewModel.setArchived(category, true) },
                        onRestore = { viewModel.setArchived(category, false) },
                    )
                }
            }

            item(key = "bottom-space") { Spacer(Modifier.height(96.dp)) }
        }
    }

    val target = editing
    if (target != null) {
        CategoryEditorDialog(
            initial = target,
            onDismiss = { editing = null },
            onConfirm = { name, emoji, color ->
                viewModel.update(target.copy(name = name, emoji = emoji, colorArgb = color))
                editing = null
            },
        )
    }

    if (creating) {
        CategoryEditorDialog(
            initial = null,
            onDismiss = { creating = false },
            onConfirm = { name, emoji, color ->
                viewModel.add(name, emoji, color)
                creating = false
            },
        )
    }
}

@Composable
private fun CategoryRow(
    category: Category,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onEdit: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CategoryAvatar(emoji = category.emoji, colorArgb = category.colorArgb, size = 40.dp)
        Spacer(Modifier.width(14.dp))
        Text(
            text = category.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )

        Box {
            IconButton(onClick = { menuOpen = true }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.category_options, category.name),
                )
            }
            DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.category_edit_action)) },
                    onClick = { menuOpen = false; onEdit() },
                )
                if (canMoveUp) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.category_move_up)) },
                        onClick = { menuOpen = false; onMoveUp() },
                    )
                }
                if (canMoveDown) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.category_move_down)) },
                        onClick = { menuOpen = false; onMoveDown() },
                    )
                }
                if (category.isArchived) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.category_restore)) },
                        onClick = { menuOpen = false; onRestore() },
                    )
                } else {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.category_archive)) },
                        onClick = { menuOpen = false; onArchive() },
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CategoryEditorDialog(
    initial: Category?,
    onDismiss: () -> Unit,
    onConfirm: (name: String, emoji: String, colorArgb: Int) -> Unit,
) {
    var name by remember { mutableStateOf(initial?.name.orEmpty()) }
    var emoji by remember { mutableStateOf(initial?.emoji ?: EmojiCatalog.DEFAULT) }
    var color by remember { mutableStateOf(initial?.colorArgb ?: DefaultCategories.palette.first()) }
    var iconQuery by remember { mutableStateOf("") }

    val icons = remember(iconQuery) { EmojiCatalog.search(iconQuery) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (initial == null) R.string.category_new else R.string.category_edit
                )
            )
        },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryAvatar(emoji = emoji, colorArgb = color, size = 48.dp)
                    Spacer(Modifier.width(12.dp))
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.category_name)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(20.dp))
                Text(stringResource(R.string.category_icon), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = iconQuery,
                    onValueChange = { iconQuery = it },
                    placeholder = { Text(stringResource(R.string.category_icon_search)) },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    trailingIcon = {
                        if (iconQuery.isNotEmpty()) {
                            IconButton(onClick = { iconQuery = "" }) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = stringResource(R.string.category_icon_search_clear),
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )

                Spacer(Modifier.height(8.dp))
                if (icons.isEmpty()) {
                    Text(
                        text = stringResource(R.string.category_icon_no_match),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 24.dp),
                    )
                } else {
                    // Capped, not pinned: the grid holds 230-odd icons and would
                    // push the colour picker off the screen given its head, but a
                    // search narrowed to one hit should not leave a hole either.
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(6),
                        modifier = Modifier.heightIn(max = 216.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(items = icons, key = { it.emoji }) { candidate ->
                            // The keywords the catalogue already carries make a
                            // better spoken label than the emoji on its own,
                            // which a screen reader reads by its Unicode name.
                            val iconLabel = candidate.keywords.firstOrNull() ?: candidate.emoji
                            Box(
                                modifier = Modifier
                                    // Was 38dp, under the 48dp minimum target.
                                    .size(TOUCH_TARGET)
                                    .clip(CircleShape)
                                    .background(
                                        if (candidate.emoji == emoji) {
                                            MaterialTheme.colorScheme.secondaryContainer
                                        } else {
                                            Color.Transparent
                                        }
                                    )
                                    .selectable(
                                        selected = candidate.emoji == emoji,
                                        role = Role.RadioButton,
                                        onClick = { emoji = candidate.emoji },
                                    )
                                    .semantics { contentDescription = iconLabel },
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = candidate.emoji,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text(stringResource(R.string.category_colour), style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(8.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    maxItemsInEachRow = 6,
                ) {
                    DefaultCategories.palette.forEachIndexed { index, swatch ->
                        // A screen reader had nothing to announce here: thirty
                        // unlabelled boxes whose only state was a border. The
                        // swatch is numbered because a colour has no fixed name.
                        val label = stringResource(R.string.category_colour_swatch, index + 1)
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                // Was 32dp, well under the 48dp minimum. The
                                // painted circle stays 32dp; the target grew.
                                .size(TOUCH_TARGET)
                                .selectable(
                                    selected = swatch == color,
                                    role = Role.RadioButton,
                                    onClick = { color = swatch },
                                )
                                .semantics { contentDescription = label },
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(swatch))
                                    .border(
                                        width = if (swatch == color) 3.dp else 0.dp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        shape = CircleShape,
                                    )
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim(), emoji, color) },
                enabled = name.isNotBlank(),
            ) {
                Text(stringResource(R.string.save), fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

/** The Android minimum touch target. */
private val TOUCH_TARGET = 48.dp
