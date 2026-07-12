import { useEffect, useMemo, useState } from 'react'
import './App.css'

const BACKEND_URL = import.meta.env.VITE_BACKEND_URL || ''
const categories = [
  'WEDDING_HALL',
  'STUDIO',
  'DRESS',
  'MAKEUP',
  'FLOWER',
  'JEWELRY',
  'HANBOK',
  'RETURN_GIFT',
  'PHOTO',
  'VIDEO',
]
const scheduleTypes = [
  'CONSULTATION',
  'VENUE_TOUR',
  'DRESS_FITTING',
  'STUDIO_SHOOT',
  'MAKEUP',
  'WEDDING_DAY',
  'VENDOR_VISIT',
  'CONTRACT',
  'PERSONAL_TASK',
]
const targetTypes = ['CUSTOMER', 'VENDOR', 'PLANNER']
const tabs = [
  { id: 'dashboard', label: '대시보드' },
  { id: 'customers', label: '고객' },
  { id: 'vendors', label: '업체' },
  { id: 'schedules', label: '일정' },
  { id: 'agent', label: 'AI' },
  { id: 'team', label: '팀' },
]

const customerSubTabs = [
  { id: 'customer-add', label: '고객 등록' },
  { id: 'customer-list', label: '고객 목록' },
]

const vendorSubTabs = [
  { id: 'vendor-add', label: '업체 등록' },
  { id: 'vendor-list', label: '업체 목록' },
]

const scheduleSubTabs = [
  { id: 'schedule-add', label: '일정 등록' },
  { id: 'schedule-list', label: '일정 목록' },
]

async function api(path, options = {}) {
  const response = await fetch(path, {
    credentials: 'include',
    headers: {
      'Content-Type': 'application/json',
      ...(options.headers || {}),
    },
    ...options,
  })

  if (!response.ok) {
    const error = await response.json().catch(() => ({}))
    throw new Error(error.message || response.statusText)
  }

  return response.status === 204 ? null : response.json()
}

function compactPayload(payload) {
  return Object.fromEntries(
    Object.entries(payload).map(([key, value]) => [key, value === '' ? null : value]),
  )
}

function toDateTimeLocal(value) {
  return value ? String(value).slice(0, 16) : ''
}

function buildWeddingDate(data) {
  const mode = data.get('weddingDateMode')
  if (mode === 'undecided') return null
  const year = data.get('weddingYear')
  const month = data.get('weddingMonth')
  const day = data.get('weddingDay')
  if (!year || !month) return null
  if (mode === 'month') return `${year}-${String(month).padStart(2, '0')}-01`
  if (!day) return null
  return `${year}-${String(month).padStart(2, '0')}-${String(day).padStart(2, '0')}`
}

function customerPayload(data) {
  return {
    groomName: data.get('groomName'),
    brideName: data.get('brideName'),
    phoneNumber: data.get('phoneNumber'),
    residenceArea: data.get('residenceArea'),
    weddingDate: buildWeddingDate(data),
    preferredWeddingArea: data.get('preferredWeddingArea'),
    expectedGuestCount: Number(data.get('expectedGuestCount') || 0),
    totalBudget: Number(data.get('totalBudget') || 0),
    preferredAtmosphere: data.get('preferredAtmosphere'),
    preferredStyle: data.get('preferredStyle'),
    importantConditions: data.get('importantConditions'),
    avoidConditions: data.get('avoidConditions'),
    itemBudgetMemo: data.get('itemBudgetMemo'),
    consultationMemo: data.get('consultationMemo'),
    todoMemo: data.get('todoMemo'),
    completedMemo: data.get('completedMemo'),
  }
}

function vendorPayload(data, form) {
  const name = data.get('name') || ''
  return {
    kakaoPlaceId: data.get('kakaoPlaceId') || `manual-${name}`,
    name,
    category: data.get('category'),
    address: data.get('address'),
    roadAddress: data.get('roadAddress'),
    phone: data.get('phone'),
    latitude: data.get('latitude'),
    longitude: data.get('longitude'),
    placeUrl: data.get('placeUrl'),
    partnered: form.partnered.checked,
    contactPerson: data.get('contactPerson'),
    memo: data.get('memo'),
  }
}

function schedulePayload(data) {
  return {
    targetType: data.get('targetType'),
    targetId: Number(data.get('targetId')),
    scheduleType: data.get('scheduleType'),
    title: data.get('title'),
    startsAt: `${data.get('startsAt')}:00`,
    endsAt: `${data.get('endsAt')}:00`,
    location: data.get('location'),
  }
}

function App() {
  const [me, setMe] = useState(null)
  const [workspaceId, setWorkspaceId] = useState('')
  const [customers, setCustomers] = useState([])
  const [vendors, setVendors] = useState([])
  const [schedules, setSchedules] = useState([])
  const [members, setMembers] = useState([])
  const [invitations, setInvitations] = useState([])
  const [agentHistory, setAgentHistory] = useState([])
  const [recommendation, setRecommendation] = useState(null)
  const [selectedCustomer, setSelectedCustomer] = useState(null)
  const [selectedVendor, setSelectedVendor] = useState(null)
  const [editingCustomer, setEditingCustomer] = useState(null)
  const [editingVendor, setEditingVendor] = useState(null)
  const [editingSchedule, setEditingSchedule] = useState(null)
  const [selectedVendorId, setSelectedVendorId] = useState('')
  const [vendorExperiences, setVendorExperiences] = useState([])
  const [activeTab, setActiveTab] = useState('dashboard')
  const [customerSubTab, setCustomerSubTab] = useState('customer-add')
  const [vendorSubTab, setVendorSubTab] = useState('vendor-add')
  const [scheduleSubTab, setScheduleSubTab] = useState('schedule-add')
  const [selectedSchedule, setSelectedSchedule] = useState(null)
  const [status, setStatus] = useState('로그인 상태를 확인하고 있습니다.')
  const [loading, setLoading] = useState(true)
  const [inviteToken, setInviteToken] = useState(
    () => new URLSearchParams(window.location.search).get('inviteToken'),
  )
  const [inviteDetail, setInviteDetail] = useState(null)

  const currentWorkspace = useMemo(() => {
    return me?.workspaces?.find((workspace) => String(workspace.workspaceId) === String(workspaceId))
  }, [me, workspaceId])
  const canManageTeam = currentWorkspace?.role === 'OWNER' || currentWorkspace?.role === 'ADMIN'
  const latestAgentRecommendation = agentHistory.length
    ? agentHistory[agentHistory.length - 1].response.vendorRecommendation
    : null

  useEffect(() => {
    if (new URLSearchParams(window.location.search).has('loginError')) {
      setStatus('로그인에 실패했습니다. 잠시 후 다시 시도하거나 관리자에게 문의해 주세요.')
    }
    loadMe()
    if (inviteToken) {
      loadInvite(inviteToken)
    }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [])

  useEffect(() => {
    if (workspaceId) {
      loadWorkspaceData(workspaceId)
    }
    // oxlint-disable-next-line react-hooks/exhaustive-deps
  }, [workspaceId])

  async function loadMe() {
    setLoading(true)
    try {
      const data = await api('/api/me')
      const nextWorkspaceId = data.currentWorkspaceId || data.workspaces?.[0]?.workspaceId || ''
      setMe(data)
      setWorkspaceId(nextWorkspaceId)
      setStatus('백엔드 세션과 연결되었습니다.')
    } catch {
      setMe(null)
      setStatus('Google 로그인 후 업무 데이터를 확인할 수 있습니다.')
    } finally {
      setLoading(false)
    }
  }

  async function loadWorkspaceData(nextWorkspaceId = workspaceId) {
    if (!nextWorkspaceId) {
      return
    }

    setLoading(true)
    try {
      const selectedWorkspace = me?.workspaces?.find(
        (workspace) => String(workspace.workspaceId) === String(nextWorkspaceId),
      )
      const canLoadInvitations = selectedWorkspace?.role === 'OWNER' || selectedWorkspace?.role === 'ADMIN'
      const [customerData, vendorData, scheduleData, memberData] = await Promise.all([
        api(`/api/workspaces/${nextWorkspaceId}/customers`),
        api(`/api/workspaces/${nextWorkspaceId}/vendors`),
        api(`/api/workspaces/${nextWorkspaceId}/schedules`),
        api(`/api/workspaces/${nextWorkspaceId}/members`).catch(() => []),
      ])
      setCustomers(customerData)
      setVendors(vendorData)
      setSchedules(scheduleData)
      setMembers(memberData)

      if (canLoadInvitations) {
        setInvitations(await api(`/api/workspaces/${nextWorkspaceId}/invitations`).catch(() => []))
      } else {
        setInvitations([])
      }
      setStatus('워크스페이스 데이터를 불러왔습니다.')
    } catch (error) {
      setStatus(error.message)
    } finally {
      setLoading(false)
    }
  }

  async function submitForm(event, path, payloadBuilder, afterSubmit = () => loadWorkspaceData()) {
    event.preventDefault()
    const form = event.currentTarget
    setLoading(true)
    try {
      const payload = payloadBuilder(new FormData(form), form)
      const result = await api(path, {
        method: 'POST',
        body: JSON.stringify(compactPayload(payload)),
      })
      form.reset()
      await afterSubmit(result)
      setStatus('요청을 처리했습니다.')
    } catch (error) {
      setStatus(error.message)
    } finally {
      setLoading(false)
    }
  }

  async function updateForm(event, path, payloadBuilder, afterSubmit = () => loadWorkspaceData()) {
    event.preventDefault()
    const form = event.currentTarget
    setLoading(true)
    try {
      const payload = payloadBuilder(new FormData(form), form)
      await api(path, {
        method: 'PUT',
        body: JSON.stringify(compactPayload(payload)),
      })
      await afterSubmit()
      setStatus('수정했습니다.')
    } catch (error) {
      setStatus(error.message)
    } finally {
      setLoading(false)
    }
  }

  async function deleteResource(path, message) {
    if (!window.confirm(message)) {
      return
    }

    setLoading(true)
    try {
      await api(path, { method: 'DELETE' })
      await loadWorkspaceData()
      setStatus('삭제했습니다.')
    } catch (error) {
      setStatus(error.message)
    } finally {
      setLoading(false)
    }
  }

  async function updateMemberRole(targetUserId, role) {
    setLoading(true)
    try {
      await api(`/api/workspaces/${workspaceId}/members/${targetUserId}/role`, {
        method: 'PATCH',
        body: JSON.stringify({ role }),
      })
      await loadWorkspaceData()
      setStatus('멤버 역할을 변경했습니다.')
    } catch (error) {
      setStatus(error.message)
    } finally {
      setLoading(false)
    }
  }

  async function loadVendorExperiences(vendorId) {
    setSelectedVendorId(vendorId)
    if (!vendorId) {
      setVendorExperiences([])
      return
    }

    setLoading(true)
    try {
      const data = await api(`/api/workspaces/${workspaceId}/vendors/${vendorId}/experiences`)
      setVendorExperiences(data)
      setStatus('업체 경험을 불러왔습니다.')
    } catch (error) {
      setStatus(error.message)
    } finally {
      setLoading(false)
    }
  }

  async function submitAgentMessage(event) {
    event.preventDefault()
    const form = event.currentTarget
    const message = new FormData(form).get('message')
    if (!message) {
      return
    }
    setLoading(true)
    try {
      const response = await api(`/api/workspaces/${workspaceId}/agent`, {
        method: 'POST',
        body: JSON.stringify({ message }),
      })
      setAgentHistory((prev) => [...prev, { message, response }])
      form.reset()
      setStatus('AI Agent 응답을 받았습니다.')
    } catch (error) {
      setStatus(error.message)
    } finally {
      setLoading(false)
    }
  }

  async function loadInvite(token) {
    try {
      const data = await api(`/api/workspaces/invitations/${token}`)
      setInviteDetail(data)
    } catch (error) {
      setInviteDetail({ error: error.message })
    }
  }

  function clearInviteTokenFromUrl() {
    const url = new URL(window.location.href)
    url.searchParams.delete('inviteToken')
    window.history.replaceState({}, '', url)
    setInviteToken(null)
    setInviteDetail(null)
  }

  async function acceptInvite() {
    if (!inviteToken) {
      return
    }
    setLoading(true)
    try {
      await api(`/api/workspaces/invitations/${inviteToken}/accept`, { method: 'POST' })
      setStatus('초대를 수락했습니다. 워크스페이스가 전환되었습니다.')
      clearInviteTokenFromUrl()
      await loadMe()
    } catch (error) {
      setStatus(error.message)
    } finally {
      setLoading(false)
    }
  }

  function loginWithGoogle() {
    const query = inviteToken ? `?inviteToken=${encodeURIComponent(inviteToken)}` : ''
    window.location.href = `${BACKEND_URL}/oauth2/authorization/google${query}`
  }

  if (!me) {
    return (
      <div className="landing">
        <header className="landing-header">
          <span className="landing-logo">marry-it</span>
          <button type="button" className="login-google-btn" onClick={loginWithGoogle}>
            <GoogleIcon />
            Google로 시작하기
          </button>
        </header>

        {inviteToken && (
          <InviteBanner
            inviteDetail={inviteDetail}
            signedIn={false}
            loading={loading}
            onAcceptInvite={loginWithGoogle}
          />
        )}

        <section className="landing-hero">
          <p className="eyebrow">Wedding Planner Suite</p>
          <h1 className="landing-hero__title">웨딩 플래너의<br />모든 업무를 한 곳에서</h1>
          <p className="landing-hero__sub">고객 관리부터 업체 검색, 일정 조율, AI 추천까지<br />marry-it 하나로 처리하세요.</p>
          <button type="button" className="login-google-btn landing-hero__cta" onClick={loginWithGoogle}>
            <GoogleIcon />
            무료로 시작하기
          </button>
        </section>

        <section className="landing-features">
          <FeatureCard icon="👥" title="고객 관리" desc="신랑·신부 정보, 예산, 선호 스타일을 체계적으로 기록하고 관리합니다." />
          <FeatureCard icon="🏢" title="업체 관리" desc="제휴 업체를 등록하고 카카오맵으로 새 업체를 빠르게 검색합니다." />
          <FeatureCard icon="📅" title="일정 관리" desc="상담, 드레스 피팅, 웨딩 당일까지 모든 일정을 한눈에 확인합니다." />
          <FeatureCard icon="✨" title="AI 추천" desc="급한 상황에도 AI가 조건에 맞는 업체를 즉시 추천해 드립니다." />
        </section>
      </div>
    )
  }

  const activeCustomerTab = activeTab === 'customers' ? customerSubTab : null
  const activeVendorTab = activeTab === 'vendors' ? vendorSubTab : null
  const activeScheduleTab = activeTab === 'schedules' ? scheduleSubTab : null

  return (
    <main className="app-shell">
      <header className="topbar">
        <span className="topbar-logo">marry-it</span>
        <nav aria-label="주요 메뉴" className="tab-nav">
          {tabs.map((tab) => (
            <button
              key={tab.id}
              type="button"
              className={activeTab === tab.id ? 'active' : ''}
              onClick={() => setActiveTab(tab.id)}
            >
              {tab.label}
            </button>
          ))}
        </nav>
        <div className="topbar-actions">
          <select
            aria-label="워크스페이스 선택"
            value={workspaceId}
            onChange={(event) => setWorkspaceId(event.target.value)}
          >
            {me.workspaces.map((workspace) => (
              <option key={workspace.workspaceId} value={workspace.workspaceId}>
                {workspace.name}
              </option>
            ))}
          </select>
          <button type="button" className="secondary-button" onClick={() => loadWorkspaceData()} disabled={loading}>
            새로고침
          </button>
          <a className="button secondary" href={`${BACKEND_URL}/logout`}>
            로그아웃
          </a>
        </div>
      </header>

      {inviteToken && (
        <InviteBanner
          inviteDetail={inviteDetail}
          signedIn
          loading={loading}
          onAcceptInvite={acceptInvite}
          onDismiss={clearInviteTokenFromUrl}
        />
      )}

      <div className="body-layout">
        {activeTab === 'customers' && (
          <aside className="sub-sidebar">
            <p className="sub-sidebar__title">고객</p>
            <nav>
              {customerSubTabs.map((sub) => (
                <button
                  key={sub.id}
                  type="button"
                  className={customerSubTab === sub.id ? 'active' : ''}
                  onClick={() => setCustomerSubTab(sub.id)}
                >
                  {sub.label}
                </button>
              ))}
            </nav>
          </aside>
        )}
        {activeTab === 'vendors' && (
          <aside className="sub-sidebar">
            <p className="sub-sidebar__title">업체</p>
            <nav>
              {vendorSubTabs.map((sub) => (
                <button
                  key={sub.id}
                  type="button"
                  className={vendorSubTab === sub.id ? 'active' : ''}
                  onClick={() => setVendorSubTab(sub.id)}
                >
                  {sub.label}
                </button>
              ))}
            </nav>
          </aside>
        )}
        {activeTab === 'schedules' && (
          <aside className="sub-sidebar">
            <p className="sub-sidebar__title">일정</p>
            <nav>
              {scheduleSubTabs.map((sub) => (
                <button
                  key={sub.id}
                  type="button"
                  className={scheduleSubTab === sub.id ? 'active' : ''}
                  onClick={() => setScheduleSubTab(sub.id)}
                >
                  {sub.label}
                </button>
              ))}
            </nav>
          </aside>
        )}

      <section className="content">

        <>
            {activeTab === 'dashboard' && (
              <>
                <section className="workspace-summary">
                  <div>
                    <p className="eyebrow">Workspace</p>
                    <h2>{currentWorkspace?.name || '워크스페이스'}</h2>
                    <p>역할: {currentWorkspace?.role || '-'}</p>
                  </div>
                  <div className="metric-grid">
                    <Metric label="고객" value={customers.length} />
                    <Metric label="업체" value={vendors.length} />
                    <Metric label="일정" value={schedules.length} />
                  </div>
                </section>

                <section className="data-grid">
                  <PreviewList
                    title="최근 고객"
                    emptyMessage="등록된 고객이 없습니다."
                    items={customers}
                    renderItem={(customer) => `${customer.groomName || ''} & ${customer.brideName || ''}`}
                  />
                  <PreviewList
                    title="최근 업체"
                    emptyMessage="등록된 업체가 없습니다."
                    items={vendors}
                    renderItem={(vendor) => `${vendor.name} · ${vendor.category}`}
                  />
                  <PreviewList
                    title="다가오는 일정"
                    emptyMessage="등록된 일정이 없습니다."
                    items={schedules}
                    renderItem={(schedule) => `${schedule.title} · ${schedule.startsAt}`}
                  />
                </section>
              </>
            )}

            {activeCustomerTab === 'customer-add' && (
            <section className="tab-grid">
              <Panel title="고객 등록" id="customer-form">
                <form
                  className="stack-form"
                  onSubmit={(event) =>
                    submitForm(event, `/api/workspaces/${workspaceId}/customers`, customerPayload)
                  }
                >
                  <TwoColumns>
                    <input name="groomName" placeholder="신랑 이름" required />
                    <input name="brideName" placeholder="신부 이름" required />
                    <input name="phoneNumber" placeholder="연락처" />
                    <input name="residenceArea" placeholder="거주 지역" />
                    <input name="preferredWeddingArea" placeholder="희망 예식 지역" />
                    <input name="expectedGuestCount" type="number" min="0" placeholder="예상 하객 수" />
                    <input name="totalBudget" type="number" min="0" placeholder="총예산" />
                  </TwoColumns>
                  <WeddingDatePicker />
                  <textarea name="preferredAtmosphere" placeholder="선호 분위기" />
                  <textarea name="preferredStyle" placeholder="선호 스타일" />
                  <textarea name="importantConditions" placeholder="중요 조건" />
                  <textarea name="avoidConditions" placeholder="피하고 싶은 조건" />
                  <textarea name="itemBudgetMemo" placeholder="항목별 예산 메모" />
                  <textarea name="consultationMemo" placeholder="상담 메모" />
                  <textarea name="todoMemo" placeholder="해야 할 일" />
                  <textarea name="completedMemo" placeholder="완료한 일" />
                  <button type="submit" disabled={loading}>
                    고객 저장
                  </button>
                </form>
              </Panel>
            </section>
            )}

            {activeCustomerTab === 'customer-list' && (
            <section className="tab-grid">
              <Panel title="고객 목록" id="customer-list">
                <ManageList
                  items={customers}
                  emptyMessage="등록된 고객이 없습니다."
                  renderLabel={(customer) =>
                    `${customer.groomName || ''} & ${customer.brideName || ''} ${customer.weddingDate ? '· ' + formatWeddingDate(customer.weddingDate) : ''}`
                  }
                  onEdit={(customer) => { setEditingCustomer(customer); setSelectedCustomer(customer) }}
                  onDelete={(customer) =>
                    deleteResource(
                      `/api/workspaces/${workspaceId}/customers/${customer.id}`,
                      `${customer.groomName} & ${customer.brideName} 고객을 삭제할까요?`,
                    )
                  }
                  onSelect={(customer) => { setSelectedCustomer(customer); setEditingCustomer(null) }}
                  selectedId={selectedCustomer?.id}
                />
              </Panel>
              <Panel title={selectedCustomer ? `${selectedCustomer.groomName} & ${selectedCustomer.brideName}` : '고객 상세'} id="customer-detail">
                {selectedCustomer && !editingCustomer ? (
                  <CustomerDetailView
                    customer={selectedCustomer}
                    onEdit={(c) => setEditingCustomer(c)}
                    onDelete={(customer) =>
                      deleteResource(
                        `/api/workspaces/${workspaceId}/customers/${customer.id}`,
                        `${customer.groomName} & ${customer.brideName} 고객을 삭제할까요?`,
                      ).then(() => setSelectedCustomer(null))
                    }
                  />
                ) : editingCustomer ? (
                  <CustomerEditForm
                    customer={editingCustomer}
                    loading={loading}
                    onCancel={() => setEditingCustomer(null)}
                    onSubmit={(event) =>
                      updateForm(
                        event,
                        `/api/workspaces/${workspaceId}/customers/${editingCustomer.id}`,
                        customerPayload,
                        async () => {
                          setEditingCustomer(null)
                          await loadWorkspaceData()
                        },
                      )
                    }
                  />
                ) : (
                  <div className="customer-detail__empty">
                    <span className="customer-detail__empty-icon">👤</span>
                    <span>목록에서 고객을 선택하세요</span>
                  </div>
                )}
              </Panel>
            </section>
            )}

            {activeVendorTab === 'vendor-add' && (
            <section className="tab-grid">
              <Panel title="업체 등록" id="vendor-form">
                <VendorAddForm
                  workspaceId={workspaceId}
                  loading={loading}
                  onSubmit={(event) =>
                    submitForm(event, `/api/workspaces/${workspaceId}/vendors`, vendorPayload)
                  }
                />
              </Panel>
            </section>
            )}

            {activeVendorTab === 'vendor-list' && (
            <section className="tab-grid">
              <Panel title="업체 목록" id="vendor-list">
                <ManageList
                  items={vendors}
                  emptyMessage="등록된 업체가 없습니다."
                  renderLabel={(vendor) => `${vendor.name} · ${vendor.category}${vendor.partnered ? ' · 제휴' : ''}`}
                  onEdit={(vendor) => { setEditingVendor(vendor); setSelectedVendor(vendor) }}
                  onDelete={(vendor) =>
                    deleteResource(
                      `/api/workspaces/${workspaceId}/vendors/${vendor.id}`,
                      `${vendor.name} 업체를 삭제할까요?`,
                    )
                  }
                  onSelect={(vendor) => { setSelectedVendor(vendor); setEditingVendor(null) }}
                  selectedId={selectedVendor?.id}
                />
              </Panel>
              <Panel title={selectedVendor ? selectedVendor.name : '업체 상세'} id="vendor-detail">
                {selectedVendor && !editingVendor ? (
                  <VendorDetailView
                    vendor={selectedVendor}
                    workspaceId={workspaceId}
                    loading={loading}
                    onEdit={(v) => setEditingVendor(v)}
                    onDelete={(vendor) =>
                      deleteResource(
                        `/api/workspaces/${workspaceId}/vendors/${vendor.id}`,
                        `${vendor.name} 업체를 삭제할까요?`,
                      ).then(() => setSelectedVendor(null))
                    }
                    submitForm={submitForm}
                  />
                ) : editingVendor ? (
                  <VendorEditForm
                    vendor={editingVendor}
                    loading={loading}
                    onCancel={() => setEditingVendor(null)}
                    onSubmit={(event) =>
                      updateForm(
                        event,
                        `/api/workspaces/${workspaceId}/vendors/${editingVendor.id}`,
                        vendorPayload,
                        async () => {
                          setEditingVendor(null)
                          await loadWorkspaceData()
                        },
                      )
                    }
                  />
                ) : (
                  <div className="customer-detail__empty">
                    <span className="customer-detail__empty-icon">🏢</span>
                    <span>목록에서 업체를 선택하세요</span>
                  </div>
                )}
              </Panel>
            </section>
            )}

            {activeScheduleTab === 'schedule-add' && (
            <section className="tab-grid">
              <Panel title="일정 등록" id="schedules">
                <ScheduleAddForm
                  customers={customers}
                  vendors={vendors}
                  loading={loading}
                  onSubmit={(event) =>
                    submitForm(event, `/api/workspaces/${workspaceId}/schedules`, schedulePayload)
                  }
                />
              </Panel>
            </section>
            )}

            {activeScheduleTab === 'schedule-list' && (
            <section className="tab-grid">
              <Panel title="일정 목록" id="schedule-list">
                <ManageList
                  items={schedules}
                  emptyMessage="등록된 일정이 없습니다."
                  renderLabel={(schedule) => `${schedule.title} · ${schedule.startsAt?.slice(0, 16)}`}
                  onEdit={(schedule) => { setEditingSchedule(schedule); setSelectedSchedule(schedule) }}
                  onDelete={(schedule) =>
                    deleteResource(
                      `/api/workspaces/${workspaceId}/schedules/${schedule.id}`,
                      `${schedule.title} 일정을 삭제할까요?`,
                    )
                  }
                  onSelect={(schedule) => { setSelectedSchedule(schedule); setEditingSchedule(null) }}
                  selectedId={selectedSchedule?.id}
                />
              </Panel>
              <Panel title={selectedSchedule ? selectedSchedule.title : '일정 상세'} id="schedule-detail">
                {selectedSchedule && !editingSchedule ? (
                  <ScheduleDetailView
                    schedule={selectedSchedule}
                    customers={customers}
                    vendors={vendors}
                    onEdit={(s) => setEditingSchedule(s)}
                    onDelete={(schedule) =>
                      deleteResource(
                        `/api/workspaces/${workspaceId}/schedules/${schedule.id}`,
                        `${schedule.title} 일정을 삭제할까요?`,
                      ).then(() => setSelectedSchedule(null))
                    }
                  />
                ) : editingSchedule ? (
                  <ScheduleEditForm
                    schedule={editingSchedule}
                    customers={customers}
                    vendors={vendors}
                    loading={loading}
                    onCancel={() => setEditingSchedule(null)}
                    onSubmit={(event) =>
                      updateForm(
                        event,
                        `/api/workspaces/${workspaceId}/schedules/${editingSchedule.id}`,
                        schedulePayload,
                        async () => {
                          setEditingSchedule(null)
                          await loadWorkspaceData()
                        },
                      )
                    }
                  />
                ) : (
                  <div className="customer-detail__empty">
                    <span className="customer-detail__empty-icon">📅</span>
                    <span>목록에서 일정을 선택하세요</span>
                  </div>
                )}
              </Panel>
            </section>
            )}

            {activeTab === 'agent' && (
            <section className="tab-grid">
              {activeTab === 'agent' && (
              <Panel title="AI 추천 결과 상세">
                {latestAgentRecommendation ? (
                  <RecommendationResult recommendation={latestAgentRecommendation} />
                ) : (
                  <p className="empty">AI Agent 요청 결과에 추천 후보가 있으면 여기에 표시됩니다.</p>
                )}
              </Panel>
              )}
            </section>
            )}

            {activeTab === 'agent' && (
            <section className="tab-grid">
              <Panel title="AI Agent" id="agent">
                <form className="stack-form" onSubmit={submitAgentMessage}>
                  <textarea
                    name="message"
                    placeholder="예: 서영 커플에게 어울리는 업체 조합 추천해줘. / 부케 업체가 갑자기 취소됐어. 대체 업체 찾아줘."
                    required
                  />
                  <button type="submit" disabled={loading}>
                    AI에게 요청
                  </button>
                </form>
                {agentHistory.length === 0 ? (
                  <p className="empty">아직 AI Agent에게 요청한 내역이 없습니다.</p>
                ) : (
                  <ul className="agent-history">
                    {agentHistory
                      .slice()
                      .reverse()
                      .map((entry, index) => (
                        <li key={agentHistory.length - index} className="agent-history__item">
                          <p className="agent-history__question">{entry.message}</p>
                          <p className="agent-history__answer">{entry.response.answer}</p>
                          {entry.response.toolCalls?.length > 0 && (
                            <details className="agent-history__tools">
                              <summary>AI가 확인한 내용 ({entry.response.toolCalls.length}건)</summary>
                              <ul>
                                {entry.response.toolCalls.map((call, callIndex) => (
                                  <li key={callIndex}>
                                    <code>{call.tool}</code> {call.arguments}
                                  </li>
                                ))}
                              </ul>
                            </details>
                          )}
                        </li>
                      ))}
                  </ul>
                )}
              </Panel>

              <Panel title="업체 추천" id="recommendation">
                <form
                  className="stack-form"
                  onSubmit={(event) =>
                    submitForm(
                      event,
                      `/api/workspaces/${workspaceId}/recommendations/vendors`,
                      (data, form) => ({
                        category: data.get('category'),
                        areaKeyword: data.get('areaKeyword'),
                        includeExternalSearch: form.includeExternalSearch.checked,
                      }),
                      (result) => setRecommendation(result),
                    )
                  }
                >
                  <TwoColumns>
                    <select name="category" defaultValue="WEDDING_HALL">
                      {categories.map((category) => (
                        <option key={category} value={category}>
                          {category}
                        </option>
                      ))}
                    </select>
                    <input name="areaKeyword" placeholder="지역 키워드" />
                  </TwoColumns>
                  <label className="check-row">
                    <input name="includeExternalSearch" type="checkbox" />
                    외부 업체 후보 포함
                  </label>
                  <button type="submit" disabled={loading}>
                    추천 받기
                  </button>
                </form>
                {recommendation && <RecommendationResult recommendation={recommendation} />}
              </Panel>
            </section>
            )}

            {activeTab === 'team' && (
            <section className="tab-grid" id="team">
              <Panel title="팀 멤버">
                <ResultList
                  items={members}
                  emptyMessage="멤버 정보를 불러오지 못했거나 멤버가 없습니다."
                  renderItem={(member) => (
                    <>
                      <strong>{member.name || member.email || `User ${member.userId}`}</strong>
                      <span>{member.role}</span>
                      {canManageTeam && (
                        <div className="member-actions">
                          <select
                            defaultValue={member.role}
                            onChange={(event) => updateMemberRole(member.userId, event.target.value)}
                            disabled={loading}
                          >
                            <option value="OWNER">OWNER</option>
                            <option value="ADMIN">ADMIN</option>
                            <option value="MEMBER">MEMBER</option>
                          </select>
                          <button
                            type="button"
                            className="small-button danger"
                            disabled={loading}
                            onClick={() =>
                              deleteResource(
                                `/api/workspaces/${workspaceId}/members/${member.userId}`,
                                `${member.name || member.email || member.userId} 멤버를 제거할까요?`,
                              )
                            }
                          >
                            제거
                          </button>
                        </div>
                      )}
                    </>
                  )}
                />
              </Panel>

              <Panel title="워크스페이스 초대">
                {canManageTeam ? (
                  <>
                    <form
                      className="stack-form"
                      onSubmit={(event) =>
                        submitForm(event, `/api/workspaces/${workspaceId}/invitations`, (data) => ({
                          invitedEmail: data.get('invitedEmail'),
                          role: data.get('role'),
                        }))
                      }
                    >
                      <TwoColumns>
                        <input name="invitedEmail" type="email" placeholder="초대 이메일" required />
                        <select name="role" defaultValue="MEMBER">
                          <option value="ADMIN">ADMIN</option>
                          <option value="MEMBER">MEMBER</option>
                        </select>
                      </TwoColumns>
                      <button type="submit" disabled={loading}>
                        초대 생성
                      </button>
                    </form>
                    <ResultList
                      items={invitations}
                      emptyMessage="초대 내역이 없습니다."
                      renderItem={(invitation) => (
                        <>
                          <strong>{invitation.invitedEmail}</strong>
                          <span>{invitation.role} · {invitation.status}</span>
                        </>
                      )}
                    />
                  </>
                ) : (
                  <p className="empty">OWNER 또는 ADMIN만 초대를 관리할 수 있습니다.</p>
                )}
              </Panel>
            </section>
            )}
          </>
      </section>
      </div>
    </main>
  )
}

function GoogleIcon() {
  return (
    <svg width="18" height="18" viewBox="0 0 18 18" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
      <path d="M17.64 9.205c0-.639-.057-1.252-.164-1.841H9v3.481h4.844a4.14 4.14 0 0 1-1.796 2.716v2.259h2.908c1.702-1.567 2.684-3.875 2.684-6.615Z" fill="#4285F4"/>
      <path d="M9 18c2.43 0 4.467-.806 5.956-2.18l-2.908-2.259c-.806.54-1.837.86-3.048.86-2.344 0-4.328-1.584-5.036-3.711H.957v2.332A8.997 8.997 0 0 0 9 18Z" fill="#34A853"/>
      <path d="M3.964 10.71A5.41 5.41 0 0 1 3.682 9c0-.593.102-1.17.282-1.71V4.958H.957A8.996 8.996 0 0 0 0 9c0 1.452.348 2.827.957 4.042l3.007-2.332Z" fill="#FBBC05"/>
      <path d="M9 3.58c1.321 0 2.508.454 3.44 1.345l2.582-2.58C13.463.891 11.426 0 9 0A8.997 8.997 0 0 0 .957 4.958L3.964 6.29C4.672 4.163 6.656 3.58 9 3.58Z" fill="#EA4335"/>
    </svg>
  )
}

function roleLabel(role) {
  if (role === 'OWNER') return '대표'
  if (role === 'ADMIN') return '관리자'
  return '멤버'
}

function InviteBanner({ inviteDetail, signedIn, loading, onAcceptInvite, onDismiss }) {
  return (
    <section className="invite-banner">
      {!inviteDetail ? (
        <p>초대 내용을 확인하는 중입니다…</p>
      ) : inviteDetail.error ? (
        <p>{inviteDetail.error}</p>
      ) : (
        <>
          <div>
            <strong>{inviteDetail.workspaceName} 워크스페이스 초대</strong>
            <p>
              {inviteDetail.invitedEmail} 계정으로 {roleLabel(inviteDetail.role)} 권한 초대가 도착했습니다.
              {inviteDetail.status !== 'PENDING' && ' 이미 처리된 초대입니다.'}
            </p>
          </div>
          <div className="invite-banner__actions">
            {inviteDetail.status === 'PENDING' && (
              <button type="button" onClick={onAcceptInvite} disabled={loading}>
                {signedIn ? '초대 수락' : 'Google로 로그인하고 수락'}
              </button>
            )}
            {onDismiss && (
              <button type="button" className="secondary-button" onClick={onDismiss}>
                닫기
              </button>
            )}
          </div>
        </>
      )}
    </section>
  )
}

function FeatureCard({ icon, title, desc }) {
  return (
    <div className="feature-card">
      <span className="feature-card__icon">{icon}</span>
      <h3 className="feature-card__title">{title}</h3>
      <p className="feature-card__desc">{desc}</p>
    </div>
  )
}

function Metric({ label, value }) {
  return (
    <div className="metric">
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  )
}

function Panel({ id, title, children }) {
  return (
    <article className="panel" id={id}>
      <h3>{title}</h3>
      {children}
    </article>
  )
}

function TwoColumns({ children }) {
  return <div className="two-columns">{children}</div>
}

function PreviewList({ id, title, emptyMessage, items, renderItem }) {
  return (
    <article className="preview-list" id={id}>
      <h3>{title}</h3>
      {items.length === 0 ? (
        <p className="empty">{emptyMessage}</p>
      ) : (
        <ul>
          {items.slice(0, 5).map((item) => (
            <li key={item.id}>{renderItem(item)}</li>
          ))}
        </ul>
      )}
    </article>
  )
}

function ResultList({ items, emptyMessage, renderItem }) {
  if (items.length === 0) {
    return <p className="empty">{emptyMessage}</p>
  }

  return (
    <ul className="result-list">
      {items.map((item, index) => (
        <li key={item.id || item.kakaoPlaceId || item.token || index}>{renderItem(item)}</li>
      ))}
    </ul>
  )
}

function ManageList({ items, emptyMessage, renderLabel, onEdit, onDelete, onSelect, selectedId }) {
  if (items.length === 0) {
    return <p className="empty">{emptyMessage}</p>
  }

  return (
    <ul className="manage-list">
      {items.map((item) => (
        <li key={item.id} className={selectedId === item.id ? 'selected' : ''}>
          <span
            className={onSelect ? 'manage-list__label clickable' : 'manage-list__label'}
            onClick={() => onSelect?.(item)}
          >
            {renderLabel(item)}
          </span>
          <div className="manage-list__actions">
            <button type="button" className="small-button" onClick={() => onEdit(item)}>
              수정
            </button>
            <button type="button" className="small-button danger" onClick={() => onDelete(item)}>
              삭제
            </button>
          </div>
        </li>
      ))}
    </ul>
  )
}

function formatWeddingDate(dateStr) {
  if (!dateStr) return '미정'
  const [year, month, day] = dateStr.split('-')
  if (day === '01' || !day) return `${year}년 ${parseInt(month)}월`
  return `${year}년 ${parseInt(month)}월 ${parseInt(day)}일`
}

function WeddingDatePicker({ defaultValue }) {
  const [mode, setMode] = useState(() => {
    if (!defaultValue) return 'undecided'
    const parts = defaultValue.split('-')
    return parts[2] === '01' ? 'month' : 'day'
  })
  const [year, setYear] = useState(() => defaultValue ? defaultValue.split('-')[0] : '')
  const [month, setMonth] = useState(() => defaultValue ? String(parseInt(defaultValue.split('-')[1])) : '')
  const [day, setDay] = useState(() => {
    if (!defaultValue) return ''
    const d = defaultValue.split('-')[2]
    return d === '01' ? '' : String(parseInt(d))
  })

  return (
    <div className="wedding-date-picker">
      <p className="wedding-date-picker__label">결혼 날짜</p>
      <div className="wedding-date-picker__modes">
        {[['undecided', '미정'], ['month', '년·월'], ['day', '년·월·일']].map(([val, label]) => (
          <label key={val} className="radio-option">
            <input type="radio" name="weddingDateMode" value={val} checked={mode === val} onChange={() => setMode(val)} />
            {label}
          </label>
        ))}
      </div>
      {mode !== 'undecided' && (
        <div className="wedding-date-picker__inputs">
          <input
            type="number" name="weddingYear" placeholder="년도" min="2020" max="2035"
            value={year} onChange={(e) => setYear(e.target.value)}
          />
          <select name="weddingMonth" value={month} onChange={(e) => setMonth(e.target.value)}>
            <option value="">월</option>
            {Array.from({ length: 12 }, (_, i) => i + 1).map((m) => (
              <option key={m} value={m}>{m}월</option>
            ))}
          </select>
          {mode === 'day' && (
            <select name="weddingDay" value={day} onChange={(e) => setDay(e.target.value)}>
              <option value="">일</option>
              {Array.from({ length: 31 }, (_, i) => i + 1).map((d) => (
                <option key={d} value={d}>{d}일</option>
              ))}
            </select>
          )}
        </div>
      )}
    </div>
  )
}

function CustomerDetailView({ customer, onEdit, onDelete }) {
  const fields = [
    ['연락처', customer.phoneNumber],
    ['거주 지역', customer.residenceArea],
    ['결혼 날짜', formatWeddingDate(customer.weddingDate)],
    ['희망 예식 지역', customer.preferredWeddingArea],
    ['예상 하객 수', customer.expectedGuestCount != null ? `${customer.expectedGuestCount}명` : null],
    ['총예산', customer.totalBudget != null ? `${customer.totalBudget.toLocaleString()}원` : null],
    ['선호 분위기', customer.preferredAtmosphere],
    ['선호 스타일', customer.preferredStyle],
    ['중요 조건', customer.importantConditions],
    ['피하고 싶은 조건', customer.avoidConditions],
    ['항목별 예산 메모', customer.itemBudgetMemo],
    ['상담 메모', customer.consultationMemo],
    ['해야 할 일', customer.todoMemo],
    ['완료한 일', customer.completedMemo],
  ]

  return (
    <div className="customer-detail">
      <div className="customer-detail__header">
        <h4>{customer.groomName} &amp; {customer.brideName}</h4>
        <div className="customer-detail__actions">
          <button type="button" className="small-button" onClick={() => onEdit(customer)}>수정</button>
          <button type="button" className="small-button danger" onClick={() => onDelete(customer)}>삭제</button>
        </div>
      </div>
      <dl className="customer-detail__fields">
        {fields.filter(([, v]) => v != null && v !== '' && v !== '미정').map(([label, value]) => (
          <div key={label} className="customer-detail__field">
            <dt>{label}</dt>
            <dd>{value}</dd>
          </div>
        ))}
      </dl>
    </div>
  )
}

function CustomerEditForm({ customer, loading, onSubmit, onCancel }) {
  return (
    <form key={customer.id} className="stack-form edit-form" onSubmit={onSubmit}>
      <TwoColumns>
        <input name="groomName" placeholder="신랑 이름" defaultValue={customer.groomName || ''} required />
        <input name="brideName" placeholder="신부 이름" defaultValue={customer.brideName || ''} required />
        <input name="phoneNumber" placeholder="연락처" defaultValue={customer.phoneNumber || ''} />
        <input name="residenceArea" placeholder="거주 지역" defaultValue={customer.residenceArea || ''} />
        <input name="preferredWeddingArea" placeholder="희망 예식 지역" defaultValue={customer.preferredWeddingArea || ''} />
        <input
          name="expectedGuestCount"
          type="number"
          min="0"
          placeholder="예상 하객 수"
          defaultValue={customer.expectedGuestCount || 0}
        />
        <input name="totalBudget" type="number" min="0" placeholder="총예산" defaultValue={customer.totalBudget || 0} />
      </TwoColumns>
      <WeddingDatePicker defaultValue={customer.weddingDate || ''} />
      <textarea name="preferredAtmosphere" placeholder="선호 분위기" defaultValue={customer.preferredAtmosphere || ''} />
      <textarea name="preferredStyle" placeholder="선호 스타일" defaultValue={customer.preferredStyle || ''} />
      <textarea name="importantConditions" placeholder="중요 조건" defaultValue={customer.importantConditions || ''} />
      <textarea name="avoidConditions" placeholder="피하고 싶은 조건" defaultValue={customer.avoidConditions || ''} />
      <textarea name="itemBudgetMemo" placeholder="항목별 예산 메모" defaultValue={customer.itemBudgetMemo || ''} />
      <textarea name="consultationMemo" placeholder="상담 메모" defaultValue={customer.consultationMemo || ''} />
      <textarea name="todoMemo" placeholder="해야 할 일" defaultValue={customer.todoMemo || ''} />
      <textarea name="completedMemo" placeholder="완료한 일" defaultValue={customer.completedMemo || ''} />
      <div className="action-row">
        <button type="submit" disabled={loading}>
          수정 저장
        </button>
        <button type="button" className="secondary-button" onClick={onCancel}>
          취소
        </button>
      </div>
    </form>
  )
}

const categoryLabel = {
  WEDDING_HALL: '웨딩홀',
  STUDIO: '스튜디오',
  DRESS: '드레스',
  MAKEUP: '메이크업',
  FLOWER: '플라워',
  JEWELRY: '주얼리',
  HANBOK: '한복',
  RETURN_GIFT: '답례품',
  PHOTO: '사진',
  VIDEO: '영상',
}

function VendorDetailView({ vendor, workspaceId, loading, onEdit, onDelete, submitForm }) {
  const [experiences, setExperiences] = useState([])
  const [expLoading, setExpLoading] = useState(false)

  useEffect(() => {
    setExpLoading(true)
    api(`/api/workspaces/${workspaceId}/vendors/${vendor.id}/experiences`)
      .then(setExperiences)
      .catch(() => setExperiences([]))
      .finally(() => setExpLoading(false))
  }, [vendor.id, workspaceId])

  const fields = [
    ['카테고리', categoryLabel[vendor.category] || vendor.category],
    ['담당자', vendor.contactPerson],
    ['전화번호', vendor.phone],
    ['도로명 주소', vendor.roadAddress],
    ['주소', vendor.address],
    ['제휴 여부', vendor.partnered ? '제휴 업체' : null],
    ['메모', vendor.memo],
    ['카카오 장소 ID', vendor.kakaoPlaceId],
    ['카카오 URL', vendor.placeUrl],
  ]

  return (
    <div className="customer-detail">
      <div className="customer-detail__header">
        <h4>{vendor.name}</h4>
        <div className="customer-detail__actions">
          <button type="button" className="small-button" onClick={() => onEdit(vendor)}>수정</button>
          <button type="button" className="small-button danger" onClick={() => onDelete(vendor)}>삭제</button>
        </div>
      </div>
      <dl className="customer-detail__fields">
        {fields.filter(([, v]) => v != null && v !== '').map(([label, value]) => (
          <div key={label} className="customer-detail__field">
            <dt>{label}</dt>
            <dd>{value}</dd>
          </div>
        ))}
      </dl>
      <div>
        <p className="wedding-date-picker__label" style={{ marginBottom: 8 }}>경험 · 노하우</p>
        <form
          className="stack-form"
          onSubmit={(event) =>
            submitForm(
              event,
              `/api/workspaces/${workspaceId}/vendors/${vendor.id}/experiences`,
              (data) => ({ content: data.get('content') }),
              async () => {
                const data = await api(`/api/workspaces/${workspaceId}/vendors/${vendor.id}/experiences`)
                setExperiences(data)
              },
            )
          }
        >
          <textarea name="content" placeholder="예: 급한 주문 대응이 빠르고 화이트톤 부케를 잘함" required />
          <button type="submit" disabled={loading}>경험 저장</button>
        </form>
        {expLoading ? (
          <p className="empty">불러오는 중…</p>
        ) : (
          <ResultList
            items={experiences}
            emptyMessage="등록된 경험이 없습니다."
            renderItem={(experience) => (
              <>
                <strong>{experience.plannerName}</strong>
                <span>{experience.content}</span>
              </>
            )}
          />
        )}
      </div>
    </div>
  )
}

function VendorAddForm({ workspaceId, loading, onSubmit }) {
  const [query, setQuery] = useState('')
  const [results, setResults] = useState([])
  const [searching, setSearching] = useState(false)
  const [place, setPlace] = useState(null)

  async function handleSearch(event) {
    event.preventDefault()
    if (!query.trim()) return
    setSearching(true)
    try {
      const data = await api(
        `/api/workspaces/${workspaceId}/external/kakao/places?query=${encodeURIComponent(query)}`,
      )
      setResults(data)
    } catch {
      setResults([])
    } finally {
      setSearching(false)
    }
  }

  function selectPlace(p) {
    setPlace(p)
    setResults([])
  }

  return (
    <div className="vendor-add-form">
      <div className="vendor-search-box">
        <p className="wedding-date-picker__label">카카오맵으로 검색 <span className="optional-hint">(선택)</span></p>
        <form className="inline-form" onSubmit={handleSearch}>
          <input
            value={query}
            onChange={(e) => setQuery(e.target.value)}
            placeholder="예: 강남 웨딩홀, 청담 드레스"
          />
          <button type="submit" disabled={searching}>검색</button>
        </form>
        {results.length > 0 && (
          <ul className="kakao-result-list">
            {results.map((r) => (
              <li key={r.kakaoPlaceId}>
                <button type="button" className="kakao-result-item" onClick={() => selectPlace(r)}>
                  <strong>{r.name}</strong>
                  <span>{r.roadAddress || r.address}</span>
                </button>
              </li>
            ))}
          </ul>
        )}
        {place && (
          <div className="kakao-selected-badge">
            <span>선택됨: <strong>{place.name}</strong></span>
            <button type="button" className="small-button" onClick={() => setPlace(null)}>해제</button>
          </div>
        )}
      </div>

      <form key={place?.kakaoPlaceId || 'manual'} className="stack-form" onSubmit={onSubmit}>
        <TwoColumns>
          <input name="name" placeholder="업체명" defaultValue={place?.name || ''} required />
          <select name="category" defaultValue="WEDDING_HALL">
            {categories.map((c) => (
              <option key={c} value={c}>{categoryLabel[c] || c}</option>
            ))}
          </select>
          <input name="contactPerson" placeholder="담당자" />
          <input name="phone" placeholder="전화번호" defaultValue={place?.phone || ''} />
          <input name="roadAddress" placeholder="도로명 주소" defaultValue={place?.roadAddress || ''} />
          <input name="address" placeholder="주소" defaultValue={place?.address || ''} />
        </TwoColumns>
        <input name="kakaoPlaceId" type="hidden" defaultValue={place?.kakaoPlaceId || ''} />
        <input name="placeUrl" type="hidden" defaultValue={place?.placeUrl || ''} />
        <input name="latitude" type="hidden" defaultValue={place?.latitude || ''} />
        <input name="longitude" type="hidden" defaultValue={place?.longitude || ''} />
        <label className="check-row">
          <input name="partnered" type="checkbox" />
          제휴 업체
        </label>
        <textarea name="memo" placeholder="간략한 특징 (예: 빠른 대응, 화이트톤 강점, 주차 협소)" />
        <button type="submit" disabled={loading}>업체 저장</button>
      </form>
    </div>
  )
}

function VendorEditForm({ vendor, loading, onSubmit, onCancel }) {
  return (
    <form key={vendor.id} className="stack-form edit-form" onSubmit={onSubmit}>
      <TwoColumns>
        <input name="kakaoPlaceId" placeholder="카카오 장소 ID" defaultValue={vendor.kakaoPlaceId || ''} required />
        <input name="name" placeholder="업체명" defaultValue={vendor.name || ''} required />
        <select name="category" defaultValue={vendor.category || 'WEDDING_HALL'}>
          {categories.map((category) => (
            <option key={category} value={category}>
              {category}
            </option>
          ))}
        </select>
        <input name="contactPerson" placeholder="담당자" defaultValue={vendor.contactPerson || ''} />
        <input name="phone" placeholder="전화번호" defaultValue={vendor.phone || ''} />
        <input name="roadAddress" placeholder="도로명 주소" defaultValue={vendor.roadAddress || ''} />
        <input name="address" placeholder="주소" defaultValue={vendor.address || ''} />
        <input name="placeUrl" placeholder="카카오 장소 URL" defaultValue={vendor.placeUrl || ''} />
        <input name="latitude" placeholder="위도" defaultValue={vendor.latitude || ''} />
        <input name="longitude" placeholder="경도" defaultValue={vendor.longitude || ''} />
      </TwoColumns>
      <label className="check-row">
        <input name="partnered" type="checkbox" defaultChecked={vendor.partnered} />
        제휴 업체
      </label>
      <textarea name="memo" placeholder="간략한 특징" defaultValue={vendor.memo || ''} />
      <div className="action-row">
        <button type="submit" disabled={loading}>
          수정 저장
        </button>
        <button type="button" className="secondary-button" onClick={onCancel}>
          취소
        </button>
      </div>
    </form>
  )
}

const scheduleTypeLabel = {
  CONSULTATION: '상담',
  VENUE_TOUR: '예식장 투어',
  DRESS_FITTING: '드레스 피팅',
  STUDIO_SHOOT: '스튜디오 촬영',
  MAKEUP: '메이크업',
  WEDDING_DAY: '웨딩 당일',
  VENDOR_VISIT: '업체 방문',
  CONTRACT: '계약',
  PERSONAL_TASK: '개인 업무',
}

const targetTypeLabel = {
  CUSTOMER: '고객',
  VENDOR: '업체',
  PLANNER: '플래너',
}

function TargetSelect({ customers, vendors, defaultType, defaultId }) {
  const [type, setType] = useState(defaultType || 'CUSTOMER')

  const options = type === 'CUSTOMER'
    ? customers.map((c) => ({ value: c.id, label: `${c.groomName} & ${c.brideName}` }))
    : type === 'VENDOR'
    ? vendors.map((v) => ({ value: v.id, label: v.name }))
    : []

  return (
    <TwoColumns>
      <select name="targetType" value={type} onChange={(e) => setType(e.target.value)}>
        {targetTypes.map((t) => (
          <option key={t} value={t}>{targetTypeLabel[t] || t}</option>
        ))}
      </select>
      {type === 'PLANNER' ? (
        <input name="targetId" type="number" min="1" placeholder="플래너 ID" defaultValue={defaultId || ''} required />
      ) : (
        <select name="targetId" defaultValue={defaultId || ''} required>
          <option value="">선택하세요</option>
          {options.map((o) => (
            <option key={o.value} value={o.value}>{o.label}</option>
          ))}
        </select>
      )}
    </TwoColumns>
  )
}

function ScheduleAddForm({ customers, vendors, loading, onSubmit }) {
  return (
    <form className="stack-form" onSubmit={onSubmit}>
      <TargetSelect customers={customers} vendors={vendors} />
      <TwoColumns>
        <select name="scheduleType">
          {scheduleTypes.map((type) => (
            <option key={type} value={type}>{scheduleTypeLabel[type] || type}</option>
          ))}
        </select>
        <input name="title" placeholder="일정명" required />
        <input name="startsAt" type="datetime-local" required />
        <input name="endsAt" type="datetime-local" required />
      </TwoColumns>
      <input name="location" placeholder="장소" />
      <button type="submit" disabled={loading}>일정 저장</button>
    </form>
  )
}

function ScheduleDetailView({ schedule, customers, vendors, onEdit, onDelete }) {
  const targetName = schedule.targetType === 'CUSTOMER'
    ? customers.find((c) => c.id === schedule.targetId)
      ? `${customers.find((c) => c.id === schedule.targetId).groomName} & ${customers.find((c) => c.id === schedule.targetId).brideName}`
      : `고객 #${schedule.targetId}`
    : schedule.targetType === 'VENDOR'
    ? vendors.find((v) => v.id === schedule.targetId)?.name || `업체 #${schedule.targetId}`
    : `플래너 #${schedule.targetId}`

  const fields = [
    ['대상', `${targetTypeLabel[schedule.targetType] || schedule.targetType} · ${targetName}`],
    ['일정 유형', scheduleTypeLabel[schedule.scheduleType] || schedule.scheduleType],
    ['시작', schedule.startsAt?.slice(0, 16).replace('T', ' ')],
    ['종료', schedule.endsAt?.slice(0, 16).replace('T', ' ')],
    ['장소', schedule.location],
  ]

  return (
    <div className="customer-detail">
      <div className="customer-detail__header">
        <h4>{schedule.title}</h4>
        <div className="customer-detail__actions">
          <button type="button" className="small-button" onClick={() => onEdit(schedule)}>수정</button>
          <button type="button" className="small-button danger" onClick={() => onDelete(schedule)}>삭제</button>
        </div>
      </div>
      <dl className="customer-detail__fields">
        {fields.filter(([, v]) => v != null && v !== '').map(([label, value]) => (
          <div key={label} className="customer-detail__field">
            <dt>{label}</dt>
            <dd>{value}</dd>
          </div>
        ))}
      </dl>
    </div>
  )
}

function ScheduleEditForm({ schedule, customers, vendors, loading, onSubmit, onCancel }) {
  return (
    <form key={schedule.id} className="stack-form edit-form" onSubmit={onSubmit}>
      <TargetSelect customers={customers} vendors={vendors} defaultType={schedule.targetType} defaultId={schedule.targetId} />
      <TwoColumns>
        <select name="scheduleType" defaultValue={schedule.scheduleType}>
          {scheduleTypes.map((type) => (
            <option key={type} value={type}>{scheduleTypeLabel[type] || type}</option>
          ))}
        </select>
        <input name="title" placeholder="일정명" defaultValue={schedule.title || ''} required />
        <input name="startsAt" type="datetime-local" defaultValue={toDateTimeLocal(schedule.startsAt)} required />
        <input name="endsAt" type="datetime-local" defaultValue={toDateTimeLocal(schedule.endsAt)} required />
      </TwoColumns>
      <input name="location" placeholder="장소" defaultValue={schedule.location || ''} />
      <div className="action-row">
        <button type="submit" disabled={loading}>수정 저장</button>
        <button type="button" className="secondary-button" onClick={onCancel}>취소</button>
      </div>
    </form>
  )
}

function RecommendationResult({ recommendation }) {
  return (
    <div className="recommendation-result">
      <h4>기존 Workspace 업체</h4>
      <ResultList
        items={recommendation.workspaceCandidates || []}
        emptyMessage="조건에 맞는 기존 업체가 없습니다."
        renderItem={(candidate) => (
          <>
            <strong>{candidate.name || candidate.vendorName}</strong>
            <span>{candidate.reason || candidate.source}</span>
          </>
        )}
      />
      <h4>외부 업체 후보</h4>
      <ResultList
        items={recommendation.externalCandidates || []}
        emptyMessage="외부 후보가 없습니다."
        renderItem={(candidate) => (
          <>
            <strong>{candidate.name || candidate.vendorName}</strong>
            <span>{candidate.reason || candidate.source}</span>
          </>
        )}
      />
    </div>
  )
}

export default App
