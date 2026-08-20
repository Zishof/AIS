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

    var autosaveForm = document.querySelector('[data-repo-autosave]');
    if (autosaveForm && window.FormData && window.fetch) {
      var autosaveTimer = null;
      var autosaveBusy = false;
      var autosaveDirty = false;
      var autosaveStatus = autosaveForm.querySelector('.repo-autosave-status');
      function saveDraft() {
        if (autosaveBusy || !autosaveDirty) return;
        autosaveBusy = true; autosaveDirty = false;
        if (autosaveStatus) autosaveStatus.textContent = 'Menyimpan…';
        var data = new FormData(autosaveForm);
        data.set('action', 'autosave');
        fetch(autosaveForm.action, { method: 'POST', body: data, credentials: 'same-origin', headers: { 'Accept': 'application/json' } })
          .then(function (response) { return response.json().then(function (body) { if (!response.ok) throw new Error(body.message || 'Autosave gagal'); return body; }); })
          .then(function (body) {
            var version = autosaveForm.querySelector('[name="version"]');
            if (version && body.version !== null && body.version !== undefined) version.value = body.version;
            if (autosaveStatus) autosaveStatus.textContent = 'Tersimpan otomatis';
          })
          .catch(function (error) { autosaveDirty = true; if (autosaveStatus) autosaveStatus.textContent = error.message || 'Autosave gagal'; })
          .then(function () { autosaveBusy = false; });
      }
      autosaveForm.addEventListener('input', function () {
        autosaveDirty = true;
        if (autosaveStatus) autosaveStatus.textContent = 'Perubahan belum disimpan';
        if (autosaveTimer) window.clearTimeout(autosaveTimer);
        autosaveTimer = window.setTimeout(saveDraft, 1800);
      });
      window.setInterval(saveDraft, 30000);
    }
  });
}());
