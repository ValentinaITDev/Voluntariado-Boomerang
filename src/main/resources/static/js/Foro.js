// Variables globales para el foro actual
let currentForoId = null;
let currentForo = null;
const defaultUserImage = '/Perfiles/default-user.png'; // Imagen por defecto actualizada
const defaultProjectImage = '/Proyectos/4998f734-a9eb-4757-b819-1eac90cd3b89.png'; // Imagen por defecto para proyectos que existe en el sistema

// Las variables 'currentUser' y 'forosData' se deben obtener de otra forma
// ya que no se puede usar th:inline="javascript" en un archivo JS externo.
// Posibles soluciones: pasar datos a través de atributos data-* en el HTML,
// o hacer llamadas API adicionales si es necesario.

// Intentaremos obtener currentUser de un script incrustado en el HTML
let currentUser = { id: 'usuario-id-default', nombre: 'Nombre Usuario Default', imagenPerfil: defaultUserImage };
// Intentaremos obtener forosData de un script incrustado en el HTML
let forosData = [];

// Renderizar los foros iniciales (si los hay en el modelo)
document.addEventListener('DOMContentLoaded', function() {
    // Intentar obtener datos globales definidos en el HTML
    if (typeof window.currentUserData !== 'undefined') {
        currentUser = window.currentUserData;
        console.log("currentUser obtenido desde window:", currentUser);
    } else {
        console.warn("currentUserData no encontrado en window. Usando valores por defecto.");
    }
    if (typeof window.initialForosData !== 'undefined') {
        forosData = window.initialForosData;
        console.log("forosData obtenido desde window:", forosData);
    }

    if (forosData && forosData.length > 0) {
        renderForos(forosData);
    } else {
        console.log("No hay foros iniciales, cargando desde API...");
        cargarForos();
    }
    // Actualizar imagen en input de comentario
    const inputAvatar = document.querySelector('.user-avatar-input');
    if (inputAvatar && currentUser.imagenPerfil) {
        inputAvatar.src = currentUser.imagenPerfil;
        inputAvatar.onerror = function() { this.onerror=null; this.src=defaultUserImage; };
    }

    // Crear un elemento de estilo
    const style = document.createElement('style');
    style.textContent = `
        .edit-comment-input {
            width: 100%;
            padding: 10px;
            border: 1px solid var(--color-primary, #3498db);
            border-radius: 8px;
            margin-bottom: 10px;
            font-family: inherit;
            resize: vertical;
            box-shadow: 0 2px 5px rgba(0,0,0,0.05);
        }
        
        .edit-actions {
            display: flex;
            gap: 10px;
            margin-top: 8px;
            justify-content: flex-end;
        }
        
        .edit-submit-btn, .edit-cancel-btn {
            padding: 8px 16px;
            border: none;
            border-radius: 6px;
            cursor: pointer;
            font-weight: 500;
            display: flex;
            align-items: center;
            gap: 5px;
            transition: all 0.2s ease;
        }
        
        .edit-submit-btn {
            background-color: var(--color-primary, #3498db);
            color: white;
            box-shadow: 0 2px 5px rgba(52, 152, 219, 0.3);
        }
        
        .edit-cancel-btn {
            background-color: #f1f1f1;
            color: #333;
        }
        
        .edit-submit-btn:hover {
            background-color: var(--color-primary-dark, #2980b9);
            transform: translateY(-2px);
        }
        
        .edit-cancel-btn:hover {
            background-color: #e1e1e1;
            transform: translateY(-2px);
        }

        .post {
            border-radius: 12px;
            box-shadow: 0 3px 10px rgba(0,0,0,0.1);
            margin-bottom: 22px;
            overflow: hidden;
            transition: all 0.3s ease;
            border: 1px solid rgba(0,0,0,0.08);
        }

        .post:hover {
            box-shadow: 0 5px 15px rgba(0,0,0,0.15);
            transform: translateY(-3px);
        }

        .post-header {
            padding: 16px 20px 10px;
            display: flex;
            align-items: center;
        }

        .post-content {
            padding: 5px 20px 15px;
        }

        .post-content p {
            margin: 0;
            line-height: 1.5;
            color: #333;
        }

        .post-actions {
            padding: 10px 20px;
            border-top: 1px solid #eee;
            display: flex;
            gap: 18px;
            flex-wrap: wrap;
            align-items: center;
        }

        .action-btn {
            padding: 8px 14px;
            border-radius: 6px;
            transition: all 0.2s ease;
            font-size: 0.9rem;
            display: inline-flex;
            align-items: center;
            gap: 8px;
        }

        .action-btn:hover {
            background-color: #f0f0f0;
            transform: translateY(-2px);
        }

        .action-btn.reply-btn {
            color: #3498db;
        }

        .action-btn.reply-btn:hover {
            background-color: rgba(52, 152, 219, 0.1);
        }

        .action-btn.report-btn {
            color: #e74c3c;
        }

        .action-btn.report-btn:hover {
            background-color: rgba(231, 76, 60, 0.1);
        }

        .action-btn.edit-btn {
            color: #2ecc71;
        }

        .action-btn.edit-btn:hover {
            background-color: rgba(46, 204, 113, 0.1);
        }

        .action-btn.delete-btn {
            color: #e74c3c;
        }

        .action-btn.delete-btn:hover {
            background-color: rgba(231, 76, 60, 0.1);
        }

        .reply-form {
            background-color: #f8f9fa;
            padding: 15px;
            border-radius: 10px;
            margin-top: 10px;
            border: 1px solid #e1e4e8;
            display: flex;
            gap: 12px;
            align-items: flex-start;
        }

        .reply-input {
            border: 1px solid #ddd;
            border-radius: 8px;
            padding: 12px;
            width: 100%;
            min-height: 60px;
            font-family: inherit;
            transition: border-color 0.2s;
            box-shadow: 0 1px 3px rgba(0,0,0,0.05);
        }

        .reply-input:focus {
            outline: none;
            border-color: var(--color-primary, #3498db);
        }

        .reply-submit-btn {
            background-color: var(--color-primary, #3498db);
            color: white;
            border: none;
            border-radius: 8px;
            padding: 10px 16px;
            cursor: pointer;
            font-weight: 500;
            display: flex;
            align-items: center;
            gap: 6px;
            transition: all 0.2s ease;
            box-shadow: 0 2px 5px rgba(0,0,0,0.1);
        }

        .reply-submit-btn:hover {
            background-color: var(--color-primary-dark, #2980b9);
            transform: translateY(-2px);
            box-shadow: 0 4px 8px rgba(0,0,0,0.15);
        }

        .reported-badge {
            display: inline-flex;
            align-items: center;
            gap: 5px;
            background-color: rgba(231, 76, 60, 0.1);
            color: #e74c3c;
            padding: 6px 12px;
            border-radius: 6px;
            font-size: 0.85rem;
            font-weight: 500;
            margin-bottom: 12px;
            box-shadow: 0 1px 3px rgba(231, 76, 60, 0.2);
        }

        .new-post-form {
            background-color: white;
            padding: 20px;
            border-radius: 12px;
            box-shadow: 0 -2px 10px rgba(0,0,0,0.1);
            border-top: 1px solid #eee;
            display: flex;
            gap: 15px;
            align-items: flex-start;
        }

        .new-post-input {
            border: 1px solid #ddd;
            border-radius: 10px;
            padding: 15px;
            flex: 1;
            min-height: 80px;
            font-family: inherit;
            transition: all 0.2s ease;
            box-shadow: 0 2px 5px rgba(0,0,0,0.05);
        }

        .new-post-input:focus {
            outline: none;
            border-color: var(--color-primary, #3498db);
            box-shadow: 0 3px 8px rgba(52, 152, 219, 0.2);
        }

        .submit-btn {
            background-color: var(--color-primary, #3498db);
            color: white;
            border: none;
            border-radius: 10px;
            padding: 12px 20px;
            cursor: pointer;
            font-weight: 600;
            display: flex;
            align-items: center;
            gap: 8px;
            transition: all 0.3s ease;
            box-shadow: 0 3px 8px rgba(52, 152, 219, 0.3);
        }

        .submit-btn:hover {
            background-color: var(--color-primary-dark, #2980b9);
            transform: translateY(-3px);
            box-shadow: 0 5px 15px rgba(52, 152, 219, 0.4);
        }
    `;
    
    // Añadir el estilo al documento
    document.head.appendChild(style);

    // Agregar evento para reportar comentarios
    document.addEventListener('click', function(e) {
        // Delegar evento de clic para botones de reporte
        if (e.target.closest('.report-btn')) {
            const reportBtn = e.target.closest('.report-btn');
            if (reportBtn.disabled) return;
            
            const commentId = reportBtn.getAttribute('data-comment-id');
            if (commentId) {
                reportComment(commentId);
            }
        }
    });
});

// Función para cargar los foros desde la API usando Promesas
function cargarForos() {
    fetch('/api/foros')
        .then(function(response) {
            if (!response.ok) {
                throw new Error('Error al cargar foros: ' + response.status);
            }
            return response.json();
        })
        .then(function(forosApiResponse) {
            let foros = [];
            // Manejar la respuesta paginada o la lista directa
            if (forosApiResponse && forosApiResponse.content) {
                foros = forosApiResponse.content;
            } else if (Array.isArray(forosApiResponse)){
                 foros = forosApiResponse;
            } else {
                 console.warn("La respuesta de /api/foros no tiene el formato esperado.", forosApiResponse);
            }
            
            renderForos(foros);
        })
        .catch(function(error) {
            console.error('Error al obtener foros:', error);
            const forosGrid = document.getElementById('forosGrid');
            if(forosGrid) {
                forosGrid.innerHTML = 
                '<div class="error-message">Error al cargar foros. Por favor, intenta de nuevo más tarde.</div>';
            } else {
                 console.error("Elemento #forosGrid no encontrado.");
            }
        });
}

// Función para obtener las iniciales de un nombre
function obtenerIniciales(nombre) {
    if (!nombre || typeof nombre !== 'string') return "P";
    
    const palabras = nombre.trim().split(/\s+/);
    if (palabras.length === 1 && palabras[0].length > 0) {
        return palabras[0].substring(0, Math.min(palabras[0].length, 2)).toUpperCase();
    }
    
    let iniciales = "";
    for(let i = 0; i < Math.min(palabras.length, 2); i++) {
        if (palabras[i].length > 0) {
            iniciales += palabras[i][0].toUpperCase();
        }
    }
    return iniciales;
}

// Renderizar las tarjetas de foros usando bucle for
function renderForos(foros) {
    const forosGrid = document.getElementById('forosGrid');
    if (!forosGrid) {
        console.error("Elemento #forosGrid no encontrado para renderizar.");
        return;
    }
    
    if (!Array.isArray(foros) || foros.length === 0) {
        forosGrid.innerHTML = '<div class="no-foros-message"><p>No hay foros disponibles en este momento.</p></div>';
        return;
    }
    
    forosGrid.innerHTML = ''; // Limpiar antes de renderizar
    
    for (let i = 0; i < foros.length; i++) {
        const foro = foros[i];
        const foroCard = document.createElement('div');
        foroCard.className = 'foro-card';
        foroCard.dataset.foroId = foro.id;
        
        const esActivo = foro.etiquetas ? !foro.etiquetas.includes('CERRADO') : true; // Asumir activo si no hay etiquetas
        
        let nombreProyecto = 'Foro General';
        if(foro.etiquetas && Array.isArray(foro.etiquetas)) {
            for (let j = 0; j < foro.etiquetas.length; j++) {
                let etiqueta = foro.etiquetas[j];
                if (etiqueta !== 'Proyecto' && !etiqueta.startsWith('proyecto-') && etiqueta !== 'CERRADO') {
                    nombreProyecto = etiqueta;
                    break;
                }
            }
        }
         if (nombreProyecto === 'Foro General' && foro.titulo && !foro.titulo.startsWith('Foro:')) {
            nombreProyecto = foro.titulo;
         }
        
         let ultimaActividad = foro.fechaCreacion;
         if (foro.comentarios && foro.comentarios.length > 0) {
             let latestDate = new Date(foro.fechaCreacion || 0);
             for(let k = 0; k < foro.comentarios.length; k++){
                 const comentario = foro.comentarios[k];
                 let currentCommentDate = new Date(comentario.fechaCreacion);

                 if(comentario.respuestas && comentario.respuestas.length > 0){
                     let latestReplyDate = new Date(0);
                     for(let l=0; l < comentario.respuestas.length; l++){
                          const respuesta = comentario.respuestas[l];
                          const replyDate = new Date(respuesta.fechaCreacion);
                          if(replyDate > latestReplyDate) latestReplyDate = replyDate;
                     }
                     if(latestReplyDate > currentCommentDate) currentCommentDate = latestReplyDate;
                 }
                  if (currentCommentDate > latestDate) latestDate = currentCommentDate;
             }
              ultimaActividad = latestDate;
         }
         const tiempoUltimaActividad = calcularTiempoTranscurrido(new Date(ultimaActividad));
        
        foroCard.innerHTML = `
            <div class="foro-card-header">
                <h3>${nombreProyecto}</h3>
                <span class="project-status-badge ${esActivo ? 'status-active' : 'status-completed'}">${esActivo ? 'Activo' : 'Cerrado'}</span>
            </div>
            <div class="foro-stats">
                <div class="stat-item">
                    <i class="fas fa-users"></i>
                    <span>Participantes N/A</span>
                </div>
                <div class="stat-item">
                    <i class="fas fa-comments"></i>
                    <span>${foro.comentarios ? foro.comentarios.length : 0} comentarios</span>
                </div>
                <div class="stat-item">
                    <i class="fas fa-clock"></i>
                    <span>Última actividad: ${tiempoUltimaActividad}</span>
                </div>
            </div>
        `;
        
        foroCard.addEventListener('click', function() { openForo(foro.id); }); // Usando function()
        forosGrid.appendChild(foroCard);
    }
}

// Abrir un foro específico usando Promesas
function openForo(id) {
    if (!id) {
        console.error("Se intentó abrir un foro sin ID.");
        return;
    }
    currentForoId = id;
    console.log(`Abriendo foro con ID: ${id}`);
    
    const foroDetailElement = document.getElementById('foroDetail');
    const foroHeader = document.querySelector('#foroDetail .foro-header');
    const foroTitleElement = document.getElementById('foroTitle');
    const postsContainer = document.getElementById('postsContainer');
    const submitCommentBtn = document.getElementById('submitCommentBtn');

    if (!foroDetailElement || !foroHeader || !foroTitleElement || !postsContainer || !submitCommentBtn) {
        console.error("No se encontraron todos los elementos necesarios para abrir el foro.");
        return;
    }

    foroDetailElement.classList.add('show');
    document.body.style.overflow = 'hidden';
    
    const existingProjectImage = foroHeader.querySelector('.foro-header-project-image');
    if (existingProjectImage) {
        existingProjectImage.remove(); 
    }
    foroTitleElement.innerText = 'Cargando foro...';
    postsContainer.innerHTML = '<div class="loading"><i class="fas fa-spinner fa-spin"></i> Cargando comentarios...</div>';
    submitCommentBtn.disabled = true; // Deshabilitar hasta que cargue
    
    fetch(`/api/foros/${id}`)
        .then(function(response) {
            if (!response.ok) {
                throw new Error(`Error al cargar el foro: ${response.status} ${response.statusText}`);
            }
            return response.json();
        })
        .then(function(foroData) {
            currentForo = foroData;
            console.log("Datos del foro cargados:", currentForo);
            
            let nombreProyecto = 'Foro General';
            let proyectoId = null;
            if (currentForo.etiquetas && Array.isArray(currentForo.etiquetas)) {
                for (let i = 0; i < currentForo.etiquetas.length; i++) {
                    let etiqueta = currentForo.etiquetas[i];
                    if (etiqueta.startsWith('proyecto-')) {
                        proyectoId = etiqueta.substring('proyecto-'.length);
                    } else if (etiqueta !== 'Proyecto' && etiqueta !== 'CERRADO') { // Ignorar 'Proyecto' y 'CERRADO'
                        nombreProyecto = etiqueta;
                    }
                }
            } else {
                 console.warn("El foro no tiene etiquetas o no es un array.");
            }

             if (nombreProyecto === 'Foro General' && currentForo.titulo && !currentForo.titulo.startsWith('Foro:')) {
                nombreProyecto = currentForo.titulo;
             }
            
            foroTitleElement.innerText = `Foro: ${nombreProyecto}`;
            
             // Elemento decorativo placeholder
             const decorElement = document.createElement('div');
             decorElement.className = 'foro-header-project-image project-image-placeholder';
             decorElement.style.background = 'linear-gradient(135deg, var(--color-primary, #3498db), var(--color-secondary, #2ecc71))';
             decorElement.style.display = 'flex';
             decorElement.style.alignItems = 'center';
             decorElement.style.justifyContent = 'center';
             // Icono por defecto
             decorElement.innerHTML = `<i class="fas fa-comments" style="color: white; font-size: 24px;"></i>`; 

             if (proyectoId) {
                 console.log(`Intentando cargar info adicional para proyecto ID: ${proyectoId}`);
                 fetch(`/api/proyectos/${proyectoId}`)
                     .then(function(response) {
                         if (!response.ok) {
                            return Promise.reject('Error API proyecto');
                         }
                         return response.json();
                     })
                     .then(function(proyectoData) {
                         console.log("Datos del proyecto cargados:", proyectoData);
                         if (proyectoData && proyectoData.nombre) {
                             const iniciales = obtenerIniciales(proyectoData.nombre);
                             decorElement.innerHTML = `<span style="color: white; font-size: 18px; font-weight: bold;">${iniciales}</span>`;
                         }
                     })
                     .catch(function(error) {
                         console.warn("No se pudieron cargar datos adicionales del proyecto:", error);
                     });
             }
             
             // Insertar elemento decorativo
             foroHeader.insertBefore(decorElement, foroTitleElement);

            renderComentarios(currentForo.comentarios || []);
            
            // Habilitar el botón de enviar comentario ahora que todo cargó
            submitCommentBtn.onclick = submitComment;
            submitCommentBtn.disabled = false;
            
        })
        .catch(function(error) {
            console.error('Error al abrir foro:', error);
            postsContainer.innerHTML = 
                '<div class="error-message"><i class="fas fa-exclamation-triangle"></i> Error al cargar el foro. Por favor, intenta de nuevo más tarde.</div>';
            // Mantener botón deshabilitado en caso de error
            submitCommentBtn.onclick = null;
            submitCommentBtn.disabled = true;
        });
}

// Cerrar el foro actual
function closeForo() {
    const foroDetailElement = document.getElementById('foroDetail');
    if(foroDetailElement) {
        foroDetailElement.classList.remove('show');
    }
    document.body.style.overflow = 'auto';
    currentForoId = null;
    currentForo = null;
}

// Renderizar los comentarios de un foro
function renderComentarios(comentarios) {
    const postsContainer = document.getElementById('postsContainer');
    if(!postsContainer) return;
    
    if (!Array.isArray(comentarios) || comentarios.length === 0) {
        postsContainer.innerHTML = '<div class="no-comments">No hay comentarios aún. ¡Sé el primero en comentar!</div>';
        return;
    }
    
    postsContainer.innerHTML = ''; // Limpiar antes de renderizar
    
    for (let i = 0; i < comentarios.length; i++) {
        const comentario = comentarios[i];
        if(!comentario || !comentario.autor) {
            console.warn("Comentario inválido o sin autor:", comentario);
            continue; // Saltar comentario inválido
        }
        
        // Crear el elemento para el comentario
        const postElement = document.createElement('div');
        postElement.className = 'post';
        postElement.setAttribute('data-comment-id', comentario.id);
        
        // Añadir clases para estados especiales
        if (comentario.reportado) postElement.classList.add('reported');
        if (comentario.restringido) postElement.classList.add('restricted');
        
        // Definir si el usuario actual es el autor o admin
        const esAutor = window.currentUserData && comentario.autor.id === window.currentUserData.id;
        const esAdmin = window.currentUserData && window.currentUserData.esAdmin;
        
        // Avatar del autor (con fallback a imagen predeterminada)
        const imagenPerfil = comentario.autor.imagenPerfil || '/Perfiles/default-user.png';
        
        // Fecha formateada
        const fechaFormateada = calcularTiempoTranscurrido(comentario.fechaCreacion);
        
        // Crear contenido HTML para el comentario
        let postHTML = `
            <div class="post-header">
                <img src="${imagenPerfil}" alt="${comentario.autor.nombre}" class="user-avatar" 
                     onerror="this.onerror=null; this.src='/Perfiles/default-user.png'">
                <div class="post-info">
                    <div class="post-author">${comentario.autor.nombre}</div>
                    <div class="post-date">${fechaFormateada}</div>
                </div>
            </div>
            <div class="post-content">
        `;
        
        // Si el comentario está reportado, mostrar badge
        if (comentario.reportado) {
            postHTML += `<div class="reported-badge"><i class="fas fa-flag"></i> Reportado</div>`;
        }
        
        // Si el comentario está restringido, mostrar mensaje especial
        if (comentario.restringido) {
            postHTML += `
                <div class="restricted-content">
                    <div class="restricted-icon"><i class="fas fa-ban"></i></div>
                    <div class="restricted-message">Este comentario ha sido restringido por un administrador</div>
                    <div class="restricted-reason">${comentario.motivoRestriccion || 'Incumplimiento de normas comunitarias'}</div>
                </div>
            `;
            
            // Mostrar contenido original solo para administradores
            if (esAdmin) {
                postHTML += `
                    <div class="original-content">
                        <div class="content-label">Contenido original:</div>
                        <p>${comentario.contenido}</p>
                    </div>
                `;
            }
        } else {
            // Contenido normal si no está restringido
            postHTML += `<p>${comentario.contenido}</p>`;
        }
        
        postHTML += `</div>`;
        
        // Añadir acciones de comentario (likes, respuestas, etc.)
        postHTML += `
            <div class="post-actions">
                <button class="action-btn like-btn" onclick="toggleLike(this, '${comentario.id}', false)">
                    <i class="far fa-heart"></i> <span>${comentario.likes || 0}</span>
                </button>
                <button class="action-btn reply-btn" onclick="showReplyForm(this, '${comentario.id}')">
                    <i class="far fa-comment"></i> Responder
                </button>
                
                ${esAutor ? `
                    <button class="action-btn edit-btn" onclick="editComment('${comentario.id}')">
                        <i class="far fa-edit"></i> Editar
                    </button>
                    <button class="action-btn delete-btn" onclick="deleteComment('${comentario.id}')">
                        <i class="far fa-trash-alt"></i> Eliminar
                    </button>
                ` : ''}
                
                ${!esAutor && !comentario.reportado ? `
                    <button class="action-btn report-btn" onclick="reportComment('${comentario.id}')">
                        <i class="far fa-flag"></i> Reportar
                    </button>
                ` : ''}
                
                ${!esAutor && comentario.reportado ? `
                    <button class="action-btn report-btn" disabled>
                        <i class="fas fa-flag"></i> Reportado
                    </button>
                ` : ''}
            </div>
        `;
        
        // Contenedor para respuestas
        postHTML += `<div class="replies-container" id="replies-${comentario.id}"></div>`;
        
        // Formulario para responder (oculto por defecto)
        postHTML += `
            <div class="reply-form-container" id="reply-form-${comentario.id}" style="display: none;">
                <div class="reply-form">
                    <textarea class="reply-input" placeholder="Escribe tu respuesta..."></textarea>
                    <div class="reply-form-actions">
                        <button class="cancel-reply-btn" onclick="hideReplyForm('${comentario.id}')">Cancelar</button>
                        <button class="submit-reply-btn" onclick="submitReply('${comentario.id}', this)">Responder</button>
                    </div>
                </div>
            </div>
        `;
        
        // Establecer el HTML al elemento
        postElement.innerHTML = postHTML;
        
        // Añadir el comentario al contenedor
        postsContainer.appendChild(postElement);
        
        // Renderizar respuestas si existen
        if (comentario.respuestas && comentario.respuestas.length > 0) {
            renderRespuestas(comentario.respuestas, comentario.id);
        }
    }
}

// Renderizar las respuestas de un comentario usando bucle for
function renderRespuestas(respuestas, comentarioId) {
    if (!Array.isArray(respuestas) || respuestas.length === 0) {
        return '';
    }
    
    let respuestasHTML = '<div class="reply-list">';
    
    for (let i = 0; i < respuestas.length; i++) {
         const respuesta = respuestas[i];
          if(!respuesta || !respuesta.autor) {
             console.warn("Respuesta inválida o sin autor:", respuesta);
             continue; // Saltar respuesta inválida
        }
        const tiempoTranscurrido = calcularTiempoTranscurrido(new Date(respuesta.fechaCreacion));
        const autorImagen = respuesta.autor.imagenPerfil || defaultUserImage;
        
        // Verificar si el usuario actual ya dio like
        const usuarioDioLike = currentUser && respuesta.usuariosQueDieronLike && 
            respuesta.usuariosQueDieronLike.includes(currentUser.id);
        
        respuestasHTML += `
            <div class="post reply"> 
                <div class="post-header">
                    <img src="${autorImagen}" alt="Avatar" class="user-avatar" onerror="this.onerror=null; this.src='${defaultUserImage}'">
                    <div class="post-info">
                        <h4>${respuesta.autor.nombre || 'Usuario Anónimo'}</h4>
                        <span>${tiempoTranscurrido}</span>
                    </div>
                </div>
                <div class="post-content">
                    <p>${respuesta.contenido || ''}</p>
                </div>
                <div class="post-interactions">
                    <button class="interaction-btn like-btn ${usuarioDioLike ? 'liked' : ''}" 
                            data-likes="${respuesta.likes || 0}" 
                            onclick="toggleLike(this, '${respuesta.id}', true)">
                        <i class="${usuarioDioLike ? 'fas' : 'far'} fa-heart"></i>
                        <span>${respuesta.likes || 0} Me gusta</span>
                    </button>
                </div>
            </div>
        `;
    }
     respuestasHTML += '</div>';
    return respuestasHTML;
}

// Mostrar/ocultar el formulario de respuesta
function showReplyForm(button, commentId) {
     const postElement = button.closest('.post');
     if (!postElement) return;
     const repliesContainer = postElement.querySelector('.replies-container');
     if (!repliesContainer) return;
     
     const existingForm = repliesContainer.querySelector('.reply-form');
     if (existingForm) {
         existingForm.remove();
         return; 
     }

     // Eliminar otros formularios abiertos usando bucle for
     const openForms = document.querySelectorAll('.reply-form');
     for(let i = 0; i < openForms.length; i++){
         openForms[i].remove();
     }

     const replyForm = document.createElement('div');
     replyForm.className = 'reply-form';
     replyForm.innerHTML = `
         <img src="${currentUser.imagenPerfil || defaultUserImage}" 
              alt="Tu avatar" class="user-avatar-input"
              onerror="this.onerror=null; this.src='${defaultUserImage}'">
         <textarea class="new-post-input reply-input" placeholder="Escribe una respuesta..."></textarea>
         <button class="submit-btn reply-submit-btn" onclick="submitReply('${commentId}', this)">
             <i class="fas fa-paper-plane"></i> Responder
         </button>
     `;

     repliesContainer.appendChild(replyForm);
     const textarea = replyForm.querySelector('textarea');
     if(textarea) textarea.focus();
}

// Enviar una respuesta a un comentario usando Promesas
function submitReply(commentId, button) {
    if (!currentForoId || !commentId) {
        console.error("Falta ID de foro o comentario para responder.");
        return;
    }

    const replyForm = button.closest('.reply-form');
    if (!replyForm) return;
    const replyInput = replyForm.querySelector('.reply-input');
    if (!replyInput) return;
    const contenido = replyInput.value.trim();

    if (!contenido) {
        alert('Por favor, escribe una respuesta.');
        return;
    }
             
     button.disabled = true;
     button.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Enviando...';

    fetch(`/api/foros/${currentForoId}/comentarios/${commentId}/respuestas`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            // Añadir token de autorización si es necesario
        },
        body: JSON.stringify(contenido) 
    })
    .then(function(response) {
        if (!response.ok) {
            return response.text().then(function(errorText){
                throw new Error(`Error al publicar respuesta: ${response.status} - ${errorText}`);
            });
        }
        
        // Respuesta exitosa, no necesariamente devuelve todo el foro
        // Es mejor recargar solo los comentarios o añadir la respuesta dinámicamente
        console.log("Respuesta publicada con éxito. Recargando foro...");
        return openForo(currentForoId); // Recargar para simplicidad por ahora
    })
    .catch(function(error) {
        console.error('Error al enviar respuesta:', error);
        alert(`Error al publicar la respuesta: ${error.message}`);
         button.disabled = false;
         button.innerHTML = '<i class="fas fa-paper-plane"></i> Responder';
    });
}

// Enviar un nuevo comentario usando Promesas
function submitComment() {
    if (!currentForoId) {
        console.error("No hay un foro seleccionado para comentar.");
        return;
    }
    
    const comentarioInput = document.getElementById('newPostInput');
    if (!comentarioInput) return;
    const contenido = comentarioInput.value.trim();
    
    if (!contenido) {
        alert('Por favor, escribe un comentario antes de publicar.');
        return;
    }

     const submitButton = document.getElementById('submitCommentBtn');
     if(!submitButton) return;
     submitButton.disabled = true;
     submitButton.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Publicando...';
    
    fetch(`/api/foros/${currentForoId}/comentarios`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
             // Añadir token de autorización si es necesario
        },
        body: JSON.stringify(contenido) 
    })
    .then(function(response) {
        if (!response.ok) {
            return response.text().then(function(errorText){
                throw new Error(`Error al publicar comentario: ${response.status} - ${errorText}`);
            });
        }
        
        // Respuesta exitosa, recargar los comentarios
        console.log("Comentario publicado con éxito. Recargando foro...");
        comentarioInput.value = ''; // Limpiar input
        return openForo(currentForoId); // Recargar para simplicidad
        
    })
    .catch(function(error) {
        console.error('Error al enviar comentario:', error);
        alert(`Error al publicar el comentario: ${error.message}`);
        submitButton.disabled = false;
        submitButton.innerHTML = '<i class="fas fa-paper-plane"></i> Publicar';
    });
}

// Funciones de interacción con los posts
function togglePostMenu(btn) {
    const menu = btn.nextElementSibling;
    if (!menu || !menu.classList.contains('post-menu')) return;

    // Cerrar otros menús usando bucle for
    const openMenus = document.querySelectorAll('.post-menu.show');
    for(let i = 0; i < openMenus.length; i++){
        if (openMenus[i] !== menu) {
            openMenus[i].classList.remove('show');
        }
    }
    menu.classList.toggle('show');
}

// Función para manejar likes
function toggleLike(btn, entityId, isReply) {
    isReply = isReply || false; // Valor por defecto si no se pasa
    console.log(`Toggle like para ${isReply ? 'respuesta' : 'comentario'} ID: ${entityId}`);
    
    // Prevenir reacciones dobles mientras se procesa la petición
    if (btn.disabled) return;
    btn.disabled = true;
    
    // Obtener el ID del foro de la variable global currentForoId
    if (!currentForoId) {
        console.error('No se encontró el ID del foro actual');
        btn.disabled = false;
        return;
    }

    // Construir la URL del endpoint según si es comentario o respuesta
    let endpoint;
    if (isReply) {
        const comentarioId = btn.closest('.post').closest('.replies-container').dataset.commentId;
        if (!comentarioId) {
            console.error('No se encontró el ID del comentario padre');
            btn.disabled = false;
            return;
        }
        endpoint = `/api/foros/${currentForoId}/comentarios/${comentarioId}/respuestas/${entityId}/like`;
    } else {
        endpoint = `/api/foros/${currentForoId}/comentarios/${entityId}/like`;
    }

    // Hacer la petición al backend
    fetch(endpoint, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        }
    })
    .then(response => {
        if (!response.ok) {
            return response.text().then(text => {
                throw new Error(text || 'Error al actualizar el like');
            });
        }
        return response.json();
    })
    .then(foroActualizado => {
        // Actualizar la UI con los nuevos datos
        const icon = btn.querySelector('i');
        const counterSpan = btn.querySelector('span');
        let likes = parseInt(btn.dataset.likes || '0');
        
        // Cambiar el estado del botón y actualizar el contador
        if (btn.classList.contains('liked')) {
            icon.classList.replace('fas', 'far');
            likes = Math.max(0, likes - 1);
            btn.classList.remove('liked');
        } else {
            icon.classList.replace('far', 'fas');
            likes++;
            btn.classList.add('liked');
        }
        
        btn.dataset.likes = likes;
        counterSpan.textContent = `${likes} Me gusta`;
        
        // Actualizar el foro actual con los nuevos datos
        currentForo = foroActualizado;
    })
    .catch(error => {
        console.error('Error:', error);
        alert(`Error al actualizar el like: ${error.message}`);
    })
    .finally(() => {
        // Rehabilitar el botón después de la petición
        btn.disabled = false;
    });
}

// Funciones de edición y eliminación (a implementar)
function editComment(commentId) {
    if (!currentForoId || !commentId) {
        console.error("Falta ID de foro o comentario para editar.");
        return;
    }
    
    const commentElement = document.querySelector(`.post[data-comment-id="${commentId}"]`);
    if (!commentElement) {
        console.error("No se encontró el elemento del comentario para editar.");
        return;
    }
    
    const contentElement = commentElement.querySelector('.post-content p');
    if (!contentElement) return;
    
    const currentContent = contentElement.textContent || '';
    
    // Reemplazar el contenido con un formulario de edición
    const contentContainer = commentElement.querySelector('.post-content');
    const originalContent = contentContainer.innerHTML;
    
    // Guardar el contenido original como atributo de datos para cancelar
    commentElement.dataset.originalContent = originalContent;
    
    contentContainer.innerHTML = `
        <textarea class="edit-comment-input" rows="4">${currentContent}</textarea>
        <div class="edit-actions">
            <button class="edit-submit-btn" onclick="submitEditComment('${commentId}', this)">
                <i class="fas fa-check"></i> Guardar
            </button>
            <button class="edit-cancel-btn" onclick="cancelEdit('${commentId}')">
                <i class="fas fa-times"></i> Cancelar
            </button>
        </div>
    `;
    
    // Enfocar el textarea y colocar cursor al final
    const textarea = contentContainer.querySelector('textarea');
    if (textarea) {
        textarea.focus();
        textarea.selectionStart = textarea.value.length;
    }
    
    // Ocultar el menú de acciones si está abierto
    const openMenu = commentElement.querySelector('.post-menu.show');
    if (openMenu) openMenu.classList.remove('show');
}

// Función para cancelar la edición y restaurar el contenido original
function cancelEdit(commentId) {
    const commentElement = document.querySelector(`.post[data-comment-id="${commentId}"]`);
    if (!commentElement) return;
    
    const originalContent = commentElement.dataset.originalContent;
    if (!originalContent) return;
    
    const contentContainer = commentElement.querySelector('.post-content');
    if (contentContainer) {
        contentContainer.innerHTML = originalContent;
    }
}

// Función para enviar un comentario editado
function submitEditComment(commentId, button) {
    if (!currentForoId || !commentId) {
        console.error("Falta ID de foro o comentario para enviar edición.");
        return;
    }
    
    const commentElement = document.querySelector(`.post[data-comment-id="${commentId}"]`);
    if (!commentElement) return;
    
    const textarea = commentElement.querySelector('.edit-comment-input');
    if (!textarea) return;
    
    const nuevoContenido = textarea.value.trim();
    if (!nuevoContenido) {
        alert('El comentario no puede estar vacío.');
        return;
    }
    
    // Deshabilitar el botón y mostrar carga
    button.disabled = true;
    button.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Guardando...';
    
    // Enviar solo el contenido al servidor
    fetch(`/api/foros/${currentForoId}/comentarios/${commentId}`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(nuevoContenido)
    })
    .then(function(response) {
        if (!response.ok) {
            return response.text().then(function(errorText) {
                throw new Error(`Error al editar comentario: ${response.status} - ${errorText}`);
            });
        }
        return response.json();
    })
    .then(function(foroActualizado) {
        console.log("Comentario editado con éxito");
        
        // Actualizar la vista inmediatamente sin depender del servidor
        const contentContainer = commentElement.querySelector('.post-content');
        if (contentContainer) {
            contentContainer.innerHTML = `<p>${nuevoContenido}</p>`;
        }
        
        // Actualizar el objeto actual del foro con la respuesta del servidor
        if (foroActualizado) {
            currentForo = foroActualizado;
        }
        
        // Aquí no necesitamos recargar todo el foro, ya hemos actualizado la vista
    })
    .catch(function(error) {
        console.error('Error al editar comentario:', error);
        alert(`No se pudo editar el comentario: ${error.message}`);
        // Restaurar el formulario para que el usuario pueda intentar de nuevo
        button.disabled = false;
        button.innerHTML = '<i class="fas fa-check"></i> Guardar';
    });
}

// Eliminar comentario usando Promesas
function deleteComment(commentId) {
     if (!currentForoId || !commentId) {
        console.error("Falta ID de foro o comentario para eliminar.");
        return;
    }
    
    if (!confirm('¿Estás seguro de que quieres eliminar este comentario? Esta acción no se puede deshacer.')) {
        return;
    }
    
    console.log(`Intentando eliminar comentario ${commentId} del foro ${currentForoId}`);
    
    fetch(`/api/foros/${currentForoId}/comentarios/${commentId}`, {
        method: 'DELETE',
        headers: {
            // Añadir token de autorización si es necesario
        }
    })
    .then(function(response) {
        if (!response.ok) {
             return response.text().then(function(errorText) {
                 let errorMsg = `Error al eliminar comentario: ${response.status}`;
                 if (errorText) errorMsg += ` - ${errorText}`;
                 throw new Error(errorMsg);
             }).catch(function() { // Si no hay cuerpo de error
                 throw new Error(`Error al eliminar comentario: ${response.status}`);
             });
        }
        // Si la eliminación fue exitosa
        console.log(`Comentario ${commentId} eliminado con éxito.`);
        const commentElement = document.querySelector(`.post[data-comment-id="${commentId}"]`);
        if (commentElement) {
             commentElement.style.transition = 'opacity 0.5s ease';
             commentElement.style.opacity = '0';
             setTimeout(function() { commentElement.remove(); }, 500);
        } else {
             // Si no se encuentra, recargar por si acaso
             openForo(currentForoId);
        }
    })
    .catch(function(error) {
        console.error('Error al eliminar comentario:', error);
        alert(`No se pudo eliminar el comentario: ${error.message}`);
    });
}

// Utilidad para calcular tiempo transcurrido
function calcularTiempoTranscurrido(fecha) {
     if (!(fecha instanceof Date) || isNaN(fecha.getTime())) {
         // Intentar parsear si es string
         if (typeof fecha === 'string') {
             fecha = new Date(fecha);
             if (isNaN(fecha.getTime())) {
                 return 'Fecha inválida';
             }
         } else {
              return 'Fecha inválida';
         }
     }
    const ahora = new Date();
    const diferencia = (ahora.getTime() - fecha.getTime()) / 1000;
    
    const segundosEnMinuto = 60;
    const segundosEnHora = 3600;
    const segundosEnDia = 86400;
    const segundosEnMes = 2592000; // Aprox 30 días
    const segundosEnAno = 31536000;

    if (diferencia < segundosEnMinuto) {
        return 'hace unos segundos';
    } else if (diferencia < segundosEnHora) {
        const minutos = Math.floor(diferencia / segundosEnMinuto);
        return `hace ${minutos} minuto${minutos > 1 ? 's' : ''}`;
    } else if (diferencia < segundosEnDia) {
        const horas = Math.floor(diferencia / segundosEnHora);
        return `hace ${horas} hora${horas > 1 ? 's' : ''}`;
    } else if (diferencia < segundosEnMes) {
        const dias = Math.floor(diferencia / segundosEnDia);
        return `hace ${dias} día${dias > 1 ? 's' : ''}`;
    } else if (diferencia < segundosEnAno) {
         const meses = Math.floor(diferencia / segundosEnMes);
         return `hace ${meses} mes${meses > 1 ? 'es' : ''}`;
    } else {
        const anos = Math.floor(diferencia / segundosEnAno);
        return `hace ${anos} año${anos > 1 ? 's' : ''}`;
    }
}

// Cerrar menús y formularios al hacer clic fuera
document.addEventListener('click', function(e) {
    // Cerrar menús de acciones
    if (!e.target.closest('.post-actions')) {
        const openMenus = document.querySelectorAll('.post-menu.show');
        for(let i = 0; i < openMenus.length; i++){
            openMenus[i].classList.remove('show');
        }
    }
    // Cerrar formulario de respuesta
     if (!e.target.closest('.reply-form') && !e.target.closest('.interaction-btn[onclick^="showReplyForm"]')) {
         const openForms = document.querySelectorAll('.reply-form');
         for(let i = 0; i < openForms.length; i++){
             openForms[i].remove();
         }
     }
});

// Cerrar modal del foro al hacer clic fuera del contenido
let foroDetailModal = document.getElementById('foroDetail');
if(foroDetailModal) {
    foroDetailModal.addEventListener('click', function(e) {
        // Si el clic fue en el fondo (el propio modal) y no en su contenido
        if (e.target === e.currentTarget) {
            closeForo();
        }
    });
}

// Event listener para el botón de cerrar explícito (aunque ya existe onclick)
const closeButton = document.querySelector('#foroDetail .close-btn');
if(closeButton && typeof closeForo === 'function') { // Asegurar que closeForo esté definida
     closeButton.addEventListener('click', closeForo);
}

// Función para reportar un comentario como inapropiado
function reportComment(commentId) {
    if (!currentForoId || !commentId) {
        console.error("Falta ID de foro o comentario para reportar.");
        return;
    }
    
    // Pedir confirmación al usuario
    if (!confirm('¿Estás seguro de que deseas reportar este comentario por contenido inapropiado?')) {
        return;
    }
    
    // Solicitar motivo del reporte
    const motivo = prompt('Por favor, indica el motivo del reporte:', 'Contenido inapropiado');
    if (motivo === null) { // Usuario canceló
        return;
    }
    
    // Mostrar indicación visual de que se está procesando
    const commentElement = document.querySelector(`.post[data-comment-id="${commentId}"]`);
    if (commentElement) {
        commentElement.classList.add('processing');
        
        // Deshabilitar el botón de reportar para evitar múltiples reportes
        const reportBtn = commentElement.querySelector('.report-btn');
        if (reportBtn) {
            reportBtn.disabled = true;
        }
    }
    
    // Llamar a la API para reportar el comentario
    fetch(`/api/foros/${currentForoId}/comentarios/${commentId}/reportar`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(motivo)
    })
    .then(response => {
        if (!response.ok) {
            throw new Error(`Error ${response.status}: ${response.statusText}`);
        }
        return response.json();
    })
    .then(data => {
        // Quitar clase de procesamiento
        if (commentElement) {
            commentElement.classList.remove('processing');
            
            // Añadir clase de reportado
            commentElement.classList.add('reported');
            
            // Añadir badge de reportado si no existe
            if (!commentElement.querySelector('.reported-badge')) {
                const contentDiv = commentElement.querySelector('.post-content');
                if (contentDiv) {
                    const reportedBadge = document.createElement('div');
                    reportedBadge.className = 'reported-badge';
                    reportedBadge.innerHTML = '<i class="fas fa-flag"></i> Reportado';
                    contentDiv.insertBefore(reportedBadge, contentDiv.firstChild);
                }
            }
        }
        
        // Mostrar mensaje de éxito
        alert(data.mensaje || 'El comentario ha sido reportado correctamente. Un administrador lo revisará pronto.');
    })
    .catch(error => {
        console.error("Error reportando comentario:", error);
        
        // Quitar clase de procesamiento
        if (commentElement) {
            commentElement.classList.remove('processing');
            
            // Restaurar botón
            const reportBtn = commentElement.querySelector('.report-btn');
            if (reportBtn) {
                reportBtn.disabled = false;
            }
        }
        
        // Mostrar mensaje de error
        alert(`No se pudo reportar el comentario: ${error.message}`);
    });
}

// ====== FUNCIONES DE ADMINISTRACIÓN Y MODERACIÓN ======

// Mostrar ventanas modales
function showBanUserModal() {
    loadUsers();
    document.getElementById('banUserModal').classList.add('active');
    document.body.style.overflow = 'hidden'; // Evitar scroll
}

function showWordFilterModal() {
    loadFilteredWords();
    document.getElementById('wordFilterModal').classList.add('active');
    document.body.style.overflow = 'hidden';
}

function showAnnouncementModal() {
    // Establecer fecha predeterminada 7 días en el futuro
    const defaultDate = new Date();
    defaultDate.setDate(defaultDate.getDate() + 7);
    document.getElementById('announcementExpiration').valueAsDate = defaultDate;
    
    document.getElementById('announcementModal').classList.add('active');
    document.body.style.overflow = 'hidden';
}

function showModerationQueueModal() {
    loadReportedItems();
    document.getElementById('moderationQueueModal').classList.add('active');
    document.body.style.overflow = 'hidden';
}

// Cerrar ventanas modales
function closeModal(modalId) {
    document.getElementById(modalId).classList.remove('active');
    document.body.style.overflow = ''; // Restaurar scroll
}

// Cerrar todos los modales
function closeAllModals() {
    const modals = document.querySelectorAll('.moderation-modal, .foro-detail, .confirm-modal');
    modals.forEach(modal => {
        modal.classList.remove('active', 'show');
    });
    document.body.style.overflow = '';
}

// Evento global para cerrar modales al hacer clic fuera
document.addEventListener('DOMContentLoaded', function() {
    const modals = document.querySelectorAll('.moderation-modal, .foro-detail, .confirm-modal');
    
    modals.forEach(modal => {
        modal.addEventListener('click', function(e) {
            // Si el clic es en el fondo oscuro (el modal mismo) y no en su contenido
            if (e.target === modal) {
                modal.classList.remove('active', 'show');
                // Restaurar scroll solo si no hay otros modales activos
                if (!document.querySelector('.moderation-modal.active, .foro-detail.active, .confirm-modal.active')) {
                    document.body.style.overflow = '';
                }
            }
        });
    });
    
    // Botones de cerrar
    const closeButtons = document.querySelectorAll('.moderation-modal-close, .close-btn');
    closeButtons.forEach(button => {
        button.addEventListener('click', function() {
            const modal = button.closest('.moderation-modal, .foro-detail, .confirm-modal');
            if (modal) {
                modal.classList.remove('active', 'show');
                // Restaurar scroll solo si no hay otros modales activos
                if (!document.querySelector('.moderation-modal.active, .foro-detail.active, .confirm-modal.active')) {
                    document.body.style.overflow = '';
                }
            }
        });
    });
    
    // Inicializar panel de administración si existen los elementos
    if (document.getElementById('totalForos')) {
        loadStats();
    }
});

// Carga de usuarios para el ban
function loadUsers() {
    const userSelect = document.getElementById('banUserId');
    userSelect.innerHTML = '<option value="">Cargando usuarios...</option>';
    
    fetch('/api/usuarios/activos')
        .then(response => response.json())
        .then(users => {
            userSelect.innerHTML = '';
            users.forEach(user => {
                const option = document.createElement('option');
                option.value = user.id;
                option.textContent = user.nombre + ' (' + user.email + ')';
                userSelect.appendChild(option);
            });
        })
        .catch(error => {
            console.error('Error al cargar usuarios:', error);
            userSelect.innerHTML = '<option value="">Error al cargar usuarios</option>';
        });
}

// Bannear usuario
function banUser() {
    const userId = document.getElementById('banUserId').value;
    const reason = document.getElementById('banReason').value;
    const duration = document.getElementById('banDuration').value;
    
    if (!userId) {
        alert('Por favor seleccione un usuario');
        return;
    }
    
    if (!reason) {
        alert('Por favor proporcione una razón para el ban');
        return;
    }
    
    const banData = {
        userId: userId,
        reason: reason,
        durationHours: parseInt(duration)
    };
    
    fetch('/api/moderacion/ban', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(banData)
    })
    .then(response => {
        if (response.ok) {
            return response.json();
        }
        throw new Error('Error al bannear usuario');
    })
    .then(data => {
        alert('Usuario banneado correctamente');
        closeModal('banUserModal');
    })
    .catch(error => {
        console.error('Error:', error);
        alert('Error al bannear usuario: ' + error.message);
    });
}

// Cargar palabras filtradas
function loadFilteredWords() {
    const wordsList = document.getElementById('filteredWordsList');
    wordsList.innerHTML = '<li>Cargando palabras filtradas...</li>';
    
    fetch('/api/moderacion/palabras-filtradas')
        .then(response => response.json())
        .then(words => {
            wordsList.innerHTML = '';
            if (words.length === 0) {
                wordsList.innerHTML = '<li class="no-words">No hay palabras filtradas</li>';
                return;
            }
            
            words.forEach(word => {
                const li = document.createElement('li');
                li.innerHTML = `
                    <span>${word}</span>
                    <button class="btn-delete" onclick="removeFilteredWord('${word}')">
                        <i class="fas fa-trash"></i>
                    </button>
                `;
                wordsList.appendChild(li);
            });
        })
        .catch(error => {
            console.error('Error al cargar palabras filtradas:', error);
            wordsList.innerHTML = '<li>Error al cargar palabras filtradas</li>';
        });
}

// Añadir palabra filtrada
function addFilteredWord() {
    const wordInput = document.getElementById('newFilteredWord');
    const word = wordInput.value.trim();
    
    if (!word) {
        alert('Por favor ingrese una palabra para filtrar');
        return;
    }
    
    // Verificar si la palabra ya está en la lista
    const items = document.querySelectorAll('#filteredWordsList li span');
    for (let i = 0; i < items.length; i++) {
        if (items[i].textContent === word) {
            alert('Esta palabra ya está en la lista de filtros');
            wordInput.value = '';
            return;
        }
    }
    
    // Añadir a la lista visual
    const wordsList = document.getElementById('filteredWordsList');
    const noWordsItem = wordsList.querySelector('.no-words');
    if (noWordsItem) {
        wordsList.innerHTML = '';
    }
    
    const li = document.createElement('li');
    li.innerHTML = `
        <span>${word}</span>
        <button class="btn-delete" onclick="removeFilteredWord('${word}')">
            <i class="fas fa-trash"></i>
        </button>
    `;
    wordsList.appendChild(li);
    
    // Limpiar el input
    wordInput.value = '';
}

// Eliminar palabra filtrada
function removeFilteredWord(word) {
    // Solo eliminar visualmente, los cambios se guardarán al hacer clic en Guardar
    const items = document.querySelectorAll('#filteredWordsList li');
    items.forEach(item => {
        if (item.querySelector('span').textContent === word) {
            item.remove();
        }
    });
    
    // Si no quedan palabras, mostrar mensaje
    if (document.querySelectorAll('#filteredWordsList li').length === 0) {
        document.getElementById('filteredWordsList').innerHTML = '<li class="no-words">No hay palabras filtradas</li>';
    }
}

// Guardar cambios en palabras filtradas
function saveFilteredWords() {
    const items = document.querySelectorAll('#filteredWordsList li span');
    const words = Array.from(items).map(item => item.textContent);
    
    // Filtrar el elemento "No hay palabras filtradas" si existe
    const filteredWords = words.filter(word => word !== 'No hay palabras filtradas');
    
    fetch('/api/moderacion/palabras-filtradas', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(filteredWords)
    })
    .then(response => {
        if (response.ok) {
            alert('Palabras filtradas guardadas correctamente');
            closeModal('wordFilterModal');
        } else {
            throw new Error('Error al guardar palabras filtradas');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        alert('Error al guardar palabras filtradas: ' + error.message);
    });
}

// Crear anuncio
function createAnnouncement() {
    const title = document.getElementById('announcementTitle').value.trim();
    const content = document.getElementById('announcementContent').value.trim();
    const expiration = document.getElementById('announcementExpiration').value;
    const important = document.getElementById('announcementImportant').checked;
    
    if (!title) {
        alert('Por favor ingrese un título para el anuncio');
        return;
    }
    
    if (!content) {
        alert('Por favor ingrese el contenido del anuncio');
        return;
    }
    
    if (!expiration) {
        alert('Por favor seleccione una fecha de expiración');
        return;
    }
    
    const announcementData = {
        title: title,
        content: content,
        expirationDate: expiration,
        important: important
    };
    
    fetch('/api/moderacion/anuncios', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(announcementData)
    })
    .then(response => {
        if (response.ok) {
            alert('Anuncio creado correctamente');
            closeModal('announcementModal');
            // Limpiar formulario
            document.getElementById('announcementTitle').value = '';
            document.getElementById('announcementContent').value = '';
            document.getElementById('announcementImportant').checked = false;
        } else {
            throw new Error('Error al crear anuncio');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        alert('Error al crear anuncio: ' + error.message);
    });
}

// Cargar elementos reportados
function loadReportedItems() {
    const reportsList = document.getElementById('reportedItemsList');
    reportsList.innerHTML = '<div class="loading">Cargando elementos reportados...</div>';
    
    const filter = document.getElementById('reportTypeFilter').value;
    let url = '/api/moderacion/reportes';
    
    if (filter !== 'all') {
        url += '?tipo=' + filter;
    }
    
    fetch(url)
        .then(response => response.json())
        .then(reports => {
            reportsList.innerHTML = '';
            
            if (reports.length === 0) {
                reportsList.innerHTML = '<div class="no-reports">No hay elementos reportados</div>';
                return;
            }
            
            reports.forEach(report => {
                const reportItem = document.createElement('div');
                reportItem.className = 'reported-item';
                
                let contentPreview = '';
                if (report.tipo === 'comentario') {
                    contentPreview = `<p class="content-preview">${report.contenido}</p>`;
                } else if (report.tipo === 'foro') {
                    contentPreview = `<p class="content-preview">Título: ${report.titulo}</p>`;
                }
                
                reportItem.innerHTML = `
                    <div class="report-header">
                        <span class="report-type ${report.razon.toLowerCase()}">${report.razon}</span>
                        <span class="report-date">${formatDate(report.fechaReporte)}</span>
                    </div>
                    <h4>${report.tipo.charAt(0).toUpperCase() + report.tipo.slice(1)} reportado</h4>
                    ${contentPreview}
                    <div class="report-info">
                        <span>Reportado por: ${report.reportadoPor}</span>
                        <span>Usuario responsable: ${report.autorOriginal}</span>
                    </div>
                    <div class="report-actions">
                        <button class="btn-view" onclick="viewReportedItem(${report.id})">
                            <i class="fas fa-eye"></i> Ver
                        </button>
                        <button class="btn-delete" onclick="deleteReportedItem(${report.id})">
                            <i class="fas fa-trash"></i> Eliminar
                        </button>
                        <button class="btn-approve" onclick="approveReport(${report.id})">
                            <i class="fas fa-check"></i> Aprobar reporte
                        </button>
                        <button class="btn-reject" onclick="rejectReport(${report.id})">
                            <i class="fas fa-times"></i> Rechazar reporte
                        </button>
                    </div>
                `;
                
                reportsList.appendChild(reportItem);
            });
        })
        .catch(error => {
            console.error('Error al cargar elementos reportados:', error);
            reportsList.innerHTML = '<div class="error">Error al cargar elementos reportados</div>';
        });
}

// Ver elemento reportado
function viewReportedItem(id) {
    // Abrir en una nueva ventana o mostrar en detalle
    window.open(`/moderacion/reportes/${id}`, '_blank');
}

// Eliminar elemento reportado
function deleteReportedItem(id) {
    if (!confirm('¿Está seguro de eliminar este elemento reportado? Esta acción eliminará el contenido original.')) {
        return;
    }
    
    fetch(`/api/moderacion/reportes/${id}/eliminar`, {
        method: 'POST'
    })
    .then(response => {
        if (response.ok) {
            loadReportedItems(); // Recargar lista
            alert('Elemento eliminado correctamente');
        } else {
            throw new Error('Error al eliminar elemento');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        alert('Error al eliminar elemento: ' + error.message);
    });
}

// Aprobar reporte
function approveReport(id) {
    fetch(`/api/moderacion/reportes/${id}/aprobar`, {
        method: 'POST'
    })
    .then(response => {
        if (response.ok) {
            loadReportedItems(); // Recargar lista
            alert('Reporte aprobado correctamente');
        } else {
            throw new Error('Error al aprobar reporte');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        alert('Error al aprobar reporte: ' + error.message);
    });
}

// Rechazar reporte
function rejectReport(id) {
    fetch(`/api/moderacion/reportes/${id}/rechazar`, {
        method: 'POST'
    })
    .then(response => {
        if (response.ok) {
            loadReportedItems(); // Recargar lista
            alert('Reporte rechazado correctamente');
        } else {
            throw new Error('Error al rechazar reporte');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        alert('Error al rechazar reporte: ' + error.message);
    });
}

// Utilidad para formatear fechas
function formatDate(dateString) {
    const options = { year: 'numeric', month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' };
    return new Date(dateString).toLocaleDateString('es-ES', options);
}

// Actualizar filtro de reportes
document.addEventListener('DOMContentLoaded', function() {
    const reportTypeFilter = document.getElementById('reportTypeFilter');
    if (reportTypeFilter) {
        reportTypeFilter.addEventListener('change', loadReportedItems);
    }
});

// ====== FUNCIONES PARA REPORTES DE CONTENIDO ======

// Mostrar modal de reporte
function showReportModal(itemId, itemType) {
    // Limpiar campos
    document.getElementById('reportReason').value = '';
    document.getElementById('reportDescription').value = '';
    
    // Establecer los datos del ítem a reportar
    document.getElementById('reportItemId').value = itemId;
    document.getElementById('reportItemType').value = itemType;
    
    // Mostrar el modal
    document.getElementById('reportContentModal').classList.add('active');
    document.body.style.overflow = 'hidden';
}

// Enviar reporte
function submitReport() {
    const itemId = document.getElementById('reportItemId').value;
    const itemType = document.getElementById('reportItemType').value;
    const reason = document.getElementById('reportReason').value;
    const description = document.getElementById('reportDescription').value;
    
    if (!reason) {
        alert('Por favor seleccione una razón para el reporte');
        return;
    }
    
    const reportData = {
        itemId: itemId,
        itemType: itemType,
        reason: reason,
        description: description
    };
    
    fetch('/api/moderacion/reportar', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(reportData)
    })
    .then(response => {
        if (response.ok) {
            alert('Reporte enviado correctamente. Gracias por ayudar a mantener la comunidad.');
            closeModal('reportContentModal');
        } else {
            throw new Error('Error al enviar reporte');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        alert('Error al enviar reporte: ' + error.message);
    });
}

// Agregar botones de reporte a cada comentario y post
function addReportButtons() {
    // Para comentarios existentes
    const comments = document.querySelectorAll('.comment');
    comments.forEach(comment => {
        // Verificar si ya tiene botón de reporte
        if (!comment.querySelector('.report-btn')) {
            const commentId = comment.getAttribute('data-id');
            const commentHeader = comment.querySelector('.comment-header');
            
            if (commentHeader) {
                // Crear div para acciones si no existe
                let actionsDiv = commentHeader.querySelector('.comment-actions');
                if (!actionsDiv) {
                    actionsDiv = document.createElement('div');
                    actionsDiv.className = 'comment-actions';
                    commentHeader.appendChild(actionsDiv);
                }
                
                // Añadir botón de reporte
                const reportBtn = document.createElement('button');
                reportBtn.className = 'report-btn';
                reportBtn.innerHTML = '<i class="fas fa-flag"></i> Reportar';
                reportBtn.onclick = function() {
                    showReportModal(commentId, 'comentario');
                };
                
                actionsDiv.appendChild(reportBtn);
            }
        }
    });
    
    // Para publicaciones/foros
    const posts = document.querySelectorAll('.post, .foro-item');
    posts.forEach(post => {
        if (!post.querySelector('.report-btn')) {
            const postId = post.getAttribute('data-id');
            const postHeader = post.querySelector('.post-header, .foro-header');
            
            if (postHeader) {
                let actionsDiv = postHeader.querySelector('.post-actions, .foro-actions');
                if (!actionsDiv) {
                    actionsDiv = document.createElement('div');
                    actionsDiv.className = post.classList.contains('post') ? 'post-actions' : 'foro-actions';
                    postHeader.appendChild(actionsDiv);
                }
                
                const reportBtn = document.createElement('button');
                reportBtn.className = 'report-btn';
                reportBtn.innerHTML = '<i class="fas fa-flag"></i> Reportar';
                reportBtn.onclick = function() {
                    showReportModal(postId, 'foro');
                };
                
                actionsDiv.appendChild(reportBtn);
            }
        }
    });
}

// Ejecutar cuando la página se carga
document.addEventListener('DOMContentLoaded', function() {
    // Inicializar botones de reporte
    addReportButtons();
    
    // Re-agregar botones de reporte cuando se carguen nuevos comentarios
    const commentsList = document.getElementById('commentsList');
    if (commentsList) {
        // Observar cambios en la lista de comentarios para añadir botones de reporte a nuevos comentarios
        const observer = new MutationObserver(function() {
            addReportButtons();
        });
        
        observer.observe(commentsList, { childList: true, subtree: true });
    }
});

// ====== FUNCIONES PARA ESTADÍSTICAS ======

// Cargar estadísticas
function loadStats() {
    fetch('/api/admin/foro/estadisticas')
        .then(response => response.json())
        .then(data => {
            document.getElementById('totalForos').textContent = formatNumber(data.totalForos);
            document.getElementById('totalComentarios').textContent = formatNumber(data.totalComentarios);
            document.getElementById('reportesPendientes').textContent = formatNumber(data.reportesPendientes);
            document.getElementById('usuariosActivos').textContent = formatNumber(data.usuariosActivos);
            
            // Actualizar las tendencias
            updateTrend('totalForos', data.tendenciaForos);
            updateTrend('totalComentarios', data.tendenciaComentarios);
            updateTrend('reportesPendientes', data.tendenciaReportes);
            updateTrend('usuariosActivos', data.tendenciaUsuarios);
        })
        .catch(error => {
            console.error('Error al cargar estadísticas:', error);
        });
}

// Actualizar indicador de tendencia
function updateTrend(elementId, trend) {
    const element = document.getElementById(elementId).nextElementSibling;
    
    if (element) {
        // Eliminar clases previas
        element.classList.remove('positive', 'negative', 'neutral');
        
        // Establecer clase según tendencia
        if (trend > 0) {
            element.classList.add('positive');
            element.innerHTML = `+${trend}% <span>vs mes anterior</span>`;
        } else if (trend < 0) {
            element.classList.add('negative');
            element.innerHTML = `${trend}% <span>vs mes anterior</span>`;
        } else {
            element.classList.add('neutral');
            element.innerHTML = `0% <span>vs mes anterior</span>`;
        }
    }
}

// Formatear números grandes
function formatNumber(num) {
    if (num >= 1000000) {
        return (num / 1000000).toFixed(1) + 'M';
    } else if (num >= 1000) {
        return (num / 1000).toFixed(1) + 'K';
    }
    return num.toString();
}

// Cargar estadísticas al cargar la página
document.addEventListener('DOMContentLoaded', function() {
    // Cargar estadísticas si existen los elementos
    if (document.getElementById('totalForos')) {
        loadStats();
    }
    
    // Otros inicializadores...
});

// ====== FUNCIONES PARA GESTIÓN DE CATEGORÍAS ======

// Variables globales para categorías
let currentIcon = 'fa-folder';
let currentColor = '#4f46e5';
let editingCategoryId = null;

// Mostrar modal de categorías
function showCategoryModal() {
    loadCategories();
    document.getElementById('categoryManagementModal').classList.add('active');
    document.body.style.overflow = 'hidden';
}

// Cambiar entre pestañas
function switchCategoryTab(tabId) {
    // Desactivar todas las pestañas
    document.querySelectorAll('.category-tab-content').forEach(tab => {
        tab.classList.remove('active');
    });
    
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.classList.remove('active');
    });
    
    // Activar la pestaña seleccionada
    document.getElementById(tabId).classList.add('active');
    document.querySelector(`.tab-btn[onclick="switchCategoryTab('${tabId}')"]`).classList.add('active');
    
    // Ajustar el botón de guardar según la pestaña
    if (tabId === 'categoryList') {
        document.getElementById('saveCategoryBtn').style.display = 'none';
    } else {
        document.getElementById('saveCategoryBtn').style.display = 'block';
    }
}

// Cargar categorías
function loadCategories() {
    const categoriesList = document.getElementById('categoriesList');
    categoriesList.innerHTML = '<tr><td colspan="4" class="loading-td">Cargando categorías...</td></tr>';
    
    fetch('/api/admin/categorias')
        .then(response => response.json())
        .then(categories => {
            categoriesList.innerHTML = '';
            
            if (categories.length === 0) {
                categoriesList.innerHTML = '<tr><td colspan="4" class="empty-td">No hay categorías definidas</td></tr>';
                return;
            }
            
            categories.forEach(category => {
                const row = document.createElement('tr');
                
                row.innerHTML = `
                    <td>
                        <div class="category-name">
                            <i class="fas ${category.icon}" style="color: ${category.color}"></i>
                            <span>${category.nombre}</span>
                        </div>
                    </td>
                    <td>
                        <div class="category-color">
                            <div class="color-preview" style="background-color: ${category.color}"></div>
                            <span>${category.color}</span>
                        </div>
                    </td>
                    <td>${category.forosCount || 0}</td>
                    <td>
                        <div class="actions">
                            <button class="btn-icon btn-edit" onclick="editCategory(${category.id})">
                                <i class="fas fa-edit"></i>
                            </button>
                            <button class="btn-icon btn-delete" onclick="deleteCategory(${category.id})">
                                <i class="fas fa-trash"></i>
                            </button>
                        </div>
                    </td>
                `;
                
                categoriesList.appendChild(row);
            });
        })
        .catch(error => {
            console.error('Error al cargar categorías:', error);
            categoriesList.innerHTML = '<tr><td colspan="4" class="error-td">Error al cargar categorías</td></tr>';
        });
}

// Mostrar/ocultar selector de iconos
function toggleIconPicker() {
    const iconPicker = document.getElementById('iconPicker');
    iconPicker.style.display = iconPicker.style.display === 'none' ? 'grid' : 'none';
}

// Seleccionar icono
function selectIcon(iconClass) {
    currentIcon = iconClass;
    document.getElementById('selectedIcon').className = `fas ${iconClass}`;
    document.querySelector('.selected-icon span').textContent = iconClass;
    
    // Actualizar vista previa
    document.querySelector('#categoryPreview i').className = `fas ${iconClass}`;
    
    // Cerrar selector
    document.getElementById('iconPicker').style.display = 'none';
}

// Actualizar color en tiempo real
document.addEventListener('DOMContentLoaded', function() {
    const colorInput = document.getElementById('categoryColor');
    if (colorInput) {
        colorInput.addEventListener('input', function() {
            currentColor = this.value;
            document.getElementById('colorHex').textContent = this.value;
            document.getElementById('categoryPreview').style.backgroundColor = this.value;
        });
    }
    
    // Actualizar vista previa del nombre de categoría
    const categoryNameInput = document.getElementById('categoryName');
    if (categoryNameInput) {
        categoryNameInput.addEventListener('input', function() {
            document.querySelector('#categoryPreview span').textContent = this.value || 'Nombre de la categoría';
        });
    }
    
    // Buscar categorías
    const categorySearch = document.getElementById('categorySearch');
    if (categorySearch) {
        categorySearch.addEventListener('input', function() {
            const searchTerm = this.value.toLowerCase();
            const rows = document.querySelectorAll('#categoriesList tr');
            
            rows.forEach(row => {
                if (row.querySelector('.category-name')) {
                    const categoryName = row.querySelector('.category-name span').textContent.toLowerCase();
                    
                    if (categoryName.includes(searchTerm)) {
                        row.style.display = '';
                    } else {
                        row.style.display = 'none';
                    }
                }
            });
        });
    }
});

// Guardar categoría
function saveCategory() {
    const name = document.getElementById('categoryName').value.trim();
    const description = document.getElementById('categoryDescription').value.trim();
    const restricted = document.getElementById('categoryRestricted').checked;
    
    if (!name) {
        alert('Por favor ingrese un nombre para la categoría');
        return;
    }
    
    const categoryData = {
        nombre: name,
        descripcion: description,
        color: currentColor,
        icon: currentIcon,
        restringido: restricted
    };
    
    // Si estamos editando, incluir el ID
    if (editingCategoryId) {
        categoryData.id = editingCategoryId;
    }
    
    fetch('/api/admin/categorias', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify(categoryData)
    })
    .then(response => {
        if (response.ok) {
            alert(`Categoría ${editingCategoryId ? 'actualizada' : 'creada'} correctamente`);
            resetCategoryForm();
            switchCategoryTab('categoryList');
            loadCategories();
        } else {
            throw new Error(`Error al ${editingCategoryId ? 'actualizar' : 'crear'} categoría`);
        }
    })
    .catch(error => {
        console.error('Error:', error);
        alert(`Error al ${editingCategoryId ? 'actualizar' : 'crear'} categoría: ${error.message}`);
    });
}

// Resetear formulario de categoría
function resetCategoryForm() {
    document.getElementById('categoryName').value = '';
    document.getElementById('categoryDescription').value = '';
    document.getElementById('categoryColor').value = '#4f46e5';
    document.getElementById('colorHex').textContent = '#4f46e5';
    document.getElementById('categoryRestricted').checked = false;
    
    currentIcon = 'fa-folder';
    currentColor = '#4f46e5';
    editingCategoryId = null;
    
    document.getElementById('selectedIcon').className = 'fas fa-folder';
    document.querySelector('.selected-icon span').textContent = 'fa-folder';
    
    document.getElementById('categoryPreview').style.backgroundColor = '#4f46e5';
    document.querySelector('#categoryPreview i').className = 'fas fa-folder';
    document.querySelector('#categoryPreview span').textContent = 'Nombre de la categoría';
    
    document.getElementById('saveCategoryBtn').textContent = 'Guardar Categoría';
}

// Editar categoría
function editCategory(categoryId) {
    fetch(`/api/admin/categorias/${categoryId}`)
        .then(response => response.json())
        .then(category => {
            // Establecer el ID de la categoría que se está editando
            editingCategoryId = categoryId;
            
            // Llenar formulario con datos de la categoría
            document.getElementById('categoryName').value = category.nombre;
            document.getElementById('categoryDescription').value = category.descripcion || '';
            document.getElementById('categoryColor').value = category.color;
            document.getElementById('colorHex').textContent = category.color;
            document.getElementById('categoryRestricted').checked = category.restringido;
            
            // Actualizar variables e iconos
            currentColor = category.color;
            currentIcon = category.icon;
            
            document.getElementById('selectedIcon').className = `fas ${category.icon}`;
            document.querySelector('.selected-icon span').textContent = category.icon;
            
            // Actualizar vista previa
            document.getElementById('categoryPreview').style.backgroundColor = category.color;
            document.querySelector('#categoryPreview i').className = `fas ${category.icon}`;
            document.querySelector('#categoryPreview span').textContent = category.nombre;
            
            // Cambiar texto del botón
            document.getElementById('saveCategoryBtn').textContent = 'Actualizar Categoría';
            
            // Cambiar a la pestaña de edición
            switchCategoryTab('newCategory');
        })
        .catch(error => {
            console.error('Error al cargar datos de categoría:', error);
            alert('Error al cargar datos de la categoría');
        });
}

// Eliminar categoría
function deleteCategory(categoryId) {
    if (!confirm('¿Está seguro de eliminar esta categoría? Esta acción no se puede deshacer y podría afectar a los foros asociados.')) {
        return;
    }
    
    fetch(`/api/admin/categorias/${categoryId}`, {
        method: 'DELETE'
    })
    .then(response => {
        if (response.ok) {
            alert('Categoría eliminada correctamente');
            loadCategories();
        } else {
            throw new Error('Error al eliminar categoría');
        }
    })
    .catch(error => {
        console.error('Error:', error);
        alert('Error al eliminar categoría: ' + error.message);
    });
}
