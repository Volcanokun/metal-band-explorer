import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { describe, it, expect, vi } from 'vitest'
import { SearchBar } from '@/components/SearchBar'

describe('SearchBar', () => {
  it('入力フィールドと検索ボタンを表示する', () => {
    render(<SearchBar onSearch={vi.fn()} />)
    expect(screen.getByRole('textbox', { name: 'バンド名' })).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /検索/ })).toBeInTheDocument()
  })

  it('入力が空のとき検索ボタンが無効', () => {
    render(<SearchBar onSearch={vi.fn()} />)
    expect(screen.getByRole('button', { name: /検索/ })).toBeDisabled()
  })

  it('フォーム送信で onSearch が呼ばれる', async () => {
    const onSearch = vi.fn()
    render(<SearchBar onSearch={onSearch} />)

    await userEvent.type(screen.getByRole('textbox', { name: 'バンド名' }), 'Metallica')
    await userEvent.click(screen.getByRole('button', { name: /検索/ }))

    expect(onSearch).toHaveBeenCalledWith('Metallica')
  })

  it('空白のみの入力では onSearch が呼ばれない', async () => {
    const onSearch = vi.fn()
    render(<SearchBar onSearch={onSearch} />)

    await userEvent.type(screen.getByRole('textbox', { name: 'バンド名' }), '   ')
    await userEvent.click(screen.getByRole('button', { name: /検索/ }))

    expect(onSearch).not.toHaveBeenCalled()
  })
})
