import axios from 'axios'
import type { SearchResponse, TagDto, SimilarArtistDto } from '@/types/artist'

const SESSION_KEY = 'metal-explorer-session-id'

function getSessionId(): string {
  let id = sessionStorage.getItem(SESSION_KEY)
  if (!id) {
    id = crypto.randomUUID()
    sessionStorage.setItem(SESSION_KEY, id)
  }
  return id
}

const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? '',
  headers: { 'Content-Type': 'application/json' },
})

http.interceptors.request.use((config) => {
  config.headers['X-Session-Id'] = getSessionId()
  return config
})

export async function searchArtists(query: string): Promise<SearchResponse> {
  const { data } = await http.get<SearchResponse>('/api/bands/search', {
    params: { q: query },
  })
  return data
}

export async function getArtistTags(artistId: string): Promise<TagDto[]> {
  const { data } = await http.get<TagDto[]>(`/api/bands/${artistId}/tags`)
  return data
}

export async function getSimilarArtists(artistId: string): Promise<SimilarArtistDto[]> {
  const { data } = await http.get<SimilarArtistDto[]>(`/api/bands/${artistId}/similar`)
  return data
}
