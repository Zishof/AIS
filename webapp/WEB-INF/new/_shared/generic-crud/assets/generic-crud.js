(function () {
  'use strict';
  var root = document.currentScript && document.currentScript.previousElementSibling;
  if (!root || !root.hasAttribute('data-gc-root')) root = document.querySelector('[data-gc-root]');
  if (!root || root.getAttribute('data-ready') === '1') return;
  root.setAttribute('data-ready', '1');

  var endpoint = root.getAttribute('data-endpoint');
  var meta = null;
  var state = {page: 1, pageSize: 10, q: '', sort: '', direction: 'ASC', filters: [], columns: [], editing: null, version: null, dirty: false, importJob: null};

  function node(name, attrs, text) {
    var result = document.createElement(name), key;
    if (attrs) for (key in attrs) if (Object.prototype.hasOwnProperty.call(attrs, key)) {
      if (key === 'class') result.className = attrs[key]; else result.setAttribute(key, attrs[key]);
    }
    if (text !== undefined) result.textContent = text;
    return result;
  }
  function query(name) { return root.querySelector('[data-gc-' + name + ']'); }
  function pairs(params) {
    var result = [], key, value, i;
    for (key in params) if (Object.prototype.hasOwnProperty.call(params, key) && params[key] !== null && params[key] !== undefined) {
      value = params[key];
      if (Object.prototype.toString.call(value) === '[object Array]') {
        for (i = 0; i < value.length; i++) result.push(encodeURIComponent(key) + '=' + encodeURIComponent(value[i]));
      } else result.push(encodeURIComponent(key) + '=' + encodeURIComponent(value));
    }
    return result.join('&');
  }
  function url(action, params) {
    params = params || {}; params.action = action;
    return endpoint + (endpoint.indexOf('?') < 0 ? '?' : '&') + pairs(params);
  }
  function api(action, params, method) {
    params = params || {}; params.action = action;
    var options = {method: method || 'GET', credentials: 'same-origin', headers: {'Accept': 'application/json'}};
    var target = endpoint;
    if (options.method === 'POST') {
      options.headers['Content-Type'] = 'application/x-www-form-urlencoded;charset=UTF-8'; options.body = pairs(params);
    } else target += (target.indexOf('?') < 0 ? '?' : '&') + pairs(params);
    return fetch(target, options).then(function (response) {
      return response.json().then(function (body) {
        if (!response.ok || body.success === false) { var error = new Error(body.message || 'Permintaan gagal'); error.payload = body; throw error; }
        return body.data;
      });
    });
  }
  function notify(message) {
    var box = query('alert'); box.textContent = message || ''; box.hidden = !message;
    if (message) window.setTimeout(function () { box.hidden = true; }, 7000);
  }
  function filterParams() {
    var result = {q: state.q, sort: state.sort, direction: state.direction}, fields = [], operators = [], values = [];
    state.filters.forEach(function (filter) { fields.push(filter.property); operators.push(filter.operator); values.push(filter.value || ''); });
    result.filterField = fields; result.filterOperator = operators; result.filterValue = values;
    return result;
  }
  function readableFields() {
    return (meta.fields || []).filter(function (field) { return field.readable !== false && !field.sensitive; });
  }
  function visibleFields() {
    var allowed = {};
    state.columns.forEach(function (property) { allowed[property] = true; });
    return readableFields().filter(function (field) { return !!allowed[field.property]; });
  }
  function defaultColumns() {
    return readableFields().filter(function (field) { return field.tableVisible; }).map(function (field) { return field.property; });
  }
  function loadMeta() {
    return api('meta').then(function (data) {
      meta = data; query('title').textContent = data.displayName; state.pageSize = data.pageSize || 10;
      query('page-size').value = String(state.pageSize); query('add').hidden = !data.canCreate;
      query('template').hidden = !data.importEnabled; query('import-label').hidden = !data.importEnabled;
      var formats = query('export-format'), enabled = {xlsx: data.exportXlsx, pdf: data.exportPdf, docx: data.exportDocx, pptx: data.exportPptx};
      Array.prototype.forEach.call(formats.options, function (option) { option.disabled = !enabled[option.value]; });
      buildParityActions();
      return api('preference_load').catch(function () { return null; }).then(function (preference) {
        state.columns = preference && preference.columns ? preference.columns : defaultColumns();
        if (preference && preference.pageSize) { state.pageSize = preference.pageSize; query('page-size').value = String(state.pageSize); }
        if (preference && preference.sort) { state.sort = preference.sort.property || ''; state.direction = preference.sort.direction || 'ASC'; }
        if (preference && preference.filters) state.filters = preference.filters;
        buildFilterFields(); buildColumnChooser(); renderFilters(); buildHeader(); return loadList();
      });
    });
  }
  function buildParityActions() {
    var actions = meta.formActions || [], container = query('parity-actions'), groups = {}, nativeCount = 0, bridgeCount = 0;
    if (!actions.length) return;
    query('parity').hidden = false; container.textContent = '';
    actions.forEach(function (action) {
      var group = action.group || 'Lainnya';
      if (!groups[group]) groups[group] = [];
      groups[group].push(action);
      if (action.implementationStatus === 'NEW_UI_NATIVE') nativeCount++; else if (action.implementationStatus === 'SAFE_LEGACY_BRIDGE') bridgeCount++;
    });
    query('parity-summary').textContent = nativeCount + ' fungsi native New UI • ' + bridgeCount + ' fungsi memakai business flow ZKOSS asli';
    Object.keys(groups).forEach(function (groupName) {
      var group = node('section', {'class': 'gc-parity-group'}), title = node('h3', null, groupName), list = node('div', {'class': 'gc-parity-grid'});
      group.appendChild(title);
      groups[groupName].forEach(function (action) {
        var card = node('article', {'class': 'gc-parity-action'}), top = node('div'), label = node('strong', null, action.label), status;
        top.appendChild(label); card.appendChild(top);
        card.appendChild(node('small', null, 'Sumber: ' + (action.sourceHandler || action.sourceAction || 'ZKOSS')));
        if (action.implementationStatus === 'NEW_UI_NATIVE') {
          status = node('span', {'class': 'gc-parity-status native'}, 'New UI aktif'); card.appendChild(status);
        } else if (action.legacyRoute) {
          status = node('button', {type: 'button', 'class': 'gc-btn gc-parity-open'}, 'Buka fungsi lengkap');
          status.addEventListener('click', function () { openLegacy(action); }); card.appendChild(status);
        } else {
          card.appendChild(node('span', {'class': 'gc-parity-status blocked'}, 'Belum dipetakan'));
        }
        list.appendChild(card);
      });
      group.appendChild(list); container.appendChild(group);
    });
  }
  function openLegacy(action) {
    if (!action || !action.legacyRoute) { notify('Route ZKOSS tidak tersedia.'); return; }
    query('legacy-title').textContent = action.label || 'Modul ZKOSS';
    query('legacy-frame').src = action.legacyRoute;
    query('overlay').hidden = false; query('legacy').hidden = false;
  }
  function buildHeader() {
    var row = node('tr');
    visibleFields().forEach(function (field) {
      var heading = node('th', field.sortable ? {'data-sort': field.property} : null, field.label + (state.sort === field.property ? (state.direction === 'ASC' ? ' ▲' : ' ▼') : ''));
      if (field.sortable) heading.addEventListener('click', function () {
        if (state.sort === field.property) state.direction = state.direction === 'ASC' ? 'DESC' : 'ASC'; else { state.sort = field.property; state.direction = 'ASC'; }
        state.page = 1; buildHeader(); savePreference(); loadList();
      });
      row.appendChild(heading);
    });
    row.appendChild(node('th', null, 'Aksi')); query('head').textContent = ''; query('head').appendChild(row);
  }
  function buildFilterFields() {
    var select = query('filter-field'); select.textContent = '';
    readableFields().filter(function (field) { return field.searchable || field.quickFilter; }).forEach(function (field) { select.appendChild(node('option', {value: field.property}, field.label)); });
  }
  function renderFilters() {
    var chips = query('filter-chips'); chips.textContent = '';
    state.filters.forEach(function (filter, index) {
      var field = meta.fields.filter(function (candidate) { return candidate.property === filter.property; })[0];
      var chip = node('span', {'class': 'gc-chip'}, (field ? field.label : filter.property) + ' ' + filter.operator + (filter.value ? ' ' + filter.value : '') + ' ');
      var remove = node('button', {type: 'button', 'aria-label': 'Hapus filter'}, '×');
      remove.addEventListener('click', function () { state.filters.splice(index, 1); state.page = 1; renderFilters(); savePreference(); loadList(); });
      chip.appendChild(remove); chips.appendChild(chip);
    });
  }
  function addFilter() {
    var property = query('filter-field').value, operator = query('filter-operator').value, value = query('filter-value').value;
    if (!property) return;
    if (operator !== 'IS_NULL' && operator !== 'IS_NOT_NULL' && !value) { notify('Nilai filter wajib diisi.'); return; }
    state.filters.push({property: property, operator: operator, value: value}); state.page = 1; query('filter-value').value = '';
    renderFilters(); savePreference(); loadList();
  }
  function buildColumnChooser() {
    var container = query('columns'); container.textContent = '';
    readableFields().forEach(function (field) {
      var label = node('label', {'class': 'gc-column-option'}), checkbox = node('input', {type: 'checkbox', value: field.property});
      checkbox.checked = state.columns.indexOf(field.property) >= 0;
      checkbox.addEventListener('change', function () {
        if (checkbox.checked && state.columns.indexOf(field.property) < 0) state.columns.push(field.property);
        if (!checkbox.checked && state.columns.indexOf(field.property) >= 0 && state.columns.length > 1) state.columns.splice(state.columns.indexOf(field.property), 1);
        if (!state.columns.length) { state.columns = defaultColumns(); buildColumnChooser(); }
        buildHeader(); savePreference(); loadList();
      });
      label.appendChild(checkbox); label.appendChild(document.createTextNode(field.label)); container.appendChild(label);
    });
  }
  function savePreference() {
    if (!meta) return;
    api('preference_save', {nui_csrf: meta.csrf, preferenceJson: JSON.stringify({columns: state.columns, pageSize: state.pageSize, sort: {property: state.sort, direction: state.direction}, filters: state.filters})}, 'POST').catch(function () {});
  }
  function resetPreference() {
    api('preference_reset', {nui_csrf: meta.csrf}, 'POST').then(function (preference) {
      state.columns = preference.columns || defaultColumns(); state.pageSize = preference.pageSize || meta.pageSize; state.sort = ''; state.filters = [];
      query('page-size').value = String(state.pageSize); buildColumnChooser(); renderFilters(); buildHeader(); return loadList();
    }).catch(function (error) { notify(error.message); });
  }
  function loadList() {
    query('status').textContent = 'Memuat…'; var params = filterParams(); params.page = state.page; params.pageSize = state.pageSize;
    return api('list', params).then(renderRows).catch(function (error) { notify(error.message); query('status').textContent = 'Gagal memuat'; });
  }
  function renderRows(data) {
    var body = query('body'), fields = visibleFields(); body.textContent = '';
    if (!data.rows || !data.rows.length) {
      var empty = node('tr'); empty.appendChild(node('td', {colspan: String(fields.length + 1), 'class': 'gc-empty'}, 'Tidak ada data yang cocok.')); body.appendChild(empty);
    } else data.rows.forEach(function (rowData) {
      var row = node('tr');
      fields.forEach(function (field) {
        var cell = node('td', {'data-label': field.label}), value = rowData[field.property];
        if (field.javaType === 'java.lang.Boolean' || field.javaType === 'boolean') cell.appendChild(node('span', {'class': 'gc-pill' + (value ? '' : ' off')}, value ? 'Aktif' : 'Tidak aktif'));
        else if (field.relationEntityKey) cell.textContent = rowData[field.property + '__label'] || (value === null || value === undefined ? '—' : String(value));
        else cell.textContent = value === null || value === undefined ? '—' : String(value);
        row.appendChild(cell);
      });
      var actions = node('td', {'data-label': 'Aksi'}), buttons = node('div', {'class': 'gc-row-actions'});
      var rowId = rowData[meta.identifierProperty];
      var detail = node('button', {type: 'button', 'class': 'gc-btn'}, meta.canUpdate ? 'Edit' : 'Detail'); detail.addEventListener('click', function () { openExisting(rowId); }); buttons.appendChild(detail);
      if (meta.rowAudit) { var audit = node('button', {type: 'button', 'class': 'gc-btn'}, 'Riwayat'); audit.addEventListener('click', function () { openAudit(rowId); }); buttons.appendChild(audit); }
      if (meta.canDelete) { var remove = node('button', {type: 'button', 'class': 'gc-btn'}, 'Nonaktifkan'); remove.addEventListener('click', function () { removeRow(rowId); }); buttons.appendChild(remove); }
      actions.appendChild(buttons); row.appendChild(actions); body.appendChild(row);
    });
    query('status').textContent = data.total + ' data'; query('page-info').textContent = 'Halaman ' + data.page + ' dari ' + Math.max(1, data.pageCount);
    query('prev').disabled = data.page <= 1; query('next').disabled = data.page >= data.pageCount; state.page = data.page;
  }
  function formFields() { return (meta.fields || []).filter(function (field) { return state.editing === null ? field.createable : (field.updateable || (!meta.canUpdate && field.detailVisible)); }); }
  function openDrawer(rowData) {
    state.dirty = false; var editing = state.editing !== null, container = query('fields'); container.textContent = '';
    query('form-title').textContent = (editing ? (meta.canUpdate ? 'Ubah ' : 'Detail ') : 'Tambah ') + meta.displayName; query('form-mode').textContent = meta.formMode;
    formFields().forEach(function (field) {
      var wrap = node('div', {'class': 'gc-field' + (field.editorType === 'textarea' ? ' gc-field-wide' : '')}), label = node('label', {'for': 'gc-' + field.property}, field.label + (field.required ? ' *' : '')), input, dataList = null;
      if (field.editorType === 'textarea') input = node('textarea', {rows: '4'});
      else if (field.editorType === 'checkbox') input = node('input', {type: 'checkbox'});
      else if (field.editorType === 'select') {
        input = node('select'); input.appendChild(node('option', {value: ''}, '— Pilih —'));
        (field.enumValues || []).forEach(function (value) { input.appendChild(node('option', {value: value}, value)); });
      } else if (field.editorType === 'relation') {
        dataList = node('datalist', {id: 'gc-list-' + field.property});
        input = node('input', {type: 'search', list: dataList.id, autocomplete: 'off', placeholder: 'Ketik ID atau cari pilihan…'});
      } else input = node('input', {type: ['number', 'date', 'time', 'datetime-local'].indexOf(field.editorType) >= 0 ? field.editorType : 'text'});
      input.id = 'gc-' + field.property; input.name = field.property;
      input.required = !!field.required;
      if (rowData && rowData[field.property] !== null && rowData[field.property] !== undefined) {
        if (input.type === 'checkbox') input.checked = !!rowData[field.property];
        else if (input.type === 'date') { var date = new Date(rowData[field.property]); if (!isNaN(date.getTime())) input.value = date.getFullYear() + '-' + String(date.getMonth() + 1).replace(/^(\d)$/, '0$1') + '-' + String(date.getDate()).replace(/^(\d)$/, '0$1'); }
        else if (input.type === 'datetime-local') { var timestamp = new Date(rowData[field.property]); if (!isNaN(timestamp.getTime())) input.value = timestamp.getFullYear() + '-' + String(timestamp.getMonth() + 1).replace(/^(\d)$/, '0$1') + '-' + String(timestamp.getDate()).replace(/^(\d)$/, '0$1') + 'T' + String(timestamp.getHours()).replace(/^(\d)$/, '0$1') + ':' + String(timestamp.getMinutes()).replace(/^(\d)$/, '0$1'); }
        else input.value = String(rowData[field.property]);
      }
      if (editing && !meta.canUpdate) input.disabled = true; input.addEventListener('input', function () { state.dirty = true; }); wrap.appendChild(label); wrap.appendChild(input); if (dataList) wrap.appendChild(dataList); container.appendChild(wrap);
      if (field.editorType === 'relation' && !input.disabled) bindRelationLookup(field, input, dataList, rowData);
    });
    query('form-error').hidden = true; query('overlay').hidden = false; query('drawer').hidden = false; var first = container.querySelector('input,textarea,select'); if (first) first.focus();
  }
  function bindRelationLookup(field, input, dataList, rowData) {
    var relationTimer = null, currentId = rowData ? rowData[field.property] : null, currentLabel = rowData ? rowData[field.property + '__label'] : null;
    function addOption(id, label) {
      if (id === null || id === undefined) return;
      var option = node('option', {value: String(id)}, label || String(id)); dataList.appendChild(option);
    }
    function loadOptions(search) {
      api('relation_lookup', {field: field.property, q: search || '', page: 1, pageSize: 30}).then(function (data) {
        dataList.textContent = ''; if (currentId !== null && currentId !== undefined) addOption(currentId, currentLabel);
        (data.items || []).forEach(function (item) { addOption(item.id, item.label); });
      }).catch(function (error) { notify(error.message); });
    }
    if (currentId !== null && currentId !== undefined) addOption(currentId, currentLabel);
    input.addEventListener('input', function () {
      window.clearTimeout(relationTimer); relationTimer = window.setTimeout(function () { loadOptions(input.value); }, 300);
    });
    loadOptions('');
  }
  function openExisting(id) { api('get', {id: id}).then(function (rowData) { state.editing = id; state.version = meta.versionProperty ? rowData[meta.versionProperty] : null; openDrawer(rowData); }).catch(function (error) { notify(error.message); }); }
  function closeAll(force) {
    if (!force && state.dirty && !window.confirm('Perubahan belum disimpan. Tutup form?')) return;
    query('drawer').hidden = true; query('audit').hidden = true; query('import-drawer').hidden = true; query('legacy').hidden = true; query('overlay').hidden = true; state.editing = null; state.version = null; state.dirty = false;
  }
  function save(event) {
    event.preventDefault(); if (state.editing !== null && !meta.canUpdate) return;
    var params = {nui_csrf: meta.csrf}, form = query('form');
    formFields().forEach(function (field) { var input = form.elements[field.property]; if (!input || input.disabled) return; params[field.property] = input.type === 'checkbox' ? (input.checked ? 'true' : 'false') : input.value; });
    var action = state.editing === null ? 'create' : 'update'; if (state.editing !== null) { params.id = state.editing; if (meta.versionProperty) params.version = state.version; }
    api(action, params, 'POST').then(function () { closeAll(true); return loadList(); }).catch(function (error) {
      var box = query('form-error'), messages = []; box.textContent = error.message;
      if (error.payload && error.payload.fieldErrors) Object.keys(error.payload.fieldErrors).forEach(function (key) { messages.push(error.payload.fieldErrors[key]); });
      if (messages.length) box.textContent = messages.join(' • '); box.hidden = false;
    });
  }
  function removeRow(id) { if (window.confirm('Data akan dinonaktifkan, bukan menghapus histori audit. Lanjutkan?')) api('delete', {id: id, nui_csrf: meta.csrf}, 'POST').then(loadList).catch(function (error) { notify(error.message); }); }
  function openAudit(id) {
    query('overlay').hidden = false; query('audit').hidden = false; var list = query('audit-list'); list.textContent = 'Memuat…';
    api('revisions', {id: id, page: 1, pageSize: 25}).then(function (data) {
      list.textContent = ''; if (!data.rows.length) list.appendChild(node('div', {'class': 'gc-empty'}, 'Belum ada histori.'));
      data.rows.forEach(function (revision) { var item = node('div', {'class': 'gc-revision'}); item.appendChild(node('strong', null, 'Revisi ' + revision.revision + ' • ' + revision.type)); item.appendChild(node('span', null, revision.timestamp ? new Date(revision.timestamp).toLocaleString('id-ID') : 'Waktu tidak tersedia')); list.appendChild(item); });
    }).catch(function (error) { list.textContent = error.message; });
  }
  function exportData() { var format = query('export-format').value; window.location.href = url('export_' + format, filterParams()); }
  function readImport(file) {
    if (!file) return; var reader = new FileReader();
    reader.onload = function () {
      var encoded = String(reader.result).split(',')[1] || '';
      api('import_preview', {nui_csrf: meta.csrf, fileName: file.name, fileData: encoded}, 'POST').then(showImport).catch(function (error) { notify(error.message); });
    };
    reader.onerror = function () { notify('Berkas import tidak dapat dibaca.'); }; reader.readAsDataURL(file);
  }
  function showImport(summary) {
    state.importJob = summary.jobKey; var container = query('import-summary'); container.textContent = '';
    [['Status', summary.status], ['Total baris', summary.totalRows], ['Create', summary.createRows], ['Update', summary.updateRows], ['Delete', summary.deleteRows], ['Error', summary.errorRows]].forEach(function (item) { var line = node('div', {'class': 'gc-revision'}); line.appendChild(node('strong', null, item[0])); line.appendChild(node('span', null, String(item[1]))); container.appendChild(line); });
    (summary.errors || []).slice(0, 20).forEach(function (error) { container.appendChild(node('div', {'class': 'gc-alert'}, 'Baris ' + error.row + ': ' + error.message)); });
    query('import-confirm').disabled = summary.errorRows > 0; query('overlay').hidden = false; query('import-drawer').hidden = false;
  }
  function confirmImport() {
    if (!state.importJob || !window.confirm('Jalankan import sesuai hasil dry-run?')) return;
    query('import-confirm').disabled = true;
    api('import_confirm', {nui_csrf: meta.csrf, jobKey: state.importJob}, 'POST').then(function (summary) {
      showImport(summary); if (summary.errorRows) { var link = node('a', {'class': 'gc-btn', href: url('import_errors', {jobKey: state.importJob})}, 'Unduh daftar error'); query('import-summary').appendChild(link); }
      loadList();
    }).catch(function (error) { notify(error.message); query('import-confirm').disabled = false; });
  }

  var timer;
  query('search').addEventListener('input', function (event) { window.clearTimeout(timer); timer = window.setTimeout(function () { state.q = event.target.value; state.page = 1; loadList(); }, 300); });
  query('page-size').addEventListener('change', function (event) { state.pageSize = parseInt(event.target.value, 10); state.page = 1; savePreference(); loadList(); });
  query('prev').addEventListener('click', function () { if (state.page > 1) { state.page--; loadList(); } }); query('next').addEventListener('click', function () { state.page++; loadList(); });
  query('add').addEventListener('click', function () { state.editing = null; openDrawer(null); }); query('export').addEventListener('click', exportData);
  query('template').addEventListener('click', function () { window.location.href = url('import_template'); }); query('import').addEventListener('change', function (event) { readImport(event.target.files[0]); event.target.value = ''; });
  query('filter-toggle').addEventListener('click', function () { query('filter-panel').hidden = !query('filter-panel').hidden; }); query('filter-add').addEventListener('click', addFilter);
  query('columns-toggle').addEventListener('click', function () { query('columns-panel').hidden = !query('columns-panel').hidden; }); query('columns-reset').addEventListener('click', resetPreference);
  query('form').addEventListener('submit', save); query('close').addEventListener('click', function () { closeAll(false); }); query('cancel').addEventListener('click', function () { closeAll(false); });
  query('audit-close').addEventListener('click', function () { closeAll(true); }); query('import-close').addEventListener('click', function () { closeAll(true); }); query('import-cancel').addEventListener('click', function () { closeAll(true); }); query('import-confirm').addEventListener('click', confirmImport);
  query('legacy-close').addEventListener('click', function () { closeAll(true); });
  query('parity-toggle').addEventListener('click', function () { var panel = query('parity-actions'), button = query('parity-toggle'), opened = panel.hidden; panel.hidden = !opened; button.setAttribute('aria-expanded', opened ? 'true' : 'false'); button.textContent = opened ? 'Sembunyikan fungsi' : 'Tampilkan semua fungsi'; });
  query('overlay').addEventListener('click', function () { closeAll(false); }); document.addEventListener('keydown', function (event) { if (event.key === 'Escape' && !query('overlay').hidden) closeAll(false); });
  window.addEventListener('beforeunload', function (event) { if (state.dirty) { event.preventDefault(); event.returnValue = ''; } }); loadMeta().catch(function (error) { notify(error.message); });
}());
