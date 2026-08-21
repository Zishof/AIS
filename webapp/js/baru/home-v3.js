(function () {
    'use strict';
    var button = document.querySelector('.home-menu-button');
    var panel = document.getElementById('home-mobile-nav');
    if (!button || !panel) return;
    var lastFocus = null;

    function focusable() {
        return panel.querySelectorAll('a[href],button:not([disabled]),[tabindex]:not([tabindex="-1"])');
    }
    function closeMenu(restoreFocus) {
        button.setAttribute('aria-expanded', 'false');
        button.setAttribute('aria-label', 'Buka menu');
        panel.hidden = true;
        document.body.classList.remove('home-menu-open');
        if (restoreFocus && lastFocus) lastFocus.focus();
    }
    function openMenu() {
        lastFocus = document.activeElement;
        button.setAttribute('aria-expanded', 'true');
        button.setAttribute('aria-label', 'Tutup menu');
        panel.hidden = false;
        document.body.classList.add('home-menu-open');
        var items = focusable();
        if (items.length) items[0].focus();
    }
    button.addEventListener('click', function () {
        button.getAttribute('aria-expanded') === 'true' ? closeMenu(false) : openMenu();
    });
    panel.addEventListener('click', function (event) {
        if (event.target.closest('a')) closeMenu(false);
    });
    document.addEventListener('keydown', function (event) {
        if (button.getAttribute('aria-expanded') !== 'true') return;
        if (event.key === 'Escape') { event.preventDefault(); closeMenu(true); return; }
        if (event.key !== 'Tab') return;
        var items = focusable();
        if (!items.length) return;
        var first = items[0], last = items[items.length - 1];
        if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last.focus(); }
        else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first.focus(); }
    });
    window.addEventListener('resize', function () { if (window.innerWidth > 980) closeMenu(false); });
}());
