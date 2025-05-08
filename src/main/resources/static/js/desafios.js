/**
 * Funcionalidad para gestión de desafíos
 * Este archivo maneja toda la funcionalidad relacionada con desafíos
 */

// Variables globales
// let challengesData = [];
let initializationAttempts = 0;
const MAX_INIT_ATTEMPTS = 3;

// Punto de entrada principal
function initializeChallenges() {
    console.log("▶️ Inicializando módulo de desafíos...");
    
    try {
        // Configurar botón de nuevo desafío
        setupNewChallengeButton();
        
        // Configurar formulario de desafíos
        setupChallengeForm();
        
        // Configurar botones de cierre del formulario
        setupCloseButtons();
        
        // Exportar funciones al ámbito global para que puedan ser llamadas desde el HTML
        exportGlobalFunctions();
        
        console.log("✅ Módulo de desafíos inicializado correctamente");
        
        // Cargar desafíos solo si la pestaña de desafíos está activa y visible
        const challengesTabElement = document.getElementById('challengesTab');
        if (challengesTabElement && challengesTabElement.classList.contains('active') && window.getComputedStyle(challengesTabElement).display === 'block') {
            console.log("📋 Pestaña de desafíos activa y visible, cargando desafíos iniciales...");
            loadChallenges();
        } else {
            console.log("ℹ️ Pestaña de desafíos no activa/visible en la inicialización, no se cargan desafíos aún.");
        }
    } catch (error) {
        console.error("❌ Error al inicializar módulo de desafíos:", error);
        
        // Reintentar inicialización después de un retraso si no superamos el límite
        if (initializationAttempts < MAX_INIT_ATTEMPTS) {
            initializationAttempts++;
            console.log(`🔄 Reintentando inicialización (${initializationAttempts}/${MAX_INIT_ATTEMPTS})...`);
            setTimeout(initializeChallenges, 500);
        } else {
            console.error("❌ Se superó el límite de intentos de inicialización");
            showNotification("Hubo un problema al cargar el módulo de desafíos. Por favor, recargue la página.", "error");
        }
    }
}

// Configura el botón de nuevo desafío
function setupNewChallengeButton() {
    const newChallengeButton = document.getElementById('newChallengeButton');
    if (!newChallengeButton) {
        throw new Error("No se encontró el botón de nuevo desafío (ID: newChallengeButton)");
    }
    
    console.log("🔘 Configurando botón de nuevo desafío");
    
    // Eliminar eventos anteriores para evitar duplicados
    newChallengeButton.removeEventListener('click', openChallengeForm);
    
    // Añadir el evento actualizado
    newChallengeButton.addEventListener('click', function(event) {
        event.preventDefault();
        console.log("🖱️ Botón de nuevo desafío clickeado");
        openChallengeForm();
    });
    
    // Asegurarse de que el atributo onclick también llame a la función correcta
    newChallengeButton.setAttribute('onclick', 'openChallengeForm(); return false;');
}

// Configura el formulario de desafíos
function setupChallengeForm() {
    const challengeForm = document.getElementById('newChallengeForm');
    if (!challengeForm) {
        throw new Error("No se encontró el formulario de desafíos (ID: newChallengeForm)");
    }
    
    console.log("📝 Configurando formulario de desafíos");
    
    // Eliminar eventos anteriores para evitar duplicados
    challengeForm.removeEventListener('submit', handleChallengeSubmit);
    
    // Añadir el evento actualizado
    challengeForm.addEventListener('submit', function(event) {
        event.preventDefault();
        console.log("📨 Formulario de desafío enviado");
        handleChallengeSubmit(event);
    });
}

// Configura los botones de cierre
function setupCloseButtons() {
    console.log("🔴 Configurando botones de cierre");
    
    const closeButtons = document.querySelectorAll('button[onclick="closeForm()"]');
    if (closeButtons.length === 0) {
        console.warn("⚠️ No se encontraron botones de cierre con onclick='closeForm()'");
    }
    
    closeButtons.forEach(button => {
        button.removeAttribute('onclick');
        button.addEventListener('click', function(event) {
            event.preventDefault();
            console.log("🖱️ Botón de cierre clickeado");
            closeForm();
        });
    });
}

// Exporta las funciones al ámbito global
function exportGlobalFunctions() {
    console.log("🌐 Exportando funciones al ámbito global");
    
    window.openChallengeForm = openChallengeForm;
    window.closeForm = closeForm;
    window.handleChallengeSubmit = handleChallengeSubmit;
    window.loadChallenges = loadChallenges;
    window.toggleChallengeForm = openChallengeForm; // Compatibilidad con función original
}

/**
 * Abre el formulario de creación de desafíos
 */
function openChallengeForm() {
    console.log("🔓 Abriendo formulario de desafíos...");
    
    // Verificar si existe el contenedor actualizado
    const formContainer = document.getElementById('challengeFormContainer');
    if (formContainer) {
        formContainer.style.display = 'flex';
        
        // Cargar proyectos en el formulario
        loadProjectsForChallengeForm();
        
        // Configurar fecha mínima como hoy
        const today = new Date().toISOString().split('T')[0];
        const startDate = document.getElementById('challengeStartDate');
        const endDate = document.getElementById('challengeEndDate');
        
        if (startDate) startDate.min = today;
        if (endDate) endDate.min = today;
        
        // Limpiar formulario
        const challengeForm = document.getElementById('newChallengeForm');
        if (challengeForm) challengeForm.reset();
        
        // Mostrar notificación amigable
        showNotification("Complete el formulario para crear un desafío", "info");
    } else {
        console.error("❌ No se encontró el contenedor del formulario de desafíos (ID: challengeFormContainer)");
        alert("Error: No se pudo abrir el formulario de desafíos");
    }
}

/**
 * Cierra el formulario de desafíos
 */
function closeForm() {
    console.log("🔒 Cerrando formulario de desafíos");
    
    const formContainer = document.getElementById('challengeFormContainer');
    if (formContainer) {
        formContainer.style.display = 'none';
    } else {
        console.warn("⚠️ No se encontró el contenedor del formulario de desafíos para cerrarlo");
    }
}

/**
 * Carga los proyectos disponibles en el formulario de desafíos
 */
function loadProjectsForChallengeForm() {
    const projectSelect = document.getElementById('challengeProject');
    const projectError = document.getElementById('projectError');
    
    if (!projectSelect || !projectError) {
        console.error("No se encontraron elementos del formulario");
        return;
    }

    // Mostrar mensaje de carga
    projectSelect.innerHTML = '<option value="">Cargando proyectos...</option>';
    projectSelect.disabled = true;

    fetch('/api/proyectos')
        .then(response => {
            if (!response.ok) {
                throw new Error(`Error ${response.status} obteniendo proyectos`);
            }
            return response.json();
        })
        .then(data => {
            projectSelect.disabled = false;
            projectSelect.innerHTML = '<option value="">Seleccione un proyecto</option>';

            if (data.length === 0) {
                projectError.textContent = 'No hay proyectos disponibles. Debe crear un proyecto primero.';
                projectError.style.display = 'block';
                return;
            }

            projectError.style.display = 'none';

            data.forEach(project => {
                const option = document.createElement('option');
                option.value = project.id;
                option.textContent = project.nombre;
                projectSelect.appendChild(option);
            });

            // Añadir evento de cambio para cargar foros cuando se selecciona un proyecto
            projectSelect.addEventListener('change', function() {
                const selectedProjectId = this.value;
                if (selectedProjectId) {
                    loadForosForProject(selectedProjectId);
                } else {
                    // Si no hay proyecto seleccionado, limpiar el select de foros
                    const forumSelect = document.getElementById('challengeForum');
                    if (forumSelect) {
                        forumSelect.innerHTML = '<option value="">Seleccione un foro</option>';
                    }
                }
            });
        })
        .catch(error => {
            console.error('Error al cargar proyectos para el formulario:', error);
            projectSelect.disabled = false;
            projectSelect.innerHTML = '<option value="">Error al cargar proyectos</option>';
            projectError.textContent = 'Error al cargar proyectos. Por favor, recargue la página.';
            projectError.style.display = 'block';
        });
}

/**
 * Carga los foros disponibles para un proyecto seleccionado
 */
function loadForosForProject(projectId) {
    const forumSelect = document.getElementById('challengeForum');
    const forumError = document.getElementById('forumError');
    
    if (!forumSelect || !forumError) {
        console.error("No se encontraron elementos del formulario de foros");
        return;
    }

    // Mostrar mensaje de carga
    forumSelect.innerHTML = '<option value="">Cargando foros...</option>';
    forumSelect.disabled = true;

    fetch(`/api/foros/proyecto/${projectId}`)
        .then(response => {
            if (!response.ok) {
                throw new Error(`Error ${response.status} obteniendo foros`);
            }
            return response.json();
        })
        .then(data => {
            forumSelect.disabled = false;
            forumSelect.innerHTML = '<option value="">Seleccione un foro</option>';

            // Procesamos la respuesta correctamente, sea un array u objeto paginado
            let forosArray = data;
            
            // Si es un objeto paginado con propiedad content
            if (data && typeof data === 'object' && Array.isArray(data.content)) {
                forosArray = data.content;
            }
            
            // Si no es ninguno de los formatos esperados o está vacío
            if (!Array.isArray(forosArray) || forosArray.length === 0) {
                forumError.textContent = 'No hay foros disponibles para este proyecto.';
                forumError.style.display = 'block';
                return;
            }

            forumError.style.display = 'none';

            forosArray.forEach(forum => {
                const option = document.createElement('option');
                option.value = forum.id;
                // Intentar obtener el nombre del foro de forma segura
                option.textContent = forum.titulo || forum.nombre || `Foro (ID: ${forum.id})`;
                forumSelect.appendChild(option);
            });
        })
        .catch(error => {
            console.error('Error al cargar foros para el proyecto:', error);
            forumSelect.disabled = false;
            forumSelect.innerHTML = '<option value="">Error al cargar foros</option>';
            forumError.textContent = 'Error al cargar foros. Verifique que existan foros para este proyecto.';
            forumError.style.display = 'block';
        });
}

/**
 * Maneja el envío del formulario de desafío
 */
function handleChallengeSubmit(event) {
    event.preventDefault();

    // Mostrar indicador de carga
    const submitBtn = event.target.querySelector('button[type="submit"]');
    const originalBtnText = submitBtn.innerHTML;
    submitBtn.disabled = true;
    submitBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Guardando...';

    // Recopilar datos del formulario
    const startDate = document.getElementById('challengeStartDate').value;
    const endDate = document.getElementById('challengeEndDate').value;

    // Convertir fechas a formato compatible con LocalDateTime
    const fechaInicio = startDate ? new Date(startDate).toISOString() : new Date().toISOString();
    const fechaFin = endDate ? new Date(endDate).toISOString() : null;
    
    // Obtener tipo de desafío del select
    const tipoSelect = document.getElementById('challengeType');
    const tipo = tipoSelect ? tipoSelect.value : 'INDIVIDUAL'; // Valor por defecto

    const challengeData = {
        nombre: document.getElementById('challengeName').value,
        descripcion: document.getElementById('challengeDescription').value,
        fechaInicio: fechaInicio,
        fechaFin: fechaFin,
        puntosRecompensa: parseInt(document.getElementById('challengePoints').value) || 0,
        proyectoId: document.getElementById('challengeProject').value,
        tipo: tipo, // Usar el tipo seleccionado o el valor predeterminado
        foroId: document.getElementById('challengeForum').value || null,
        criterios: [] // Array vacío por defecto
    };

    // Validaciones básicas antes de enviar
    if (!challengeData.nombre || !challengeData.fechaInicio || !challengeData.fechaFin || !challengeData.tipo || !challengeData.proyectoId) {
        showNotification('Por favor, completa los campos obligatorios: Título, Proyecto, Fecha de Inicio, Fecha de Fin y Tipo de Desafío.', 'error');
        submitBtn.disabled = false;
        submitBtn.innerHTML = originalBtnText;
        return;
    }

    // Validar que la fecha de fin sea posterior a la de inicio
    if (new Date(fechaInicio) >= new Date(fechaFin)) {
        showNotification('La fecha de fin debe ser posterior a la fecha de inicio.', 'error');
        submitBtn.disabled = false;
        submitBtn.innerHTML = originalBtnText;
        return;
    }

    console.log("Enviando datos del desafío:", challengeData);

    // Enviar datos al servidor con los headers adecuados
    fetch('/api/desafios', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'Accept': 'application/json',
            'X-Requested-With': 'XMLHttpRequest'
        },
        credentials: 'include', // Incluir cookies para autenticación
        body: JSON.stringify(challengeData)
    })
        .then(response => {
            if (!response.ok) {
                return response.text().then(text => {
                    // Intentar analizar como JSON primero
                    try {
                        const json = JSON.parse(text);
                        throw new Error(`Error ${response.status}: ${json.message || json.error || response.statusText}`);
                    } catch (e) {
                        // Si no es JSON, comprobar si es HTML
                        if (text.includes('<!DOCTYPE html>') || text.includes('<html')) {
                            if (text.includes('login') || text.includes('iniciar sesión')) {
                                throw new Error('Tu sesión ha expirado. Por favor, inicia sesión nuevamente.');
                            } else {
                                throw new Error(`Error del servidor (${response.status}). Verifica la configuración y autenticación.`);
                            }
                        } else {
                            throw new Error(`Error ${response.status}: ${text || response.statusText}`);
                        }
                    }
                });
            }
            return response.json();
        })
        .then(newChallenge => {
            submitBtn.disabled = false;
            submitBtn.innerHTML = originalBtnText;

            console.log("Desafío creado:", newChallenge);
            closeForm();

            // Recargar todos los desafíos
            setTimeout(loadChallenges, 200);

            showNotification('Desafío creado con éxito', 'success');
        })
        .catch(error => {
            console.error('Error al crear desafío:', error);
            submitBtn.disabled = false;
            submitBtn.innerHTML = originalBtnText;

            showNotification('Error al crear el desafío: ' + error.message, 'error');
        });
}

/**
 * Carga los desafíos disponibles
 */
function loadChallenges() {
    const challengesGrid = document.querySelector('.challenges-grid');
    if (!challengesGrid) {
        console.error("No se encontró el contenedor de desafíos");
        return;
    }
    
    challengesGrid.innerHTML = '<div class="no-data" style="text-align: center; padding: 20px; color: #666;">Cargando desafíos...</div>';

    console.log("Cargando desafíos...");
    
    // Añadir headers de autenticación y JSON
    const fetchOptions = {
        method: 'GET',
        headers: {
            'Accept': 'application/json',
            'X-Requested-With': 'XMLHttpRequest'
        },
        credentials: 'include' // Incluir cookies para la autenticación
    };
    
    fetch('/api/desafios/paginados', fetchOptions)
        .then(response => {
            if (!response.ok) {
                // Obtener más detalles sobre el error
                return response.text().then(text => {
                    console.error(`Error HTTP ${response.status}: Respuesta del servidor:`, text.substring(0, 500) + '...');
                    
                    if (text.includes('<!DOCTYPE html>') || text.includes('<html')) {
                        // La respuesta es HTML, probablemente un error 401/403 redirigiendo a login
                        if (text.includes('login') || text.includes('iniciar sesión')) {
                            throw new Error(`Error de autenticación. Es posible que tu sesión haya expirado.`);
                        }
                    }
                    
                    throw new Error(`Error ${response.status}: ${response.statusText}`);
                });
            }
            
            console.log("Respuesta de desafíos recibida correctamente");
            return response.json();
        })
        .then(data => {
            console.log("Datos de desafíos recibidos:", data);
            
            // Manejar objeto paginado (Spring Data)
            if (data && typeof data === 'object') {
                // Si es un objeto paginado
                if (Array.isArray(data.content)) {
                    console.log(`Detectada respuesta paginada con ${data.content.length} desafíos`);
                    // Guardar referencia global a los datos de desafíos
                    window.challengesData = data.content;
                    // Mostrar desafíos
                    displayChallenges(data);
                    return;
                }
                // Si es un array directamente
                else if (Array.isArray(data)) {
                    console.log(`Detectado array directo con ${data.length} desafíos`);
                    // Guardar referencia global a los datos de desafíos
                    window.challengesData = data;
                    // Mostrar desafíos
                    displayChallenges(data);
                    return;
                }
            }
            
            // Si llegamos aquí, el formato no es reconocido
            console.warn("Formato de respuesta no reconocido:", data);
            displayChallenges([]); // Mostrar como vacío
        })
        .catch(error => {
            console.error('Error al cargar desafíos:', error);
            let errorMessage = error.message || 'Error desconocido al cargar los desafíos';
            
            challengesGrid.innerHTML = `
                <div style="text-align: center; padding: 20px;">
                    <div style="color: #d32f2f; margin-bottom: 15px;">
                        <i class="fas fa-exclamation-circle" style="font-size: 24px;"></i>
                        <p style="margin-top: 10px;">${errorMessage}</p>
                    </div>
                    <button onclick="loadChallenges()" style="
                        margin-top: 10px;
                        padding: 8px 16px;
                        background: var(--primary-color, #4caf50);
                        color: white;
                        border: none;
                        border-radius: 4px;
                        cursor: pointer;
                    "><i class="fas fa-sync-alt"></i> Reintentar</button>
                </div>`;
        });
}

/**
 * Muestra los desafíos en la interfaz
 */
function displayChallenges(challenges) {
    const challengesGrid = document.querySelector('.challenges-grid');
    if (!challengesGrid) {
        console.error("No se encontró el contenedor de desafíos");
        return;
    }
    
    challengesGrid.innerHTML = ''; // Limpiar grid antes de añadir nuevos

    let actualChallenges = [];
    if (challenges && typeof challenges === 'object' && Array.isArray(challenges.content)) {
        actualChallenges = challenges.content;
        console.log("Desafíos extraídos de la propiedad 'content'. Cantidad:", actualChallenges.length);
    } else if (Array.isArray(challenges)) {
        actualChallenges = challenges;
        console.log("Desafíos recibidos como array directo. Cantidad:", actualChallenges.length);
    }

    if (actualChallenges.length === 0) {
        challengesGrid.innerHTML = '<div class="no-data" style="text-align: center; padding: 20px; color: #666;">No hay desafíos disponibles. ¡Crea el primero!</div>';
        console.log("No challenges found or data is empty after processing:", challenges);
        return;
    }

    actualChallenges.forEach((challenge, index) => {
        // Validar que 'challenge' sea un objeto y tenga al menos un ID y nombre
        if (typeof challenge !== 'object' || challenge === null || !challenge.id || !challenge.nombre) {
            console.warn("Skipping invalid challenge data:", challenge);
            return; // Saltar este desafío si los datos básicos no son válidos
        }

        // Manejar fechas de forma más segura
        const formatDate = (dateString) => {
            if (!dateString) return 'No definida';
            try {
                return new Date(dateString).toLocaleDateString('es-ES', {
                    year: 'numeric',
                    month: 'short',
                    day: 'numeric'
                });
            } catch (e) {
                console.error("Error formatting date:", dateString, e);
                return 'Fecha inválida';
            }
        };
        const startDate = formatDate(challenge.fechaInicio);
        const endDate = formatDate(challenge.fechaFin);

        // Buscar nombre del proyecto
        let proyectoNombre = 'No asociado';
        if (challenge.proyectoId) {
            // Intentar obtener el proyecto de la lista global
            const proyecto = window.projectsData ?
                window.projectsData.find(p => p.id === challenge.proyectoId) : null;

            if (proyecto && proyecto.nombre) {
                proyectoNombre = proyecto.nombre;
            } else {
                // Usar el ID como fallback
                proyectoNombre = `Proyecto (ID: ${challenge.proyectoId.substring(0, 8)}...)`;
            }
        }

        // Valores por defecto para participantes
        const participantes = challenge.usuariosAsignados ? challenge.usuariosAsignados.length : 0;
        const completados = challenge.completados || 0;
        const porcentajeCompletado = participantes > 0 ? Math.round((completados / participantes) * 100) : 0;

        // Estado del desafío
        let estado = "ACTIVO";
        const now = new Date();
        const fechaInicio = new Date(challenge.fechaInicio);
        const fechaFin = new Date(challenge.fechaFin);
        
        if (fechaInicio > now) {
            estado = "PENDIENTE";
        } else if (fechaFin < now) {
            estado = "FINALIZADO";
        }
        
        // Normalizar tipo de desafío
        let tipoDesafio = challenge.tipo || "INDIVIDUAL";
        if (typeof tipoDesafio !== 'string') {
            tipoDesafio = "INDIVIDUAL"; // Valor por defecto
        }
        
        // Traducir enum a texto amigable
        let tipoTexto = "Individual";
        switch (tipoDesafio.toUpperCase()) {
            case 'GRUPAL':
                tipoTexto = "Grupal";
                break;
            case 'COMPETITIVO':
                tipoTexto = "Competitivo";
                break;
            default:
                tipoTexto = "Individual";
        }

        const challengeCard = document.createElement('div');
        challengeCard.id = `challenge-${challenge.id}`;
        challengeCard.className = `challenge-card ${estado.toLowerCase()}`;
        
        // Añadir clase adicional basada en el tipo
        challengeCard.classList.add(`tipo-${tipoDesafio.toLowerCase()}`);

        challengeCard.innerHTML = `
            <div class="challenge-header">
                <h3 title="ID: ${challenge.id}">${challenge.nombre}</h3>
                <span class="challenge-points">${challenge.puntosRecompensa || 0} pts</span>
            </div>
            <p>${challenge.descripcion || 'Sin descripción'}</p>
            <div class="challenge-dates">
                <span class="date-item" title="Fecha de Inicio">
                    <i class="fas fa-calendar-plus"></i> ${startDate}
                </span>
                <span class="date-item" title="Fecha de Fin">
                    <i class="fas fa-calendar-check"></i> ${endDate}
                </span>
            </div>
            <div class="challenge-type">
                <i class="fas fa-users-cog"></i> Tipo: ${tipoTexto}
            </div>
            <div class="challenge-project">
                <i class="fas fa-project-diagram"></i> Proyecto: ${proyectoNombre}
            </div>
            <div class="challenge-estado">
                <i class="fas fa-info-circle"></i> Estado: ${estado}
            </div>
            <div class="challenge-progress" title="${completados} de ${participantes} participantes completaron">
                <div class="progress-bar">
                    <div class="progress" style="width: ${porcentajeCompletado}%"></div>
                </div>
                <span style="font-size: 0.8em;">${participantes} participantes | ${completados} completados</span>
            </div>
            <div class="project-actions">
                <button class="action-btn view-btn" onclick="viewChallengeDetails('${challenge.id}')" title="Ver detalles">
                    <i class="fas fa-eye"></i>
                </button>
                <button class="action-btn edit-btn" onclick="editChallenge('${challenge.id}')" title="Editar">
                    <i class="fas fa-edit"></i>
                </button>
                <button class="action-btn delete-btn" onclick="deleteChallenge('${challenge.id}')" title="Eliminar">
                    <i class="fas fa-trash"></i>
                </button>
            </div>
        `;

        challengesGrid.appendChild(challengeCard);

        // Aplicar animación de entrada con pequeño retraso escalonado
        setTimeout(() => {
            challengeCard.style.opacity = 1;
            challengeCard.style.transform = 'translateY(0)';
            challengeCard.classList.add('show');
        }, 50 * index);
    });
}

/**
 * Función para mostrar notificaciones
 */
function showNotification(message, type) {
    console.log(`🔔 Mostrando notificación [${type}]: ${message}`);
    
    // Verificar si la función ya existe en el ámbito global
    if (window.showNotification && window.showNotification !== showNotification) {
        window.showNotification(message, type);
        return;
    }
    
    // Eliminar notificaciones existentes primero
    const existingNotifications = document.querySelectorAll('.notification');
    existingNotifications.forEach(notification => {
        document.body.removeChild(notification);
    });
    
    const notification = document.createElement('div');
    notification.className = `notification ${type || 'info'}`;
    notification.style.position = 'fixed';
    notification.style.top = '20px';
    notification.style.right = '20px';
    notification.style.zIndex = '10000';
    notification.style.background = type === 'error' ? '#ff5555' : 
                                  type === 'success' ? '#55cc55' : 
                                  type === 'warning' ? '#ffaa55' : '#5599ff';
    notification.style.color = 'white';
    notification.style.padding = '15px 25px';
    notification.style.borderRadius = '5px';
    notification.style.boxShadow = '0 4px 12px rgba(0,0,0,0.1)';
    notification.style.transition = 'all 0.3s ease-in-out';
    notification.style.opacity = '0';
    notification.style.transform = 'translateY(-20px)';
    
    notification.innerHTML = `
        <span>${message}</span>
        <button style="background: none; border: none; color: white; margin-left: 10px; cursor: pointer;">
            <i class="fas fa-times"></i>
        </button>
    `;

    document.body.appendChild(notification);

    // Mostrar notificación con animación
    setTimeout(() => {
        notification.style.opacity = '1';
        notification.style.transform = 'translateY(0)';
    }, 10);

    // Ocultar después de 4 segundos
    const timeout = setTimeout(() => {
        notification.style.opacity = '0';
        notification.style.transform = 'translateY(-20px)';
        setTimeout(() => {
            if (document.body.contains(notification)) {
                document.body.removeChild(notification);
            }
        }, 300);
    }, 4000);

    // Configurar botón para cerrar
    const closeButton = notification.querySelector('button');
    if (closeButton) {
        closeButton.addEventListener('click', () => {
            clearTimeout(timeout);
            notification.style.opacity = '0';
            notification.style.transform = 'translateY(-20px)';
            setTimeout(() => {
                if (document.body.contains(notification)) {
                    document.body.removeChild(notification);
                }
            }, 300);
        });
    }
}

// Inicializar cuando el DOM esté listo
document.addEventListener('DOMContentLoaded', function() {
    console.log("🚀 DOM cargado, iniciando módulo de desafíos");
    
    // Inicializar con un pequeño retraso para asegurar que todos los elementos estén disponibles
    setTimeout(initializeChallenges, 100);
}); 