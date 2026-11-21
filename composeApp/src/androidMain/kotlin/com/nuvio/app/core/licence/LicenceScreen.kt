package com.nuvio.app.core.licence

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.IosShare
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import nuvio.composeapp.generated.resources.Res
import nuvio.composeapp.generated.resources.app_icon_original
import org.jetbrains.compose.resources.painterResource

// Fond bleu nuit, dégradé bleu → violet pour l'action principale (maquettes Nuvio Pro).
private val Night = Color(0xFF07080D)
private val Glow = Color(0xFF3B4BB8)
private val GradientStart = Color(0xFF5B8CFF)
private val GradientEnd = Color(0xFF8B5CF6)
private val Red = Color(0xFFFF6B6B)
private val Green = Color(0xFF34C759)
private val Muted = Color(0xFF9A9CA8)
private val CardTop = Color(0xFF1A1D33)
private val CardBottom = Color(0xFF10111A)
private val Edge = Color.White.copy(alpha = 0.10f)
private val DarkPill = Color(0xFF1B1C22)
private val StepRing = Color(0xFF7C8CFF)

private enum class LicenceStep { Welcome, RequestKey, EnterKey }

/**
 * Écran de Nuvio Pro tant qu'il n'y a pas de clé valable.
 * Accueil : logo, « Nuvio Pro », les 3 étapes, puis « J'ai une clé » et « Demander une clé » en bas.
 * « Demander une clé » : le code de l'appareil à copier ou envoyer, puis « J'ai reçu ma clé ».
 * « J'ai une clé » : le champ de la clé et « Activer ».
 */
@Composable
internal fun LicenceScreen(
    deviceCode: String,
    message: String?,
    onActivate: (String) -> Unit,
) {
    val context = LocalContext.current
    var input by remember { mutableStateOf("") }
    var copied by remember { mutableStateOf(false) }
    var step by remember { mutableStateOf(LicenceStep.Welcome) }
    // Une clé refusée ramène sur le champ de la clé, avec le message.
    LaunchedEffect(message) {
        if (message != null) step = LicenceStep.EnterKey
    }
    LaunchedEffect(copied) {
        if (copied) {
            delay(2_000)
            copied = false
        }
    }
    BackHandler(enabled = step != LicenceStep.Welcome) { step = LicenceStep.Welcome }

    val drift = rememberInfiniteTransition(label = "glow")
    val glow by drift.animateFloat(
        initialValue = 0.55f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(4_000), RepeatMode.Reverse),
        label = "glow",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Night),
    ) {
        // Lueur bleu nuit derrière le logo, qui respire doucement.
        Box(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 40.dp)
                .size(520.dp)
                .background(Brush.radialGradient(listOf(Glow.copy(alpha = glow * 0.55f), Color.Transparent))),
        )

        AnimatedContent(
            targetState = step,
            transitionSpec = { fadeIn(tween(220)) togetherWith fadeOut(tween(160)) },
            label = "licence_step",
            modifier = Modifier.fillMaxSize().safeDrawingPadding().imePadding(),
        ) { current ->
            when (current) {
                LicenceStep.Welcome -> StepLayout(
                    bottom = {
                        GradientButton(text = "J'ai une clé", icon = Icons.Rounded.Key) { step = LicenceStep.EnterKey }
                        Spacer(Modifier.height(12.dp))
                        DarkButton(text = "Demander une clé", icon = Icons.Rounded.ChatBubbleOutline) {
                            step = LicenceStep.RequestKey
                        }
                    },
                ) {
                    Spacer(Modifier.height(96.dp))
                    AppLogo(size = 132.dp)
                    Spacer(Modifier.height(28.dp))
                    Title("Nuvio Pro", 36)
                    Spacer(Modifier.height(10.dp))
                    Subtitle("Activez votre accès pour commencer")
                    Spacer(Modifier.height(40.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(18.dp),
                    ) {
                        StepRow(1, "Demandez une clé au vendeur")
                        StepRow(2, "Recevez votre clé d’activation")
                        StepRow(3, "Collez-la pour débloquer l’accès")
                    }
                }

                LicenceStep.RequestKey -> StepLayout(
                    onBack = { step = LicenceStep.Welcome },
                    bottom = {
                        GradientButton(text = "J'ai reçu ma clé", icon = Icons.Rounded.Key) { step = LicenceStep.EnterKey }
                    },
                ) {
                    Spacer(Modifier.height(24.dp))
                    AppLogo(size = 88.dp)
                    Spacer(Modifier.height(24.dp))
                    Title("Demander une clé", 30)
                    Spacer(Modifier.height(10.dp))
                    Subtitle("Envoyez ce code au vendeur pour recevoir votre clé")
                    Spacer(Modifier.height(32.dp))
                    NightCard {
                        Label("Code de cet appareil")
                        Text(
                            deviceCode,
                            color = Color.White,
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 6.sp,
                            maxLines = 1,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            DarkButton(
                                text = if (copied) "Copié" else "Copier",
                                icon = if (copied) Icons.Rounded.Check else Icons.Rounded.ContentCopy,
                                tint = if (copied) Green else Color.White,
                                modifier = Modifier.weight(1f),
                            ) {
                                copyToClipboard(context, deviceCode)
                                copied = true
                            }
                            DarkButton(
                                text = "Envoyer",
                                icon = Icons.Rounded.IosShare,
                                modifier = Modifier.weight(1f),
                            ) { shareCode(context, deviceCode) }
                        }
                    }
                    Spacer(Modifier.height(18.dp))
                    Text(
                        "La clé reçue ne fonctionnera que sur cet appareil.",
                        color = Muted.copy(alpha = 0.85f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                    )
                }

                LicenceStep.EnterKey -> StepLayout(
                    onBack = { step = LicenceStep.Welcome },
                    bottom = {
                        GradientButton(text = "Activer", icon = null, enabled = input.isNotBlank()) { onActivate(input) }
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Pas encore de clé ? Demander une clé",
                            color = StepRing,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .clip(RoundedCornerShape(10.dp))
                                .clickable { step = LicenceStep.RequestKey }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                        )
                    },
                ) {
                    Spacer(Modifier.height(24.dp))
                    AppLogo(size = 88.dp)
                    Spacer(Modifier.height(24.dp))
                    Title("Saisir ma clé", 30)
                    Spacer(Modifier.height(10.dp))
                    Subtitle("Collez la clé d’activation reçue du vendeur")
                    Spacer(Modifier.height(32.dp))
                    NightCard {
                        Label("Clé d’activation")
                        OutlinedTextField(
                            value = input,
                            onValueChange = { input = it },
                            placeholder = { Text("Collez votre clé ici", color = Muted) },
                            minLines = 3,
                            isError = message != null,
                            textStyle = TextStyle(color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 15.sp),
                            shape = RoundedCornerShape(18.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = StepRing,
                                unfocusedBorderColor = Edge,
                                errorBorderColor = Red,
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                errorTextColor = Color.White,
                                cursorColor = StepRing,
                                focusedContainerColor = Color.White.copy(alpha = 0.04f),
                                unfocusedContainerColor = Color.White.copy(alpha = 0.04f),
                                errorContainerColor = Color.White.copy(alpha = 0.04f),
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                        AnimatedVisibility(visible = message != null, enter = fadeIn(), exit = fadeOut()) {
                            Text(message.orEmpty(), color = Red, fontSize = 15.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}

/** Contenu défilant en haut, boutons fixés en bas ; flèche retour ronde en haut à gauche si [onBack]. */
@Composable
private fun StepLayout(
    onBack: (() -> Unit)? = null,
    bottom: @Composable ColumnScope.() -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 28.dp)) {
        if (onBack != null) {
            Box(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .size(60.dp)
                    .clip(CircleShape)
                    .background(DarkPill)
                    .border(1.dp, Edge, CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Retour", tint = Color.White)
            }
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            content = content,
        )
        Column(modifier = Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 20.dp), content = bottom)
    }
}

@Composable
private fun AppLogo(size: androidx.compose.ui.unit.Dp) {
    Image(
        painter = painterResource(Res.drawable.app_icon_original),
        contentDescription = "Nuvio",
        modifier = Modifier.size(size).clip(RoundedCornerShape(size * 0.24f)),
    )
}

@Composable
private fun Title(text: String, sizeSp: Int) {
    Text(text, color = Color.White, fontSize = sizeSp.sp, fontWeight = FontWeight.Black, textAlign = TextAlign.Center)
}

@Composable
private fun Subtitle(text: String) {
    Text(text, color = Muted, fontSize = 17.sp, textAlign = TextAlign.Center, lineHeight = 24.sp)
}

@Composable
private fun StepRow(number: Int, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .clip(CircleShape)
                .background(Color(0xFF151A3A))
                .border(1.dp, StepRing.copy(alpha = 0.35f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text("$number", color = StepRing, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        Spacer(Modifier.width(16.dp))
        Text(text, color = Color.White.copy(alpha = 0.78f), fontSize = 17.sp)
    }
}

@Composable
private fun NightCard(content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Brush.verticalGradient(listOf(CardTop, CardBottom)))
            .border(1.dp, Edge, shape)
            .padding(22.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        content = content,
    )
}

@Composable
private fun Label(text: String) {
    Text(text.uppercase(), color = Muted, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 1.2.sp)
}

@Composable
private fun GradientButton(
    text: String,
    icon: ImageVector?,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(62.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(shape)
            .background(Brush.horizontalGradient(listOf(GradientStart, GradientEnd)))
            .clickable(enabled = enabled, onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
        }
        Text(text, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun DarkButton(
    text: String,
    icon: ImageVector?,
    modifier: Modifier = Modifier.fillMaxWidth(),
    tint: Color = Color.White,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(50)
    Row(
        modifier = modifier
            .height(62.dp)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(Color(0xFF23242B), DarkPill)))
            .border(1.dp, Edge, shape)
            .clickable(onClick = onClick),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(10.dp))
        }
        Text(text, color = tint, fontSize = 18.sp, fontWeight = FontWeight.Bold)
    }
}

private fun copyToClipboard(context: Context, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText("Code de l’appareil", text))
    Toast.makeText(context, "Code copié : $text", Toast.LENGTH_SHORT).show()
}

/** Ouvre le partage Android (WhatsApp, SMS…) avec le code de l'appareil. */
private fun shareCode(context: Context, code: String) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, "Bonjour, voici le code de mon appareil pour Nuvio Pro : $code")
    }
    context.startActivity(Intent.createChooser(send, "Envoyer le code").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
}
