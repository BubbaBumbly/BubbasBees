// Bee filter on the bees page: matches name, tier, home dimension, traits and flowers.
(function () {
  var box = document.getElementById("beefilter");
  if (!box) return;
  var bees = document.querySelectorAll(".bee");
  box.addEventListener("input", function () {
    var words = box.value.toLowerCase().split(/\s+/).filter(Boolean);
    bees.forEach(function (b) {
      var text = b.getAttribute("data-search");
      b.style.display = words.every(function (w) { return text.indexOf(w) >= 0; }) ? "" : "none";
    });
  });
})();
