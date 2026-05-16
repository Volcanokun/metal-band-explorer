import { render, screen } from '@testing-library/react'
import { describe, it, expect } from 'vitest'
import { TagBadge } from '@/components/TagBadge'

describe('TagBadge', () => {
  it('タグ名を表示する', () => {
    render(<TagBadge name="thrash metal" />)
    expect(screen.getByText('thrash metal')).toBeInTheDocument()
  })

  it('複数タグがそれぞれ独立して表示される', () => {
    render(
      <>
        <TagBadge name="death metal" />
        <TagBadge name="black metal" />
      </>,
    )
    expect(screen.getByText('death metal')).toBeInTheDocument()
    expect(screen.getByText('black metal')).toBeInTheDocument()
  })
})
