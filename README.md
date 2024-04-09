# Markvisor: Generative AI for Product Promotion

Markvisor is a demo app that demonstrates how it can generate engaging product promotions from images. It leverages the multimodal capabilities of Gemini 1.0 Pro Vision, driven by the powerful Gemini API.

## Setup

Before using Markvisor, you need to replace the dummy API key in `gradle.properties` with a valid key generated from Google AI Studio:
```
API_KEY=dummyKey
```

## Features

### Language Support

At present, Markvisor supports English and Traditional Chinese languages. To see how the AI performs in different languages, you can easily toggle between these two languages within the app itself, without having to change the language settings on your device.

### Customizable Prompts

Markvisor allows you to customize the tone and style of the generated product promotions by modifying keywords on the screen. Additionally, you can directly modify the prompt used by the app within the code. The current prompt structure is as follows:

```
Create an engaging product advertisement, up to 80 words, based on the given image. Use the user provided tone style (wrapped by triple quote """) 

"""$tone"""

to effectively highlight the product's key qualities. Additionally, suggest five relevant tags with format (#CONTENT) that best describe the featured item. Provide your response in locale "$language"
```

Note that `$tone` is a placeholder that can be replaced by users directly in the app's UI to change the desired tone.

### Theme Adaptation

The app automatically adapts to your system's Dark or Light theme preferences, providing a seamless visual experience.

Alternatively, you can enable the Dynamic Color feature within the `MarkvisorTheme`. This option is defined as follows:

```kotlin
fun MarkvisorTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true, // default is false
    content: @Composable (colorScheme: ColorScheme) -> Unit
) 
```





