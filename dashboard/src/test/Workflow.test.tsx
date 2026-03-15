import userEvent from '@testing-library/user-event'
import { render, screen, waitFor } from '@testing-library/react'
import App from '../App'

const ok = (body: unknown) =>
  Promise.resolve(new Response(JSON.stringify(body), { status: 200, headers: { 'Content-Type': 'application/json' } }))

describe('approval workflow view', () => {
  it('approves a pending violation and refreshes the queue', async () => {
    const fetchMock = vi.fn(async (input: RequestInfo | URL, init?: RequestInit) => {
      const url = String(input)

      if (url.includes('/api/v1/violations') && init?.method === 'POST' && url.endsWith('/approve')) {
        return ok({
          id: 'violation-1',
          traceId: 'trace-1',
          policyId: 'policy-1',
          policyName: 'Approval Policy',
          violationType: 'REQUIRE_APPROVAL',
          severity: 'BLOCK',
          details: {},
          actionTaken: 'APPROVED_OVERRIDE',
          resolvedBy: 'admin@agentlens.dev',
          createdAt: '2026-03-15T10:00:00Z',
        })
      }

      if (url.includes('/api/v1/violations')) {
        return ok({
          content: fetchMock.mock.calls.some(([, options]) => String(options?.method).toUpperCase() === 'POST')
            ? [
                {
                  id: 'violation-1',
                  traceId: 'trace-1',
                  policyId: 'policy-1',
                  policyName: 'Approval Policy',
                  violationType: 'REQUIRE_APPROVAL',
                  severity: 'BLOCK',
                  details: {},
                  actionTaken: 'APPROVED_OVERRIDE',
                  resolvedBy: 'admin@agentlens.dev',
                  createdAt: '2026-03-15T10:00:00Z',
                },
              ]
            : [
                {
                  id: 'violation-1',
                  traceId: 'trace-1',
                  policyId: 'policy-1',
                  policyName: 'Approval Policy',
                  violationType: 'REQUIRE_APPROVAL',
                  severity: 'BLOCK',
                  details: { tool: 'execShell' },
                  actionTaken: 'PENDING_APPROVAL',
                  resolvedBy: null,
                  createdAt: '2026-03-15T10:00:00Z',
                },
              ],
          totalElements: 1,
          totalPages: 1,
          number: 0,
          size: 50,
        })
      }

      return ok({ content: [], totalElements: 0, totalPages: 0, number: 0, size: 20 })
    })

    vi.stubGlobal('fetch', fetchMock)
    window.history.pushState({}, '', '/violations')

    render(<App />)

    const approveButton = await screen.findByRole('button', { name: 'Approve' })
    await userEvent.click(approveButton)

    await waitFor(() => {
      expect(screen.getByText(/Resolved by admin@agentlens.dev/)).toBeInTheDocument()
    })
  })
})
