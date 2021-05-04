var fs = require('fs');

function deleteFolderRecursive(path) {
  if (fs.existsSync(path) && fs.lstatSync(path).isDirectory()) {
    fs.readdirSync(path).forEach(function(file, index){
      var curPath = path + "/" + file;

      if (fs.lstatSync(curPath).isDirectory()) { // recurse
        deleteFolderRecursive(curPath);
      } else { // delete file
        fs.unlinkSync(curPath);
      }
    });

    console.log(`Deleting directory "${path}"...`);
    fs.rmdirSync(path);
  }
};

console.log("Cleaning working tree...");

deleteFolderRecursive(".shadow-cljs");
deleteFolderRecursive(".node_modules");
deleteFolderRecursive(".cpcache");
deleteFolderRecursive(".calva");
deleteFolderRecursive("shells/chrome/js");
deleteFolderRecursive("shells/chrome/out");
deleteFolderRecursive("public/js");

console.log("Successfully cleaned working tree!");