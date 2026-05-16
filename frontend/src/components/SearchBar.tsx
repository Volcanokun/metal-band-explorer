import { useState } from 'react'
import { Search } from 'lucide-react'
import { Button } from '@/components/ui/button'

interface Props {
  onSearch: (query: string) => void
  isLoading?: boolean
}

export function SearchBar({ onSearch, isLoading = false }: Props) {
  const [value, setValue] = useState('')

  function handleSubmit(e: React.FormEvent) {
    e.preventDefault()
    const q = value.trim()
    if (q) onSearch(q)
  }

  return (
    <form onSubmit={handleSubmit} className="flex gap-2">
      <input
        type="text"
        value={value}
        onChange={(e) => setValue(e.target.value)}
        placeholder="バンド名を入力 (例: Metallica)"
        className="flex-1 rounded-md border border-input bg-background px-4 py-2 text-sm text-foreground placeholder:text-muted-foreground focus:outline-none focus:ring-2 focus:ring-ring"
        aria-label="バンド名"
      />
      <Button type="submit" disabled={isLoading || !value.trim()}>
        <Search className="mr-2 h-4 w-4" />
        {isLoading ? '検索中...' : '検索'}
      </Button>
    </form>
  )
}
