// GLOBAL VERSION - do NOT use export/import
const BASE_URL = "http://localhost:8080";

function setToken(t) {
    if (t) localStorage.setItem("litecore_token", t);
    else localStorage.removeItem("litecore_token");
}

function getToken() {
    return localStorage.getItem("litecore_token");
}

function requireAuth() {
    if (!getToken()) {
        window.location.href = "login.html";
    }
}

// ---- Fetch Wrappers ---- //
async function fetchJson(url, opts) {
    const res = await fetch(url, opts);
    const txt = await res.text();
    try { return JSON.parse(txt); } catch(e) { return txt; }
}

async function apiPost(path, data = {}) {
    const body = new URLSearchParams(data).toString();
    return fetchJson(BASE_URL + path, {
        method: "POST",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded",
            "Authorization": getToken() || ""
        },
        body
    });
}

async function apiGet(path) {
    return fetchJson(BASE_URL + path, {
        method: "GET",
        headers: { "Authorization": getToken() || "" }
    });
}

async function apiPut(path, data = {}) {
    const body = new URLSearchParams(data).toString();
    return fetchJson(BASE_URL + path, {
        method: "PUT",
        headers: {
            "Content-Type": "application/x-www-form-urlencoded",
            "Authorization": getToken() || ""
        },
        body
    });
}

async function apiDelete(path, data = null) {
    const opts = {
        method: "DELETE",
        headers: { "Authorization": getToken() || "" }
    };
    if (data) {
        opts.headers["Content-Type"] = "application/x-www-form-urlencoded";
        opts.body = new URLSearchParams(data).toString();
    }
    return fetchJson(BASE_URL + path, opts);
}

function logoutAndRedirect() {
    setToken(null);
    window.location.href = "login.html";
}
