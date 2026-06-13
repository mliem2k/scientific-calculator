import 'dart:convert';
import 'dart:io';

// Baked in at build time via --dart-define=BUILD_NUMBER=<unix_ts>.
// Zero means a dev build — update checks are skipped.
const int _buildNumber = int.fromEnvironment('BUILD_NUMBER', defaultValue: 0);

const _repo = 'mliem2k/scientific-calculator';

// Use /releases?per_page=1 instead of /releases/latest.
// /releases/latest returns 404 for repos that only have prerelease releases,
// which would make every check silently report "up to date".
const _apiUrl = 'https://api.github.com/repos/$_repo/releases?per_page=1';

class UpdateInfo {
  final String tagName;
  final String releaseUrl;
  const UpdateInfo({required this.tagName, required this.releaseUrl});
}

Future<UpdateInfo?> checkForUpdate() async {
  if (_buildNumber == 0) return null;

  final client = HttpClient();
  try {
    final req = await client.getUrl(Uri.parse(_apiUrl));
    req.headers
      ..set('User-Agent', 'scientific-calculator/$_buildNumber')
      ..set('Accept', 'application/vnd.github+json');
    final res = await req.close();
    if (res.statusCode != 200) return null;

    final body = await res.transform(utf8.decoder).join();
    final list = jsonDecode(body) as List<dynamic>;
    if (list.isEmpty) return null;

    final json = list[0] as Map<String, dynamic>;
    final tagName = json['tag_name'] as String?;
    final htmlUrl = json['html_url'] as String?;
    if (tagName == null || htmlUrl == null) return null;

    // Primary: parse build number from tag "nightly-YYYYMMDD-BUILDNUM".
    // This is exact and immune to timestamp drift between build and release.
    final latestBuild = _parseBuildFromTag(tagName);
    if (latestBuild != null) {
      return latestBuild > _buildNumber
          ? UpdateInfo(tagName: tagName, releaseUrl: htmlUrl)
          : null;
    }

    // Fallback for old-format tags without an embedded build number.
    // A 300-second grace period absorbs the ~40s gap between _buildNumber
    // (captured at script start) and published_at (set when gh release create
    // finishes), preventing false "update available" for the current build.
    final publishedAt = json['published_at'] as String?;
    if (publishedAt == null) return null;
    final latestTs = DateTime.parse(publishedAt).millisecondsSinceEpoch ~/ 1000;
    return latestTs > _buildNumber + 300
        ? UpdateInfo(tagName: tagName, releaseUrl: htmlUrl)
        : null;
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
