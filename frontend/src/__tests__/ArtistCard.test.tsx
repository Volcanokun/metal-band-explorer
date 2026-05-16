import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi } from 'vitest'
import { MemoryRouter } from 'react-router-dom'
import { ArtistCard } from '@/components/ArtistCard'
import type { ArtistDto } from '@/types/artist'

const mockNavigate = vi.fn()
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual<typeof import('react-router-dom')>('react-router-dom')
  return { ...actual, useNavigate: () => mockNavigate }
})

const artist: ArtistDto = {
  id: 'abc-123',
  name: 'Metallica',
  mbid: null,
  listeners: 5_000_000,
  playcount: null,
  bioSummary: null,
  tags: [
    { name: 'thrash metal', weight: 100 },
    { name: 'heavy metal', weight: 90 },
    { name: 'metal', weight: 80 },
  ],
}

function renderCard() {
  return render(
    <MemoryRouter>
      <ArtistCard artist={artist} />
    </MemoryRouter>,
  )
}

describe('ArtistCard', () => {
  it('バンド名を表示する', () => {
    renderCard()
    expect(screen.getByText('Metallica')).toBeInTheDocument()
  })

  it('リスナー数を整形して表示する', () => {
    renderCard()
    expect(screen.getByText(/5\.0M/)).toBeInTheDocument()
  })

  it('タグを最大 5 件表示する', () => {
    renderCard()
    expect(screen.getByText('thrash metal')).toBeInTheDocument()
    expect(screen.getByText('heavy metal')).toBeInTheDocument()
    expect(screen.getByText('metal')).toBeInTheDocument()
  })

  it('クリックで詳細ページに遷移する', async () => {
    renderCard()
    await userEvent.click(screen.getByRole('button'))
    expect(mockNavigate).toHaveBeenCalledWith('/artist/abc-123', expect.anything())
  })
})
