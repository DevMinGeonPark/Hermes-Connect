// Apply the saved appearance before styles load, including in standalone views.
(() => {
  const key = "hermes-connect-appearance";
  const valid = (value) => ["system", "light", "dark"].includes(value);
  const media = window.matchMedia("(prefers-color-scheme: dark)");
  const query = new URLSearchParams(location.search).get("theme");
  let saved;
  try {
    saved = localStorage.getItem(key);
  } catch {
    /* Storage is optional. */
  }
  let preference = valid(query) ? query : valid(saved) ? saved : "system";
  function apply() {
    const resolved =
      preference === "system" ? (media.matches ? "dark" : "light") : preference;
    document.documentElement.dataset.appearance = resolved;
    document.documentElement.style.colorScheme = resolved;
    window.dispatchEvent(
      new CustomEvent("appearance-change", {
        detail: { preference, resolved },
      }),
    );
  }
  function setPreference(value, persist = true) {
    if (!valid(value)) return;
    if (persist) {
      try {
        localStorage.setItem(key, value);
      } catch {
        /* Keep the in-memory choice. */
      }
    }
    if (preference === value) return;
    preference = value;
    apply();
  }
  window.previewAppearance = Object.freeze({
    getPreference: () => preference,
    setPreference,
  });
  media.addEventListener("change", () => {
    if (preference === "system") apply();
  });
  window.addEventListener("storage", (event) => {
    if (event.key === key || event.key === null)
      setPreference(valid(event.newValue) ? event.newValue : "system", false);
  });
  apply();
})();
