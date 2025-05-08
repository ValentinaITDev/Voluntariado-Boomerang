// Animación de números en las estadísticas
function animateValue(obj, start, end, duration) {
    let startTimestamp = null;
    const step = function(timestamp) {
        if (!startTimestamp) startTimestamp = timestamp;
        const progress = Math.min((timestamp - startTimestamp) / duration, 1);
        let value = Math.floor(progress * (end - start) + start);
        if (obj.classList.contains('stat-number')) {
            obj.innerHTML = value > 100 ? value.toLocaleString() : value;
            if (end <= 100 && obj.innerText.indexOf('%') === -1 && obj.parentElement.querySelector('h3').innerText.includes('Satisfacción')) {
                obj.innerHTML += '%';
            }
        }
        if (progress < 1) {
            window.requestAnimationFrame(step);
        }
    };
    window.requestAnimationFrame(step);
}

// Cargar proyectos destacados usando Promesas y bucle for
function cargarProyectosDestacados() {
    const proyectosGrid = document.getElementById('proyectos-grid');
    if (!proyectosGrid) {
        console.error('Elemento proyectos-grid no encontrado.');
        return;
    }

    fetch('/api/proyectos')
        .then(function(response) {
            if (!response.ok) {
                throw new Error(`Error al cargar proyectos: ${response.statusText}`);
            }
            return response.json();
        })
        .then(function(proyectos) {
            proyectosGrid.innerHTML = '';

            if (!Array.isArray(proyectos) || proyectos.length === 0) {
                proyectosGrid.innerHTML = '<div class="no-projects">No hay proyectos destacados disponibles</div>';
                console.log('No se encontraron proyectos, cargando ejemplos...');
                cargarProyectosEjemplo();
                return;
            }

            const proyectosAMostrar = proyectos.slice(0, 2);
            for (let i = 0; i < proyectosAMostrar.length; i++) {
                const proyecto = proyectosAMostrar[i];
                const progreso = proyecto.progreso || Math.floor(Math.random() * 100);

                let prioridadClase = 'medium-priority';
                let prioridadTexto = 'Media Prioridad';
                if (proyecto.estado) {
                    if (proyecto.estado === 'URGENTE' || proyecto.estado === 'CRITICO') {
                        prioridadClase = 'high-priority';
                        prioridadTexto = 'Alta Prioridad';
                    } else if (proyecto.estado === 'COMPLETADO' || proyecto.estado === 'INACTIVO') {
                        prioridadClase = 'low-priority';
                        prioridadTexto = 'Baja Prioridad';
                    }
                }

                const voluntarios = proyecto.voluntarios || proyecto.participantes || Math.floor(Math.random() * 50) + 5;

                let diasRestantes = 30;
                if (proyecto.fechaFin) {
                    try {
                        const fechaFin = new Date(proyecto.fechaFin);
                        const hoy = new Date();
                        if (!isNaN(fechaFin.getTime()) && !isNaN(hoy.getTime())) {
                            const diferenciaTiempo = fechaFin - hoy;
                            diasRestantes = Math.max(0, Math.ceil(diferenciaTiempo / (1000 * 60 * 60 * 24)));
                        } else {
                             console.warn(`FechaFin inválida para proyecto ${proyecto.nombre || proyecto.id}: ${proyecto.fechaFin}`);
                             diasRestantes = '?';
                        }
                    } catch(e) {
                         console.error(`Error calculando días restantes para proyecto ${proyecto.nombre || proyecto.id}:`, e);
                         diasRestantes = '?';
                    }
                } else {
                    diasRestantes = Math.floor(Math.random() * 30) + 1;
                }

                const tarjeta = document.createElement('div');
                tarjeta.className = 'admin-project-card';
                tarjeta.innerHTML = `
                    <div class="project-header">
                        <h3>${proyecto.nombre || 'Proyecto Sin Nombre'}</h3>
                        <span class="status ${prioridadClase}">${prioridadTexto}</span>
                    </div>
                    <div class="project-stats">
                        <div class="stat">
                            <i class="fas fa-user-check"></i>
                            <span>${voluntarios} Voluntarios</span>
                        </div>
                        <div class="stat">
                            <i class="fas fa-clock"></i>
                            <span>${diasRestantes} días restantes</span>
                        </div>
                    </div>
                    <div class="progress-container">
                        <div class="progress-info">
                            <span>Progreso</span>
                            <span>${progreso}%</span>
                        </div>
                        <div class="progress-bar">
                            <div class="progress" style="width: ${progreso}%"></div>
                        </div>
                    </div>
                `;
                proyectosGrid.appendChild(tarjeta);
            }
        })
        .catch(function(error) {
            console.error('Error al procesar proyectos:', error);
            proyectosGrid.innerHTML = `
                <div class="error-message">
                    <i class="fas fa-exclamation-triangle"></i>
                    <span>Error al cargar proyectos. Intente más tarde.</span>
                </div>
            `;
            console.log('Error en API, cargando proyectos de ejemplo...');
            cargarProyectosEjemplo();
        });
}

// Cargar proyectos de ejemplo (sin cambios mayores)
function cargarProyectosEjemplo() {
    const proyectosGrid = document.getElementById('proyectos-grid');
     if (!proyectosGrid) return;
    proyectosGrid.innerHTML = `
        <div class="admin-project-card">
            <div class="project-header">
                <h3>Limpieza de Playas (Ejemplo)</h3>
                <span class="status high-priority">Alta Prioridad</span>
            </div>
            <div class="project-stats">
                <div class="stat">
                    <i class="fas fa-user-check"></i>
                    <span>45 Voluntarios</span>
                </div>
                <div class="stat">
                    <i class="fas fa-clock"></i>
                    <span>3 días restantes</span>
                </div>
            </div>
            <div class="progress-container">
                <div class="progress-info">
                    <span>Progreso</span>
                    <span>75%</span>
                </div>
                <div class="progress-bar">
                    <div class="progress" style="width: 75%"></div>
                </div>
            </div>
        </div>

        <div class="admin-project-card">
            <div class="project-header">
                <h3>Reforestación Urbana (Ejemplo)</h3>
                <span class="status medium-priority">Media Prioridad</span>
            </div>
            <div class="project-stats">
                <div class="stat">
                    <i class="fas fa-user-check"></i>
                    <span>32 Voluntarios</span>
                </div>
                <div class="stat">
                    <i class="fas fa-clock"></i>
                    <span>7 días restantes</span>
                </div>
            </div>
            <div class="progress-container">
                <div class="progress-info">
                    <span>Progreso</span>
                    <span>45%</span>
                </div>
                <div class="progress-bar">
                    <div class="progress" style="width: 45%"></div>
                </div>
            </div>
        </div>
    `;
}

// Actualizar fecha y hora (sin cambios mayores)
function updateDateTime() {
    try {
        const now = new Date();
        const timeElement = document.getElementById('current-time');
        const dateElement = document.getElementById('current-date');

        if (timeElement) {
            timeElement.textContent = now.toLocaleTimeString('es-ES', {
                hour: '2-digit',
                minute: '2-digit'
            });
        }

        if (dateElement) {
            dateElement.textContent = now.toLocaleDateString('es-ES', {
                day: 'numeric',
                month: 'long',
                year: 'numeric'
            });
        }
    } catch (error) {
        console.error('Error actualizando fecha/hora:', error);
    }
}

// Función para actualizar tendencias (simplificada)
function actualizarTendencias(data) {
    try {
        // Actualizar tendencias de usuarios
        if (typeof data.tendenciaUsuarios === 'number') {
            actualizarElementoTendencia(document.querySelector('.stat-card:nth-child(1) .stat-trend'), data.tendenciaUsuarios);
        }

        // Actualizar tendencias de proyectos
        if (typeof data.tendenciaProyectos === 'number') {
            actualizarElementoTendencia(document.querySelector('.stat-card:nth-child(2) .stat-trend'), data.tendenciaProyectos);
        }
         // Actualizar tendencias foros
         if (typeof data.tendenciaForos === 'number') {
             actualizarElementoTendencia(document.querySelector('.stat-card:nth-child(3) .stat-trend'), data.tendenciaForos);
         }
         // Actualizar tendencias desafíos
         if (typeof data.tendenciaDesafios === 'number') {
            actualizarElementoTendencia(document.querySelector('.stat-card:nth-child(4) .stat-trend'), data.tendenciaDesafios);
         }

    } catch (error) {
        console.error('Error al actualizar tendencias:', error);
    }
}

// Función auxiliar para actualizar un elemento de tendencia
function actualizarElementoTendencia(elemento, valor) {
     if (!elemento) return;
     const esPositivo = valor >= 0;
     elemento.className = `stat-trend ${esPositivo ? 'positive' : 'negative'}`;
     elemento.innerHTML = `
         <i class="fas fa-arrow-${esPositivo ? 'up' : 'down'}"></i>
         <span>${Math.abs(valor).toFixed(1)}% este mes</span>
     `;
}

// Función para cargar datos de ejemplo (sin cambios mayores)
function cargarDatosEstadisticasEjemplo() {
    try {
        const datosEjemplo = {
            totalUsuarios: 1234,
            proyectosActivos: 856,
            forosActivos: 2547,
            desafiosCompletados: 98,
            tendenciaUsuarios: 12.5,
            tendenciaProyectos: -8.1,
            tendenciaForos: 15.0,
            tendenciaDesafios: 2.3
        };

        const statCards = document.querySelectorAll('.stat-card');
        for(let index = 0; index < statCards.length; index++){
            const card = statCards[index];
            const numberElement = card.querySelector('.stat-number');
            if (!numberElement) continue;

            let valor = 0;
            switch(index) {
                case 0: valor = datosEjemplo.totalUsuarios; break;
                case 1: valor = datosEjemplo.proyectosActivos; break;
                case 2: valor = datosEjemplo.forosActivos; break;
                case 3: valor = datosEjemplo.desafiosCompletados; break;
            }
            card.dataset.value = valor;
            animateValue(numberElement, 0, valor, 1500);
        }

        actualizarTendencias(datosEjemplo);
    } catch (error) {
        console.error('Error al cargar datos de ejemplo:', error);
    }
}

// Cargar estadísticas principales y detalladas usando Promesas y bucle for
function cargarEstadisticas() {
     // 1. Animación inicial de números
     const statCards = document.querySelectorAll('.stat-card');
     for(let i = 0; i < statCards.length; i++){
        const card = statCards[i];
        try {
            const numberElement = card.querySelector('.stat-number');
            if (numberElement) {
                const targetValue = parseInt(card.dataset.value || '0');
                if (targetValue > 0 || numberElement.textContent === '0') {
                     animateValue(numberElement, 0, targetValue, 2000);
                }
            }
        } catch (error) {
            console.error('Error animando tarjeta estadística inicial:', error);
        }
    }

    // 2. Fetch para datos más detallados
    fetch('/api/dashboard/resumen')
        .then(function(response) {
            if (!response.ok) {
                throw new Error(`Error al cargar resumen: ${response.statusText}`);
            }
            return response.json();
        })
        .then(function(data) {
            if (!data || typeof data !== 'object') {
                throw new Error('Formato de datos inválido desde /api/dashboard/resumen');
            }
            console.log('Datos de resumen cargados:', data);

            // Actualizar números
             const usuariosCard = document.querySelector('.stat-card:nth-child(1)');
             if (usuariosCard && data.totalUsuarios !== undefined) {
                 const numberElement = usuariosCard.querySelector('.stat-number');
                 if (numberElement) animateValue(numberElement, parseInt(numberElement.textContent.replace(/,/g, '') || '0'), data.totalUsuarios, 1500);
             }

             const proyectosCard = document.querySelector('.stat-card:nth-child(2)');
             if (proyectosCard && data.proyectosActivos !== undefined) {
                 const numberElement = proyectosCard.querySelector('.stat-number');
                 if (numberElement) animateValue(numberElement, parseInt(numberElement.textContent.replace(/,/g, '') || '0'), data.proyectosActivos, 1500);
             }

             const forosCard = document.querySelector('.stat-card:nth-child(3)');
             if (forosCard && data.forosActivos !== undefined) {
                 const numberElement = forosCard.querySelector('.stat-number');
                 if (numberElement) animateValue(numberElement, parseInt(numberElement.textContent.replace(/,/g, '') || '0'), data.forosActivos, 1500);
             }

             const desafiosCard = document.querySelector('.stat-card:nth-child(4)');
             if (desafiosCard && data.desafiosCompletados !== undefined) {
                 const numberElement = desafiosCard.querySelector('.stat-number');
                 if (numberElement) animateValue(numberElement, parseInt(numberElement.textContent.replace(/,/g, '') || '0'), data.desafiosCompletados, 1500);
             }

            actualizarTendencias(data);
        })
        .catch(function(error) {
            console.error('Error al cargar estadísticas detalladas:', error);
            console.log('Usando datos iniciales de estadísticas debido a error en API.');
        });
}

// Iniciar todo cuando la página carga
document.addEventListener('DOMContentLoaded', function() {
    try {
        updateDateTime();
        setInterval(updateDateTime, 60000);

        cargarEstadisticas();
        cargarProyectosDestacados();

    } catch (error) {
        console.error('Error general en inicialización (DOMContentLoaded):', error);
        const errorContainer = document.createElement('div');
        errorContainer.className = 'global-error';
        errorContainer.innerHTML = '<i class="fas fa-exclamation-triangle"></i> Error al cargar el dashboard. Por favor, recargue la página.';
        errorContainer.style.cssText = 'color: #ff3860; padding: 15px; margin: 15px; background-color: rgba(255, 56, 96, 0.1); border-radius: 4px; text-align: center; font-weight: bold;';
        const dashboardElement = document.querySelector('.admin-dashboard') || document.body;
        dashboardElement.prepend(errorContainer);
    }
});
