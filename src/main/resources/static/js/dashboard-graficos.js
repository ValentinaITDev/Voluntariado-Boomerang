/**
 * Dashboard Gráficos - Panel Administrativo
 * Versión simplificada con funciones básicas de JS
 */

// Indicador de si las gráficas se han inicializado
let chartsInitialized = false;

// Almacena las instancias de gráficos
let chartInstances = {};

// Datos de respaldo
const fallbackData = {
    totalUsuarios: 5, totalVoluntarios: 4, totalAdministradores: 1,
    proyectosActivos: 4, desafiosCompletados: 8, empresasParticipantes: 3,
    tendenciaUsuarios: 5.2, tendenciaProyectos: 2.8,
    completados: 8, enProgreso: 5, sinComenzar: 3,
    proyectosPorMes: {"2024-01": 2, "2024-02": 3, "2024-03": 4, "2024-04": 2, "2024-05": 1, "2024-06": 4},
    actividad: [
        {fecha: "Lun", actividades: 2}, {fecha: "Mar", actividades: 4}, {fecha: "Mie", actividades: 3},
        {fecha: "Jue", actividades: 5}, {fecha: "Vie", actividades: 7}, {fecha: "Sab", actividades: 2}, {fecha: "Dom", actividades: 1}
    ],
    forosActivos: [
        {nombre: "Desarrollo Web", comentarios: 32}, {nombre: "Sustentabilidad", comentarios: 24}, {nombre: "Innovación", comentarios: 21},
        {nombre: "App Móvil", comentarios: 18}, {nombre: "Comunidad", comentarios: 15}
    ],
    desafiosCategorias: {"Programación": 42, "Diseño": 28, "Gestión": 18, "Sostenibilidad": 22, "Marketing": 14}
};

const empresasFallbackData = {
    empresasRanking: [
        { nombre: "Anaya", participaciones: 5, puntos: 95, usuarios: 3 }, { nombre: "Telefónica", participaciones: 4, puntos: 80, usuarios: 4 },
        { nombre: "BBVA", participaciones: 3, puntos: 65, usuarios: 2 }, { nombre: "Santander", participaciones: 2, puntos: 50, usuarios: 3 },
        { nombre: "Iberdrola", participaciones: 1, puntos: 25, usuarios: 1 }
    ],
    participacionMensual: {
        "2023-11": { "Anaya": 0, "Telefónica": 0, "BBVA": 0 }, "2023-12": { "Anaya": 0, "Telefónica": 0, "BBVA": 0 },
        "2024-01": { "Anaya": 0, "Telefónica": 0, "BBVA": 0 }, "2024-02": { "Anaya": 0, "Telefónica": 0, "BBVA": 0 },
        "2024-03": { "Anaya": 0, "Telefónica": 0, "BBVA": 0 }, "2024-04": { "Anaya": 2, "Telefónica": 1, "BBVA": 1 }
    }
};

let lastRealEmpresasData = null;
let cachedEmpresasRanking = null;

document.addEventListener('DOMContentLoaded', function() {
    setupRefreshButton();
    setTimeout(loadDashboardData, 300);
});

function setupRefreshButton() {
    console.log("Configurando botón de actualización");
    const refreshBtn = document.getElementById('refreshButton');
    if (refreshBtn) {
        console.log("Botón de actualización encontrado, añadiendo event listener");
        refreshBtn.addEventListener('click', function() {
            console.log("Botón de actualización pulsado");
            const icon = this.querySelector('i');
            if (icon) {
                icon.classList.add('fa-spin');
                setTimeout(function() {
                    icon.classList.remove('fa-spin');
                }, 2000);
            }
            showStatusMessage('Actualizando datos...', 'info');
            loadDashboardData()
                .then(function(data) {
                    console.log("Datos cargados exitosamente:", data);
                })
                .catch(function(error) {
                    console.error("Error al cargar datos:", error);
                })
                .finally(function() {
                    if (icon) icon.classList.remove('fa-spin');
                });
        });
        console.log("Event listener añadido al botón de actualización");
    } else {
        console.warn("Botón de actualización no encontrado en el DOM");
    }
}

// Carga los datos del dashboard usando Promesas
function loadDashboardData() {
    return new Promise(function(resolve, reject) {
        showStatusMessage('Cargando datos...', 'info');
        console.log("Iniciando carga de datos del dashboard");

        const controller = new AbortController();
        const timeoutId = setTimeout(function() { 
            controller.abort(); 
            reject(new Error('Timeout al cargar datos')); // Rechazar promesa en timeout
        }, 10000);

        let url = '/api/dashboard/avanzado/datos';

        fetch(url, { signal: controller.signal, headers: {'Cache-Control': 'no-cache', 'Pragma': 'no-cache'} })
            .then(function(response) {
                if (!response.ok) {
                    console.warn(`Endpoint avanzado no disponible (${response.status}), usando endpoint estándar`);
                    url = '/api/dashboard/resumen'; // Cambiar a URL estándar
                    // Encadenar la nueva llamada fetch
                    return fetch(url, { signal: controller.signal, headers: {'Cache-Control': 'no-cache', 'Pragma': 'no-cache'} });
                }
                return response; // Devolver la respuesta original si fue ok
            })
            .then(function(response) {
                // Esta función ahora recibe la respuesta de CUALQUIERA de los dos fetches
                if (!response.ok) {
                    // Si llegamos aquí, ambos fetches fallaron
                    throw new Error(`Error al cargar datos (${response.status}): ${response.statusText}`);
                }
                return response.json(); // Procesar el JSON de la respuesta exitosa
            })
            .then(function(data) {
                clearTimeout(timeoutId); // Limpiar timeout si la carga fue exitosa
                console.log("Datos recibidos del servidor:", data);

                if (!data) {
                    console.error("Datos recibidos son nulos o indefinidos");
                    data = { ...fallbackData };
                    showStatusMessage("Error: datos recibidos inválidos", "error", 5000);
                }

                // Cacheo y normalización de datos (sin cambios)
                if (data.empresasRanking && Array.isArray(data.empresasRanking)) {
                    console.log("Cacheando datos de empresasRanking:", data.empresasRanking.length, "empresas");
                    cachedEmpresasRanking = data.empresasRanking;
                    data.empresasParticipantes = data.empresasRanking.length;
                    console.log("Actualizado empresasParticipantes con datos reales:", data.empresasParticipantes);
                } else if (cachedEmpresasRanking) {
                    console.log("Usando datos cacheados de empresasRanking");
                    data.empresasRanking = cachedEmpresasRanking;
                    data.empresasParticipantes = cachedEmpresasRanking.length;
                }
                const normalizedData = {
                    totalUsuarios: parseInt(data.totalUsuarios || 0),
                    totalVoluntarios: parseInt(data.totalVoluntarios || 0),
                    totalAdministradores: parseInt(data.totalAdministradores || 0),
                    proyectosActivos: parseInt(data.proyectosActivos || 0),
                    desafiosCompletados: parseInt(data.desafiosCompletados || 0),
                    empresasParticipantes: parseInt(data.empresasParticipantes || 0),
                    tendenciaUsuarios: parseFloat(data.tendenciaUsuarios || 0),
                    tendenciaProyectos: parseFloat(data.tendenciaProyectos || 0),
                    tendenciaDesafios: parseFloat(data.tendenciaDesafios || 0),
                    tendenciaEmpresas: parseFloat(data.tendenciaEmpresas || 0),
                    completados: parseInt(data.completados || 0),
                    enProgreso: parseInt(data.enProgreso || 0),
                    sinComenzar: parseInt(data.sinComenzar || 0),
                    proyectosPorEstado: data.proyectosPorEstado || { ACTIVO: 0, COMPLETADO: 0, EXPIRADO: 0, CANCELADO: 0 },
                    proyectosPorMes: data.proyectosPorMes || fallbackData.proyectosPorMes,
                    actividad: data.actividad || fallbackData.actividad,
                    forosActivos: data.forosActivos || fallbackData.forosActivos,
                    desafiosCategorias: data.desafiosCategorias || fallbackData.desafiosCategorias,
                    empresasRanking: data.empresasRanking || [],
                    participacionMensual: data.participacionMensual || {}
                };
                console.log("Datos normalizados:", normalizedData);
                if (data.error) {
                    console.warn("Error reportado por el servidor:", data.error);
                    showStatusMessage(`Advertencia: ${data.error}`, "warning", 5000);
                }
                actualizarMetricasPrincipales(normalizedData);
                if (!chartsInitialized) {
                    console.log("Inicializando gráficas por primera vez");
                    initializeCharts(normalizedData);
                    chartsInitialized = true;
                } else {
                    console.log("Actualizando gráficas existentes");
                    updateCharts(normalizedData);
                }
                clearStatusMessages();
                showStatusMessage('Datos actualizados correctamente', 'success', 3000);
                resolve(normalizedData); // Resolver la promesa con los datos
            })
            .catch(function(error) {
                clearTimeout(timeoutId); // Limpiar timeout también en caso de error
                console.error('Error en la cadena de promesas de carga:', error);
                showStatusMessage(`Error al cargar datos: ${error.message}. Usando datos de respaldo.`, 'error', 5000);

                const datosRespaldoConValores = { // Usar fallback
                    ...fallbackData,
                    totalUsuarios: 10, proyectosActivos: 5, desafiosCompletados: 15, empresasParticipantes: 3,
                    tendenciaUsuarios: 5.2, tendenciaProyectos: 2.8
                };
                console.log("Usando datos de respaldo:", datosRespaldoConValores);
                actualizarMetricasPrincipales(datosRespaldoConValores);
                if (!chartsInitialized) {
                    initializeCharts(datosRespaldoConValores);
                    chartsInitialized = true;
                } else {
                    updateCharts(datosRespaldoConValores);
                }
                reject(error); // Rechazar la promesa con el error
            });
    });
}

/**
 * Actualiza los elementos de la interfaz con los datos recibidos
 */
function updateDashboardData(data) {
    try {
        console.log("Actualizando datos del dashboard:", data);
        
        // Convertir valores a números para evitar problemas
        const totalUsuarios = parseInt(data.totalUsuarios || 0);
        const proyectosActivos = parseInt(data.proyectosActivos || 0);
        const desafiosCompletados = parseInt(data.desafiosCompletados || 0);
        const empresasParticipantes = parseInt(data.empresasParticipantes || 0);
        
        console.log("Valores procesados:", {
            totalUsuarios, 
            proyectosActivos, 
            desafiosCompletados, 
            empresasParticipantes
        });
        
        // Actualizar directamente los elementos del DOM usando getElementById
        const elemTotalUsuarios = document.getElementById('totalUsuarios');
        const elemProyectosActivos = document.getElementById('proyectosActivos');
        const elemDesafiosCompletados = document.getElementById('desafiosCompletados');
        const elemEmpresasParticipantes = document.getElementById('empresasParticipantes');
        
        // Verificar que los elementos existen antes de actualizar
        if (elemTotalUsuarios) {
            elemTotalUsuarios.textContent = totalUsuarios;
            console.log("Actualizado totalUsuarios:", totalUsuarios);
        } else {
            console.warn("Elemento totalUsuarios no encontrado");
        }
        
        if (elemProyectosActivos) {
            elemProyectosActivos.textContent = proyectosActivos;
            console.log("Actualizado proyectosActivos:", proyectosActivos);
        } else {
            console.warn("Elemento proyectosActivos no encontrado");
        }
        
        if (elemDesafiosCompletados) {
            elemDesafiosCompletados.textContent = desafiosCompletados;
            console.log("Actualizado desafiosCompletados:", desafiosCompletados);
        } else {
            console.warn("Elemento desafiosCompletados no encontrado");
        }
        
        if (elemEmpresasParticipantes) {
            elemEmpresasParticipantes.textContent = empresasParticipantes;
            console.log("Actualizado empresasParticipantes:", empresasParticipantes);
        } else {
            console.warn("Elemento empresasParticipantes no encontrado");
        }
        
        // Formatear tendencias evitando NaN
        const tendenciaUsuarios = isNaN(parseFloat(data.tendenciaUsuarios)) ? 0 : parseFloat(data.tendenciaUsuarios);
        const tendenciaProyectos = isNaN(parseFloat(data.tendenciaProyectos)) ? 0 : parseFloat(data.tendenciaProyectos);
        const tendenciaDesafios = isNaN(parseFloat(data.tendenciaDesafios)) ? 0 : parseFloat(data.tendenciaDesafios);
        const tendenciaEmpresas = isNaN(parseFloat(data.tendenciaEmpresas)) ? 0 : parseFloat(data.tendenciaEmpresas);
        
        console.log("Tendencias procesadas:", {
            tendenciaUsuarios,
            tendenciaProyectos,
            tendenciaDesafios,
            tendenciaEmpresas
        });
        
        // Actualizar tendencias usando getElementById directo
        actualizarElementoTendencia('usuariosTendencia', tendenciaUsuarios);
        actualizarElementoTendencia('proyectosTendencia', tendenciaProyectos);
        actualizarElementoTendencia('desafiosTendencia', tendenciaDesafios);
        actualizarElementoTendencia('empresasTendencia', tendenciaEmpresas);
        
    } catch (error) {
        console.error("Error al actualizar datos del dashboard:", error);
        showStatusMessage("Error al actualizar estadísticas básicas", "error", 3000);
    }
}

/**
 * Actualiza un elemento de tendencia según el valor, cambiando su clase y contenido.
 */
function actualizarElementoTendencia(id, valor) {
    try {
        console.log(`Actualizando tendencia ${id} con valor ${valor}`);
        const elementoTendencia = document.getElementById(id);
        if (!elementoTendencia) {
            console.warn(`Elemento de tendencia ${id} no encontrado`);
            return;
        }

        // Formatear el valor como porcentaje con signo
        const valorFormateado = valor > 0 ? `+${valor.toFixed(1)}%` : `${valor.toFixed(1)}%`;
        console.log(`Valor formateado para ${id}: ${valorFormateado}`);
        
        // Limpiar clases anteriores
        elementoTendencia.classList.remove('positivo', 'negativo', 'neutro');
        
        // Agregar clase según valor
        if (valor > 0) {
            elementoTendencia.classList.add('positivo');
            elementoTendencia.innerHTML = `<i class="fas fa-arrow-up"></i> ${valorFormateado}`;
        } else if (valor < 0) {
            elementoTendencia.classList.add('negativo');
            elementoTendencia.innerHTML = `<i class="fas fa-arrow-down"></i> ${valorFormateado}`;
        } else {
            elementoTendencia.classList.add('neutro');
            elementoTendencia.innerHTML = `<i class="fas fa-equals"></i> ${valorFormateado}`;
        }
        
        console.log(`Tendencia ${id} actualizada con éxito: ${elementoTendencia.innerHTML}`);
    } catch (error) {
        console.error(`Error al actualizar tendencia ${id}:`, error);
    }
}

/**
 * Formatea un valor como porcentaje con signo
 */
function formatearPorcentaje(valor) {
    // Validar que el valor sea un número
    if (valor === undefined || valor === null || isNaN(parseFloat(valor))) {
        return '0%';
    }
    
    // Convertir a número si es string
    if (typeof valor === 'string') {
        valor = parseFloat(valor);
    }
    
    // Redondear a 1 decimal y convertir a string
    const valorRedondeado = parseFloat(valor.toFixed(1));
    
    // Formatear con signo +/- y añadir símbolo %
    if (valorRedondeado > 0) {
        return `+${valorRedondeado}%`;
    } else if (valorRedondeado < 0) {
        return `${valorRedondeado}%`; // El signo negativo ya está incluido
    } else {
        return `0%`;
    }
}

/**
 * Inicializa todas las gráficas
 */
function initializeCharts(data) {
    console.log("Inicializando gráficas con datos:", data);
    
    // Verificar elementos del DOM primero
    verificarElementosDOM();
    
    // Destruir gráficas existentes si hay alguna
    Object.values(chartInstances).forEach(chart => {
        if (chart && typeof chart.destroy === 'function') {
            chart.destroy();
        }
    });
    
    // Inicializar gráficas con los datos
    const usuariosChartElement = document.getElementById('usuariosChart');
    if (usuariosChartElement) {
        try {
            chartInstances.usuariosChart = createUsuariosChart(data);
            console.log("Gráfica de usuarios creada con éxito");
        } catch (error) {
            console.error("Error al crear gráfica de usuarios:", error);
            mostrarErrorEnGrafica(usuariosChartElement, "Error al cargar la gráfica de usuarios");
        }
    } else {
        console.warn("Elemento usuariosChart no encontrado");
    }
    
    const proyectosPorMesChartElement = document.getElementById('proyectosPorMesChart');
    if (proyectosPorMesChartElement) {
        try {
            chartInstances.proyectosPorMesChart = createProyectosPorMesChart(data);
            console.log("Gráfica de proyectos por mes creada con éxito");
        } catch (error) {
            console.error("Error al crear gráfica de proyectos por mes:", error);
            mostrarErrorEnGrafica(proyectosPorMesChartElement, "Error al cargar la gráfica de proyectos");
        }
    } else {
        console.warn("Elemento proyectosPorMesChart no encontrado");
    }
    
    const desafiosChartElement = document.getElementById('desafiosChart');
    if (desafiosChartElement) {
        try {
            chartInstances.desafiosChart = createDesafiosChart(data);
            console.log("Gráfica de desafíos creada con éxito");
        } catch (error) {
            console.error("Error al crear gráfica de desafíos:", error);
            mostrarErrorEnGrafica(desafiosChartElement, "Error al cargar la gráfica de desafíos");
        }
    } else {
        console.warn("Elemento desafiosChart no encontrado");
    }
    
    const actividadChartElement = document.getElementById('actividadChart');
    if (actividadChartElement) {
        try {
            chartInstances.actividadChart = createActividadChart(data);
            console.log("Gráfica de actividad creada con éxito");
        } catch (error) {
            console.error("Error al crear gráfica de actividad:", error);
            mostrarErrorEnGrafica(actividadChartElement, "Error al cargar la gráfica de actividad");
        }
    } else {
        console.warn("Elemento actividadChart no encontrado");
    }
    
    // Actualizar directamente las métricas principales también
    actualizarMetricasPrincipales(data);
    
    // Inicializar las nuevas gráficas de empresas
    initializeEmpresasCharts(data);
}

/**
 * Actualiza las gráficas existentes con nuevos datos
 */
function updateCharts(data) {
    console.log("Actualizando gráficas con nuevos datos");
    
    // Actualizar gráficas existentes solo si están inicializadas
    if (chartInstances.usuariosChart) {
        try {
            updateUsuariosChart(chartInstances.usuariosChart, data);
            console.log("Gráfica de usuarios actualizada con éxito");
        } catch (error) {
            console.error("Error al actualizar gráfica de usuarios:", error);
            const element = document.getElementById('usuariosChart');
            if (element) mostrarErrorEnGrafica(element, "Error al actualizar");
        }
    }
    
    if (chartInstances.proyectosPorMesChart) {
        try {
            updateProyectosPorMesChart(chartInstances.proyectosPorMesChart, data);
            console.log("Gráfica de proyectos por mes actualizada con éxito");
        } catch (error) {
            console.error("Error al actualizar gráfica de proyectos por mes:", error);
            const element = document.getElementById('proyectosPorMesChart');
            if (element) mostrarErrorEnGrafica(element, "Error al actualizar");
        }
    }
    
    if (chartInstances.desafiosChart) {
        try {
            updateDesafiosChart(chartInstances.desafiosChart, data);
            console.log("Gráfica de desafíos actualizada con éxito");
        } catch (error) {
            console.error("Error al actualizar gráfica de desafíos:", error);
            const element = document.getElementById('desafiosChart');
            if (element) mostrarErrorEnGrafica(element, "Error al actualizar");
        }
    }
    
    if (chartInstances.actividadChart) {
        try {
            updateActividadChart(chartInstances.actividadChart, data);
            console.log("Gráfica de actividad actualizada con éxito");
        } catch (error) {
            console.error("Error al actualizar gráfica de actividad:", error);
            const element = document.getElementById('actividadChart');
            if (element) mostrarErrorEnGrafica(element, "Error al actualizar");
        }
    }
    
    // Actualizar las nuevas gráficas de empresas
    updateEmpresasCharts(data);
}

/**
 * Crea la gráfica de distribución de usuarios
 */
function createUsuariosChart(data) {
    const canvas = document.getElementById('usuariosChart');
    if (!canvas) return null;
    
    // Colores más vibrantes
    const colors = ['#36a2eb', '#ff6384'];
    
    return new Chart(canvas, {
        type: 'doughnut',
        data: {
            labels: ['Voluntarios', 'Administradores'],
            datasets: [{
                data: [
                    data.totalVoluntarios || 0,
                    data.totalAdministradores || 0
                ],
                backgroundColor: colors,
                borderWidth: 1,
                borderColor: '#ffffff',
                hoverOffset: 6
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            layout: {
                padding: 0
            },
            plugins: {
                legend: { 
                    position: 'bottom',
                    labels: {
                        boxWidth: 12,
                        padding: 10,
                        usePointStyle: true,
                        pointStyle: 'circle'
                    }
                },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            const label = context.label || '';
                            const value = context.formattedValue || '';
                            const total = context.dataset.data.reduce((a, b) => a + b, 0);
                            const percentage = Math.round((context.raw / total) * 100);
                            return `${label}: ${value} (${percentage}%)`;
                        }
                    }
                }
            },
            animation: {
                animateRotate: true,
                animateScale: true,
                duration: 1000,
                easing: 'easeOutQuart'
            }
        }
    });
}

/**
 * Actualiza la gráfica de usuarios con nuevos datos
 */
function updateUsuariosChart(chart, data) {
    if (!chart) return;
    
    chart.data.datasets[0].data = [
        data.totalVoluntarios || 0,
        data.totalAdministradores || 0
    ];
    
    chart.update('none'); // Sin animación para actualización
}

/**
 * Crea la gráfica de estado de proyectos
 */
function createProyectosChart(data) {
    const canvas = document.getElementById('proyectosChart');
    // Si no existe en esta versión del dashboard, buscar alternativa
    if (!canvas) {
        const altCanvas = document.getElementById('proyectosPorMesChart');
        if (!altCanvas) return null;
        
        // Si existe el canvas para proyectos por mes, usar esos datos
        return createProyectosPorMesChart(data);
    }
    
    // Obtener datos de estado de proyectos
    const estadosProyecto = data.proyectosPorEstado || {};
    
    // Mapear etiquetas y valores
    const labels = ['ACTIVO', 'COMPLETADO', 'EXPIRADO', 'CANCELADO'];
    const values = [
        estadosProyecto.ACTIVO || 0,
        estadosProyecto.COMPLETADO || 0,
        estadosProyecto.EXPIRADO || 0,
        estadosProyecto.CANCELADO || 0
    ];
    
    // Asegurar que al menos hay un valor positivo para evitar un gráfico vacío
    const hayDatos = values.some(v => v > 0);
    if (!hayDatos) {
        // Si no hay datos, usar valores mínimos para visualización
        values[0] = 1; // Al menos un proyecto activo
    }
    
    // Colores vibrantes para cada estado
    const colors = [
        '#36a2eb', // Azul para ACTIVO
        '#4bc0c0', // Verde-azulado para COMPLETADO
        '#ff9f40', // Naranja para EXPIRADO
        '#ff6384'  // Rojo para CANCELADO
    ];
    
    // Crear contexto para gradientes
    const ctx = canvas.getContext('2d');
    const gradients = colors.map(color => {
        const colorRgb = hexToRgb(color);
        const gradient = ctx.createLinearGradient(0, 0, 0, canvas.height);
        gradient.addColorStop(0, `rgba(${colorRgb.r}, ${colorRgb.g}, ${colorRgb.b}, 0.8)`);
        gradient.addColorStop(1, `rgba(${colorRgb.r}, ${colorRgb.g}, ${colorRgb.b}, 0.6)`);
        return gradient;
    });
    
    // Etiquetas más amigables para el usuario
    const friendlyLabels = [
        'Activos', 
        'Completados', 
        'Expirados', 
        'Cancelados'
    ];
    
    // Establecer altura mínima para el canvas si no la tiene
    if (!canvas.style.height) {
        canvas.style.height = '250px';
    }
    
    // Forzar el refresco del canvas para evitar problemas de renderizado
    canvas.height = canvas.offsetHeight;
    canvas.width = canvas.offsetWidth;
    
    return new Chart(canvas, {
        type: 'doughnut',
        data: {
            labels: friendlyLabels,
            datasets: [{
                data: values,
                backgroundColor: gradients,
                borderColor: colors.map(color => {
                    const rgb = hexToRgb(color);
                    return `rgba(${rgb.r}, ${rgb.g}, ${rgb.b}, 1)`;
                }),
                borderWidth: 1,
                hoverOffset: 8,
                hoverBorderWidth: 2
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            cutout: '60%',
            layout: {
                padding: 10
            },
            plugins: {
                legend: { 
                    position: 'bottom',
                    labels: {
                        boxWidth: 12,
                        padding: 10,
                        usePointStyle: true,
                        pointStyle: 'circle'
                    }
                },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            const label = context.label || '';
                            const value = context.formattedValue || '';
                            const total = context.dataset.data.reduce((a, b) => a + b, 0);
                            const percentage = Math.round((context.raw / total) * 100);
                            return `${label}: ${value} (${percentage}%)`;
                        }
                    }
                }
            },
            animation: {
                animateRotate: true,
                animateScale: true,
                duration: 1200,
                easing: 'easeOutQuart'
            }
        }
    });
}

/**
 * Crea la gráfica de proyectos por mes
 */
function createProyectosPorMesChart(data) {
    const canvas = document.getElementById('proyectosPorMesChart');
    if (!canvas) return null;
    
    // Obtener datos de proyectos por mes
    const proyectosPorMes = data.proyectosPorMes || {};
    
    // Ordenar las claves por fecha
    const meses = Object.keys(proyectosPorMes).sort();
    
    // Limitar a los últimos 6 meses para no saturar el gráfico
    const ultimos6Meses = meses.slice(-6);
    
    // Mapear valores para los últimos 6 meses
    const valores = ultimos6Meses.map(mes => proyectosPorMes[mes] || 0);
    
    // Formatear etiquetas para mostrar solo mes (Ene, Feb, etc.)
    const etiquetas = ultimos6Meses.map(formatMonthLabel);
    
    // Crear contexto para gradiente
    const ctx = canvas.getContext('2d');
    const gradient = ctx.createLinearGradient(0, 0, 0, canvas.height);
    gradient.addColorStop(0, 'rgba(54, 162, 235, 0.8)');
    gradient.addColorStop(1, 'rgba(54, 162, 235, 0.2)');
    
    return new Chart(canvas, {
        type: 'bar',
        data: {
            labels: etiquetas,
            datasets: [{
                label: 'Proyectos creados',
                data: valores,
                backgroundColor: gradient,
                borderColor: 'rgba(54, 162, 235, 1)',
                borderWidth: 1,
                borderRadius: 4,
                barThickness: 'flex',
                maxBarThickness: 35
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            layout: {
                padding: 10
            },
            scales: {
                y: {
                    beginAtZero: true,
                    grid: {
                        color: 'rgba(0, 0, 0, 0.05)'
                    },
                    ticks: {
                        precision: 0
                    }
                },
                x: {
                    grid: {
                        display: false
                    }
                }
            },
            plugins: {
                legend: {
                    display: false
                },
                tooltip: {
                    callbacks: {
                        title: function(context) {
                            return context[0].label;
                        },
                        label: function(context) {
                            const label = context.dataset.label || '';
                            const value = context.formattedValue || '';
                            return `${label}: ${value}`;
                        }
                    }
                }
            },
            animation: {
                duration: 1000,
                easing: 'easeOutQuart'
            }
        }
    });
}

/**
 * Actualiza la gráfica de proyectos con nuevos datos
 */
function updateProyectosChart(chart, data) {
    if (!chart) return;
    
    // Verificar si es una gráfica de proyectos por mes
    if (chart.config.type === 'bar') {
        updateProyectosPorMesChart(chart, data);
        return;
    }
    
    // Para gráfica de estado de proyectos (tipo doughnut)
    const estadosProyecto = data.proyectosPorEstado || {};
    
    chart.data.datasets[0].data = [
        estadosProyecto.ACTIVO || 0,
        estadosProyecto.COMPLETADO || 0,
        estadosProyecto.EXPIRADO || 0,
        estadosProyecto.CANCELADO || 0
    ];
    
    chart.update('none'); // Sin animación para actualización
}

/**
 * Actualiza la gráfica de proyectos por mes
 */
function updateProyectosPorMesChart(chart, data) {
    if (!chart) return;
    
    // Obtener datos actualizados
    const proyectosPorMes = data.proyectosPorMes || {};
    
    // Ordenar las claves por fecha
    const meses = Object.keys(proyectosPorMes).sort();
    
    // Limitar a los últimos 6 meses
    const ultimos6Meses = meses.slice(-6);
    
    // Actualizar etiquetas y datos
    chart.data.labels = ultimos6Meses.map(formatMonthLabel);
    chart.data.datasets[0].data = ultimos6Meses.map(mes => proyectosPorMes[mes] || 0);
    
    chart.update('none');
}

/**
 * Convierte un color hexadecimal a un objeto RGB
 */
function hexToRgb(hex) {
    // Eliminar el símbolo # si existe
    hex = hex.replace(/^#/, '');
    
    // Convertir el valor hexadecimal a RGB
    let bigint = parseInt(hex, 16);
    return {
        r: (bigint >> 16) & 255,
        g: (bigint >> 8) & 255,
        b: bigint & 255
    };
}

/**
 * Crea la gráfica de estado de desafíos
 */
function createDesafiosChart(data) {
    const canvas = document.getElementById('desafiosChart');
    if (!canvas) return null;
    
    return new Chart(canvas, {
        type: 'pie',
        data: {
            labels: ['Completados', 'En Progreso', 'Sin Comenzar'],
            datasets: [{
                data: [
                    data.completados || 0,
                    data.enProgreso || 0,
                    data.sinComenzar || 0
                ],
                backgroundColor: [
                    '#4bc0c0', // Completados
                    '#ffcd56', // En Progreso
                    '#ff9f40'  // Sin Comenzar
                ],
                borderColor: '#ffffff',
                borderWidth: 1,
                hoverOffset: 6
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            layout: {
                padding: 0
            },
            plugins: {
                legend: {
                    position: 'bottom',
                    labels: {
                        boxWidth: 12,
                        padding: 10,
                        usePointStyle: true,
                        pointStyle: 'circle'
                    }
                },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            const label = context.label || '';
                            const value = context.formattedValue || '';
                            const total = context.dataset.data.reduce((a, b) => a + b, 0);
                            const percentage = Math.round((context.raw / total) * 100);
                            return `${label}: ${value} (${percentage}%)`;
                        }
                    }
                }
            },
            animation: {
                animateRotate: true,
                animateScale: true,
                duration: 1000,
                easing: 'easeOutQuart'
            }
        }
    });
}

/**
 * Actualiza la gráfica de desafíos con nuevos datos
 */
function updateDesafiosChart(chart, data) {
    if (!chart) return;
    
    chart.data.datasets[0].data = [
        data.completados || 0,
        data.enProgreso || 0,
        data.sinComenzar || 0
    ];
    
    chart.update('none');
}

/**
 * Crea la gráfica de actividad de usuarios
 */
function createActividadChart(data) {
    const canvas = document.getElementById('actividadChart');
    if (!canvas) return null;
    
    const actividadData = data.actividad || [];
    const labels = actividadData.map(item => item.fecha);
    const values = actividadData.map(item => item.actividades);
    
    // Definir gradiente para el área bajo la curva
    const ctx = canvas.getContext('2d');
    const gradientFill = ctx.createLinearGradient(0, 0, 0, 300);
    gradientFill.addColorStop(0, 'rgba(80, 205, 137, 0.6)');
    gradientFill.addColorStop(1, 'rgba(80, 205, 137, 0.1)');
    
    // Colores más vibrantes para los puntos
    const maxValue = Math.max(...values, 1);
    const colors = values.map(v => {
        const intensity = Math.max(0.3, v / maxValue);
        // Usar color verde azulado más vivo
        return `rgba(46, 184, 115, ${intensity + 0.2})`;
    });
    
    return new Chart(canvas, {
        type: 'line',
        data: {
            labels: labels,
            datasets: [{
                label: 'Actividades',
                data: values,
                backgroundColor: gradientFill,
                borderColor: 'rgba(46, 184, 115, 1)',
                borderWidth: 3,
                tension: 0.4,
                pointBackgroundColor: colors,
                pointBorderColor: 'white',
                pointBorderWidth: 2,
                pointRadius: 6,
                pointHoverRadius: 8,
                pointHoverBackgroundColor: '#2eb873',
                pointHoverBorderColor: 'white',
                pointHoverBorderWidth: 2,
                fill: true,
                cubicInterpolationMode: 'monotone'
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    display: false
                },
                tooltip: {
                    backgroundColor: 'rgba(46, 184, 115, 0.9)',
                    titleColor: 'white',
                    bodyColor: 'white',
                    bodyFont: {
                        size: 14
                    },
                    titleFont: {
                        size: 16,
                        weight: 'bold'
                    },
                    padding: 12,
                    displayColors: false,
                    callbacks: {
                        title: function(items) {
                            return `${items[0].label}`;
                        },
                        label: function(context) {
                            const value = context.raw;
                            return value === 1 ? `${value} actividad` : `${value} actividades`;
                        },
                        afterLabel: function(context) {
                            const value = context.raw;
                            const max = Math.max(...context.chart.data.datasets[0].data);
                            const percentage = Math.round((value / max) * 100);
                            return `${percentage}% del máximo semanal`;
                        }
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: {
                        stepSize: 1,
                        font: {
                            size: 11
                        },
                        color: '#666'
                    },
                    grid: {
                        display: true,
                        drawBorder: false,
                        color: 'rgba(200, 200, 200, 0.15)'
                    }
                },
                x: {
                    grid: {
                        display: false,
                        drawBorder: false
                    },
                    ticks: {
                        font: {
                            size: 11
                        },
                        color: '#666'
                    }
                }
            },
            animation: {
                duration: 2000,
                easing: 'easeOutQuart',
                delay: function(context) {
                    return context.dataIndex * 100;
                }
            },
            elements: {
                line: {
                    capBezierPoints: true
                }
            },
            interaction: {
                mode: 'index',
                intersect: false
            }
        }
    });
}

/**
 * Actualiza la gráfica de actividad con nuevos datos
 */
function updateActividadChart(chart, data) {
    if (!chart) return;
    
    const actividadData = data.actividad || [];
    const labels = actividadData.map(item => item.fecha);
    const values = actividadData.map(item => item.actividades);
    
    chart.data.labels = labels;
    chart.data.datasets[0].data = values;
    
    // Actualizar colores basados en los nuevos valores
    const maxValue = Math.max(...values, 1);
    const colors = values.map(v => {
        const intensity = Math.max(0.3, v / maxValue);
        return `rgba(46, 184, 115, ${intensity + 0.2})`;
    });
    chart.data.datasets[0].pointBackgroundColor = colors;
    
    chart.update('none');
}

/**
 * Formatea un número para presentación en la interfaz
 */
function formatNumber(num) {
    // Verificar si el valor es un número válido
    if (num === undefined || num === null || isNaN(parseFloat(num))) {
        return "0";
    }
    
    // Convertir a número si es string
    if (typeof num === 'string') {
        num = parseFloat(num);
    }
    
    // Si es un número entero, mostrar sin decimales
    if (Number.isInteger(num)) {
        return num.toString();
    }
    
    // Si tiene decimales, mostrar un decimal máximo
    return num.toFixed(1);
}

/**
 * Formatea etiquetas de meses
 */
function formatMonthLabel(yearMonthStr) {
    try {
        // Si la cadena es undefined o null, devolver un valor por defecto
        if (!yearMonthStr) {
            return "N/A";
        }
        
        // Si la cadena tiene formato ISO completo (2023-04-10T03:00)
        if (yearMonthStr.includes('T')) {
            const date = new Date(yearMonthStr);
            if (isNaN(date.getTime())) {
                return yearMonthStr; // Si la fecha no es válida, devolver el original
            }
            // Formatear como dd/mm/yy
            return `${date.getDate().toString().padStart(2, '0')}/${(date.getMonth() + 1).toString().padStart(2, '0')}/${date.getFullYear().toString().substring(2)}`;
        }
        
        // Para formato año-mes (2023-04)
        if (yearMonthStr.includes('-')) {
            const [year, month] = yearMonthStr.split('-');
            if (!year || !month || isNaN(parseInt(month)) || parseInt(month) < 1 || parseInt(month) > 12) {
                return yearMonthStr; // Si el formato no es válido, devolver el original
            }
            const months = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'];
            return `${months[parseInt(month) - 1]} ${year}`;
        }
        
        // Para cualquier otro formato
        return yearMonthStr;
    } catch (e) {
        console.error("Error al formatear fecha:", e);
        return yearMonthStr; // Devolver la fecha original en caso de error
    }
}

/**
 * Muestra un mensaje de estado
 */
function showStatusMessage(message, type = 'info', duration = 0) {
    // Crear contenedor para mensajes si no existe
    let container = document.getElementById('statusMessages');
    if (!container) {
        container = document.createElement('div');
        container.id = 'statusMessages';
        document.body.appendChild(container);
    }
    
    // Crear nuevo mensaje
    const messageElement = document.createElement('div');
    messageElement.className = `notification ${type}`;
    
    // Estructura del mensaje
    messageElement.innerHTML = `
        <div class="notification-header">
            <h5 class="notification-title">${type.charAt(0).toUpperCase() + type.slice(1)}</h5>
            <button class="notification-close">&times;</button>
        </div>
        <p class="notification-message">${message}</p>
    `;
    
    // Agregar al contenedor
    container.appendChild(messageElement);
    
    // Configurar cierre
    const closeButton = messageElement.querySelector('.notification-close');
    closeButton.addEventListener('click', () => {
        container.removeChild(messageElement);
    });
    
    // Si hay duración, eliminar después del tiempo indicado
    if (duration > 0) {
        setTimeout(() => {
            if (container.contains(messageElement)) {
                container.removeChild(messageElement);
            }
        }, duration);
    }
}

/**
 * Elimina todos los mensajes de estado
 */
function clearStatusMessages() {
    const container = document.getElementById('statusMessages');
    if (container) {
        container.innerHTML = '';
    }
}

/**
 * Procesa los datos de foros para la gráfica
 */
function procesarDatosForos(foros) {
    // Ordenar foros por número de comentarios y tomar los 5 con más actividad
    return foros
        .map(foro => ({
            nombre: foro.titulo || 'Sin título',
            comentarios: foro.comentarios ? foro.comentarios.length : 0
        }))
        .sort((a, b) => b.comentarios - a.comentarios)
        .slice(0, 5);
}

/**
 * Procesa los datos de desafíos para agruparlos por categoría
 */
function procesarDatosDesafios(desafios) {
    // Agrupar desafíos por categoría
    const categorias = {};
    
    desafios.forEach(desafio => {
        const categoria = desafio.categoria || 'Sin categoría';
        categorias[categoria] = (categorias[categoria] || 0) + 1;
    });
    
    return categorias;
}

/**
 * Crea la gráfica de foros más activos
 */
function createForosActivosChart(data) {
    const canvas = document.getElementById('forosActivosChart');
    if (!canvas) return null;
    
    const forosActivos = data.forosActivos || fallbackData.forosActivos;
    const labels = forosActivos.map(foro => foro.nombre);
    const values = forosActivos.map(foro => foro.comentarios);
    
    // Colores con gradiente para las barras
    const ctx = canvas.getContext('2d');
    const gradient = ctx.createLinearGradient(0, 0, 0, 400);
    gradient.addColorStop(0, 'rgba(155, 89, 182, 0.8)');
    gradient.addColorStop(1, 'rgba(155, 89, 182, 0.2)');
    
    return new Chart(canvas, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: [{
                label: 'Comentarios',
                data: values,
                backgroundColor: gradient,
                borderColor: 'rgba(155, 89, 182, 1)',
                borderWidth: 1,
                borderRadius: 6,
                hoverBackgroundColor: 'rgba(155, 89, 182, 0.9)'
            }]
        },
        options: {
            indexAxis: 'y',  // Barras horizontales
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    display: false
                },
                tooltip: {
                    backgroundColor: 'rgba(155, 89, 182, 0.9)',
                    titleColor: 'white',
                    bodyColor: 'white',
                    callbacks: {
                        label: function(context) {
                            const value = context.raw;
                            return `${value} comentarios`;
                        }
                    }
                }
            },
            scales: {
                x: {
                    beginAtZero: true,
                    ticks: {
                        precision: 0
                    },
                    grid: {
                        display: false
                    }
                },
                y: {
                    ticks: {
                        callback: function(value) {
                            const label = this.getLabelForValue(value);
                            // Truncar etiquetas largas
                            return label.length > 15 ? label.substr(0, 15) + '...' : label;
                        }
                    }
                }
            },
            animation: {
                delay: function(context) {
                    return context.dataIndex * 100;
                },
                duration: 1000,
                easing: 'easeOutQuart'
            }
        }
    });
}

/**
 * Actualiza la gráfica de foros más activos
 */
function updateForosActivosChart(chart, data) {
    if (!chart) return;
    
    const forosActivos = data.forosActivos || fallbackData.forosActivos;
    const labels = forosActivos.map(foro => foro.nombre);
    const values = forosActivos.map(foro => foro.comentarios);
    
    chart.data.labels = labels;
    chart.data.datasets[0].data = values;
    
    chart.update('none');
}

/**
 * Crea la gráfica de desafíos por categoría
 */
function createDesafiosCategoriaChart(data) {
    const canvas = document.getElementById('desafiosCategoriaChart');
    if (!canvas) return null;
    
    const desafiosCategorias = data.desafiosCategorias || fallbackData.desafiosCategorias;
    const labels = Object.keys(desafiosCategorias);
    const values = Object.values(desafiosCategorias);
    
    // Colores vibrantes para cada categoría
    const backgroundColors = [
        'rgba(230, 126, 34, 0.8)',
        'rgba(241, 196, 15, 0.8)',
        'rgba(46, 204, 113, 0.8)',
        'rgba(52, 152, 219, 0.8)',
        'rgba(155, 89, 182, 0.8)'
    ];
    
    // Asegurar que hay suficientes colores para todas las categorías
    while (backgroundColors.length < labels.length) {
        backgroundColors.push(backgroundColors[backgroundColors.length % 5]);
    }
    
    return new Chart(canvas, {
        type: 'polarArea',
        data: {
            labels: labels,
            datasets: [{
                data: values,
                backgroundColor: backgroundColors,
                borderWidth: 1,
                borderColor: 'white'
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'right',
                    labels: {
                        boxWidth: 12,
                        padding: 10,
                        usePointStyle: true,
                        pointStyle: 'circle',
                        font: {
                            size: 10
                        }
                    }
                },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            const value = context.raw;
                            const total = context.chart.data.datasets[0].data.reduce((a, b) => a + b, 0);
                            const percentage = Math.round((value / total) * 100);
                            return `${context.label}: ${value} (${percentage}%)`;
                        }
                    }
                }
            },
            scales: {
                r: {
                    ticks: {
                        display: false
                    },
                    grid: {
                        color: 'rgba(0, 0, 0, 0.05)'
                    }
                }
            },
            animation: {
                animateRotate: true,
                animateScale: true,
                duration: 1500,
                easing: 'easeOutCirc'
            }
        }
    });
}

/**
 * Actualiza la gráfica de desafíos por categoría
 */
function updateDesafiosCategoriaChart(chart, data) {
    if (!chart) return;
    
    const desafiosCategorias = data.desafiosCategorias || fallbackData.desafiosCategorias;
    const labels = Object.keys(desafiosCategorias);
    const values = Object.values(desafiosCategorias);
    
    chart.data.labels = labels;
    chart.data.datasets[0].data = values;
    
    chart.update('none');
}

function procesarDatosProyectos(proyectos) {
    // Verificar si hay proyectos antes de procesar
    if (!proyectos || proyectos.length === 0) {
        return {
            ACTIVO: 0,
            COMPLETADO: 0,
            EXPIRADO: 0,
            CANCELADO: 0
        };
    }
    
    // Inicializar contador para cada estado
    const conteoEstados = {
        ACTIVO: 0,
        COMPLETADO: 0,
        EXPIRADO: 0,
        CANCELADO: 0
    };
    
    // Fecha actual para verificar proyectos expirados
    const fechaActual = new Date();
    
    // Contar proyectos por estado
    proyectos.forEach(proyecto => {
        // Verificar si el proyecto está expirado pero aún marcado como activo
        if (proyecto.estado === 'ACTIVO' && proyecto.fechaExpiracion) {
            const fechaExpiracion = new Date(proyecto.fechaExpiracion);
            if (fechaExpiracion < fechaActual) {
                // Debería ser EXPIRADO pero está marcado como ACTIVO
                conteoEstados.EXPIRADO++;
            } else {
                conteoEstados.ACTIVO++;
            }
        } else {
            // Incrementar el contador correspondiente al estado del proyecto
            if (conteoEstados.hasOwnProperty(proyecto.estado)) {
                conteoEstados[proyecto.estado]++;
            }
        }
    });
    
    return conteoEstados;
}

/**
 * Actualiza las estadísticas del dashboard con los datos proporcionados
 * @param {Object} data - Datos del dashboard
 */
function actualizarEstadisticas(data) {
    try {
        if (!data) {
            console.error("Error: datos nulos o indefinidos en actualizarEstadisticas");
            return;
        }
        
        console.log("Actualizando estadísticas con datos:", data);
        
        // Actualizar las métricas principales utilizando la función dedicada
        actualizarMetricasPrincipales(data);
        
        // Aquí se pueden agregar más actualizaciones para otras estadísticas específicas
    } catch (error) {
        console.error("Error al actualizar estadísticas:", error);
    }
}

/**
 * Actualiza las métricas principales en el DOM con los datos proporcionados
 * @param {Object} data - Datos del dashboard
 */
function actualizarMetricasPrincipales(data) {
    try {
        if (!data) {
            console.error("Error: datos nulos o indefinidos en actualizarMetricasPrincipales");
            return;
        }
        
        console.log("Actualizando métricas principales:", data);
        
        // Verificar si los datos vienen en formato anidado (data.metricas) o directamente en data
        const metricas = data.metricas || data;
        
        // Actualizar total de usuarios
        const totalUsuariosElement = document.getElementById("totalUsuarios");
        if (totalUsuariosElement) {
            totalUsuariosElement.textContent = formatNumber(metricas.totalUsuarios || 0);
            totalUsuariosElement.classList.remove("loading");
        } else {
            console.warn("Elemento totalUsuarios no encontrado en el DOM");
        }
        
        // Actualizar proyectos activos
        const proyectosActivosElement = document.getElementById("proyectosActivos");
        if (proyectosActivosElement) {
            proyectosActivosElement.textContent = formatNumber(metricas.proyectosActivos || 0);
            proyectosActivosElement.classList.remove("loading");
        } else {
            console.warn("Elemento proyectosActivos no encontrado en el DOM");
        }
        
        // Actualizar desafíos completados
        const desafiosCompletadosElement = document.getElementById("desafiosCompletados");
        if (desafiosCompletadosElement) {
            desafiosCompletadosElement.textContent = formatNumber(metricas.desafiosCompletados || 0);
            desafiosCompletadosElement.classList.remove("loading");
        } else {
            console.warn("Elemento desafiosCompletados no encontrado en el DOM");
        }
        
        // Actualizar empresas participantes
        const empresasParticipantesElement = document.getElementById("empresasParticipantes");
        if (empresasParticipantesElement) {
            empresasParticipantesElement.textContent = formatNumber(metricas.empresasParticipantes || 0);
            empresasParticipantesElement.classList.remove("loading");
        } else {
            console.warn("Elemento empresasParticipantes no encontrado en el DOM");
        }
        
        // Actualizar las tendencias
        actualizarClaseTendencia("usuariosTendencia", metricas.tendenciaUsuarios);
        actualizarClaseTendencia("proyectosTendencia", metricas.tendenciaProyectos);
        actualizarClaseTendencia("desafiosTendencia", metricas.tendenciaDesafios);
        actualizarClaseTendencia("empresasTendencia", metricas.tendenciaEmpresas);
        
    } catch (error) {
        console.error("Error al actualizar métricas principales:", error);
    }
}

/**
 * Actualiza la clase y contenido de un elemento de tendencia según el valor
 * @param {HTMLElement} elemento - Elemento DOM de tendencia
 * @param {number} valor - Valor numérico de la tendencia
 */
function actualizarClaseTendencia(id, valor) {
    const elemento = typeof id === 'string' ? document.getElementById(id) : id;
    
    if (!elemento) return;
    
    // Limpiar clases actuales
    elemento.classList.remove("positivo", "negativo", "neutro");
    elemento.classList.remove("loading");
    
    // Valor por defecto
    if (valor === undefined || valor === null) {
        valor = 0;
    }
    
    // Parseamos el valor a número si es un string
    if (typeof valor === 'string') {
        valor = parseFloat(valor);
    }
    
    // Si no es un número, convertir a 0
    if (isNaN(valor)) {
        valor = 0;
    }
    
    // Formatear el valor con signo y porcentaje
    const formateado = formatearPorcentaje(valor);
    
    // Añadir clase y icono según el valor
    if (valor > 0) {
        elemento.classList.add("positivo");
        elemento.innerHTML = `<i class="fas fa-arrow-up"></i> ${formateado}`;
    } else if (valor < 0) {
        elemento.classList.add("negativo");
        elemento.innerHTML = `<i class="fas fa-arrow-down"></i> ${formateado}`;
    } else {
        elemento.classList.add("neutro");
        elemento.innerHTML = `<i class="fas fa-equals"></i> ${formateado}`;
    }
}

/**
 * Verifica si los elementos del DOM para gráficas existen
 * y muestra mensajes en consola para depuración
 */
function verificarElementosDOM() {
    const elementos = [
        "totalUsuarios", "proyectosActivos", "desafiosCompletados", "empresasParticipantes", 
        "usuariosTendencia", "proyectosTendencia", "desafiosTendencia", "empresasTendencia",
        "usuariosChart", "proyectosPorMesChart", "desafiosChart", "actividadChart"
    ];
    
    let todosPresentes = true;
    
    elementos.forEach(id => {
        const elemento = document.getElementById(id);
        if (!elemento) {
            console.warn(`Elemento ${id} no encontrado en el DOM`);
            todosPresentes = false;
        }
    });
    
    if (todosPresentes) {
        console.log("Todos los elementos necesarios para el dashboard están presentes en el DOM");
    } else {
        console.error("Faltan elementos en el DOM, algunas funcionalidades pueden no funcionar correctamente");
    }
}

/**
 * Muestra un mensaje de error en un contenedor de gráfica
 */
function mostrarErrorEnGrafica(element, mensaje) {
    if (!element) return;
    
    // Crear contenedor de error si no existe
    let errorContainer = element.parentNode.querySelector('.chart-message');
    if (!errorContainer) {
        errorContainer = document.createElement('div');
        errorContainer.className = 'chart-message';
        element.parentNode.appendChild(errorContainer);
    }
    
    // Mostrar mensaje de error
    errorContainer.innerHTML = `
        <div class="error-message">
            <i class="fas fa-exclamation-triangle" style="font-size: 24px; margin-bottom: 10px;"></i>
            <p>${mensaje}</p>
        </div>
    `;
}

/**
 * Inicializa las gráficas de empresas
 */
function initializeEmpresasCharts(data) {
    // Verificar si tenemos datos válidos de empresas
    if (!data || (!data.empresasRanking && !cachedEmpresasRanking)) {
        console.warn("No hay datos de empresas disponibles para inicializar gráficos");
        return;
    }
    
    // Usar datos cacheados si están disponibles y no hay datos nuevos
    if (!data.empresasRanking && cachedEmpresasRanking) {
        console.log("Usando datos de empresas cacheados para inicialización");
        data.empresasRanking = cachedEmpresasRanking;
    }
    
    // Asegurar que empresasParticipantes refleje el número real de empresas
    if (data.empresasRanking) {
        data.empresasParticipantes = data.empresasRanking.length;
    }
    
    try {
        console.log("Inicializando gráficas de empresas con datos:", data);
        
        // Verificar si los datos contienen información real de empresas
        if (data && data.empresasRanking && Array.isArray(data.empresasRanking) && data.empresasRanking.length > 0) {
            // Guardar en caché los datos reales para uso futuro
            lastRealEmpresasData = {
                empresasRanking: [...data.empresasRanking],
                participacionMensual: data.participacionMensual ? {...data.participacionMensual} : {}
            };
            console.log("Datos reales de empresas guardados en caché");
            
            // Asegurar que la métrica refleje consistentemente el número de empresas
            // Ya no necesitamos actualizar aquí pues se actualiza en loadDashboardData
            console.log("Métrica de empresas participantes ya actualizada en normalizedData:", data.empresasParticipantes);
        } else if (lastRealEmpresasData) {
            // Usar datos reales de caché si están disponibles
            console.log("Usando datos de empresas en caché:", lastRealEmpresasData);
            
            // Combinar datos para asegurar que los datos de caché se usen solo para empresas
            data = {
                ...data,
                empresasRanking: lastRealEmpresasData.empresasRanking,
                participacionMensual: lastRealEmpresasData.participacionMensual
            };
        }
        
        // Crear gráfica de ranking de empresas
        if (document.getElementById('empresasRankingChart')) {
            // Destruir la instancia existente si hay alguna
            if (chartInstances.empresasRankingChart && typeof chartInstances.empresasRankingChart.destroy === 'function') {
                console.log("Destruyendo gráfica de ranking de empresas existente");
                chartInstances.empresasRankingChart.destroy();
            }
            chartInstances.empresasRankingChart = createEmpresasRankingChart(data);
            console.log("Gráfica de ranking de empresas creada con éxito");
        } else {
            console.warn("Elemento empresasRankingChart no encontrado");
        }
        
        // Crear gráfica de participación mensual
        if (document.getElementById('empresasMensualChart')) {
            // Destruir la instancia existente si hay alguna
            if (chartInstances.empresasMensualChart && typeof chartInstances.empresasMensualChart.destroy === 'function') {
                console.log("Destruyendo gráfica de participación mensual existente");
                chartInstances.empresasMensualChart.destroy();
            }
            chartInstances.empresasMensualChart = createEmpresasMensualChart(data);
            console.log("Gráfica de participación mensual creada con éxito");
        } else {
            console.warn("Elemento empresasMensualChart no encontrado");
        }
        
        // Actualizar tabla de empresas
        actualizarTablaEmpresas(data);
    } catch (error) {
        console.error("Error al inicializar gráficas de empresas:", error);
        showStatusMessage("Error al inicializar gráficas de empresas: " + error.message, "error", 5000);
    }
}

/**
 * Actualiza las gráficas de empresas con nuevos datos
 * Optimizada para rendimiento y robustez
 */
function updateEmpresasCharts(data) {
    // Validación robusta de datos de entrada
    if (!data) {
        console.warn("No hay datos para actualizar gráficos de empresas");
        showStatusMessage("No se pudieron cargar datos de empresas", "warning", 3000);
        return;
    }
    
    // Usar datos cacheados con validación mejorada
    if (!data.empresasRanking && cachedEmpresasRanking && Array.isArray(cachedEmpresasRanking)) {
        console.log("Usando datos de empresas cacheados para actualización");
        data.empresasRanking = [...cachedEmpresasRanking]; // Crear copia para evitar mutaciones no deseadas
    }
    
    try {
        // Actualizar métricas de empresas participantes con validación
        if (data.empresasRanking && Array.isArray(data.empresasRanking)) {
            // Actualizar contador con el número real de empresas
        data.empresasParticipantes = data.empresasRanking.length;
        
            // Actualizar elemento DOM con manejo de errores
            const metricaElement = document.getElementById('empresasParticipantesValor');
        if (metricaElement) {
            metricaElement.textContent = data.empresasParticipantes;
                // Aplicar animación sutil para destacar la actualización
                metricaElement.classList.add('actualizado');
                setTimeout(() => metricaElement.classList.remove('actualizado'), 1000);
            }
        }
        
        console.log("Actualizando gráficas de empresas con datos:", 
                   data.empresasRanking ? `${data.empresasRanking.length} empresas` : "Sin datos de ranking");
        
        // Verificación mejorada de datos reales
        const tieneRankingValido = data && 
                                 data.empresasRanking && 
                                 Array.isArray(data.empresasRanking) && 
                                 data.empresasRanking.length > 0;
        
        // Gestión de datos para las gráficas
        if (tieneRankingValido) {
            // Guardar datos reales en caché con copia profunda para evitar referencias
            lastRealEmpresasData = {
                empresasRanking: JSON.parse(JSON.stringify(data.empresasRanking)),
                participacionMensual: data.participacionMensual ? JSON.parse(JSON.stringify(data.participacionMensual)) : {}
            };
            
            // Actualizar caché global para uso futuro
            cachedEmpresasRanking = [...data.empresasRanking];
            
            console.log("Datos de empresas actualizados y almacenados en caché correctamente");
        } else if (lastRealEmpresasData) {
            // Usar datos reales de caché con verificación
            console.log("Usando datos históricos de empresas para actualización");
            
            // Combinar datos con spread operator para mayor legibilidad
            data = {
                ...data,
                empresasRanking: [...lastRealEmpresasData.empresasRanking],
                participacionMensual: {...lastRealEmpresasData.participacionMensual}
            };
        }
        
        // Función de utilidad para reconstruir gráficos de forma segura
        const reconstruirGrafico = (chartInstance, chartType, chartCreator) => {
            if (chartInstance) {
                try {
                    // Comprobar si el método destroy existe antes de llamarlo
                    if (typeof chartInstance.destroy === 'function') {
                        chartInstance.destroy();
                    }
                    return chartCreator(data);
                } catch (err) {
                    console.error(`Error al reconstruir gráfico ${chartType}:`, err);
                    mostrarErrorEnGrafica(
                        document.getElementById(chartType), 
                        `Error al actualizar: ${err.message}`
                    );
                    return null;
                }
            }
            return chartInstance;
        };
        
        // Actualizar gráficos de forma segura con manejo optimizado de errores
        chartInstances.empresasRankingChart = reconstruirGrafico(
            chartInstances.empresasRankingChart, 
            'empresasRankingChart', 
            createEmpresasRankingChart
        );
        
        chartInstances.empresasMensualChart = reconstruirGrafico(
            chartInstances.empresasMensualChart, 
            'empresasMensualChart', 
            createEmpresasMensualChart
        );
        
        // Actualizar tabla de empresas con gestión de rendimiento
        window.requestAnimationFrame(() => {
        actualizarTablaEmpresas(data);
        });
        
        // Indicar éxito en la actualización
        console.log("Actualización de gráficas de empresas completada con éxito");
        
    } catch (error) {
        // Manejo de errores mejorado con detalle específico
        console.error("Error al actualizar gráficos de empresas:", error);
        showStatusMessage(`Error en actualización de datos: ${error.message}`, "error", 4000);
        
        // Intentar recuperación en caso de error
        if (chartInstances.empresasRankingChart && !chartInstances.empresasRankingChart.data) {
            chartInstances.empresasRankingChart = createEmpresasRankingChart(lastRealEmpresasData || {});
        }
        
        if (chartInstances.empresasMensualChart && !chartInstances.empresasMensualChart.data) {
            chartInstances.empresasMensualChart = createEmpresasMensualChart(lastRealEmpresasData || {});
        }
    }
}

/**
 * Crea la gráfica de ranking de empresas con visualizaciones avanzadas
 * Versión optimizada con mejor rendimiento y experiencia visual
 */
function createEmpresasRankingChart(data) {
    // Obtener y validar el elemento canvas
    const canvas = document.getElementById('empresasRankingChart');
    if (!canvas) {
        console.warn("Elemento empresasRankingChart no encontrado en el DOM");
        return null;
    }
    
    // Validación de contexto para prevenir errores de renderizado
    const ctx = canvas.getContext('2d');
    if (!ctx) {
        console.error("No se pudo obtener el contexto 2D del canvas");
        return null;
    }
    
    // Obtener datos de empresas con manejo avanzado de casos de error
    let empresasRanking = [];
    
    try {
        // Verificación robusta de datos de entrada
        if (data && data.empresasRanking && Array.isArray(data.empresasRanking) && data.empresasRanking.length > 0) {
            // Crear copia profunda para prevenir mutaciones no deseadas
            empresasRanking = JSON.parse(JSON.stringify(data.empresasRanking));
            console.log(`Procesando datos de ${empresasRanking.length} empresas para el ranking`);
        } else {
            // Sistema de respaldo inteligente con múltiples niveles
            if (cachedEmpresasRanking && Array.isArray(cachedEmpresasRanking) && cachedEmpresasRanking.length > 0) {
                empresasRanking = [...cachedEmpresasRanking];
                console.log("Usando datos cacheados para el ranking de empresas");
            } else if (lastRealEmpresasData && lastRealEmpresasData.empresasRanking) {
                empresasRanking = [...lastRealEmpresasData.empresasRanking];
                console.log("Usando datos históricos para el ranking de empresas");
            } else if (empresasFallbackData && empresasFallbackData.empresasRanking) {
                empresasRanking = [...empresasFallbackData.empresasRanking];
            console.warn("Usando datos de respaldo para el ranking de empresas");
            } else {
                // Generar datos de muestra si no hay nada disponible
                empresasRanking = [
                    { nombre: "Empresa A", participaciones: 45, puntos: 120, usuarios: 15 },
                    { nombre: "Empresa B", participaciones: 32, puntos: 95, usuarios: 12 },
                    { nombre: "Empresa C", participaciones: 28, puntos: 85, usuarios: 10 }
                ];
                console.warn("Usando datos de muestra para el ranking de empresas");
            }
        }
        
        // Normalización y validación de los datos para cada empresa
        empresasRanking = empresasRanking.map(empresa => ({
            nombre: empresa.nombre || "Empresa sin nombre",
            // Conversión segura a números con valores predeterminados
            participaciones: parseInt(empresa.participaciones) || 0,
            puntos: parseInt(empresa.puntos) || 0,
            usuarios: parseInt(empresa.usuarios) || 0,
            // Calcular una puntuación ponderada para clasificación
            puntuacion: (parseInt(empresa.participaciones) || 0) * 0.6 + 
                        (parseInt(empresa.puntos) || 0) * 0.3 +
                        (parseInt(empresa.usuarios) || 0) * 0.1
        }));
    } catch (error) {
        console.error("Error al procesar datos de empresas para el gráfico:", error);
        
        // Notificar el error en la interfaz
        mostrarErrorEnGrafica(canvas, `Error al procesar datos: ${error.message}`);
        
        // Intentar usar datos de respaldo como último recurso
        if (empresasFallbackData && empresasFallbackData.empresasRanking) {
            empresasRanking = [...empresasFallbackData.empresasRanking];
        } else {
            // Regresar null si no hay datos disponibles
            return null;
        }
    }
    
    // Ordenar por puntuación ponderada (algoritmo mejorado de clasificación)
    empresasRanking.sort((a, b) => b.puntuacion - a.puntuacion);
    
    // Limitar a top empresas para mejor visualización
    const TOP_EMPRESAS = 7; // Configurable según necesidades
    const topEmpresas = empresasRanking.slice(0, TOP_EMPRESAS);
    
    // Verificar si hay empresas para mostrar
    if (topEmpresas.length === 0) {
        console.warn("No hay datos de empresas para mostrar en el gráfico");
        mostrarErrorEnGrafica(canvas, "No hay datos disponibles");
        return null;
    }
    
    // Preparar datos para la gráfica con formato optimizado
    const labels = topEmpresas.map(emp => {
        // Limitar longitud de nombres muy largos para mejor visualización
        return emp.nombre.length > 15 ? emp.nombre.substring(0, 12) + '...' : emp.nombre;
    });
    
    const participaciones = topEmpresas.map(emp => emp.participaciones);
    
    // Generar colores con gradiente para mejor estética visual
    const generarColores = (cantidad) => {
        const baseColor = 'rgba(156, 39, 176, '; // Color base (púrpura)
        const colores = [];
        const opacidades = [0.9, 0.8, 0.7, 0.6, 0.5, 0.4, 0.3];
        
        for (let i = 0; i < cantidad; i++) {
            colores.push(baseColor + opacidades[Math.min(i, opacidades.length - 1)] + ')');
        }
        
        return colores;
    };
    
    const colors = generarColores(topEmpresas.length);
    const borderColors = topEmpresas.map((_, i) => colors[i].replace(')', '1)'));
    
    // Configurar opciones avanzadas para la gráfica
    const options = {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    display: false
                },
                tooltip: {
                backgroundColor: 'rgba(0, 0, 0, 0.8)',
                titleFont: {
                    weight: 'bold'
                },
                    callbacks: {
                    title: function(context) {
                        return context[0].label; // Usar nombre de empresa como título
                    },
                        label: function(context) {
                            const index = context.dataIndex;
                            const empresa = topEmpresas[index];
                            return [
                            `Participaciones: ${formatNumber(empresa.participaciones)}`,
                            `Usuarios: ${formatNumber(empresa.usuarios)}`,
                            `Puntos: ${formatNumber(empresa.puntos)}`
                        ];
                    },
                    labelTextColor: function() {
                        return '#fff';
                        }
                    }
                }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    grid: {
                        display: true,
                        color: 'rgba(0, 0, 0, 0.05)'
                    },
                    ticks: {
                    precision: 0,
                    font: {
                        size: 11
                    },
                    callback: function(value) {
                        return formatNumber(value);
                    }
                },
                title: {
                    display: true,
                    text: 'Participaciones',
                    color: 'rgba(0, 0, 0, 0.7)',
                    font: {
                        size: 12,
                        weight: 'normal'
                    }
                    }
                },
                x: {
                    grid: {
                        display: false
                },
                ticks: {
                    font: {
                        size: 11
                    }
                    }
                }
            },
            animation: {
                delay: function(context) {
                // Retraso incremental para efecto de cascada
                return context.dataIndex * 120;
                },
            duration: 1200,
                easing: 'easeOutQuart'
        },
        // Configuración para interacción táctil
        interaction: {
            intersect: false,
            mode: 'index'
        }
    };
    
    // Crear y retornar nueva instancia de gráfico con visualización mejorada
    try {
        return new Chart(canvas, {
            type: 'bar',
            data: {
                labels: labels,
                datasets: [{
                    label: 'Participaciones',
                    data: participaciones,
                    backgroundColor: colors,
                    borderColor: borderColors,
                    borderWidth: 1,
                    borderRadius: 6,
                    maxBarThickness: 40,
                    hoverBackgroundColor: 'rgba(156, 39, 176, 1)'
                }]
            },
            options: options
        });
    } catch (error) {
        console.error("Error al crear el gráfico de ranking:", error);
        mostrarErrorEnGrafica(canvas, `Error al crear el gráfico: ${error.message}`);
        return null;
    }
}

/**
 * Actualiza la gráfica de ranking de empresas
 */
function updateEmpresasRankingChart(chart, data) {
    if (!chart) return;
    
    try {
        // Obtener datos de empresas o usar fallback
        const empresasRanking = obtencionSegura(() => data.empresasRanking, empresasFallbackData.empresasRanking);
        
        // Ordenar por participaciones (descendente)
        empresasRanking.sort((a, b) => b.participaciones - a.participaciones);
        
        // Limitar a top 5 empresas
        const topEmpresas = empresasRanking.slice(0, 5);
        
        // Actualizar datos de la gráfica
        chart.data.labels = topEmpresas.map(emp => emp.nombre);
        chart.data.datasets[0].data = topEmpresas.map(emp => emp.participaciones);
        
        chart.update('none');
    } catch (error) {
        console.error("Error al actualizar gráfica de ranking de empresas:", error);
    }
}

/**
 * Crea la gráfica de participación mensual por empresa
 */
function createEmpresasMensualChart(data) {
    const canvas = document.getElementById('empresasMensualChart');
    if (!canvas) return null;
    
    // Obtener datos de participación mensual o usar fallback
    let participacionMensual = {};
    
    try {
        // Intentar obtener datos reales primero
        if (data && data.participacionMensual && typeof data.participacionMensual === 'object') {
            participacionMensual = data.participacionMensual;
            console.log("Usando datos reales de participación mensual:", participacionMensual);
        } else {
            // Si no hay datos reales, usar los de respaldo
            participacionMensual = empresasFallbackData.participacionMensual;
            console.warn("Usando datos de respaldo para la participación mensual");
        }
    } catch (error) {
        console.error("Error al procesar datos de participación mensual:", error);
        participacionMensual = empresasFallbackData.participacionMensual;
    }
    
    // Extraer los meses como etiquetas
    const meses = Object.keys(participacionMensual);
    
    // Identificar las empresas únicas en todos los meses
    let todasEmpresas = new Set();
    
    for (const mes in participacionMensual) {
        const empresasMes = participacionMensual[mes];
        for (const empresa in empresasMes) {
            todasEmpresas.add(empresa);
        }
    }
    
    // Convertir a array y limitar a 5 empresas más activas
    let empresasActivas = Array.from(todasEmpresas);
    
    // Ordenar empresas por su participación total
    empresasActivas = empresasActivas.sort((a, b) => {
        let totalA = 0;
        let totalB = 0;
        
        for (const mes in participacionMensual) {
            const empresasMes = participacionMensual[mes];
            totalA += empresasMes[a] || 0;
            totalB += empresasMes[b] || 0;
        }
        
        return totalB - totalA; // Ordenar de mayor a menor
    }).slice(0, 5); // Limitar a 5 empresas
    
    // Crear datasets para cada empresa
    const datasets = [];
    
    // Colores para las empresas (diferentes a los usados en otras gráficas)
    const colores = [
        'rgba(75, 192, 192, 0.8)',  // Verde azulado
        'rgba(153, 102, 255, 0.8)', // Púrpura
        'rgba(255, 159, 64, 0.8)',  // Naranja
        'rgba(54, 162, 235, 0.8)',  // Azul
        'rgba(255, 99, 132, 0.8)'   // Rosa
    ];
    
    // Crear un dataset para cada empresa
    empresasActivas.forEach((empresa, index) => {
        const data = meses.map(mes => {
            const empresasMes = participacionMensual[mes] || {};
            return empresasMes[empresa] || 0;
        });
        
        datasets.push({
            label: empresa,
            data: data,
            backgroundColor: colores[index % colores.length],
            borderColor: colores[index % colores.length].replace('0.8', '1'),
            borderWidth: 1
        });
    });
    
    // Formatear las etiquetas de meses para mejor visualización
    const labels = meses.map(mes => formatMonthLabel(mes));
    
    // Configurar la gráfica
    return new Chart(canvas, {
        type: 'bar',
        data: {
            labels: labels,
            datasets: datasets
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'top',
                    labels: {
                        boxWidth: 12,
                        usePointStyle: true,
                        pointStyle: 'rect'
                    }
                },
                tooltip: {
                    mode: 'index',
                    intersect: false
                }
            },
            scales: {
                x: {
                    grid: {
                        display: false
                    }
                },
                y: {
                    beginAtZero: true,
                    ticks: {
                        precision: 0
                    },
                    grid: {
                        color: 'rgba(0, 0, 0, 0.05)'
                    }
                }
            }
        }
    });
}

/**
 * Actualiza la gráfica de participación mensual por empresa
 */
function updateEmpresasMensualChart(chart, data) {
    if (!chart) return;
    
    try {
        // Obtener datos de participación mensual o usar fallback
        const participacionMensual = obtencionSegura(() => data.participacionMensual, empresasFallbackData.participacionMensual);
        
        // Extraer meses (ordenados cronológicamente)
        const meses = Object.keys(participacionMensual).sort();
        
        // Actualizar labels (etiquetas de meses)
        chart.data.labels = meses.map(formatMonthLabel);
        
        // Actualizar datos de cada dataset (empresa)
        chart.data.datasets.forEach((dataset, index) => {
            const empresa = dataset.label;
            
            // Datos de esta empresa para cada mes
            const datosEmpresa = meses.map(mes => {
                const datosMes = participacionMensual[mes];
                return datosMes[empresa] || 0;
            });
            
            dataset.data = datosEmpresa;
        });
        
        chart.update('none');
    } catch (error) {
        console.error("Error al actualizar gráfica de participación mensual:", error);
    }
}

/**
 * Actualiza la tabla de empresas con los datos más recientes
 * Versión optimizada con mejor rendimiento y experiencia de usuario
 */
function actualizarTablaEmpresas(data) {
    // Obtener referencia al elemento de tabla con validación
    const tablaElement = document.getElementById('empresasTable');
    if (!tablaElement) {
        console.warn("Elemento empresasTable no encontrado en el DOM");
        return;
    }
    
    // Validación detallada de datos
    if (!data) {
        console.warn("No hay datos para actualizar la tabla de empresas");
        return;
    }
    
    console.log("Actualizando tabla de empresas...");
    
    // Obtener datos de empresas con manejo avanzado de errores
    let empresasRanking = [];
    
    try {
        // Implementar validación robusta para datos de entrada
        if (data.empresasRanking && 
            Array.isArray(data.empresasRanking) && 
            data.empresasRanking.length > 0) {
            
            // Crear copia para evitar modificar la fuente original
            empresasRanking = JSON.parse(JSON.stringify(data.empresasRanking));
            console.log(`Procesando ${empresasRanking.length} empresas para la tabla`);
        } else {
            // Usar respaldo con mensaje informativo adecuado
            if (cachedEmpresasRanking && Array.isArray(cachedEmpresasRanking) && cachedEmpresasRanking.length > 0) {
                empresasRanking = [...cachedEmpresasRanking];
                console.log("Usando datos cacheados para la tabla de empresas");
            } else if (empresasFallbackData && empresasFallbackData.empresasRanking) {
                empresasRanking = [...empresasFallbackData.empresasRanking];
            console.warn("Usando datos de respaldo para la tabla de empresas");
            } else {
                // Sin datos disponibles
                console.warn("No hay datos disponibles para la tabla de empresas");
            }
        }
        
        // Normalización y validación de datos para cada empresa
        empresasRanking = empresasRanking.map(empresa => ({
            nombre: empresa.nombre || "Empresa sin nombre",
            participaciones: parseInt(empresa.participaciones) || 0,
            puntos: parseInt(empresa.puntos) || 0,
            usuarios: parseInt(empresa.usuarios) || 0,
            // Calcular puntuación para clasificación
            puntuacion: (parseInt(empresa.participaciones) || 0) * 0.6 + 
                       (parseInt(empresa.puntos) || 0) * 0.3 +
                       (parseInt(empresa.usuarios) || 0) * 0.1
        }));
    } catch (error) {
        console.error("Error al procesar datos para la tabla de empresas:", error);
        // Notificar al usuario del problema
        showStatusMessage("Error al procesar datos de empresas: " + error.message, "warning", 3000);
        
        // Intentar usar datos de respaldo
        if (empresasFallbackData && empresasFallbackData.empresasRanking) {
            empresasRanking = [...empresasFallbackData.empresasRanking];
        }
    }
    
    // Ordenar por puntuación compuesta (mejor algoritmo de clasificación)
    empresasRanking.sort((a, b) => b.puntuacion - a.puntuacion);
    
    // Limitar a top empresas (configurable)
    const MAX_EMPRESAS_TABLA = 10;
    const topEmpresas = empresasRanking.slice(0, MAX_EMPRESAS_TABLA);
    
    // Obtener el cuerpo de la tabla con validación
    let tbody = tablaElement.querySelector('tbody');
    if (!tbody) {
        // Crear tbody si no existe
        tbody = document.createElement('tbody');
        tablaElement.appendChild(tbody);
    }
    
    // Optimización: crear fragmento para inserción eficiente en el DOM
    const fragment = document.createDocumentFragment();
    
    // Si no hay datos, mostrar mensaje informativo
    if (topEmpresas.length === 0) {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td colspan="4" class="text-center empty-data-message">
                <i class="fas fa-info-circle"></i> No hay datos disponibles
            </td>
        `;
        fragment.appendChild(tr);
    } else {
        // Agregar filas con datos de empresas usando métodos eficientes
    topEmpresas.forEach((empresa, index) => {
        const tr = document.createElement('tr');
        
            // Aplicar clases para mejor visualización según posición
        if (index === 0) {
            tr.classList.add('ranking-first');
            } else if (index === 1) {
                tr.classList.add('ranking-second');
            } else if (index === 2) {
                tr.classList.add('ranking-third');
        }
        
            // Añadir clase para animación de entrada
            tr.classList.add('animate-row');
            tr.style.animationDelay = `${index * 50}ms`;
            
            // Construir contenido con template literals para mejor legibilidad
        tr.innerHTML = `
            <td>
                    <div class="empresa-ranking">
                        <span class="ranking-position position-${index + 1}">${index + 1}</span>
                <span class="empresa-nombre">${empresa.nombre}</span>
                    </div>
            </td>
                <td>
                    <span class="valor-numerico">${formatNumber(empresa.usuarios)}</span>
                    <span class="etiqueta-valor">usuarios</span>
                </td>
                <td>
                    <span class="valor-numerico">${formatNumber(empresa.participaciones)}</span>
                    <span class="etiqueta-valor">part.</span>
                </td>
                <td>
                    <span class="valor-numerico">${formatNumber(empresa.puntos)}</span>
                    <span class="etiqueta-valor">puntos</span>
                </td>
            `;
            
            // Agregar evento para mostrar detalles (opcional)
            tr.addEventListener('click', () => {
                showStatusMessage(`Detalles de ${empresa.nombre} cargados`, "info", 2000);
                // Aquí se podría implementar visualización de detalles
            });
            
            fragment.appendChild(tr);
        });
    }
    
    // Optimización: primero limpiar y luego insertar todo de una vez
    tbody.innerHTML = '';
    tbody.appendChild(fragment);
    
    // Agregar clase para indicar actualización con animación
    tablaElement.classList.add('table-updated');
    setTimeout(() => {
        tablaElement.classList.remove('table-updated');
    }, 1000);
    
    console.log("Tabla de empresas actualizada con éxito");
}

/**
 * Función de utilidad para obtener datos de forma segura evitando errores
 * @param {Function} getter - Función que intenta obtener los datos
 * @param {*} fallback - Valor a devolver si hay un error
 * @returns {*} Los datos obtenidos o el fallback
 */
function obtencionSegura(getter, fallback) {
    try {
        const result = getter();
        return result !== undefined && result !== null ? result : fallback;
    } catch (error) {
        return fallback;
    }
}

// Extender la función de inicialización existente para incluir las nuevas gráficas
const originalInitializeCharts = initializeCharts;
initializeCharts = function(data) {
    // Llamar a la función original primero
    originalInitializeCharts(data);
    
    // Inicializar las nuevas gráficas de empresas
    initializeEmpresasCharts(data);
};

// Extender la función de actualización existente
const originalUpdateCharts = updateCharts;
updateCharts = function(data) {
    // Llamar a la función original primero
    originalUpdateCharts(data);
    
    // Actualizar las nuevas gráficas de empresas
    updateEmpresasCharts(data);
}; 

/**
 * Sistema inteligente de gestión de caché y datos de respaldo para gráficos
 * Optimiza el rendimiento y la disponibilidad de datos
 * @param {Object} datosActuales - Datos actuales recibidos del servidor
 * @param {string} tipoEntidad - Tipo de entidad (empresas, proyectos, etc.)
 * @param {string} claveDatos - Clave para acceder a los datos específicos
 * @param {Array|Object} datosFallback - Datos de respaldo a usar si no hay otros disponibles
 * @returns {Array|Object} - Datos optimizados y verificados para su uso
 */
function gestionarDatos(datosActuales, tipoEntidad, claveDatos, datosFallback) {
    // Variables para almacenar los datos y su estado
    let datos = null;
    let fuenteDatos = 'actual';
    
    try {
        // Intentar obtener datos del objeto actual
        if (datosActuales && 
            datosActuales[claveDatos] && 
            (Array.isArray(datosActuales[claveDatos]) ? 
             datosActuales[claveDatos].length > 0 : 
             Object.keys(datosActuales[claveDatos]).length > 0)) {
                
            // Crear copia profunda para evitar modificar datos originales
            datos = JSON.parse(JSON.stringify(datosActuales[claveDatos]));
            
            // Verificar que los datos tengan la estructura correcta según tipo
            if (Array.isArray(datos)) {
                const tieneEstructuraCorrecta = datos.every(item => 
                    typeof item === 'object' && item !== null);
                    
                if (!tieneEstructuraCorrecta) {
                    console.warn(`Datos ${tipoEntidad} con estructura incorrecta`);
                    datos = null;
                }
            }
        }
        
        // Si no hay datos válidos, intentar usar caché según el tipo
        if (!datos) {
            // Buscar en las diferentes capas de caché según prioridad
            if (tipoEntidad === 'empresas') {
                if (cachedEmpresasRanking && Array.isArray(cachedEmpresasRanking) && cachedEmpresasRanking.length > 0) {
                    datos = JSON.parse(JSON.stringify(cachedEmpresasRanking));
                    fuenteDatos = 'caché global';
                } else if (lastRealEmpresasData && lastRealEmpresasData[claveDatos]) {
                    datos = JSON.parse(JSON.stringify(lastRealEmpresasData[claveDatos]));
                    fuenteDatos = 'caché histórica';
                }
            } else if (tipoEntidad === 'proyectos' && window.cachedProyectos) {
                datos = JSON.parse(JSON.stringify(window.cachedProyectos));
                fuenteDatos = 'caché proyectos';
            } else if (tipoEntidad === 'usuarios' && window.cachedUsuarios) {
                datos = JSON.parse(JSON.stringify(window.cachedUsuarios));
                fuenteDatos = 'caché usuarios';
            } else if (tipoEntidad === 'desafios' && window.cachedDesafios) {
                datos = JSON.parse(JSON.stringify(window.cachedDesafios));
                fuenteDatos = 'caché desafíos';
            }
        }
        
        // Si todavía no hay datos, usar fallback
        if (!datos) {
            datos = datosFallback ? JSON.parse(JSON.stringify(datosFallback)) : null;
            fuenteDatos = 'fallback';
        }
        
        // Si no se encontraron datos en ninguna fuente
        if (!datos) {
            console.warn(`No se encontraron datos de ${tipoEntidad} en ninguna fuente`);
            return [];
        }
        
        // Registrar fuente de datos para diagnóstico
        console.log(`Datos de ${tipoEntidad} obtenidos desde ${fuenteDatos}`);
        
        // Guardar en caché para uso futuro si fueron datos actuales
        if (fuenteDatos === 'actual' && tipoEntidad === 'empresas') {
            // Actualizar cachés existentes
            cachedEmpresasRanking = Array.isArray(datos) ? [...datos] : datos;
            
            // Asegurar que lastRealEmpresasData esté inicializado
            if (!lastRealEmpresasData) lastRealEmpresasData = {};
            lastRealEmpresasData[claveDatos] = JSON.parse(JSON.stringify(datos));
            
            console.log(`Datos de ${tipoEntidad} almacenados en caché`);
        }
        
        return datos;
    } catch (error) {
        console.error(`Error al gestionar datos de ${tipoEntidad}:`, error);
        // En caso de error, intentar usar fallback como último recurso
        return datosFallback || [];
    }
}
  