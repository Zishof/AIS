(function (global) {
  'use strict';

  function escapeHtml(value) {
    return String(value == null ? '' : value).replace(/[&<>'"]/g, function (char) {
      return {'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[char];
    });
  }

  function query(params) {
    var result = [];
    Object.keys(params || {}).forEach(function (key) {
      var value = params[key];
      if (value !== null && value !== undefined && String(value).trim() !== '') {
        result.push(encodeURIComponent(key) + '=' + encodeURIComponent(String(value)));
      }
    });
    return result.join('&');
  }

  function responseError(response, fallback) {
    var status = response.status;
    var message = status === 401 ? 'Sesi Anda telah berakhir. Silakan masuk kembali.'
      : status === 403 ? 'Anda tidak memiliki hak untuk melakukan tindakan ini.'
      : status === 409 ? 'Data telah berubah atau sedang diproses pengguna lain. Muat ulang lalu coba kembali.'
      : status === 429 ? 'Terlalu banyak permintaan. Tunggu sebentar lalu coba kembali.'
      : status >= 500 ? 'Layanan sedang mengalami gangguan. Silakan coba kembali.'
      : fallback || ('Permintaan gagal (HTTP ' + status + ').');
    var error = new Error(message);
    error.status = status;
    error.requestId = response.headers.get('X-Request-Id') || '';
    return error;
  }

  function parseJson(response) {
    var type = (response.headers.get('Content-Type') || '').toLowerCase();
    if (!response.ok) throw responseError(response);
    if (type.indexOf('application/json') < 0) throw responseError(response, 'Server mengembalikan format respons yang tidak sesuai.');
    return response.json().catch(function () { throw responseError(response, 'Respons server tidak dapat dibaca.'); });
  }

  function notifyRequestError(error) {
    try { global.dispatchEvent(new CustomEvent('library:request-error', {detail: error})); } catch (ignored) {}
    throw error;
  }

  function fetchJson(url, params, options) {
    var separator = url.indexOf('?') >= 0 ? '&' : '?';
    options = options || {};
    return fetch(url + separator + query(params), {
      method: 'GET',
      credentials: 'same-origin',
      headers: {'Accept': 'application/json'},
      signal: options.signal
    }).then(function (response) {
      return parseJson(response);
    }).then(function (data) {
      if (data && data.ok === false) throw new Error(data.error || 'Permintaan gagal');
      return data;
    }).catch(notifyRequestError);
  }

  function postJson(url, params) {
    return fetch(url, {
      method: 'POST',
      credentials: 'same-origin',
      headers: {'Accept': 'application/json', 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'},
      body: query(params)
    }).then(function (response) {
      return parseJson(response);
    }).then(function (data) {
      if (data && data.ok === false) throw new Error(data.error || 'Permintaan gagal');
      return data;
    }).catch(notifyRequestError);
  }

  function setBusy(element, busy) {
    if (!element) return;
    element.setAttribute('aria-busy', busy ? 'true' : 'false');
    if ('disabled' in element) element.disabled = !!busy;
  }

  global.LibraryModern = {
    escapeHtml: escapeHtml,
    query: query,
    fetchJson: fetchJson,
    postJson: postJson,
    setBusy: setBusy
  };
}(window));
