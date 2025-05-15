/**
 * Funcionalidad para gestión de desafíos
 * Este archivo maneja toda la funcionalidad relacionada con desafíos
 */

// Variables globales
// let challengesData = [];
let initializationAttempts = 0;
const MAX_INIT_ATTEMPTS = 3;

// Referencias a elementos DOM frecuentemente utilizados
const DOM = {
    // Se inicializarán en la función initDOMReferences
    form: null,
    formContainer: null,
    newChallengeButton: null,
    challengesGrid: null,
    formElements: {}
};

// Punto de entrada principal
function initializeChallenges() {
    console.log("▶️ Inicializando módulo de desafíos...");
    
    try {
        // Inicializar referencias DOM
        initDOMReferences();
        
        // Configurar botón de nuevo desafío
        setupNewChallengeButton();
        
        // Configurar formulario de desafíos usando delegación de eventos
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

/**
 * Inicializa referencias DOM para optimizar el rendimiento
 */
function initDOMReferences() {
    console.log("🔍 Inicializando referencias DOM");
    
    DOM.form = document.getElementById('newChallengeForm');
    DOM.formContainer = document.getElementById('challengeForm');
    DOM.newChallengeButton = document.querySelector('.add-challenge-btn');
    DOM.challengesGrid = document.querySelector('.challenges-grid');
    
    // Referencias a elementos del formulario para reducir lookups repetitivos
    if (DOM.form) {
        DOM.formElements = {
            name: document.getElementById('challengeName'),
            description: document.getElementById('challengeDescription'),
            startDate: document.getElementById('challengeStartDate'),
            endDate: document.getElementById('challengeEndDate'),
            points: document.getElementById('challengeRewardPoints'),
            type: document.getElementById('challengeType'),
            project: document.getElementById('challengeProject'),
            forum: document.getElementById('challengeForum'),
            conditionType: document.getElementById('challengeConditionType'),
            forumSelectGroup: document.getElementById('forumSelectGroup')
        };
    }
    
    // Verificar que se encontraron los elementos principales
    if (!DOM.form) console.warn("⚠️ No se encontró el formulario de desafíos");
    if (!DOM.formContainer) console.warn("⚠️ No se encontró el contenedor del formulario");
    if (!DOM.newChallengeButton) console.warn("⚠️ No se encontró el botón de nuevo desafío");
    if (!DOM.challengesGrid) console.warn("⚠️ No se encontró la cuadrícula de desafíos");
}

// Configura el botón de nuevo desafío
function setupNewChallengeButton() {
    if (!DOM.newChallengeButton) {
        throw new Error("No se encontró el botón de nuevo desafío (clase: add-challenge-btn)");
    }
    
    console.log("🔘 Configurando botón de nuevo desafío");
    
    // Eliminar eventos anteriores para evitar duplicados
    DOM.newChallengeButton.removeEventListener('click', openChallengeForm);
    
    // Añadir el evento actualizado
    DOM.newChallengeButton.addEventListener('click', function(event) {
        event.preventDefault();
        console.log("🖱️ Botón de nuevo desafío clickeado");
        openChallengeForm();
    });
    
    // Asegurarse de que el atributo onclick también llame a la función correcta
    DOM.newChallengeButton.setAttribute('onclick', 'openChallengeForm(); return false;');
}

// Configura el formulario de desafíos usando delegación de eventos
function setupChallengeForm() {
    if (!DOM.form) {
        throw new Error("No se encontró el formulario de desafíos (ID: newChallengeForm)");
    }
    
    console.log("📝 Configurando formulario de desafíos");
    
    // Eliminar eventos anteriores para evitar duplicados
    DOM.form.removeEventListener('submit', handleChallengeSubmit);
    
    // Añadir el evento de envío del formulario
    DOM.form.addEventListener('submit', function(event) {
        event.preventDefault();
        console.log("📨 Formulario de desafío enviado");
        handleChallengeSubmit(event);
    });
    
    // Configurar delegación de eventos para elementos del formulario
    DOM.form.addEventListener('change', handleFormElementChanges);
}

/**
 * Maneja cambios en elementos del formulario mediante delegación de eventos
 */
function handleFormElementChanges(event) {
    const target = event.target;
    
    // Manejar cambio en tipo de condición
    if (target.id === 'challengeConditionType') {
        handleConditionTypeChange(target.value);
    }
    // Manejar cambio en el proyecto seleccionado
    else if (target.id === 'challengeProject') {
        handleProjectChange(target.value);
    }
}

/**
 * Maneja los cambios en el tipo de condición
 */
function handleConditionTypeChange(value) {
    if (!DOM.formElements.forumSelectGroup) return;
    
    if (value === 'COMENTAR_FORO') {
        DOM.formElements.forumSelectGroup.style.display = 'block';
        
        // Cargar foros si hay un proyecto seleccionado
        const projectId = DOM.formElements.project.value;
        if (projectId) {
            loadForosForProject(projectId);
        }
    } else {
        DOM.formElements.forumSelectGroup.style.display = 'none';
    }
}

/**
 * Maneja los cambios en el proyecto seleccionado
 */
function handleProjectChange(projectId) {
    if (!projectId) return;
    
    const conditionType = DOM.formElements.conditionType?.value;
    if (conditionType === 'COMENTAR_FORO') {
        loadForosForProject(projectId);
    }
}

// Configura los botones de cierre
function setupCloseButtons() {
    console.log("🔴 Configurando botones de cierre");
    
    // Usar delegación de eventos para los botones de cierre
    document.addEventListener('click', function(event) {
        // Verificar si el elemento clickeado es un botón de cierre
        if (event.target.matches('button[onclick="closeForm()"]')) {
            event.preventDefault();
            console.log("🖱️ Botón de cierre clickeado");
            closeForm();
        }
    });
}

// Exporta las funciones al ámbito global
function exportGlobalFunctions() {
    console.log("🌐 Exportando funciones al ámbito global");
    
    window.openChallengeForm = openChallengeForm;
    window.closeForm = closeForm;
    window.handleChallengeSubmit = handleChallengeSubmit;
    window.loadChallenges = loadChallenges;
    
    // Definir toggleChallengeForm como una función propia compatible con el HTML
    window.toggleChallengeForm = function() {
        console.log("🔄 Llamando a toggleChallengeForm (compatibilidad)");
        if (!DOM.formContainer) {
            console.error("❌ No se encontró el contenedor del formulario (ID: challengeForm)");
            return;
        }
        
        if (DOM.formContainer.classList.contains('show')) {
            // Si está visible, lo ocultamos
            closeForm();
        } else {
            // Si está oculto, lo mostramos
            openChallengeForm();
        }
    };
    
    // Función para ver detalles de un desafío
    window.viewChallengeDetails = function(challengeId) {
        console.log("👁️ Viendo detalles del desafío:", challengeId);
        
        // Verificar si existe la variable global
        if (!window.challengesData || !Array.isArray(window.challengesData)) {
            console.error("❌ No se encontró la variable global challengesData o no es un array");
            alert("No se pudieron cargar los datos de los desafíos");
            return;
        }
        
        // Buscar el desafío en la lista global
        const challenge = window.challengesData.find(c => c.id === challengeId);
        if (!challenge) {
            console.error(`❌ No se encontró el desafío con ID: ${challengeId}`);
            alert('Desafío no encontrado');
            return;
        }
        
        // Por ahora, mostrar un alert con la información básica
        // En una versión futura, se podría implementar un modal similar al de proyectos
        alert(`
            Título: ${challenge.nombre || challenge.titulo || 'Sin título'}
            Descripción: ${challenge.descripcion || 'Sin descripción'}
            Puntos: ${challenge.puntosRecompensa || challenge.puntos || 0}
            Tipo: ${challenge.tipo || 'INDIVIDUAL'}
            Fechas: ${new Date(challenge.fechaInicio).toLocaleDateString()} - ${new Date(challenge.fechaFin).toLocaleDateString()}
        `);
    };
    
    // También exportamos otras funciones necesarias
    window.editChallenge = editChallenge;
    window.deleteChallenge = deleteChallenge;
    window.searchChallenges = searchChallenges;
}

/**
 * Abre el formulario de creación de desafíos
 */
function openChallengeForm() {
    console.log("🔓 Abriendo formulario de desafíos...");
    
    // Verificar si existe el contenedor actualizado
    if (!DOM.formContainer) {
        console.error("❌ No se encontró el contenedor del formulario de desafíos (ID: challengeForm)");
        alert("Error: No se pudo abrir el formulario de desafíos");
        return;
    }
    
    // Mostrar el formulario
    DOM.formContainer.classList.add('show');
    DOM.formContainer.style.display = 'flex';
    
    // Cargar proyectos en el formulario
    loadProjectsForChallengeForm();
    
    // Configurar fecha mínima como hoy
    const today = new Date().toISOString().split('T')[0];
    if (DOM.formElements.startDate) DOM.formElements.startDate.min = today;
    if (DOM.formElements.endDate) DOM.formElements.endDate.min = today;
    
    // Limpiar formulario
    if (DOM.form) DOM.form.reset();
    
    // Configurar selector de condición de completitud
    const conditionTypeSelect = DOM.formElements.conditionType;
    if (conditionTypeSelect) {
        // Limpiar opciones anteriores
        conditionTypeSelect.innerHTML = '';
        
        // Añadir opciones de tipo de condición
        const options = [
            { value: 'PARTICIPAR_PROYECTO', text: 'Participar en el proyecto' },
            { value: 'COMENTAR_FORO', text: 'Comentar en el foro' },
            { value: 'ACCION_GENERICA', text: 'Acción genérica (manual)' }
        ];
        
        options.forEach(option => {
            const optElement = document.createElement('option');
            optElement.value = option.value;
            optElement.textContent = option.text;
            conditionTypeSelect.appendChild(optElement);
        });
        
        // Trigger el cambio para inicializar correctamente
        conditionTypeSelect.dispatchEvent(new Event('change'));
    }
    
    // Mostrar notificación amigable
    showNotification("Complete el formulario para crear un desafío", "info");
}

/**
 * Cierra el formulario de desafíos
 */
function closeForm() {
    console.log("🔒 Cerrando formulario de desafíos");
    
    if (DOM.formContainer) {
        DOM.formContainer.classList.remove('show');
        DOM.formContainer.style.display = 'none';
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
 * Carga los foros para un proyecto específico
 * @param {string} projectId - ID del proyecto
 * @param {string} targetSelector - ID del elemento select donde se cargarán los foros (opcional)
 */
async function loadForosForProject(projectId, targetSelector = 'criterioForoSelect') {
    console.log("🔄 Cargando foros para el proyecto:", projectId);
    
    if (!projectId) {
        console.error("❌ No se proporcionó un ID de proyecto válido");
        return;
    }
    
    // Mostrar el grupo del select de foros si existe
    const forumSelectGroup = document.getElementById('forumSelectGroup');
    if (forumSelectGroup) {
        forumSelectGroup.style.display = 'block';
    }
    
    // Obtener el elemento select
    const foroSelect = document.getElementById(targetSelector);
    if (!foroSelect) {
        console.error(`❌ No se encontró el elemento select con ID: ${targetSelector}`);
        return;
    }
    
    try {
        // Limpiar opciones actuales
        foroSelect.innerHTML = '<option value="" disabled selected>Cargando foros...</option>';
        
        // Realizar la petición a la API
        const response = await fetch(`/api/foros/proyecto/${projectId}`);
        
        if (!response.ok) {
            throw new Error(`Error HTTP: ${response.status}`);
        }
        
        const foros = await response.json();
        
        if (foros.length === 0) {
            foroSelect.innerHTML = '<option value="" disabled selected>No hay foros disponibles</option>';
            return;
        }
        
        // Agregar opciones al select
        foroSelect.innerHTML = '<option value="" disabled selected>Selecciona un foro</option>';
        foros.forEach(foro => {
            const option = document.createElement('option');
            option.value = foro.id;
            option.textContent = foro.titulo || `Foro #${foro.id}`;
            foroSelect.appendChild(option);
        });
        
    } catch (error) {
        console.error("❌ Error al cargar los foros:", error);
        foroSelect.innerHTML = '<option value="" disabled selected>Error al cargar foros</option>';
    }
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

    // Obtener el proyecto y foro seleccionados
    const proyectoId = document.getElementById('challengeProject').value;
    const foroId = document.getElementById('challengeForum').value || null;

    // Obtener el tipo de condición de completitud seleccionado
    const conditionTypeSelect = document.getElementById('challengeConditionType');
    let tipoCondicionCompletitud = 'ACCION_GENERICA'; // Valor por defecto
    
    if (conditionTypeSelect && conditionTypeSelect.value) {
        tipoCondicionCompletitud = conditionTypeSelect.value;
    }

    // Determinar el objetivoId según el tipo de condición
    let objetivoId = null;
    if (tipoCondicionCompletitud === 'COMENTAR_FORO' && foroId) {
        objetivoId = foroId;
    } else if (tipoCondicionCompletitud === 'PARTICIPAR_PROYECTO') {
        objetivoId = proyectoId;
    }

    const challengeData = {
        nombre: document.getElementById('challengeName').value,
        descripcion: document.getElementById('challengeDescription').value,
        fechaInicio: fechaInicio,
        fechaFin: fechaFin,
        puntosRecompensa: parseInt(document.getElementById('challengeRewardPoints').value) || 0,
        proyectoId: proyectoId,
        tipo: tipo, // Usar el tipo seleccionado o el valor predeterminado
        tipoCondicionCompletitud: tipoCondicionCompletitud,
        objetivoId: objetivoId,
        criterios: [] // Array vacío por defecto
    };

    // Validaciones básicas antes de enviar
    if (!challengeData.nombre) {
        showNotification('Error: El título del desafío es obligatorio.', 'error');
        submitBtn.disabled = false;
        submitBtn.innerHTML = originalBtnText;
        return;
    }
    
    if (!challengeData.proyectoId) {
        showNotification('Error: Debe seleccionar un proyecto para el desafío.', 'error');
        submitBtn.disabled = false;
        submitBtn.innerHTML = originalBtnText;
        return;
    }
    
    if (!challengeData.fechaInicio || !challengeData.fechaFin) {
        showNotification('Error: Las fechas de inicio y fin son obligatorias.', 'error');
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

    // Validar que si la condición es COMENTAR_FORO, se haya seleccionado un foro
    if (tipoCondicionCompletitud === 'COMENTAR_FORO' && !foroId) {
        showNotification('Para la condición "Comentar en el foro" debes seleccionar un foro.', 'error');
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
                            } else if (response.status === 400) {
                                throw new Error(`Error de validación: Verifique los datos del desafío (${response.status}).`);
                            } else if (response.status === 403) {
                                throw new Error(`Error de permisos: No tiene autorización para crear desafíos (${response.status}).`);
                            } else if (response.status === 500) {
                                throw new Error(`Error interno del servidor: Contacte al administrador (${response.status}).`);
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
 * @param {number} attemptCount - Contador de intentos (para reintentos)
 */
function loadChallenges(attemptCount = 0) {
    const MAX_LOAD_ATTEMPTS = 3;
    const challengesGrid = document.querySelector('.challenges-grid');
    if (!challengesGrid) {
        console.error("No se encontró el contenedor de desafíos");
        return;
    }
    
    challengesGrid.innerHTML = '<div class="no-data" style="text-align: center; padding: 20px; color: #666;">Cargando desafíos...</div>';

    console.log(`Cargando desafíos... (Intento ${attemptCount + 1}/${MAX_LOAD_ATTEMPTS})`);
    
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
                    
                    // Detallar mejor los códigos de error HTTP
                    if (response.status === 400) {
                        throw new Error(`Error de solicitud incorrecta (${response.status}). Verifique los parámetros.`);
                    } else if (response.status === 401) {
                        throw new Error(`Error de autenticación (${response.status}). Su sesión ha expirado.`);
                    } else if (response.status === 403) {
                        throw new Error(`Error de permisos (${response.status}). No tiene acceso a esta funcionalidad.`);
                    } else if (response.status === 404) {
                        throw new Error(`Recurso no encontrado (${response.status}). La API solicitada no existe.`);
                    } else if (response.status >= 500) {
                        throw new Error(`Error del servidor (${response.status}). Por favor intente más tarde.`);
                    } else {
                        throw new Error(`Error ${response.status}: ${response.statusText}`);
                    }
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
            
            // Sistema de reintentos mejorado
            if (attemptCount < MAX_LOAD_ATTEMPTS - 1) {
                const nextAttempt = attemptCount + 1;
                const retryDelay = Math.pow(2, nextAttempt) * 500; // Backoff exponencial: 1s, 2s, 4s...
                
                console.log(`Reintentando cargar desafíos en ${retryDelay/1000} segundos... (Intento ${nextAttempt + 1}/${MAX_LOAD_ATTEMPTS})`);
                
                challengesGrid.innerHTML = `
                    <div style="text-align: center; padding: 20px;">
                        <div style="color: #d32f2f; margin-bottom: 15px;">
                            <i class="fas fa-sync fa-spin"></i>
                            <p style="margin-top: 10px;">Error al cargar desafíos. Reintentando automáticamente...</p>
                            <p style="font-size: 0.9em; color: #666;">Intento ${nextAttempt + 1}/${MAX_LOAD_ATTEMPTS}</p>
                            <p style="font-size: 0.8em; color: #888;">${errorMessage}</p>
                        </div>
                    </div>`;
                
                setTimeout(() => loadChallenges(nextAttempt), retryDelay);
            } else {
                // Mostrar mensaje de error final después de agotar intentos
                challengesGrid.innerHTML = `
                    <div style="text-align: center; padding: 20px;">
                        <div style="color: #d32f2f; margin-bottom: 15px;">
                            <i class="fas fa-exclamation-circle" style="font-size: 24px;"></i>
                            <p style="margin-top: 10px;">${errorMessage}</p>
                            <p style="font-size: 0.9em; color: #666;">Se agotaron los intentos automáticos.</p>
                        </div>
                        <button onclick="loadChallenges(0)" style="
                            margin-top: 10px;
                            padding: 8px 16px;
                            background: var(--primary-color, #4caf50);
                            color: white;
                            border: none;
                            border-radius: 4px;
                            cursor: pointer;
                        "><i class="fas fa-sync-alt"></i> Reintentar manualmente</button>
                    </div>`;
            }
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
    
    // Limpiar grid antes de mostrar los resultados filtrados
    challengesGrid.innerHTML = '';
    
    // Verificar si hay datos válidos
    let challengesArray = [];
    
    if (challenges && Array.isArray(challenges)) {
        challengesArray = challenges;
    } else if (challenges && typeof challenges === 'object' && Array.isArray(challenges.content)) {
        challengesArray = challenges.content;
    }
    
    if (challengesArray.length === 0) {
        challengesGrid.innerHTML = '<p>No se encontraron desafíos que coincidan con la búsqueda.</p>';
        return;
    }
    
    // Guardar referencia global a los datos de desafíos para que otras funciones puedan acceder
    window.challengesData = challengesArray;

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
        
        // Determinar texto de condición de completitud
        let condicionTexto = "Completar manualmente";
        if (challenge.tipoCondicionCompletitud === 'PARTICIPAR_PROYECTO') {
            condicionTexto = "Participar en el proyecto";
        } else if (challenge.tipoCondicionCompletitud === 'COMENTAR_FORO') {
            condicionTexto = "Comentar en el foro";
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
            <div class="challenge-condition">
                <i class="fas fa-check-circle"></i> Condición: ${condicionTexto}
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
 * Muestra una notificación personalizada al usuario
 * @param {string} message - Mensaje a mostrar
 * @param {string} type - Tipo de notificación (success, error, info, warning)
 * @param {number} duration - Duración en milisegundos (opcional, por defecto 3000)
 */
function showNotification(message, type = 'info', duration = 3000) {
    console.log(`🔔 Mostrando notificación [${type}]: ${message}`);
    
    // Si ya existe una función global de notificaciones, la usamos
    if (typeof window.showNotification === 'function' && window.showNotification !== showNotification) {
        window.showNotification(message, type, duration);
        return;
    }
    
    // Crear contenedor principal si no existe
    let notificationContainer = document.getElementById('notificationContainer');
    if (!notificationContainer) {
        notificationContainer = document.createElement('div');
        notificationContainer.id = 'notificationContainer';
        notificationContainer.style.position = 'fixed';
        notificationContainer.style.top = '20px';
        notificationContainer.style.right = '20px';
        notificationContainer.style.zIndex = '9999';
        document.body.appendChild(notificationContainer);
    }
    
    // Crear notificación
    const notification = document.createElement('div');
    notification.classList.add('notification', type);
    notification.style.backgroundColor = getBackgroundColor(type);
    notification.style.color = '#fff';
    notification.style.borderRadius = '5px';
    notification.style.padding = '10px 20px';
    notification.style.marginBottom = '10px';
    notification.style.boxShadow = '0 2px 5px rgba(0,0,0,0.2)';
    notification.style.transition = 'all 0.3s ease-in-out';
    notification.style.cursor = 'pointer';
    
    // Agregar ícono según tipo
    const icon = document.createElement('i');
    icon.className = getIconClass(type);
    icon.style.marginRight = '10px';
    notification.appendChild(icon);
    
    // Agregar mensaje
    const messageText = document.createTextNode(message);
    notification.appendChild(messageText);
    
    // Agregar notificación al contenedor
    notificationContainer.appendChild(notification);
    
    // Aplicar animación de entrada
    setTimeout(() => {
        notification.style.opacity = '1';
    }, 10);
    
    // Auto-cerrar después del tiempo especificado
    setTimeout(() => {
        notification.style.opacity = '0';
        notification.style.transform = 'translateX(100%)';
        setTimeout(() => {
            notificationContainer.removeChild(notification);
        }, 300);
    }, duration);
    
    // Permitir cerrar al hacer clic
    notification.addEventListener('click', () => {
        notification.style.opacity = '0';
        notification.style.transform = 'translateX(100%)';
        setTimeout(() => {
            try {
                notificationContainer.removeChild(notification);
            } catch (e) {
                // La notificación ya podría haber sido eliminada
            }
        }, 300);
    });
    
    // Funciones auxiliares
    function getBackgroundColor(type) {
        switch (type) {
            case 'success': return '#28a745';
            case 'error': return '#dc3545';
            case 'warning': return '#ffc107';
            case 'info':
            default: return '#17a2b8';
        }
    }
    
    function getIconClass(type) {
        switch (type) {
            case 'success': return 'fas fa-check-circle';
            case 'error': return 'fas fa-exclamation-circle';
            case 'warning': return 'fas fa-exclamation-triangle';
            case 'info':
            default: return 'fas fa-info-circle';
        }
    }
}

/**
 * Función para editar un desafío
 */
function editChallenge(challengeId) {
    console.log("✏️ Editando desafío:", challengeId);
    
    // Verificar si existe la variable global
    if (!window.challengesData || !Array.isArray(window.challengesData)) {
        console.error("❌ No se encontró la variable global challengesData o no es un array");
        alert("No se pudieron cargar los datos de los desafíos");
        return;
    }
    
    // Buscar el desafío en la lista
    const challenge = window.challengesData.find(c => c.id === challengeId);
    if (!challenge) {
        console.error(`❌ No se encontró el desafío con ID: ${challengeId}`);
        alert('Desafío no encontrado');
        return;
    }
    
    // Abrir el formulario
    openChallengeForm();
    
    // Llenar el formulario con los datos del desafío
    document.getElementById('challengeName').value = challenge.nombre || '';
    document.getElementById('challengeDescription').value = challenge.descripcion || '';
    document.getElementById('challengeRewardPoints').value = challenge.puntosRecompensa || 0;
    
    // Asegurar que tenemos un tipo válido
    let tipoDesafio = challenge.tipo || 'INDIVIDUAL';
    // Sanitizar el tipo para que coincida con las opciones disponibles
    if (!['INDIVIDUAL', 'GRUPAL', 'COMPETITIVO'].includes(tipoDesafio.toUpperCase())) {
        tipoDesafio = 'INDIVIDUAL';
    }
    document.getElementById('challengeType').value = tipoDesafio;
    
    // Establecer fechas si existen
    if (challenge.fechaInicio) {
        // Formatear la fecha para el input datetime-local
        const startDate = new Date(challenge.fechaInicio);
        const formattedStartDate = startDate.toISOString().slice(0, 16);
        document.getElementById('challengeStartDate').value = formattedStartDate;
    }
    
    if (challenge.fechaFin) {
        // Formatear la fecha para el input datetime-local
        const endDate = new Date(challenge.fechaFin);
        const formattedEndDate = endDate.toISOString().slice(0, 16);
        document.getElementById('challengeEndDate').value = formattedEndDate;
    }
    
    // Seleccionar proyecto asociado si existe
    if (challenge.proyectoId) {
        document.getElementById('challengeProject').value = challenge.proyectoId;
        // Cargar foros del proyecto
        loadForosForProject(challenge.proyectoId);
    }
    
    // Guardar ID del desafío en edición
    document.getElementById('newChallengeForm').dataset.editingChallengeId = challengeId;
}

/**
 * Confirma la eliminación de un desafío
 */
function confirmDeleteChallenge(challengeId) {
    if (confirm('¿Estás seguro de que deseas eliminar este desafío? Esta acción no se puede deshacer.')) {
        deleteChallenge(challengeId);
    }
}

/**
 * Elimina un desafío
 */
async function deleteChallenge(challengeId) {
    console.log("🗑️ Eliminando desafío:", challengeId);
    try {
        const response = await fetch(`/api/desafios/${challengeId}`, {
            method: 'DELETE'
        });
        
        if (!response.ok) {
            throw new Error(`Error HTTP: ${response.status}`);
        }
        
        // Eliminar el desafío de la lista
        if (window.challengesData) {
            window.challengesData = window.challengesData.filter(challenge => challenge.id !== challengeId);
        }
        
        // Actualizar la vista
        displayChallenges(window.challengesData);
        
        // Mostrar notificación
        showNotification('Desafío eliminado correctamente', 'success');
        
    } catch (error) {
        console.error('Error al eliminar desafío:', error);
        showNotification(`Error al eliminar el desafío: ${error.message}`, 'error');
    }
}

// Inicializar cuando el DOM esté listo
document.addEventListener('DOMContentLoaded', function() {
    console.log("🚀 DOM cargado, iniciando módulo de desafíos");
    
    // Inicializar con un pequeño retraso para asegurar que todos los elementos estén disponibles
    setTimeout(initializeChallenges, 100);
}); 