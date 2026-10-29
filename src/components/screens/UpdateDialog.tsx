import { useEffect, useRef } from 'react'
import { formatTag, UpdateInfo } from '@/lib/updater'

interface Props {
  /** Null means the app is already up to date. */
  info: UpdateInfo | null
  onClose: () => void
}

const PRIMARY = 'min-h-12 rounded-lg bg-eq px-5 font-semibold text-on-eq'
const SECONDARY = 'min-h-12 rounded-lg px-4 text-dim'

export function UpdateDialog({ info, onClose }: Props) {
  const ref = useRef<HTMLDialogElement>(null)

  useEffect(() => {
    ref.current?.showModal()
  }, [])

  // The system browser downloads the APK and hands it to the package installer.
  const target = info?.apkUrl ?? info?.releaseUrl

  return (
    <dialog
      ref={ref}
      onClose={onClose}
      aria-labelledby="update-title"
      className="m-auto w-[min(22rem,calc(100vw-2rem))] rounded-2xl bg-display p-6 text-expr backdrop:bg-black/60"
    >
      <h2 id="update-title" className="text-lg font-semibold">{info ? 'Update available' : 'Up to date'}</h2>
      <p className="mt-2 text-result">
        {info ? `${formatTag(info.tagName)} is ready to download.` : "You're running the latest version."}
      </p>
      <form method="dialog" className="mt-6 flex justify-end gap-2">
        {info ? (
          <>
            <button className={SECONDARY}>Later</button>
            <a className={`${PRIMARY} flex items-center`} href={target} target="_blank" rel="noopener noreferrer">
              {info.apkUrl ? 'Download' : 'Open in browser'}
            </a>
          </>
        ) : (
          <button className={PRIMARY}>Got it</button>
        )}
      </form>
    </dialog>
  )
}
