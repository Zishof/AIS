/**
 * AIS Advanced Filter — "Pencarian Lebih Lanjut" popup
 *
 * Auto-enhances every .ais-crud-filter-card that has more than 1 filter row:
 *  - Row 1 stays visible as the "quick search" strip
 *  - Rows 2+ and extra action buttons (anything other than Tambah/Cari) are
 *    moved into a centered modal popup when the user clicks "Pencarian Lanjut"
 *
 * ZK-safety: We move DOM nodes (not clone them). ZK's event system uses widget
 * UUIDs stored as DOM attributes, so moving a node does NOT break event binding.
 * Nodes are always moved BACK to their original position before any ZK server
 * round-trip is triggered ("Terapkan Filter" → restore → click Cari).
 *
 * Works globally — no per-ZUL changes required (beyond having the css class).
 */
(function () {
    'use strict';

    /* ── state ─────────────────────────────────────────────────────────────── */
    var activeCard   = null;   // currently-open card
    var movedRows    = [];     // rows taken from card's tbody
    var movedBtns    = [];     // extra action buttons taken from card's actions

    /* ── helpers ────────────────────────────────────────────────────────────── */

    function qs(el, sel)  { return el.querySelector(sel); }
    function qsa(el, sel) { return Array.from(el.querySelectorAll(sel)); }

    /** Return true if btn should stay in the primary strip (Tambah / Cari). */
    function isPrimaryBtn(btn) {
        if (btn.getAttribute('data-af-primary') === '1') return true;
        if (btn.classList.contains('ais-af-lanjut-btn'))  return false;

        var img   = qs(btn, 'img');
        var src   = img ? (img.getAttribute('src') || '') : '';
        if (src.match(/new\.gif|add\.gif/i))    return true;
        if (src.match(/search\.gif|find\.gif/i)) return true;

        var label = (qs(btn, '.z-toolbarbutton-cnt, .z-label') || btn)
                        .textContent.replace(/\s+/g, ' ').trim();
        if (/^(Tambah|Add|Baru|New)$/i.test(label))           return true;
        if (/^(Cari|Cari Data|Search|Find)$/i.test(label))    return true;

        return false;
    }

    /** Mark each toolbarbutton in the actions bar as primary / extra. */
    function labelButtons(card) {
        var actions = qs(card, '.ais-crud-filter-actions');
        if (!actions) return;
        qsa(actions, '[class*="z-toolbarbutton"]').forEach(function (btn) {
            if (btn.classList.contains('ais-af-lanjut-btn')) return;
            if (isPrimaryBtn(btn)) {
                btn.setAttribute('data-af-primary', '1');
            } else if (!btn.getAttribute('data-af-extra')) {
                btn.setAttribute('data-af-extra', '1');
            }
        });
    }

    /* ── popup DOM ──────────────────────────────────────────────────────────── */

    var _panel    = null;
    var _backdrop = null;

    function ensurePopup() {
        if (_panel) return;

        _backdrop = document.createElement('div');
        _backdrop.className = 'ais-af-backdrop';
        _backdrop.addEventListener('click', function () { closePanel(); });
        document.body.appendChild(_backdrop);

        _panel = document.createElement('div');
        _panel.className = 'ais-af-panel';
        _panel.setAttribute('role', 'dialog');
        _panel.setAttribute('aria-modal', 'true');
        _panel.innerHTML =
            '<div class="ais-af-panel-head">' +
                '<span class="ais-af-panel-title">' +
                    '<svg viewBox="0 0 20 20" width="15" height="15" fill="none" stroke="currentColor" stroke-width="2">' +
                        '<circle cx="8" cy="8" r="6"/><path d="M14 14l4 4"/>' +
                        '<line x1="5" y1="8" x2="11" y2="8"/><line x1="8" y1="5" x2="8" y2="11"/>' +
                    '</svg> Pencarian Lebih Lanjut' +
                '</span>' +
                '<div class="ais-af-extra-btns" id="aisAfExtraBtns"></div>' +
                '<button class="ais-af-close-btn" id="aisAfClose" title="Tutup (Esc)">' +
                    '<svg viewBox="0 0 12 12" width="11" height="11" stroke="currentColor" stroke-width="2"><path d="M1 1l10 10M11 1L1 11"/></svg>' +
                '</button>' +
            '</div>' +
            '<div class="ais-af-panel-body">' +
                '<table class="ais-af-adv-table">' +
                    '<tbody id="aisAfAdvTbody"></tbody>' +
                '</table>' +
            '</div>' +
            '<div class="ais-af-panel-foot">' +
                '<button class="ais-af-apply-btn" id="aisAfApply">' +
                    '<svg viewBox="0 0 16 16" width="12" height="12" fill="currentColor">' +
                        '<path d="M6 2a4 4 0 1 0 0 8A4 4 0 0 0 6 2zm-6 4a6 6 0 1 1 10.89 3.476l4.817 4.817a1 1 0 0 1-1.414 1.414l-4.816-4.816A6 6 0 0 1 0 6z"/>' +
                    '</svg> Terapkan Filter' +
                '</button>' +
                '<button class="ais-af-cancel-btn" id="aisAfCancel">Tutup</button>' +
            '</div>';

        document.body.appendChild(_panel);

        document.getElementById('aisAfClose').addEventListener('click',  closePanel);
        document.getElementById('aisAfCancel').addEventListener('click', closePanel);
        document.getElementById('aisAfApply').addEventListener('click',  applyFilter);

        document.addEventListener('keydown', function (e) {
            if (e.key === 'Escape' && activeCard) closePanel();
        });
    }

    /* ── open / close ───────────────────────────────────────────────────────── */

    function openPanel(card) {
        ensurePopup();
        if (activeCard) closePanel(true); // silent restore before re-open
        activeCard = card;

        // Re-label buttons (Java may add them after page init)
        labelButtons(card);

        // Move advanced rows into popup tbody
        var rowsContainer = qs(card, '.z-rows') || qs(card, 'tbody');
        var advTbody      = document.getElementById('aisAfAdvTbody');
        advTbody.innerHTML = '';
        movedRows = qsa(rowsContainer, '[data-af-advanced]');
        movedRows.forEach(function (r) { advTbody.appendChild(r); });

        // Move extra buttons into popup header
        var actions      = qs(card, '.ais-crud-filter-actions');
        var extraSlot    = document.getElementById('aisAfExtraBtns');
        extraSlot.innerHTML = '';
        movedBtns = qsa(actions, '[data-af-extra]');
        movedBtns.forEach(function (b) { extraSlot.appendChild(b); });

        // Visual state
        var lBtn = qs(card, '.ais-af-lanjut-btn');
        if (lBtn) lBtn.classList.add('active');

        _backdrop.classList.add('active');
        _panel.classList.add('active');
        document.body.classList.add('ais-af-open');

        // Animate in (next frame)
        requestAnimationFrame(function () {
            requestAnimationFrame(function () {
                _panel.classList.add('visible');
            });
        });
    }

    function closePanel(silent) {
        if (!activeCard) return;
        var card = activeCard;

        // Restore rows to original tbody
        var rowsContainer = qs(card, '.z-rows') || qs(card, 'tbody');
        movedRows.forEach(function (r) { rowsContainer.appendChild(r); });
        movedRows = [];

        // Restore buttons to original actions bar
        var actions = qs(card, '.ais-crud-filter-actions');
        movedBtns.forEach(function (b) { actions.appendChild(b); });
        movedBtns = [];

        var lBtn = qs(card, '.ais-af-lanjut-btn');
        if (lBtn) lBtn.classList.remove('active');

        if (_panel)    { _panel.classList.remove('active', 'visible'); }
        if (_backdrop) { _backdrop.classList.remove('active'); }
        document.body.classList.remove('ais-af-open');

        activeCard = null;

        if (silent !== true) {
            // Update active-filter badge on lanjut button
            updateBadge(card);
        }
    }

    function applyFilter() {
        var card = activeCard;
        if (!card) return;

        // Restore DOM FIRST (ZK must find all fields in original position)
        closePanel(true);

        // Update badge
        updateBadge(card);

        // Click the "Cari" button
        var actions = qs(card, '.ais-crud-filter-actions');
        var cariBtn = qsa(actions, '[class*="z-toolbarbutton"]').find(function (b) {
            return b.getAttribute('data-af-primary') === '1' && !isAddBtn(b);
        });
        if (cariBtn) cariBtn.click();
    }

    function isAddBtn(btn) {
        var img = qs(btn, 'img');
        if (img && (img.getAttribute('src') || '').match(/new\.gif|add\.gif/i)) return true;
        var label = ((qs(btn, '.z-toolbarbutton-cnt') || btn).textContent || '').trim();
        return /^(Tambah|Add|Baru|New)$/i.test(label);
    }

    /** Show a dot on the "Pencarian Lanjut" button if any advanced field is filled. */
    function updateBadge(card) {
        var lBtn = qs(card, '.ais-af-lanjut-btn');
        if (!lBtn) return;
        var rowsContainer = qs(card, '.z-rows') || qs(card, 'tbody');
        var advRows = qsa(rowsContainer, '[data-af-advanced]');
        var hasValue = advRows.some(function (row) {
            return qsa(row, 'input[type="text"], input[type="number"]').some(function (i) {
                return i.value && i.value.trim() !== '';
            }) || qsa(row, 'input[type="checkbox"]').some(function (cb) {
                return cb.checked;
            });
        });
        lBtn.classList.toggle('has-value', hasValue);
    }

    /* ── enhance one card ───────────────────────────────────────────────────── */

    function enhance(card) {
        card.setAttribute('data-af', '1');

        var grid          = qs(card, '.ais-crud-filter-grid');
        if (!grid) return;
        var rowsContainer = qs(grid, '.z-rows') || qs(grid, 'tbody');
        if (!rowsContainer) return;

        var rows = qsa(rowsContainer, ':scope > tr.z-row, :scope > .z-row');
        if (rows.length <= 1) return; // single row — no popup needed

        // Mark rows 2+ as advanced
        rows.slice(1).forEach(function (r) { r.setAttribute('data-af-advanced', '1'); });

        // Label buttons (also done again at open-time for dynamically added ones)
        labelButtons(card);

        // Build "Pencarian Lanjut" toggle button
        var lanjutBtn = document.createElement('button');
        lanjutBtn.className  = 'ais-af-lanjut-btn';
        lanjutBtn.type       = 'button';
        lanjutBtn.title      = 'Buka panel pencarian lanjut';
        lanjutBtn.innerHTML  =
            '<svg viewBox="0 0 16 16" width="12" height="12" fill="none" stroke="currentColor" stroke-width="2">' +
                '<circle cx="6.5" cy="6.5" r="5.5"/><path d="M11.5 11.5l3.5 3.5"/>' +
                '<line x1="4" y1="6.5" x2="9" y2="6.5"/><line x1="6.5" y1="4" x2="6.5" y2="9"/>' +
            '</svg><span>Pencarian Lanjut</span>';

        lanjutBtn.addEventListener('click', function (e) {
            e.preventDefault();
            e.stopPropagation();
            openPanel(card);
        });

        var actions  = qs(card, '.ais-crud-filter-actions');
        var firstBtn = actions && qs(actions, '[class*="z-toolbarbutton"]');
        if (firstBtn) {
            actions.insertBefore(lanjutBtn, firstBtn);
        } else if (actions) {
            actions.appendChild(lanjutBtn);
        }
    }

    /* ── init ───────────────────────────────────────────────────────────────── */

    function enhanceAll() {
        qsa(document, '.ais-crud-filter-card:not([data-af])').forEach(enhance);
    }

    /* Debounced re-init (called after DOM mutations) */
    var debTimer = null;
    function scheduleEnhance() {
        clearTimeout(debTimer);
        debTimer = setTimeout(enhanceAll, 120);
    }

    /* MutationObserver — catches ZK includes loaded after page init */
    var observer = new MutationObserver(function (mutations) {
        var relevant = mutations.some(function (m) {
            return m.type === 'childList' &&
                Array.from(m.addedNodes).some(function (n) {
                    return n.nodeType === 1 && (
                        n.classList && n.classList.contains('ais-crud-filter-card') ||
                        (n.querySelector && n.querySelector('.ais-crud-filter-card:not([data-af])'))
                    );
                });
        });
        if (relevant) scheduleEnhance();
    });

    function start() {
        enhanceAll();
        observer.observe(document.body, { childList: true, subtree: true });
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', function () { setTimeout(start, 150); });
    } else {
        setTimeout(start, 150);
    }

    /* Expose for manual re-init if needed */
    window.AisAdvancedFilter = { init: enhanceAll };

}());
