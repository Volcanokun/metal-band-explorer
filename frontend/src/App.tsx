import { BrowserRouter, Routes, Route } from 'react-router-dom'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { SearchPage } from '@/pages/SearchPage'
import { ArtistDetailPage } from '@/pages/ArtistDetailPage'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      refetchOnWindowFocus: false,
    },
  },
})

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter future={{ v7_startTransition: true, v7_relativeSplatPath: true }}>
        <Routes>
          <Route path="/" element={<SearchPage />} />
          <Route path="/artist/:id" element={<ArtistDetailPage />} />
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  )
}
