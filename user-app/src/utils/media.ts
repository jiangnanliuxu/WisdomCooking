import { composeApiUrl } from './request'

export function resolveAssetUrl(url?: string) {
  if (!url) return ''
  if (/^(https?:)?\/\//.test(url) || url.startsWith('data:') || url.startsWith('blob:')) {
    return url
  }
  return composeApiUrl(url.startsWith('/') ? url : `/${url}`)
}

export function mediaIdToRawUrl(mediaId?: number | null) {
  if (!mediaId) return ''
  return composeApiUrl(`/api/v1/uploads/${mediaId}/raw`)
}

export function mediaIdsToUrls(mediaIds?: Array<number | null | undefined>) {
  return (mediaIds || []).map(mediaId => mediaIdToRawUrl(mediaId)).filter(Boolean)
}
