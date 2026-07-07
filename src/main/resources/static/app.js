const {useEffect, useMemo, useState} = React;

const pages = [
    {id: "dashboard", label: "홈"},
    {id: "customers", label: "고객"},
    {id: "vendors", label: "업체"},
    {id: "schedules", label: "일정"},
    {id: "agent", label: "AI 도우미"}
];

const categories = ["WEDDING_HALL", "STUDIO", "DRESS", "MAKEUP", "FLOWER", "JEWELRY", "HANBOK", "RETURN_GIFT", "PHOTO", "VIDEO"];
const scheduleTypes = ["CONSULTATION", "VENUE_TOUR", "DRESS_FITTING", "STUDIO_SHOOT", "MAKEUP", "WEDDING_DAY", "VENDOR_VISIT", "CONTRACT", "PERSONAL_TASK"];
const labels = {
    WEDDING_HALL: "웨딩홀",
    STUDIO: "스튜디오",
    DRESS: "드레스",
    MAKEUP: "메이크업",
    FLOWER: "플라워",
    JEWELRY: "주얼리",
    HANBOK: "한복",
    RETURN_GIFT: "답례품",
    PHOTO: "스냅",
    VIDEO: "영상",
    CUSTOMER: "고객",
    VENDOR: "업체",
    PLANNER: "플래너",
    CONSULTATION: "상담",
    VENUE_TOUR: "웨딩홀 투어",
    DRESS_FITTING: "드레스 피팅",
    STUDIO_SHOOT: "스튜디오 촬영",
    WEDDING_DAY: "본식",
    VENDOR_VISIT: "업체 방문",
    CONTRACT: "계약",
    PERSONAL_TASK: "개인 업무",
    OWNER: "소유자",
    ADMIN: "관리자",
    MEMBER: "멤버"
};

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
    return labels[value] || value || "";
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

    const workspace = useMemo(() => {
        return me?.workspaces?.find(item => String(item.workspaceId) === String(workspaceId));
    }, [me, workspaceId]);

    useEffect(() => {
        if (new URLSearchParams(window.location.search).has("loginError")) {
            setStatus("로그인에 실패했습니다. 잠시 후 다시 시도하거나 관리자에게 문의해 주세요.");
        }
        loadMe();
    }, []);

    async function loadMe() {
        try {
            const data = await api("/api/me");
            const nextWorkspaceId = data.currentWorkspaceId || data.workspaces[0]?.workspaceId || "";
            setMe(data);
            setWorkspaceId(nextWorkspaceId);
            setStatus("오늘의 웨딩 업무를 바로 시작할 수 있습니다.");
            if (nextWorkspaceId) {
                await refreshAll(nextWorkspaceId);
            }
        } catch (error) {
            if (!new URLSearchParams(window.location.search).has("loginError")) {
                setStatus("로그인 후 고객과 업체 정보를 확인할 수 있습니다.");
            }
        }
    }

    async function refreshAll(id = workspaceId) {
        if (!id) {
            setStatus("로그인 후 워크스페이스 정보를 불러올 수 있습니다.");
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
            setStatus("최신 업무 정보를 불러왔습니다.");
        } catch (error) {
            setStatus(error.message);
        } finally {
            setBusy(false);
        }
    }

    async function submit(path, payload, form, after) {
        if (!workspaceId) {
            setStatus("로그인 후 이용할 수 있습니다.");
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
            setStatus("AI 도우미 응답을 받았습니다.");
        });
    }

    return h("div", {className: "app-frame"},
        h(Sidebar, {activePage, setActivePage, status}),
        h("div", {className: "page-shell"},
            h(TopBar, {refreshAll, busy, workspace, signedIn: Boolean(me)}),
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
            h("small", null, "웨딩 워크스페이스")
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
            h("span", null, "오늘의 안내"),
            h("p", null, status)
        ),
        h("a", {className: "google-button", href: "/oauth2/authorization/google"}, "시작하기"),
        h("a", {className: "logout-link", href: "/logout"}, "로그아웃")
    );
}

function TopBar({refreshAll, busy, workspace, signedIn}) {
    return h("header", {className: "topbar"},
        h("div", null,
            h("span", {className: "eyebrow"}, "marry-it wedding workspace"),
            h("h1", null, workspace?.workspaceName || "웨딩플래너 업무공간"),
            h("p", null, signedIn
                ? "고객, 업체, 일정, 추천 업무를 워크스페이스 기준으로 관리하세요."
                : "로그인하면 개인 워크스페이스가 자동으로 준비됩니다.")
        ),
        h("div", {className: "top-actions"},
            signedIn
                ? h("button", {className: "dark-button", onClick: () => refreshAll(), disabled: busy}, busy ? "불러오는 중" : "업무 정보 새로고침")
                : h("a", {className: "dark-button", href: "/oauth2/authorization/google"}, "구글로 시작하기")
        )
    );
}

function DashboardPage({customers, vendors, schedules, workspace, setActivePage}) {
    const nextSchedules = schedules.slice(0, 4);
    const recentCustomers = customers.slice(0, 4);
    const recentVendors = vendors.slice(0, 4);

    return h("section", {className: "page-stack"},
        h("div", {className: "metrics"},
            h(Metric, {tone: "rose", label: "등록 고객", value: customers.length, onClick: () => setActivePage("customers")}),
            h(Metric, {tone: "mint", label: "등록 업체", value: vendors.length, onClick: () => setActivePage("vendors")}),
            h(Metric, {tone: "sky", label: "예정 일정", value: schedules.length, onClick: () => setActivePage("schedules")}),
            h(Metric, {tone: "lavender", label: "내 권한", value: workspace?.role || "-"})
        ),
        h("div", {className: "content-grid"},
            h(Panel, {title: "최근 등록 고객", action: "전체 보기", onAction: () => setActivePage("customers")},
                h(CustomerTable, {customers: recentCustomers})
            ),
            h(Panel, {title: "최근 등록 업체", action: "전체 보기", onAction: () => setActivePage("vendors")},
                h(VendorCards, {vendors: recentVendors})
            ),
            h(Panel, {title: "다가오는 일정", action: "전체 보기", onAction: () => setActivePage("schedules")},
                h(ScheduleCards, {schedules: nextSchedules})
            ),
            h(Panel, {title: "업무 지원", action: "요청하기", onAction: () => setActivePage("agent")},
                h("div", {className: "assistant-preview"},
                    h("strong", null, "AI 도우미는 워크스페이스 등록 업체를 먼저 확인합니다."),
                    h("p", null, "기존 거래처가 없을 때만 카카오맵 외부 후보를 분리해서 보여줍니다.")
                )
            )
        )
    );
}

function CustomersPage({customers, onSubmit}) {
    const [query, setQuery] = useState("");
    const filteredCustomers = customers.filter(customer => [customer.groomName, customer.brideName, customer.plannerName, customer.preferredWeddingArea]
            .filter(Boolean)
            .some(value => value.includes(query)));

    return h("section", {className: "page-stack"},
        h(PageTitle, {eyebrow: "고객 관리", title: "고객과 담당 플래너를 함께 확인하세요", description: "커플 정보, 담당자명, 예산, 선호 조건을 워크스페이스 단위로 관리합니다."}),
        h("div", {className: "split-page"},
            h(Panel, {title: "고객 등록"}, h(CustomerForm, {onSubmit})),
            h(Panel, {title: "고객 목록", action: `${filteredCustomers.length}명`},
                h(SearchBox, {value: query, onChange: setQuery, placeholder: "고객명, 담당자명, 지역 검색"}),
                h(CustomerTable, {customers: filteredCustomers})
            )
        )
    );
}

function VendorsPage({vendors, onSubmit}) {
    const [query, setQuery] = useState("");
    const filteredVendors = vendors.filter(vendor => [vendor.name, vendor.category, vendor.roadAddress, vendor.contactPerson]
            .filter(Boolean)
            .some(value => value.includes(query)));

    return h("section", {className: "page-stack"},
        h(PageTitle, {eyebrow: "업체 관리", title: "등록 업체와 제휴 상태를 관리하세요", description: "워크스페이스에 등록한 업체, 담당자, 주소, 제휴 여부를 한 곳에서 확인합니다."}),
        h("div", {className: "split-page"},
            h(Panel, {title: "업체 등록"}, h(VendorForm, {onSubmit})),
            h(Panel, {title: "업체 목록", action: `${filteredVendors.length}개`},
                h(SearchBox, {value: query, onChange: setQuery, placeholder: "업체명, 카테고리, 주소, 담당자 검색"}),
                h(VendorCards, {vendors: filteredVendors})
            )
        )
    );
}

function SchedulesPage({schedules, onSubmit}) {
    const [query, setQuery] = useState("");
    const filteredSchedules = schedules.filter(schedule => [schedule.title, schedule.scheduleType, schedule.targetType, schedule.location]
            .filter(Boolean)
            .some(value => value.includes(query)));

    return h("section", {className: "page-stack"},
        h(PageTitle, {eyebrow: "일정 관리", title: "고객·업체·플래너 일정을 분리해서 확인하세요", description: "같은 대상의 일정 시간이 겹치지 않도록 서버에서 충돌을 검증합니다."}),
        h("div", {className: "split-page"},
            h(Panel, {title: "일정 등록"}, h(ScheduleForm, {onSubmit})),
            h(Panel, {title: "일정 목록", action: `${filteredSchedules.length}건`},
                h(SearchBox, {value: query, onChange: setQuery, placeholder: "일정명, 유형, 장소 검색"}),
                h(ScheduleCards, {schedules: filteredSchedules})
            )
        )
    );
}

function AgentPage({onSubmit, result}) {
    return h("section", {className: "page-stack"},
        h(PageTitle, {eyebrow: "AI 도우미", title: "기존 거래처와 외부 후보를 구분해 추천합니다", description: "현재 워크스페이스 데이터와 카카오맵 외부 후보를 분리해서 보여줍니다."}),
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

function SearchBox({value, onChange, placeholder}) {
    return h("div", {className: "search-box"},
        h("input", {value, onChange: event => onChange(event.target.value), placeholder})
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
        h("div", {className: "form-row"}, h("input", {name: "kakaoPlaceId", placeholder: "장소 식별값", required: true}), h("input", {name: "name", placeholder: "업체명", required: true})),
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
        h("button", {className: "submit-button"}, "AI 도우미에게 요청")
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
        h("div", {className: "card-title"}, h("strong", null, vendor.name), h("em", null, vendor.partnered ? "제휴" : "일반")),
        h("span", null, label(vendor.category)),
        h("span", null, vendor.roadAddress || vendor.address || "주소 미입력"),
        h("span", null, `장소 식별값 ${vendor.kakaoPlaceId}`)
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
    if (!result) return h("div", {className: "empty"}, "AI 도우미 응답이 여기에 표시됩니다.");
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
