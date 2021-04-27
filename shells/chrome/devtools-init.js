chrome.devtools.panels.create(
  "Datalog DB",
  "",
  "inspect-panel.html",
  function (panel) {
    console.log("panel initialized", panel);
  }
);