import 'dart:io';

import 'package:flutter/material.dart';
import 'package:open_file/open_file.dart';
import 'package:path_provider/path_provider.dart';
import 'package:url_launcher/url_launcher.dart';
import '../services/updater.dart';
import '../theme/calc_theme.dart';

enum _DownloadState { idle, downloading, done, error }

class UpdateDialog extends StatefulWidget {
  final UpdateInfo info;

  const UpdateDialog({super.key, required this.info});

  @override
  State<UpdateDialog> createState() => _UpdateDialogState();
}

class _UpdateDialogState extends State<UpdateDialog> {
  _DownloadState _state = _DownloadState.idle;
  double _progress = 0;
  String? _apkPath;

  Future<void> _download() async {
    final apkUrl = widget.info.apkUrl;
    if (apkUrl == null) {
      await launchUrl(
        Uri.parse(widget.info.releaseUrl),
        mode: LaunchMode.externalApplication,
      );
      return;
    }

    setState(() => _state = _DownloadState.downloading);

    try {
      final dir = await getTemporaryDirectory();
      final path = '${dir.path}/update-${widget.info.tagName}.apk';

      final client = HttpClient();
      try {
        final req = await client.getUrl(Uri.parse(apkUrl));
        final res = await req.close();

        final total = res.contentLength;
        var received = 0;
        final file = File(path).openWrite();
        await for (final chunk in res) {
          file.add(chunk);
          received += chunk.length;
          if (total > 0 && mounted) {
            setState(() => _progress = received / total);
          }
        }
        await file.flush();
        await file.close();
      } finally {
        client.close();
      }

      if (!mounted) return;
      setState(() {
        _state = _DownloadState.done;
        _apkPath = path;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() => _state = _DownloadState.error);
    }
  }

  Future<void> _install() async {
    if (_apkPath == null) return;
    await OpenFile.open(_apkPath!);
  }

  @override
  Widget build(BuildContext context) {
    final ct = Theme.of(context).extension<CalcTheme>()!;

    return Dialog(
      backgroundColor: ct.displayBg,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Text(
              'Update Available',
              style: TextStyle(
                color: ct.expressionText,
                fontSize: 18,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              widget.info.tagName,
              style: TextStyle(color: ct.secondaryLabel, fontSize: 14),
            ),
            const SizedBox(height: 24),
            if (_state == _DownloadState.downloading) ...[
              LinearProgressIndicator(
                value: _progress > 0 ? _progress : null,
                backgroundColor: ct.buttonBorder.withAlpha(60),
                valueColor: AlwaysStoppedAnimation<Color>(ct.shiftActiveColor),
              ),
              const SizedBox(height: 8),
              Text(
                _progress > 0
                    ? '${(_progress * 100).toStringAsFixed(0)}%'
                    : 'Downloading...',
                style: TextStyle(color: ct.secondaryLabel, fontSize: 13),
                textAlign: TextAlign.center,
              ),
            ] else if (_state == _DownloadState.error) ...[
              Text(
                'Download failed. Tap below to open in browser.',
                style: TextStyle(color: ct.opText, fontSize: 13),
              ),
            ],
            const SizedBox(height: 16),
            Row(
              mainAxisAlignment: MainAxisAlignment.end,
              children: [
                TextButton(
                  onPressed: () => Navigator.of(context).pop(),
                  child: Text(
                    'Later',
                    style: TextStyle(color: ct.secondaryLabel),
                  ),
                ),
                const SizedBox(width: 8),
                if (_state == _DownloadState.done)
                  ElevatedButton(
                    onPressed: _install,
                    style: ElevatedButton.styleFrom(
                      backgroundColor: ct.shiftActiveColor,
                      foregroundColor: Colors.white,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(8),
                      ),
                    ),
                    child: const Text('Install'),
                  )
                else if (_state != _DownloadState.downloading)
                  ElevatedButton(
                    onPressed: _state == _DownloadState.error
                        ? () => launchUrl(
                              Uri.parse(widget.info.releaseUrl),
                              mode: LaunchMode.externalApplication,
                            )
                        : _download,
                    style: ElevatedButton.styleFrom(
                      backgroundColor: ct.shiftActiveColor,
                      foregroundColor: Colors.white,
                      shape: RoundedRectangleBorder(
                        borderRadius: BorderRadius.circular(8),
                      ),
                    ),
                    child: Text(
                      _state == _DownloadState.error ? 'Open in Browser' : 'Download',
                    ),
                  ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class UpToDateDialog extends StatelessWidget {
  const UpToDateDialog({super.key});

  @override
  Widget build(BuildContext context) {
    final ct = Theme.of(context).extension<CalcTheme>()!;

    return Dialog(
      backgroundColor: ct.displayBg,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: Padding(
        padding: const EdgeInsets.all(24),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            Icon(Icons.check_circle_outline, size: 52, color: ct.shiftActiveColor),
            const SizedBox(height: 16),
            Text(
              'Up to Date',
              style: TextStyle(
                color: ct.expressionText,
                fontSize: 18,
                fontWeight: FontWeight.bold,
              ),
            ),
            const SizedBox(height: 8),
            Text(
              'You\'re running the latest version.',
              style: TextStyle(color: ct.secondaryLabel, fontSize: 14),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 24),
            SizedBox(
              width: double.infinity,
              child: ElevatedButton(
                onPressed: () => Navigator.of(context).pop(),
                style: ElevatedButton.styleFrom(
                  backgroundColor: ct.shiftActiveColor,
                  foregroundColor: Colors.white,
                  shape: RoundedRectangleBorder(
                    borderRadius: BorderRadius.circular(8),
                  ),
                ),
                child: const Text('Got it'),
              ),
            ),
          ],
        ),
      ),
    );
  }
}
