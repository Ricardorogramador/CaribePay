const API = "/api";

function getToken() { return localStorage.getItem("token"); }
function setToken(token) { localStorage.setItem("token", token); }
function clearToken() { localStorage.removeItem("token"); }

function authHeaders() {
    const token = getToken();
    return token ? { "Authorization": "Bearer " + token } : {};
}

function isPage(name) { return window.location.pathname.endsWith(name); }
function redirectTo(page) { window.location.href = page; }

function requireAuthOrRedirect() {
    if (!getToken()) redirectTo("index.html");
}

async function readBody(res) {
    const text = await res.text();
    try { return JSON.parse(text); } catch { return text; }
}

function errorMessage(data, fallback) {
    if (!data) return fallback;
    if (typeof data === "string") return data;
    return data.mensaje || data.message || fallback;
}

function formatMoney(n) {
    const value = Number(n ?? 0);
    return value.toLocaleString("es-CO", { style: "currency", currency: "COP", maximumFractionDigits: 0 });
}

function formatDate(d) {
    if (!d) return "";
    const date = new Date(d);
    if (Number.isNaN(date.getTime())) return String(d);
    return date.toLocaleString("es-CO");
}

function logout() {
    clearToken();
    redirectTo("index.html");
}

if (isPage("index.html") && getToken()) redirectTo("dashboard.html");

// LOGIN
const loginForm = document.getElementById("loginForm");
if (loginForm) {
    loginForm.addEventListener("submit", async (e) => {
        e.preventDefault();

        const emailEl = document.getElementById("email");
        const passEl = document.getElementById("password");
        if (!emailEl || !passEl) {
            alert("Error UI: faltan campos de login");
            return;
        }

        const res = await fetch(`${API}/auth/login`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({ email: emailEl.value.trim(), password: passEl.value })
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

// REGISTRO (si lo usas con teléfono)
const registerForm = document.getElementById("registerForm");
if (registerForm) {
    registerForm.addEventListener("submit", async (e) => {
        e.preventDefault();

        const emailEl = document.getElementById("email");
        const telEl = document.getElementById("telefono");
        const passEl = document.getElementById("password");

        if (!emailEl || !telEl || !passEl) {
            alert("Error UI: faltan campos (email/teléfono/contraseña)");
            return;
        }

        const res = await fetch(`${API}/auth/registro`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                email: emailEl.value.trim(),
                telefono: telEl.value.trim(),
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

const logoutBtn = document.getElementById("logoutBtn");
if (logoutBtn) logoutBtn.addEventListener("click", logout);

// PERFIL
let perfilCache = null;

async function cargarPerfil() {
    const res = await fetch(`${API}/usuarios/perfil`, { headers: authHeaders() });
    const data = await readBody(res);

    if (res.status === 401) { logout(); return null; }
    if (!res.ok) return null;

    perfilCache = data;

    document.querySelectorAll("#saldo").forEach(el => (el.textContent = formatMoney(data.saldo)));

    const welcomeText = document.getElementById("welcomeText");
    if (welcomeText) {
        const email = data?.email || "";
        const tel = data?.telefono ? ` • ${data.telefono}` : "";
        welcomeText.textContent = email ? `Conectado como ${email}${tel}` : "Bienvenido";
    }

    return data;
}

// TX
let txCache = [];
let currentFilter = "all";

function isRecarga(tx, myUserId) {
    const desc = (tx?.descripcion || "").toString().toUpperCase();
    return (desc.includes("RECARGA")) || (tx?.emisorId === myUserId && tx?.receptorId === myUserId);
}

function classifyTx(tx, myUserId) {
    if (!tx || !myUserId) return "unknown";
    if (isRecarga(tx, myUserId)) return "recarga";
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
    arr.sort((a, b) => {
        const da = new Date(a.fecha).getTime();
        const db = new Date(b.fecha).getTime();
        if (Number.isNaN(da) || Number.isNaN(db)) return 0;
        return db - da;
    });

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

        // Monto: recarga se muestra como +
        const sign = type === "sent" ? "− " : "+ ";
        const amountClass =
            type === "sent" ? "sent" :
                (type === "received" || type === "recarga") ? "received" : "";

        let title = "Movimiento";
        if (type === "recarga") {
            title = "Recarga de saldo";
        } else if (type === "sent") {
            title = `Enviado a ${t.telefonoReceptor ?? "usuario"}`;
        } else if (type === "received") {
            title = `Recibido de ${t.telefonoEmisor ?? "usuario"}`;
        }

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

    if (res.status === 401) { logout(); return []; }
    if (!res.ok) { txCache = []; return []; }

    txCache = Array.isArray(data) ? data : [];
    return txCache;
}

function wireFilters(myUserId) {
    const buttons = document.querySelectorAll(".seg-btn");
    if (!buttons.length) return;

    buttons.forEach(btn => {
        btn.addEventListener("click", () => {
            buttons.forEach(b => b.classList.remove("active"));
            btn.classList.add("active");

            const f = btn.getAttribute("data-filter") || "all";
            // si en el HTML no pusiste botón recarga, no pasa nada: queda en "all/sent/received"
            currentFilter = f;
            renderTxList(myUserId);
        });
    });
}

// RECARGA MODAL
function setupRecargaModal() {
    const modal = document.getElementById("recargaModal");
    const openBtn = document.getElementById("openRecargaBtn");
    const closeBtn = document.getElementById("closeRecargaBtn");
    const cancelBtn = document.getElementById("cancelRecargaBtn");
    const confirmBtn = document.getElementById("confirmRecargaBtn");
    const montoEl = document.getElementById("recargaMonto");
    const errEl = document.getElementById("recargaError");

    if (!modal || !openBtn || !closeBtn || !cancelBtn || !confirmBtn || !montoEl) return;

    function showError(msg) {
        if (!errEl) return;
        errEl.style.display = "block";
        errEl.textContent = msg;
    }

    function clearError() {
        if (!errEl) return;
        errEl.style.display = "none";
        errEl.textContent = "";
    }

    function open() {
        clearError();
        modal.classList.add("show");
        modal.setAttribute("aria-hidden", "false");
        setTimeout(() => montoEl.focus(), 50);
    }

    function close() {
        modal.classList.remove("show");
        modal.setAttribute("aria-hidden", "true");
    }

    openBtn.addEventListener("click", open);
    closeBtn.addEventListener("click", close);
    cancelBtn.addEventListener("click", close);

    modal.addEventListener("click", (e) => {
        const t = e.target;
        if (t && t.getAttribute && t.getAttribute("data-close") === "true") close();
    });

    document.addEventListener("keydown", (e) => {
        if (e.key === "Escape" && modal.classList.contains("show")) close();
    });

    montoEl.addEventListener("keydown", (e) => {
        if (e.key === "Enter") {
            e.preventDefault();
            confirmBtn.click();
        }
    });

    confirmBtn.addEventListener("click", async () => {
        clearError();

        const monto = Number(montoEl.value);
        if (!Number.isFinite(monto) || monto <= 0) {
            showError("Monto inválido. Debe ser mayor a 0.");
            return;
        }

        confirmBtn.disabled = true;
        confirmBtn.textContent = "Recargando...";

        try {
            const res = await fetch(`${API}/usuarios/agregar-saldo/${encodeURIComponent(monto)}`, {
                method: "POST",
                headers: authHeaders()
            });

            const data = await readBody(res);

            if (res.status === 401) { logout(); return; }

            if (!res.ok) {
                showError(errorMessage(data, "No se pudo recargar."));
                return;
            }

            close();
            await initDashboard();
        } catch {
            showError("Error de red. Intenta de nuevo.");
        } finally {
            confirmBtn.disabled = false;
            confirmBtn.textContent = "Recargar";
        }
    });
}

// TRANSFER page (si existe)
const transaccionForm = document.getElementById("transaccionForm");
if (transaccionForm) {
    requireAuthOrRedirect();

    const prefillBtn = document.getElementById("prefillBtn");
    if (prefillBtn) {
        prefillBtn.addEventListener("click", () => {
            const tel = document.getElementById("telefonoDestino");
            const monto = document.getElementById("monto");
            const desc = document.getElementById("descripcion");
            if (tel) tel.value = "3123456789";
            if (monto) monto.value = "5000";
            if (desc) desc.value = "Ejemplo";
        });
    }

    transaccionForm.addEventListener("submit", async (e) => {
        e.preventDefault();

        const telefonoDestino = document.getElementById("telefonoDestino")?.value?.trim() || "";
        const montoValue = Number(document.getElementById("monto")?.value);
        const descValue = (document.getElementById("descripcion")?.value || "").trim();

        const res = await fetch(`${API}/transacciones/enviar`, {
            method: "POST",
            headers: { "Content-Type": "application/json", ...authHeaders() },
            body: JSON.stringify({
                telefonoDestino,
                monto: montoValue,
                descripcion: descValue || "Transferencia"
            })
        });

        const data = await readBody(res);

        if (res.status === 401) { logout(); return; }

        if (res.ok) {
            alert("Transferencia realizada");
            transaccionForm.reset();
            redirectTo("dashboard.html");
        } else {
            alert(errorMessage(data, "Error al enviar"));
        }
    });
}

// INIT
async function initDashboard() {
    const perfil = await cargarPerfil();
    await cargarTransacciones();
    renderStats(perfil?.id);
    wireFilters(perfil?.id);
    renderTxList(perfil?.id);
}

async function init() {
    if (isPage("dashboard.html") || isPage("transfer.html")) requireAuthOrRedirect();

    const refreshBtn = document.getElementById("refreshBtn");
    if (refreshBtn) refreshBtn.addEventListener("click", initDashboard);

    setupRecargaModal();
    await cargarPerfil();

    if (isPage("dashboard.html")) {
        await initDashboard();
    }
}

init();