import { useQuery } from '@tanstack/react-query'
import { getArtistTags, getSimilarArtists } from '@/api/client'

export function useArtistDetail(artistId: string) {
  const tags = useQuery({
    queryKey: ['artist-tags', artistId],
    queryFn: () => getArtistTags(artistId),
    enabled: !!artistId,
    staleTime: 5 * 60 * 1000,
  })

  const similar = useQuery({
    queryKey: ['similar-artists', artistId],
    queryFn: () => getSimilarArtists(artistId),
    enabled: !!artistId,
    staleTime: 5 * 60 * 1000,
  })

  return { tags, similar }
}
