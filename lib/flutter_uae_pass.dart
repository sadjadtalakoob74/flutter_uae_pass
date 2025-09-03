import 'dart:convert';
import 'dart:typed_data';

import 'package:flutter/services.dart';
import 'core/uae_pass_platform_interface.dart';
import 'model/profile_data.dart';
import 'package:pdf/widgets.dart' as pw;
import 'package:pdf/pdf.dart';
export 'core/export.dart';
export 'model/export.dart';

class UaePass {
  Future<void> setUpSandbox({String language = "en"}) async {
    UaePassPlatform.instance.setUp(
      clientId: "sandbox_stage",
      clientSecret: "sandbox_stage",
      isProduction: false,
      urlScheme: "uaepassdemoappDS",
      state: "123123213",
      redirectUri: "https://oauthtest.com/authorization/return",
      scope: "urn:uae:digitalid:profile",
      language: language,
    );
  }

  Future<void> setUpEnvironment({
    required String clientId,
    required String clientSecret,
    required String urlScheme,
    String state = "HnlHOJTkTb66Y5H",
    bool isProduction = false,
    String redirectUri = "https://oauthtest.com/authorization/return",
    String scope = "urn:uae:digitalid:profile",
    String language = "en",
  }) async {
    UaePassPlatform.instance.setUp(
      clientId: clientId,
      clientSecret: clientSecret,
      isProduction: isProduction,
      urlScheme: urlScheme,
      state: state,
      redirectUri: redirectUri,
      scope: scope,
      language: language,
    );
  }

  Future<String> signIn() async {
    try {
      return await UaePassPlatform.instance.signIn();
    } on PlatformException catch (e) {
      throw (e.message ?? "Unknown error");
    } catch (e) {
      throw (e.toString());
    }
  }

  Future<String> getAccessToken(String token) async {
    try {
      return await UaePassPlatform.instance.getAccessToken(token);
    } on PlatformException catch (e) {
      throw (e.message ?? "Unknown error");
    } catch (e) {
      throw (e.toString());
    }
  }

  Future<ProfileData?> getProfile(String accessToken) async {
    try {
      final result = await UaePassPlatform.instance.getProfile(accessToken);
      return ProfileData.fromJson(json.decode(result));
    } on PlatformException catch (e) {
      throw (e.message ?? "Unknown error");
    } catch (e) {
      throw (e.toString());
    }
  }

  Future<void> signOut() async {
    try {
      return await UaePassPlatform.instance.signOut();
    } on PlatformException catch (e) {
      throw (e.message ?? "Unknown error");
    } catch (e) {
      throw (e.toString());
    }
  }

  // New: Public method to start the document signing process
  Future<String?> signDocument({
    required String textToSign,
    required String finishCallbackUrl,
  }) async {
    try {
      // 1. Create a PDF document from the provided text
      final pdf = pw.Document();
      pdf.addPage(
        pw.Page(
          pageFormat: PdfPageFormat.a4,
          build: (pw.Context context) {
            return pw.Center(
              child: pw.Text(textToSign),
            );
          },
        ),
      );

      // 2. Convert the PDF document to bytes
      final documentBytes = await pdf.save();

      // 3. Pass these bytes to the native platform for signing
      return await UaePassPlatform.instance.signDocument(
        documentBytes: documentBytes,
        finishCallbackUrl: finishCallbackUrl,
      );
    } on PlatformException catch (e) {
      throw (e.message ?? "Unknown error");
    } catch (e) {
      throw (e.toString());
    }
  }
}
