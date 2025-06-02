// Stub implementation for non-web platforms
import 'core/uae_pass_platform_interface.dart';

class UaePassWeb extends UaePassPlatform {
  static void registerWith(dynamic registrar) {
    // This is a stub - web implementation is handled separately
    throw UnsupportedError('Web platform not supported on this platform');
  }
}
