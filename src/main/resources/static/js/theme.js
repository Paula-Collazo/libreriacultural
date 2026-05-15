/**
 * theme.js — The Cultured Department
 * Gestiona el modo claro/oscuro con persistencia en localStorage.
 * Se aplica antes del primer render para evitar parpadeo (FOUC).
 */
(function () {
    const KEY = 'tcd_mode';

    /** Aplica el modo al <html> y sincroniza todos los botones toggle. */
    function applyMode(mode) {
        document.documentElement.setAttribute('data-mode', mode);
        document.querySelectorAll('[data-mode-toggle]').forEach(function (btn) {
            btn.setAttribute('aria-label', mode === 'dark' ? 'Cambiar a modo claro' : 'Cambiar a modo oscuro');
            btn.textContent = mode === 'dark' ? '☀' : '☾';
        });
    }

    /** Lee el modo guardado (o detecta preferencia del sistema). */
    function savedMode() {
        var stored = localStorage.getItem(KEY);
        if (stored === 'dark' || stored === 'light') return stored;
        return window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches
            ? 'dark' : 'light';
    }

    var current = savedMode();
    applyMode(current);

    /** Toggle llamado desde el botón en cada página. */
    window.toggleMode = function () {
        current = current === 'dark' ? 'light' : 'dark';
        localStorage.setItem(KEY, current);
        applyMode(current);
    };

    /* Sincroniza los botones cuando el DOM este listo. */
    function initButtons() {
        applyMode(current);
        document.querySelectorAll('[data-mode-toggle]').forEach(function (btn) {
            // Remove existing listener to avoid duplicates just in case
            btn.removeEventListener('click', window.toggleMode);
            btn.addEventListener('click', window.toggleMode);
        });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initButtons);
    } else {
        initButtons();
    }
})();
