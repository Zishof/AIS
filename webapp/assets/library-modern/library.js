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

  function fetchJson(url, params, options) {
    var separator = url.indexOf('?') >= 0 ? '&' : '?';
    options = options || {};
    return fetch(url + separator + query(params), {
      method: 'GET',
      credentials: 'same-origin',
      headers: {'Accept': 'application/json'},
      signal: options.signal
    }).then(function (response) {
      if (!response.ok) { var error = new Error('HTTP ' + response.status); error.status = response.status; throw error; }
      return response.json();
    }).then(function (data) {
      if (data && data.ok === false) throw new Error(data.error || 'Permintaan gagal');
      return data;
    });
  }

  function postJson(url, params) {
    return fetch(url, {
      method: 'POST',
      credentials: 'same-origin',
      headers: {'Accept': 'application/json', 'Content-Type': 'application/x-www-form-urlencoded; charset=UTF-8'},
      body: query(params)
    }).then(function (response) {
      if (!response.ok) throw new Error('HTTP ' + response.status);
      return response.json();
    }).then(function (data) {
      if (data && data.ok === false) throw new Error(data.error || 'Permintaan gagal');
      return data;
    });
  }

  global.LibraryModern = {
    escapeHtml: escapeHtml,
    query: query,
    fetchJson: fetchJson,
    postJson: postJson
  };
}(window));
