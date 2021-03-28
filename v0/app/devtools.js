chrome.devtools.panels.create(
  "Datalog DB",
  "favicon.png",
  "index.html",
  function (panel) {
    console.log("console panel initialized", panel);
  }
);