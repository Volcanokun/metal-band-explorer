import { useNavigate } from 'react-router-dom'
import { Users } from 'lucide-react'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { TagBadge } from '@/components/TagBadge'
import type { ArtistDto } from '@/types/artist'

interface Props {
  artist: ArtistDto
}

export function ArtistCard({ artist }: Props) {
  const navigate = useNavigate()

  function formatListeners(n: number | null): string {
    if (n == null) return '—'
    return n >= 1_000_000
      ? `${(n / 1_000_000).toFixed(1)}M`
      : n >= 1_000
        ? `${(n / 1_000).toFixed(0)}K`
        : String(n)
  }

  return (
    <Card
      role="button"
      tabIndex={0}
      onClick={() => navigate(`/artist/${artist.id}`, { state: { artist } })}
      onKeyDown={(e) => e.key === 'Enter' && navigate(`/artist/${artist.id}`, { state: { artist } })}
      className="cursor-pointer transition-colors hover:border-primary/60 hover:bg-card/80"
    >
      <CardHeader className="pb-2">
        <CardTitle className="text-lg text-foreground">{artist.name}</CardTitle>
        {artist.listeners != null && (
          <p className="flex items-center gap-1 text-xs text-muted-foreground">
            <Users className="h-3 w-3" />
            {formatListeners(artist.listeners)} listeners
          </p>
        )}
      </CardHeader>
      <CardContent>
        <div className="flex flex-wrap gap-1.5">
          {artist.tags.slice(0, 5).map((tag) => (
            <TagBadge key={tag.name} name={tag.name} />
          ))}
        </div>
      </CardContent>
    </Card>
  )
}
