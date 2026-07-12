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
  { id: 'kakao', label: '카카오 검색' },
  { id: 'schedules', label: '일정' },
  { id: 'agent', label: 'AI' },
  { id: 'team', label: '팀' },
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

function customerPayload(data) {
  return {
    groomName: data.get('groomName'),
    brideName: data.get('brideName'),
    phoneNumber: data.get('phoneNumber'),
    residenceArea: data.get('residenceArea'),
    weddingDate: data.get('weddingDate'),
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
  return {
    kakaoPlaceId: data.get('kakaoPlaceId'),
    name: data.get('name'),
    category: data.get('category'),
    address: data.get('address'),
    roadAddress: data.get('roadAddress'),
    phone: data.get('phone'),
    latitude: data.get('latitude'),
    longitude: data.get('longitude'),
    placeUrl: data.get('placeUrl'),
    partnered: form.partnered.checked,
    contactPerson: data.get('contactPerson'),
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
  const [kakaoResults, setKakaoResults] = useState([])
  const [agentResult, setAgentResult] = useState(null)
  const [recommendation, setRecommendation] = useState(null)
  const [selectedKakaoPlace, setSelectedKakaoPlace] = useState(null)
  const [editingCustomer, setEditingCustomer] = useState(null)
  const [editingVendor, setEditingVendor] = useState(null)
  const [editingSchedule, setEditingSchedule] = useState(null)
  const [selectedVendorId, setSelectedVendorId] = useState('')
  const [vendorExperiences, setVendorExperiences] = useState([])
  const [activeTab, setActiveTab] = useState('dashboard')
  const [status, setStatus] = useState('로그인 상태를 확인하고 있습니다.')
  const [loading, setLoading] = useState(true)

  const currentWorkspace = useMemo(() => {
    return me?.workspaces?.find((workspace) => String(workspace.workspaceId) === String(workspaceId))
  }, [me, workspaceId])
  const canManageTeam = currentWorkspace?.role === 'OWNER' || currentWorkspace?.role === 'ADMIN'

  useEffect(() => {
    loadMe()
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

  async function searchKakao(event) {
    event.preventDefault()
    const query = new FormData(event.currentTarget).get('query')
    setLoading(true)
    try {
      const results = await api(
        `/api/workspaces/${workspaceId}/external/kakao/places?query=${encodeURIComponent(query)}`,
      )
      setKakaoResults(results)
      setStatus('카카오 장소 검색 결과를 불러왔습니다.')
    } catch (error) {
      setStatus(error.message)
    } finally {
      setLoading(false)
    }
  }

  function loginWithGoogle() {
    window.location.href = `${BACKEND_URL}/oauth2/authorization/google`
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

            {(activeTab === 'customers' || activeTab === 'vendors') && (
            <section className="tab-grid">
              {activeTab === 'customers' && (
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
                    <input name="weddingDate" type="date" />
                    <input name="preferredWeddingArea" placeholder="희망 예식 지역" />
                    <input name="expectedGuestCount" type="number" min="0" placeholder="예상 하객 수" />
                    <input name="totalBudget" type="number" min="0" placeholder="총예산" />
                  </TwoColumns>
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
              )}

              {activeTab === 'vendors' && (
              <Panel title="업체 등록" id="vendor-form">
                <form
                  key={selectedKakaoPlace?.kakaoPlaceId || 'manual-vendor'}
                  className="stack-form"
                  onSubmit={(event) =>
                    submitForm(event, `/api/workspaces/${workspaceId}/vendors`, vendorPayload)
                  }
                >
                  <TwoColumns>
                    <input
                      name="kakaoPlaceId"
                      placeholder="카카오 장소 ID"
                      defaultValue={selectedKakaoPlace?.kakaoPlaceId || ''}
                      required
                    />
                    <input name="name" placeholder="업체명" defaultValue={selectedKakaoPlace?.name || ''} required />
                    <select name="category" defaultValue="WEDDING_HALL">
                      {categories.map((category) => (
                        <option key={category} value={category}>
                          {category}
                        </option>
                      ))}
                    </select>
                    <input name="contactPerson" placeholder="담당자" />
                    <input name="phone" placeholder="전화번호" defaultValue={selectedKakaoPlace?.phone || ''} />
                    <input name="roadAddress" placeholder="도로명 주소" defaultValue={selectedKakaoPlace?.roadAddress || ''} />
                    <input name="address" placeholder="주소" defaultValue={selectedKakaoPlace?.address || ''} />
                    <input name="placeUrl" placeholder="카카오 장소 URL" defaultValue={selectedKakaoPlace?.placeUrl || ''} />
                    <input name="latitude" placeholder="위도" defaultValue={selectedKakaoPlace?.latitude || ''} />
                    <input name="longitude" placeholder="경도" defaultValue={selectedKakaoPlace?.longitude || ''} />
                  </TwoColumns>
                  <label className="check-row">
                    <input name="partnered" type="checkbox" />
                    제휴 업체
                  </label>
                  <button type="submit" disabled={loading}>
                    업체 저장
                  </button>
                </form>
              </Panel>
              )}
            </section>
            )}

            {(activeTab === 'kakao' || activeTab === 'schedules') && (
            <section className="tab-grid">
              {activeTab === 'kakao' && (
              <Panel title="카카오맵 업체 검색" id="kakao">
                <form className="inline-form" onSubmit={searchKakao}>
                  <input name="query" placeholder="예: 강남 플라워, 청담 드레스" required />
                  <button type="submit" disabled={loading}>
                    검색
                  </button>
                </form>
                <ResultList
                  items={kakaoResults}
                  emptyMessage="검색 결과가 없습니다."
                  renderItem={(place) => (
                    <>
                      <strong>{place.name}</strong>
                      <span>{place.roadAddress || place.address}</span>
                      <button type="button" onClick={() => setSelectedKakaoPlace(place)}>
                        업체 폼에 채우기
                      </button>
                    </>
                  )}
                />
              </Panel>
              )}

              {activeTab === 'schedules' && (
              <Panel title="일정 등록" id="schedules">
                <form
                  className="stack-form"
                  onSubmit={(event) =>
                    submitForm(event, `/api/workspaces/${workspaceId}/schedules`, schedulePayload)
                  }
                >
                  <TwoColumns>
                    <select name="targetType">
                      {targetTypes.map((type) => (
                        <option key={type} value={type}>
                          {type}
                        </option>
                      ))}
                    </select>
                    <input name="targetId" type="number" min="1" placeholder="대상 ID" required />
                    <select name="scheduleType">
                      {scheduleTypes.map((type) => (
                        <option key={type} value={type}>
                          {type}
                        </option>
                      ))}
                    </select>
                    <input name="title" placeholder="일정명" required />
                    <input name="startsAt" type="datetime-local" required />
                    <input name="endsAt" type="datetime-local" required />
                  </TwoColumns>
                  <input name="location" placeholder="장소" />
                  <button type="submit" disabled={loading}>
                    일정 저장
                  </button>
                </form>
              </Panel>
              )}
            </section>
            )}

            {(activeTab === 'customers' || activeTab === 'vendors' || activeTab === 'schedules') && (
            <section className="tab-grid">
              {activeTab === 'customers' && (
              <Panel title="고객 상세 · 수정 · 삭제" id="customers-manage">
                <ManageList
                  items={customers}
                  emptyMessage="관리할 고객이 없습니다."
                  renderLabel={(customer) =>
                    `${customer.id}. ${customer.groomName || ''} & ${customer.brideName || ''}`
                  }
                  onEdit={setEditingCustomer}
                  onDelete={(customer) =>
                    deleteResource(
                      `/api/workspaces/${workspaceId}/customers/${customer.id}`,
                      `${customer.groomName} & ${customer.brideName} 고객을 삭제할까요?`,
                    )
                  }
                />
                {editingCustomer && (
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
                )}
              </Panel>
              )}

              {activeTab === 'vendors' && (
              <Panel title="업체 상세 · 수정 · 삭제">
                <ManageList
                  items={vendors}
                  emptyMessage="관리할 업체가 없습니다."
                  renderLabel={(vendor) => `${vendor.id}. ${vendor.name} · ${vendor.category}`}
                  onEdit={setEditingVendor}
                  onDelete={(vendor) =>
                    deleteResource(
                      `/api/workspaces/${workspaceId}/vendors/${vendor.id}`,
                      `${vendor.name} 업체를 삭제할까요?`,
                    )
                  }
                />
                {editingVendor && (
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
                )}
              </Panel>
              )}

              {activeTab === 'schedules' && (
              <Panel title="일정 상세 · 수정 · 삭제">
                <ManageList
                  items={schedules}
                  emptyMessage="관리할 일정이 없습니다."
                  renderLabel={(schedule) => `${schedule.id}. ${schedule.title} · ${schedule.startsAt}`}
                  onEdit={setEditingSchedule}
                  onDelete={(schedule) =>
                    deleteResource(
                      `/api/workspaces/${workspaceId}/schedules/${schedule.id}`,
                      `${schedule.title} 일정을 삭제할까요?`,
                    )
                  }
                />
                {editingSchedule && (
                  <ScheduleEditForm
                    schedule={editingSchedule}
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
                )}
              </Panel>
              )}
            </section>
            )}

            {(activeTab === 'vendors' || activeTab === 'agent') && (
            <section className="tab-grid">
              {activeTab === 'vendors' && (
              <Panel title="업체 경험 · 노하우" id="experiences">
                <form
                  className="stack-form"
                  onSubmit={(event) =>
                    submitForm(
                      event,
                      `/api/workspaces/${workspaceId}/vendors/${selectedVendorId}/experiences`,
                      (data) => ({ content: data.get('content') }),
                      async () => loadVendorExperiences(selectedVendorId),
                    )
                  }
                >
                  <select
                    value={selectedVendorId}
                    onChange={(event) => loadVendorExperiences(event.target.value)}
                    required
                  >
                    <option value="">업체 선택</option>
                    {vendors.map((vendor) => (
                      <option key={vendor.id} value={vendor.id}>
                        {vendor.name}
                      </option>
                    ))}
                  </select>
                  <textarea name="content" placeholder="예: 급한 주문 대응이 빠르고 화이트톤 부케를 잘함" required />
                  <button type="submit" disabled={loading || !selectedVendorId}>
                    경험 저장
                  </button>
                </form>
                <ResultList
                  items={vendorExperiences}
                  emptyMessage="선택한 업체의 경험이 없습니다."
                  renderItem={(experience) => (
                    <>
                      <strong>{experience.plannerName}</strong>
                      <span>{experience.content}</span>
                    </>
                  )}
                />
              </Panel>
              )}

              {activeTab === 'agent' && (
              <Panel title="AI 추천 결과 상세">
                {agentResult?.vendorRecommendation ? (
                  <RecommendationResult recommendation={agentResult.vendorRecommendation} />
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
                <form
                  className="stack-form"
                  onSubmit={(event) =>
                    submitForm(
                      event,
                      `/api/workspaces/${workspaceId}/agent`,
                      (data, form) => ({
                        message: data.get('message'),
                        vendorCategory: data.get('vendorCategory'),
                        areaKeyword: data.get('areaKeyword'),
                        includeExternalSearch: form.includeExternalSearch.checked,
                      }),
                      (result) => setAgentResult(result),
                    )
                  }
                >
                  <textarea name="message" placeholder="예: 부케 업체가 갑자기 취소됐어. 대체 업체 찾아줘." required />
                  <TwoColumns>
                    <select name="vendorCategory" defaultValue="">
                      <option value="">카테고리 선택 안 함</option>
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
                    기존 업체가 부족하면 외부 업체도 검색
                  </label>
                  <button type="submit" disabled={loading}>
                    AI에게 요청
                  </button>
                </form>
                {agentResult && (
                  <div className="result-box">
                    <strong>AI 응답</strong>
                    <p>{agentResult.answer}</p>
                  </div>
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

function ManageList({ items, emptyMessage, renderLabel, onEdit, onDelete }) {
  if (items.length === 0) {
    return <p className="empty">{emptyMessage}</p>
  }

  return (
    <ul className="manage-list">
      {items.map((item) => (
        <li key={item.id}>
          <span>{renderLabel(item)}</span>
          <div>
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

function CustomerEditForm({ customer, loading, onSubmit, onCancel }) {
  return (
    <form key={customer.id} className="stack-form edit-form" onSubmit={onSubmit}>
      <TwoColumns>
        <input name="groomName" placeholder="신랑 이름" defaultValue={customer.groomName || ''} required />
        <input name="brideName" placeholder="신부 이름" defaultValue={customer.brideName || ''} required />
        <input name="phoneNumber" placeholder="연락처" defaultValue={customer.phoneNumber || ''} />
        <input name="residenceArea" placeholder="거주 지역" defaultValue={customer.residenceArea || ''} />
        <input name="weddingDate" type="date" defaultValue={customer.weddingDate || ''} />
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

function ScheduleEditForm({ schedule, loading, onSubmit, onCancel }) {
  return (
    <form key={schedule.id} className="stack-form edit-form" onSubmit={onSubmit}>
      <TwoColumns>
        <select name="targetType" defaultValue={schedule.targetType}>
          {targetTypes.map((type) => (
            <option key={type} value={type}>
              {type}
            </option>
          ))}
        </select>
        <input name="targetId" type="number" min="1" placeholder="대상 ID" defaultValue={schedule.targetId} required />
        <select name="scheduleType" defaultValue={schedule.scheduleType}>
          {scheduleTypes.map((type) => (
            <option key={type} value={type}>
              {type}
            </option>
          ))}
        </select>
        <input name="title" placeholder="일정명" defaultValue={schedule.title || ''} required />
        <input name="startsAt" type="datetime-local" defaultValue={toDateTimeLocal(schedule.startsAt)} required />
        <input name="endsAt" type="datetime-local" defaultValue={toDateTimeLocal(schedule.endsAt)} required />
      </TwoColumns>
      <input name="location" placeholder="장소" defaultValue={schedule.location || ''} />
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
