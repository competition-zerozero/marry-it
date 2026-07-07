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
    MEMBER: "멤버",
    PENDING: "대기",
    ACCEPTED: "수락됨",
    REVOKED: "취소됨"
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
    const [teamMembers, setTeamMembers] = useState([]);
    const [invitations, setInvitations] = useState([]);
    const [inviteDetail, setInviteDetail] = useState(null);
    const [agentResult, setAgentResult] = useState(null);
    const [busy, setBusy] = useState(false);
    const inviteToken = useMemo(() => new URLSearchParams(window.location.search).get("inviteToken"), []);
    const currentUserId = me?.userId;
    const canInvite = workspace?.role === "OWNER" || workspace?.role === "ADMIN";

    const workspace = useMemo(() => {
        return me?.workspaces?.find(item => String(item.workspaceId) === String(workspaceId));
    }, [me, workspaceId]);

    useEffect(() => {
        if (new URLSearchParams(window.location.search).has("loginError")) {
            setStatus("로그인에 실패했습니다. 잠시 후 다시 시도하거나 관리자에게 문의해 주세요.");
        }
        loadMe();
        if (inviteToken) {
            loadInvite(inviteToken);
        }
    }, []);

    async function loadMe() {
        try {
            const data = await api("/api/me");
            const nextWorkspaceId = data.currentWorkspaceId || data.workspaces[0]?.workspaceId || "";
            const nextWorkspace = data.workspaces.find(item => String(item.workspaceId) === String(nextWorkspaceId));
            setMe(data);
            setWorkspaceId(nextWorkspaceId);
            setStatus("오늘의 웨딩 업무를 바로 시작할 수 있습니다.");
            if (nextWorkspaceId) {
                await refreshAll(nextWorkspaceId, nextWorkspace?.role);
            }
        } catch (error) {
            if (!new URLSearchParams(window.location.search).has("loginError")) {
                setStatus("로그인 후 고객과 업체 정보를 확인할 수 있습니다.");
            }
        }
    }

    async function refreshAll(id = workspaceId, role = workspace?.role) {
        if (!id) {
            setStatus("로그인 후 워크스페이스 정보를 불러올 수 있습니다.");
            return;
        }
        setBusy(true);
        try {
            const [customerData, vendorData, scheduleData, memberData] = await Promise.all([
                api(`/api/workspaces/${id}/customers`),
                api(`/api/workspaces/${id}/vendors`),
                api(`/api/workspaces/${id}/schedules`),
                api(`/api/workspaces/${id}/members`)
            ]);
            setCustomers(customerData);
            setVendors(vendorData);
            setSchedules(scheduleData);
            setTeamMembers(memberData);
            if (role === "OWNER" || role === "ADMIN") {
                setInvitations(await api(`/api/workspaces/${id}/invitations`));
            } else {
                setInvitations([]);
            }
            setStatus("최신 업무 정보를 불러왔습니다.");
        } catch (error) {
            setStatus(error.message);
        } finally {
            setBusy(false);
        }
    }

    async function loadInvite(token) {
        try {
            const data = await api(`/api/workspaces/invitations/${token}`);
            setInviteDetail(data);
        } catch (error) {
            setInviteDetail({error: error.message});
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

    async function submitInvite(event) {
        event.preventDefault();
        const form = event.currentTarget;
        const data = Object.fromEntries(new FormData(form));
        await submit(`/api/workspaces/${workspaceId}/invitations`, data, form, result => {
            setInvitations(prev => [result, ...prev]);
            setStatus("초대 링크를 생성했습니다. 상대에게 링크를 전달하세요.");
        });
    }

    async function updateMemberRole(targetUserId, role) {
        if (!workspaceId) return;
        setBusy(true);
        try {
            await api(`/api/workspaces/${workspaceId}/members/${targetUserId}/role`, {
                method: "PATCH",
                body: JSON.stringify({role})
            });
            await refreshAll(workspaceId, workspace?.role);
            setStatus("멤버 권한을 변경했습니다.");
        } catch (error) {
            setStatus(error.message);
        } finally {
            setBusy(false);
        }
    }

    async function removeMember(targetUserId) {
        if (!workspaceId) return;
        setBusy(true);
        try {
            await api(`/api/workspaces/${workspaceId}/members/${targetUserId}`, {
                method: "DELETE"
            });
            await refreshAll(workspaceId, workspace?.role);
            setStatus("멤버를 제거했습니다.");
        } catch (error) {
            setStatus(error.message);
        } finally {
            setBusy(false);
        }
    }

    async function switchWorkspace(nextWorkspaceId) {
        const nextWorkspace = me?.workspaces?.find(item => String(item.workspaceId) === String(nextWorkspaceId));
        setWorkspaceId(nextWorkspaceId);
        await refreshAll(nextWorkspaceId, nextWorkspace?.role);
    }

    async function acceptInvite() {
        if (!inviteToken) {
            return;
        }
        setBusy(true);
        try {
            const result = await api(`/api/workspaces/invitations/${inviteToken}/accept`, {method: "POST"});
            setInviteDetail(result);
            setStatus("초대를 수락했습니다. 워크스페이스가 전환되었습니다.");
            await loadMe();
        } catch (error) {
            setStatus(error.message);
        } finally {
            setBusy(false);
        }
    }

    return h("div", {className: "app-frame"},
        h(Sidebar, {activePage, setActivePage, status}),
        h("div", {className: "page-shell"},
            h(TopBar, {
                refreshAll,
                busy,
                workspace,
                workspaces: me?.workspaces || [],
                workspaceId,
                onSwitchWorkspace: switchWorkspace,
                signedIn: Boolean(me)
            }),
            inviteToken && h(InviteBanner, {inviteDetail, signedIn: Boolean(me), onAcceptInvite: acceptInvite, busy}),
            h("main", {className: "page-content"},
                activePage === "dashboard" && h(DashboardPage, {
                    customers,
                    vendors,
                    schedules,
                    teamMembers,
                    invitations,
                    workspace,
                    canInvite,
                    currentUserId,
                    setActivePage,
                    onSubmitInvite: submitInvite,
                    onUpdateMemberRole: updateMemberRole,
                    onRemoveMember: removeMember
                }),
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

function TopBar({refreshAll, busy, workspace, workspaces, workspaceId, onSwitchWorkspace, signedIn}) {
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
                ? h("div", {className: "workspace-switcher"},
                    h("select", {
                        value: workspaceId,
                        onChange: event => onSwitchWorkspace(event.target.value)
                    }, workspaces.map(item => h("option", {key: item.workspaceId, value: item.workspaceId}, item.workspaceName))),
                    h("button", {className: "dark-button", onClick: () => refreshAll(), disabled: busy}, busy ? "불러오는 중" : "업무 정보 새로고침")
                )
                : h("a", {className: "dark-button", href: "/oauth2/authorization/google"}, "구글로 시작하기")
        )
    );
}

function DashboardPage({customers, vendors, schedules, teamMembers, invitations, workspace, canInvite, currentUserId, setActivePage, onSubmitInvite, onUpdateMemberRole, onRemoveMember}) {
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
            ),
            h(Panel, {title: "팀 구성"},
                h("div", {className: "team-summary"},
                    h("strong", null, `현재 멤버 ${teamMembers.length}명`),
                    h("p", null, "같은 워크스페이스를 공유하는 계정과 초대 상태를 확인합니다.")
                ),
                h(TeamCards, {
                    members: teamMembers,
                    currentUserId,
                    currentRole: workspace?.role,
                    onUpdateMemberRole,
                    onRemoveMember
                }),
                canInvite && h("div", {className: "invite-block"},
                    h("h4", null, "새 멤버 초대"),
                    h("p", null, "이메일로 초대 링크를 공유합니다. 수락한 계정만 워크스페이스에 들어옵니다."),
                    h(InviteForm, {onSubmit: onSubmitInvite, workspaceRole: workspace?.role}),
                    h(InvitationCards, {invitations})
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

function InviteBanner({inviteDetail, signedIn, onAcceptInvite, busy}) {
    return h("section", {className: "invite-banner"},
        h("div", null,
            h("span", {className: "eyebrow"}, "워크스페이스 초대"),
            inviteDetail?.error
                ? h("p", null, inviteDetail.error)
                : h("h2", null, inviteDetail ? `${inviteDetail.workspaceName} 초대` : "초대 내용을 확인하는 중"),
            inviteDetail && !inviteDetail.error && h("p", null, `${inviteDetail.invitedEmail} 계정으로 ${label(inviteDetail.role)} 권한 초대가 도착했습니다.`)
        ),
        inviteDetail && !inviteDetail.error && h("div", {className: "invite-actions"},
            signedIn
                ? h("button", {className: "dark-button", onClick: onAcceptInvite, disabled: busy}, busy ? "수락 처리 중" : "초대 수락")
                : h("a", {className: "dark-button", href: "/oauth2/authorization/google"}, "구글로 로그인")
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

function InviteForm({onSubmit, workspaceRole}) {
    const canInviteAdmin = workspaceRole === "OWNER";
    return h("form", {className: "form", onSubmit},
        h("input", {name: "invitedEmail", type: "email", placeholder: "초대할 이메일", required: true}),
        h("select", {name: "role", required: true},
            h("option", {value: "MEMBER"}, "멤버"),
            canInviteAdmin && h("option", {value: "ADMIN"}, "관리자")
        ),
        h("button", {className: "submit-button"}, "초대 링크 생성")
    );
}

function TeamCards({members, currentUserId, currentRole, onUpdateMemberRole, onRemoveMember}) {
    if (members.length === 0) return h(Empty, {message: "등록된 멤버가 없습니다."});
    return h("div", {className: "cards team-cards"},
        members.map(member => h("div", {className: "data-card", key: member.userId},
            h("div", {className: "card-title"}, h("strong", null, member.userName), h("em", null, label(member.role))),
            h("span", null, member.userEmail),
            h("span", null, `합류일 ${member.joinedAt ? member.joinedAt.replace("T", " ") : "-"}`),
            member.userId !== currentUserId && currentRole && currentRole !== "MEMBER" && h("div", {className: "member-actions"},
                h("select", {
                    defaultValue: member.role,
                    onChange: event => event.currentTarget.dataset.changedRole = event.target.value
                },
                    h("option", {value: "MEMBER"}, "멤버"),
                    currentRole === "OWNER" && h("option", {value: "ADMIN"}, "관리자")
                ),
                h("button", {
                    className: "panel-action",
                    onClick: event => {
                        const nextRole = event.currentTarget.parentElement.querySelector("select").value;
                        onUpdateMemberRole(member.userId, nextRole);
                    }
                }, "권한 변경"),
                h("button", {
                    className: "panel-action",
                    onClick: () => onRemoveMember(member.userId)
                }, "제거")
            )
        ))
    );
}

function InvitationCards({invitations}) {
    if (invitations.length === 0) return h(Empty, {message: "보낸 초대가 없습니다."});
    return h("div", {className: "cards invitation-cards"},
        invitations.map(invitation => h("div", {className: "data-card", key: invitation.id},
            h("div", {className: "card-title"}, h("strong", null, invitation.invitedEmail), h("em", null, label(invitation.status))),
            h("span", null, `${label(invitation.role)} 초대`),
            h("span", null, `만료 ${invitation.expiresAt ? invitation.expiresAt.replace("T", " ") : "-"}`),
            h("a", {href: invitation.inviteUrl}, "초대 링크 열기")
        ))
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
