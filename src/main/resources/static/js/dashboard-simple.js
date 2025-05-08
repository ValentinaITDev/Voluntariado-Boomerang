/**
 * Dashboard Simple - Panel administrativo para Alcambio
 * Optimizado para recibir datos de la base de datos
 * Versión simplificada con funciones JS básicas
 */

// Almacena las gráficas
let graficas = {};

// Datos para cuando falle la carga
const datosRespaldo = {
    totalUsuarios: 0, totalVoluntarios: 0, totalAdministradores: 0,
    proyectosActivos: 0, desafiosCompletados: 0, empresasParticipantes: 0,
    tendenciaUsuarios: 0, tendenciaProyectos: 0,
    completados: 0, enProgreso: 0, sinComenzar: 0,
    proyectosPorMes: {},
    actividad: [
        {fecha: "Lun", actividades: 0}, {fecha: "Mar", actividades: 0}, {fecha: "Mie", actividades: 0},
        {fecha: "Jue", actividades: 0}, {fecha: "Vie", actividades: 0}, {fecha: "Sab", actividades: 0}, {fecha: "Dom", actividades: 0}
    ]
};

// Inicializar cuando se carga la página
document.addEventListener('DOMContentLoaded', function() {
    configurarBotonActualizar();
    cargarDatos();
});

// Configura el botón de actualización
function configurarBotonActualizar() {
    const boton = document.getElementById('refreshChartsBtn');
    if (boton) {
        boton.addEventListener('click', function() {
            const icono = this.querySelector('i');
            if (icono) icono.classList.add('spinning');
            mostrarMensaje('Actualizando datos...', 'info');
            cargarDatos()
                .catch(function(error) {
                    console.error("Error durante la actualización:", error);
                    // El manejo de errores ya está en cargarDatos
                })
                .finally(function() {
                    if (icono) icono.classList.remove('spinning');
                });
        });
    }
}

// Carga los datos desde el API usando Promesas
function cargarDatos() {
    return new Promise(function(resolve, reject) {
        mostrarMensaje('Cargando datos desde la base de datos...', 'info');
        const controlador = new AbortController();
        const idTimeout = setTimeout(function() {
            controlador.abort();
            reject(new Error('Timeout al cargar datos'));
        }, 8000);

        fetch('/api/dashboard/resumen', {
            signal: controlador.signal,
            headers: {'Cache-Control': 'no-cache', 'Pragma': 'no-cache'}
        })
        .then(function(respuesta) {
            clearTimeout(idTimeout);
            if (!respuesta.ok) {
                throw new Error(`Error del servidor: ${respuesta.status} ${respuesta.statusText}`);
            }
            return respuesta.json();
        })
        .then(function(datos) {
            if (datos.error) {
                console.warn("Advertencia desde el servidor:", datos.error);
            }
            const datosProcesados = procesarDatos(datos);
            actualizarUI(datosProcesados);
            mostrarMensaje('Datos actualizados correctamente desde la base de datos', 'success', 3000);
            resolve(datosProcesados);
        })
        .catch(function(error) {
            clearTimeout(idTimeout);
            console.error('Error al cargar datos:', error);
            mostrarMensaje('Error al cargar datos: ' + error.message + '. Usando datos de respaldo.', 'error');
            const datosFallbackProcesados = procesarDatos(datosRespaldo); // Procesar también el fallback
            actualizarUI(datosFallbackProcesados);
            reject(error); // Rechazar la promesa
        });
    });
}

// Procesa y formatea los datos recibidos (simplificado)
function procesarDatos(datos) {
    let actividadProcesada = datosRespaldo.actividad; // Empezar con fallback
    if (datos.actividad && Array.isArray(datos.actividad)) {
        actividadProcesada = [];
        for (let i = 0; i < datos.actividad.length; i++) {
            let item = datos.actividad[i];
            let fechaFormateada = item.fecha; // Mantener original por defecto
            if (typeof item.fecha === 'string' && item.fecha.length > 10) {
                try {
                    const fecha = new Date(item.fecha);
                    const diasSemana = ['Dom', 'Lun', 'Mar', 'Mie', 'Jue', 'Vie', 'Sab'];
                    fechaFormateada = diasSemana[fecha.getDay()];
                } catch(e){ console.error("Error parseando fecha de actividad", e); }
            } else if (item.diaSemana && !item.fecha) {
                fechaFormateada = item.diaSemana.substring(0, 3);
            }
            actividadProcesada.push({ ...item, fecha: fechaFormateada });
        }
    }

    return {
        ...datosRespaldo, // Usar como base
        ...datos, // Sobrescribir con datos reales
        actividad: actividadProcesada // Usar actividad procesada
    };
}

// Actualiza la interfaz con los datos
function actualizarUI(datos) {
    actualizarElemento('totalUsuarios', datos.totalUsuarios || 0);
    actualizarElemento('proyectosActivos', datos.proyectosActivos || 0);
    actualizarElemento('desafiosCompletados', datos.desafiosCompletados || 0);
    actualizarElemento('empresasParticipantes', datos.empresasParticipantes || 0);

    actualizarElemento('tendenciaUsuarios', `${formatearNumero(datos.tendenciaUsuarios)}% este mes`);
    actualizarElemento('tendenciaProyectos', `${formatearNumero(datos.tendenciaProyectos)}% este mes`);

    actualizarClaseTendencia('tendenciaUsuarios', datos.tendenciaUsuarios);
    actualizarClaseTendencia('tendenciaProyectos', datos.tendenciaProyectos);

    if (Object.keys(graficas).length === 0) {
        crearGraficas(datos);
    } else {
        actualizarGraficas(datos);
    }
    document.querySelectorAll('.loading').forEach(function(el){ el.classList.remove('loading'); });
}

// Crea todas las gráficas
function crearGraficas(datos) {
    const graficasExistentes = Object.keys(graficas);
    for(let i=0; i < graficasExistentes.length; i++){
        const g = graficas[graficasExistentes[i]];
        if (g && typeof g.destroy === 'function') g.destroy();
    }
    graficas = {}; // Resetear

    graficas.usuarios = crearGraficaUsuarios(datos);
    graficas.proyectos = crearGraficaProyectos(datos);
    graficas.desafios = crearGraficaDesafios(datos);
    graficas.actividad = crearGraficaActividad(datos);
}

// Actualiza todas las gráficas con nuevos datos
function actualizarGraficas(datos) {
    actualizarGraficaUsuarios(datos);
    actualizarGraficaProyectos(datos);
    actualizarGraficaDesafios(datos);
    actualizarGraficaActividad(datos);
}

// ---- Funciones de creación y actualización de gráficas ----
// (Se mantienen las estructuras básicas de Chart.js para funcionalidad)
// Se simplifican callbacks y bucles internos donde sea posible

function crearGraficaUsuarios(datos) {
    const canvas = document.getElementById('usuariosChart');
    if (!canvas) return null;
    if (!datos || typeof datos !== 'object') datos = { totalVoluntarios: 0, totalAdministradores: 0 };

    const ctx = canvas.getContext('2d');
    const gradientVol = ctx.createLinearGradient(0, 0, 0, 200); // Altura menor para simple
    gradientVol.addColorStop(0, 'hsl(210, 70%, 50%)');
    gradientVol.addColorStop(1, 'hsl(210, 70%, 70%)');
    const gradientAdmin = ctx.createLinearGradient(0, 0, 0, 200);
    gradientAdmin.addColorStop(0, 'hsl(340, 80%, 55%)');
    gradientAdmin.addColorStop(1, 'hsl(340, 80%, 75%)');

    const valoresData = [ parseInt(datos.totalVoluntarios || 0), parseInt(datos.totalAdministradores || 0) ];

    return new Chart(canvas, {
        type: 'doughnut',
        data: {
            labels: ['Voluntarios', 'Administradores'],
            datasets: [{
                data: valoresData,
                backgroundColor: [gradientVol, gradientAdmin],
                borderColor: ['#1565C0', '#C2185B'],
                borderWidth: 1, hoverOffset: 10 // Menor offset
            }]
        },
        options: {
            responsive: true, maintainAspectRatio: false, cutout: '60%',
            plugins: {
                legend: { position: 'bottom', labels: { font: { size: 12 }, padding: 15, usePointStyle: true } },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            if (!context || !context.dataset || !context.dataset.data) return '';
                            let value = context.dataset.data[context.dataIndex] || 0;
                            let total = 0;
                            for(let k=0; k < context.dataset.data.length; k++) total += (context.dataset.data[k] || 0);
                            let percentage = total > 0 ? Math.round((value / total) * 100) : 0;
                            return `${context.label}: ${value} (${percentage}%)`;
                        }
                    }
                }
            },
            animation: { animateScale: true, duration: 1000, easing: 'easeOutQuad' }
        }
    });
}

function actualizarGraficaUsuarios(datos) {
    if (!graficas.usuarios || !datos) return;
    const nuevosDatos = [ parseInt(datos.totalVoluntarios || 0), parseInt(datos.totalAdministradores || 0) ];
    try {
        graficas.usuarios.data.datasets[0].data = nuevosDatos;
        graficas.usuarios.update();
    } catch (error) {
        console.error('Error al actualizar gráfica de usuarios:', error);
    }
}

function crearGraficaProyectos(datos) {
    const canvas = document.getElementById('proyectosPorMesChart');
    if (!canvas) return null;
    if (!datos || !datos.proyectosPorMes) datos = { proyectosPorMes: {} };

    const proyectosPorMes = datos.proyectosPorMes || {};
    const meses = Object.keys(proyectosPorMes).sort();
    const valores = [];
    for(let i=0; i<meses.length; i++) valores.push(parseInt(proyectosPorMes[meses[i]] || 0));
    const etiquetas = meses.map(formatearMes);

    const ctx = canvas.getContext('2d');
    const gradientBar = ctx.createLinearGradient(0, 0, 0, 200);
    gradientBar.addColorStop(0, 'rgba(0, 150, 136, 0.8)');
    gradientBar.addColorStop(1, 'rgba(128, 203, 196, 0.6)');

    return new Chart(canvas, {
        type: 'bar',
        data: {
            labels: etiquetas,
            datasets: [{
                label: 'Proyectos',
                data: valores,
                backgroundColor: gradientBar,
                borderColor: '#00796B',
                borderWidth: 1, borderRadius: 4
            }]
        },
        options: {
            responsive: true, maintainAspectRatio: false,
            plugins: { legend: { display: false }, tooltip: {
                 callbacks: {
                    label: function(context) {
                        if (!context || !context.dataset || !context.dataset.data) return '';
                        return `Proyectos: ${context.dataset.data[context.dataIndex] || 0}`;
                    }
                }
            }},
            scales: {
                y: { beginAtZero: true, grid: { color: 'rgba(0, 0, 0, 0.05)' }, ticks: { color: '#666' } },
                x: { grid: { display: false }, ticks: { color: '#666' } }
            },
            animation: { duration: 800, easing: 'easeOutQuad' }
        }
    });
}

function actualizarGraficaProyectos(datos) {
    if (!graficas.proyectos) return;
    const proyectosPorMes = datos.proyectosPorMes || {};
    const meses = Object.keys(proyectosPorMes).sort();
    const valores = [];
    for(let i=0; i<meses.length; i++) valores.push(parseInt(proyectosPorMes[meses[i]] || 0));
    const etiquetas = meses.map(formatearMes);

    graficas.proyectos.data.labels = etiquetas;
    graficas.proyectos.data.datasets[0].data = valores;
    graficas.proyectos.update();
}

function crearGraficaDesafios(datos) {
    const canvas = document.getElementById('desafiosChart');
    if (!canvas) return null;
    if (!datos) datos = { completados: 0, enProgreso: 0, sinComenzar: 0 };

    const completados = parseInt(datos.completados || 0);
    const enProgreso = parseInt(datos.enProgreso || 0);
    const sinComenzar = parseInt(datos.sinComenzar || 0);

    return new Chart(canvas, {
        type: 'pie', // Cambiado a pie para simplicidad
        data: {
            labels: ['Completados', 'En Progreso', 'Sin Comenzar'],
            datasets: [{
                data: [completados, enProgreso, sinComenzar],
                backgroundColor: [ '#4CAF50', '#FFC107', '#9E9E9E' ],
                borderColor: ['#388E3C', '#FBC02D', '#616161'],
                borderWidth: 1, hoverOffset: 8
            }]
        },
        options: {
            responsive: true, maintainAspectRatio: false,
            plugins: {
                legend: { position: 'bottom', labels: { font: { size: 12 }, usePointStyle: true, padding: 15 } },
                tooltip: {
                    callbacks: {
                        label: function(context) {
                            if (!context || !context.dataset || !context.dataset.data) return '';
                            const value = context.dataset.data[context.dataIndex] || 0;
                            let sum = 0;
                            for(let k=0; k < context.dataset.data.length; k++) sum += (context.dataset.data[k] || 0);
                            const percentage = sum > 0 ? Math.round((value / sum) * 100) : 0;
                            return `${context.label}: ${value} (${percentage}%)`;
                        }
                    }
                }
            },
            animation: { animateRotate: true, duration: 1200, easing: 'easeOutBounce' }
        }
    });
}

function actualizarGraficaDesafios(datos) {
    if (!graficas.desafios) return;
    const nuevosDatos = [
        parseInt(datos.completados || 0),
        parseInt(datos.enProgreso || 0),
        parseInt(datos.sinComenzar || 0)
    ];
    graficas.desafios.data.datasets[0].data = nuevosDatos;
    graficas.desafios.update();
}

function crearGraficaActividad(datos) {
    const canvas = document.getElementById('actividadChart');
    if (!canvas) return null;
    if (!datos || !datos.actividad || !Array.isArray(datos.actividad)) datos = { actividad: [] };

    const actividad = datos.actividad || [];
    const etiquetas = [];
    const valores = [];
    for(let i=0; i<actividad.length; i++) {
        etiquetas.push(actividad[i].fecha || '');
        valores.push(actividad[i].actividades || 0);
    }

    const ctx = canvas.getContext('2d');
    const gradientLine = ctx.createLinearGradient(0, 0, 0, 200);
    gradientLine.addColorStop(0, 'rgba(3, 169, 244, 0.6)');
    gradientLine.addColorStop(1, 'rgba(179, 229, 252, 0.1)');

    return new Chart(canvas, {
        type: 'line',
        data: {
            labels: etiquetas,
            datasets: [{
                label: 'Actividades',
                data: valores,
                borderColor: '#0288D1',
                backgroundColor: gradientLine,
                pointBackgroundColor: '#01579B',
                pointRadius: 4, pointHoverRadius: 6,
                tension: 0.1, // Menos curvo
                fill: true, borderWidth: 2
            }]
        },
        options: {
            responsive: true, maintainAspectRatio: false,
            scales: {
                y: { beginAtZero: true, grid: { color: 'rgba(0, 0, 0, 0.05)' }, ticks: { color: '#666' } },
                x: { grid: { display: false }, ticks: { color: '#666' } }
            },
            plugins: { legend: { display: false }, tooltip: {
                callbacks: {
                    label: function(context) {
                        if (!context || !context.dataset || !context.dataset.data) return '';
                        return `Actividades: ${context.dataset.data[context.dataIndex] || 0}`;
                    },
                    title: function(context) {
                        if (!context || !context[0]) return '';
                        return `Día: ${context[0].label}`;
                    }
                }
            }},
            interaction: { mode: 'nearest', intersect: false, axis: 'x' },
            animation: { duration: 1000, easing: 'easeOutQuad' }
        }
    });
}

function actualizarGraficaActividad(datos) {
    if (!graficas.actividad) return;
    const actividad = datos.actividad || datosRespaldo.actividad;
    const etiquetas = [];
    const valores = [];
    for(let i=0; i<actividad.length; i++) {
        etiquetas.push(actividad[i].fecha || '');
        valores.push(actividad[i].actividades || 0);
    }
    graficas.actividad.data.labels = etiquetas;
    graficas.actividad.data.datasets[0].data = valores;
    graficas.actividad.update();
}

// --- Funciones auxiliares --- 

function actualizarElemento(id, valor) {
    const elemento = document.getElementById(id);
    if (elemento) elemento.textContent = valor;
}

function actualizarClaseTendencia(id, tendencia) {
    const elemento = document.getElementById(id);
    if (!elemento) return;
    const icono = elemento.parentElement.querySelector('i');
    if (!icono) return;

    elemento.parentElement.classList.remove('trend-up', 'trend-down');
    icono.classList.remove('fa-arrow-up', 'fa-arrow-down');

    if (tendencia >= 0) {
        elemento.parentElement.classList.add('trend-up');
        icono.classList.add('fa-arrow-up');
    } else {
        elemento.parentElement.classList.add('trend-down');
        icono.classList.add('fa-arrow-down');
    }
}

function mostrarMensaje(mensaje, tipo, duracion) {
    tipo = tipo || 'info';
    duracion = duracion || 0;
    const contenedor = document.getElementById('statusMessages');
    if (!contenedor) return;

    const idMensaje = 'msg_' + Date.now();
    const claseMensaje = tipo === 'error' ? 'alert-danger' :
                         tipo === 'success' ? 'alert-success' :
                         'alert-info';
    const elemento = document.createElement('div');
    elemento.id = idMensaje;
    elemento.className = `alert ${claseMensaje}`;
    elemento.innerHTML = mensaje;
    elemento.style.opacity = '0';
    elemento.style.transform = 'translateY(-10px)';
    elemento.style.transition = 'all 0.3s ease';
    contenedor.appendChild(elemento);
    setTimeout(function() {
        elemento.style.opacity = '1';
        elemento.style.transform = 'translateY(0)';
    }, 50);

    if (duracion > 0) {
        setTimeout(function() {
            const msg = document.getElementById(idMensaje);
            if (msg) {
                msg.style.opacity = '0';
                msg.style.transform = 'translateY(-10px)';
                setTimeout(function(){ msg.remove(); }, 300);
            }
        }, duracion);
    }
    return idMensaje;
}

function formatearNumero(num) {
    if (num === undefined || num === null) return 0;
    return Number(num).toFixed(1);
}

function formatearMes(yearMonth) {
    if (!yearMonth || typeof yearMonth !== 'string') return 'N/A';
    try {
        const parts = yearMonth.split('-');
        if (parts.length !== 2) return yearMonth;
        const year = parts[0];
        const month = parts[1];
        const meses = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'];
        const monthIndex = parseInt(month) - 1;
        if (monthIndex >= 0 && monthIndex < 12) {
            return `${meses[monthIndex]} ${year.substring(2)}`; // Año corto
        } else {
            return yearMonth;
        }
    } catch (e) {
        console.error('Error al formatear mes:', e);
        return yearMonth;
    }
}

// Función de animación deprecada, se usa la interna de Chart.js
/* function animarActualizacion(grafica, datosAntiguos, datosNuevos, duracion, tipoAnimacion) { ... } */ 