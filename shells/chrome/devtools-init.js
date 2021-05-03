/*
 This code can be used to trigger a stop and start on subscriptions to the database
*/

var port = chrome.runtime.connect({name: "example"})

function handleShown() {
  console.log("panel is being shown");
// port.postMessage({example: "message"});
}

function handleHidden() {
  console.log("panel is being hidden");
}

/*
 end note
*/



chrome.devtools.panels.create(
  "Datalog DB",
  "",
  "inspect-panel.html",
  function (panel) {
    panel.onShown.addListener(handleShown);
    panel.onHidden.addListener(handleHidden);
  }
);