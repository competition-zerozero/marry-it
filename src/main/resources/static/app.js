const {useEffect, useMemo, useState} = React;

const pages = [
    {id: "dashboard", label: "Dashboard"},
    {id: "customers", label: "Customers"},
    {id: "vendors", label: "Vendors"},
    {id: "schedules", label: "Schedule"},
    {id: "agent", label: "Agent"}
];

const categories = ["WEDDING_HALL", "STUDIO", "DRESS", "MAKEUP", "FLOWER", "JEWELRY", "HANBOK", "RETURN_GIFT", "PHOTO", "VIDEO"];
const scheduleTypes = ["CONSULTATION", "VENUE_TOUR", "DRESS_FITTING", "STUDIO_SHOOT", "MAKEUP", "WEDDING_DAY", "VENDOR_VISIT", "CONTRACT", "PERSONAL_TASK"];

async function api(path, options = {}) {
    const response = await fetch(path, {
        headers: {"Content-Type": "application/json", ...(options.headers || {})},
        ...options
    });
    if (!response.ok) {
        const error = await response.json().catch(() => ({message: response.statusText}));
        throw new Error(error.message || response.statusText);
    }
    return response.status === 204 ? null : response.json();
}

function label(value) {
    return value ? value.replaceAll("_", " ").toLowerCase().replace(/\b\w/g, char => char.toUpperCase()) : "";
}

function toNumberOrNull(value) {
    return value === "" || value === null || value === undefined ? null : Number(value);
}

function toDateTime(value) {
    return value ? `${value}:00` : null;
}

function money(value) {
    return value ? `${Number(value).toLocaleString()}원` : "예산 미입력";
}

function App() {
    const [activePage, setActivePage] = useState("dashboard");
    const [me, setMe] = useState(null);
    const [workspaceId, setWorkspaceId] = useState("");
    const [status, setStatus] = useState("로그인 상태를 확인하고 있습니다.");
    const [customers, setCustomers] = useState([]);
    const [vendors, setVendors] = useState([]);
    const [schedules, setSchedules] = useState([]);
    const [agentResult, setAgentResult] = useState(null);
    const [busy, setBusy] = useState(false);

    const redirectUri = `${window.location.origin}/login/oauth2/code/google`;
    const workspace = useMemo(() => {
        return me?.workspaces?.find(item => String(item.workspaceId) === String(workspaceId));
    }, [me, workspaceId]);

    useEffect(() => {
        loadMe();
    }, []);

    async function loadMe() {
        try {
            const data = await api("/api/me");
            const nextWorkspaceId = data.currentWorkspaceId || data.workspaces[0]?.workspaceId || "";
            setMe(data);
            setWorkspaceId(nextWorkspaceId);
            setStatus(`로그인됨 · User #${data.userId}`);
            if (nextWorkspaceId) {
                await refreshAll(nextWorkspaceId);
            }
        } catch (error) {
            setStatus("로그인 전입니다. Google Login으로 시작하세요.");
        }
    }

    async function refreshAll(id = workspaceId) {
        if (!id) {
            setStatus("Workspace ID가 필요합니다.");
            return;
        }
        setBusy(true);
        try {
            const [customerData, vendorData, scheduleData] = await Promise.all([
                api(`/api/workspaces/${id}/customers`),
                api(`/api/workspaces/${id}/vendors`),
                api(`/api/workspaces/${id}/schedules`)
            ]);
            setCustomers(customerData);
            setVendors(vendorData);
            setSchedules(scheduleData);
            setStatus(`Workspace ${id} 정보를 불러왔습니다.`);
        } catch (error) {
            setStatus(error.message);
        } finally {
            setBusy(false);
        }
    }

    async function submit(path, payload, form, after) {
        if (!workspaceId) {
            setStatus("Workspace ID가 필요합니다.");
            return;
        }
        setBusy(true);
        try {
            const result = await api(path, {method: "POST", body: JSON.stringify(payload)});
            form?.reset();
            await after(result);
        } catch (error) {
            setStatus(error.message);
        } finally {
            setBusy(false);
        }
    }

    async function submitCustomer(event) {
        event.preventDefault();
        const form = event.currentTarget;
        const data = Object.fromEntries(new FormData(form));
        data.expectedGuestCount = toNumberOrNull(data.expectedGuestCount);
        data.totalBudget = toNumberOrNull(data.totalBudget);
        await submit(`/api/workspaces/${workspaceId}/customers`, data, form, () => refreshAll());
    }

    async function submitVendor(event) {
        event.preventDefault();
        const form = event.currentTarget;
        const data = Object.fromEntries(new FormData(form));
        data.partnered = form.partnered.checked;
        data.address = data.roadAddress;
        await submit(`/api/workspaces/${workspaceId}/vendors`, data, form, () => refreshAll());
    }

    async function submitSchedule(event) {
        event.preventDefault();
        const form = event.currentTarget;
        const data = Object.fromEntries(new FormData(form));
        data.targetId = Number(data.targetId);
        data.startsAt = toDateTime(data.startsAt);
        data.endsAt = toDateTime(data.endsAt);
        await submit(`/api/workspaces/${workspaceId}/schedules`, data, form, () => refreshAll());
    }

    async function submitAgent(event) {
        event.preventDefault();
        const form = event.currentTarget;
        const data = Object.fromEntries(new FormData(form));
        data.vendorCategory = data.vendorCategory || null;
        data.includeExternalSearch = form.includeExternalSearch.checked;
        await submit(`/api/workspaces/${workspaceId}/agent`, data, null, result => {
            setAgentResult(result);
            setStatus("Agent 응답을 받았습니다.");
        });
    }

    return h("div", {className: "app-frame"},
        h(Sidebar, {activePage, setActivePage, status}),
        h("div", {className: "page-shell"},
            h(TopBar, {workspaceId, setWorkspaceId, refreshAll, busy, workspace, redirectUri}),
            h("main", {className: "page-content"},
                activePage === "dashboard" && h(DashboardPage, {customers, vendors, schedules, workspace, setActivePage}),
                activePage === "customers" && h(CustomersPage, {customers, onSubmit: submitCustomer}),
                activePage === "vendors" && h(VendorsPage, {vendors, onSubmit: submitVendor}),
                activePage === "schedules" && h(SchedulesPage, {schedules, onSubmit: submitSchedule}),
                activePage === "agent" && h(AgentPage, {onSubmit: submitAgent, result: agentResult})
            )
        )
    );
}

function Sidebar({activePage, setActivePage, status}) {
    return h("aside", {className: "sidebar"},
        h("div", {className: "brand"},
            h("span", {className: "brand-mark"}, "m"),
            h("div", null,
                h("strong", null, "marry-it"),
                h("small", null, "Planner Workspace")
            )
        ),
        h("nav", {className: "side-nav"},
            pages.map(page => h("button", {
                key: page.id,
                className: activePage === page.id ? "active" : "",
                onClick: () => setActivePage(page.id)
            }, page.label))
        ),
        h("div", {className: "side-status"},
            h("span", null, "Status"),
            h("p", null, status)
        ),
        h("a", {className: "google-button", href: "/oauth2/authorization/google"}, "Google Login"),
        h("a", {className: "logout-link", href: "/logout"}, "Logout")
    );
}

function TopBar({workspaceId, setWorkspaceId, refreshAll, busy, workspace, redirectUri}) {
    return h("header", {className: "topbar"},
        h("div", null,
            h("span", {className: "eyebrow"}, "Active Workspace"),
            h("h1", null, workspace?.workspaceName || "Wedding Workspace"),
            h("p", null, `Google Redirect URI · ${redirectUri}`)
        ),
        h("div", {className: "workspace-control"},
            h("label", null,
                "Workspace ID",
                h("input", {
                    value: workspaceId,
                    onChange: event => setWorkspaceId(event.target.value),
                    type: "number",
                    min: "1",
                    placeholder: "로그인 후 자동 입력"
                })
            ),
            h("button", {className: "dark-button", onClick: () => refreshAll(), disabled: busy}, busy ? "Loading" : "Refresh")
        )
    );
}

function DashboardPage({customers, vendors, schedules, workspace, setActivePage}) {
    const nextSchedules = schedules.slice(0, 4);
    const recentCustomers = customers.slice(0, 4);
    const recentVendors = vendors.slice(0, 4);

    return h("section", {className: "page-stack"},
        h("div", {className: "metrics"},
            h(Metric, {tone: "rose", label: "Customers", value: customers.length, onClick: () => setActivePage("customers")}),
            h(Metric, {tone: "mint", label: "Vendors", value: vendors.length, onClick: () => setActivePage("vendors")}),
            h(Metric, {tone: "sky", label: "Schedules", value: schedules.length, onClick: () => setActivePage("schedules")}),
            h(Metric, {tone: "lavender", label: "Workspace", value: workspace?.role || "-"})
        ),
        h("div", {className: "content-grid"},
            h(Panel, {title: "등록된 고객", action: "View all", onAction: () => setActivePage("customers")},
                h(CustomerTable, {customers: recentCustomers})
            ),
            h(Panel, {title: "등록된 업체", action: "View all", onAction: () => setActivePage("vendors")},
                h(VendorCards, {vendors: recentVendors})
            ),
            h(Panel, {title: "다가오는 일정", action: "View all", onAction: () => setActivePage("schedules")},
                h(ScheduleCards, {schedules: nextSchedules})
            ),
            h(Panel, {title: "업무 지원", action: "Ask", onAction: () => setActivePage("agent")},
                h("div", {className: "assistant-preview"},
                    h("strong", null, "Agent는 Workspace 등록 업체를 먼저 확인합니다."),
                    h("p", null, "기존 거래처가 없을 때만 카카오맵 외부 후보를 분리해서 보여줍니다.")
                )
            )
        )
    );
}

function CustomersPage({customers, onSubmit}) {
    return h("section", {className: "page-stack"},
        h(PageTitle, {eyebrow: "CRM", title: "고객 관리", description: "커플 정보, 담당자명, 예산, 선호 조건을 Workspace 단위로 관리합니다."}),
        h("div", {className: "split-page"},
            h(Panel, {title: "고객 등록"}, h(CustomerForm, {onSubmit})),
            h(Panel, {title: "고객 목록", action: `${customers.length}명`}, h(CustomerTable, {customers}))
        )
    );
}

function VendorsPage({vendors, onSubmit}) {
    return h("section", {className: "page-stack"},
        h(PageTitle, {eyebrow: "Partners", title: "업체 관리", description: "Workspace에 등록한 업체와 카카오 장소 정보를 한 곳에서 확인합니다."}),
        h("div", {className: "split-page"},
            h(Panel, {title: "업체 등록"}, h(VendorForm, {onSubmit})),
            h(Panel, {title: "업체 목록", action: `${vendors.length}개`}, h(VendorCards, {vendors}))
        )
    );
}

function SchedulesPage({schedules, onSubmit}) {
    return h("section", {className: "page-stack"},
        h(PageTitle, {eyebrow: "Calendar", title: "일정 관리", description: "고객, 업체, 플래너 일정을 분리하고 같은 대상의 시간 충돌을 막습니다."}),
        h("div", {className: "split-page"},
            h(Panel, {title: "일정 등록"}, h(ScheduleForm, {onSubmit})),
            h(Panel, {title: "일정 목록", action: `${schedules.length}건`}, h(ScheduleCards, {schedules}))
        )
    );
}

function AgentPage({onSubmit, result}) {
    return h("section", {className: "page-stack"},
        h(PageTitle, {eyebrow: "Assistant", title: "AI Agent", description: "현재 Workspace 데이터와 외부 후보를 구분해서 추천 결과를 반환합니다."}),
        h("div", {className: "split-page"},
            h(Panel, {title: "요청 작성"}, h(AgentForm, {onSubmit})),
            h(Panel, {title: "응답"}, h(AgentResult, {result}))
        )
    );
}

function PageTitle({eyebrow, title, description}) {
    return h("div", {className: "page-title"},
        h("span", {className: "eyebrow"}, eyebrow),
        h("h2", null, title),
        h("p", null, description)
    );
}

function Metric({tone, label, value, onClick}) {
    return h("button", {className: `metric ${tone}`, onClick},
        h("span", null, label),
        h("strong", null, value)
    );
}

function Panel({title, action, onAction, children}) {
    return h("article", {className: "panel"},
        h("div", {className: "panel-head"},
            h("h3", null, title),
            action && h("button", {className: "panel-action", onClick: onAction}, action)
        ),
        children
    );
}

function CustomerForm({onSubmit}) {
    return h("form", {className: "form", onSubmit},
        h("div", {className: "form-row"}, h("input", {name: "groomName", placeholder: "신랑 이름", required: true}), h("input", {name: "brideName", placeholder: "신부 이름", required: true})),
        h("div", {className: "form-row"}, h("input", {name: "phoneNumber", placeholder: "연락처"}), h("input", {name: "residenceArea", placeholder: "거주 지역"})),
        h("div", {className: "form-row"}, h("input", {name: "weddingDate", type: "date"}), h("input", {name: "preferredWeddingArea", placeholder: "희망 예식 지역"})),
        h("div", {className: "form-row"}, h("input", {name: "expectedGuestCount", type: "number", min: "0", placeholder: "예상 하객 수"}), h("input", {name: "totalBudget", type: "number", min: "0", placeholder: "총예산"})),
        h("textarea", {name: "preferredAtmosphere", placeholder: "선호 분위기"}),
        h("textarea", {name: "importantConditions", placeholder: "중요 조건"}),
        h("button", {className: "submit-button"}, "고객 등록")
    );
}

function VendorForm({onSubmit}) {
    return h("form", {className: "form", onSubmit},
        h("div", {className: "form-row"}, h("input", {name: "kakaoPlaceId", placeholder: "Kakao Place ID", required: true}), h("input", {name: "name", placeholder: "업체명", required: true})),
        h("select", {name: "category", required: true}, categories.map(category => h("option", {key: category, value: category}, label(category)))),
        h("input", {name: "roadAddress", placeholder: "도로명 주소"}),
        h("div", {className: "form-row"}, h("input", {name: "phone", placeholder: "전화번호"}), h("input", {name: "contactPerson", placeholder: "담당자"})),
        h("label", {className: "toggle"}, h("input", {name: "partnered", type: "checkbox"}), h("span", null, "제휴 업체")),
        h("button", {className: "submit-button"}, "업체 등록")
    );
}

function ScheduleForm({onSubmit}) {
    return h("form", {className: "form", onSubmit},
        h("div", {className: "form-row"}, h("select", {name: "targetType"}, ["CUSTOMER", "VENDOR", "PLANNER"].map(type => h("option", {key: type, value: type}, label(type)))), h("input", {name: "targetId", type: "number", min: "1", placeholder: "대상 ID", required: true})),
        h("select", {name: "scheduleType"}, scheduleTypes.map(type => h("option", {key: type, value: type}, label(type)))),
        h("input", {name: "title", placeholder: "일정 제목", required: true}),
        h("div", {className: "form-row"}, h("input", {name: "startsAt", type: "datetime-local", required: true}), h("input", {name: "endsAt", type: "datetime-local", required: true})),
        h("input", {name: "location", placeholder: "장소"}),
        h("button", {className: "submit-button"}, "일정 등록")
    );
}

function AgentForm({onSubmit}) {
    return h("form", {className: "form", onSubmit},
        h("textarea", {name: "message", placeholder: "예: 부케 업체가 갑자기 취소됐어. 대체 업체 찾아줘.", required: true}),
        h("select", {name: "vendorCategory"}, h("option", {value: ""}, "카테고리 선택"), categories.map(category => h("option", {key: category, value: category}, label(category)))),
        h("input", {name: "areaKeyword", placeholder: "지역 키워드"}),
        h("label", {className: "toggle"}, h("input", {name: "includeExternalSearch", type: "checkbox"}), h("span", null, "기존 업체가 없으면 카카오 외부 후보 검색")),
        h("button", {className: "submit-button"}, "Agent 요청")
    );
}

function CustomerTable({customers}) {
    if (customers.length === 0) return h(Empty, {message: "등록된 고객이 없습니다."});
    return h("div", {className: "table-list"},
        customers.map(customer => h("div", {className: "table-row customer-row", key: customer.id},
            h("div", null, h("strong", null, `${customer.groomName} · ${customer.brideName}`), h("span", null, `담당자 ${customer.plannerName || `#${customer.plannerUserId}`}`)),
            h("div", null, h("span", null, "예식일"), h("strong", null, customer.weddingDate || "-")),
            h("div", null, h("span", null, "희망 지역"), h("strong", null, customer.preferredWeddingArea || "-")),
            h("div", null, h("span", null, "예산"), h("strong", null, money(customer.totalBudget)))
        ))
    );
}

function VendorCards({vendors}) {
    if (vendors.length === 0) return h(Empty, {message: "등록된 업체가 없습니다."});
    return h("div", {className: "cards"}, vendors.map(vendor => h("div", {className: "data-card", key: vendor.id},
        h("div", {className: "card-title"}, h("strong", null, vendor.name), h("em", null, vendor.partnered ? "Partnered" : "External")),
        h("span", null, label(vendor.category)),
        h("span", null, vendor.roadAddress || vendor.address || "주소 미입력"),
        h("span", null, `Kakao Place ${vendor.kakaoPlaceId}`)
    )));
}

function ScheduleCards({schedules}) {
    if (schedules.length === 0) return h(Empty, {message: "등록된 일정이 없습니다."});
    return h("div", {className: "cards"}, schedules.map(schedule => h("div", {className: "data-card", key: schedule.id},
        h("div", {className: "card-title"}, h("strong", null, schedule.title), h("em", null, label(schedule.scheduleType))),
        h("span", null, `${schedule.startsAt} - ${schedule.endsAt}`),
        h("span", null, `${label(schedule.targetType)} #${schedule.targetId}`),
        h("span", null, schedule.location || "장소 미입력")
    )));
}

function AgentResult({result}) {
    if (!result) return h("div", {className: "empty"}, "Agent 응답이 여기에 표시됩니다.");
    return h("div", {className: "agent-result"},
        h("p", null, result.answer),
        h("pre", null, JSON.stringify(result.vendorRecommendation, null, 2))
    );
}

function Empty({message}) {
    return h("div", {className: "empty"}, message);
}

function h(type, props, ...children) {
    return React.createElement(type, props, ...children.flat().filter(child => child !== false && child !== null && child !== undefined));
}

ReactDOM.createRoot(document.getElementById("root")).render(h(App));
