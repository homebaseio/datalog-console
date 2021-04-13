chrome.devtools.panels.create(
  "Datalog DB",
  "favicon.png",
  "inspect-panel.html",
  function (panel) {
    console.log("panel initialized", panel);
  }
);