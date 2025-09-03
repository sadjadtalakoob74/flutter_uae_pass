import 'dart:typed_data';

import 'package:flutter/foundation.dart';
import 'package:flutter/services.dart';

import 'uae_pass_platform_interface.dart';

/// An implementation of [UaePassPlatform] that uses method channels.
class MethodChannelUaePass extends UaePassPlatform {
  /// The method channel used to interact with the native platform.
  @visibleForTesting
  final methodChannel = const MethodChannel('uae_pass');

  @override
  Future<void> setUp({
    required String clientId,
    required String clientSecret,
    required bool isProduction,
    required String urlScheme,
    required String state,
    required String redirectUri,
    required String scope,
    required String language,
  }) async {
    await methodChannel
        .invokeMethod<void>('set_up_environment', <String, String>{
      'client_id': clientId,
      'client_secret': clientSecret,
      'environment': isProduction ? 'production' : 'qa',
      "redirect_uri_login": urlScheme,
      "scheme": urlScheme,
      'state': state,
      "redirect_url": redirectUri,
      "scope": scope,
      "language": language,
    });
  }

  @override
  Future<String> signIn() async {
    final result = await methodChannel.invokeMethod<String>('sign_in');
    return result!;
  }

  @override
  Future<String> getAccessToken(String code) async {
    final result = await methodChannel.invokeMethod<String>('access_token', {
      'code': code,
    });
    return result!;
  }

  @override
  Future<dynamic> getProfile(String accessToken) async {
    final result = await methodChannel.invokeMethod<String>(
      'profile',
      {
        'token': accessToken,
      },
    );
    return result!;
  }

  @override
  Future<void> signOut() async {
    await methodChannel.invokeMethod<void>('sign_out');
  }

  // New: Implementation for document signing
  @override
  Future<String?> signDocument({
    required Uint8List documentBytes,
    required String finishCallbackUrl,
  }) async {
    try {
      final String? result = await methodChannel.invokeMethod('sign_document', {
        'documentBytes': documentBytes,
        'finishCallbackUrl': finishCallbackUrl,
      });
      return result;
    } on PlatformException catch (e) {
      debugPrint("Failed to sign document: '${e.message}'.");
      return null;
    }
  }
}
