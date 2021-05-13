# datalog-console

Administration UI for Datascript, Datahike, and other Datalog databases

## Integrations

- [homebase-react](https://github.com/homebaseio/homebase-react) `>=0.7.0`

## Installation and Usage

Install the extension and visit a url with an Datalog db that has the `datalog-console` integration (such as the `homebase-react` [demo](https://homebaseio.github.io/homebase-react/#!/dev.example.todo)). You will see a green dot appear next to the icon with the following pop up message upon clicking the extension. 

![Extension notification and popup message](docs/datalog-db-detected.png)

Open the Chrome console and look for the Datalog DB tab. Load the database with the button in the top right of the panel and you are ready to go.
![Datalog DB panel open in Chrome console](docs/chrome-panel.png)

### Features

You will find 3 views in the Datalog DB panel.
- [Schema](#schema)
- [Entities](#entities)
- [Entity](#entity)

![View of a loaded db in the Chrome panel](docs/loaded-db.png)

#### Schema

If a schema exists in the database it will use this and in the case of schema on read it will also infer the schema based on contents of the database. 

_An example of Schema inference_.

![Schema inference example](docs/schema.png)

#### Entities

Renders a list of entities found in the database. Clicking on any of these entities renders them in the Entity view.

#### Entity

Directly look up an entity by `id` or `unique attribute`. This renders a tree view of an entity where you can also traverse it's reverse references.


---

## Development

Make sure you have Java and the Clojure CLI [installed](https://clojure.org/guides/getting_started). There are ways to get by without these given this project mostly relies on Javascript, but using the Clojure JVM CLI allows us to resolve dependencies in a more flexible way, which comes in really handy when developing across dependencies.

```bash
yarn install
yarn dev
```
When running for the first time you will want to run Tailwind to generate the styles.

```bash
yarn build:tailwind:dev
```

### Load the dev extension into the Chrome browser

1. Go to `chrome://extensions/`
2. Turn on **developer mode** in top right
3. Load unpacked `your-file-path-to/datalog_console/shells/chrome`

To view the datalog panel open the Chrome console with either of these options.
- Right click anywhere on the page -> Inspect
- **Option + ⌘ + J**   (on macOS)
- **Shift + CTRL + J** (on Windows/Linux)

### Connect the REPL

You can connect a repl to each process by opening new windows in your terminal and doing the following.


- `yarn repl-background` - for the background page
- `yarn repl-panel` - for the devtool panel console

You can also connect your editor connected repl. Selecting build `chrome` gets you background and `chrome-devtool` gets you the panel.

### Runtime environments

There is **3 runtime environments** and 4 environments which both send and receive messages.
- Application environment
- **Content script**
- **Background script**
- **Chrome panel**

The first one you only have access to via postMessage to the window. You also get read and write access to the DOM (used to signal from the external Application environment that a Datalog DB is available). This is all done through `Content script`. Messages between the `Chrome panel` and `Content script` must go through `Background script`.

#### How to find the console of these environments

- Content script is executed in the standard browser Console
- Background is executed in the background. Found in figure (Fig: 1) below 👇
- Chrome panel is available by opening up the chrome console inside of _Datalog DB_ panel tab


(Fig: 1) - To view the background page go to `chrome://extensions/` and click here:

![Datalog Chrome Extension background page](docs/chrome-extension.jpg)


## Load the dev extension into Firefox

1. Go to `about:debugging#/runtime/this-firefox`
2. Click **Load Temporary Add-on...** towards the top right
3. Select any file in `your-file-path-to/datalog_console/shells/chrome`

[Refer to runtime evironments](#runtime-environments) to find where code is executed.


(Fig: 2) - To view the background page go to `about:debugging#/runtime/this-firefox` and click here:

![Datalog Firefox Extension background page](docs/firefox-extension.png)


### Quirks

Execution of code inside of `content-script` will not run on `www.google.com`