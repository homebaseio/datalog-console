chrome.devtools.panels.create(
  "Datalog DB",
  "favicon.png",
  "devtools-init.html",
  function (panel) {
    console.log("panel initialized", panel);
  }
);