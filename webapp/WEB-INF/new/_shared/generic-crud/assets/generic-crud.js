(function () {
  'use strict';
  var root = document.currentScript && document.currentScript.previousElementSibling;
  if (!root || !root.hasAttribute('data-gc-root')) root = document.querySelector('[data-gc-root]');
  if (!root || root.getAttribute('data-ready') === '1') return;
  root.setAttribute('data-ready', '1');

  var endpoint = root.getAttribute('data-endpoint');
  var meta = null;
  var state = {page: 1, pageSize: 10, q: '', sort: '', direction: 'ASC', filters: [], columns: [], editing: null, version: null, dirty: false, importJob: null, photoFile: null};

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
        if (!response.ok || body.success === false) { var error = new Error((body.code ? body.code + ': ' : '') + (body.message || 'Permintaan gagal')); error.payload = body; throw error; }
        return body.data;
      });
    });
  }
  function notify(message, persistent) {
    var box = query('alert'); box.textContent = message || ''; box.hidden = !message;
    if (message && !persistent) window.setTimeout(function () { box.hidden = true; }, 7000);
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
      meta = data; query('title').textContent = data.menuSelectionLabel || data.displayName; state.pageSize = data.pageSize || 10;
      query('page-size').value = String(state.pageSize); query('add').hidden = !data.canCreate;
      query('template').hidden = !data.importEnabled; query('import-label').hidden = !data.importEnabled;
      var formats = query('export-format'), enabled = {xlsx: data.exportXlsx, pdf: data.exportPdf, docx: data.exportDocx, pptx: data.exportPptx};
      Array.prototype.forEach.call(formats.options, function (option) { option.disabled = !enabled[option.value]; });
      buildParityActions(); buildCustomActions(); renderDashboard();
      return api('preference_load').catch(function () { return null; }).then(function (preference) {
        state.columns = preference && preference.columns ? preference.columns : defaultColumns();
        if (preference && preference.pageSize) { state.pageSize = preference.pageSize; query('page-size').value = String(state.pageSize); }
        if (preference && preference.sort) { state.sort = preference.sort.property || ''; state.direction = preference.sort.direction || 'ASC'; }
        if (preference && preference.filters) state.filters = preference.filters;
        buildFilterFields(); buildColumnChooser(); renderFilters(); buildHeader(); return loadList();
      });
    });
  }
  function buildCustomActions() {
    var container = query('custom-actions'); container.textContent = '';
    (meta.customActions || []).forEach(function (action) {
      if (action.enabled === false) return;
      var button = node('button', {type: 'button', 'class': 'gc-btn' + (action.dangerous ? ' gc-btn-danger' : '')}, action.label || action.actionKey);
      button.addEventListener('click', function () {
        if (action.confirmation && !window.confirm(action.confirmation)) return;
        button.disabled = true;
        api('custom_action', {nui_csrf: meta.csrf, actionKey: action.actionKey}, 'POST').then(function () {
          notify('Aksi berhasil dijalankan.'); return loadList();
        }).then(loadMetaDashboard).catch(function (error) { notify(error.message, true); }).then(function () { button.disabled = false; });
      }); container.appendChild(button);
    });
  }
  function loadMetaDashboard() { return api('meta').then(function (data) { meta.dashboard = data.dashboard; renderDashboard(); }); }
  function renderDashboard() {
    var box = query('dashboard'), dashboard = meta.dashboard, max = 1; box.textContent = '';
    if (!dashboard) { box.hidden = true; return; } box.hidden = false;
    var head = node('header', {'class': 'gc-dashboard-head'}); head.appendChild(node('h2', null, dashboard.title || 'Dashboard')); head.appendChild(node('p', null, dashboard.description || '')); box.appendChild(head);
    var cards = node('div', {'class': 'gc-kpis'}); (dashboard.kpis || []).forEach(function (item) { var card = node('article', {'class': 'gc-kpi'}); card.appendChild(node('strong', null, item.label)); card.appendChild(node('span', null, String(item.value) + (item.unit ? ' ' + item.unit : ''))); cards.appendChild(card); }); box.appendChild(cards);
    (dashboard.trend || []).forEach(function (point) { max = Math.max(max, Number(point.value || 0)); });
    if ((dashboard.trend || []).length) { var trend = node('div', {'class': 'gc-trend', title: 'Tren pemakaian memori'}); dashboard.trend.forEach(function (point) { trend.appendChild(node('i', {title: point.label + ': ' + point.value + ' MB', style: 'height:' + Math.max(2, Math.round(Number(point.value || 0) * 100 / max)) + '%'})); }); box.appendChild(trend); }
    if ((dashboard.recent || []).length) { var wrap = node('div', {'class': 'gc-dashboard-table'}), table = node('table'), thead = node('tr'); ['ID','Waktu','Maks MB','Alokasi MB','Bebas MB'].forEach(function (label) { thead.appendChild(node('th', null, label)); }); table.appendChild(thead); dashboard.recent.forEach(function (item) { var row = node('tr'); [item.id, item.timestamp ? new Date(item.timestamp).toLocaleString('id-ID') : '', item.maxMb, item.allocatedMb, item.freeMb].forEach(function (value) { row.appendChild(node('td', null, String(value))); }); table.appendChild(row); }); wrap.appendChild(table); box.appendChild(wrap); }
  }
  function buildParityActions() {
    var actions = meta.formActions || [], container = query('parity-actions'), groups = {}, nativeCount = 0, panelCount = 0;
    if (meta.menuSelectionAction && meta.menuSelectionAction !== 'master' && meta.menuSelectionAction !== 'group') {
      actions = actions.filter(function (action) { return action.sourceHandler === meta.menuSelectionAction || action.actionKey === meta.menuSelectionAction; });
    } else if (meta.menuSelectionGroup && meta.menuSelectionAction === 'group') {
      actions = actions.filter(function (action) { return action.group === meta.menuSelectionGroup; });
    }
    if (!actions.length) return;
    query('parity').hidden = false; container.textContent = '';
    actions.forEach(function (action) {
      var group = action.group || 'Lainnya';
      if (!groups[group]) groups[group] = [];
      groups[group].push(action);
      if (action.implementationStatus === 'NEW_UI_NATIVE') nativeCount++; else if (action.implementationStatus === 'NEW_UI_NATIVE_ROUTE') panelCount++;
    });
    query('parity-summary').textContent = nativeCount + ' fungsi CRUD aktif • ' + panelCount + ' panel native New UI';
    Object.keys(groups).forEach(function (groupName) {
      var group = node('section', {'class': 'gc-parity-group'}), title = node('h3', null, groupName), list = node('div', {'class': 'gc-parity-grid'});
      group.appendChild(title);
      groups[groupName].forEach(function (action) {
        var card = node('article', {'class': 'gc-parity-action'}), top = node('div'), label = node('strong', null, action.label), status;
        top.appendChild(label); card.appendChild(top);
        card.appendChild(node('small', null, 'Service: ' + (action.sourceHandler || action.sourceAction || 'New UI')));
        if (action.implementationStatus === 'NEW_UI_NATIVE') {
          status = node('span', {'class': 'gc-parity-status native'}, 'New UI aktif'); card.appendChild(status);
        } else if (action.implementationStatus === 'NEW_UI_NATIVE_ROUTE') {
          status = node('button', {type: 'button', 'class': 'gc-btn gc-parity-open'}, 'Buka halaman New UI');
          status.addEventListener('click', function () { openNativePanel(action); }); card.appendChild(status);
        } else {
          card.appendChild(node('span', {'class': 'gc-parity-status blocked'}, 'Belum dipetakan'));
        }
        list.appendChild(card);
      });
      group.appendChild(list); container.appendChild(group);
    });
  }
  function openNativePanel(action) {
    action = action || {};
    if (action.nativeSubroute) {
      var target = new URL(window.location.href);
      target.searchParams.set('frame', '1');
      target.searchParams.set('nativeSubroute', action.nativeSubroute);
      window.location.href = target.toString();
      return;
    }
    var body = query('native-panel-body'), title = action.label || 'Fungsi Mahasiswa';
    query('native-panel-title').textContent = title; body.textContent = '';
    body.appendChild(node('p', {'class': 'gc-native-panel-intro'}, 'Panel ini merupakan UI New sendiri dan tidak membuka iframe, redirect, atau tampilan aplikasi lain.'));
    var details = node('div', {'class': 'gc-native-panel-details'});
    [['Kelompok', action.group || 'Data Mahasiswa'], ['Hak akses', action.requiredPrivilege || 'READ'], ['Service handler', action.sourceHandler || action.actionKey || action.tabKey || 'New UI service']].forEach(function (item) {
      var row = node('div', {'class': 'gc-revision'}); row.appendChild(node('strong', null, item[0])); row.appendChild(node('span', null, item[1])); details.appendChild(row);
    });
    body.appendChild(details);
    var tools = node('div', {'class': 'gc-native-panel-tools'}), search = node('input', {type: 'search', placeholder: 'Cari data dalam ' + title + '…', 'aria-label': 'Cari ' + title});
    tools.appendChild(search);
    if (/upload|import/i.test(action.actionKey || action.tabKey || '')) tools.appendChild(node('input', {type: 'file', 'aria-label': 'Pilih berkas'}));
    var refresh = node('button', {type: 'button', 'class': 'gc-btn gc-primary'}, 'Terapkan');
    refresh.addEventListener('click', function () { state.q = search.value || state.q; query('search').value = state.q; closeAll(true); loadList(); });
    tools.appendChild(refresh); body.appendChild(tools);
    query('overlay').hidden = false; query('native-panel').hidden = false;
  }
  function buildHeader() {
    var row = node('tr');
    if (meta.photoEnabled) row.appendChild(node('th', null, 'Foto'));
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
      var empty = node('tr'); empty.appendChild(node('td', {colspan: String(fields.length + 1 + (meta.photoEnabled ? 1 : 0)), 'class': 'gc-empty'}, 'Tidak ada data yang cocok.')); body.appendChild(empty);
    } else data.rows.forEach(function (rowData) {
      var row = node('tr');
      if (meta.photoEnabled) row.appendChild(photoCell(rowData));
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
      if (meta.approvalEnabled && meta.canApprove) {
        var approvalAction = rowData.status ? 'unapprove' : 'approve';
        var approvalLabel = rowData.status ? 'Batalkan Persetujuan' : 'Setujui';
        var approval = node('button', {type: 'button', 'class': 'gc-btn'}, approvalLabel);
        approval.addEventListener('click', function () { changeApproval(rowId, approvalAction, approvalLabel); });
        buttons.appendChild(approval);
      }
      if (meta.canDelete) { var remove = node('button', {type: 'button', 'class': 'gc-btn'}, 'Nonaktifkan'); remove.addEventListener('click', function () { removeRow(rowId); }); buttons.appendChild(remove); }
      actions.appendChild(buttons); row.appendChild(actions); body.appendChild(row);
    });
    query('status').textContent = data.total + ' data'; query('page-info').textContent = 'Halaman ' + data.page + ' dari ' + Math.max(1, data.pageCount);
    query('prev').disabled = data.page <= 1; query('next').disabled = data.page >= data.pageCount; state.page = data.page;
  }
  function photoUrl(rowData) {
    if (!meta.photoUrlTemplate || !rowData) return '';
    return meta.photoUrlTemplate.replace('{nim}', encodeURIComponent(rowData.nim || '')).replace('{id}', encodeURIComponent(rowData[meta.identifierProperty] || ''));
  }
  function initials(name) {
    var words = String(name || 'M').trim().split(/\s+/), value = words[0] ? words[0].charAt(0) : 'M';
    if (words.length > 1) value += words[words.length - 1].charAt(0); return value.toUpperCase();
  }
  function photoCell(rowData) {
    var cell = node('td', {'data-label': 'Foto'}), frame = node('span', {'class': 'gc-photo-frame'}), fallback = node('span', {'class': 'gc-photo-fallback'}, initials(rowData && rowData.nama));
    var image = node('img', {'class': 'gc-photo', alt: 'Foto ' + ((rowData && rowData.nama) || 'mahasiswa'), loading: 'lazy', src: photoUrl(rowData)});
    image.addEventListener('load', function () { fallback.hidden = true; }); image.addEventListener('error', function () { image.hidden = true; fallback.hidden = false; });
    frame.appendChild(image); frame.appendChild(fallback); cell.appendChild(frame); return cell;
  }
  function formFields() { return (meta.fields || []).filter(function (field) { return state.editing === null ? field.createable : (field.updateable || (!meta.canUpdate && field.detailVisible)); }); }
  function openDrawer(rowData) {
    state.dirty = false; state.photoFile = null; var editing = state.editing !== null, container = query('fields'); container.textContent = '';
    query('form-title').textContent = (editing ? (meta.canUpdate ? 'Ubah ' : 'Detail ') : 'Tambah ') + meta.displayName; query('form-mode').textContent = meta.formMode;
    buildFormTabs(container);
    if (meta.photoEnabled) buildPhotoEditor(container, rowData, editing);
    if (meta.attachmentEnabled && editing) buildAttachmentEditor(container, rowData);
    formFields().forEach(function (field) {
      var wrap = node('div', {'class': 'gc-field' + (field.editorType === 'textarea' ? ' gc-field-wide' : '')}), label = node('label', {'for': 'gc-' + field.property}, field.label + (field.required ? ' *' : '')), input, relationUi = null;
      if (field.editorType === 'textarea') input = node('textarea', {rows: '4'});
      else if (field.editorType === 'checkbox') input = node('input', {type: 'checkbox'});
      else if (field.editorType === 'select') {
        input = node('select'); input.appendChild(node('option', {value: ''}, '— Pilih —'));
        (field.enumValues || []).forEach(function (value) { input.appendChild(node('option', {value: value}, value)); });
      } else if (field.editorType === 'relation') {
        input = node('input', {type: 'hidden', 'data-gc-relation-value': field.property});
        var relationShell = node('div', {'class': 'gc-relation-picker', 'data-relation-property': field.property});
        var relationControl = node('div', {'class': 'gc-relation-control'});
        var relationSearch = node('input', {type: 'search', autocomplete: 'off', role: 'combobox',
          'aria-autocomplete': 'list', 'aria-expanded': 'false', placeholder: 'Cari dan pilih ' + field.label + '…',
          'data-gc-relation-display': field.property});
        var relationToggle = node('button', {type: 'button', 'class': 'gc-relation-toggle',
          'aria-label': 'Buka pilihan ' + field.label}, '⌄');
        var relationPanel = node('div', {'class': 'gc-relation-panel', role: 'listbox', hidden: 'hidden'});
        var relationResults = node('div', {'class': 'gc-relation-results'});
        var relationStatus = node('small', {'class': 'gc-relation-status'}, 'Ketik untuk mencari pilihan.');
        relationControl.appendChild(relationSearch); relationControl.appendChild(relationToggle);
        relationPanel.appendChild(relationResults); relationPanel.appendChild(relationStatus);
        relationShell.appendChild(input); relationShell.appendChild(relationControl); relationShell.appendChild(relationPanel);
        relationUi = {shell: relationShell, search: relationSearch, toggle: relationToggle,
          panel: relationPanel, results: relationResults, status: relationStatus};
      } else input = node('input', {type: ['number', 'date', 'time', 'datetime-local'].indexOf(field.editorType) >= 0 ? field.editorType : 'text'});
      input.id = 'gc-' + field.property; input.name = field.property;
      input.required = !!field.required;
      if (rowData && rowData[field.property] !== null && rowData[field.property] !== undefined) {
        if (input.type === 'checkbox') input.checked = !!rowData[field.property];
        else if (input.type === 'date') { var date = new Date(rowData[field.property]); if (!isNaN(date.getTime())) input.value = date.getFullYear() + '-' + String(date.getMonth() + 1).replace(/^(\d)$/, '0$1') + '-' + String(date.getDate()).replace(/^(\d)$/, '0$1'); }
        else if (input.type === 'datetime-local') { var timestamp = new Date(rowData[field.property]); if (!isNaN(timestamp.getTime())) input.value = timestamp.getFullYear() + '-' + String(timestamp.getMonth() + 1).replace(/^(\d)$/, '0$1') + '-' + String(timestamp.getDate()).replace(/^(\d)$/, '0$1') + 'T' + String(timestamp.getHours()).replace(/^(\d)$/, '0$1') + ':' + String(timestamp.getMinutes()).replace(/^(\d)$/, '0$1'); }
        else input.value = String(rowData[field.property]);
      }
      if (relationUi && rowData && rowData[field.property] !== null && rowData[field.property] !== undefined) {
        relationUi.search.value = String(rowData[field.property + '__label'] || rowData[field.property]);
      }
      if (relationUi) {
        relationUi.search.id = 'gc-' + field.property + '-display'; label.setAttribute('for', relationUi.search.id);
        relationUi.search.required = !!field.required;
      }
      if (editing && !meta.canUpdate) { input.disabled = true; if (relationUi) { relationUi.search.disabled = true; relationUi.toggle.disabled = true; } }
      if (!relationUi) input.addEventListener('input', function () { state.dirty = true; });
      wrap.appendChild(label); wrap.appendChild(relationUi ? relationUi.shell : input); container.appendChild(wrap);
      if (relationUi && !input.disabled) bindRelationLookup(field, input, relationUi, rowData);
    });
    query('form-error').hidden = true; query('overlay').hidden = false; query('drawer').hidden = false; var first = container.querySelector('input,textarea,select'); if (first) first.focus();
  }
  function buildFormTabs(container) {
    var tabs = meta.formDefinition && meta.formDefinition.tabs ? meta.formDefinition.tabs : [];
    if (!tabs.length) return;
    var wrap = node('nav', {'class': 'gc-form-tabs', 'aria-label': 'Bagian data mahasiswa'});
    tabs.forEach(function (tab, index) {
      var button = node('button', {type: 'button', 'class': 'gc-btn' + (index === 0 ? ' gc-primary' : '')}, tab.label);
      if (index > 0) button.addEventListener('click', function () { openNativePanel({label: tab.label, tabKey: tab.key, group: 'Form Mahasiswa', requiredPrivilege: 'UPDATE', sourceHandler: tab.sourceAction}); });
      wrap.appendChild(button);
    });
    container.appendChild(wrap);
  }
  function buildPhotoEditor(container, rowData, editing) {
    var wrap = node('div', {'class': 'gc-field gc-field-wide gc-photo-editor'}), label = node('label', null, 'Foto Mahasiswa'), body = node('div', {'class': 'gc-photo-editor-body'});
    var preview = photoCell(rowData || {nama: 'Mahasiswa', nim: ''}).firstChild, input = node('input', {type: 'file', accept: 'image/jpeg,image/png,image/webp'});
    input.addEventListener('change', function () { state.photoFile = input.files && input.files[0] ? input.files[0] : null; state.dirty = !!state.photoFile || state.dirty; if (state.photoFile) { var image = preview.querySelector('img'); image.hidden = false; image.src = URL.createObjectURL(state.photoFile); } });
    body.appendChild(preview); body.appendChild(input);
    if (editing && meta.canUpdate) { var remove = node('button', {type: 'button', 'class': 'gc-btn'}, 'Hapus Foto'); remove.addEventListener('click', function () { if (window.confirm('Hapus foto mahasiswa ini?')) removePhoto(state.editing); }); body.appendChild(remove); }
    wrap.appendChild(label); wrap.appendChild(body); container.appendChild(wrap);
  }
  function buildAttachmentEditor(container, rowData) {
    var wrap = node('section', {'class': 'gc-attachments'}), head = node('div', {'class': 'gc-attachment-head'}), list = node('div', {'class': 'gc-attachment-list'}, 'Memuat lampiran…');
    head.appendChild(node('strong', null, 'Lampiran (PDF/JPG/PNG, maks. 20 MB)'));
    if (meta.canUpdate) {
      var label = node('label', {'class': 'gc-btn gc-file'}, '+ Tambah Lampiran'), input = node('input', {type: 'file', accept: 'application/pdf,image/jpeg,image/png'});
      input.addEventListener('change', function () { var file = input.files && input.files[0]; if (!file) return; uploadAttachment(state.editing, file).then(function () { input.value = ''; loadAttachments(state.editing, list); }).catch(function (error) { notify(error.message); input.value = ''; }); });
      label.appendChild(input); head.appendChild(label);
    }
    wrap.appendChild(head); wrap.appendChild(list); container.appendChild(wrap); loadAttachments(rowData[meta.identifierProperty], list);
  }
  function loadAttachments(ownerId, list) {
    list.textContent = 'Memuat lampiran…';
    return api('attachment_list', {id: ownerId}).then(function (items) {
      list.textContent = '';
      if (!items || !items.length) { list.appendChild(node('div', {'class': 'gc-empty'}, 'Belum ada lampiran.')); return; }
      items.forEach(function (item) {
        var row = node('div', {'class': 'gc-attachment-row'}), info = node('span'), actions = node('div', {'class': 'gc-attachment-actions'});
        info.appendChild(node('strong', null, item.name || 'Lampiran')); info.appendChild(document.createTextNode(' · ' + (item.contentType || 'file')));
        var download = node('a', {'class': 'gc-btn', href: url('attachment_download', {attachmentId: item.id})}, 'Unduh'); actions.appendChild(download);
        if (meta.canUpdate) { var remove = node('button', {type: 'button', 'class': 'gc-btn'}, 'Hapus'); remove.addEventListener('click', function () { if (window.confirm('Hapus lampiran ini?')) api('attachment_delete', {attachmentId: item.id, nui_csrf: meta.csrf}, 'POST').then(function () { return loadAttachments(ownerId, list); }).catch(function (error) { notify(error.message); }); }); actions.appendChild(remove); }
        row.appendChild(info); row.appendChild(actions); list.appendChild(row);
      });
    }).catch(function (error) { list.textContent = error.message; });
  }
  function uploadAttachment(ownerId, file) {
    var data = new FormData(); data.append('attachment', file, file.name);
    var target = endpoint + (endpoint.indexOf('?') < 0 ? '?' : '&') + pairs({action: 'attachment_upload', id: ownerId, nui_csrf: meta.csrf});
    return fetch(target, {method: 'POST', credentials: 'same-origin', headers: {'Accept': 'application/json'}, body: data}).then(function (response) { return response.json().then(function (body) { if (!response.ok || body.success === false) { var error = new Error(body.message || 'Upload lampiran gagal.'); error.payload = body; throw error; } return body.data; }); });
  }
  function bindRelationLookup(field, valueInput, picker, rowData) {
    var relationTimer = null, requestNumber = 0, loaded = false;
    var threshold = Math.max(1, Math.min(parseInt(meta.lookupThreshold || 50, 10), 50));
    var currentId = rowData ? rowData[field.property] : null;
    var currentLabel = rowData ? rowData[field.property + '__label'] : null;
    var selectedLabel = currentId === null || currentId === undefined ? '' : String(currentLabel || currentId);
    if (currentId !== null && currentId !== undefined) valueInput.value = String(currentId);
    picker.search.value = selectedLabel;

    function setOpen(open) {
      picker.panel.hidden = !open; picker.search.setAttribute('aria-expanded', open ? 'true' : 'false');
      picker.shell.classList.toggle('open', open);
    }
    function selectItem(item) {
      valueInput.value = item && item.id !== null && item.id !== undefined ? String(item.id) : '';
      selectedLabel = item ? String(item.label || item.id) : '';
      picker.search.value = selectedLabel; picker.search.setCustomValidity('');
      state.dirty = true; setOpen(false);
    }
    function renderOptions(data) {
      var items = data.items || [], total = parseInt(data.total || 0, 10), identifier = data.identifierProperty || 'id';
      picker.results.textContent = '';
      if (!field.required) {
        var clear = node('button', {type: 'button', 'class': 'gc-relation-option clear'}, '— Kosongkan pilihan —');
        clear.addEventListener('click', function () { selectItem(null); }); picker.results.appendChild(clear);
      }
      items.forEach(function (item) {
        var option = node('button', {type: 'button', 'class': 'gc-relation-option', role: 'option'});
        option.appendChild(node('strong', null, String(item.label || item.id)));
        if (String(item.label || '') !== String(item.id)) option.appendChild(node('span', null, identifier + ': ' + item.id));
        option.addEventListener('click', function () { selectItem(item); }); picker.results.appendChild(option);
      });
      if (!items.length) picker.results.appendChild(node('div', {'class': 'gc-relation-empty'}, 'Pilihan tidak ditemukan.'));
      var large = total > threshold; picker.shell.classList.toggle('gc-relation-large', large);
      picker.status.textContent = large
        ? total + ' data tersedia — ketik nama/kode/ID untuk mempersempit pencarian.'
        : total + ' pilihan tersedia.';
      loaded = true; setOpen(true);
    }
    function loadOptions(search) {
      var sequence = ++requestNumber; picker.status.textContent = 'Memuat pilihan…'; setOpen(true);
      api('relation_lookup', {field: field.property, q: search || '', page: 1, pageSize: threshold}).then(function (data) {
        if (sequence === requestNumber) renderOptions(data);
      }).catch(function (error) { if (sequence === requestNumber) { picker.status.textContent = error.message; notify(error.message); } });
    }
    picker.search.addEventListener('focus', function () { if (!loaded) loadOptions(''); else setOpen(true); });
    picker.search.addEventListener('input', function () {
      state.dirty = true;
      if (picker.search.value !== selectedLabel) valueInput.value = '';
      picker.search.setCustomValidity(''); window.clearTimeout(relationTimer);
      relationTimer = window.setTimeout(function () { loadOptions(picker.search.value); }, 300);
    });
    picker.search.addEventListener('keydown', function (event) {
      if (event.key === 'Escape') { setOpen(false); return; }
      if (event.key === 'ArrowDown') {
        var first = picker.results.querySelector('.gc-relation-option:not(.clear)');
        if (first) { event.preventDefault(); first.focus(); }
      }
    });
    picker.search.addEventListener('blur', function (event) {
      var next = event.relatedTarget;
      if (!next || !picker.shell.contains(next)) setOpen(false);
    });
    picker.toggle.addEventListener('click', function () {
      if (picker.panel.hidden) { picker.search.focus(); if (!loaded) loadOptions(''); else setOpen(true); }
      else setOpen(false);
    });
  }
  function openExisting(id) { api('get', {id: id}).then(function (rowData) { state.editing = id; state.version = meta.versionProperty ? rowData[meta.versionProperty] : null; openDrawer(rowData); }).catch(function (error) { notify(error.message); }); }
  function closeAll(force) {
    if (!force && state.dirty && !window.confirm('Perubahan belum disimpan. Tutup form?')) return;
    query('drawer').hidden = true; query('audit').hidden = true; query('import-drawer').hidden = true; query('native-panel').hidden = true; query('overlay').hidden = true; state.editing = null; state.version = null; state.dirty = false;
  }
  function save(event) {
    event.preventDefault(); if (state.editing !== null && !meta.canUpdate) return;
    var params = {nui_csrf: meta.csrf}, form = query('form'), relationErrors = [], firstInvalid = null;
    if (!form.checkValidity()) { if (form.reportValidity) form.reportValidity(); return; }
    formFields().forEach(function (field) {
      var input = form.elements[field.property]; if (!input || input.disabled) return;
      if (field.editorType === 'relation') {
        var display = input.parentNode.querySelector('[data-gc-relation-display]');
        var typed = display ? display.value.replace(/^\s+|\s+$/g, '') : '';
        if ((field.required || typed.length > 0) && !input.value) {
          relationErrors.push(field.label + ': pilih satu data dari daftar hasil pencarian.');
          if (display) { display.setCustomValidity('Pilih data dari daftar hasil pencarian.'); if (!firstInvalid) firstInvalid = display; }
          return;
        }
      }
      params[field.property] = input.type === 'checkbox' ? (input.checked ? 'true' : 'false') : input.value;
    });
    if (relationErrors.length) {
      var invalidBox = query('form-error'); invalidBox.textContent = relationErrors.join(' • '); invalidBox.hidden = false;
      if (firstInvalid) { firstInvalid.focus(); if (firstInvalid.reportValidity) firstInvalid.reportValidity(); }
      return;
    }
    var action = state.editing === null ? 'create' : 'update'; if (state.editing !== null) { params.id = state.editing; if (meta.versionProperty) params.version = state.version; }
    api(action, params, 'POST').then(function (result) { var id = state.editing !== null ? state.editing : (result && result.id); return state.photoFile ? uploadPhoto(id, state.photoFile) : null; }).then(function () { closeAll(true); return loadList(); }).catch(function (error) {
      var box = query('form-error'), messages = []; box.textContent = error.message;
      if (error.payload && error.payload.fieldErrors) Object.keys(error.payload.fieldErrors).forEach(function (key) { messages.push(error.payload.fieldErrors[key]); });
      if (messages.length) box.textContent = messages.join(' • '); box.hidden = false;
    });
  }
  function uploadPhoto(id, file) {
    var data = new FormData(); data.append('photo', file, file.name);
    var target = endpoint + (endpoint.indexOf('?') < 0 ? '?' : '&') + pairs({action: 'photo_upload', id: id, nui_csrf: meta.csrf});
    return fetch(target, {method: 'POST', credentials: 'same-origin', headers: {'Accept': 'application/json'}, body: data}).then(function (response) { return response.json().then(function (body) { if (!response.ok || body.success === false) throw new Error(body.message || 'Upload foto gagal.'); return body.data; }); });
  }
  function removePhoto(id) { api('photo_delete', {id: id, nui_csrf: meta.csrf, reason: 'Dihapus melalui form Mahasiswa New UI'}, 'POST').then(function () { notify('Foto berhasil dihapus.'); openExisting(id); loadList(); }).catch(function (error) { notify(error.message); }); }
  function changeApproval(id, action, label) { if (window.confirm(label + ' data ini?')) api(action, {id: id, nui_csrf: meta.csrf, reason: label + ' melalui New UI'}, 'POST').then(function (result) { notify(result && result.message ? result.message : 'Status persetujuan diperbarui.'); return loadList(); }).catch(function (error) { notify(error.message); }); }
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
  query('native-panel-close').addEventListener('click', function () { closeAll(true); });
  query('parity-toggle').addEventListener('click', function () { var panel = query('parity-actions'), button = query('parity-toggle'), opened = panel.hidden; panel.hidden = !opened; button.setAttribute('aria-expanded', opened ? 'true' : 'false'); button.textContent = opened ? 'Sembunyikan fungsi' : 'Tampilkan semua fungsi'; });
  query('overlay').addEventListener('click', function () { closeAll(false); }); document.addEventListener('keydown', function (event) { if (event.key === 'Escape' && !query('overlay').hidden) closeAll(false); });
  window.addEventListener('beforeunload', function (event) { if (state.dirty) { event.preventDefault(); event.returnValue = ''; } }); loadMeta().catch(function (error) { query('title').textContent = 'Gagal memuat data'; query('status').textContent = 'Gagal'; notify(error.message, true); });
}());
