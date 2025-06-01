# uae_pass

Un-official UAE Pass Flutter plugin for **iOS, Android, and Web**.

![Demo](https://github.com/MohamedAbd0/flutter_uae_pass/blob/main/screenshots/demo.gif?raw=true)

## Platform Screenshots

| Android                                                                                                | iOS                                                                                            | Web                                                                                            |
| ------------------------------------------------------------------------------------------------------ | ---------------------------------------------------------------------------------------------- | ---------------------------------------------------------------------------------------------- |
| ![Android](https://github.com/MohamedAbd0/flutter_uae_pass/blob/main/screenshots/andorid.png?raw=true) | ![iOS](https://github.com/MohamedAbd0/flutter_uae_pass/blob/main/screenshots/ios.png?raw=true) | ![Web](https://github.com/MohamedAbd0/flutter_uae_pass/blob/main/screenshots/web.png?raw=true) |

## Platform Support

| Platform | Supported | Implementation                   |
| -------- | --------- | -------------------------------- |
| iOS      | ✅        | WKWebView with full screen modal |
| Android  | ✅        | Custom WebView Activity          |
| Web      | ✅        | OAuth2 popup authentication      |

## Getting Started

- Add the plugin to your pubspec.yaml file

```yaml
flutter_uae_pass: ^1.0.5
```

- Run flutter pub get

```bash
flutter pub get
```

- Import the package

```dart
import 'package:flutter_uae_pass/flutter_uae_pass.dart';
final _uaePassPlugin = UaePass();


```

- Initialize the plugin - Sandbox

```dart
  await _uaePassPlugin.setUpSandbox();
```

- Initialize the plugin - Production

```dart
    await _uaePassPlugin.setUpEnvironment(
      clientId: "<clientId>",
      clientSecret: "<clientSecret>",
      urlScheme: "myappscheme",
      redirectUri: "<redirectUri>",
      isProduction: true,
      language : "en",
    );
```

# Scopes are as follows

- urn:uae:digitalid:profile:general
- urn:uae:digitalid:profile

# Main features

- signIn()
- getAccessToken(String authCode)
- getProfile(String token)
- signOut()

# Call the authenticate method

```dart
  String? authCode = await _uaePassPlugin.signIn();
```

# To get access token

```dart
  String? accessToken = await _uaePassPlugin.getAccessToken(authCode);
```

# To get public profile data

you can fetch this information from profile

- idn
- email
- firstnameAR
- firstnameEN
- fullnameEN
- gender
- lastnameAR
- lastnameEN
- mobile
- nationalityAR
- nationalityEN
- sub
- userType
- uuid

```dart
  ProfileData? profileData = await _uaePassPlugin.getProfile(accessToken);
```

## iOS Setup

- Add the following to your Info.plist file

```xml

		<key>LSApplicationQueriesSchemes</key>
		<array>
			<string>uaepass</string>
			<string>uaepassqa</string>
			<string>uaepassdev</string>
			<string>uaepassstg</string>
		</array>
		<key>CFBundleURLTypes</key>
		<array>
			<dict>
				<key>CFBundleTypeRole</key>
				<string>Editor</string>
				<key>CFBundleURLName</key>
				<string>myappscheme</string>
				<key>CFBundleURLSchemes</key>
				<array>
					<string>myappscheme</string>
				</array>
			</dict>
		</array>
```

## Android Setup

- Update android:launchMode="singleTask" the AndroidManifest.xml file

```xml

 <activity
            android:name=".MainActivity"
            android:exported="true"
            android:launchMode="singleTask"
            .....

            </activity>

```

- Set up the queries in your AndroidManifest.xml file (To make our app open UAE Pass App)

```xml
    <queries>
        <package android:name="ae.uaepass.mainapp" />
        <package android:name="ae.uaepass.mainapp.qa" />
        <package android:name="ae.uaepass.mainapp.stg" />
    </queries>

```

- Set up the intent filter in your AndroidManifest.xml file (To Reopen our application after uaepass done)

```xml
            <intent-filter >
                <action android:name="android.intent.action.VIEW" />

                <category android:name="android.intent.category.DEFAULT" />
                <category android:name="android.intent.category.BROWSABLE" />


                <data
                    android:host="success"
                    android:scheme="myappscheme" />

                <data
                    android:host="failure"
                    android:scheme="myappscheme" />

            </intent-filter>

```

## Web Setup

For web applications, no additional configuration is required! The plugin automatically uses a popup-based OAuth2 flow that works in all modern browsers.

### Web-Specific Features:

- **Popup Authentication**: Opens UAE Pass login in a secure popup window
- **Automatic Redirect Handling**: Automatically detects and processes the authorization callback
- **CORS Compliance**: Uses proper HTTP requests for token exchange and profile retrieval
- **Session Management**: Handles cookies and localStorage for proper logout functionality

### Browser Requirements:

- Modern browsers with popup support (Chrome, Firefox, Safari, Edge)
- JavaScript enabled
- Cookies enabled for proper session management

### Production Considerations:

- Ensure your redirect URI is properly configured in the UAE Pass dashboard
- For production, make sure your domain is whitelisted in UAE Pass configuration
- Test popup blockers - inform users to allow popups for your domain if needed

## References

- [Documentation](https://docs.uaepass.ae/)
- [Read Common issues](https://docs.uaepass.ae/faq/common-integration-issues)
- [Staging Apps](https://docs.uaepass.ae/resources/staging-apps)

Thanks for [Faisal](https://github.com/Faisalkc4u) for this repo [uae_pass_flutter](https://github.com/Faisalkc4u/uae_pass) i improve the code and create this package
