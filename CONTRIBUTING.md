# Contributing to Exif Notes

First off, thanks for your interest in contributing to Exif Notes! There are few ways in which you can help develop Exif Notes.

- Bug reports
- Feature requests / improvement ideas
- Contributing code

## Bug reports

Before filing a new bug report, please search through existing issues to see whether an issue was already created regarding the bug you have encountered. If there was no corresponding issue to be found, you can create a new issue to report the bug. When creating bug report issues, please make sure you include the following things in your report.
1. Concise but specific title 
2. Description of the bug
3. Steps to reproduce
4. Expected behaviour
5. System information
    - Android version
    - Device make and model

## Feature requests / improvement ideas

Same as with bug reports, please check existing issues to see whether a similar feature or improvement was already suggested by someone. If not, feel free to create an issue describing the new feature or improvement idea.
1. Clear and descriptive title
2. Current behaviour (when posting an improvement idea)
3. New feature or behaviour
4. Why this enhancement would be useful

## Contributing code

If you feel like you're up to the task, code contributions are most welcome! Here are some guidelines on how to get started.
- Use Kotlin instead of Java.
    - The entire codebase of Exif Notes is written in Kotlin (apart from resource and build files).
    - Kotlin is the recommended language for Android development (https://developer.android.com/kotlin/first).
- Find an issue to work on and let others know you are working on that issue.
    - If there is no issue for the change you are making, please create the issue first (bug report / feature request / improvement idea) before writing any code.
- Test your changes thoroughly!
    - Use multiple devices for testing, if possible.
- Please be extra careful if you are working on the main database class.
    - Test the changes on a fresh installation of Exif Notes and with an existing database.
    - Changes affecting the database class will be reviewed more thoroughly.
- Keep your changes limited to the files you need to modify.
    - Don't stray and do changes to files that are not directly related for your bug fix or feature to work.
    - If you find something unrelated to improve in the code, please create a new issue for that.
- Create a pull request and include at least the following things.
    - Type of change
        - Bug fix
        - New feature
        - Improvement
    - Purpose of the change
    - Impact of the change
    - Link to the issue

### Setting up

There are a couple of additional steps you need to take to fully set up your development environment in addition to cloning the repository and configuring Android Studio. Place the following two files in your project root. They must not be placed into version control since they will contain sensitive information.
- keystore.properties
- googlemapsapi.properties

The properties placed in these files are imported in the module level Gradle build file `/app/build.gradle`.

**keystore.properties**

```
# Location of key store used to sign the app on macOS (can be omitted on Windows)
storeFileMac=/Users/john/Documents/Programming/Keys/android.jks

# Location of key store used to sign the app on Windows (can be omitted on macOS)
storeFileWin=C:\\Users\\John\\Documents\\Programming\\Keys\\android.jks

storePassword=aGoodPassword123
keyAlias=MyAndroidKey
keyPassword=anotherPassword321
```
The key store referenced in the property path is used to sign the app (both debug and release).

**googlemapsapi.properties**

```
googleMapsKey=<place your Google Maps API key here>
```
The API key is read in the module level Gradle build file `/app/build.gradle` and added as a string resource.

This property is not mandatory in order to build the project and to develop and use the app. However, it is needed if you need to test the geocoding or Google Maps related features. An API key is needed to use these Google Cloud services.

In order to acquire a Google Maps API key, you will need to set up a Google Cloud account and enable these APIs for the key:
- Geocoding API
- Maps SDK for Android

To acquire an API key, you can get started here: https://mapsplatform.google.com/