const API = "/api";

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
            // 1. Buscar usuario por teléfono
            console.log("Buscando usuario con teléfono:", telefono);

            // Usar una búsqueda alternativa: intenta login para verificar que existe
            // En un cajero real, buscaríamos por teléfono en la BD
            // Por ahora, haremos una verificación simple

            // 2. Hacer recarga sin autenticación (simulamos que es un cajero)
            // Esto requiere un endpoint especial que crearemos en el backend

            const res = await fetch(`${API}/cajero/recargar`, {
                method: "POST",
                headers: { "Content-Type": "application/json" },
                body: JSON.stringify({
                    telefono: telefono,
                    monto: montoValue,
                    email: email,
                    descripcion: descripcion
                })
            });

            const data = await readBody(res);

            if (res.ok) {
                showSuccess(
                    `✅ Recarga exitosa de $${formatMoney(montoValue)}\n` +
                    `Teléfono: ${telefono}\n` +
                    `Referencia: ${data.id || "N/A"}`
                );
                cajeroRecargaForm.reset();
                document.getElementById("cajeroMonto").value = "10000";
            } else {
                showError(errorMessage(data, "No se pudo procesar la recarga"));
            }
        } catch (err) {
            console.error("Error:", err);
            showError("Error de conexión. Intenta de nuevo.");
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
    }
}

function showSuccess(msg) {
    if (cajeroSuccess) {
        cajeroSuccess.style.display = "block";
        cajeroSuccess.innerHTML = msg.replace(/\n/g, "<br>");
        cajeroSuccess.scrollIntoView({ behavior: "smooth", block: "center" });
        setTimeout(() => {
            cajeroSuccess.style.display = "none";
        }, 5000);
    }
}

function clearMessages() {
    if (cajeroError) cajeroError.style.display = "none";
    if (cajeroSuccess) cajeroSuccess.style.display = "none";
}