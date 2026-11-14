package io.github.rulecade.ui.catalog

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.rulecade.R
import io.github.rulecade.catalog.Catalog
import io.github.rulecade.catalog.Grade
import io.github.rulecade.catalog.Sign
import io.github.rulecade.catalog.SignCategory
import io.github.rulecade.catalog.grade
import io.github.rulecade.progress.Progress
import io.github.rulecade.ui.component.COMPACT_WIDTH
import io.github.rulecade.ui.component.RcButton
import io.github.rulecade.ui.component.RcButtonStyle
import io.github.rulecade.ui.component.RcTab
import io.github.rulecade.ui.library.Header
import io.github.rulecade.ui.library.LibraryLink
import io.github.rulecade.ui.library.Page
import io.github.rulecade.ui.library.dateText
import io.github.rulecade.ui.license.RoadSign
import io.github.rulecade.ui.license.RoadSignImage
import io.github.rulecade.ui.theme.Radius
import io.github.rulecade.ui.theme.RulecadeTheme
import io.github.rulecade.ui.theme.Spacing

private val TILE_MIN_WIDTH = 220.dp
private val TILE_SIGN = 120.dp
private val DETAIL_SIGN = 260.dp
private val DETAIL_SIGN_COMPACT = 180.dp
private val STAR = 28.dp
private val SWATCH = 24.dp
private val BORDER = 3.dp
private const val IDENTITY_WEIGHT = 0.8f
private const val DIMMED_ALPHA = 0.55f

val SignCategory.tabLabel: Int
    get() = when (this) {
        SignCategory.Caution -> R.string.catalog_tab_caution
        SignCategory.Regulation -> R.string.catalog_tab_regulation
        SignCategory.Instruction -> R.string.catalog_tab_instruction
    }

private val SignCategory.kindLabel: Int
    get() = when (this) {
        SignCategory.Caution -> R.string.catalog_kind_caution
        SignCategory.Regulation -> R.string.catalog_kind_regulation
        SignCategory.Instruction -> R.string.catalog_kind_instruction
    }

private val Grade.label: Int
    get() = when (this) {
        Grade.Uncollected -> R.string.grade_uncollected
        Grade.Discovered -> R.string.grade_discovered
        Grade.Collected -> R.string.grade_collected
        Grade.Mastered -> R.string.grade_mastered
    }

// Background, edge and text colors of a grade; never the only cue, the grade is also written.
private data class GradeColors(val fill: Color, val edge: Color, val text: Color)

@Composable
private fun Grade.colors(): GradeColors {
    val colors = RulecadeTheme.colors
    return when (this) {
        Grade.Uncollected -> GradeColors(
            colors.surfaceTile,
            colors.gradeSilverSoft,
            colors.gradeSilver
        )
        Grade.Discovered -> GradeColors(
            colors.gradeSilverSoft,
            colors.gradeSilver,
            colors.textSecondary
        )
        Grade.Collected -> GradeColors(colors.gradeGoldSoft, colors.gradeGoldLine, colors.gradeGold)
        Grade.Mastered -> GradeColors(colors.accentSoft, colors.accent, colors.accent)
    }
}

private val grayscale = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0f) })

// Signs not collected yet are shown faded and grey, with their name, so it is clear what to solve.
private fun Modifier.faded() = graphicsLayer { alpha = DIMMED_ALPHA }.drawWithContent {
    val paint = Paint().apply { colorFilter = grayscale }
    drawIntoCanvas { canvas ->
        canvas.saveLayer(Rect(Offset.Zero, size), paint)
        drawContent()
        canvas.restore()
    }
}

@Composable
private fun SignImage(sign: Sign, modifier: Modifier) {
    RoadSign.of(sign.number)?.let { RoadSignImage(it, modifier) }
}

// Signs by category with their grades from ranked play.
@Composable
fun CatalogScreen(
    catalog: Catalog,
    progress: Progress,
    category: SignCategory?,
    onCategory: (SignCategory) -> Unit,
    onOpen: (Sign) -> Unit,
    onBack: () -> Unit
) = BoxWithConstraints(Modifier.fillMaxSize()) {
    val compact = maxWidth < COMPACT_WIDTH
    // Categories without signs stay hidden.
    val categories = SignCategory.entries.filter { catalog.byCategory(it).isNotEmpty() }
    val current = category?.takeIf { it in categories } ?: categories.firstOrNull()
    val signs = current?.let { catalog.byCategory(it) }.orEmpty()
    val collected = catalog.signs.count { progress.grade(it) >= Grade.Collected }
    Page(compact) {
        Header(
            stringResource(R.string.sign_catalog),
            onBack,
            stringResource(R.string.catalog_collected, collected, catalog.signs.size)
        )
        FlowRow(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.small),
            verticalArrangement = Arrangement.spacedBy(Spacing.small)
        ) {
            categories.forEach { c ->
                RcTab(stringResource(c.tabLabel), c == current) { onCategory(c) }
            }
        }
        LazyVerticalGrid(
            columns = GridCells.Adaptive(TILE_MIN_WIDTH),
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(Spacing.medium),
            verticalArrangement = Arrangement.spacedBy(Spacing.medium)
        ) {
            items(signs, key = { it.id }) { SignTile(it, progress.grade(it)) { onOpen(it) } }
        }
        Legend()
    }
}

@Composable
private fun SignTile(sign: Sign, grade: Grade, onOpen: () -> Unit) {
    val colors = RulecadeTheme.colors
    val type = RulecadeTheme.typography
    val look = grade.colors()
    val shape = RoundedCornerShape(Radius.card)
    Box(
        Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(colors.surfaceTile)
            .background(look.fill)
            .border(BORDER, look.edge, shape)
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(Spacing.medium)
    ) {
        Column(
            Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Spacing.small)
        ) {
            val dim = if (grade == Grade.Uncollected) Modifier.faded() else Modifier
            SignImage(sign, Modifier.size(TILE_SIGN).then(dim))
            Text(
                sign.name,
                style = type.label,
                color = if (grade == Grade.Uncollected) colors.textTertiary else colors.textPrimary,
                textAlign = TextAlign.Center
            )
            Text(stringResource(grade.label), style = type.label, color = look.text)
        }
        if (grade == Grade.Mastered) {
            Icon(
                painterResource(R.drawable.ic_star),
                contentDescription = null,
                modifier = Modifier.align(Alignment.TopEnd).size(STAR),
                tint = colors.accent
            )
        }
    }
}

@Composable
private fun Legend() = FlowRow(
    Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(Spacing.large),
    verticalArrangement = Arrangement.spacedBy(Spacing.small)
) {
    listOf(
        Grade.Uncollected to R.string.catalog_legend_uncollected,
        Grade.Discovered to R.string.catalog_legend_discovered,
        Grade.Collected to R.string.catalog_legend_collected,
        Grade.Mastered to R.string.catalog_legend_mastered
    ).forEach { (grade, text) ->
        val look = grade.colors()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.extraSmall)
        ) {
            Box(
                Modifier
                    .size(SWATCH)
                    .background(look.fill, RoundedCornerShape(Spacing.extraSmall))
                    .border(BORDER, look.edge, RoundedCornerShape(Spacing.extraSmall))
            )
            Text(stringResource(text), style = RulecadeTheme.typography.label, color = look.text)
        }
    }
}

@Composable
private fun GradeBadge(grade: Grade) {
    val look = grade.colors()
    Box(
        Modifier
            .background(look.fill, CircleShape)
            .border(BORDER / 2, look.edge, CircleShape)
            .padding(horizontal = Spacing.small, vertical = Spacing.extraSmall / 2)
    ) {
        Text(stringResource(grade.label), style = RulecadeTheme.typography.label, color = look.text)
    }
}

// One sign: its drawing, the annex meaning, how many linked questions are solved, the library
// topic and practice with its questions.
@Composable
fun SignScreen(
    sign: Sign,
    grade: Grade,
    solved: Int,
    checked: String,
    onPractice: () -> Unit,
    onBack: () -> Unit
) = BoxWithConstraints(Modifier.fillMaxSize().background(RulecadeTheme.colors.backgroundPrimary)) {
    val compact = maxWidth < COMPACT_WIDTH
    BackHandler(onBack = onBack)
    val practice = @Composable { modifier: Modifier ->
        RcButton(
            text = stringResource(R.string.library_practice, sign.questions.size),
            onClick = onPractice,
            modifier = modifier,
            style = RcButtonStyle.Accent,
            large = true,
            icon = R.drawable.ic_pencil
        )
    }
    Page(compact) {
        Header(stringResource(R.string.sign_title, stringResource(sign.category.tabLabel)), onBack)
        if (compact) {
            Column(
                Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(Spacing.extraLarge)
            ) {
                Identity(sign, grade, DETAIL_SIGN_COMPACT)
                Details(sign, solved, checked)
            }
            practice(Modifier.fillMaxWidth())
        } else {
            Row(
                Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(Spacing.extraLarge)
            ) {
                Column(
                    Modifier.weight(IDENTITY_WEIGHT).fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                ) {
                    Identity(sign, grade, DETAIL_SIGN)
                }
                Column(
                    Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.spacedBy(Spacing.medium)
                ) {
                    Column(
                        Modifier.weight(1f).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(Spacing.medium)
                    ) {
                        Details(sign, solved, checked)
                    }
                    practice(Modifier.fillMaxWidth())
                }
            }
        }
    }
}

@Composable
private fun Identity(sign: Sign, grade: Grade, signSize: Dp) =
    Column(verticalArrangement = Arrangement.spacedBy(Spacing.medium)) {
        SignImage(sign, Modifier.size(signSize))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.small)
        ) {
            Text(sign.name, style = RulecadeTheme.typography.headlineSmall)
            GradeBadge(grade)
        }
        Text(
            stringResource(
                R.string.sign_number,
                stringResource(sign.category.kindLabel),
                sign.number
            ),
            style = RulecadeTheme.typography.bodyLarge,
            color = RulecadeTheme.colors.textTertiary
        )
    }

@Composable
private fun Details(sign: Sign, solved: Int, checked: String) {
    val colors = RulecadeTheme.colors
    val type = RulecadeTheme.typography
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.surfaceTile, RoundedCornerShape(Radius.tile))
            .padding(Spacing.large),
        verticalArrangement = Arrangement.spacedBy(Spacing.small)
    ) {
        Text(
            stringResource(R.string.sign_meaning),
            style = type.label,
            color = colors.textSecondary
        )
        Text("“${sign.meaning}”", style = type.bodyLarge)
    }
    Column(
        Modifier
            .fillMaxWidth()
            .background(colors.surfaceCard, RoundedCornerShape(Radius.tile))
            .padding(Spacing.large),
        verticalArrangement = Arrangement.spacedBy(Spacing.small)
    ) {
        Text(stringResource(R.string.sign_linked), style = type.titleSmall)
        Fact(
            stringResource(R.string.sign_solved),
            stringResource(R.string.sign_solved_value, solved, sign.questions.size)
        )
        val left = sign.questions.size - solved
        Fact(
            stringResource(R.string.sign_to_master),
            if (left == 0) {
                stringResource(R.string.sign_mastered)
            } else {
                stringResource(R.string.sign_to_master_value, left)
            }
        )
    }
    LibraryLink(sign.questions.first())
    Text(
        stringResource(R.string.sign_source, dateText(checked)),
        style = type.bodyMedium,
        color = colors.textTertiary
    )
}

@Composable
private fun Fact(label: String, value: String) =
    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.medium)) {
        Text(
            label,
            Modifier.weight(1f),
            style = RulecadeTheme.typography.bodyLarge,
            color = RulecadeTheme.colors.textSecondary
        )
        Text(value, style = RulecadeTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold))
    }
