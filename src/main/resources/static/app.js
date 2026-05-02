const API = "/api";

function getToken() {
    return localStorage.getItem("token");
}

function authHeaders() {
    return { "Authorization": "Bearer " + getToken() };
}

function requireAuthOrRedirect() {
    if (!getToken()) window.location = "index.html";
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

// LOGIN
const loginForm = document.getElementById("loginForm");
if (loginForm) {
    loginForm.addEventListener("submit", async (e) => {
        e.preventDefault();

        const res = await fetch(`${API}/auth/login`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                email: email.value,
                password: password.value
            })
        });

        const data = await readBody(res);

        if (res.ok && data && typeof data === "object" && data.token) {
            localStorage.setItem("token", data.token);
            window.location = "dashboard.html";
        } else {
            alert(errorMessage(data, "Credenciales incorrectas"));
        }
    });
}

// REGISTRO
const registerForm = document.getElementById("registerForm");
if (registerForm) {
    registerForm.addEventListener("submit", async (e) => {
        e.preventDefault();

        const res = await fetch(`${API}/auth/registro`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify({
                email: email.value,
                password: password.value
            })
        });

        const data = await readBody(res);

        if (res.ok) {
            alert("Cuenta creada. Ahora inicia sesión.");
            window.location = "index.html";
        } else {
            alert(errorMessage(data, "Error en registro"));
        }
    });
}

// PERFIL
async function cargarPerfil() {
    const res = await fetch(`${API}/usuarios/perfil`, {
        headers: authHeaders()
    });

    const data = await readBody(res);
    if (!res.ok) return;

    const saldoEl = document.getElementById("saldo");
    if (saldoEl && data && typeof data === "object") {
        saldoEl.textContent = `$ ${data.saldo ?? 0}`;
    }
}

// TRANSACCIONES
async function cargarTransacciones() {
    const res = await fetch(`${API}/transacciones/historial`, {
        headers: authHeaders()
    });

    const data = await readBody(res);
    const lista = document.getElementById("listaTransacciones");
    if (!lista) return;

    lista.innerHTML = "";

    if (!res.ok) {
        const li = document.createElement("li");
        li.className = "item";
        li.textContent = errorMessage(data, "No se pudieron cargar las transacciones.");
        lista.appendChild(li);
        return;
    }

    const arr = Array.isArray(data) ? data : [];
    if (arr.length === 0) {
        const li = document.createElement("li");
        li.className = "item";
        li.textContent = "Sin movimientos todavía.";
        lista.appendChild(li);
        return;
    }

    arr.forEach(t => {
        const li = document.createElement("li");
        li.className = "item";

        const monto = t.monto ?? 0;
        const desc = t.descripcion ?? "";
        const estado = t.estado ?? "";
        const fecha = t.fecha ? new Date(t.fecha).toLocaleString() : "";

        li.innerHTML = `
      <div><strong>$${monto}</strong> ${desc ? "- " + desc : ""} ${estado ? "(" + estado + ")" : ""}</div>
      <div style="font-size:12px; opacity:.8">${fecha}</div>
    `;
        lista.appendChild(li);
    });
}

// ENVIAR
const transaccionForm = document.getElementById("transaccionForm");
if (transaccionForm) {
    requireAuthOrRedirect();

    transaccionForm.addEventListener("submit", async (e) => {
        e.preventDefault();

        const emailDestino = destinatario.value.trim();
        const montoValue = Number(monto.value);
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

        if (res.ok) {
            alert("Enviado 💸");
            transaccionForm.reset();
            await cargarPerfil();
            await cargarTransacciones();
        } else {
            alert(errorMessage(data, "Error al enviar"));
        }
    });
}

// LOGOUT
function logout() {
    localStorage.removeItem("token");
    window.location = "index.html";
}

// Auto-cargar dashboard
if (window.location.pathname.endsWith("dashboard.html")) {
    requireAuthOrRedirect();
    cargarPerfil();
    cargarTransacciones();
}