import { useQuery } from '@tanstack/react-query'
import { searchArtists } from '@/api/client'

export function useArtistSearch(query: string) {
  return useQuery({
    queryKey: ['search', query],
    queryFn: () => searchArtists(query),
    enabled: query.trim().length > 0,
    staleTime: 5 * 60 * 1000,
  })
}
