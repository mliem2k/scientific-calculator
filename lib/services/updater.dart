import 'dart:convert';
import 'dart:io';

// Baked in at build time via --dart-define=BUILD_NUMBER=<unix_ts>.
// Zero means a dev build — update checks are skipped.
const int _buildNumber = int.fromEnvironment('BUILD_NUMBER', defaultValue: 0);

const _repo = 'mliem2k/scientific-calculator';
const _apiUrl =
    'https://api.github.com/repos/$_repo/releases/latest';

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
    final json = jsonDecode(body) as Map<String, dynamic>;

    final publishedAt = json['published_at'] as String?;
    final tagName = json['tag_name'] as String?;
    final htmlUrl = json['html_url'] as String?;
    if (publishedAt == null || tagName == null || htmlUrl == null) return null;

    final latestTs = DateTime.parse(publishedAt).millisecondsSinceEpoch ~/ 1000;
    if (latestTs > _buildNumber) {
      return UpdateInfo(tagName: tagName, releaseUrl: htmlUrl);
    }
    return null;
  } catch (_) {
    return null;
  } finally {
    client.close();
  }
}
