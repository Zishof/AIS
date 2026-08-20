(function () {
  'use strict';

  function ready(fn) {
    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', fn);
    else fn();
  }

  ready(function () {
    var filterButton = document.querySelector('[data-repo-filter-toggle]');
    var filterPanel = document.querySelector('[data-repo-facets]');
    if (filterButton && filterPanel) {
      filterButton.addEventListener('click', function () {
        var opened = filterPanel.classList.toggle('is-open');
        filterButton.setAttribute('aria-expanded', opened ? 'true' : 'false');
        if (opened) filterPanel.setAttribute('tabindex', '-1');
        if (opened) filterPanel.focus();
      });
    }

    var copyButtons = document.querySelectorAll('[data-repo-copy]');
    for (var i = 0; i < copyButtons.length; i++) {
      copyButtons[i].addEventListener('click', function () {
        var button = this;
        var value = button.getAttribute('data-repo-copy') || window.location.href;
        if (navigator.clipboard && navigator.clipboard.writeText) {
          navigator.clipboard.writeText(value).then(function () {
            var old = button.textContent;
            button.textContent = 'Tersalin';
            window.setTimeout(function () { button.textContent = old; }, 1600);
          });
        }
      });
    }
  });
}());
