import { createI18n } from 'vue-i18n'
import zh from './locales/zh.json'
import en from './locales/en.json'

const savedLocale = typeof localStorage !== 'undefined'
  ? localStorage.getItem('locale') || 'zh'
  : 'zh'

const i18n = createI18n({
  legacy: false,
  locale: savedLocale,
  fallbackLocale: 'zh',
  messages: { zh, en }
})

export default i18n
