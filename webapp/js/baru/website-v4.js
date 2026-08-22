(function () {
    "use strict";
    var openButton = document.querySelector("[data-menu-open]");
    var closeButton = document.querySelector("[data-menu-close]");
    var menu = document.querySelector("[data-mobile-menu]");
    var overlay = document.querySelector("[data-menu-overlay]");
    var previousFocus = null;

    if (!openButton || !closeButton || !menu || !overlay) return;

    function focusables() {
        return menu.querySelectorAll('a[href], button:not([disabled])');
    }

    function openMenu() {
        previousFocus = document.activeElement;
        overlay.hidden = false;
        menu.classList.add("is-open");
        menu.setAttribute("aria-hidden", "false");
        openButton.setAttribute("aria-expanded", "true");
        document.body.classList.add("menu-open");
        closeButton.focus();
    }

    function closeMenu() {
        menu.classList.remove("is-open");
        menu.setAttribute("aria-hidden", "true");
        openButton.setAttribute("aria-expanded", "false");
        document.body.classList.remove("menu-open");
        overlay.hidden = true;
        if (previousFocus && previousFocus.focus) previousFocus.focus();
    }

    function trapFocus(event) {
        if (event.key !== "Tab" || !menu.classList.contains("is-open")) return;
        var items = focusables();
        if (!items.length) return;
        var first = items[0];
        var last = items[items.length - 1];
        if (event.shiftKey && document.activeElement === first) {
            event.preventDefault(); last.focus();
        } else if (!event.shiftKey && document.activeElement === last) {
            event.preventDefault(); first.focus();
        }
    }

    openButton.addEventListener("click", openMenu);
    closeButton.addEventListener("click", closeMenu);
    overlay.addEventListener("click", closeMenu);
    menu.addEventListener("click", function (event) {
        if (event.target.closest("a")) closeMenu();
    });
    document.addEventListener("keydown", function (event) {
        if (event.key === "Escape" && menu.classList.contains("is-open")) closeMenu();
        trapFocus(event);
    });
    window.addEventListener("resize", function () {
        if (window.innerWidth > 1160 && menu.classList.contains("is-open")) closeMenu();
    });
}());
