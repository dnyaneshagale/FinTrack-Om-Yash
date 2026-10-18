import { useEffect, useMemo, useState } from 'react'
import {
  ArrowDownLeft, ArrowUpRight, ArrowUpDown, CalendarDays, Check, ChevronRight,
  CircleHelp, Eye, EyeOff, LogOut, Menu, Moon, Plus, Send, Sparkles, Sun, WalletCards, X
} from 'lucide-react'
import './App.css'

// ── Types ────────────────────────────────────────────────────────────────────
type Account = { id: string; ownerName: string; balancePaise: number; openingBalancePaise: number }
type Transaction = { id: string; accountId: string; type: 'CREDIT' | 'DEBIT'; amountPaise: number; date: string; description: string; notes: string; createdByName?: string }
type Need = { id: string; requesterName: string; cause: string; amountPaise: number; fundedPaise: number; remainingPaise: number; requiredBy: string; status: string }
type Profile = { id: string; username: string; displayName: string; role: 'ADMIN' | 'MEMBER' }

// ── Demo Accounts (Production & Local Dev) ──────────────────────────────────
const isProd = typeof window !== 'undefined' && window.location.hostname !== 'localhost'


// ── API helpers ───────────────────────────────────────────────────────────────────────────────
const api = import.meta.env.VITE_API_BASE_URL ||
  (isProd ? '/api/v1' : 'http://localhost:8080/api/v1')

const money = (paise: number) =>
  new Intl.NumberFormat('en-IN', { style: 'currency', currency: 'INR', maximumFractionDigits: 2 }).format(paise / 100)

const apiFetch = (path: string, init: RequestInit = {}) => {
  const headers = new Headers(init.headers)
  const savedUser = localStorage.getItem('fintrack_user')
  if (savedUser) {
    try {
      const u = JSON.parse(savedUser)
      if (u?.username) headers.set('X-User', u.username)
    } catch {}
  }
  return fetch(`${api}${path}`, { ...init, headers })
}

// ── Date utilities ────────────────────────────────────────────────────────────
const formatHeaderDate = () => {
  const now = new Date()
  const days = ['SUNDAY', 'MONDAY', 'TUESDAY', 'WEDNESDAY', 'THURSDAY', 'FRIDAY', 'SATURDAY']
  const months = ['JANUARY', 'FEBRUARY', 'MARCH', 'APRIL', 'MAY', 'JUNE', 'JULY', 'AUGUST', 'SEPTEMBER', 'OCTOBER', 'NOVEMBER', 'DECEMBER']
  return `${days[now.getDay()]}, ${months[now.getMonth()]} ${now.getDate()}, ${now.getFullYear()}`
}
const getGreeting = () => {
  const hour = new Date().getHours()
  if (hour < 12) return 'Good morning'
  if (hour < 17) return 'Good afternoon'
  return 'Good evening'
}
const todayISO = () => new Date().toISOString().slice(0, 10)
const currentMonthName = () => new Date().toLocaleString('en-IN', { month: 'long' })
const currentMonthPad = () => String(new Date().getMonth() + 1).padStart(2, '0')

// ── App ───────────────────────────────────────────────────────────────────────
function App() {
  const [active, setActive] = useState('Overview')
  const [accounts, setAccounts] = useState<Account[]>([])
  const [transactions, setTransactions] = useState<Transaction[]>([])
  const [needs, setNeeds] = useState<Need[]>([])
  const [showAdd, setShowAdd] = useState(false)
  const [mobileMenu, setMobileMenu] = useState(false)
  const [dataLoading, setDataLoading] = useState(false)
  const [dataLoaded, setDataLoaded] = useState(false) // prevents infinite spinner on empty accounts
  const [profile, setProfile] = useState<Profile | null>(null)
  const [authLoading, setAuthLoading] = useState(true)
  const [authError, setAuthError] = useState('')
  const [loginLoading, setLoginLoading] = useState(false)
  const [loginUsername, setLoginUsername] = useState('Pappa')
  const [loginPassword, setLoginPassword] = useState('')
  const [showPassword, setShowPassword] = useState(false)
  const [dataError, setDataError] = useState('')
  const [selectedAccount, setSelectedAccount] = useState<Account | null>(null)
  const [showNeedForm, setShowNeedForm] = useState(false)
  const [showBalanceForm, setShowBalanceForm] = useState(false)
  const [fundingNeed, setFundingNeed] = useState<Need | null>(null)
  const firstAccountId = accounts[0]?.id
  const [darkMode, setDarkMode] = useState(() => localStorage.getItem('family-finance-theme') === 'dark')

  // ── Effects ─────────────────────────────────────────────────────────────────
  useEffect(() => {
    document.documentElement.classList.toggle('dark', darkMode)
    localStorage.setItem('family-finance-theme', darkMode ? 'dark' : 'light')
  }, [darkMode])

  // Restore session from localStorage on mount
  useEffect(() => {
    const saved = localStorage.getItem('fintrack_user')
    if (saved) {
      try {
        const u = JSON.parse(saved)
        if (u && u.username) {
          setProfile(u)
        }
      } catch {}
    }
    setAuthLoading(false)
  }, [])

  // Load accounts + needs after login
  useEffect(() => {
    if (!profile) return
    setDataLoading(true)
    setDataLoaded(false)
    Promise.all([apiFetch('/accounts'), apiFetch('/needs')])
      .then(async ([a, n]) => {
        if (!a.ok || !n.ok) throw new Error('Unable to load family data')
        setAccounts(await a.json())
        setNeeds(await n.json())
        setDataError('')
      })
      .catch(() => setDataError('Could not load the family ledger. Check the backend connection and retry.'))
      .finally(() => {
        setDataLoading(false)
        setDataLoaded(true)
      })
  }, [profile])

  // Load transactions after accounts are known
  useEffect(() => {
    if (!profile || accounts.length === 0) return
    Promise.all(
      accounts.map((account) =>
        apiFetch(`/accounts/${account.id}/transactions`)
          .then((r) => (r.ok ? (r.json() as Promise<Transaction[]>) : []))
      )
    )
      .then((lists) => setTransactions(lists.flat()))
      .catch(() => undefined)
  }, [firstAccountId, profile])

  // ── Derived values ───────────────────────────────────────────────────────────
  const total = useMemo(() => accounts.reduce((sum, a) => sum + a.balancePaise, 0), [accounts])
  const monthStr = currentMonthPad()
  const monthCredits = transactions
    .filter((t) => t.type === 'CREDIT' && t.date.slice(5, 7) === monthStr)
    .reduce((sum, t) => sum + t.amountPaise, 0)
  const monthDebits = transactions
    .filter((t) => t.type === 'DEBIT' && t.date.slice(5, 7) === monthStr)
    .reduce((sum, t) => sum + t.amountPaise, 0)
  const reportRows = ['January', 'February', 'March', 'April', 'May', 'June', 'July', 'August', 'September', 'October', 'November', 'December']
    .map((name, index) => {
      const month = String(index + 1).padStart(2, '0')
      const mTx = transactions.filter((tx) => tx.date.slice(5, 7) === month)
      const credits = mTx.filter((tx) => tx.type === 'CREDIT').reduce((s, tx) => s + tx.amountPaise, 0)
      const debits = mTx.filter((tx) => tx.type === 'DEBIT').reduce((s, tx) => s + tx.amountPaise, 0)
      return { name, credits, debits, net: credits - debits }
    })
  const hasMultipleAccounts = accounts.length >= 2
  const navItems = [
    ['Overview', Sparkles],
    ['Transactions', ArrowUpDown],
    ['Family Needs', Send],
    ['Reports', CalendarDays],
  ] as const

  // ── Handlers ─────────────────────────────────────────────────────────────────
  const signOut = () => {
    localStorage.removeItem('fintrack_user')
    setProfile(null)
    setAccounts([])
    setTransactions([])
    setNeeds([])
    setDataLoaded(false)
    setDataError('')
    apiFetch('/auth/logout', { method: 'POST' }).catch(() => {})
  }

  const login = (event?: React.FormEvent<HTMLFormElement>) => {
    if (event) event.preventDefault()
    if (loginLoading) return
    setAuthError('')
    setLoginLoading(true)
    apiFetch('/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ username: loginUsername, password: loginPassword }),
    })
      .then(async (response) => {
        if (!response.ok) {
          const data = await response.json().catch(() => null)
          throw new Error(data?.message || 'Invalid username or password. Please try again.')
        }
        const userProfile = (await response.json()) as Profile
        localStorage.setItem('fintrack_user', JSON.stringify(userProfile))
        setProfile(userProfile)
      })
      .catch((error: Error) => setAuthError(error.message))
      .finally(() => setLoginLoading(false))
  }

  // FIX: reads the `type` field from the form; was previously hardcoded to CREDIT
  const addTransaction = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setDataError('')
    const form = new FormData(event.currentTarget)
    const amountPaise = Math.round(Number(form.get('amount')) * 100)
    const accountId = String(form.get('account'))
    const txType = String(form.get('type')) as 'CREDIT' | 'DEBIT'
    apiFetch(`/accounts/${accountId}/transactions`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ type: txType, amountPaise, date: String(form.get('date')), description: String(form.get('description')), notes: String(form.get('notes') || ''), idempotencyKey: crypto.randomUUID() }),
    })
      .then(async (response) => {
        if (!response.ok) throw new Error('Transaction could not be saved')
        const transaction = await response.json() as Transaction
        setTransactions((current) => [transaction, ...current])
        setAccounts((current) =>
          current.map((a) =>
            a.id === accountId
              ? { ...a, balancePaise: a.balancePaise + (txType === 'CREDIT' ? amountPaise : -amountPaise) }
              : a
          )
        )
        setShowAdd(false)
      })
      .catch((error: Error) => setDataError(error.message))
  }

  const addNeed = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setDataError('')
    const form = new FormData(event.currentTarget)
    apiFetch('/needs', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ cause: String(form.get('cause')), amountPaise: Math.round(Number(form.get('amount')) * 100), requiredBy: String(form.get('requiredBy')), notes: String(form.get('notes') || '') }),
    })
      .then(async (response) => {
        if (!response.ok) throw new Error('Need could not be saved')
        const need = await response.json() as Need
        setNeeds((current) => [...current, need])
        setShowNeedForm(false)
      })
      .catch((error: Error) => setDataError(error.message))
  }

  const adjustBalance = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    setDataError('')
    const form = new FormData(event.currentTarget)
    const account = accounts[0]
    if (!account) return
    const desiredPaise = Math.round(Number(form.get('balance')) * 100)
    const deltaPaise = desiredPaise - account.balancePaise
    apiFetch(`/accounts/${account.id}/adjustments`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ deltaPaise, reason: String(form.get('reason')), idempotencyKey: crypto.randomUUID() }),
    })
      .then(async (response) => {
        if (!response.ok) throw new Error('Balance could not be updated')
        setAccounts([await response.json() as Account])
        setShowBalanceForm(false)
      })
      .catch((error: Error) => setDataError(error.message))
  }

  const reviewNeed = (need: Need, status: 'APPROVED' | 'REJECTED') => {
    setDataError('')
    apiFetch(`/needs/${need.id}/status`, {
      method: 'PATCH',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ status }),
    })
      .then(async (response) => {
        if (!response.ok) throw new Error('Need review could not be saved')
        const updated = await response.json() as Need
        setNeeds((current) => current.map((item) => (item.id === updated.id ? updated : item)))
      })
      .catch((error: Error) => setDataError(error.message))
  }

  const submitFunding = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    if (!fundingNeed) return
    setDataError('')
    const form = new FormData(event.currentTarget)
    apiFetch(`/needs/${fundingNeed.id}/fundings`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ amountPaise: Math.round(Number(form.get('amount')) * 100) }),
    })
      .then(async (response) => {
        if (!response.ok) throw new Error('Need funding could not be saved')
        const updated = await response.json() as Need
        setNeeds((current) => current.map((item) => (item.id === updated.id ? updated : item)))
        setFundingNeed(null)
      })
      .catch((error: Error) => setDataError(error.message))
  }

  // ── Screens ───────────────────────────────────────────────────────────────────

  if (authLoading) {
    return (
      <div className="auth-screen">
        <div className="auth-card">
          <div className="brand-mark"><WalletCards size={20} /></div>
          <h1>Opening your family ledger</h1>
          <p className="muted">Connecting securely...</p>
        </div>
      </div>
    )
  }

  if (!profile) {
    return (
      <div className="auth-screen">
        <form className="auth-card login-card" onSubmit={login}>
          <div className="auth-brand">
            <div className="brand-mark"><WalletCards size={20} /></div>
            <div>
              <strong>Fintrack Om Yash</strong>
              <span>PRIVATE FAMILY LEDGER</span>
            </div>
          </div>
          <p className="eyebrow">PAPPA · OM · YASH</p>
          <h1>Welcome back.</h1>
          <p className="muted">Sign in as Pappa, Om, or Yash using your password.</p>

          <label htmlFor="login-username">
            Name
            <input
              id="login-username"
              value={loginUsername}
              onChange={(e) => setLoginUsername(e.target.value)}
              autoComplete="username"
              placeholder="Pappa, Om, or Yash"
              required
              disabled={loginLoading}
            />
          </label>
          <label htmlFor="login-password">
            Password
            <div className="password-input-wrapper">
              <input
                id="login-password"
                type={showPassword ? 'text' : 'password'}
                value={loginPassword}
                onChange={(e) => setLoginPassword(e.target.value)}
                autoComplete="current-password"
                placeholder="Enter password"
                required
                disabled={loginLoading}
              />
              <button
                type="button"
                className="password-toggle-btn"
                onClick={() => setShowPassword((s) => !s)}
                tabIndex={-1}
                aria-label={showPassword ? 'Hide password' : 'Show password'}
              >
                {showPassword ? <EyeOff size={16} /> : <Eye size={16} />}
              </button>
            </div>
          </label>
          {authError && (
            <div className="auth-error">
              <CircleHelp size={15} />
              {authError}
            </div>
          )}
          <button className="primary-button wide" type="submit" id="login-submit-btn" disabled={loginLoading}>
            {loginLoading ? 'Signing in…' : 'Sign in securely'}
          </button>
          <button className="theme-link" type="button" onClick={() => setDarkMode((d) => !d)}>
            {darkMode ? <Sun size={16} /> : <Moon size={16} />} Use {darkMode ? 'light' : 'dark'} theme
          </button>
        </form>
      </div>
    )
  }

  // FIX: use dataLoaded flag — prevents infinite spinner when accounts is empty but loaded
  if (dataLoading || !dataLoaded) {
    return (
      <div className="auth-screen">
        <div className="auth-card">
          <div className="brand-mark"><WalletCards size={20} /></div>
          <h1>Loading your ledger</h1>
          <p className="muted">Fetching the latest records securely...</p>
        </div>
      </div>
    )
  }

  if (dataError && accounts.length === 0) {
    return (
      <div className="auth-screen">
        <div className="auth-card">
          <div className="brand-mark"><WalletCards size={20} /></div>
          <h1>Ledger unavailable</h1>
          <p className="muted">{dataError}</p>
          <button className="primary-button wide" onClick={() => window.location.reload()}>
            Retry connection
          </button>
          <button className="theme-link" onClick={signOut}>
            Sign out
          </button>
        </div>
      </div>
    )
  }

  // ── Member view (Om / Yash) ──────────────────────────────────────────────────
  if (profile.role === 'MEMBER') {
    return (
      <div className="member-shell">
        <header className="member-header">
          <div>
            <p className="eyebrow">FAMILY FINANCE</p>
            <h1>Hello, {profile.displayName}.</h1>
          </div>
          <div className="header-actions">
            <button className="icon-button theme-toggle" onClick={() => setDarkMode((d) => !d)} aria-label="Toggle theme">
              {darkMode ? <Sun size={18} /> : <Moon size={18} />}
            </button>
            <button
              className="icon-button"
              onClick={signOut}
              aria-label="Sign out"
            >
              <LogOut size={18} />
            </button>
          </div>
        </header>
        <main className="member-main">
          {dataError && (
            <div className="data-error">
              <CircleHelp size={15} />
              <span>{dataError}</span>
              <button onClick={() => setDataError('')}>Dismiss</button>
            </div>
          )}
          <section className="member-balance">
            <span className="label">CURRENT BALANCE</span>
            <strong>{money(accounts[0]?.balancePaise ?? 0)}</strong>
            <p>Calculated from your tracked ledger</p>
            <button className="secondary-button" onClick={() => setShowBalanceForm(true)}>
              Update current balance
            </button>
          </section>
          <section className="member-needs">
            <div className="panel-heading">
              <div>
                <h2>My needs</h2>
                <p className="muted">Requests waiting for family review.</p>
              </div>
              <button className="primary-button" onClick={() => setShowNeedForm(true)}>
                <Plus size={17} />Add need
              </button>
            </div>
            {needs.length === 0 ? (
              <div className="member-empty">
                No needs yet. Add one when you need help with an upcoming expense.
              </div>
            ) : (
              needs.map((need) => (
                <div className="member-need" key={need.id}>
                  <div>
                    <strong>{need.cause}</strong>
                    <span>Due {need.requiredBy}</span>
                  </div>
                  <div>
                    <b>{money(need.amountPaise)}</b>
                    <small>{need.status.replace('_', ' ')}</small>
                  </div>
                </div>
              ))
            )}
          </section>
        </main>
        {showBalanceForm && (
          <div className="modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && setShowBalanceForm(false)}>
            <div className="modal">
              <div className="modal-heading">
                <div>
                  <p className="eyebrow">LEDGER ADJUSTMENT</p>
                  <h2>Update current balance</h2>
                </div>
                <button className="icon-button" onClick={() => setShowBalanceForm(false)} aria-label="Close"><X size={18} /></button>
              </div>
              <p className="muted modal-copy">This records the difference as an adjustment transaction. Your history remains intact.</p>
              <form onSubmit={adjustBalance}>
                <label>
                  Current balance (INR)
                  <input name="balance" type="number" min="0" step="0.01" defaultValue={(accounts[0]?.balancePaise ?? 0) / 100} required />
                </label>
                <label>
                  Reason
                  <input name="reason" placeholder="e.g. Bank balance reconciliation" required />
                </label>
                <button className="primary-button wide" type="submit"><Check size={17} />Save balance</button>
              </form>
            </div>
          </div>
        )}
        {showNeedForm && (
          <div className="modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && setShowNeedForm(false)}>
            <div className="modal">
              <div className="modal-heading">
                <div>
                  <p className="eyebrow">NEW REQUEST</p>
                  <h2>Add a need</h2>
                </div>
                <button className="icon-button" onClick={() => setShowNeedForm(false)} aria-label="Close"><X size={18} /></button>
              </div>
              <form onSubmit={addNeed}>
                <label>What do you need?<input name="cause" placeholder="e.g. Course books" required /></label>
                <label>Amount (INR)<input name="amount" type="number" min="0.01" step="0.01" required /></label>
                <label>Required by<input name="requiredBy" type="date" required /></label>
                <label>Notes <span className="muted">(optional)</span><textarea name="notes" rows={3} /></label>
                <button className="primary-button wide" type="submit"><Check size={17} />Submit need</button>
              </form>
            </div>
          </div>
        )}
      </div>
    )
  }

  // ── Admin view (Pappa) ──────────────────────────────────────────────────────
  return (
    <div className="app-shell">
      {mobileMenu && <div className="sidebar-backdrop" onClick={() => setMobileMenu(false)} />}
      <aside className={mobileMenu ? 'sidebar open' : 'sidebar'}>
        <div className="brand">
          <div className="brand-mark"><WalletCards size={20} /></div>
          <div>
            <strong>Family Finance</strong>
            <span>TRACKER</span>
          </div>
        </div>
        <div className="profile">
          <div className="avatar">{profile.displayName.charAt(0)}</div>
          <div>
            <strong>{profile.displayName}</strong>
            <span>{profile.role === 'ADMIN' ? 'Administrator' : 'Family member'}</span>
          </div>
          <ChevronRight size={16} />
        </div>
        <nav>
          {navItems.map(([item, Icon]) => (
            <button
              className={active === item ? 'nav-item selected' : 'nav-item'}
              key={item}
              onClick={() => { setActive(item); setMobileMenu(false) }}
            >
              <Icon size={17} />{item}
            </button>
          ))}
        </nav>
        <div className="sidebar-bottom">
          <button className="nav-item" onClick={signOut}>
            <LogOut size={17} />Sign out
          </button>
          <small>v0.1.0 · Asia/Kolkata</small>
        </div>
      </aside>

      <main className="main">
        <header>
          <button className="mobile-menu" onClick={() => setMobileMenu(!mobileMenu)} aria-label="Toggle navigation">
            <Menu size={20} />
          </button>
          <div>
            <p className="eyebrow">{formatHeaderDate()}</p>
            <h1>{getGreeting()}, {profile.displayName}.</h1>
          </div>
          <div className="header-actions">
            <button
              className="icon-button theme-toggle"
              onClick={() => setDarkMode((d) => !d)}
              aria-label={darkMode ? 'Use light theme' : 'Use dark theme'}
              title={darkMode ? 'Use light theme' : 'Use dark theme'}
            >
              {darkMode ? <Sun size={18} /> : <Moon size={18} />}
            </button>
            <button
              className="icon-button sign-out-btn"
              onClick={() => apiFetch('/auth/logout', { method: 'POST' }).then(() => setProfile(null))}
              aria-label="Sign out"
              title="Sign out"
            >
              <LogOut size={18} />
            </button>
            <div className="header-avatar" title={`${profile.displayName} (${profile.role})`}>{profile.displayName.charAt(0)}</div>
          </div>
        </header>

        {dataError && (
          <div className="data-error">
            <CircleHelp size={15} />{dataError}
            <button onClick={() => window.location.reload()}>Retry</button>
          </div>
        )}
        {active !== 'Overview' && (
          <div className="view-banner">
            <strong>{active}</strong>
            <span>Selected view is ready for the next action.</span>
            <button onClick={() => setActive('Overview')}>Back to overview</button>
          </div>
        )}

        <section className="action-row">
          <div>
            <h2>{active === 'Overview' ? 'Family overview' : active}</h2>
            <p className="muted">Choose a son in the credit form below.</p>
          </div>
          <button className="primary-button" onClick={() => setShowAdd(true)}>
            <Plus size={17} />Credit balance
          </button>
        </section>

        {active === 'Family Needs' && (
          <section className="panel focus-panel">
            <div className="panel-heading">
              <div>
                <h2>All family needs</h2>
                <p className="muted">Review and fund requests from Om and Yash.</p>
              </div>
              <button className="text-button" onClick={() => setActive('Overview')}>
                Close <X size={15} />
              </button>
            </div>
            <div className="needs-list">
              {needs.length === 0 ? (
                <p className="muted">No needs have been recorded yet.</p>
              ) : (
                needs.map((need) => (
                  <div className="need-row" key={`all-${need.id}`}>
                    <div className="need-icon"><Send size={16} /></div>
                    <div className="tx-copy">
                      <strong>{need.cause}</strong>
                      <span>{need.requesterName} · due {need.requiredBy}</span>
                    </div>
                    <div className="need-progress">
                      <div>
                        <span>{money(need.fundedPaise)} funded</span>
                        <b>{Math.round(need.fundedPaise / need.amountPaise * 100)}%</b>
                      </div>
                      <div className="progress-track">
                        <i style={{ width: `${need.fundedPaise / need.amountPaise * 100}%` }} />
                      </div>
                    </div>
                    <span className="status">{need.status.replace('_', ' ')}</span>
                    {need.status === 'PENDING' && (
                      <>
                        <button className="small-action" onClick={() => reviewNeed(need, 'APPROVED')}>Approve</button>
                        <button className="small-action reject" onClick={() => reviewNeed(need, 'REJECTED')}>Reject</button>
                      </>
                    )}
                    {(need.status === 'APPROVED' || need.status === 'PARTIALLY_FUNDED') && (
                      <button className="small-action" onClick={() => setFundingNeed(need)}>Fund</button>
                    )}
                  </div>
                ))
              )}
            </div>
          </section>
        )}

        {active === 'Reports' && (
          <section className="panel focus-panel">
            <div className="panel-heading">
              <div>
                <h2>Monthly snapshots</h2>
                <p className="muted">Credits and spending grouped by transaction month.</p>
              </div>
              <CalendarDays size={18} className="panel-icon" />
            </div>
            <div className="report-table">
              <div className="report-row report-header">
                <span>Month</span><span>Credits</span><span>Debits</span><span>Net</span>
              </div>
              {reportRows.map((row) => (
                <div className="report-row" key={row.name}>
                  <strong>{row.name}</strong>
                  <span className="credit-text">+{money(row.credits)}</span>
                  <span>-{money(row.debits)}</span>
                  <strong className={row.net >= 0 ? 'credit-text' : 'amount'}>
                    {row.net >= 0 ? '+' : ''}{money(row.net)}
                  </strong>
                </div>
              ))}
            </div>
          </section>
        )}

        {active === 'Transactions' && (
          <section className="panel focus-panel">
            <div className="panel-heading">
              <div>
                <h2>All recent activity</h2>
                <p className="muted">Every movement across Om and Yash accounts.</p>
              </div>
              <button className="text-button" onClick={() => setActive('Overview')}>
                Close <X size={15} />
              </button>
            </div>
            <div className="transaction-list">
              {transactions.length === 0 ? (
                <p className="muted">No transactions recorded yet.</p>
              ) : (
                transactions.map((tx) => (
                  <div className="transaction" key={`all-${tx.id}`}>
                    <div className={tx.type === 'CREDIT' ? 'tx-icon credit' : 'tx-icon debit'}>
                      {tx.type === 'CREDIT' ? <ArrowDownLeft size={18} /> : <ArrowUpRight size={18} />}
                    </div>
                    <div className="tx-copy">
                      <strong>{tx.description}</strong>
                      <span>{accounts.find((a) => a.id === tx.accountId)?.ownerName} · {tx.date}</span>
                      <small>Recorded by {tx.createdByName ?? 'Unknown member'}{tx.notes ? ` · ${tx.notes}` : ''}</small>
                    </div>
                    <strong className={tx.type === 'CREDIT' ? 'amount credit-text' : 'amount'}>
                      {tx.type === 'CREDIT' ? '+' : '-'}{money(tx.amountPaise)}
                    </strong>
                  </div>
                ))
              )}
            </div>
          </section>
        )}

        {/* FIX: guard against accounts[1] being undefined before rendering the split-bar */}
        {accounts.length > 0 && (
          <section className="balance-grid">
            <div className="total-card">
              <div className="card-top">
                <span className="label">COMBINED TRACKED BALANCE</span>
                <span className="live-dot">LIVE</span>
              </div>
              <strong>{money(total)}</strong>
              <p>Across {accounts.map((a) => a.ownerName).join(' and ')} accounts</p>
              {hasMultipleAccounts && (
                <>
                  <div className="total-bar">
                    <span style={{ width: `${Math.round(accounts[0].balancePaise / (total || 1) * 100)}%` }} />
                  </div>
                  <div className="split-labels">
                    <span><i className="teal-dot" />{accounts[0].ownerName} <b>{money(accounts[0].balancePaise)}</b></span>
                    <span><i className="coral-dot" />{accounts[1].ownerName} <b>{money(accounts[1].balancePaise)}</b></span>
                  </div>
                </>
              )}
            </div>
            {accounts.map((account, index) => (
              <div
                className="account-card clickable"
                key={account.id}
                onClick={() => setSelectedAccount(account)}
                title={`Click to view ${account.ownerName}'s account history`}
              >
                <div className="account-heading">
                  <div className={index === 0 ? 'account-icon teal' : 'account-icon coral'}>
                    {account.ownerName.slice(0, 2).toUpperCase()}
                  </div>
                  <div>
                    <span className="label">TRACKED ACCOUNT</span>
                    <h3>{account.ownerName}</h3>
                  </div>
                  <ChevronRight size={16} className="card-arrow" />
                </div>
                <strong>{money(account.balancePaise)}</strong>
                <p className="positive">Active account <span>· View details</span></p>
              </div>
            ))}
          </section>
        )}

        {/* Activity + summary panels — previously hidden by .main > .content-grid { display:none } */}
        <section className="content-grid">
          <div className="panel activity-panel">
            <div className="panel-heading">
              <div>
                <h2>Recent activity</h2>
                <p className="muted">Latest movements across both accounts</p>
              </div>
              <button className="text-button" onClick={() => setActive('Transactions')}>
                View all <ChevronRight size={15} />
              </button>
            </div>
            <div className="transaction-list">
              {transactions.length === 0 ? (
                <p className="muted">No transactions recorded yet.</p>
              ) : (
                transactions.map((tx) => (
                  <div className="transaction" key={tx.id}>
                    <div className={tx.type === 'CREDIT' ? 'tx-icon credit' : 'tx-icon debit'}>
                      {tx.type === 'CREDIT' ? <ArrowDownLeft size={18} /> : <ArrowUpRight size={18} />}
                    </div>
                    <div className="tx-copy">
                      <strong>{tx.description}</strong>
                      <span>{accounts.find((a) => a.id === tx.accountId)?.ownerName} · {tx.date}</span>
                      <small>Recorded by {tx.createdByName ?? 'Unknown member'}{tx.notes ? ` · ${tx.notes}` : ''}</small>
                    </div>
                    <strong className={tx.type === 'CREDIT' ? 'amount credit-text' : 'amount'}>
                      {tx.type === 'CREDIT' ? '+' : '-'}{money(tx.amountPaise)}
                    </strong>
                  </div>
                ))
              )}
            </div>
          </div>
          <div className="panel summary-panel">
            <div className="panel-heading">
              <div>
                <h2>{currentMonthName()} snapshot</h2>
                <p className="muted">Family activity this month</p>
              </div>
              <button className="icon-button" onClick={() => setActive('Reports')} aria-label="Open monthly reports">
                <CalendarDays size={18} />
              </button>
            </div>
            <div className="metric">
              <span>Credits received</span>
              <strong className="credit-text">+{money(monthCredits)}</strong>
            </div>
            <div className="metric">
              <span>Spending recorded</span>
              <strong>-{money(monthDebits)}</strong>
            </div>
            <div className="metric">
              <span>Needs outstanding</span>
              <strong>{money(needs.reduce((sum, need) => sum + need.remainingPaise, 0))}</strong>
            </div>
            <div className="summary-note">
              <Check size={16} /> Click the calendar to view all monthly snapshots.
            </div>
          </div>
        </section>

        <section className="panel needs-panel">
          <div className="panel-heading">
            <div>
              <h2>Needs requiring attention</h2>
              <p className="muted">Requests from the family</p>
            </div>
            <button className="text-button" onClick={() => setActive('Family Needs')}>
              See all <ChevronRight size={15} />
            </button>
          </div>
          <div className="needs-list">
            {needs.length === 0 ? (
              <p className="muted">No needs recorded yet.</p>
            ) : (
              needs.map((need) => (
                <div className="need-row" key={need.id}>
                  <div className="need-icon"><Send size={16} /></div>
                  <div className="tx-copy">
                    <strong>{need.cause}</strong>
                    <span>{need.requesterName} · due {need.requiredBy}</span>
                  </div>
                  <div className="need-progress">
                    <div>
                      <span>{money(need.fundedPaise)} funded</span>
                      <b>{Math.round(need.fundedPaise / need.amountPaise * 100)}%</b>
                    </div>
                    <div className="progress-track">
                      <i style={{ width: `${need.fundedPaise / need.amountPaise * 100}%` }} />
                    </div>
                  </div>
                  <span className="status">{need.status.replace('_', ' ')}</span>
                </div>
              ))
            )}
          </div>
        </section>

        {/* Fund-need modal */}
        {fundingNeed && (
          <div className="modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && setFundingNeed(null)}>
            <div className="modal mobile-modal">
              <div className="modal-heading">
                <div>
                  <p className="eyebrow">FUND NEED</p>
                  <h2>{fundingNeed.cause}</h2>
                </div>
                <button className="icon-button" onClick={() => setFundingNeed(null)} aria-label="Close">
                  <X size={18} />
                </button>
              </div>
              <p className="muted">Remaining: {money(fundingNeed.remainingPaise)}</p>
              <form onSubmit={submitFunding}>
                <label>
                  Amount (INR)
                  <input name="amount" type="number" min="0.01" max={fundingNeed.remainingPaise / 100} step="0.01" required autoFocus />
                </label>
                <button className="primary-button wide" type="submit">
                  <Check size={17} />Fund need
                </button>
              </form>
            </div>
          </div>
        )}
      </main>

      {/* Add-transaction modal */}
      {showAdd && (
        <div className="modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && setShowAdd(false)}>
          <div className="modal">
            <div className="modal-heading">
              <div>
                <p className="eyebrow">LEDGER ENTRY</p>
                <h2>Add transaction</h2>
              </div>
              <button className="icon-button" onClick={() => setShowAdd(false)} aria-label="Close">
                <X size={18} />
              </button>
            </div>
            <form onSubmit={addTransaction}>
              <label>
                Account
                <select name="account" defaultValue={accounts[0]?.id}>
                  {accounts.map((account) => (
                    <option key={account.id} value={account.id}>{account.ownerName}</option>
                  ))}
                </select>
              </label>
              <div className="form-grid">
                <label>
                  Type
                  <select name="type" defaultValue="CREDIT">
                    <option value="CREDIT">Credit</option>
                    <option value="DEBIT">Debit</option>
                  </select>
                </label>
                <label>
                  Amount (INR)
                  <input name="amount" type="number" min="0.01" step="0.01" required />
                </label>
              </div>
              <label>
                Date
                <input name="date" type="date" defaultValue={todayISO()} required />
              </label>
              <label>
                Description
                <input name="description" placeholder="e.g. Bus fare to college" required />
              </label>
              <label>
                Notes <span className="muted">(optional)</span>
                <textarea name="notes" rows={3} placeholder="Add context for the family record" />
              </label>
              <button className="primary-button wide" type="submit">
                <Check size={17} />Save transaction
              </button>
            </form>
          </div>
        </div>
      )}

      {/* Account-detail modal — FIX: duplicate modal removed, only one is rendered */}
      {selectedAccount && (
        <div className="modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && setSelectedAccount(null)}>
          <div className="modal">
            <div className="modal-heading">
              <div>
                <p className="eyebrow">ACCOUNT DETAILS</p>
                <h2>{selectedAccount.ownerName}</h2>
              </div>
              <button className="icon-button" onClick={() => setSelectedAccount(null)} aria-label="Close">
                <X size={18} />
              </button>
            </div>
            <div className="account-detail">
              <span className="label">CALCULATED BALANCE</span>
              <strong>{money(selectedAccount.balancePaise)}</strong>
              <p className="muted">Opening balance: {money(selectedAccount.openingBalancePaise)}</p>
              <button
                className="primary-button wide"
                onClick={() => { setSelectedAccount(null); setActive('Transactions') }}
              >
                View transactions
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}

export default App
