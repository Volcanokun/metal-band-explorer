export interface TagDto {
  name: string
  weight: number
}

export interface ArtistDto {
  id: string
  name: string
  mbid: string | null
  listeners: number | null
  playcount: number | null
  bioSummary: string | null
  tags: TagDto[]
}

export interface SearchResponse {
  query: string
  totalResults: number
  artists: ArtistDto[]
}

export interface SimilarArtistDto {
  id: string
  name: string
  lastfmScore: number | null
  computedScore: number | null
}
