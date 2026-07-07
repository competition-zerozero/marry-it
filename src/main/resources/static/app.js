const statusEl = document.querySelector("#sessionStatus");
const workspaceInput = document.querySelector("#workspaceId");

async function api(path, options = {}) {
    const response = await fetch(path, {
        headers: {"Content-Type": "application/json", ...(options.headers || {})},
        ...options
    });
    if (!response.ok) {
        const error = await response.json().catch(() => ({message: response.statusText}));
        throw new Error(error.message || response.statusText);
    }
    if (response.status === 204) {
        return null;
    }
    return response.json();
}

function workspaceId() {
    const value = workspaceInput.value;
    if (!value) {
        throw new Error("Workspace ID is required.");
    }
    return value;
}

function values(form) {
    const data = new FormData(form);
    return Object.fromEntries([...data.entries()].map(([key, value]) => [key, value === "" ? null : value]));
}

function numberOrNull(value) {
    return value === null || value === "" ? null : Number(value);
}

function dateTime(value) {
    return value ? `${value}:00` : null;
}

function item(title, lines) {
    return `<div class="item"><strong>${title}</strong>${lines.filter(Boolean).map(line => `<div>${line}</div>`).join("")}</div>`;
}

async function loadMe() {
    try {
        const me = await api("/api/me");
        workspaceInput.value = me.currentWorkspaceId || me.workspaces[0]?.workspaceId || "";
        statusEl.textContent = `Logged in as user ${me.userId}. Workspaces: ${me.workspaces.length}`;
    } catch (error) {
        statusEl.textContent = "Not logged in. Use Google Login or enter a workspace id for authenticated API testing.";
    }
}

async function loadCustomers() {
    const customers = await api(`/api/workspaces/${workspaceId()}/customers`);
    document.querySelector("#customers").innerHTML = customers.map(customer => item(
        `${customer.id}. ${customer.groomName} + ${customer.brideName}`,
        [
            `Wedding: ${customer.weddingDate || "not set"}`,
            `Area: ${customer.preferredWeddingArea || "not set"}`,
            `Budget: ${customer.totalBudget || "not set"}`
        ]
    )).join("");
}

async function loadVendors() {
    const vendors = await api(`/api/workspaces/${workspaceId()}/vendors`);
    document.querySelector("#vendors").innerHTML = vendors.map(vendor => item(
        `${vendor.id}. ${vendor.name}`,
        [
            `Category: ${vendor.category}`,
            `Kakao: ${vendor.kakaoPlaceId}`,
            `Address: ${vendor.roadAddress || vendor.address || "not set"}`
        ]
    )).join("");
}

async function loadSchedules() {
    const schedules = await api(`/api/workspaces/${workspaceId()}/schedules`);
    document.querySelector("#schedules").innerHTML = schedules.map(schedule => item(
        `${schedule.id}. ${schedule.title}`,
        [
            `${schedule.startsAt} to ${schedule.endsAt}`,
            `Target: ${schedule.targetType} #${schedule.targetId}`,
            `Location: ${schedule.location || "not set"}`
        ]
    )).join("");
}

async function refreshAll() {
    try {
        await Promise.all([loadCustomers(), loadVendors(), loadSchedules()]);
    } catch (error) {
        statusEl.textContent = error.message;
    }
}

document.querySelector("#refreshAll").addEventListener("click", refreshAll);

document.querySelector("#customerForm").addEventListener("submit", async event => {
    event.preventDefault();
    const data = values(event.currentTarget);
    data.expectedGuestCount = numberOrNull(data.expectedGuestCount);
    data.totalBudget = numberOrNull(data.totalBudget);
    await api(`/api/workspaces/${workspaceId()}/customers`, {method: "POST", body: JSON.stringify(data)});
    event.currentTarget.reset();
    await loadCustomers();
});

document.querySelector("#vendorForm").addEventListener("submit", async event => {
    event.preventDefault();
    const data = values(event.currentTarget);
    data.partnered = event.currentTarget.partnered.checked;
    data.address = data.roadAddress;
    await api(`/api/workspaces/${workspaceId()}/vendors`, {method: "POST", body: JSON.stringify(data)});
    event.currentTarget.reset();
    await loadVendors();
});

document.querySelector("#scheduleForm").addEventListener("submit", async event => {
    event.preventDefault();
    const data = values(event.currentTarget);
    data.targetId = Number(data.targetId);
    data.startsAt = dateTime(data.startsAt);
    data.endsAt = dateTime(data.endsAt);
    await api(`/api/workspaces/${workspaceId()}/schedules`, {method: "POST", body: JSON.stringify(data)});
    event.currentTarget.reset();
    await loadSchedules();
});

document.querySelector("#agentForm").addEventListener("submit", async event => {
    event.preventDefault();
    const data = values(event.currentTarget);
    data.includeExternalSearch = event.currentTarget.includeExternalSearch.checked;
    const result = await api(`/api/workspaces/${workspaceId()}/agent`, {method: "POST", body: JSON.stringify(data)});
    document.querySelector("#agentResult").textContent = JSON.stringify(result, null, 2);
});

loadMe();
