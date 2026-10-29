interface Release {
  prerelease: boolean;
  assets: { name: string; browser_download_url: string }[];
}

document.querySelectorAll<HTMLButtonElement>('[data-download-apk]').forEach((btn) => {
  const idle = btn.textContent ?? 'Download APK';
  btn.addEventListener('click', async () => {
    btn.disabled = true;
    btn.textContent = 'Fetching...';
    try {
      const res = await fetch('https://api.github.com/repos/mliem2k/scientific-calculator/releases');
      const releases: Release[] = await res.json();
      const apk = releases.find((r) => r.prerelease)?.assets?.find((a) => a.name.endsWith('.apk'));
      if (!apk) throw new Error('APK not found');
      window.location.href = apk.browser_download_url;
      btn.textContent = idle;
    } catch {
      btn.textContent = 'Download failed';
    } finally {
      btn.disabled = false;
    }
  });
});
