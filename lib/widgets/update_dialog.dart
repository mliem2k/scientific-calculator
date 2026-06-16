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

  // nightly-20260616-1781592056 → "Nightly · Jun 16, 2026"
  String _formatTag(String tag) {
    final parts = tag.split('-');
    if (parts.length >= 2) {
      final dateStr = parts[1];
      if (dateStr.length == 8) {
        final year = dateStr.substring(0, 4);
        final month = int.tryParse(dateStr.substring(4, 6));
        final day = int.tryParse(dateStr.substring(6, 8));
        const months = [
          'Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun',
          'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec',
        ];
        if (month != null && month >= 1 && month <= 12 && day != null) {
          return 'Nightly · ${months[month - 1]} $day, $year';
        }
      }
    }
    return tag;
  }

  Future<void> _download() async {
    final apkUrl = widget.info.apkUrl;
    if (apkUrl == null) {
      await _openInBrowser();
      if (mounted) Navigator.of(context).pop();
      return;
    }

    setState(() {
      _state = _DownloadState.downloading;
      _progress = 0;
    });

    try {
      final dir = await getTemporaryDirectory();
      final path = '${dir.path}/update-${widget.info.tagName}.apk';

      final client = HttpClient();
      try {
        final req = await client.getUrl(Uri.parse(apkUrl));
        final res = await req.close();

        final total = res.contentLength;
        var received = 0;
        final sink = File(path).openWrite();
        await for (final chunk in res) {
          sink.add(chunk);
          received += chunk.length;
          if (total > 0 && mounted) {
            setState(() => _progress = received / total);
          }
        }
        await sink.flush();
        await sink.close();
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
    final path = _apkPath;
    if (path == null) return;
    final result = await OpenFile.open(path);
    if (!mounted) return;
    // Fall back to browser if system installer can't open the APK
    // (e.g. "Install unknown apps" permission not yet granted, then granted
    // and retried — or the open_file call fails for any other reason).
    if (result.type != ResultType.done) {
      await _openInBrowser();
    }
  }

  Future<void> _openInBrowser() => launchUrl(
        Uri.parse(widget.info.releaseUrl),
        mode: LaunchMode.externalApplication,
      );

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
            Row(
              children: [
                Icon(Icons.system_update_outlined,
                    color: ct.shiftActiveColor, size: 28),
                const SizedBox(width: 12),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Update Available',
                        style: TextStyle(
                          color: ct.expressionText,
                          fontSize: 18,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      Text(
                        _formatTag(widget.info.tagName),
                        style:
                            TextStyle(color: ct.secondaryLabel, fontSize: 13),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 20),
            if (_state == _DownloadState.downloading) ...[
              ClipRRect(
                borderRadius: BorderRadius.circular(4),
                child: LinearProgressIndicator(
                  value: _progress > 0 ? _progress : null,
                  minHeight: 6,
                  backgroundColor: ct.buttonBorder.withAlpha(60),
                  valueColor:
                      AlwaysStoppedAnimation<Color>(ct.shiftActiveColor),
                ),
              ),
              const SizedBox(height: 8),
              Text(
                _progress > 0
                    ? 'Downloading ${(_progress * 100).toStringAsFixed(0)}%'
                    : 'Starting download...',
                style: TextStyle(color: ct.secondaryLabel, fontSize: 13),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 8),
            ] else if (_state == _DownloadState.done) ...[
              _StatusBanner(
                icon: Icons.check_circle_outline,
                color: ct.shiftActiveColor,
                text: 'Ready to install',
              ),
              const SizedBox(height: 12),
            ] else if (_state == _DownloadState.error) ...[
              _StatusBanner(
                icon: Icons.error_outline,
                color: ct.delText,
                text: 'Download failed. Open in browser to install manually.',
              ),
              const SizedBox(height: 12),
            ],
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
                  _ActionButton(
                    label: 'Install',
                    icon: Icons.install_mobile_outlined,
                    color: ct.shiftActiveColor,
                    onPressed: _install,
                  )
                else if (_state == _DownloadState.error)
                  _ActionButton(
                    label: 'Open in Browser',
                    icon: Icons.open_in_new,
                    color: ct.shiftActiveColor,
                    onPressed: _openInBrowser,
                  )
                else if (_state != _DownloadState.downloading)
                  _ActionButton(
                    label: 'Download',
                    icon: Icons.download_outlined,
                    color: ct.shiftActiveColor,
                    onPressed: _download,
                  ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _StatusBanner extends StatelessWidget {
  const _StatusBanner({
    required this.icon,
    required this.color,
    required this.text,
  });

  final IconData icon;
  final Color color;
  final String text;

  @override
  Widget build(BuildContext context) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 10),
      decoration: BoxDecoration(
        color: color.withAlpha(25),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Row(
        children: [
          Icon(icon, color: color, size: 16),
          const SizedBox(width: 8),
          Expanded(
            child: Text(
              text,
              style: TextStyle(color: color, fontSize: 13),
            ),
          ),
        ],
      ),
    );
  }
}

class _ActionButton extends StatelessWidget {
  const _ActionButton({
    required this.label,
    required this.icon,
    required this.color,
    required this.onPressed,
  });

  final String label;
  final IconData icon;
  final Color color;
  final VoidCallback onPressed;

  @override
  Widget build(BuildContext context) {
    return ElevatedButton.icon(
      onPressed: onPressed,
      icon: Icon(icon, size: 16),
      label: Text(label),
      style: ElevatedButton.styleFrom(
        backgroundColor: color,
        foregroundColor: Colors.white,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
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
            Icon(Icons.check_circle_outline,
                size: 52, color: ct.shiftActiveColor),
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
              "You're running the latest version.",
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
                      borderRadius: BorderRadius.circular(8)),
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
