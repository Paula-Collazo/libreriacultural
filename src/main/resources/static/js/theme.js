(function () {
    const storageKey = "lc_theme";
    const defaultTheme = "rosa";
    const allowedThemes = new Set(["rosa", "azul", "menta", "durazno"]);
    const legacyThemeMap = {
        otono: "durazno",
        verano: "rosa",
        salvia: "menta"
    };

    const root = document.documentElement;
    const selector = document.querySelector("[data-theme-select]");

    function applyTheme(theme) {
        const selectedTheme = allowedThemes.has(theme) ? theme : defaultTheme;
        root.setAttribute("data-theme", selectedTheme);
        return selectedTheme;
    }

    const rawSavedTheme = localStorage.getItem(storageKey) || defaultTheme;
    const normalizedTheme = legacyThemeMap[rawSavedTheme] || rawSavedTheme;
    const activeTheme = applyTheme(normalizedTheme);
    if (activeTheme !== rawSavedTheme) {
        localStorage.setItem(storageKey, activeTheme);
    }

    if (selector) {
        selector.value = activeTheme;
        selector.addEventListener("change", function (event) {
            const newTheme = event.target.value;
            const appliedTheme = applyTheme(newTheme);
            localStorage.setItem(storageKey, appliedTheme);
        });
    }
})();
