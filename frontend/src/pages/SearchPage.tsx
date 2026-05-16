import { useState } from 'react'
import { Music2 } from 'lucide-react'
import { SearchBar } from '@/components/SearchBar'
import { SearchHistory, addToSearchHistory } from '@/components/SearchHistory'
import { ArtistCard } from '@/components/ArtistCard'
import { useArtistSearch } from '@/hooks/useArtistSearch'

export function SearchPage() {
  const [submittedQuery, setSubmittedQuery] = useState('')
  const { data, isFetching, isError, error } = useArtistSearch(submittedQuery)

  function handleSearch(query: string) {
    setSubmittedQuery(query)
    addToSearchHistory(query)
  }

  return (
    <div className="min-h-screen bg-background">
      {/* ヘッダー */}
      <header className="border-b border-border bg-card/50 px-4 py-6">
        <div className="mx-auto max-w-4xl">
          <div className="mb-4 flex items-center gap-2">
            <Music2 className="h-6 w-6 text-primary" />
            <h1 className="text-2xl font-bold tracking-tight">Metal Band Explorer</h1>
          </div>
          <SearchBar onSearch={handleSearch} isLoading={isFetching} />
          <SearchHistory onSelect={handleSearch} />
        </div>
      </header>

      {/* コンテンツ */}
      <main className="mx-auto max-w-4xl px-4 py-8">
        {/* ローディング */}
        {isFetching && (
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            {Array.from({ length: 4 }).map((_, i) => (
              <div key={i} className="h-32 animate-pulse rounded-lg border border-border bg-card" />
            ))}
          </div>
        )}

        {/* エラー */}
        {isError && !isFetching && (
          <div className="rounded-lg border border-destructive/30 bg-destructive/10 p-4 text-sm text-destructive">
            {error instanceof Error ? error.message : '検索中にエラーが発生しました'}
          </div>
        )}

        {/* 結果なし */}
        {!isFetching && !isError && data && data.totalResults === 0 && (
          <p className="text-center text-muted-foreground">
            「{data.query}」に一致するバンドは見つかりませんでした
          </p>
        )}

        {/* 検索結果 */}
        {!isFetching && data && data.totalResults > 0 && (
          <>
            <p className="mb-4 text-sm text-muted-foreground">
              {data.totalResults} 件見つかりました
            </p>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              {data.artists.map((artist) => (
                <ArtistCard key={artist.id} artist={artist} />
              ))}
            </div>
          </>
        )}

        {/* 初期状態 */}
        {!submittedQuery && (
          <p className="text-center text-sm text-muted-foreground">
            バンド名を入力して検索してください
          </p>
        )}
      </main>
    </div>
  )
}
