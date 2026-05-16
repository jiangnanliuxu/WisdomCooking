export const CHINESE_CUISINES = [
  { code: 'sichuan', label: '川菜', icon: '🌶️' },
  { code: 'cantonese', label: '粤菜', icon: '🥘' },
  { code: 'shandong', label: '鲁菜', icon: '🥣' },
  { code: 'jiangsu', label: '苏菜', icon: '🦐' },
  { code: 'zhejiang', label: '浙菜', icon: '🐟' },
  { code: 'hunan', label: '湘菜', icon: '🔥' },
  { code: 'fujian', label: '闽菜', icon: '🍲' },
  { code: 'anhui', label: '徽菜', icon: '🍄' },
] as const

export const CHINESE_CUISINE_LABELS = CHINESE_CUISINES.map(item => item.label)
