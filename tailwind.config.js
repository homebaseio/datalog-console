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
    },
    borderWidth: {
      DEFAULT: '1px',
      '0': '0',
      '2': '2px',
      '3': '3px',
      '4': '4px',
      '6': '6px',
      '8': '8px',
    }
  },
  variants: {
    extend: {
      backgroundColor: ['even','odd'],
      borderWidth: ['hover'],
    }
  },
  plugins: [
    require('@tailwindcss/line-clamp'),
  ],
}
