(function () {
  var lastV = null;
  var wasDown = false;

  function check() {
    fetch('/api/livereload')
      .then(function (r) { return r.json(); })
      .then(function (d) {
        if (lastV === null) {
          lastV = d.v;
        } else if (d.v !== lastV || wasDown) {
          location.reload();
        }
        wasDown = false;
        setTimeout(check, 1000);
      })
      .catch(function () {
        wasDown = true;
        setTimeout(check, 1000);
      });
  }

  check();
})();
