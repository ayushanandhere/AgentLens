import { render, screen, waitFor } from '@testing-library/react'
import App from '../App'

const jsonResponse = (body: unknown) =>
  Promise.resolve(new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } }))

describe('App', () => {
  beforeEach(() => {
    window.history.pushState({}, '', '/')
    vi.stubGlobal('fetch', vi.fn((input: RequestInfo | URL) => {
      const url = String(input)
      if (url.includes('/api/v1/traces')) {
        return jsonResponse({
          content: [
            {
              id: 'trace-1',
              agentId: 'agent-1',
              agentName: 'financial-analyst',
              model: 'gpt-4o',
              status: 'COMPLETED',
              policyResult: 'PASS',
              totalTokens: 1200,
              estimatedCost: 0.0123,
              latencyMs: 240,
              groundingScore: 0.81,
              startedAt: '2026-03-15T10:00:00Z',
            },
          ],
          totalElements: 1,
          totalPages: 1,
          number: 0,
          size: 20,
        })
      }
      return jsonResponse({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 })
    }))
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('renders the dashboard shell and traces view', async () => {
    render(<App />)

    expect(screen.getByText('AgentLens')).toBeInTheDocument()
    expect(screen.getByText('Live execution ledger')).toBeInTheDocument()

    await waitFor(() => {
      expect(screen.getAllByText('financial-analyst')).toHaveLength(2)
    })
  })
})
