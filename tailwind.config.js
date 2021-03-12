const plugin = require('tailwindcss/plugin')

// NOTE: run `yarn build:tailwind:dev` after editing this file
module.exports = {
  purge: [
    './src/**/*.cljs',
  ],
  darkMode: false, // or 'media' or 'class'
  theme: {
    extend: {},
  },
  variants: {
    extend: {},
  },
  plugins: [
    require('@tailwindcss/line-clamp'),
  ],
}
