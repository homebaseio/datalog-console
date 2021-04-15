# Datalog Chrome Extension


## Development

```bash
yarn install
yarn dev
```

### Load the dev extension into the Chrome browser

1. Go to `chrome://extensions/`
2. Turn on **developer mode** in top right
3. Load unpacked `your-file-path-to/datalog-console/shells/chrome`


To view the background page click here

![Datalog Console Extension background page](../../../../docs/datalog-extension.jpg)

To view the datalog panel open the Chrome console with either of these options.
- Right click anywhere on the page -> Inspect
- **Option + ⌘ + J**   (on macOS)
- **Shift + CTRL + J** (on Windows/Linux)

## Connect REPL

To connect to the background page
`yarn shadow-cljs cljs-repl chrome`

To connect to the devtool panel in the chrome console
`yarn shadow-cljs cljs-repl chrome-devtool`