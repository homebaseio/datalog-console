const plugin = require('tailwindcss/plugin')

// NOTE: run `yarn build:tailwind:dev` after editing this file
module.exports = {
  purge: [
    './src/**/*.cljs',
  ],
  darkMode: false, // or 'media' or 'class'
  theme: {
    minWidth: {
      '0': '0',
      '1/4': '25%',
      '1/2': '50%',
      '3/4': '75%',
      'full': '100%',
    }
  },
  variants: {
    extend: {
      backgroundColor: ['even','odd'],
    }
  },
  plugins: [
    require('@tailwindcss/line-clamp'),
  ],
}
