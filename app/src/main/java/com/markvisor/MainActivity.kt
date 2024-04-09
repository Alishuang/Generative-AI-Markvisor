package com.markvisor

import android.content.Context
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.markvisor.ui.theme.MarkvisorTheme
import java.util.Locale

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels<MainViewModel>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MarkvisorTheme { colorScheme ->
                Scaffold(
                    topBar = {
                        Text(
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                                .padding(12.dp)
                                .background(colorScheme.background),
                            textAlign = TextAlign.Center,
                            color = colorScheme.primary,
                            fontSize = 20.sp,
                            text = getString(R.string.app_name),
                            fontWeight = FontWeight.Bold,
                        )
                    },
                    content = { paddingValues ->
                        Content(
                            modifier = Modifier
                                .background(
                                    colorScheme.background,
                                )
                                .padding(paddingValues)
                                .fillMaxSize(),
                            colorScheme,
                        )
                    },
                )
            }
        }
    }

    @Composable
    fun Content(modifier: Modifier, colorScheme: ColorScheme) {
        val scrollState = rememberScrollState()
        val currentLocale = remember { mutableStateOf(Locale.getDefault()) }
        Column(
            modifier = modifier
                .verticalScroll(scrollState)
                .padding(vertical = 12.dp, horizontal = 28.dp)
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val imageUri: MutableState<Uri?> = remember { mutableStateOf(null) }
            val result by viewModel.result.collectAsState(initial = null)

            if (imageUri.value == null) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(data = R.drawable.main_image)
                        .crossfade(true)
                        .build(),
                    contentScale = ContentScale.Crop,
                    contentDescription = "default image",
                    modifier = Modifier
                        .clip(RoundedCornerShape(15f))
                        .fillMaxWidth(),
                    alignment = Alignment.Center,
                )
            } else {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current)
                        .data(data = imageUri.value)
                        .crossfade(true)
                        .build(),
                    contentDescription = "uploaded image",
                    modifier = Modifier
                        .clip(RoundedCornerShape(15f))
                        .fillMaxSize(fraction = 0.5f),
                    alignment = Alignment.Center,
                )
            }

            val defaultTone: String = stringResource(R.string.default_tone)
            val tone = remember { mutableStateOf(defaultTone) }

            GalleryImagePicker(
                onImageSelected = {
                    imageUri.value = it
                    viewModel.recycleBitmap()
                    startGenerateContent(it, tone.value)
                },
                colorScheme = colorScheme,
            )

            PromptGuidance(colorScheme, tone, defaultTone, imageUri)

            imageUri.value?.let {
                GeneratedContent(result, colorScheme, scrollState)
            }

            ActionItems(result, imageUri, colorScheme, tone, currentLocale)
        }
    }

    @Composable
    fun GalleryImagePicker(
        onImageSelected: (Uri) -> Unit,
        colorScheme: ColorScheme,
    ) {
        val hasUploadedImage = remember { mutableStateOf(false) }
        val launcher = rememberLauncherForActivityResult(
            contract = GetImageContract(),
            onResult = { uri: Uri? ->
                uri?.let {
                    onImageSelected(it)
                    hasUploadedImage.value = true
                }
            },
        )

        RoundedCornerTextButton(
            modifier = Modifier
                .padding(top = 20.dp)
                .fillMaxWidth()
                .wrapContentHeight(),
            colorScheme = colorScheme,
            label = if (!hasUploadedImage.value) {
                stringResource(R.string.pick_image)
            } else {
                stringResource(R.string.change_image)
            },
        ) {
            launcher.launch(arrayOf("image/jpeg", "image/png"))
        }
    }

    @Composable
    private fun PromptGuidance(
        colorScheme: ColorScheme,
        tone: MutableState<String>,
        defaultTone: String,
        imageUri: MutableState<Uri?>,
    ) {
        val fontSize = 16.sp
        Text(
            modifier = Modifier.padding(20.dp),
            text = stringResource(R.string.generate_content_prefix),
            fontSize = fontSize,
            color = colorScheme.primary,
        )

        Box(
            modifier = Modifier
                .wrapContentSize()
                .background(
                    color = colorScheme.primary.copy(alpha = 0.3f),
                    shape = RoundedCornerShape(8.dp),
                ),
            contentAlignment = Alignment.Center,
        ) {
            val focusManager = LocalFocusManager.current

            BasicTextField(
                value = tone.value,
                onValueChange = {
                    tone.value = it
                },
                keyboardActions = KeyboardActions(onAny = {
                    if (tone.value.isEmpty()) {
                        tone.value = defaultTone
                    }
                    imageUri.value?.let {
                        startGenerateContent(it, tone.value)
                    }
                    focusManager.clearFocus()
                }),
                modifier = Modifier
                    .padding(4.dp)
                    .wrapContentSize(),
                textStyle = TextStyle(
                    fontSize = fontSize,
                    color = colorScheme.primary,
                    textAlign = TextAlign.Center,
                ),
                singleLine = true,
            )
        }
        Text(
            text = stringResource(R.string.generate_content_suffix),
            modifier = Modifier
                .padding(vertical = 20.dp)
                .wrapContentHeight(),
            fontSize = fontSize,
            color = colorScheme.primary,
        )
    }

    @Composable
    private fun GeneratedContent(
        result: Resource<String>?,
        colorScheme: ColorScheme,
        scrollState: ScrollState,
    ) {
        result?.let { data ->
            when (data) {
                is Resource.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                    ) {
                        CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                    }
                }

                is Resource.Success -> {
                    data.data?.let {
                        SelectionContainer {
                            Text(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight()
                                    .border(
                                        width = 1.dp,
                                        color = colorScheme.primary,
                                        shape = RoundedCornerShape(15.dp),
                                    )
                                    .padding(16.dp),
                                text = it,
                                fontSize = 16.sp,
                                color = colorScheme.primary,
                            )
                        }

                        LaunchedEffect(it.length) {
                            scrollState.scrollTo(Int.MAX_VALUE)
                        }
                    }
                }

                is Resource.Error -> {
                    data.message?.let {
                        Text(
                            modifier = Modifier.padding(16.dp),
                            text = "Error: $it",
                            color = colorScheme.primary,
                        )
                    }
                }
            }
        }
    }

    @Composable
    private fun ActionItems(
        result: Resource<String>?,
        imageUri: MutableState<Uri?>,
        colorScheme: ColorScheme,
        tone: MutableState<String>,
        currentLocale: MutableState<Locale>,
    ) {
        if (result !is Resource.Loading) {
            imageUri.value?.let { uri ->
                Row(
                    modifier = Modifier
                        .padding(top = 20.dp)
                        .fillMaxWidth()
                        .wrapContentHeight(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    RoundedCornerTextButton(
                        modifier = Modifier
                            .wrapContentSize(),
                        colorScheme = colorScheme,
                        label = stringResource(R.string.regenerate),
                    ) {
                        startGenerateContent(
                            uri,
                            tone.value,
                        )
                    }

                    LanguageSwitcher(
                        colorScheme,
                        currentLocale.value,
                    ) { locale: Locale ->
                        setAppLocale(this@MainActivity, newLocale(currentLocale))
                        currentLocale.value = locale
                        tone.value = this@MainActivity.getString(R.string.default_tone)
                        startGenerateContent(
                            uri,
                            tone.value,
                        )
                    }
                }
            }
        }
    }

    private fun startGenerateContent(
        imageUri: Uri,
        tone: String,
    ) {
        viewModel.getResultFromModule(
            this@MainActivity,
            imageUri,
            tone,
            Locale.getDefault().toLanguageTag()
        )
    }

    private fun newLocale(currentLocale: MutableState<Locale>): Locale {
        return if (currentLocale.value != Locale.TAIWAN) {
            Locale.TAIWAN
        } else {
            Locale.ENGLISH
        }
    }

    @Composable
    private fun LanguageSwitcher(
        colorScheme: ColorScheme,
        currentLocale: Locale,
        onClick: (Locale) -> Unit,
    ) {
        val locale = if (currentLocale.language == Locale.TAIWAN.language) {
            Locale.TAIWAN
        } else {
            Locale.ENGLISH
        }
        // Fix wording that shouldn't be changed by any locale
        SwitchButton(Locale.ENGLISH, locale, colorScheme, "English", onClick)
        SwitchButton(Locale.TAIWAN, locale, colorScheme, "中文", onClick)
    }

    @Composable
    private fun SwitchButton(
        locale: Locale,
        currentLocale: Locale,
        colorScheme: ColorScheme,
        label: String,
        onClick: (newLocale: Locale) -> Unit,
    ) {
        val isSelected = currentLocale == locale
        SwitchTextButton(
            modifier = Modifier
                .padding(start = 8.dp)
                .wrapContentSize(),
            isSelected = isSelected,
            colorScheme = colorScheme,
            onClick = {
                if (!isSelected) {
                    onClick(locale)
                }
            },
        ) {
            Text(
                fontSize = 16.sp,
                color = if (isSelected) colorScheme.background else colorScheme.primary.copy(alpha = 0.5f),
                text = label,
            )
        }
    }

    @Composable
    private fun SwitchTextButton(
        modifier: Modifier,
        isSelected: Boolean,
        colorScheme: ColorScheme,
        onClick: () -> Unit,
        content: @Composable RowScope.() -> Unit,
    ) {
        if (isSelected) {
            TextButton(
                modifier = modifier,
                colors = ButtonDefaults.buttonColors(
                    contentColor = colorScheme.background,
                    containerColor = colorScheme.primary,
                ),
                onClick = onClick,
                content = content,
            )
        } else {
            TextButton(
                modifier = modifier,
                border = BorderStroke(
                    width = 1.dp,
                    color = colorScheme.primary.copy(alpha = 0.5f),
                ),
                onClick = onClick,
                content = content,
            )
        }
    }

    @Composable
    private fun RoundedCornerTextButton(
        modifier: Modifier,
        colorScheme: ColorScheme,
        label: String,
        onClick: () -> Unit,
    ) {
        TextButton(
            modifier = modifier,
            onClick = onClick,
            shape = RoundedCornerShape(30.dp),
            colors = ButtonDefaults.buttonColors(
                contentColor = colorScheme.background,
                containerColor = colorScheme.primary,
            ),
        ) {
            Text(
                modifier = Modifier,
                fontSize = 16.sp,
                color = colorScheme.background,
                text = label,
            )
        }
    }

    private fun setAppLocale(context: Context, locale: Locale) {
        val resources = context.resources
        val configuration = Configuration(resources.configuration)
        Locale.setDefault(locale)
        configuration.setLocale(locale)
        resources.updateConfiguration(configuration, resources.displayMetrics)
    }
}
