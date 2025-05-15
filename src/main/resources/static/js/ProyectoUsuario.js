// Imagen por defecto en Base64 (un rectángulo gris con el icono de imagen)
const defaultImageBase64 = 'data:image/svg+xml;base64,PHN2ZyB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgeG1sbnM9Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvc3ZnIj48cmVjdCB3aWR0aD0iMjAwIiBoZWlnaHQ9IjIwMCIgZmlsbD0iI2VlZWVlZSIvPjxjaXJjbGUgY3g9IjUwIiBjeT0iNTAiIHI9IjE1IiBmaWxsPSIjYmJiIi8+PHBvbHlnb24gcG9pbnRzPSIwLDIwMCAyMDAsMTUwIDIwMCwyMDAiIGZpbGw9IiNiYmIiLz48cGF0aCBkPSJNNjAgOTBMOTAgMTUwIDE0MCAxMTAgMTgwIDE3MCIgc3Ryb2tlPSIjYmJiIiBzdHJva2Utd2lkdGg9IjEwIiBmaWxsPSJub25lIi8+PHRleHQgeD0iNDAiIHk9IjEwMCIgZm9udC1mYW1pbHk9IkFyaWFsIiBmb250LXNpemU9IjE2IiBmaWxsPSIjODg4Ij5TaW4gaW1hZ2VuPC90ZXh0Pjwvc3ZnPg==';

// --- CONFIGURACIÓN API ---
const API_PROYECTOS_URL = '/api/proyectos';
const API_DESAFIOS_URL = '/api/desafios';
const API_USUARIO_URL = '/api/usuarios/me';
let currentUserProjects = [];
let activeTab = 'projects';
let currentPage = 1;
let totalPages = 1;
let pageSize = 10;
let totalItems = 0;

// Para desafíos
let challengesCurrentPage = 1;
let challengesTotalPages = 1;
let challengesPageSize = 10;
let challengesTotalItems = 0;

// Referencias a Elementos del DOM
const projectsGrid = document.querySelector('.projects-grid');
const challengesGrid = document.querySelector('.challenges-grid');
const projectsFilters = document.getElementById('projects-filters');
const challengesFilters = document.getElementById('challenges-filters');
// const filterButtons = document.querySelectorAll('.filter-btn'); // Se obtienen dentro de los event listeners
const tabButtons = document.querySelectorAll('.tab-btn');
const challengeTemplate = document.getElementById('challenge-card-template');
const userSearchInput = document.getElementById('userSearchInput');

// Elementos de paginación para proyectos
const paginationContainer = document.getElementById('projects-pagination');
const currentPageElement = document.getElementById('current-page');
const totalPagesElement = document.getElementById('total-pages');
const totalItemsElement = document.getElementById('total-items');
const prevPageButton = document.getElementById('prev-page');
const nextPageButton = document.getElementById('next-page');
const pageNumbersContainer = document.getElementById('page-numbers');

// Elementos de paginación para desafíos
const challengesPaginationContainer = document.getElementById('challenges-pagination');
const challengesCurrentPageElement = document.getElementById('challenges-current-page');
const challengesTotalPagesElement = document.getElementById('challenges-total-pages');
const challengesTotalItemsElement = document.getElementById('challenges-total-items');
const challengesPrevPageButton = document.getElementById('challenges-prev-page');
const challengesNextPageButton = document.getElementById('challenges-next-page');
const challengesPageNumbersContainer = document.getElementById('challenges-page-numbers');

let userData = null; // Para almacenar datos del usuario actual

// --- MANEJO DE PESTAÑAS ---
function switchTab(tab) {
    activeTab = tab;

    tabButtons.forEach(btn => {
        btn.classList.toggle('active', btn.dataset.tab === tab);
    });

    const isProjectsTab = tab === 'projects';
    projectsGrid.style.display = isProjectsTab ? 'grid' : 'none';
    challengesGrid.style.display = isProjectsTab ? 'none' : 'grid';
    projectsFilters.style.display = isProjectsTab ? 'flex' : 'none';
    challengesFilters.style.display = isProjectsTab ? 'none' : 'flex';
    paginationContainer.style.display = isProjectsTab ? 'flex' : 'none';
    challengesPaginationContainer.style.display = isProjectsTab ? 'none' : 'flex';
    userSearchInput.placeholder = isProjectsTab ? 'Buscar proyectos...' : 'Buscar desafíos...';

    if (tab === 'challenges' && challengesGrid.childElementCount === 0) {
        loadChallengesPaginated(1);
    } else if (tab === 'projects') {
        // Podrías recargar proyectos o aplicar filtros si es necesario al cambiar a la pestaña
        // loadDataAndRender(currentPage); // Opcional: recargar si la data puede haber cambiado
    }
    // Resetear filtros al cambiar de pestaña para evitar confusión
    resetActiveFilters();
    applyFilter(); // Aplicar el filtro 'all' por defecto
}

function resetActiveFilters() {
    const activeFilterSection = activeTab === 'projects' ? projectsFilters : challengesFilters;
    activeFilterSection.querySelectorAll('.filter-btn').forEach(btn => {
        btn.classList.remove('active');
        if (btn.dataset.filter === 'all') {
            btn.classList.add('active');
        }
    });
}


// --- RENDERIZACIÓN DE TARJETAS DE PROYECTO ---
function renderProjectCard(project, isParticipating) {
    const card = document.createElement('div');
    card.classList.add('project-user-card');
    card.dataset.projectId = project.id;
    card.onclick = () => openProjectDetails(project.id); // Abrir modal al hacer clic


    let category = isParticipating ? 'participating' : 'available';
    if (project.estado === 'EXPIRADO' || (project.estado === 'COMPLETO' && !isParticipating)) {
        category = 'expired';
    }
    card.dataset.category = category;
    // card.classList.add(category); // La clase se añade después por el filtro

    let statusText = 'Disponible';
    if (isParticipating) statusText = 'Participando';
    if (project.estado === 'EXPIRADO') statusText = "Finalizado";
    else if (project.estado === 'COMPLETO' && !isParticipating) statusText = "Completo";
    else if (project.estado === 'CANCELADO') statusText = "Cancelado";
    else if (project.estado === 'COMPLETADO') statusText = "Completado"; // Para proyectos que el usuario ya completó como tarea


    let daysRemaining = 'N/A';
    if (project.fechaExpiracion) {
        const expDate = new Date(project.fechaExpiracion);
        const now = new Date();
        const diffTime = expDate.getTime() - now.getTime();
        const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
        daysRemaining = diffTime > 0 ? `${diffDays} día${diffDays !== 1 ? 's' : ''} restante${diffDays !== 1 ? 's' : ''}` : 'Expirado';
    } else {
        daysRemaining = 'Sin fecha límite';
    }

    const participantesInfo = project.participantesActuales !== null ?
        `${project.participantesActuales}${project.limiteParticipantes ? ' / ' + project.limiteParticipantes : ''}` :
        '0'; // Mostrar 0 si es null

    const imagePath = project.imagenUrl ? `/static/Proyectos/${project.imagenUrl}` : defaultImageBase64;

    let buttonHtml = '';
    if (isParticipating) {
        buttonHtml = `<button class="leave-project-btn" data-project-id="${project.id}" onclick="event.stopPropagation(); leaveProject('${project.id}');"><i class="fas fa-sign-out-alt"></i> Abandonar Proyecto</button>`;
    } else if (project.estado === 'ACTIVO') {
        if (project.haAlcanzadoLimiteParticipantes || (project.participantesActuales >= project.limiteParticipantes && project.limiteParticipantes > 0) ) {
            statusText = "Completo"; // Sobreescribir statusText si está lleno
            buttonHtml = `<div class="project-status-message"><i class="fas fa-users"></i> Completo</div>`;
            card.dataset.category = 'expired'; // Considerarlo 'expired' para filtrado si está lleno y no participo
        } else {
            buttonHtml = `<button class="join-project-btn" data-project-id="${project.id}" onclick="event.stopPropagation(); joinProject('${project.id}');"><i class="fas fa-hand-holding-heart"></i> Participar</button>`;
        }
    } else if (project.estado === 'COMPLETO') {
        statusText = "Completo";
        buttonHtml = `<div class="project-status-message"><i class="fas fa-check-circle"></i> Proyecto Completo</div>`;
        card.dataset.category = 'expired';
    } else if (project.estado === 'EXPIRADO') {
        statusText = "Finalizado";
        buttonHtml = `<div class="project-status-message"><i class="fas fa-clock"></i> Proyecto Finalizado</div>`;
    } else { // CANCELADO u otros
        statusText = project.estado ? project.estado.charAt(0).toUpperCase() + project.estado.slice(1).toLowerCase() : "No disponible";
        buttonHtml = `<div class="project-status-message"><i class="fas fa-info-circle"></i> Proyecto ${statusText}</div>`;
        card.dataset.category = 'expired'; // Otros estados no participables también como 'expired' para filtro
    }
    // Actualizar la clase de estado visual
    const statusClass = project.estado ? project.estado.toLowerCase() : 'disponible';


    card.innerHTML = `
        <div class="project-banner">
            <div class="project-status ${statusClass}">${statusText}</div>
            <img class="project-image" src="${imagePath}" alt="${project.nombre || 'Proyecto sin nombre'}" onerror="this.src='${defaultImageBase64}'">
            <svg viewBox="0 0 200 100" preserveAspectRatio="none">
                <path d="M0,70 C50,${isParticipating ? 100 : 40} 150,${isParticipating ? 40 : 100} 200,70 L200,100 L0,100 Z" fill="rgba(255,255,255,0.8)" stroke="rgba(0,0,0,0.1)" stroke-width="0.5"/>
            </svg>
        </div>
        <div class="project-content">
             <h3>${project.nombre || 'Proyecto sin Título'}</h3>
            <p>${project.descripcion || 'Este proyecto no tiene una descripción detallada.'}</p>
            <div class="project-details">
                <span><i class="fas fa-users"></i> ${participantesInfo} Voluntarios</span>
                <span><i class="fas fa-calendar-alt"></i> ${daysRemaining}</span>
                <span><i class="fas fa-info-circle"></i> Estado: ${project.estado || 'ACTIVO'}</span>
            </div>
            <div class="project-actions-container">
                ${buttonHtml}
            </div>
        </div>
    `;
    projectsGrid.appendChild(card);
}


// --- RENDERIZACIÓN DE TARJETAS DE DESAFÍO ---
function renderChallengeCard(challenge) {
    const templateNode = challengeTemplate.content.cloneNode(true);
    const card = templateNode.querySelector('.challenge-card');

    let status = 'available';
    let statusText = 'Disponible';
    let buttonHtml = '';
    let progressPercent = challenge.participacion ? (challenge.participacion.progreso || 0) : 0;

    if (challenge.participacion) {
        if (challenge.participacion.completado) {
            status = 'completed';
            statusText = 'Completado';
            progressPercent = 100;
            buttonHtml = `<button class="view-details-btn" data-challenge-id="${challenge.id}"><i class="fas fa-eye"></i> Ver Detalles</button>`;
        } else {
            status = 'active'; // En progreso
            statusText = 'En Progreso';
            buttonHtml = `<button class="complete-challenge-btn" data-challenge-id="${challenge.id}"><i class="fas fa-check-circle"></i> Marcar Completado</button>`;
        }
    } else { // No participa
        buttonHtml = `<button class="participate-challenge-btn" data-challenge-id="${challenge.id}"><i class="fas fa-play-circle"></i> Participar</button>`;
    }

    card.classList.add(status);
    card.dataset.challengeId = challenge.id;
    card.dataset.category = status;

    card.querySelector('.challenge-status').textContent = statusText;
    card.querySelector('.challenge-title').textContent = challenge.nombre || "Desafío sin título";
    card.querySelector('.challenge-description').textContent = challenge.descripcion || "Sin descripción.";

    card.querySelector('.challenge-project').innerHTML = `<i class="fas fa-project-diagram"></i> ${challenge.proyectoNombre || 'General'}`;

    let fechaInfo = 'Sin fecha límite';
    if (challenge.fechaFin) {
        const fechaFin = new Date(challenge.fechaFin);
        const now = new Date();
        if (fechaFin > now) {
            const diffTime = fechaFin.getTime() - now.getTime();
            const diffDays = Math.ceil(diffTime / (1000 * 60 * 60 * 24));
            fechaInfo = `${diffDays} día${diffDays !== 1 ? 's' : ''} restante${diffDays !== 1 ? 's' : ''}`;
        } else {
            fechaInfo = 'Expirado';
            if(status === 'available' || status === 'active') card.dataset.category = 'expired'; // Si expira y no está completado
        }
    }
    card.querySelector('.challenge-date').innerHTML = `<i class="far fa-calendar-alt"></i> ${fechaInfo}`;
    card.querySelector('.challenge-reward').innerHTML = `<i class="fas fa-gem"></i> ${challenge.puntosRecompensa || 0} pts`;
    card.querySelector('.progress').style.width = `${progressPercent}%`;
    card.querySelector('.progress-text').textContent = `${progressPercent}%`;
    card.querySelector('.challenge-action').innerHTML = buttonHtml;

    challengesGrid.appendChild(card);
    setTimeout(() => card.classList.add('show'), 50); // Animación
}


// --- CARGA DE DATOS (USUARIO, PROYECTOS, DESAFÍOS) ---
async function fetchUserData() {
    try {
        const response = await fetch(API_USUARIO_URL);
        if (!response.ok) {
            if (response.status === 401 || response.status === 403) throw new Error('No autorizado. Inicia sesión.');
            throw new Error(`Error ${response.status} obteniendo datos de usuario`);
        }
        userData = await response.json();
        currentUserProjects = userData.proyectosParticipadosIds || (userData.proyectosParticipados ? userData.proyectosParticipados.map(p => p.proyectoId || p.id) : []) || [];
        saveUserProjectsToLocalStorage(); // Guardar/actualizar IDs de proyectos del usuario
    } catch (error) {
        console.error('Error fetching user data:', error);
        projectsGrid.innerHTML = `<p class="error-message">Error al cargar datos del usuario: ${error.message}. <a href="/login">Inicia sesión</a>.</p>`;
        challengesGrid.innerHTML = `<p class="error-message">Error al cargar datos del usuario: ${error.message}. <a href="/login">Inicia sesión</a>.</p>`;
        throw error; // Re-lanzar para detener la carga si los datos del usuario son cruciales
    }
}

async function loadDataAndRender(page = 1) {
    if (!userData) { // Asegurarse que los datos del usuario estén cargados
        try {
            await fetchUserData();
        } catch (error) {
            return; // Detener si no se pueden cargar datos del usuario
        }
    }

    projectsGrid.innerHTML = '<div class="loading-message"><i class="fas fa-spinner fa-spin"></i> Cargando proyectos...</div>';
    try {
        const projectsResponse = await fetch(`${API_PROYECTOS_URL}?page=${page - 1}&size=${pageSize}`);
        if (!projectsResponse.ok) throw new Error(`Error ${projectsResponse.status} obteniendo proyectos`);

        const responseData = await projectsResponse.json();
        let projects;

        // Adaptar a diferentes estructuras de paginación
        if (Array.isArray(responseData)) { // Respuesta es un array directo
            projects = responseData;
            totalItems = projects.length;
            totalPages = 1;
            currentPage = 1;
        } else if (responseData && responseData.content && Array.isArray(responseData.content)) { // Spring Pageable
            projects = responseData.content;
            totalItems = responseData.totalElements;
            totalPages = responseData.totalPages;
            currentPage = responseData.number + 1;
            pageSize = responseData.size;
        } else { // Formato desconocido o sin paginación
            console.warn('Formato de respuesta de proyectos no reconocido o sin paginación:', responseData);
            projects = [];
            totalItems = 0;
            totalPages = 1;
            currentPage = 1;
        }

        // Priorizar headers si existen (común en algunas APIs REST)
        totalItems = parseInt(projectsResponse.headers.get('X-Total-Count') || totalItems);
        totalPages = parseInt(projectsResponse.headers.get('X-Total-Pages') || totalPages);
        currentPage = parseInt(projectsResponse.headers.get('X-Current-Page') || (currentPage -1)) + 1;


        updatePaginationControls('projects');
        renderAllProjects(projects);

    } catch (error) {
        console.error('Error al cargar proyectos:', error);
        projectsGrid.innerHTML = `<p class="error-message">Error al cargar proyectos: ${error.message}. Intenta recargar.</p>`;
    }
}

async function loadChallengesPaginated(page = 1) {
     if (!userData) { // Asegurarse que los datos del usuario estén cargados
        try {
            await fetchUserData();
        } catch (error) {
            return;
        }
    }
    challengesGrid.innerHTML = '<div class="loading-message"><i class="fas fa-spinner fa-spin"></i> Cargando desafíos...</div>';
    try {
        const participacionesResponse = await fetch(`${API_DESAFIOS_URL}/mis-participaciones`);
        let participacionMap = new Map();
        if (participacionesResponse.ok) {
            const participacionesData = await participacionesResponse.json();
            const participaciones = Array.isArray(participacionesData) ? participacionesData : (participacionesData.content || []);
            participaciones.forEach(p => {
                if (p && p.desafioId) participacionMap.set(p.desafioId, p);
            });
        } else {
            console.warn(`No se pudieron cargar las participaciones de desafíos: ${participacionesResponse.status}`);
        }

        // Intentar con varios endpoints comunes para desafíos paginados
        const challengeEndpoints = [
            `${API_DESAFIOS_URL}/paginados?page=${page-1}&size=${challengesPageSize}`,
            `${API_DESAFIOS_URL}/publicos?page=${page-1}&size=${challengesPageSize}`,
            `${API_DESAFIOS_URL}?page=${page-1}&size=${challengesPageSize}`
        ];
        let desafiosResponse;
        for (const endpoint of challengeEndpoints) {
            try {
                const response = await fetch(endpoint);
                if (response.ok) {
                    desafiosResponse = response;
                    break;
                }
            } catch (err) { /* Ignorar y probar el siguiente */ }
        }

        if (!desafiosResponse) throw new Error("No se pudo conectar a ningún endpoint de desafíos paginados.");

        const contentType = desafiosResponse.headers.get("content-type");
        if (!contentType || !contentType.includes("application/json")) {
            throw new Error("La API de desafíos devolvió un formato inesperado (no JSON).");
        }
        const desafiosData = await desafiosResponse.json();
        let desafios;

        if (Array.isArray(desafiosData)) {
            desafios = desafiosData;
            challengesTotalItems = desafios.length;
            challengesTotalPages = 1;
            challengesCurrentPage = 1;
        } else if (desafiosData && desafiosData.content && Array.isArray(desafiosData.content)) {
            desafios = desafiosData.content;
            challengesTotalItems = desafiosData.totalElements;
            challengesTotalPages = desafiosData.totalPages;
            challengesCurrentPage = desafiosData.number + 1;
            challengesPageSize = desafiosData.size;
        } else {
            console.warn('Formato de respuesta de desafíos no reconocido:', desafiosData);
            desafios = [];
            challengesTotalItems = 0;
            challengesTotalPages = 1;
            challengesCurrentPage = 1;
        }

        // Priorizar headers si existen
        challengesTotalItems = parseInt(desafiosResponse.headers.get('X-Total-Count') || challengesTotalItems);
        challengesTotalPages = parseInt(desafiosResponse.headers.get('X-Total-Pages') || challengesTotalPages);
        challengesCurrentPage = parseInt(desafiosResponse.headers.get('X-Current-Page') || (challengesCurrentPage -1)) + 1;


        updatePaginationControls('challenges');
        challengesGrid.innerHTML = ''; // Limpiar antes de renderizar
        if (desafios.length === 0) {
            challengesGrid.innerHTML = '<p class="info-message">No hay desafíos disponibles en esta página.</p>';
            return;
        }

        const desafiosProcesados = desafios.map(d => ({
            ...d,
            participacion: participacionMap.get(d.id),
            proyectoNombre: d.proyectoNombre || (d.proyectoId ? `Proyecto ${d.proyectoId}` : 'General')
        })).filter(d => d.id); // Asegurar que el desafío tenga un ID

        desafiosProcesados.forEach(renderChallengeCard);
        applyFilter(); // Aplicar filtro actual después de renderizar

    } catch (error) {
        console.error('Error al cargar desafíos:', error);
        let errorMsgHtml = `<p class="error-message">Error al cargar desafíos: ${error.message}.`;
        if (error.message.includes("sesión") || error.message.includes("autenticación") || error.message.includes("No autorizado")) {
            errorMsgHtml += ` <a href="/login">Inicia sesión de nuevo</a>.`;
        } else {
            errorMsgHtml += ` <button class="retry-btn" onclick="loadChallengesPaginated(${challengesCurrentPage})">Reintentar</button>`;
        }
        errorMsgHtml += `</p>`;
        challengesGrid.innerHTML = errorMsgHtml;
    }
}


// --- PAGINACIÓN ---
function updatePaginationControls(type) {
    const isProjects = type === 'projects';
    const currentPg = isProjects ? currentPage : challengesCurrentPage;
    const totalPgs = isProjects ? totalPages : challengesTotalPages;
    const totalIts = isProjects ? totalItems : challengesTotalItems;
    const currentPageEl = isProjects ? currentPageElement : challengesCurrentPageElement;
    const totalPagesEl = isProjects ? totalPagesElement : challengesTotalPagesElement;
    const totalItemsEl = isProjects ? totalItemsElement : challengesTotalItemsElement;
    const prevBtn = isProjects ? prevPageButton : challengesPrevPageButton;
    const nextBtn = isProjects ? nextPageButton : challengesNextPageButton;
    const numbersContainer = isProjects ? pageNumbersContainer : challengesPageNumbersContainer;
    const paginationCtrlContainer = isProjects ? paginationContainer : challengesPaginationContainer;
    const loadFn = isProjects ? (page => searchProjectsPaginated(userSearchInput.value, page)) : (page => searchChallengesPaginated(userSearchInput.value, page));


    currentPageEl.textContent = currentPg;
    totalPagesEl.textContent = totalPgs || 1; // Evitar NaN si totalPages es 0
    totalItemsEl.textContent = new Intl.NumberFormat().format(totalIts);

    const paginationStatusEl = document.getElementById(isProjects ? 'pagination-status' : 'challenges-pagination-status');
    if (paginationStatusEl) {
        paginationStatusEl.innerHTML = `Mostrando página <strong>${currentPg}</strong> de <strong>${totalPgs || 1}</strong>`;
    }

    prevBtn.disabled = currentPg <= 1;
    nextBtn.disabled = currentPg >= totalPgs;

    generatePageNumbers(numbersContainer, currentPg, totalPgs, loadFn);
    paginationCtrlContainer.style.display = (totalPgs <= 1 && totalIts <= (isProjects ? pageSize : challengesPageSize)) ? 'none' : 'flex';
}

function generatePageNumbers(container, currentPg, totalPgs, clickHandler) {
    container.innerHTML = '';
    if (totalPgs <= 1) return;

    const maxPagesToShow = 5;
    const pages = [];

    // Siempre añadir primera página
    pages.push(1);

    // Calcular inicio y fin del rango medio
    let startPage = Math.max(2, currentPg - Math.floor((maxPagesToShow - 3) / 2));
    let endPage = Math.min(totalPgs - 1, currentPg + Math.floor((maxPagesToShow - 2) / 2));

    // Ajustar si el rango es muy pequeño o se solapa
    if (currentPg <= maxPagesToShow - Math.floor(maxPagesToShow/2) ) { // Cerca del inicio
        endPage = Math.min(totalPgs - 1, maxPagesToShow -1);
    }
    if (currentPg > totalPgs - (maxPagesToShow - Math.floor(maxPagesToShow/2)) ) { // Cerca del final
        startPage = Math.max(2, totalPgs - (maxPagesToShow-2));
    }


    if (startPage > 2) {
        pages.push('...');
    }

    for (let i = startPage; i <= endPage; i++) {
        if (i > 1 && i < totalPgs) { // No añadir 1 o totalPgs si ya están
            pages.push(i);
        }
    }

    if (endPage < totalPgs - 1) {
        pages.push('...');
    }

    // Siempre añadir última página si es diferente de la primera
    if (totalPgs > 1) {
        pages.push(totalPgs);
    }

    // Eliminar duplicados por si acaso (ej. si totalPgs es muy pequeño)
    const uniquePages = [...new Set(pages)];


    uniquePages.forEach(pageNum => {
        const pageBtn = document.createElement('div');
        if (pageNum === '...') {
            pageBtn.className = 'page-ellipsis';
            pageBtn.textContent = '...';
        } else {
            pageBtn.className = 'page-number' + (pageNum === currentPg ? ' active' : '');
            pageBtn.textContent = pageNum;
            pageBtn.addEventListener('click', () => clickHandler(pageNum));
        }
        container.appendChild(pageBtn);
    });
}


// --- BÚSQUEDA ---
async function searchItems() {
    const query = userSearchInput.value.trim();
    if (activeTab === 'projects') {
        await searchProjectsPaginated(query, 1); // Reiniciar a página 1 en nueva búsqueda
    } else {
        await searchChallengesPaginated(query, 1); // Reiniciar a página 1
    }
}

async function searchProjectsPaginated(query, page = 1) {
    projectsGrid.innerHTML = '<div class="loading-message"><i class="fas fa-spinner fa-spin"></i> Buscando proyectos...</div>';
    try {
        const url = query ?
            `${API_PROYECTOS_URL}/buscar?nombre=${encodeURIComponent(query)}&page=${page-1}&size=${pageSize}` :
            `${API_PROYECTOS_URL}?page=${page-1}&size=${pageSize}`;

        const response = await fetch(url);
        if (!response.ok) throw new Error(`Error HTTP: ${response.status}`);

        const responseData = await response.json();
        let projects;

        if (Array.isArray(responseData)) {
            projects = responseData;
            totalItems = projects.length;
            totalPages = 1; // Asumir una sola página si no hay info de paginación
            currentPage = 1;
        } else if (responseData.content && Array.isArray(responseData.content)) {
            projects = responseData.content;
            totalItems = responseData.totalElements;
            totalPages = responseData.totalPages;
            currentPage = responseData.number + 1;
            pageSize = responseData.size;
        } else {
             console.warn('Formato de búsqueda de proyectos no reconocido:', responseData);
            projects = [];
            totalItems = 0; totalPages = 1; currentPage = 1;
        }
        // Priorizar headers si existen
        totalItems = parseInt(response.headers.get('X-Total-Count') || totalItems);
        totalPages = parseInt(response.headers.get('X-Total-Pages') || totalPages);
        currentPage = parseInt(response.headers.get('X-Current-Page') || (currentPage -1)) + 1;


        updatePaginationControls('projects');
        renderAllProjects(projects);
        if (projects.length === 0 && query) {
            projectsGrid.innerHTML = `<p class="info-message">No se encontraron proyectos que coincidan con "${query}".</p>`;
        } else if (projects.length === 0) {
            projectsGrid.innerHTML = `<p class="info-message">No hay proyectos disponibles.</p>`;
        }
    } catch (error) {
        console.error('Error al buscar proyectos:', error);
        projectsGrid.innerHTML = `<p class="error-message">Error al buscar proyectos: ${error.message}.</p>`;
    }
}

async function searchChallengesPaginated(query, page = 1) {
    challengesGrid.innerHTML = '<div class="loading-message"><i class="fas fa-spinner fa-spin"></i> Buscando desafíos...</div>';
    try {
        let url = query ?
            `${API_DESAFIOS_URL}/buscar?nombre=${encodeURIComponent(query)}&page=${page-1}&size=${challengesPageSize}` :
            `${API_DESAFIOS_URL}/paginados?page=${page-1}&size=${challengesPageSize}`; // Asumiendo que /paginados es el default sin query

        const response = await fetch(url);
        if (!response.ok) {
             // Fallback si /buscar no existe o falla y hay query: cargar todos y filtrar en cliente (menos ideal para paginación real)
            if(query) {
                console.warn(`Endpoint de búsqueda de desafíos falló o no existe. Intentando filtrar en cliente (página ${page}).`);
                await loadChallengesPaginated(page); // Carga la página actual de todos los desafíos
                // Ahora se debe filtrar en cliente, lo cual es complejo si la paginación es del backend
                // Esta parte necesitaría una lógica más robusta o asumir que el backend SIEMPRE soporta búsqueda.
                // Por simplicidad, mostraremos un mensaje o los resultados no filtrados de la página.
                const cards = challengesGrid.querySelectorAll('.challenge-card');
                const queryLower = query.toLowerCase();
                let visibleCount = 0;
                cards.forEach(card => {
                    const title = card.querySelector('.challenge-title').textContent.toLowerCase();
                    const desc = card.querySelector('.challenge-description').textContent.toLowerCase();
                    if (title.includes(queryLower) || desc.includes(queryLower)) {
                        card.style.display = 'flex'; // O la display original
                        visibleCount++;
                    } else {
                        card.style.display = 'none';
                    }
                });
                if (visibleCount === 0) {
                     challengesGrid.innerHTML = `<p class="info-message">No se encontraron desafíos que coincidan con "${query}" en esta página.</p>`;
                }
                return;
            }
            throw new Error(`Error ${response.status} obteniendo desafíos`);
        }

        const responseData = await response.json();
        let desafios;

        if (Array.isArray(responseData)) {
            desafios = responseData;
            challengesTotalItems = desafios.length;
            challengesTotalPages = 1;
            challengesCurrentPage = 1;
        } else if (responseData.content && Array.isArray(responseData.content)) {
            desafios = responseData.content;
            challengesTotalItems = responseData.totalElements;
            challengesTotalPages = responseData.totalPages;
            challengesCurrentPage = responseData.number + 1;
            challengesPageSize = responseData.size;
        } else {
            console.warn('Formato de búsqueda de desafíos no reconocido:', responseData);
            desafios = [];
            challengesTotalItems = 0; challengesTotalPages = 1; challengesCurrentPage = 1;
        }
        // Priorizar headers si existen
        challengesTotalItems = parseInt(response.headers.get('X-Total-Count') || challengesTotalItems);
        challengesTotalPages = parseInt(response.headers.get('X-Total-Pages') || challengesTotalPages);
        challengesCurrentPage = parseInt(response.headers.get('X-Current-Page') || (challengesCurrentPage-1)) + 1;

        const participacionesResponse = await fetch(`${API_DESAFIOS_URL}/mis-participaciones`);
        let participacionMap = new Map();
        if (participacionesResponse.ok) {
            const participacionesData = await participacionesResponse.json();
            const participaciones = Array.isArray(participacionesData) ? participacionesData : (participacionesData.content || []);
            participaciones.forEach(p => {
                if (p && p.desafioId) participacionMap.set(p.desafioId, p);
            });
        }


        updatePaginationControls('challenges');
        challengesGrid.innerHTML = '';
         if (desafios.length === 0 && query) {
            challengesGrid.innerHTML = `<p class="info-message">No se encontraron desafíos que coincidan con "${query}".</p>`;
        } else if (desafios.length === 0) {
            challengesGrid.innerHTML = `<p class="info-message">No hay desafíos disponibles.</p>`;
        } else {
            const desafiosProcesados = desafios.map(d => ({ ...d, participacion: participacionMap.get(d.id) })).filter(d=>d.id);
            desafiosProcesados.forEach(renderChallengeCard);
        }
        applyFilter();

    } catch (error) {
        console.error('Error al buscar desafíos:', error);
        challengesGrid.innerHTML = `<p class="error-message">Error al buscar desafíos: ${error.message}.</p>`;
    }
}

// --- FILTRADO ---
function applyFilter() {
    const activeFilterSection = activeTab === 'projects' ? projectsFilters : challengesFilters;
    const activeFilterButton = activeFilterSection.querySelector('.filter-btn.active');
    const filter = activeFilterButton ? activeFilterButton.dataset.filter : 'all';

    const grid = activeTab === 'projects' ? projectsGrid : challengesGrid;
    const cards = grid.querySelectorAll(activeTab === 'projects' ? '.project-user-card' : '.challenge-card');
    let visibleCount = 0;

    cards.forEach(card => {
        const cardCategory = card.dataset.category;
        const matchesFilter = filter === 'all' || cardCategory === filter;

        // Considerar la búsqueda actual
        const query = userSearchInput.value.trim().toLowerCase();
        let matchesSearch = true;
        if (query) {
            const title = card.querySelector(activeTab === 'projects' ? 'h3' : '.challenge-title').textContent.toLowerCase();
            const description = card.querySelector(activeTab === 'projects' ? 'p' : '.challenge-description').textContent.toLowerCase();
            matchesSearch = title.includes(query) || description.includes(query);
        }


        if (matchesFilter && matchesSearch) {
            card.style.display = ''; // O 'flex', 'grid' según el layout de la tarjeta
            card.classList.add('show-filtered'); // Para animaciones de entrada/salida si se desea
            visibleCount++;
        } else {
            card.style.display = 'none';
            card.classList.remove('show-filtered');
        }
    });
     if (visibleCount === 0 && cards.length > 0) { // Solo mostrar si había tarjetas pero ninguna coincide
        const message = query ? `No hay ${activeTab} que coincidan con tu filtro y búsqueda.` : `No hay ${activeTab} que coincidan con el filtro "${filter}".`;
        grid.innerHTML = `<p class="info-message">${message}</p>`;
    } else if (cards.length === 0 && grid.innerHTML.includes('loading-message')) {
        // No hacer nada si todavía está cargando
    } else if (cards.length === 0) { // Si no hay tarjetas en absoluto (ej. carga inicial sin resultados)
        const message = query ? `No se encontraron ${activeTab} para "${query}".` : `No hay ${activeTab} disponibles.`;
        grid.innerHTML = `<p class="info-message">${message}</p>`;
    }
}


// --- ACCIONES DE PROYECTO Y DESAFÍO ---
async function joinProject(projectId) {
    if (!userData) { showNotification('error', 'Debes iniciar sesión para unirte.'); return; }
    try {
        const yaParticipo = currentUserProjects.includes(projectId) || await verificarParticipacionEnProyecto(projectId);
        if (yaParticipo) {
            showNotification('info', 'Ya estás participando en este proyecto.');
            return;
        }
        // Deshabilitar botones temporalmente
        const joinBtn = document.querySelector(`.project-user-card[data-project-id="${projectId}"] .join-project-btn`);
        if(joinBtn) joinBtn.disabled = true;


        const response = await fetch(`${API_PROYECTOS_URL}/${projectId}/unirse?rol=VOLUNTARIO`, { method: 'POST' });
        if (!response.ok) {
            const errorData = await response.json().catch(() => ({ message: `Error ${response.status}` }));
             if (response.status === 409) { // Conflicto, ej. proyecto lleno
                showNotification('warning', errorData.message || 'El proyecto está completo o ya no admite participantes.');
                await loadDataAndRender(currentPage); // Recargar para actualizar estado
            } else {
                throw new Error(errorData.message || `Error al unirse: ${response.statusText}`);
            }
            if(joinBtn) joinBtn.disabled = false;
            return;
        }

        const result = await response.json().catch(() => ({ mensaje: 'Te has unido al proyecto.'}));
        showNotification('success', result.mensaje);
        currentUserProjects.push(projectId);
        saveUserProjectsToLocalStorage();
        // Actualizar UI para esta tarjeta específica o recargar todo
        // renderProjectCardContent(cardElement, projectData, true); // Necesitarías la tarjeta y los datos
        await loadDataAndRender(currentPage); // Manera más simple de asegurar consistencia

    } catch (error) {
        showNotification('error', `Error al unirse al proyecto: ${error.message}`);
        console.error('Error joining project:', error);
        const joinBtn = document.querySelector(`.project-user-card[data-project-id="${projectId}"] .join-project-btn`);
        if(joinBtn) joinBtn.disabled = false;
    }
}

async function leaveProject(projectId) {
    if (!userData) { showNotification('error', 'Debes iniciar sesión.'); return; }
    if (!confirm('¿Seguro que quieres abandonar este proyecto?')) return;

    try {
        const leaveBtn = document.querySelector(`.project-user-card[data-project-id="${projectId}"] .leave-project-btn`);
        if(leaveBtn) leaveBtn.disabled = true;

        const response = await fetch(`${API_PROYECTOS_URL}/${projectId}/abandonar`, { method: 'DELETE' });
        if (!response.ok) {
            const errorData = await response.json().catch(() => ({ message: `Error ${response.status}` }));
            // Si el error es 404 (no encontrado) o 409 (no participaba), igual actualiza localmente
            if (response.status === 404 || response.status === 409) {
                 showNotification('info', errorData.message || 'Ya no participabas en este proyecto o no se encontró.');
            } else {
                throw new Error(errorData.message || `Error al abandonar: ${response.statusText}`);
            }
        } else {
            const result = await response.json().catch(() => ({ mensaje: 'Has abandonado el proyecto.'}));
            showNotification('success', result.mensaje);
        }

        currentUserProjects = currentUserProjects.filter(id => id !== projectId);
        saveUserProjectsToLocalStorage();
        await loadDataAndRender(currentPage);

    } catch (error) {
        showNotification('error', `Error al abandonar el proyecto: ${error.message}`);
        console.error('Error leaving project:', error);
        const leaveBtn = document.querySelector(`.project-user-card[data-project-id="${projectId}"] .leave-project-btn`);
        if(leaveBtn) leaveBtn.disabled = false;
    }
}


async function participateInChallenge(challengeId) {
    if (!userData) { showNotification('error', 'Debes iniciar sesión.'); return; }
    try {
        const btn = document.querySelector(`.challenge-card[data-challenge-id="${challengeId}"] .participate-challenge-btn`);
        if(btn) btn.disabled = true;

        const response = await fetch(`${API_DESAFIOS_URL}/${challengeId}/participar`, { method: 'POST' });
        if (!response.ok) {
            const errorData = await response.json().catch(() => ({ message: `Error ${response.status}` }));
            throw new Error(errorData.message || `Error al participar: ${response.statusText}`);
        }
        const result = await response.json().catch(() => ({ message: 'Ahora participas en el desafío.' }));
        showNotification('success', result.mensaje || '¡Ahora participas en el desafío!');
        await loadChallengesPaginated(challengesCurrentPage); // Recargar para reflejar participación
    } catch (error) {
        showNotification('error', `Error al participar en el desafío: ${error.message}`);
        console.error('Error participating in challenge:', error);
        const btn = document.querySelector(`.challenge-card[data-challenge-id="${challengeId}"] .participate-challenge-btn`);
        if(btn) btn.disabled = false;
    }
}

async function completeChallenge(challengeId) {
    if (!userData) { showNotification('error', 'Debes iniciar sesión.'); return; }
    try {
        const btn = document.querySelector(`.challenge-card[data-challenge-id="${challengeId}"] .complete-challenge-btn`);
        if(btn) {
            btn.disabled = true;
            btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Completando...';
        }

        const response = await fetch(`${API_DESAFIOS_URL}/${challengeId}/completar`, { method: 'POST' });
        if (!response.ok) {
             const errorData = await response.json().catch(() => ({ message: `Error ${response.status}` }));
            // Manejar si el servidor devuelve HTML (ej. por redirección a login)
            if (response.headers.get('content-type')?.includes('text/html')) {
                 throw new Error('Tu sesión podría haber expirado. Intenta recargar la página.');
            }
            throw new Error(errorData.message || `Error al completar: ${response.statusText}`);
        }
        const result = await response.json().catch(() => ({ message: 'Desafío completado.' }));
        showNotification('success', result.mensaje || '¡Desafío completado con éxito!');
        await loadChallengesPaginated(challengesCurrentPage); // Recargar
    } catch (error) {
        showNotification('error', `No se pudo completar el desafío: ${error.message}`);
        console.error('Error completing challenge:', error);
        const btn = document.querySelector(`.challenge-card[data-challenge-id="${challengeId}"] .complete-challenge-btn`);
        if(btn) { // Restaurar botón si aún existe
            btn.disabled = false;
            btn.innerHTML = '<i class="fas fa-check-circle"></i> Marcar Completado';
        }
    }
}

// --- FUNCIONES DEL MODAL DE DETALLES DEL PROYECTO ---
const projectDetailModal = document.getElementById('projectDetailModal');
const modalProjectTitle = document.getElementById('modal-project-title');
const modalProjectImage = document.getElementById('modal-project-image');
const modalProjectDescription = document.getElementById('modal-project-description');
const modalProjectStatus = document.getElementById('modal-project-status');
const modalProjectCreated = document.getElementById('modal-project-created');
const modalProjectExpires = document.getElementById('modal-project-expires');
const modalProjectParticipants = document.getElementById('modal-project-participants');
const modalProjectChallenges = document.getElementById('modal-project-challenges');
// const modalProjectForums = document.getElementById('modal-project-forums'); // No parece usarse para conteo
const modalProjectActions = document.getElementById('modal-project-actions');
const modalParticipantsList = document.getElementById('modal-participants-list');
const modalChallengesList = document.getElementById('modal-challenges-list');
const adminTabButton = document.querySelector('.detail-tab-btn[data-tab="admin"]');
const adminPanel = document.getElementById('admin-panel');
const editProjectFormContainer = document.getElementById('edit-project-form');
const projectEditForm = document.getElementById('project-edit-form');

// Inputs del formulario de edición
const editProjectId = document.getElementById('edit-project-id');
const editProjectName = document.getElementById('edit-project-name');
const editProjectDescription = document.getElementById('edit-project-description');
const editProjectExpiration = document.getElementById('edit-project-expiration');
const editProjectStatus = document.getElementById('edit-project-status');


function openProjectDetails(projectId) {
    if (!projectDetailModal) {
        console.error('Modal de detalles del proyecto no encontrado.');
        return;
    }
    projectDetailModal.classList.add('show'); // Usar clase para mostrar con animación
    projectDetailModal.dataset.projectId = projectId; // Guardar ID para referencia

    // Limpiar y mostrar carga
    modalProjectTitle.textContent = 'Cargando...';
    modalProjectImage.src = defaultImageBase64; // Imagen de carga
    modalProjectDescription.textContent = 'Cargando descripción...';
    modalProjectStatus.textContent = '-';
    modalProjectCreated.textContent = '-';
    modalProjectExpires.textContent = '-';
    modalProjectParticipants.textContent = '-';
    modalProjectChallenges.textContent = '-';
    modalProjectActions.innerHTML = '';
    modalParticipantsList.innerHTML = '<p class="loading-message">Cargando participantes...</p>';
    modalChallengesList.innerHTML = '<p class="loading-message">Cargando desafíos...</p>';
    editProjectFormContainer.style.display = 'none'; // Ocultar form de edición
    switchDetailTab('participants'); // Pestaña por defecto

    // Verificar permisos para pestaña admin (si userData está disponible)
    if (userData && adminTabButton && adminPanel) {
        const isAdmin = userData.rol === 'ADMIN' || userData.rol === 'ORGANIZACION'; // O el rol que sea
        adminTabButton.style.display = isAdmin ? 'block' : 'none';
        // No ocultar el panel completo, solo el botón si no es admin,
        // el contenido del panel se podría restringir también.
    }


    fetch(`${API_PROYECTOS_URL}/${projectId}`)
        .then(response => {
            if (!response.ok) throw new Error(`Error ${response.status} al cargar proyecto`);
            return response.json();
        })
        .then(proyecto => {
            modalProjectTitle.textContent = proyecto.nombre || "Proyecto sin título";
            modalProjectImage.src = proyecto.imagenUrl ? `/static/Proyectos/${proyecto.imagenUrl}` : defaultImageBase64;
            modalProjectDescription.textContent = proyecto.descripcion || "No hay descripción disponible.";

            const estadoActual = proyecto.estado || 'DESCONOCIDO';
            modalProjectStatus.textContent = estadoActual.charAt(0).toUpperCase() + estadoActual.slice(1).toLowerCase();
            modalProjectStatus.className = `project-detail-status ${estadoActual.toLowerCase()}`;


            modalProjectCreated.textContent = proyecto.fechaCreacion ? new Date(proyecto.fechaCreacion).toLocaleDateString() : 'N/A';
            modalProjectExpires.textContent = proyecto.fechaExpiracion ? new Date(proyecto.fechaExpiracion).toLocaleDateString() : 'Sin fecha límite';

            // Llenar formulario de edición (si es admin)
            if (editProjectId) editProjectId.value = proyecto.id;
            if (editProjectName) editProjectName.value = proyecto.nombre;
            if (editProjectDescription) editProjectDescription.value = proyecto.descripcion || '';
            if (editProjectStatus) editProjectStatus.value = estadoActual;
            if (editProjectExpiration && proyecto.fechaExpiracion) {
                try { // Formatear para datetime-local
                    const date = new Date(proyecto.fechaExpiracion);
                    // Ajustar a la zona horaria local antes de convertir a ISO y cortar
                    const offset = date.getTimezoneOffset() * 60000; // offset en milisegundos
                    const localDate = new Date(date.getTime() - offset);
                    editProjectExpiration.value = localDate.toISOString().slice(0, 16);
                } catch (e) {
                    console.warn("Error formateando fecha de expiración para input:", e);
                    editProjectExpiration.value = '';
                }
            } else if (editProjectExpiration) {
                editProjectExpiration.value = '';
            }


            updateProjectActionsInModal(proyecto);
            loadProjectParticipants(projectId);
            loadProjectChallengesForModal(projectId); // Cambiado para evitar conflicto de nombre
        })
        .catch(error => {
            console.error('Error al cargar detalles del proyecto:', error);
            modalProjectTitle.textContent = 'Error';
            modalProjectDescription.textContent = `No se pudo cargar la información: ${error.message}`;
        });
}

function closeModal(modalId) { // modalId es 'projectDetailModal'
    const modalToClose = document.getElementById(modalId);
    if (modalToClose) {
        modalToClose.classList.remove('show');
    }
}

function switchDetailTab(tabName) {
    document.querySelectorAll('.detail-tab-btn').forEach(btn => btn.classList.remove('active'));
    document.querySelectorAll('.detail-tab-panel').forEach(panel => panel.classList.remove('active'));

    document.querySelector(`.detail-tab-btn[data-tab="${tabName}"]`)?.classList.add('active');
    document.getElementById(`${tabName}-panel`)?.classList.add('active');
}

async function loadProjectParticipants(projectId) {
    try {
        const response = await fetch(`${API_PROYECTOS_URL}/${projectId}/participantes`);
        if (!response.ok) throw new Error(`Error ${response.status}`);
        const participantes = await response.json();

        if(modalProjectParticipants) modalProjectParticipants.textContent = participantes.length;
        modalParticipantsList.innerHTML = '';
        if (participantes.length === 0) {
            modalParticipantsList.innerHTML = '<p class="info-message">No hay participantes en este proyecto aún.</p>';
        } else {
            participantes.forEach(p => {
                const avatarUrl = p.imagenPerfil || `https://ui-avatars.com/api/?name=${encodeURIComponent(p.nombre || 'User')}&background=random&size=60`;
                const card = document.createElement('div');
                card.className = 'participant-card';
                card.innerHTML = `
                    <img src="${avatarUrl}" alt="${p.nombre || 'Participante'}" class="participant-avatar">
                    <div class="participant-name">${p.nombre || 'Usuario Anónimo'}</div>
                    <div class="participant-role">${p.rol || 'Voluntario'}</div>`;
                modalParticipantsList.appendChild(card);
            });
        }
    } catch (error) {
        console.error('Error al cargar participantes:', error);
        modalParticipantsList.innerHTML = `<p class="error-message">Error al cargar participantes.</p>`;
        if(modalProjectParticipants) modalProjectParticipants.textContent = 'Error';
    }
}

async function loadProjectChallengesForModal(projectId) {
    try {
        const response = await fetch(`${API_PROYECTOS_URL}/${projectId}/desafios`); // Asumiendo este endpoint
        if (!response.ok) throw new Error(`Error ${response.status}`);
        const desafios = await response.json();

        if(modalProjectChallenges) modalProjectChallenges.textContent = desafios.length;
        modalChallengesList.innerHTML = '';
        if (desafios.length === 0) {
            modalChallengesList.innerHTML = '<p class="info-message">Este proyecto no tiene desafíos asociados.</p>';
        } else {
            // Aquí deberías tener una forma de renderizar cada desafío, similar a renderChallengeCard pero más simple para una lista
            desafios.forEach(d => {
                const item = document.createElement('div');
                item.className = 'challenge-list-item'; // Necesitarás estilos para esto
                item.innerHTML = `
                    <strong>${d.nombre || 'Desafío sin título'}</strong>
                    <p>${d.descripcion || 'Sin descripción.'}</p>
                    <small>${d.puntosRecompensa || 0} puntos - Estado: ${d.estado || 'Activo'}</small>
                `;
                modalChallengesList.appendChild(item);
            });
        }
    } catch (error) {
        console.error('Error al cargar desafíos del proyecto:', error);
        modalChallengesList.innerHTML = `<p class="error-message">Error al cargar desafíos del proyecto.</p>`;
         if(modalProjectChallenges) modalProjectChallenges.textContent = 'Error';
    }
}

function updateProjectActionsInModal(proyecto) {
    if (!modalProjectActions) return;
    let html = '';
    const isParticipating = currentUserProjects.includes(proyecto.id);

    // Verificar si el proyecto está "abierto" para unirse (ACTIVO y no lleno)
    const isOpenForJoining = proyecto.estado === 'ACTIVO' &&
                           !(proyecto.haAlcanzadoLimiteParticipantes ||
                             (proyecto.participantesActuales >= proyecto.limiteParticipantes && proyecto.limiteParticipantes > 0));


    if (isParticipating) {
        html += `<button class="secondary-action" onclick="leaveProject('${proyecto.id}')"><i class="fas fa-sign-out-alt"></i> Abandonar Proyecto</button>`;
    } else if (isOpenForJoining) {
        html += `<button class="primary-action" onclick="joinProject('${proyecto.id}')"><i class="fas fa-hand-holding-heart"></i> Participar</button>`;
    } else { // No participa y no se puede unir (lleno, expirado, cancelado, etc.)
        let message = "No disponible para unirse";
        if (proyecto.estado === 'EXPIRADO') message = "Proyecto finalizado";
        else if (proyecto.estado === 'CANCELADO') message = "Proyecto cancelado";
        else if (proyecto.estado === 'COMPLETO' || !isOpenForJoining) message = "Proyecto completo";

        html += `<button class="secondary-action" disabled><i class="fas fa-info-circle"></i> ${message}</button>`;
    }

    // Botón de ver foro (siempre visible si el proyecto tiene foro, o si participa)
    // Asumiendo que todos los proyectos pueden tener un foro accesible
    // Podrías condicionarlo a `proyecto.tieneForo === true` si tienes ese dato
    html += `<button class="primary-action" onclick="gotoProjectForum('${proyecto.id}')"><i class="fas fa-comments"></i> Ver Foro</button>`;

    modalProjectActions.innerHTML = html;
}


function gotoProjectForum(projectId) {
    // Redirigir a la página del foro del proyecto
    window.location.href = `/foros/proyecto/${projectId}`; // Ajusta esta URL según tu enrutamiento
    // O si es un modal/SPA: cargar el contenido del foro.
}

function toggleEditForm(show) {
    if (editProjectFormContainer) {
        editProjectFormContainer.style.display = show ? 'block' : 'none';
    }
}

async function updateProject() { // Llamado desde el onsubmit del form
    const projectId = editProjectId.value;
    if (!projectId) {
        showNotification('error', 'ID de proyecto no encontrado para actualizar.');
        return;
    }

    const updatedProjectData = {
        id: projectId,
        nombre: editProjectName.value,
        descripcion: editProjectDescription.value,
        estado: editProjectStatus.value,
        // Formatear fechaExpiracion a ISO si se proporciona, o null/undefined si está vacía
        fechaExpiracion: editProjectExpiration.value ? new Date(editProjectExpiration.value).toISOString() : null
    };

    try {
        const response = await fetch(`${API_PROYECTOS_URL}/${projectId}`, {
            method: 'PUT',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(updatedProjectData)
        });
        if (!response.ok) {
            const errorData = await response.json().catch(() => ({ message: `Error ${response.status}` }));
            throw new Error(errorData.message || `Error al actualizar: ${response.statusText}`);
        }
        showNotification('success', 'Proyecto actualizado correctamente.');
        toggleEditForm(false);
        openProjectDetails(projectId); // Recargar detalles en el modal
        await loadDataAndRender(currentPage); // Recargar la lista de proyectos
    } catch (error) {
        console.error('Error al actualizar proyecto:', error);
        showNotification('error', `Error al actualizar proyecto: ${error.message}`);
    }
}

async function confirmDeleteProject() {
    const projectId = editProjectId.value;
    const projectName = editProjectName.value || "este proyecto";
    if (!projectId) {
        showNotification('error', 'ID de proyecto no encontrado para eliminar.');
        return;
    }

    if (confirm(`¿Estás SEGURO de que deseas eliminar el proyecto "${projectName}"? Esta acción es irreversible.`)) {
        try {
            const response = await fetch(`${API_PROYECTOS_URL}/${projectId}`, { method: 'DELETE' });
            if (!response.ok) {
                const errorData = await response.json().catch(() => ({ message: `Error ${response.status}` }));
                throw new Error(errorData.message || `Error al eliminar: ${response.statusText}`);
            }
            showNotification('success', 'Proyecto eliminado correctamente.');
            closeModal('projectDetailModal');
            await loadDataAndRender(1); // Recargar lista de proyectos desde la página 1
        } catch (error) {
            console.error('Error al eliminar proyecto:', error);
            showNotification('error', `Error al eliminar proyecto: ${error.message}`);
        }
    }
}


// --- UTILIDADES (localStorage, verificación de participación) ---
function saveUserProjectsToLocalStorage() {
    localStorage.setItem('userProjects', JSON.stringify(currentUserProjects || []));
}

function loadUserProjectsFromLocalStorage() {
    const storedProjects = localStorage.getItem('userProjects');
    if (storedProjects) {
        try {
            currentUserProjects = JSON.parse(storedProjects);
            return true;
        } catch (e) {
            console.error('Error al parsear proyectos del localStorage:', e);
            currentUserProjects = []; // Resetear si hay error
        }
    }
    return false;
}

async function verificarParticipacionEnProyecto(projectId) {
    if (!userData || !userData.id) return currentUserProjects.includes(projectId); // Fallback si no hay datos de usuario

    try {
        // Intento 1: Endpoint específico si existe
        // const checkResponse = await fetch(`${API_PROYECTOS_URL}/${projectId}/verificar-participacion`);
        // if (checkResponse.ok) return (await checkResponse.json()).esParticipante === true;

        // Intento 2: Lista de participantes (menos eficiente si solo es para verificar uno)
        const participantsResponse = await fetch(`${API_PROYECTOS_URL}/${projectId}/participantes`);
        if (participantsResponse.ok) {
            const participants = await participantsResponse.json();
            return participants.some(p => p.id === userData.id || p.usuarioId === userData.id);
        }
    } catch (error) {
        console.warn(`Error al verificar participación para proyecto ${projectId} vía API:`, error);
    }
    // Fallback final a la lista local
    return currentUserProjects.includes(projectId);
}

// --- INICIALIZACIÓN ---
document.addEventListener('DOMContentLoaded', async () => {
    loadUserProjectsFromLocalStorage(); // Cargar IDs de proyectos del usuario si existen

    try {
        await fetchUserData(); // Cargar datos del usuario actual (incluyendo sus proyectos)
    } catch (error) {
        // El error ya se maneja dentro de fetchUserData, mostrando mensaje en la UI.
        // Podrías querer detener más ejecuciones aquí si es crítico.
        return;
    }


    // Event listeners para paginación
    prevPageButton.addEventListener('click', () => {
        if (currentPage > 1) searchProjectsPaginated(userSearchInput.value, currentPage - 1);
    });
    nextPageButton.addEventListener('click', () => {
        if (currentPage < totalPages) searchProjectsPaginated(userSearchInput.value, currentPage + 1);
    });
    challengesPrevPageButton.addEventListener('click', () => {
        if (challengesCurrentPage > 1) searchChallengesPaginated(userSearchInput.value, challengesCurrentPage - 1);
    });
    challengesNextPageButton.addEventListener('click', () => {
        if (challengesCurrentPage < challengesTotalPages) searchChallengesPaginated(userSearchInput.value, challengesCurrentPage + 1);
    });

    // Event listeners para pestañas
    tabButtons.forEach(button => {
        button.addEventListener('click', () => switchTab(button.dataset.tab));
    });

    // Event listeners para filtros (secciones de proyectos y desafíos)
    document.querySelectorAll('.filter-section').forEach(section => {
        section.querySelectorAll('.filter-btn').forEach(button => {
            button.addEventListener('click', () => {
                section.querySelectorAll('.filter-btn').forEach(btn => btn.classList.remove('active'));
                button.classList.add('active');
                applyFilter();
            });
        });
    });

    // Listener para búsqueda con botón e input
    const searchButton = document.querySelector('.search-btn'); // Ya definido globalmente? No, local aquí.
    if (searchButton) searchButton.addEventListener('click', searchItems);
    if (userSearchInput) userSearchInput.addEventListener('keypress', (e) => {
        if (e.key === 'Enter') searchItems();
    });


    // Carga inicial de la pestaña activa (proyectos por defecto)
    if (activeTab === 'projects') {
        await loadDataAndRender(1);
    } else {
        await loadChallengesPaginated(1); // Si por alguna razón la pestaña activa inicial fuera desafíos
    }
    resetActiveFilters(); // Asegurar que el filtro 'all' esté activo al inicio
    applyFilter();
});

// Cerrar modal con tecla Escape
window.addEventListener('keydown', (event) => {
    if (event.key === 'Escape' && projectDetailModal.classList.contains('show')) {
        closeModal('projectDetailModal');
    }
});
// Cerrar modal al hacer clic fuera del contenido
projectDetailModal.addEventListener('click', (event) => {
    if (event.target === projectDetailModal) { // Si el clic es en el overlay mismo
        closeModal('projectDetailModal');
    }
});

// Event delegation para botones dentro de las tarjetas de desafío
challengesGrid.addEventListener('click', function(event) {
    const target = event.target.closest('button');
    if (!target) return;

    const challengeCard = target.closest('.challenge-card');
    if (!challengeCard) return;

    const challengeId = challengeCard.dataset.challengeId;

    if (target.classList.contains('participate-challenge-btn')) {
        participateInChallenge(challengeId);
    } else if (target.classList.contains('complete-challenge-btn')) {
        completeChallenge(challengeId);
    } else if (target.classList.contains('view-details-btn')) {
        // Aquí podrías abrir un modal de detalles del desafío o redirigir
        console.log("Ver detalles del desafío:", challengeId);
        showNotification('info', `Detalles para desafío ${challengeId} (no implementado).`);
    }
});