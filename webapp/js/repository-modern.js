(function () {
  'use strict';

  function ready(fn) {
    if (document.readyState === 'loading') document.addEventListener('DOMContentLoaded', fn);
    else fn();
  }

  ready(function () {
    var menuButton = document.querySelector('[data-repo-menu-toggle]');
    var menu = document.querySelector('[data-repo-menu]');
    if (menuButton && menu) {
      menuButton.addEventListener('click', function () {
        var opened = menu.classList.toggle('is-open');
        menuButton.setAttribute('aria-expanded', opened ? 'true' : 'false');
      });
      menu.addEventListener('click', function () {
        menu.classList.remove('is-open'); menuButton.setAttribute('aria-expanded', 'false');
      });
      document.addEventListener('keydown', function (event) {
        if (event.key === 'Escape') { menu.classList.remove('is-open'); menuButton.setAttribute('aria-expanded', 'false'); }
      });
    }

    var suggestForms = document.querySelectorAll('[data-repo-suggest-form]');
    for (var sf = 0; sf < suggestForms.length; sf++) (function (form) {
      var input = form.querySelector('input[name="q"]');
      var field = form.querySelector('select[name="field"]');
      var panel = form.querySelector('.repo-suggestions');
      var clearButton = form.querySelector('[data-repo-search-clear]');
      if (!input || !field || !panel || !window.fetch) return;
      var timer = null, requestNumber = 0, activeIndex = -1, options = [];

      function closeSuggestions() {
        panel.hidden = true; panel.textContent = ''; options = []; activeIndex = -1;
        input.setAttribute('aria-expanded', 'false'); input.removeAttribute('aria-activedescendant');
      }
      function updateClear() { if (clearButton) clearButton.hidden = input.value.length === 0; }
      function activate(index) {
        if (!options.length) return;
        activeIndex = (index + options.length) % options.length;
        for (var i = 0; i < options.length; i++) options[i].classList.toggle('is-active', i === activeIndex);
        input.setAttribute('aria-activedescendant', options[activeIndex].id);
        options[activeIndex].scrollIntoView({ block: 'nearest' });
      }
      function choose(button) {
        input.value = button.getAttribute('data-value') || button.querySelector('strong').textContent;
        updateClear(); closeSuggestions(); form.submit();
      }
      function render(rows) {
        panel.textContent = ''; options = []; activeIndex = -1;
        if (!rows || !rows.length) { showSuggestionState('Tidak ada saran. Tekan Enter untuk mencari kata kunci ini.'); return; }
        for (var i = 0; i < rows.length; i++) {
          var row = rows[i], button = document.createElement('button');
          button.type = 'button'; button.className = 'repo-suggestion'; button.id = panel.id + '-option-' + i;
          button.setAttribute('role', 'option'); button.setAttribute('data-value', row.value || row.label || '');
          var typeLabels = { title: 'Judul', item: 'Judul', author: 'Penulis', subject: 'Subjek', identifier: 'Identifier', abstract: 'Abstrak', program: 'Program studi', advisor: 'Pembimbing', fulltext: 'Isi dokumen' };
          var type = document.createElement('span'); type.className = 'repo-suggestion-type'; type.textContent = typeLabels[row.type] || row.type || 'Judul';
          var text = document.createElement('span'), label = document.createElement('strong'), detail = document.createElement('small');
          label.textContent = row.label || ''; detail.textContent = row.detail || ''; text.appendChild(label); if (row.detail) text.appendChild(detail);
          button.appendChild(type); button.appendChild(text); button.addEventListener('click', function () { choose(this); }); panel.appendChild(button); options.push(button);
        }
        panel.hidden = false; input.setAttribute('aria-expanded', 'true');
      }
      function showSuggestionState(message) {
        panel.textContent = ''; options = []; activeIndex = -1; input.removeAttribute('aria-activedescendant');
        var state = document.createElement('span'); state.className = 'repo-suggestion-state';
        state.setAttribute('role', 'status'); state.textContent = message; panel.appendChild(state); panel.hidden = false;
        input.setAttribute('aria-expanded', 'true');
      }
      function loadSuggestions() {
        var term = input.value.trim(); updateClear();
        if (term.length < 2) { closeSuggestions(); return; }
        var current = ++requestNumber;
        var endpoint = form.getAttribute('data-suggest-url') || form.action + '?action=suggest';
        showSuggestionState('Mencari saran…');
        fetch(endpoint + '&q=' + encodeURIComponent(term) + '&field=' + encodeURIComponent(field.value),
          { credentials: 'same-origin', headers: { 'Accept': 'application/json' } })
          .then(function (response) { if (!response.ok) throw new Error('suggestion unavailable'); return response.json(); })
          .then(function (body) { if (current === requestNumber) render(body.suggestions || []); })
          .catch(function () { if (current === requestNumber) showSuggestionState('Saran belum tersedia. Tekan Enter untuk mencari.'); });
      }
      input.addEventListener('input', function () { updateClear(); if (timer) window.clearTimeout(timer); timer = window.setTimeout(loadSuggestions, 250); });
      field.addEventListener('change', function () { if (input.value.trim().length >= 2) loadSuggestions(); });
      input.addEventListener('keydown', function (event) {
        if (event.key === 'ArrowDown' && options.length) { event.preventDefault(); activate(activeIndex + 1); }
        else if (event.key === 'ArrowUp' && options.length) { event.preventDefault(); activate(activeIndex - 1); }
        else if (event.key === 'Enter' && activeIndex >= 0) { event.preventDefault(); choose(options[activeIndex]); }
        else if (event.key === 'Escape') closeSuggestions();
      });
      if (clearButton) clearButton.addEventListener('click', function () { input.value = ''; updateClear(); closeSuggestions(); input.focus(); });
      document.addEventListener('click', function (event) { if (!form.contains(event.target)) closeSuggestions(); });
      updateClear();
    }(suggestForms[sf]));

    var searchSkeleton = document.querySelector('[data-repo-search-skeleton]');
    var repositorySearchForms = document.querySelectorAll('form[role="search"], .repo-advanced-search form');
    for (var rs = 0; rs < repositorySearchForms.length; rs++) repositorySearchForms[rs].addEventListener('submit', function () {
      if (searchSkeleton) { searchSkeleton.hidden = false; searchSkeleton.setAttribute('aria-hidden', 'false'); }
      this.setAttribute('aria-busy', 'true');
      var submit = this.querySelector('[type="submit"]'); if (submit) { submit.disabled = true; submit.setAttribute('data-old-label', submit.textContent); submit.textContent = 'Memuat…'; }
    });

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

    var helpSearch = document.querySelector('[data-repo-help-search]');
    if (helpSearch) {
      var helpChapters = document.querySelectorAll('.repo-help-chapter');
      var helpStatus = document.querySelector('[data-repo-help-status]');
      var helpClear = document.querySelector('[data-repo-help-clear]');
      var helpTocLinks = document.querySelectorAll('.repo-help-toc a[href^="#"]');
      function filterHelp() {
        var term = helpSearch.value.toLowerCase().replace(/^\s+|\s+$/g, '');
        var tokens = term ? term.split(/\s+/) : [], visible = 0;
        for (var h = 0; h < helpChapters.length; h++) {
          var text = (helpChapters[h].textContent || '').toLowerCase(), match = true;
          for (var t = 0; t < tokens.length; t++) if (text.indexOf(tokens[t]) < 0) { match = false; break; }
          helpChapters[h].hidden = !match; if (match) visible++;
        }
        for (var l = 0; l < helpTocLinks.length; l++) {
          var target = document.querySelector(helpTocLinks[l].getAttribute('href'));
          helpTocLinks[l].hidden = !!(target && target.hidden);
        }
        if (helpStatus) helpStatus.textContent = term ? visible + ' dari ' + helpChapters.length + ' bab cocok.' : helpChapters.length + ' bab tersedia.';
        if (helpClear) helpClear.hidden = term.length === 0;
      }
      helpSearch.addEventListener('input', filterHelp);
      if (helpClear) helpClear.addEventListener('click', function () { helpSearch.value = ''; filterHelp(); helpSearch.focus(); });
      filterHelp();
    }
    var helpFeedbackForm = document.querySelector('.repo-help-feedback-form');
    if (helpFeedbackForm) helpFeedbackForm.addEventListener('submit', function () {
      var content = helpFeedbackForm.querySelector('[data-repo-help-content]');
      var anchor = (window.location.hash || '').replace(/^#/, '');
      if (content && /^[a-z0-9_-]+$/i.test(anchor) && document.getElementById(anchor)) content.value = 'repository-help-v2.' + anchor.toLowerCase();
    });

    // Seluruh permukaan kartu membuka detail, sementara tautan/tombol di dalam
    // kartu tetap menjalankan aksi spesifiknya sendiri.
    var linkedCards = document.querySelectorAll('[data-repo-card-href]');
    for (var lc = 0; lc < linkedCards.length; lc++) linkedCards[lc].addEventListener('click', function (event) {
      var node = event.target;
      while (node && node !== this) {
        if (/^(A|BUTTON|INPUT|SELECT|TEXTAREA|SUMMARY|LABEL)$/.test(node.tagName)) return;
        node = node.parentNode;
      }
      var target = this.getAttribute('data-repo-card-href');
      if (target) window.location.href = target;
    });

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
