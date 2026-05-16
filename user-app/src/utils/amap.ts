interface AmapWxModule {
  AMapWX: new (options: { key: string }) => AmapWxClient
}

interface AmapWxClient {
  getRegeo(options: {
    location?: string
    success: (data: AmapRegeoSdkItem[]) => void
    fail: (error: AmapSdkError) => void
  }): void
  getPoiAround(options: {
    location?: string
    querykeywords?: string
    querytypes?: string
    success: (data: AmapPoiAroundSdkResult) => void
    fail: (error: AmapSdkError) => void
  }): void
}

interface AmapSdkError {
  errCode?: string
  errMsg?: string | unknown
}

interface AmapRegeoSdkItem {
  name?: string
  desc?: string
  longitude?: number | string
  latitude?: number | string
  regeocodeData?: {
    formatted_address?: string
    addressComponent?: Record<string, unknown>
  }
}

interface AmapRawPoi {
  id?: string
  name?: string
  address?: string | string[]
  location?: string
  type?: string
  distance?: string | number
  pname?: string
  cityname?: string
  adname?: string
}

interface AmapPoiAroundSdkResult {
  poisData?: AmapRawPoi[]
}

interface AmapWebLoader {
  load(options: {
    key: string
    version: string
    plugins?: string[]
  }): Promise<AmapWebApi>
}

interface AmapWebApi {
  Map: new (
    container: string | HTMLElement,
    options: { zoom?: number; center?: [number, number] }
  ) => AmapWebMap
  Marker: new (options: { map?: AmapWebMap; position: [number, number] }) => AmapWebMarker
  Geolocation: new (options: Record<string, unknown>) => AmapWebGeolocation
  Geocoder: new (options: Record<string, unknown>) => AmapWebGeocoder
}

interface AmapWebMap {
  setCenter(center: [number, number]): void
  setZoomAndCenter?: (zoom: number, center: [number, number]) => void
  on(event: 'click', handler: (event: AmapWebMapClickEvent) => void): void
  off(event: 'click', handler: (event: AmapWebMapClickEvent) => void): void
  destroy(): void
}

interface AmapWebMarker {
  setPosition(position: [number, number]): void
  setMap(map: AmapWebMap | null): void
}

interface AmapWebMapClickEvent {
  lnglat?: AmapWebLngLat
}

interface AmapWebLngLat {
  getLng?: () => number
  getLat?: () => number
  lng?: number
  lat?: number
}

interface AmapWebGeolocation {
  getCurrentPosition(callback: (status: string, result: AmapWebGeolocationResult) => void): void
}

interface AmapWebGeolocationResult {
  position?: AmapWebLngLat
  info?: string
  message?: string
}

interface AmapWebGeocoder {
  getAddress(
    location: [number, number],
    callback: (status: string, result: AmapWebRegeoResult) => void
  ): void
}

interface AmapWebRegeoResult {
  info?: string
  regeocode?: AmapWebRegeoData
}

interface AmapWebRegeoData {
  formattedAddress?: string
  formatted_address?: string
  addressComponent?: Record<string, unknown>
  pois?: AmapWebPoi[]
}

interface AmapWebPoi {
  id?: string
  name?: string
  address?: string | string[]
  location?: string | [number, number] | AmapWebLngLat
  distance?: string | number
  pname?: string
  cityname?: string
  adname?: string
  district?: string
}

declare global {
  interface Window {
    _AMapSecurityConfig?: {
      securityJsCode?: string
    }
    AMapLoader?: AmapWebLoader
  }
}

export interface LocationCoordinate {
  latitude: number
  longitude: number
}

export interface LocationPoi extends LocationCoordinate {
  id: string
  name: string
  address: string
  distance?: number
}

export interface ReverseGeocodeResult extends LocationCoordinate {
  name: string
  address: string
}

export interface AdministrativeRegionResult extends LocationCoordinate {
  province: string
  city: string
  district: string
  text: string
}

export interface AmapWebMapController {
  setCenter(coordinate: LocationCoordinate): void
  destroy(): void
}

const LOCATION_TEXT_MAX_LENGTH = 100
const AMAP_WEIXIN_KEY = import.meta.env.VITE_AMAP_WEIXIN_KEY || ''
const AMAP_WEB_JS_KEY = import.meta.env.VITE_AMAP_WEB_JS_KEY || ''
const AMAP_WEB_SECURITY_JS_CODE = import.meta.env.VITE_AMAP_WEB_SECURITY_JS_CODE || ''
const AMAP_WEB_LOADER_ID = 'amap-web-jsapi-loader'
const AMAP_WEB_LOADER_URL = 'https://webapi.amap.com/loader.js'

let wxAmapClient: AmapWxClient | null = null
let webAmapPromise: Promise<AmapWebApi> | null = null

declare const require: (path: string) => AmapWxModule

export function isAmapWeixinConfigured() {
  return Boolean(AMAP_WEIXIN_KEY.trim())
}

export function isAmapWebConfigured() {
  return Boolean(AMAP_WEB_JS_KEY.trim() && AMAP_WEB_SECURITY_JS_CODE.trim())
}

export function isAmapConfigured() {
  let configured = false
  // #ifdef MP-WEIXIN
  configured = isAmapWeixinConfigured()
  // #endif
  // #ifdef H5
  configured = isAmapWebConfigured()
  // #endif
  return configured
}

export function normalizeLocationText(value: string) {
  return value.replace(/\s+/g, ' ').trim().slice(0, LOCATION_TEXT_MAX_LENGTH)
}

export function formatPoiLocationText(poi: Pick<LocationPoi, 'name' | 'address'>) {
  return normalizeLocationText(poi.name || poi.address)
}

export function requestUserLocationPermission() {
  // #ifdef MP-WEIXIN
  return requestWechatLocationPermission()
  // #endif
  // #ifdef H5
  return Promise.resolve()
  // #endif
  return Promise.reject(new Error('LOCATION_PLATFORM_UNSUPPORTED'))
}

export function getCurrentCoordinate() {
  // #ifdef H5
  return getWebCurrentCoordinate()
  // #endif
  // #ifdef MP-WEIXIN
  return getWechatCurrentCoordinate()
  // #endif
  return Promise.reject(new Error('LOCATION_PLATFORM_UNSUPPORTED'))
}

export function reverseGeocode(coordinate: LocationCoordinate) {
  // #ifdef H5
  return reverseGeocodeByWeb(coordinate)
  // #endif
  // #ifdef MP-WEIXIN
  return reverseGeocodeByWechat(coordinate)
  // #endif
  return Promise.reject(new Error('LOCATION_PLATFORM_UNSUPPORTED'))
}

export function reverseGeocodeAdministrativeRegion(coordinate: LocationCoordinate) {
  // #ifdef H5
  return reverseGeocodeAdministrativeRegionByWeb(coordinate)
  // #endif
  // #ifdef MP-WEIXIN
  return reverseGeocodeAdministrativeRegionByWechat(coordinate)
  // #endif
  return Promise.reject(new Error('LOCATION_PLATFORM_UNSUPPORTED'))
}

export function searchNearbyPois(coordinate: LocationCoordinate) {
  // #ifdef H5
  return searchNearbyPoisByWeb(coordinate)
  // #endif
  // #ifdef MP-WEIXIN
  return searchNearbyPoisByWechat(coordinate)
  // #endif
  return Promise.reject(new Error('LOCATION_PLATFORM_UNSUPPORTED'))
}

export async function createAmapWebMap(
  containerId: string,
  coordinate: LocationCoordinate,
  onTap: (coordinate: LocationCoordinate) => void | Promise<void>
): Promise<AmapWebMapController> {
  // #ifdef H5
  const AMap = await getWebAmapApi()
  const center = toAmapLocationArray(coordinate)
  const map = new AMap.Map(containerId, { zoom: 16, center })
  const marker = new AMap.Marker({ map, position: center })
  const handleClick = (event: AmapWebMapClickEvent) => {
    const nextCoordinate = parseWebLngLat(event.lnglat)
    if (nextCoordinate) {
      void onTap(nextCoordinate)
    }
  }
  map.on('click', handleClick)

  return {
    setCenter(nextCoordinate) {
      const nextCenter = toAmapLocationArray(nextCoordinate)
      marker.setPosition(nextCenter)
      if (map.setZoomAndCenter) {
        map.setZoomAndCenter(16, nextCenter)
      }
      else {
        map.setCenter(nextCenter)
      }
    },
    destroy() {
      map.off('click', handleClick)
      marker.setMap(null)
      map.destroy()
    },
  }
  // #endif
  return Promise.reject(new Error('LOCATION_PLATFORM_UNSUPPORTED'))
}

function formatAdministrativeRegion(component?: Record<string, unknown>) {
  if (!component) {
    return {
      province: '',
      city: '',
      district: '',
      text: '',
    }
  }
  const province = normalizeLocationText(String(component.province || component.pname || ''))
  const rawCity = component.city || component.cityname || ''
  const cityText = Array.isArray(rawCity) ? rawCity.join('') : String(rawCity)
  const city = normalizeLocationText(cityText)
  const district = normalizeLocationText(String(component.district || component.adname || ''))
  const parts = [province, city, district]
    .filter(Boolean)
    .filter((item, index, array) => array.indexOf(item) === index)
  return {
    province,
    city,
    district,
    text: normalizeLocationText(parts.join(' ')),
  }
}

function requestWechatLocationPermission() {
  return new Promise<void>((resolve, reject) => {
    uni.getSetting({
      success: (setting) => {
        const granted = setting.authSetting?.['scope.userLocation']
        if (granted) {
          resolve()
          return
        }
        uni.authorize({
          scope: 'scope.userLocation',
          success: () => resolve(),
          fail: () => reject(new Error('LOCATION_PERMISSION_DENIED')),
        })
      },
      fail: () => reject(new Error('LOCATION_PERMISSION_CHECK_FAILED')),
    })
  })
}

function getWechatCurrentCoordinate() {
  return new Promise<LocationCoordinate>((resolve, reject) => {
    uni.getLocation({
      type: 'gcj02',
      isHighAccuracy: true,
      success: (location) => {
        resolve({
          latitude: Number(location.latitude),
          longitude: Number(location.longitude),
        })
      },
      fail: () => reject(new Error('LOCATION_FAILED')),
    })
  })
}

function reverseGeocodeByWechat(coordinate: LocationCoordinate) {
  ensureAmapWeixinConfigured()
  return new Promise<ReverseGeocodeResult>((resolve, reject) => {
    getWechatAmapClient().getRegeo({
      location: toAmapLocation(coordinate),
      success: (data) => {
        const item = Array.isArray(data) ? data[0] : undefined
        const longitude = Number(item?.longitude || coordinate.longitude)
        const latitude = Number(item?.latitude || coordinate.latitude)
        const address = normalizeLocationText(item?.regeocodeData?.formatted_address || item?.name || '')
        const name = normalizeLocationText(item?.name || item?.desc || address || '已选择位置')
        resolve({
          latitude: Number.isFinite(latitude) ? latitude : coordinate.latitude,
          longitude: Number.isFinite(longitude) ? longitude : coordinate.longitude,
          name,
          address,
        })
      },
      fail: error => reject(toAmapError(error, 'REVERSE_GEOCODE_FAILED')),
    })
  })
}

function reverseGeocodeAdministrativeRegionByWechat(coordinate: LocationCoordinate) {
  ensureAmapWeixinConfigured()
  return new Promise<AdministrativeRegionResult>((resolve, reject) => {
    getWechatAmapClient().getRegeo({
      location: toAmapLocation(coordinate),
      success: (data) => {
        const item = Array.isArray(data) ? data[0] : undefined
        const region = formatAdministrativeRegion(item?.regeocodeData?.addressComponent)
        resolve({
          ...coordinate,
          ...region,
          text: region.text || normalizeLocationText(item?.regeocodeData?.formatted_address || item?.name || ''),
        })
      },
      fail: error => reject(toAmapError(error, 'REVERSE_GEOCODE_FAILED')),
    })
  })
}

function searchNearbyPoisByWechat(coordinate: LocationCoordinate) {
  ensureAmapWeixinConfigured()
  return new Promise<LocationPoi[]>((resolve, reject) => {
    getWechatAmapClient().getPoiAround({
      location: toAmapLocation(coordinate),
      success: (data) => {
        const pois = (data.poisData || [])
          .map((poi, index) => normalizeWechatPoi(poi, index))
          .filter((poi): poi is LocationPoi => Boolean(poi))
        resolve(pois)
      },
      fail: error => reject(toAmapError(error, 'POI_SEARCH_FAILED')),
    })
  })
}

function ensureAmapWeixinConfigured() {
  if (!isAmapWeixinConfigured()) {
    throw new Error('AMAP_WEIXIN_KEY_MISSING')
  }
}

function getWechatAmapClient() {
  if (!wxAmapClient) {
    // #ifdef MP-WEIXIN
    const amapWxModule = require('../static/libs/amap-wx.130.js')
    wxAmapClient = new amapWxModule.AMapWX({ key: AMAP_WEIXIN_KEY })
    // #endif
  }
  if (!wxAmapClient) {
    throw new Error('LOCATION_PLATFORM_UNSUPPORTED')
  }
  return wxAmapClient
}

function normalizeWechatPoi(poi: AmapRawPoi, index: number): LocationPoi | null {
  const coordinate = parseAmapLocation(poi.location)
  if (!coordinate || !poi.name) return null
  return {
    id: poi.id || `${poi.location || 'poi'}-${index}`,
    name: normalizeLocationText(poi.name),
    address: normalizeWechatAddress(poi),
    distance: normalizeDistance(poi.distance),
    ...coordinate,
  }
}

function normalizeWechatAddress(poi: AmapRawPoi) {
  const address = Array.isArray(poi.address) ? poi.address.join('') : poi.address || ''
  const parts = [poi.pname, poi.cityname, poi.adname, address]
    .filter((item): item is string => Boolean(item))
  return normalizeLocationText(parts.join(' '))
}

async function getWebCurrentCoordinate() {
  const AMap = await getWebAmapApi()
  return new Promise<LocationCoordinate>((resolve, reject) => {
    const geolocation = new AMap.Geolocation({
      enableHighAccuracy: true,
      timeout: 10000,
      convert: true,
      needAddress: true,
      extensions: 'all',
      showButton: false,
      showMarker: false,
      showCircle: false,
      panToLocation: false,
      zoomToAccuracy: false,
      getCityWhenFail: false,
    })

    geolocation.getCurrentPosition((status, result) => {
      const coordinate = parseWebLngLat(result.position)
      if (status === 'complete' && coordinate) {
        resolve(coordinate)
        return
      }
      reject(toLocationError(result.info || result.message))
    })
  })
}

async function reverseGeocodeByWeb(coordinate: LocationCoordinate) {
  const regeocode = await getWebRegeocode(coordinate)
  const pois = regeocode.pois || []
  const firstPoi = pois[0]
  const formattedAddress = normalizeLocationText(regeocode.formattedAddress || regeocode.formatted_address || '')
  const name = normalizeLocationText(firstPoi?.name || formattedAddress || '已选择位置')
  return {
    ...coordinate,
    name,
    address: formattedAddress || normalizeWebPoiAddress(firstPoi),
  }
}

async function reverseGeocodeAdministrativeRegionByWeb(coordinate: LocationCoordinate) {
  const regeocode = await getWebRegeocode(coordinate)
  const region = formatAdministrativeRegion(regeocode.addressComponent)
  return {
    ...coordinate,
    ...region,
    text: region.text || normalizeLocationText(regeocode.formattedAddress || regeocode.formatted_address || ''),
  }
}

async function searchNearbyPoisByWeb(coordinate: LocationCoordinate) {
  const regeocode = await getWebRegeocode(coordinate)
  return (regeocode.pois || [])
    .map((poi, index) => normalizeWebPoi(poi, index))
    .filter((poi): poi is LocationPoi => Boolean(poi))
}

async function getWebRegeocode(coordinate: LocationCoordinate) {
  const AMap = await getWebAmapApi()
  return new Promise<AmapWebRegeoData>((resolve, reject) => {
    const geocoder = new AMap.Geocoder({
      radius: 1000,
      extensions: 'all',
    })

    geocoder.getAddress(toAmapLocationArray(coordinate), (status, result) => {
      if (status === 'complete' && result.info === 'OK' && result.regeocode) {
        resolve(result.regeocode)
        return
      }
      reject(new Error('REVERSE_GEOCODE_FAILED'))
    })
  })
}

function normalizeWebPoi(poi: AmapWebPoi, index: number): LocationPoi | null {
  const coordinate = parseWebPoiLocation(poi.location)
  if (!coordinate || !poi.name) return null
  const fallbackId = `${coordinate.longitude},${coordinate.latitude}-${index}`
  return {
    id: poi.id || fallbackId,
    name: normalizeLocationText(poi.name),
    address: normalizeWebPoiAddress(poi),
    distance: normalizeDistance(poi.distance),
    ...coordinate,
  }
}

function normalizeWebPoiAddress(poi?: AmapWebPoi) {
  if (!poi) return ''
  const address = Array.isArray(poi.address) ? poi.address.join('') : poi.address || ''
  const parts = [poi.pname, poi.cityname, poi.adname, poi.district, address]
    .filter((item): item is string => Boolean(item))
  return normalizeLocationText(parts.join(' '))
}

async function getWebAmapApi() {
  ensureAmapWebConfigured()
  if (!webAmapPromise) {
    webAmapPromise = loadWebAmapApi()
  }
  return webAmapPromise
}

function ensureAmapWebConfigured() {
  if (!isAmapWebConfigured()) {
    throw new Error('AMAP_WEB_KEY_MISSING')
  }
}

async function loadWebAmapApi() {
  if (typeof window === 'undefined' || typeof document === 'undefined') {
    throw new Error('LOCATION_PLATFORM_UNSUPPORTED')
  }
  window._AMapSecurityConfig = {
    ...window._AMapSecurityConfig,
    securityJsCode: AMAP_WEB_SECURITY_JS_CODE,
  }
  await loadAmapLoaderScript()
  if (!window.AMapLoader) {
    throw new Error('AMAP_WEB_LOADER_FAILED')
  }
  return window.AMapLoader.load({
    key: AMAP_WEB_JS_KEY,
    version: '2.0',
    plugins: ['AMap.Geolocation', 'AMap.Geocoder'],
  })
}

function loadAmapLoaderScript() {
  return new Promise<void>((resolve, reject) => {
    if (window.AMapLoader) {
      resolve()
      return
    }

    const existingScript = document.getElementById(AMAP_WEB_LOADER_ID) as HTMLScriptElement | null
    if (existingScript) {
      existingScript.addEventListener('load', () => resolve(), { once: true })
      existingScript.addEventListener('error', () => reject(new Error('AMAP_WEB_LOADER_FAILED')), { once: true })
      return
    }

    const script = document.createElement('script')
    script.id = AMAP_WEB_LOADER_ID
    script.src = AMAP_WEB_LOADER_URL
    script.async = true
    script.onload = () => resolve()
    script.onerror = () => reject(new Error('AMAP_WEB_LOADER_FAILED'))
    document.head.appendChild(script)
  })
}

function parseAmapLocation(location?: string): LocationCoordinate | null {
  if (!location) return null
  const [longitude, latitude] = location.split(',').map(Number)
  if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) return null
  return { latitude, longitude }
}

function parseWebPoiLocation(location?: string | [number, number] | AmapWebLngLat) {
  if (typeof location === 'string') return parseAmapLocation(location)
  if (Array.isArray(location)) {
    const [longitude, latitude] = location.map(Number)
    if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) return null
    return { latitude, longitude }
  }
  return parseWebLngLat(location)
}

function parseWebLngLat(location?: AmapWebLngLat): LocationCoordinate | null {
  if (!location) return null
  const longitude = typeof location.getLng === 'function' ? location.getLng() : Number(location.lng)
  const latitude = typeof location.getLat === 'function' ? location.getLat() : Number(location.lat)
  if (!Number.isFinite(latitude) || !Number.isFinite(longitude)) return null
  return { latitude, longitude }
}

function toAmapLocation(coordinate: LocationCoordinate) {
  return `${coordinate.longitude},${coordinate.latitude}`
}

function toAmapLocationArray(coordinate: LocationCoordinate): [number, number] {
  return [coordinate.longitude, coordinate.latitude]
}

function normalizeDistance(distance?: string | number) {
  if (distance === undefined || distance === null || distance === '') return undefined
  const value = Number(distance)
  return Number.isFinite(value) ? value : undefined
}

function toAmapError(error: AmapSdkError, fallbackMessage: string) {
  const message = typeof error?.errMsg === 'string' ? error.errMsg : fallbackMessage
  return new Error(message || fallbackMessage)
}

function toLocationError(message?: string) {
  if (message === 'PERMISSION_DENIED' || message?.includes('permission')) {
    return new Error('LOCATION_PERMISSION_DENIED')
  }
  if (message === 'TIME_OUT') {
    return new Error('LOCATION_TIMEOUT')
  }
  return new Error('LOCATION_FAILED')
}
