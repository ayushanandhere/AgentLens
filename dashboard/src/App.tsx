import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { Layout } from './components/Layout'
import { AnalyticsPage } from './pages/AnalyticsPage'
import { PoliciesPage } from './pages/PoliciesPage'
import { TraceDetailPage } from './pages/TraceDetailPage'
import { TracesPage } from './pages/TracesPage'
import { ViolationsPage } from './pages/ViolationsPage'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      retry: 1,
      staleTime: 3000,
    },
  },
})

export default function App() {
  return (
    <QueryClientProvider client={queryClient}>
      <BrowserRouter>
        <Routes>
          <Route element={<Layout />} path="/">
            <Route index element={<TracesPage />} />
            <Route element={<TraceDetailPage />} path="traces/:traceId" />
            <Route element={<PoliciesPage />} path="policies" />
            <Route element={<ViolationsPage />} path="violations" />
            <Route element={<AnalyticsPage />} path="analytics" />
            <Route element={<Navigate replace to="/" />} path="*" />
          </Route>
        </Routes>
      </BrowserRouter>
    </QueryClientProvider>
  )
}
