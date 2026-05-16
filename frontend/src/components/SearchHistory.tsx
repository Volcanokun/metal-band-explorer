import { Clock } from 'lucide-react'

const HISTORY_KEY = 'metal-explorer-search-history'
const MAX_HISTORY = 10

export function getSearchHistory(): string[] {
  try {
    return JSON.parse(localStorage.getItem(HISTORY_KEY) ?? '[]') as string[]
  } catch {
    return []
  }
}

export function addToSearchHistory(query: string): void {
  const history = getSearchHistory().filter((h) => h !== query)
  history.unshift(query)
  localStorage.setItem(HISTORY_KEY, JSON.stringify(history.slice(0, MAX_HISTORY)))
}

interface Props {
  onSelect: (query: string) => void
}

export function SearchHistory({ onSelect }: Props) {
  const history = getSearchHistory()
  if (history.length === 0) return null

  return (
    <div className="mt-3">
      <p className="mb-1.5 flex items-center gap-1 text-xs text-muted-foreground">
        <Clock className="h-3 w-3" />
        最近の検索
      </p>
      <div className="flex flex-wrap gap-1.5">
        {history.map((q) => (
          <button
            key={q}
            onClick={() => onSelect(q)}
            className="rounded-full border border-border px-3 py-1 text-xs text-muted-foreground hover:border-primary hover:text-foreground transition-colors"
          >
            {q}
          </button>
        ))}
      </div>
    </div>
  )
}
