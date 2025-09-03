import 'package:flutter/foundation.dart';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_uae_pass/flutter_uae_pass.dart';
import 'package:flutter_uae_pass/model/profile_data.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatefulWidget {
  const MyApp({super.key});

  @override
  State<MyApp> createState() => _MyAppState();
}

class _MyAppState extends State<MyApp> {
  String? authCode;
  String? accessToken;
  ProfileData? profileData;

  final _uaePass = UaePass();

  @override
  void initState() {
    super.initState();
    _uaePass.setUpSandbox();
  }

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      home: Scaffold(
        appBar: AppBar(
          title: const Text('UAE Pass'),
          centerTitle: true,
        ),
        body: Center(
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.center,
            children: [
              MaterialButton(
                onPressed: () => login(),
                child: const Text('Sign in with UAE Pass'),
              ),
              const SizedBox(height: 20),
              if (profileData != null)
                MaterialButton(
                  onPressed: () => signDocument(),
                  color: Colors.green,
                  textColor: Colors.white,
                  child: const Text('Sign Document'),
                ),
              const SizedBox(height: 100),
              if (authCode != null)
                ListTile(
                  title: const Text('Auth Code'),
                  subtitle: Text('${authCode?.substring(0, 6)}............'),
                ),
              if (accessToken != null)
                ListTile(
                  title: const Text('Access Token'),
                  subtitle: Text('${accessToken?.substring(0, 6)}............'),
                ),
              if (profileData != null)
                Column(
                  children: [
                    ListTile(
                      title: const Text('Emirates Id'),
                      subtitle: Text(profileData?.idn ?? ""),
                    ),
                    ListTile(
                      title: const Text('Full Name EN'),
                      subtitle: Text(profileData?.fullnameEN ?? ""),
                    ),
                    ListTile(
                      title: const Text('Full Name AR'),
                      subtitle: Text(profileData?.fullnameAR ?? ""),
                    ),
                    ListTile(
                      title: const Text('Mobile'),
                      subtitle: Text(profileData?.mobile ?? ""),
                    ),
                    ListTile(
                      title: const Text('Nationality EN'),
                      subtitle: Text(profileData?.nationalityEN ?? ""),
                    ),
                  ],
                ),
            ],
          ),
        ),
      ),
    );
  }

  Future<void> login() async {
    authCode = null;
    accessToken = null;
    profileData = null;
    setState(() {});
    try {
      if (kDebugMode) {
        print("Starting login process...");
      }
      authCode = await _uaePass.signIn();
      if (kDebugMode) {
        print("Auth code received: $authCode");
      }
      accessToken = await _uaePass.getAccessToken(authCode ?? "");
      if (kDebugMode) {
        print("Access token received: $accessToken");
      }
      profileData = await _uaePass.getProfile(accessToken ?? "");
      if (kDebugMode) {
        print("Profile data received for: ${profileData?.fullnameEN}");
      }
      setState(() {});
    } on PlatformException catch (e) {
      if (kDebugMode) {
        print("PlatformException during login: ${e.message}");
      }
    } catch (e) {
      if (kDebugMode) {
        print("Unknown error during login: $e");
      }
    }
    setState(() {});
  }

  Future<void> signDocument() async {
    try {
      if (kDebugMode) {
        print("Starting document signing process...");
      }
      const textToSign = "This document is digitally signed by the user via UAE Pass.";
      final result = await _uaePass.signDocument(
        textToSign: textToSign,
        finishCallbackUrl: 'yourapp://signsuccess',
      );
      if (result != null) {
        if (kDebugMode) {
          print("Document signing result: $result");
        }
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(result),
            backgroundColor: Colors.green,
          ),
        );
      } else {
        if (kDebugMode) {
          print("Document signing failed or was canceled.");
        }
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text("Signing failed or was canceled."),
            backgroundColor: Colors.red,
          ),
        );
      }
    } on PlatformException catch (e) {
      if (kDebugMode) {
        print("PlatformException during document signing: ${e.message}");
      }
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text("Error during signing: ${e.message}"),
          backgroundColor: Colors.red,
        ),
      );
    } catch (e) {
      if (kDebugMode) {
        print("Unknown error during document signing: $e");
      }
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text("Error during signing: ${e.toString()}"),
          backgroundColor: Colors.red,
        ),
      );
    }
  }
}