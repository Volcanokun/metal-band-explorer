import { useParams, useLocation, useNavigate } from 'react-router-dom'
import { ArrowLeft, Users, Play } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardHeader, CardTitle, CardContent } from '@/components/ui/card'
import { SimilarArtistList } from '@/components/SimilarArtistList'
import { useArtistDetail } from '@/hooks/useArtistDetail'
import type { ArtistDto } from '@/types/artist'

export function ArtistDetailPage() {
  const { id = '' } = useParams()
  const location = useLocation()
  const navigate = useNavigate()
  const artist = (location.state as { artist?: ArtistDto } | null)?.artist

  const { tags, similar } = useArtistDetail(id)

  function formatNumber(n: number | null): string {
    if (n == null) return '—'
    return n.toLocaleString('ja-JP')
  }

  const tagData = tags.data ?? artist?.tags ?? []
  const maxWeight = Math.max(...tagData.map((t) => t.weight), 1)

  return (
    <div className="min-h-screen bg-background">
      <header className="border-b border-border bg-card/50 px-4 py-4">
        <div className="mx-auto max-w-4xl">
          <Button variant="ghost" size="sm" onClick={() => navigate(-1)} className="mb-2 -ml-2">
            <ArrowLeft className="mr-2 h-4 w-4" />
            戻る
          </Button>
          <h1 className="text-3xl font-bold tracking-tight text-foreground">
            {artist?.name ?? '読み込み中...'}
          </h1>
        </div>
      </header>

      <main className="mx-auto max-w-4xl space-y-6 px-4 py-8">
        {/* 基本情報 */}
        {artist && (
          <Card>
            <CardHeader>
              <CardTitle className="text-base text-muted-foreground">基本情報</CardTitle>
            </CardHeader>
            <CardContent className="space-y-3">
              <div className="flex flex-wrap gap-6 text-sm">
                <div className="flex items-center gap-2">
                  <Users className="h-4 w-4 text-primary" />
                  <span className="text-muted-foreground">Listeners:</span>
                  <span className="font-medium">{formatNumber(artist.listeners)}</span>
                </div>
                <div className="flex items-center gap-2">
                  <Play className="h-4 w-4 text-primary" />
                  <span className="text-muted-foreground">Playcount:</span>
                  <span className="font-medium">{formatNumber(artist.playcount)}</span>
                </div>
              </div>
              {artist.bioSummary && (
                <p
                  className="text-sm leading-relaxed text-muted-foreground line-clamp-4"
                  dangerouslySetInnerHTML={{ __html: artist.bioSummary }}
                />
              )}
            </CardContent>
          </Card>
        )}

        {/* タグ（バーチャート） */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base text-muted-foreground">タグ</CardTitle>
          </CardHeader>
          <CardContent>
            {tags.isLoading && (
              <div className="space-y-2">
                {Array.from({ length: 5 }).map((_, i) => (
                  <div key={i} className="h-5 animate-pulse rounded bg-secondary" />
                ))}
              </div>
            )}
            {tags.isError && (
              <p className="text-sm text-destructive">タグの取得に失敗しました</p>
            )}
            {!tags.isLoading && tagData.length === 0 && (
              <p className="text-sm text-muted-foreground">タグデータがありません</p>
            )}
            <div className="space-y-2">
              {tagData.map((tag) => (
                <div key={tag.name} className="flex items-center gap-3">
                  <span className="w-36 truncate text-right text-xs text-muted-foreground">
                    {tag.name}
                  </span>
                  <div className="flex-1 rounded-full bg-secondary h-2">
                    <div
                      className="h-2 rounded-full bg-primary transition-all duration-500"
                      style={{ width: `${(tag.weight / maxWeight) * 100}%` }}
                    />
                  </div>
                  <span className="w-8 text-right text-xs text-muted-foreground">
                    {tag.weight}
                  </span>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>

        {/* 類似バンド */}
        <Card>
          <CardHeader>
            <CardTitle className="text-base text-muted-foreground">類似バンド</CardTitle>
          </CardHeader>
          <CardContent>
            {similar.isLoading && (
              <div className="space-y-2">
                {Array.from({ length: 5 }).map((_, i) => (
                  <div key={i} className="h-10 animate-pulse rounded bg-secondary" />
                ))}
              </div>
            )}
            {similar.isError && (
              <p className="text-sm text-destructive">類似バンドの取得に失敗しました</p>
            )}
            {!similar.isLoading && similar.data && (
              <SimilarArtistList artists={similar.data} />
            )}
          </CardContent>
        </Card>
      </main>
    </div>
  )
}
