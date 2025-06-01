import 'dart:async';
import 'dart:html' as html;
import 'dart:convert';

import 'package:flutter/services.dart';
import 'package:flutter_web_plugins/flutter_web_plugins.dart';

import 'core/uae_pass_platform_interface.dart';

/// A web implementation of the UaePassPlatform of the UaePass plugin.
class UaePassWeb extends UaePassPlatform {
  /// Constructs a UaePassWeb
  UaePassWeb();

  static void registerWith(Registrar registrar) {
    UaePassPlatform.instance = UaePassWeb();
  }

  // Configuration variables
  String? _clientId;
  String? _clientSecret;
  String? _redirectUri;
  String? _scope;
  String? _state;
  bool _isProduction = false;
  String _language = "en";

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
    _clientId = clientId;
    _clientSecret = clientSecret;
    _isProduction = isProduction;
    _redirectUri = redirectUri;
    _scope = scope;
    _state = state;
    _language = language;
  }

  @override
  Future<String> signIn() async {
    if (_clientId == null) {
      throw PlatformException(
        code: 'SETUP_REQUIRED',
        message: 'Please call setUp before signIn',
      );
    }

    final completer = Completer<String>();

    try {
      // Build the authorization URL
      final authUrl = _buildAuthUrl();

      // Open popup window for authentication
      final popup = html.window.open(authUrl, 'UAE_PASS_AUTH',
          'width=600,height=700,scrollbars=yes,resizable=yes');

      // Listen for the popup to be closed or redirected
      late Timer timer;
      timer = Timer.periodic(const Duration(milliseconds: 500), (timer) {
        try {
          if (popup.closed!) {
            timer.cancel();
            if (!completer.isCompleted) {
              completer.completeError(PlatformException(
                code: 'USER_CANCELLED',
                message: 'Authentication was cancelled by user',
              ));
            }
            return;
          }

          // Check if popup URL contains our redirect URI
          final currentUrl = popup.location.toString();
          if (currentUrl.contains(_redirectUri!)) {
            timer.cancel();
            popup.close();

            // Extract authorization code from URL
            final uri = Uri.parse(currentUrl);
            final code = uri.queryParameters['code'];
            final error = uri.queryParameters['error'];

            if (error != null) {
              if (!completer.isCompleted) {
                completer.completeError(PlatformException(
                  code: 'AUTH_ERROR',
                  message: error,
                ));
              }
            } else if (code != null) {
              if (!completer.isCompleted) {
                completer.complete(code);
              }
            } else {
              if (!completer.isCompleted) {
                completer.completeError(PlatformException(
                  code: 'NO_CODE',
                  message: 'No authorization code received',
                ));
              }
            }
          }
        } catch (e) {
          // Cross-origin error means we can't access popup location
          // Continue polling
        }
      });

      // Set timeout
      Timer(const Duration(minutes: 5), () {
        if (!completer.isCompleted) {
          timer.cancel();
          popup.close();
          completer.completeError(PlatformException(
            code: 'TIMEOUT',
            message: 'Authentication timeout',
          ));
        }
      });

      return await completer.future;
    } catch (e) {
      throw PlatformException(
        code: 'SIGN_IN_ERROR',
        message: e.toString(),
      );
    }
  }

  @override
  Future<String> getAccessToken(String code) async {
    if (_clientId == null || _clientSecret == null) {
      throw PlatformException(
        code: 'SETUP_REQUIRED',
        message: 'Please call setUp before getAccessToken',
      );
    }

    try {
      final tokenUrl = _getTokenUrl();

      final response = await html.HttpRequest.request(
        tokenUrl,
        method: 'POST',
        requestHeaders: {
          'Content-Type': 'application/x-www-form-urlencoded',
          'Accept': 'application/json',
        },
        sendData: _buildTokenRequestBody(code),
      );

      if (response.status == 200) {
        final responseData = json.decode(response.responseText!);
        final accessToken = responseData['access_token'];

        if (accessToken != null) {
          return accessToken;
        } else {
          throw PlatformException(
            code: 'NO_ACCESS_TOKEN',
            message: 'No access token in response',
          );
        }
      } else {
        throw PlatformException(
          code: 'HTTP_ERROR',
          message: 'HTTP ${response.status}: ${response.responseText}',
        );
      }
    } catch (e) {
      throw PlatformException(
        code: 'TOKEN_ERROR',
        message: e.toString(),
      );
    }
  }

  @override
  Future<dynamic> getProfile(String accessToken) async {
    try {
      final profileUrl = _getProfileUrl();

      final response = await html.HttpRequest.request(
        profileUrl,
        method: 'GET',
        requestHeaders: {
          'Authorization': 'Bearer $accessToken',
          'Accept': 'application/json',
        },
      );

      if (response.status == 200) {
        return response.responseText!;
      } else {
        throw PlatformException(
          code: 'HTTP_ERROR',
          message: 'HTTP ${response.status}: ${response.responseText}',
        );
      }
    } catch (e) {
      throw PlatformException(
        code: 'PROFILE_ERROR',
        message: e.toString(),
      );
    }
  }

  @override
  Future<void> signOut() async {
    // Clear any stored session data
    html.window.localStorage.remove('uae_pass_session');

    // Clear cookies related to UAE Pass
    final cookies = html.document.cookie?.split(';') ?? [];
    for (final cookie in cookies) {
      final parts = cookie.split('=');
      if (parts.isNotEmpty) {
        final name = parts[0].trim();
        if (name.toLowerCase().contains('uaepass') ||
            name.toLowerCase().contains('session')) {
          html.document.cookie =
              '$name=; expires=Thu, 01 Jan 1970 00:00:00 UTC; path=/;';
        }
      }
    }
  }

  String _buildAuthUrl() {
    final baseUrl = _isProduction
        ? 'https://id.uaepass.ae/idshub/authorize'
        : 'https://stg-id.uaepass.ae/idshub/authorize';

    final params = {
      'response_type': 'code',
      'client_id': _clientId!,
      'redirect_uri': _redirectUri!,
      'scope': _scope!,
      'state': _state!,
      'acr_values': 'urn:safelayer:tws:policies:authentication:level:low',
      'ui_locales': _language == 'ar' ? 'ar' : 'en',
    };

    final queryString = params.entries
        .map((e) =>
            '${Uri.encodeComponent(e.key)}=${Uri.encodeComponent(e.value)}')
        .join('&');

    return '$baseUrl?$queryString';
  }

  String _getTokenUrl() {
    return _isProduction
        ? 'https://id.uaepass.ae/idshub/token'
        : 'https://stg-id.uaepass.ae/idshub/token';
  }

  String _getProfileUrl() {
    return _isProduction
        ? 'https://id.uaepass.ae/idshub/userinfo'
        : 'https://stg-id.uaepass.ae/idshub/userinfo';
  }

  String _buildTokenRequestBody(String code) {
    final params = {
      'grant_type': 'authorization_code',
      'client_id': _clientId!,
      'client_secret': _clientSecret!,
      'code': code,
      'redirect_uri': _redirectUri!,
    };

    return params.entries
        .map((e) =>
            '${Uri.encodeComponent(e.key)}=${Uri.encodeComponent(e.value)}')
        .join('&');
  }
}
