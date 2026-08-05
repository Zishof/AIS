<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<script>
  (function () {
    function readBooleanLocalStorage(key) {
      try {
        return JSON.parse(localStorage.getItem(key) || 'false') === true;
      } catch (e) {
        return false;
      }
    }

    function applyFluidLayout() {
      var container = document.querySelector('[data-layout="container"]');
      if (!container) {
        return;
      }

      if (readBooleanLocalStorage('isFluid')) {
        container.classList.remove('container');
        container.classList.add('container-fluid');
      } else {
        container.classList.remove('container-fluid');
        container.classList.add('container');
      }
    }

    if (document.readyState === 'loading') {
      document.addEventListener('DOMContentLoaded', applyFluidLayout);
    } else {
      applyFluidLayout();
    }
  })();
</script>
