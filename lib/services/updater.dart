import 'dart:convert';
import 'dart:io';

// Baked in at build time via --dart-define=BUILD_NUMBER=<unix_ts>.
// Zero means a dev build — update checks are skipped.
const int kBuildNumber = int.fromEnvironment('BUILD_NUMBER', defaultValue: 0);

const _repo = 'mliem2k/scientific-calculator';

// Fetch up to 10 recent releases and pick the one with the highest embedded
// build number. Using per_page=1 is fragile because GitHub CDN caching or
// eventual consistency can return a stale result as the first item; scanning
// the batch is order-independent and handles same-day re-releases correctly.
const _apiUrl = 'https://api.github.com/repos/$_repo/releases?per_page=10';

class UpdateInfo {
  final String tagName;
  final String releaseUrl;
  final String? apkUrl;
  const UpdateInfo({required this.tagName, required this.releaseUrl, this.apkUrl});
}

Future<UpdateInfo?> checkForUpdate() async {
  if (kBuildNumber == 0) return null;

  final client = HttpClient();
  try {
    final req = await client.getUrl(Uri.parse(_apiUrl));
    req.headers
      ..set('User-Agent', 'scientific-calculator/$kBuildNumber')
      ..set('Accept', 'application/vnd.github+json')
      ..set('Cache-Control', 'no-cache');
    final res = await req.close();
    if (res.statusCode != 200) return null;

    final body = await res.transform(utf8.decoder).join();
    final list = jsonDecode(body) as List<dynamic>;
    if (list.isEmpty) return null;

    // Scan all returned releases; pick the one with the highest build number.
    // This is robust to out-of-order results and in-flight same-day cleanups.
    int bestBuild = kBuildNumber;
    UpdateInfo? best;

    for (final item in list) {
      final json = item as Map<String, dynamic>;
      final tagName = json['tag_name'] as String?;
      final htmlUrl = json['html_url'] as String?;
      if (tagName == null || htmlUrl == null) continue;

      final build = _parseBuildFromTag(tagName);
      if (build != null && build > bestBuild) {
        bestBuild = build;
        final assets = json['assets'] as List<dynamic>?;
        String? apkUrl;
        if (assets != null) {
          for (final asset in assets) {
            final assetMap = asset as Map<String, dynamic>;
            final name = assetMap['name'] as String? ?? '';
            if (name.endsWith('.apk')) {
              apkUrl = assetMap['browser_download_url'] as String?;
              break;
            }
          }
        }
        best = UpdateInfo(tagName: tagName, releaseUrl: htmlUrl, apkUrl: apkUrl);
      }
    }

    return best;
  } catch (_) {
    return null;
  } finally {
    client.close();
  }
}

// Parses the build number from a tag of the form "nightly-YYYYMMDD-BUILDNUM".
// Returns null for any other tag format.
int? _parseBuildFromTag(String tag) {
  final i = tag.lastIndexOf('-');
  if (i < 0) return null;
  return int.tryParse(tag.substring(i + 1));
}
