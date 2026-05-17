// NO DECLARAR API aquí, ya está en app.js

const cajeroRecargaForm = document.getElementById("cajeroRecargaForm");
const cajeroError = document.getElementById("cajeroError");
const cajeroSuccess = document.getElementById("cajeroSuccess");

if (cajeroRecargaForm) {
    cajeroRecargaForm.addEventListener("submit", async (e) => {
        e.preventDefault();

        clearMessages();

        const telefono = document.getElementById("cajeroTelefono")?.value?.trim() || "";
        const montoValue = Number(document.getElementById("cajeroMonto")?.value);
        const email = document.getElementById("cajeroEmail")?.value?.trim() || "";
        const descripcion = (document.getElementById("cajeroDescripcion")?.value || "").trim();

        console.log("🏧 Iniciando recarga en cajero...");
        console.log("Teléfono:", telefono);
        console.log("Monto:", montoValue);
        console.log("Email:", email);

        // Validaciones básicas
        if (!telefono) {
            showError("Por favor ingresa tu teléfono");
            return;
        }

        if (!email) {
            showError("Por favor ingresa tu email");
            return;
        }

        if (!Number.isFinite(montoValue) || montoValue < 1000) {
            showError("El monto debe ser mínimo $1.000");
            return;
        }

        const submitBtn = cajeroRecargaForm.querySelector("button[type='submit']");
        submitBtn.disabled = true;
        submitBtn.textContent = "Procesando...";

        try {
            console.log("📤 Enviando solicitud a:", `${API}/cajero/recargar`);

            const res = await fetch(`${API}/cajero/recargar`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    telefono: telefono,
                    monto: montoValue,
                    email: email,
                    descripcion: descripcion || "Recarga desde cajero"
                })
            });

            console.log("📡 Status recibido:", res.status);

            const data = await readBody(res);
            console.log("📡 Respuesta del servidor:", data);

            if (res.ok && data.exitoso) {
                console.log("✅ Recarga exitosa");
                showSuccess(
                    `✅ Recarga exitosa de ${formatMoney(montoValue)}\n` +
                    `Teléfono: ${telefono}\n` +
                    `Nuevo saldo: ${formatMoney(data.nuevoSaldo || 0)}\n` +
                    `Referencia: ${data.id || "N/A"}`
                );
                cajeroRecargaForm.reset();
                document.getElementById("cajeroMonto").value = "10000";
            } else {
                console.error("❌ Recarga fallida:", data);
                const mensaje = data.mensaje || data.message || "No se pudo procesar la recarga";
                showError(mensaje);
            }
        } catch (err) {
            console.error("❌ Error de conexión:", err);
            showError("Error de conexión. Verifica tu conexión e intenta de nuevo.");
        } finally {
            submitBtn.disabled = false;
            submitBtn.textContent = "Recargar Ahora";
        }
    });
}

function showError(msg) {
    if (cajeroError) {
        cajeroError.style.display = "block";
        cajeroError.textContent = msg;
        cajeroError.scrollIntoView({ behavior: "smooth", block: "center" });
        console.error("🔴 Error mostrado:", msg);
    }
}

function showSuccess(msg) {
    if (cajeroSuccess) {
        cajeroSuccess.style.display = "block";
        cajeroSuccess.innerHTML = msg.replace(/\n/g, "<br>");
        cajeroSuccess.scrollIntoView({ behavior: "smooth", block: "center" });
        console.log("🟢 Éxito mostrado:", msg);
        setTimeout(() => {
            cajeroSuccess.style.display = "none";
        }, 5000);
    }
}

function clearMessages() {
    if (cajeroError) cajeroError.style.display = "none";
    if (cajeroSuccess) cajeroSuccess.style.display = "none";
}