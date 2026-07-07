const {useEffect, useMemo, useState} = React;

const categories = [
    "WEDDING_HALL",
    "STUDIO",
    "DRESS",
    "MAKEUP",
    "FLOWER",
    "JEWELRY",
    "HANBOK",
    "RETURN_GIFT",
    "PHOTO",
    "VIDEO"
];

const scheduleTypes = [
    "CONSULTATION",
    "VENUE_TOUR",
    "DRESS_FITTING",
    "STUDIO_SHOOT",
    "MAKEUP",
    "WEDDING_DAY",
    "VENDOR_VISIT",
    "CONTRACT",
    "PERSONAL_TASK"
];

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

function toNumberOrNull(value) {
    return value === "" || value === null || value === undefined ? null : Number(value);
}

function toDateTime(value) {
    return value ? `${value}:00` : null;
}

function label(value) {
    return value ? value.replaceAll("_", " ").toLowerCase().replace(/\b\w/g, char => char.toUpperCase()) : "";
}

function App() {
    const [me, setMe] = useState(null);
    const [workspaceId, setWorkspaceId] = useState("");
    const [status, setStatus] = useState("로그인 상태를 확인하고 있습니다.");
    const [customers, setCustomers] = useState([]);
    const [vendors, setVendors] = useState([]);
    const [schedules, setSchedules] = useState([]);
    const [agentResult, setAgentResult] = useState(null);
    const [busy, setBusy] = useState(false);

    const origin = window.location.origin;
    const redirectUri = `${origin}/login/oauth2/code/google`;
    const signedIn = Boolean(me?.userId);

    useEffect(() => {
        loadMe();
    }, []);

    async function loadMe() {
        try {
            const data = await api("/api/me");
            setMe(data);
            setWorkspaceId(data.currentWorkspaceId || data.workspaces[0]?.workspaceId || "");
            setStatus(`User #${data.userId} · Workspace ${data.currentWorkspaceId || data.workspaces[0]?.workspaceId || "-"}`);
        } catch (error) {
            setStatus("로그인 전입니다. Google Login으로 시작하세요.");
        }
    }

    async function refreshAll(id = workspaceId) {
        if (!id) {
            setStatus("Workspace ID가 필요합니다. 로그인 후 자동으로 채워집니다.");
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
            setStatus(`Workspace ${id} 데이터를 불러왔습니다.`);
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

    return h("div", {className: "app"},
        h(Hero, {signedIn, status, redirectUri}),
        h("main", {className: "shell"},
            h(WorkspaceBar, {workspaceId, setWorkspaceId, refreshAll, busy, me}),
            h("section", {className: "dashboard"},
                h(MetricCard, {label: "Customers", value: customers.length, accent: "rose"}),
                h(MetricCard, {label: "Vendors", value: vendors.length, accent: "mint"}),
                h(MetricCard, {label: "Schedules", value: schedules.length, accent: "sky"}),
                h(MetricCard, {label: "Agent", value: agentResult ? "Ready" : "Idle", accent: "lavender"})
            ),
            h("section", {className: "workspace-grid"},
                h(Panel, {title: "Customer Desk", eyebrow: "CRM", action: `${customers.length} records`},
                    h(CustomerForm, {onSubmit: submitCustomer}),
                    h(CustomerList, {customers})
                ),
                h(Panel, {title: "Vendor Library", eyebrow: "Partners", action: `${vendors.length} vendors`},
                    h(VendorForm, {onSubmit: submitVendor}),
                    h(VendorList, {vendors})
                ),
                h(Panel, {title: "Planner Schedule", eyebrow: "Calendar", action: `${schedules.length} events`},
                    h(ScheduleForm, {onSubmit: submitSchedule}),
                    h(ScheduleList, {schedules})
                ),
                h(Panel, {title: "AI Agent", eyebrow: "Assistant", action: "Workspace-aware"},
                    h(AgentForm, {onSubmit: submitAgent}),
                    h(AgentResult, {result: agentResult})
                )
            )
        )
    );
}

function Hero({signedIn, status, redirectUri}) {
    return h("header", {className: "hero"},
        h("nav", {className: "nav"},
            h("div", {className: "brand"},
                h("span", {className: "brand-mark"}, "m"),
                h("span", null, "marry-it")
            ),
            h("div", {className: "nav-actions"},
                h("a", {className: "link-button", href: "/logout"}, "Logout"),
                h("a", {className: "primary-button", href: "/oauth2/authorization/google"}, signedIn ? "Reconnect Google" : "Google Login")
            )
        ),
        h("section", {className: "hero-content"},
            h("div", null,
                h("p", {className: "eyebrow"}, "Wedding planner operating system"),
                h("h1", null, "고객, 업체, 일정, 추천을 한 화면에서 정리하세요."),
                h("p", {className: "hero-copy"}, "밝고 정돈된 업무 콘솔로 플래너의 고객 관리, 업체 경험, 일정 충돌 확인, Agent 추천 흐름을 빠르게 검증할 수 있습니다."),
                h("div", {className: "hero-pills"},
                    h("span", null, status),
                    h("span", null, `Redirect URI: ${redirectUri}`)
                )
            ),
            h("div", {className: "hero-card"},
                h("span", {className: "mini-label"}, "Today"),
                h("strong", null, "Workspace ready"),
                h("p", null, "Google OAuth 로그인 후 개인 Workspace가 자동 생성됩니다."),
                h("div", {className: "soft-bars"},
                    h("i", null),
                    h("i", null),
                    h("i", null)
                )
            )
        )
    );
}

function WorkspaceBar({workspaceId, setWorkspaceId, refreshAll, busy, me}) {
    const workspaceName = useMemo(() => {
        const found = me?.workspaces?.find(workspace => String(workspace.workspaceId) === String(workspaceId));
        return found?.workspaceName || "Workspace";
    }, [me, workspaceId]);

    return h("section", {className: "workspace-bar"},
        h("div", null,
            h("span", {className: "mini-label"}, "Active workspace"),
            h("strong", null, workspaceName)
        ),
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
        h("button", {className: "primary-button dark", onClick: () => refreshAll(), disabled: busy}, busy ? "Loading..." : "Refresh")
    );
}

function MetricCard({label, value, accent}) {
    return h("article", {className: `metric ${accent}`},
        h("span", null, label),
        h("strong", null, value)
    );
}

function Panel({title, eyebrow, action, children}) {
    return h("article", {className: "panel"},
        h("div", {className: "panel-head"},
            h("div", null,
                h("span", {className: "mini-label"}, eyebrow),
                h("h2", null, title)
            ),
            h("span", {className: "panel-action"}, action)
        ),
        children
    );
}

function CustomerForm({onSubmit}) {
    return h("form", {className: "form", onSubmit},
        h("div", {className: "form-row"},
            h("input", {name: "groomName", placeholder: "신랑 이름", required: true}),
            h("input", {name: "brideName", placeholder: "신부 이름", required: true})
        ),
        h("div", {className: "form-row"},
            h("input", {name: "phoneNumber", placeholder: "연락처"}),
            h("input", {name: "residenceArea", placeholder: "거주 지역"})
        ),
        h("div", {className: "form-row"},
            h("input", {name: "weddingDate", type: "date"}),
            h("input", {name: "preferredWeddingArea", placeholder: "희망 예식 지역"})
        ),
        h("div", {className: "form-row"},
            h("input", {name: "expectedGuestCount", type: "number", min: "0", placeholder: "예상 하객 수"}),
            h("input", {name: "totalBudget", type: "number", min: "0", placeholder: "총예산"})
        ),
        h("textarea", {name: "preferredAtmosphere", placeholder: "선호 분위기"}),
        h("textarea", {name: "importantConditions", placeholder: "중요 조건"}),
        h("button", {className: "submit-button"}, "고객 등록")
    );
}

function VendorForm({onSubmit}) {
    return h("form", {className: "form", onSubmit},
        h("div", {className: "form-row"},
            h("input", {name: "kakaoPlaceId", placeholder: "Kakao Place ID", required: true}),
            h("input", {name: "name", placeholder: "업체명", required: true})
        ),
        h("select", {name: "category", required: true}, categories.map(category => h("option", {key: category, value: category}, label(category)))),
        h("input", {name: "roadAddress", placeholder: "도로명 주소"}),
        h("div", {className: "form-row"},
            h("input", {name: "phone", placeholder: "전화번호"}),
            h("input", {name: "contactPerson", placeholder: "담당자"})
        ),
        h("label", {className: "toggle"}, h("input", {name: "partnered", type: "checkbox"}), h("span", null, "제휴 업체")),
        h("button", {className: "submit-button"}, "업체 등록")
    );
}

function ScheduleForm({onSubmit}) {
    return h("form", {className: "form", onSubmit},
        h("div", {className: "form-row"},
            h("select", {name: "targetType"}, ["CUSTOMER", "VENDOR", "PLANNER"].map(type => h("option", {key: type, value: type}, label(type)))),
            h("input", {name: "targetId", type: "number", min: "1", placeholder: "대상 ID", required: true})
        ),
        h("select", {name: "scheduleType"}, scheduleTypes.map(type => h("option", {key: type, value: type}, label(type)))),
        h("input", {name: "title", placeholder: "일정 제목", required: true}),
        h("div", {className: "form-row"},
            h("input", {name: "startsAt", type: "datetime-local", required: true}),
            h("input", {name: "endsAt", type: "datetime-local", required: true})
        ),
        h("input", {name: "location", placeholder: "장소"}),
        h("button", {className: "submit-button"}, "일정 등록")
    );
}

function AgentForm({onSubmit}) {
    return h("form", {className: "form", onSubmit},
        h("textarea", {name: "message", placeholder: "예: 부케 업체가 갑자기 취소됐어. 대체 업체 찾아줘.", required: true}),
        h("select", {name: "vendorCategory"},
            h("option", {value: ""}, "카테고리 선택"),
            categories.map(category => h("option", {key: category, value: category}, label(category)))
        ),
        h("input", {name: "areaKeyword", placeholder: "지역 키워드"}),
        h("label", {className: "toggle"}, h("input", {name: "includeExternalSearch", type: "checkbox"}), h("span", null, "기존 업체가 없으면 카카오 외부 후보 검색")),
        h("button", {className: "submit-button"}, "Agent 요청")
    );
}

function CustomerList({customers}) {
    if (customers.length === 0) return h(Empty, {message: "등록된 고객이 없습니다."});
    return h("div", {className: "cards"}, customers.map(customer => h("div", {className: "data-card", key: customer.id},
        h("strong", null, `${customer.groomName} · ${customer.brideName}`),
        h("span", null, `예식일 ${customer.weddingDate || "-"}`),
        h("span", null, `지역 ${customer.preferredWeddingArea || "-"}`),
        h("em", null, customer.totalBudget ? `${customer.totalBudget.toLocaleString()}원` : "예산 미입력")
    )));
}

function VendorList({vendors}) {
    if (vendors.length === 0) return h(Empty, {message: "등록된 업체가 없습니다."});
    return h("div", {className: "cards"}, vendors.map(vendor => h("div", {className: "data-card", key: vendor.id},
        h("strong", null, vendor.name),
        h("span", null, label(vendor.category)),
        h("span", null, vendor.roadAddress || vendor.address || "주소 미입력"),
        h("em", null, vendor.partnered ? "Partnered" : "Non-partner")
    )));
}

function ScheduleList({schedules}) {
    if (schedules.length === 0) return h(Empty, {message: "등록된 일정이 없습니다."});
    return h("div", {className: "cards"}, schedules.map(schedule => h("div", {className: "data-card", key: schedule.id},
        h("strong", null, schedule.title),
        h("span", null, `${schedule.startsAt} - ${schedule.endsAt}`),
        h("span", null, `${label(schedule.targetType)} #${schedule.targetId}`),
        h("em", null, schedule.location || "장소 미입력")
    )));
}

function AgentResult({result}) {
    if (!result) return h("div", {className: "agent-empty"}, "Agent 응답이 여기에 표시됩니다.");
    return h("div", {className: "agent-result"},
        h("p", null, result.answer),
        h("pre", null, JSON.stringify(result.vendorRecommendation, null, 2))
    );
}

function Empty({message}) {
    return h("div", {className: "empty"}, message);
}

function h(type, props, ...children) {
    return React.createElement(type, props, ...children.flat());
}

ReactDOM.createRoot(document.getElementById("root")).render(h(App));
