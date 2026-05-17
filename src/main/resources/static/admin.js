// ==================== UTILIDADES ====================

function adminFormatMoney(n) {
    const value = Number(n ?? 0);
    return value.toLocaleString("es-CO", { style: "currency", currency: "COP", maximumFractionDigits: 0 });
}

function adminFormatDate(d) {
    if (!d) return "—";
    const date = new Date(d);
    if (Number.isNaN(date.getTime())) return String(d);
    return date.toLocaleString("es-CO");
}

async function adminReadBody(res) {
    const text = await res.text();
    try { return JSON.parse(text); } catch { return text; }
}

function adminLogout() {
    localStorage.removeItem("token");
    window.location.href = "landing.html";
}

function adminRedirectTo(page) {
    window.location.href = page;
}

function adminAuthHeaders() {
    const token = localStorage.getItem("token");
    return token ? { "Authorization": "Bearer " + token } : {};
}

// ==================== ESTADO GLOBAL ====================

let adminFiltroActual = "todos";
let adminUsuariosCache = [];

// ==================== FUNCIONES PRINCIPALES ====================

async function adminCargarEstadisticas() {
    console.log("📊 Cargando estadísticas...");
    try {
        const res = await fetch(`${API}/admin/estadisticas`, { headers: adminAuthHeaders() });

        if (res.status === 403) {
            console.error("❌ Sin permisos de admin");
            alert("No tienes permisos de administrador");
            adminRedirectTo("landing.html");
            return;
        }

        if (!res.ok) {
            console.error("❌ Error:", res.status);
            return;
        }

        const data = await adminReadBody(res);
        console.log("✅ Estadísticas:", data);

        const totalUsuariosEl = document.getElementById("totalUsuarios");
        const usuariosActivosEl = document.getElementById("usuariosActivos");
        const usuariosDesactivadosEl = document.getElementById("usuariosDesactivados");
        const saldoTotalEl = document.getElementById("saldoTotal");

        if (totalUsuariosEl) totalUsuariosEl.textContent = data.totalUsuarios || 0;
        if (usuariosActivosEl) usuariosActivosEl.textContent = data.usuariosActivos || 0;
        if (usuariosDesactivadosEl) usuariosDesactivadosEl.textContent = data.usuariosDesactivados || 0;
        if (saldoTotalEl) saldoTotalEl.textContent = adminFormatMoney(data.saldoTotalCirculante || 0);

    } catch (e) {
        console.error("❌ Error cargando estadísticas:", e);
    }
}

async function adminCargarUsuarios(filtro = "todos") {
    console.log("👥 Cargando usuarios:", filtro);
    try {
        let url = `${API}/admin/usuarios`;
        if (filtro === "activos") url += "/activos";
        else if (filtro === "desactivados") url += "/desactivados";

        console.log("📡 URL:", url);
        const res = await fetch(url, { headers: adminAuthHeaders() });

        console.log("📡 Status:", res.status);

        if (!res.ok) {
            console.error("❌ Error:", res.status);
            const error = await adminReadBody(res);
            console.error("❌ Error detail:", error);
            return;
        }

        adminUsuariosCache = await adminReadBody(res);
        console.log("✅ Usuarios cargados:", adminUsuariosCache.length);

        adminRenderizarTabla(adminUsuariosCache);

    } catch (e) {
        console.error("❌ Error cargando usuarios:", e);
    }
}

function adminRenderizarTabla(usuarios) {
    const tbody = document.getElementById("usuariosTableBody");
    if (!tbody) {
        console.error("❌ No se encontró tbody");
        return;
    }

    tbody.innerHTML = "";

    if (!usuarios || usuarios.length === 0) {
        console.warn("⚠️ No hay usuarios");
        tbody.innerHTML = '<tr><td colspan="6" class="text-center muted">No hay usuarios</td></tr>';
        return;
    }

    console.log("📝 Renderizando", usuarios.length, "usuarios");

    usuarios.forEach(u => {
        const estado = u.activo ? "✅ Activo" : "❌ Desactivado";
        const estadoClass = u.activo ? "estado-activo" : "estado-inactivo";
        const btnTexto = u.activo ? "Desactivar" : "Activar";
        const btnClase = u.activo ? "btn-soft" : "btn-primary";

        const tr = document.createElement("tr");
        tr.innerHTML = `
            <td><strong>${u.email}</strong></td>
            <td>${u.telefono}</td>
            <td>${adminFormatMoney(u.saldo)}</td>
            <td><span class="badge ${estadoClass}">${estado}</span></td>
            <td>${adminFormatDate(u.fechaCreacion)}</td>
            <td>
                <button class="btn btn-sm ${btnClase}" onclick="adminCambiarEstado('${u.id}', ${u.activo})">
                    ${btnTexto}
                </button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

async function adminCambiarEstado(usuarioId, activo) {
    if (!confirm(`¿${activo ? "Desactivar" : "Activar"} este usuario?`)) return;

    try {
        const endpoint = activo ? "desactivar" : "activar";
        const res = await fetch(`${API}/admin/usuarios/${usuarioId}/${endpoint}`, {
            method: "POST",
            headers: adminAuthHeaders()
        });

        if (res.ok) {
            alert("✅ Operación exitosa");
            adminCargarUsuarios(adminFiltroActual);
            adminCargarEstadisticas();
        } else {
            alert("❌ Error al cambiar estado");
        }
    } catch (e) {
        console.error("❌ Error:", e);
        alert("Error de conexión");
    }
}

// ==================== FUNCIONES DE NAVEGACIÓN ====================

function adminMostrarEstadisticas() {
    console.log("📊 Mostrando estadísticas");
    const est = document.getElementById("seccionEstadisticas");
    const usr = document.getElementById("seccionUsuarios");
    if (est) est.style.display = "grid";
    if (usr) usr.style.display = "none";
    adminCargarEstadisticas();
}

function adminMostrarUsuarios() {
    console.log("👥 Mostrando usuarios");
    const est = document.getElementById("seccionEstadisticas");
    const usr = document.getElementById("seccionUsuarios");
    if (est) est.style.display = "none";
    if (usr) usr.style.display = "block";
    adminCargarUsuarios("todos");
}

function adminFiltrarUsuarios(filtro) {
    console.log("🔍 Filtrando por:", filtro);
    adminFiltroActual = filtro;
    adminCargarUsuarios(filtro);
}

function adminRecargarDatos() {
    console.log("🔄 Recargando datos...");
    adminCargarEstadisticas();
    adminCargarUsuarios(adminFiltroActual);
}

// ==================== INICIALIZACIÓN ====================

document.addEventListener("DOMContentLoaded", function() {
    console.log("🚀 Inicializando panel de admin...");

    const logoutBtn = document.getElementById("logoutBtn");
    if (logoutBtn) {
        logoutBtn.addEventListener("click", adminLogout);
    }

    // Cargar datos al iniciar
    adminMostrarEstadisticas();
});