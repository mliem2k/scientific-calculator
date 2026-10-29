import '@testing-library/jest-dom'

// jsdom has no layout engine or ResizeObserver; the display only needs it to measure.
globalThis.ResizeObserver ??= class {
  observe() {}
  unobserve() {}
  disconnect() {}
}
