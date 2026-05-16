import { useNavigate } from 'react-router-dom'
import { ChevronRight } from 'lucide-react'
import type { SimilarArtistDto } from '@/types/artist'

interface Props {
  artists: SimilarArtistDto[]
}

export function SimilarArtistList({ artists }: Props) {
  const navigate = useNavigate()

  if (artists.length === 0) {
    return <p className="text-sm text-muted-foreground">類似バンドのデータがありません</p>
  }

  return (
    <ul className="space-y-1">
      {artists.map((a) => {
        const score = a.lastfmScore ?? a.computedScore
        return (
          <li key={a.id}>
            <button
              onClick={() => navigate(`/artist/${a.id}`)}
              className="flex w-full items-center justify-between rounded-md px-3 py-2 text-sm hover:bg-secondary transition-colors"
            >
              <span className="font-medium text-foreground">{a.name}</span>
              <span className="flex items-center gap-1 text-muted-foreground">
                {score != null && (
                  <span className="text-xs">{(score * 100).toFixed(0)}%</span>
                )}
                <ChevronRight className="h-4 w-4" />
              </span>
            </button>
          </li>
        )
      })}
    </ul>
  )
}
