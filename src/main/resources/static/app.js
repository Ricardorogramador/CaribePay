const API = "/api";

function getToken() {
    return localStorage.getItem("token");
}

function setToken(token) {
    localStorage.setItem("token", token);
}

function clearToken() {
    localStorage.removeItem("token");
}

function authHeaders() {
    const token = getToken();
    return token ? { "Authorization": "Bearer " + token } : {};
}

function isPage(name) {
    return window.location.pathname.endsWith(name);
}

function redirectTo(page) {
    window.location.href = page;
}

function requireAuthOrRedirect() {
    if (!getToken()) redirectTo("index.html");
}

async function readBody(res) {
    const text = await res.text();
    try {
        return JSON.parse(text);
    } catch {
        return text;
    }
}

function errorMessage(data, fallback) {
    if (!data) return fallback;
    if (typeof data === "string") return data;
    return data.mensaje || data.message || fallback;
}

function formatMoney(n) {
    const value = Number(n ?? 0);
    return value.toLocaleString("es-DO", { style: "currency", currency: "DOP", maximumFractionDigits: 0 });
}

function formatDate(d) {
    if (!d) return "";
    // backend manda LocalDateTime; puede venir como string ISO sin zona
    const date = new Date(d);
    if (Number.isNaN(date.getTime())) return String(d);
    return date.toLocaleString("es-DO");
}

function logout() {
    clearToken();
    redirectTo("index.html");
}

// --------------------------- AUTH: auto redirect si ya hay token ---------------------------
if (isPage("index.html") && getToken()) {
    redirectTo("dashboard.html");
}

// --------------------------- LOGIN ---------------------------
const loginForm = document.getElementById("loginForm");
if (loginForm) {
    loginForm.addEventListener("submit", async (e) => {
        e.preventDefault();

        const emailEl = document.getElementById("email");
        const passEl = document.getElementById("password");

        const res = await fetch(`${API}/auth/login`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                email: emailEl.value.trim(),
                password: passEl.value
            })
        });

        const data = await readBody(res);

        if (res.ok && data && typeof data === "object" && data.token) {
            setToken(data.token);
            redirectTo("dashboard.html");
        } else {
            alert(errorMessage(data, "Credenciales incorrectas"));
        }
    });
}

// --------------------------- REGISTRO ---------------------------
const registerForm = document.getElementById("registerForm");
if (registerForm) {
    registerForm.addEventListener("submit", async (e) => {
        e.preventDefault();

        const emailEl = document.getElementById("email");
        const passEl = document.getElementById("password");

        const res = await fetch(`${API}/auth/registro`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                email: emailEl.value.trim(),
                password: passEl.value
            })
        });

        const data = await readBody(res);

        if (res.ok) {
            alert("Cuenta creada. Ahora inicia sesión.");
            redirectTo("index.html");
        } else {
            alert(errorMessage(data, "Error en registro"));
        }
    });
}

// --------------------------- LOGOUT BUTTON (sidebar) ---------------------------
const logoutBtn = document.getElementById("logoutBtn");
if (logoutBtn) logoutBtn.addEventListener("click", logout);

// --------------------------- DASHBOARD / TRANSFER: load profile ---------------------------
async function cargarPerfil() {
    const res = await fetch(`${API}/usuarios/perfil`, { headers: authHeaders() });
    const data = await readBody(res);

    if (res.status === 401) {
        logout();
        return null;
    }
    if (!res.ok) {
        return null;
    }

    const saldoEls = document.querySelectorAll("#saldo");
    saldoEls.forEach(el => (el.textContent = formatMoney(data.saldo)));

    const welcomeText = document.getElementById("welcomeText");
    if (welcomeText && data?.email) {
        welcomeText.textContent = `Conectado como ${data.email}`;
    }

    return data;
}

// --------------------------- TRANSACTIONS ---------------------------
let txCache = [];
let currentFilter = "all";

function classifyTx(tx, myUserId) {
    if (!tx || !myUserId) return "unknown";
    if (tx.emisorId === myUserId) return "sent";
    if (tx.receptorId === myUserId) return "received";
    return "unknown";
}

function renderStats(myUserId) {
    const mov = document.getElementById("movCount");
    const sent = document.getElementById("sentCount");
    const recv = document.getElementById("recvCount");
    if (!mov || !sent || !recv) return;

    const all = txCache.length;
    const s = txCache.filter(t => classifyTx(t, myUserId) === "sent").length;
    const r = txCache.filter(t => classifyTx(t, myUserId) === "received").length;

    mov.textContent = String(all);
    sent.textContent = String(s);
    recv.textContent = String(r);
}

function renderTxList(myUserId) {
    const list = document.getElementById("listaTransacciones");
    const empty = document.getElementById("emptyTx");
    if (!list) return;

    list.innerHTML = "";

    let arr = [...txCache];

    // Ordenar por fecha desc (si se puede)
    arr.sort((a, b) => {
        const da = new Date(a.fecha).getTime();
        const db = new Date(b.fecha).getTime();
        if (Number.isNaN(da) || Number.isNaN(db)) return 0;
        return db - da;
    });

    // Filtrar
    if (currentFilter !== "all") {
        arr = arr.filter(t => classifyTx(t, myUserId) === currentFilter);
    }

    if (arr.length === 0) {
        if (empty) empty.style.display = "block";
        return;
    }
    if (empty) empty.style.display = "none";

    arr.forEach(t => {
        const type = classifyTx(t, myUserId);
        const amountClass = type === "sent" ? "sent" : (type === "received" ? "received" : "");
        const sign = type === "sent" ? "− " : (type === "received" ? "+ " : "");

        const title =
            type === "sent" ? "Transferencia enviada"
                : type === "received" ? "Transferencia recibida"
                    : "Movimiento";

        const desc = t.descripcion ? String(t.descripcion) : "";
        const estado = t.estado ? String(t.estado) : "";
        const fecha = formatDate(t.fecha);

        const li = document.createElement("li");
        li.className = "tx";
        li.innerHTML = `
      <div>
        <div class="tx-title">${title}</div>
        <div class="tx-meta">
          ${desc ? `${desc} • ` : ""}${estado ? `${estado} • ` : ""}${fecha}
        </div>
      </div>
      <div class="tx-amount ${amountClass}">
        ${sign}${formatMoney(t.monto)}
      </div>
    `;

        list.appendChild(li);
    });
}

async function cargarTransacciones() {
    const res = await fetch(`${API}/transacciones/historial`, { headers: authHeaders() });
    const data = await readBody(res);

    if (res.status === 401) {
        logout();
        return [];
    }

    if (!res.ok) {
        txCache = [];
        return [];
    }

    txCache = Array.isArray(data) ? data : [];
    return txCache;
}

// --------------------------- FILTER BUTTONS ---------------------------
function wireFilters(myUserId) {
    const buttons = document.querySelectorAll(".seg-btn");
    if (!buttons.length) return;

    buttons.forEach(btn => {
        btn.addEventListener("click", () => {
            buttons.forEach(b => b.classList.remove("active"));
            btn.classList.add("active");

            currentFilter = btn.getAttribute("data-filter") || "all";
            renderTxList(myUserId);
        });
    });
}

// --------------------------- ADD SALDO (demo) ---------------------------
async function agregarSaldoFlow() {
    const raw = prompt("¿Cuánto deseas recargar? (solo pruebas)", "1000");
    if (raw == null) return;

    const monto = Number(raw);
    if (!Number.isFinite(monto) || monto <= 0) {
        alert("Monto inválido");
        return;
    }

    const res = await fetch(`${API}/usuarios/agregar-saldo/${encodeURIComponent(monto)}`, {
        method: "POST",
        headers: authHeaders()
    });

    const data = await readBody(res);
    if (!res.ok) {
        alert(errorMessage(data, "No se pudo recargar"));
        return;
    }

    alert(typeof data === "string" ? data : "Saldo recargado");
    await initDashboardOrTransfer();
}

// --------------------------- TRANSFER PAGE ---------------------------
const transaccionForm = document.getElementById("transaccionForm");
if (transaccionForm) {
    requireAuthOrRedirect();

    const prefillBtn = document.getElementById("prefillBtn");
    if (prefillBtn) {
        prefillBtn.addEventListener("click", () => {
            const destinatario = document.getElementById("destinatario");
            const monto = document.getElementById("monto");
            const descripcion = document.getElementById("descripcion");
            destinatario.value = "demo@correo.com";
            monto.value = "500";
            descripcion.value = "Ejemplo";
        });
    }

    transaccionForm.addEventListener("submit", async (e) => {
        e.preventDefault();

        const emailDestino = document.getElementById("destinatario").value.trim();
        const montoValue = Number(document.getElementById("monto").value);
        const descValue = (document.getElementById("descripcion")?.value || "").trim();

        const res = await fetch(`${API}/transacciones/enviar`, {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
                ...authHeaders()
            },
            body: JSON.stringify({
                emailDestino,
                monto: montoValue,
                descripcion: descValue || "Transferencia"
            })
        });

        const data = await readBody(res);

        if (res.status === 401) {
            logout();
            return;
        }

        if (res.ok) {
            alert("Transferencia realizada");
            transaccionForm.reset();
            redirectTo("dashboard.html");
        } else {
            alert(errorMessage(data, "Error al enviar"));
        }
    });
}

// --------------------------- DASHBOARD INIT ---------------------------
async function initDashboardOrTransfer() {
    // Protege rutas
    if (isPage("dashboard.html") || isPage("transfer.html")) {
        requireAuthOrRedirect();
    }

    // Perfil
    const perfil = await cargarPerfil();

    // Dashboard: transacciones + stats + filtros
    if (isPage("dashboard.html")) {
        const refreshBtn = document.getElementById("refreshBtn");
        if (refreshBtn) {
            refreshBtn.addEventListener("click", async () => {
                await initDashboardOrTransfer();
            });
        }

        const addSaldoBtn = document.getElementById("addSaldoBtn");
        if (addSaldoBtn) addSaldoBtn.addEventListener("click", agregarSaldoFlow);

        await cargarTransacciones();
        renderStats(perfil?.id);
        wireFilters(perfil?.id);
        renderTxList(perfil?.id);
    }
}

initDashboardOrTransfer();