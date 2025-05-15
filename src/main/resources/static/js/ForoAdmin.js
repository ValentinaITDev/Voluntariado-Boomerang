// ForoAdmin.js

document.addEventListener('DOMContentLoaded', () => {
  // console.log('ForoAdmin.js cargado - Iniciando script');
  
  // Variables globales
  const stats = window.estadisticasForos || {};
  const currentUser = window.currentUserData || {};
  const apiBase = '/api/admin/foros';

  // Elementos del DOM
  const totalForos = document.getElementById('totalForos');
  const totalComentarios = document.getElementById('totalComentarios');
  const reportesPendientes = document.getElementById('reportesPendientes');
  const usuariosActivos = document.getElementById('usuariosActivos');
  const forosList = document.getElementById('forosList');
  const searchInput = document.getElementById('searchInput');
  const searchButton = document.getElementById('searchButton');
  const filterButtons = document.querySelectorAll('.filter-btn');
  const nuevoForoModal = document.getElementById('nuevoForoModal');
  const cerrarNuevoForoBtn = document.getElementById('cerrarNuevoForoBtn');
  const cancelarNuevoForoBtn = document.getElementById('cancelarNuevoForoBtn');
  const crearNuevoForoBtn = document.getElementById('crearNuevoForoBtn');
  const tituloForo = document.getElementById('tituloForo');
  const descripcionForo = document.getElementById('descripcionForo');
  const foroPublico = document.getElementById('foroPublico');
  const permitirComentarios = document.getElementById('permitirComentarios');
  const moderacionPrevia = document.getElementById('moderacionPrevia');
  const btnNewForum = document.getElementById('btnNewForum');
  const foroDetailModal = document.getElementById('foroDetailModal');
  const closeForoDetailBtn = document.getElementById('closeForoDetailBtn');
  const modalForoTitle = document.getElementById('modalForoTitle');
  const modalForoDescription = document.getElementById('modalForoDescription');
  const modalPostsContainer = document.getElementById('modalPostsContainer');
  const newCommentAdmin = document.getElementById('newCommentAdmin');
  const postAdminCommentBtn = document.getElementById('postAdminCommentBtn');
  const modalLoader = document.getElementById('modalLoader');
  const modalContentContainer = document.getElementById('modalContentContainer');
  const notificationContainer = document.getElementById('notificationContainer');
  const menuToggle = document.getElementById('menuToggle');
  const navLinks = document.querySelector('.nav-links');

  // Verificar que los elementos críticos existen
  // console.log('Verificando elementos DOM:');
  // console.log('- btnNewForum:', btnNewForum ? 'Encontrado' : 'No encontrado');
  // console.log('- nuevoForoModal:', nuevoForoModal ? 'Encontrado' : 'No encontrado');
  // console.log('- foroDetailModal:', foroDetailModal ? 'Encontrado' : 'No encontrado');
  // console.log('- closeForoDetailBtn:', closeForoDetailBtn ? 'Encontrado' : 'No encontrado');
  // console.log('- forosList:', forosList ? 'Encontrado' : 'No encontrado');
  
  let currentFilter = 'todos';
  let currentSearch = '';
  let currentForoId = null;

  // Inicializar
  async function init() {
    await cargarEstadisticas();
    loadForos();
  }

  init();

  // Mostrar notificación
  function showNotification(message, type = 'success') {
    const notification = document.createElement('div');
    notification.className = `notification ${type}`;
    notification.textContent = message;
    notificationContainer.appendChild(notification);
    setTimeout(() => {
      notification.remove();
    }, 5000);
  }

  // Cargar foros
  async function loadForos() {
    forosList.innerHTML = '<div class="loading-indicator"><i class="fas fa-spinner fa-spin"></i> Cargando foros...</div>';
    let fetchUrl;
    const params = new URLSearchParams();

    // Parámetros de paginación y ordenación (pueden ser configurables más adelante)
    params.append('page', '0'); 
    params.append('size', '10'); 

    if (currentSearch) {
      fetchUrl = `${apiBase}/search`;
      params.append('query', currentSearch);
      // Nota: El filtro actual (currentFilter como 'activos', 'reportados') 
      // no se usa directamente con el endpoint /search en el backend actual.
      // La búsqueda opera sobre todos los foros y luego se podrían filtrar en el cliente si fuera necesario,
      // o el backend /search podría extenderse para aceptar un parámetro de filtro.
    } else {
      fetchUrl = `${apiBase}/list`;
      params.append('filter', currentFilter); // El backend para /list espera 'filter'
      params.append('sortBy', 'fechaCreacion'); // Valor por defecto consistente con el backend
      params.append('sortDir', 'desc');       // Valor por defecto consistente con el backend
    }

    const fullUrl = `${fetchUrl}?${params.toString()}`;
    // console.log(`Solicitando foros desde: ${fullUrl}`); // Log para depuración

    try {
      const response = await fetch(fullUrl);
      
      if (!response.ok) {
        let errorMsg = `Error al cargar los foros. Estado: ${response.status} (${response.statusText})`;
        try {
          // Intenta leer el cuerpo del error, podría contener más información
          const errorData = await response.text(); 
          if (errorData) {
            // Limita la longitud del mensaje de errorData para no sobrecargar
            errorMsg += ` - Respuesta del servidor: ${errorData.substring(0, 150)}${errorData.length > 150 ? '...' : ''}`;
          }
        } catch (e) {
          // No se pudo leer el cuerpo del error o no es texto
          // console.warn('No se pudo leer el cuerpo de la respuesta de error al cargar foros:', e);
        }
        throw new Error(errorMsg);
      }
      
      const data = await response.json(); // Esta línea puede fallar si el cuerpo no es JSON válido o está incompleto
      renderForos(data.content); // Acceder a la propiedad 'content' para obtener la lista de foros
    } catch (error) {
      // console.error('Error detallado al cargar foros:', error);
      let userMessage = 'Ocurrió un error al cargar los foros. Por favor, inténtalo de nuevo más tarde.';
      
      if (error instanceof TypeError && error.message === 'Failed to fetch') {
        userMessage = 'No se pudo conectar con el servidor para cargar los foros. Verifica tu conexión y el estado del servidor.';
      } else if (error.message && error.message.includes('Estado:')) {
        // Usa el mensaje más detallado que construimos arriba si el error fue por !response.ok
        userMessage = error.message;
      }
      // Si es otro tipo de error (ej. al parsear JSON), se usa el userMessage genérico.

      forosList.innerHTML = `<p>${userMessage}</p>`;
      showNotification(userMessage, 'error'); // Muestra una notificación más visible
    }
  }

  // Renderizar foros
  function renderForos(foros) {
    if (!foros || !foros.length) { // Comprobar si foros es undefined o vacío
      forosList.innerHTML = '<p>No se encontraron foros.</p>';
      return;
    }
    forosList.innerHTML = '';
    foros.forEach(foro => {
      const foroCard = document.createElement('div');
      foroCard.className = 'foro-card';
      foroCard.innerHTML = `
        <h3>${foro.titulo}</h3>
        <p>${foro.descripcion}</p>
        <div class="foro-meta">
          <span><i class="fas fa-comments"></i> ${foro.totalComentarios}</span>
          <span><i class="fas fa-user"></i> ${foro.creador}</span>
          <span><i class="fas fa-calendar-alt"></i> ${new Date(foro.fechaCreacion).toLocaleDateString()}</span>
        </div>
        <button class="action-btn" data-id="${foro.id}"><i class="fas fa-eye"></i> Ver Detalles</button>
      `;
      foroCard.querySelector('.action-btn').addEventListener('click', () => openForoDetail(foro.id));
      forosList.appendChild(foroCard);
    });
  }

  // Abrir detalle del foro
  async function openForoDetail(id) {
    currentForoId = id;
    foroDetailModal.classList.add('visible');
    
    // Mostrar loader y ocultar contenido
    modalLoader.classList.add('active');
    modalContentContainer.style.display = 'none';
    
    try {
      const response = await fetch(`${apiBase}/${id}`);
      
      if (!response.ok) {
        const errorText = await response.text();
        throw new Error(`Error ${response.status}: ${errorText}`);
      }
      
      const foro = await response.json();
      
      // Actualizar información del foro
      modalForoTitle.textContent = foro.titulo || 'Foro sin título';
      modalForoDescription.innerHTML = procesarContenido(foro.descripcion || 'Sin descripción');
      
      // Cargar estadísticas del foro
      const foroStats = document.createElement('div');
      foroStats.className = 'foro-stats';
      foroStats.innerHTML = `
        <div class="stat-item">
          <i class="fas fa-comments"></i> ${foro.comentarios?.length || 0} comentarios
        </div>
        <div class="stat-item">
          <i class="fas fa-flag"></i> ${foro.reportado ? 'Reportado' : 'Sin reportes'}
        </div>
        <div class="stat-item">
          <i class="fas fa-calendar-alt"></i> Creado: ${formatearFecha(foro.fechaCreacion)}
        </div>
        <div class="stat-item">
          <i class="fas fa-user"></i> Autor: ${foro.autor?.nombre || 'Desconocido'}
        </div>
      `;
      
      // Añadir estadísticas después de la descripción
      const descriptionElement = modalForoDescription;
      if (descriptionElement.nextSibling) {
        modalForoDescription.parentNode.insertBefore(foroStats, descriptionElement.nextSibling);
      } else {
        modalForoDescription.parentNode.appendChild(foroStats);
      }
      
      // Renderizar comentarios
      renderComentarios(foro.comentarios || []);
      
      // Mostrar controles de administración específicos para este foro
      mostrarControlesAdmin(foro);
      
      // Ocultar loader y mostrar contenido
      modalLoader.classList.remove('active');
      modalContentContainer.style.display = 'block';
      
    } catch (error) {
      console.error('Error al cargar el foro:', error);
      
      // Mostrar mensaje de error
      modalLoader.classList.remove('active');
      modalContentContainer.style.display = 'block';
      modalForoTitle.textContent = 'Error al cargar el foro';
      modalForoDescription.textContent = error.message || 'No se pudo cargar la información del foro';
      modalPostsContainer.innerHTML = '<p class="error-msg">Error al cargar los comentarios. Por favor, intenta nuevamente.</p>';
      
      showNotification('Error al cargar el foro: ' + error.message, 'error');
    }
  }

  // Procesar el contenido para mostrar correctamente enlaces y formato
  function procesarContenido(texto) {
    if (!texto) return '';
    
    // Convertir URLs en enlaces clicables
    const urlRegex = /(https?:\/\/[^\s]+)/g;
    let contenidoProcesado = texto.replace(urlRegex, url => `<a href="${url}" target="_blank" rel="noopener noreferrer">${url}</a>`);
    
    // Reemplazar saltos de línea por <br>
    contenidoProcesado = contenidoProcesado.replace(/\n/g, '<br>');
    
    return contenidoProcesado;
  }

  // Mostrar controles de administración específicos para el foro actual
  function mostrarControlesAdmin(foro) {
    // Crear o actualizar panel de acciones administrativas
    let adminPanel = document.getElementById('foro-admin-panel');
    
    if (!adminPanel) {
      adminPanel = document.createElement('div');
      adminPanel.id = 'foro-admin-panel';
      adminPanel.className = 'admin-panel';
      
      // Insertar antes del contenedor de posts
      const postsSection = document.querySelector('.posts-section');
      if (postsSection) {
        postsSection.parentNode.insertBefore(adminPanel, postsSection);
      }
    }
    
    // Actualizar contenido del panel
    adminPanel.innerHTML = `
      <h3><i class="fas fa-shield-alt"></i> Acciones de Administración</h3>
      <div class="admin-actions-grid">
        <div class="admin-action-group estado">
          <h4><i class="fas fa-toggle-on"></i> Estado del Foro</h4>
          <div class="action-buttons">
            <button class="action-btn ${foro.activo ? 'warning' : 'primary'}" id="toggleActivoBtn">
              <i class="fas ${foro.activo ? 'fa-toggle-on' : 'fa-toggle-off'}"></i>
              ${foro.activo ? 'Desactivar' : 'Activar'}
            </button>
            
            <button class="action-btn ${foro.archivado ? 'primary' : 'secondary'}" id="toggleArchivoBtn">
              <i class="fas ${foro.archivado ? 'fa-box-open' : 'fa-archive'}"></i>
              ${foro.archivado ? 'Desarchivar' : 'Archivar'}
            </button>
          </div>
        </div>
        
        <div class="admin-action-group comentarios">
          <h4><i class="fas fa-comments"></i> Comentarios</h4>
          <div class="action-buttons">
            <button class="action-btn ${foro.permiteComentarios ? 'warning' : 'primary'}" id="toggleComentariosBtn">
              <i class="fas ${foro.permiteComentarios ? 'fa-comment-slash' : 'fa-comment'}"></i>
              ${foro.permiteComentarios ? 'Deshabilitar' : 'Habilitar'} Comentarios
            </button>
          </div>
        </div>
        
        <div class="admin-action-group danger-zone">
          <h4><i class="fas fa-exclamation-triangle"></i> Zona de Peligro</h4>
          <div class="action-buttons">
            <button class="action-btn danger" id="eliminarForoBtn">
              <i class="fas fa-trash-alt"></i> Eliminar Foro
            </button>
          </div>
        </div>
      </div>
    `;
    
    // Añadir manejadores de eventos
    document.getElementById('toggleActivoBtn').addEventListener('click', () => toggleEstadoActivo(foro.id, !foro.activo));
    document.getElementById('toggleArchivoBtn').addEventListener('click', () => toggleEstadoArchivado(foro.id, !foro.archivado));
    document.getElementById('toggleComentariosBtn').addEventListener('click', () => togglePermitirComentarios(foro.id, !foro.permiteComentarios));
    document.getElementById('eliminarForoBtn').addEventListener('click', () => confirmarEliminarForo(foro.id));
  }

  // Funciones para gestionar estados del foro
  async function toggleEstadoActivo(foroId, activar) {
    try {
      const response = await fetch(`${apiBase}/${foroId}/estado-activo`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ activo: activar })
      });
      
      if (!response.ok) {
        throw new Error(`Error al ${activar ? 'activar' : 'desactivar'} el foro`);
      }
      
      showNotification(`Foro ${activar ? 'activado' : 'desactivado'} exitosamente`, 'success');
      openForoDetail(foroId);
    } catch (error) {
      console.error(`Error al ${activar ? 'activar' : 'desactivar'} foro:`, error);
      showNotification(`Error: ${error.message}`, 'error');
    }
  }

  async function toggleEstadoArchivado(foroId, archivar) {
    try {
      const response = await fetch(`${apiBase}/${foroId}/archivar`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ archivar: archivar })
      });
      
      if (!response.ok) {
        throw new Error(`Error al ${archivar ? 'archivar' : 'desarchivar'} el foro`);
      }
      
      showNotification(`Foro ${archivar ? 'archivado' : 'desarchivado'} exitosamente`, 'success');
      openForoDetail(foroId);
    } catch (error) {
      console.error(`Error al ${archivar ? 'archivar' : 'desarchivar'} foro:`, error);
      showNotification(`Error: ${error.message}`, 'error');
    }
  }

  async function togglePermitirComentarios(foroId, permitir) {
    try {
      const response = await fetch(`${apiBase}/${foroId}/permitir-comentarios`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ permitir: permitir })
      });
      
      if (!response.ok) {
        throw new Error(`Error al ${permitir ? 'habilitar' : 'deshabilitar'} comentarios en el foro`);
      }
      
      showNotification(`Comentarios ${permitir ? 'habilitados' : 'deshabilitados'} exitosamente`, 'success');
      openForoDetail(foroId);
    } catch (error) {
      console.error(`Error al cambiar estado de comentarios:`, error);
      showNotification(`Error: ${error.message}`, 'error');
    }
  }

  function confirmarEliminarForo(foroId) {
    if (confirm('¿Estás seguro de que deseas eliminar este foro? Esta acción no se puede deshacer y eliminará todos los comentarios asociados.')) {
      eliminarForo(foroId);
    }
  }

  async function eliminarForo(foroId) {
    try {
      const response = await fetch(`${apiBase}/${foroId}`, {
        method: 'DELETE'
      });
      
      if (!response.ok) {
        throw new Error('Error al eliminar el foro');
      }
      
      showNotification('Foro eliminado exitosamente', 'success');
      cerrarModalForoDetalle();
      loadForos(); // Recargar lista de foros
    } catch (error) {
      console.error('Error al eliminar foro:', error);
      showNotification(`Error: ${error.message}`, 'error');
    }
  }

  // Renderizar comentarios
  function renderComentarios(comentarios) {
    if (!comentarios || !comentarios.length) {
      modalPostsContainer.innerHTML = '<p class="no-comments-msg">No hay comentarios en este foro.</p>';
      return;
    }
    
    modalPostsContainer.innerHTML = '';
    
    comentarios.forEach(comentario => {
      // Ignorar comentarios eliminados si es necesario
      // if (comentario.eliminado) return;
      
      const comentarioDiv = document.createElement('div');
      comentarioDiv.className = 'comentario';
      comentarioDiv.setAttribute('data-comentario-id', comentario.id);
      
      // Determinar clases adicionales según el estado del comentario
      if (comentario.reportado) comentarioDiv.classList.add('reportado');
      if (comentario.restringido) comentarioDiv.classList.add('restringido');
      
      // Avatar del autor
      const avatarSrc = (comentario.autor && comentario.autor.imagenPerfil) 
                      ? comentario.autor.imagenPerfil 
                      : '/Perfiles/default-user.png';
      
      // Nombre del autor
      const autorNombre = (comentario.autor && comentario.autor.nombre) 
                        ? comentario.autor.nombre 
                        : 'Usuario Anónimo';
      
      // ID del autor para verificaciones
      const autorId = comentario.autor ? comentario.autor.usuarioId : '';
      
      // Verificar si tiene respuestas
      const tieneRespuestas = comentario.respuestas && comentario.respuestas.length > 0;
      const cantidadRespuestas = tieneRespuestas ? comentario.respuestas.length : 0;
      
      comentarioDiv.innerHTML = `
        <img src="${avatarSrc}" alt="Avatar de ${autorNombre}" class="comentario-avatar">
        <div class="comentario-content">
          <div class="comentario-bubble">
            <div class="comentario-author">${autorNombre}</div>
            <div class="comentario-text">${comentario.restringido 
              ? `<i>[Comentario restringido por un moderador: ${comentario.motivoRestriccion || 'Contenido inapropiado'}]</i>` 
              : comentario.contenido}
            </div>
          </div>
          <div class="comentario-actions">
            <span class="comentario-meta">${formatearFecha(comentario.fechaCreacion)}</span>
            <button class="comentario-btn info" data-accion="responder" data-id="${comentario.id}">
              Responder
            </button>
            <div class="moderacion-options">
              ${comentario.reportado 
                ? `<button class="comentario-btn info" data-accion="aprobar-comentario" data-id="${comentario.id}">
                    Aprobar
                  </button>` 
                : ''}
              ${!comentario.restringido 
                ? `<button class="comentario-btn warn" data-accion="restringir-comentario" data-id="${comentario.id}">
                    Restringir
                  </button>` 
                : `<button class="comentario-btn info" data-accion="quitar-restriccion" data-id="${comentario.id}">
                    Quitar restricción
                  </button>`}
              <button class="comentario-btn danger" data-accion="eliminar-comentario" data-id="${comentario.id}">
                Eliminar
              </button>
              <button class="comentario-btn warn" data-accion="bannear-usuario" data-usuario-id="${autorId}">
                Bannear usuario
              </button>
            </div>
          </div>
          
          ${tieneRespuestas 
            ? `<button class="ver-respuestas-btn" data-id="${comentario.id}" data-count="${cantidadRespuestas}">
                Ver ${cantidadRespuestas} respuesta${cantidadRespuestas !== 1 ? 's' : ''}
              </button>
              <div class="respuestas-container" id="respuestas-${comentario.id}" style="display:none;">
                <!-- Las respuestas se cargarán aquí -->
              </div>` 
            : ''}
            
          <div class="reply-form-container" id="reply-form-${comentario.id}" style="display: none;">
            <div class="comment-form">
              <img src="${currentUser.imagenPerfil || '/Perfiles/default-admin.png'}" 
                  class="user-avatar-input" alt="Avatar Admin">
              <div class="comment-input-container">
                <textarea class="comment-input" placeholder="Escribe una respuesta como administrador..."></textarea>
                <div class="comment-actions">
                  <button class="comment-btn submit" data-accion="enviar-respuesta" data-id="${comentario.id}">
                    <i class="fas fa-paper-plane"></i> Enviar respuesta
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      `;
      modalPostsContainer.appendChild(comentarioDiv);
      
      // Agregar separador después de cada comentario principal
      const separator = document.createElement('div');
      separator.className = 'comentario-separator';
      modalPostsContainer.appendChild(separator);
      
      // Si tiene respuestas, cargarlas
      if (tieneRespuestas) {
        renderRespuestas(comentario.id, comentario.respuestas);
      }
    });
    
    // Añadir eventos a los botones de ver respuestas
    document.querySelectorAll('.ver-respuestas-btn').forEach(btn => {
      btn.addEventListener('click', toggleRespuestas);
    });
  }

  // Función para mostrar/ocultar respuestas
  function toggleRespuestas(event) {
    const btn = event.currentTarget;
    const comentarioId = btn.getAttribute('data-id');
    const respuestasContainer = document.getElementById(`respuestas-${comentarioId}`);
    
    if (respuestasContainer.style.display === 'none') {
      respuestasContainer.style.display = 'block';
      btn.textContent = 'Ocultar respuestas';
    } else {
      respuestasContainer.style.display = 'none';
      const count = btn.getAttribute('data-count');
      btn.textContent = `Ver ${count} respuesta${count !== '1' ? 's' : ''}`;
    }
  }

  // Renderizar respuestas a comentarios
  function renderRespuestas(comentarioId, respuestas) {
    if (!respuestas || !respuestas.length) return;
    
    const respuestasContainer = document.getElementById(`respuestas-${comentarioId}`);
    if (!respuestasContainer) return;
    
    respuestasContainer.innerHTML = '';
    
    respuestas.forEach(respuesta => {
      // Ignorar respuestas eliminadas
      if (respuesta.eliminado) return;
      
      const respuestaDiv = document.createElement('div');
      respuestaDiv.className = 'respuesta';
      respuestaDiv.setAttribute('data-respuesta-id', respuesta.id);
      
      // Determinar clases adicionales según el estado de la respuesta
      if (respuesta.reportada) respuestaDiv.classList.add('reportada');
      if (respuesta.restringida) respuestaDiv.classList.add('restringida');
      
      // Avatar del autor
      const avatarSrc = (respuesta.autor && respuesta.autor.imagenPerfil) 
                      ? respuesta.autor.imagenPerfil 
                      : '/Perfiles/default-user.png';
      
      // Nombre del autor
      const autorNombre = (respuesta.autor && respuesta.autor.nombre) 
                        ? respuesta.autor.nombre 
                        : 'Usuario Anónimo';
      
      // ID del autor para verificaciones
      const autorId = respuesta.autor ? respuesta.autor.usuarioId : '';
      
      respuestaDiv.innerHTML = `
        <img src="${avatarSrc}" alt="Avatar de ${autorNombre}" class="comentario-avatar">
        <div class="comentario-content">
          <div class="comentario-bubble">
            <div class="comentario-author">${autorNombre}</div>
            <div class="comentario-text">${respuesta.restringida 
              ? `<i>[Respuesta restringida por un moderador]</i>` 
              : respuesta.contenido}
            </div>
          </div>
          <div class="comentario-actions">
            <span class="comentario-meta">${formatearFecha(respuesta.fechaCreacion)}</span>
            <div class="moderacion-options">
              ${respuesta.reportada 
                ? `<button class="comentario-btn info" data-accion="aprobar-respuesta" data-id="${respuesta.id}" data-comentario-id="${comentarioId}">
                    Aprobar
                  </button>` 
                : ''}
              ${!respuesta.restringida 
                ? `<button class="comentario-btn warn" data-accion="restringir-respuesta" data-id="${respuesta.id}" data-comentario-id="${comentarioId}">
                    Restringir
                  </button>` 
                : `<button class="comentario-btn info" data-accion="quitar-restriccion-respuesta" data-id="${respuesta.id}" data-comentario-id="${comentarioId}">
                    Quitar restricción
                  </button>`}
              <button class="comentario-btn danger" data-accion="eliminar-respuesta" data-id="${respuesta.id}" data-comentario-id="${comentarioId}">
                Eliminar
              </button>
            </div>
          </div>
        </div>
      `;
      
      respuestasContainer.appendChild(respuestaDiv);
    });
  }

  // Formatear fecha
  function formatearFecha(fechaISO) {
    if (!fechaISO) return 'Fecha desconocida';
    const fecha = new Date(fechaISO);
    return fecha.toLocaleString("es-ES", { dateStyle: "medium", timeStyle: "short" });
  }

  // Publicar comentario
  async function publicarComentario() {
    const contenido = newCommentAdmin.value.trim();
    if (!contenido) {
      showNotification('El comentario no puede estar vacío.', 'error');
      return;
    }
    try {
      const response = await fetch(`${apiBase}/${currentForoId}/comentarios`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ contenido })
      });
      if (!response.ok) throw new Error('Error al publicar el comentario');
      newCommentAdmin.value = '';
      openForoDetail(currentForoId);
      showNotification('Comentario publicado exitosamente.');
    } catch (error) {
      showNotification('Error al publicar el comentario.', 'error');
      // console.error(error);
    }
  }

  // Crear nuevo foro
  async function crearForo() {
    const titulo = tituloForo.value.trim();
    const descripcion = descripcionForo.value.trim();
    if (!titulo || !descripcion) {
      showNotification('Título y descripción son obligatorios.', 'error');
      return;
    }
    
    try {
      // Mostrar indicador de carga
      showNotification('Creando foro...', 'info');
      
      // Construir el objeto con todos los campos necesarios
      const nuevoForo = {
        titulo: titulo,
        contenido: descripcion,  // El campo en el modelo es contenido, no descripcion
        esPublico: foroPublico.checked,
        permiteComentarios: permitirComentarios.checked,
        requiereModeracion: moderacionPrevia.checked,
        activo: true,
        archivado: false,
        etiquetas: ["Admin", "Nuevo"]
      };
      
      console.log('Enviando datos para crear foro:', nuevoForo);
      
      const response = await fetch(apiBase, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(nuevoForo)
      });
      
      if (!response.ok) {
        // Intentar obtener mensaje de error del servidor
        let errorMsg = 'Error al crear el foro';
        try {
          const errorData = await response.json();
          if (errorData && errorData.mensaje) {
            errorMsg = errorData.mensaje;
          }
        } catch (e) {
          // Si no se puede parsear como JSON, usar el texto
          const errorText = await response.text();
          if (errorText) {
            errorMsg += ': ' + errorText;
          }
        }
        throw new Error(errorMsg);
      }
      
      const foroCreado = await response.json();
      console.log('Foro creado exitosamente:', foroCreado);
      
      cerrarModalNuevoForo();
      loadForos();
      showNotification('Foro creado exitosamente.', 'success');
    } catch (error) {
      console.error('Error detallado al crear foro:', error);
      showNotification('Error al crear el foro: ' + error.message, 'error');
    }
  }

  // Cerrar modal de nuevo foro
  function cerrarModalNuevoForo() {
    // console.log('Cerrando modal de nuevo foro');
    nuevoForoModal.classList.remove('visible');
    // console.log('Modal nuevo foro visible:', nuevoForoModal.classList.contains('visible'));
    tituloForo.value = '';
    descripcionForo.value = '';
    foroPublico.checked = true;
    permitirComentarios.checked = true;
    moderacionPrevia.checked = false;
  }

  // Cerrar modal de detalle del foro
  function cerrarModalForoDetalle() {
    // console.log('Cerrando modal de detalle del foro');
    foroDetailModal.classList.remove('visible');
    // console.log('Modal detalle visible:', foroDetailModal.classList.contains('visible'));
    modalForoTitle.textContent = '';
    modalForoDescription.textContent = '';
    modalPostsContainer.innerHTML = '';
    newCommentAdmin.value = '';
  }

  // Manejar filtros
  filterButtons.forEach(button => {
    button.addEventListener('click', () => {
      filterButtons.forEach(btn => btn.classList.remove('active'));
      button.classList.add('active');
      currentFilter = button.getAttribute('data-filter');
      loadForos();
    });
  });

  // Manejar búsqueda
  searchButton.addEventListener('click', () => {
    currentSearch = searchInput.value.trim();
    loadForos();
  });

  searchInput.addEventListener('keypress', e => {
    if (e.key === 'Enter') {
      currentSearch = searchInput.value.trim();
      loadForos();
    }
  });

  // Eventos de botones
  btnNewForum.addEventListener('click', (e) => {
    // console.log('Botón nuevo foro clickeado');
    e.preventDefault();
    nuevoForoModal.classList.add('visible');
    // console.log('Modal nuevo foro visible:', nuevoForoModal.classList.contains('visible'));
  });

  cerrarNuevoForoBtn.addEventListener('click', (e) => {
    // console.log('Botón cerrar nuevo foro clickeado');
    e.preventDefault();
    cerrarModalNuevoForo();
  });
  
  cancelarNuevoForoBtn.addEventListener('click', (e) => {
    // console.log('Botón cancelar nuevo foro clickeado');
    e.preventDefault();
    cerrarModalNuevoForo();
  });
  
  crearNuevoForoBtn.addEventListener('click', (e) => {
    // console.log('Botón crear nuevo foro clickeado');
    e.preventDefault();
    crearForo();
  });
  
  closeForoDetailBtn.addEventListener('click', (e) => {
    // console.log('Botón cerrar detalle foro clickeado');
    e.preventDefault();
    cerrarModalForoDetalle();
  });
  
  postAdminCommentBtn.addEventListener('click', (e) => {
    // console.log('Botón publicar comentario clickeado');
    e.preventDefault();
    publicarComentario();
  });
  
  menuToggle?.addEventListener('click', () => {
    // console.log('Botón menú toggle clickeado');
    navLinks.classList.toggle('active');
    // console.log('Navegación activa:', navLinks.classList.contains('active'));
  });

  // Añadir manejadores de eventos para las acciones de moderación
  modalPostsContainer.addEventListener('click', handleModeracionActions);

  // Función para manejar todas las acciones de moderación
  function handleModeracionActions(event) {
    const target = event.target;
    
    // Solo procesar si el elemento clickeado es un botón con data-accion
    if (!target.matches('button[data-accion]')) return;
    
    const accion = target.getAttribute('data-accion');
    const id = target.getAttribute('data-id');
    const comentarioId = target.getAttribute('data-comentario-id');
    const usuarioId = target.getAttribute('data-usuario-id');
    
    // Ejecutar la acción correspondiente
    switch (accion) {
      case 'responder':
        toggleFormularioRespuesta(id);
        break;
      case 'enviar-respuesta':
        const textarea = target.closest('.comment-form').querySelector('textarea');
        enviarRespuesta(currentForoId, id, textarea.value.trim());
        break;
      case 'restringir-comentario':
        mostrarDialogoRestriccion('comentario', id);
        break;
      case 'restringir-respuesta':
        mostrarDialogoRestriccion('respuesta', id, comentarioId);
        break;
      case 'aprobar-comentario':
        aprobarComentario(currentForoId, id);
        break;
      case 'aprobar-respuesta':
        aprobarRespuesta(currentForoId, comentarioId, id);
        break;
      case 'eliminar-comentario':
        confirmarEliminacion('comentario', id);
        break;
      case 'eliminar-respuesta':
        confirmarEliminacion('respuesta', id, comentarioId);
        break;
      case 'quitar-restriccion':
        quitarRestriccionComentario(currentForoId, id);
        break;
      case 'quitar-restriccion-respuesta':
        quitarRestriccionRespuesta(currentForoId, comentarioId, id);
        break;
      case 'bannear-usuario':
        mostrarDialogoBaneo(usuarioId);
        break;
      default:
        console.warn(`Acción no reconocida: ${accion}`);
    }
    
    // Prevenir comportamiento por defecto
    event.preventDefault();
  }

  // Mostrar/ocultar formulario de respuesta
  function toggleFormularioRespuesta(comentarioId) {
    const formContainer = document.getElementById(`reply-form-${comentarioId}`);
    if (!formContainer) return;
    
    const isVisible = formContainer.style.display !== 'none';
    formContainer.style.display = isVisible ? 'none' : 'block';
    
    if (!isVisible) {
      // Enfocar el textarea cuando se muestra el formulario
      const textarea = formContainer.querySelector('textarea');
      if (textarea) textarea.focus();
    }
  }

  // Enviar respuesta como administrador
  async function enviarRespuesta(foroId, comentarioId, contenido) {
    if (!contenido) {
      showNotification('El contenido de la respuesta no puede estar vacío', 'error');
      return;
    }
    
    try {
      const response = await fetch(`${apiBase}/${foroId}/comentarios/${comentarioId}/respuestas`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ contenido })
      });
      
      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || 'Error al enviar la respuesta');
      }
      
      showNotification('Respuesta enviada con éxito', 'success');
      toggleFormularioRespuesta(comentarioId); // Ocultar formulario
      openForoDetail(currentForoId); // Recargar para mostrar la nueva respuesta
    } catch (error) {
      console.error('Error al enviar respuesta:', error);
      showNotification(`Error: ${error.message}`, 'error');
    }
  }

  // Mostrar diálogo de restricción
  function mostrarDialogoRestriccion(tipo, id, parentId = null) {
    // Crear el panel de moderación para restricción
    const moderationPanel = document.createElement('div');
    moderationPanel.className = 'moderation-panel';
    moderationPanel.id = 'restriccionPanel';
    
    moderationPanel.innerHTML = `
      <div class="moderation-header">
        <h3>Restringir ${tipo}</h3>
        <button class="close-btn" id="cerrarRestriccionBtn">
          <i class="fas fa-times"></i>
        </button>
      </div>
      <div class="moderation-body">
        <div class="moderation-option">
          <label for="motivoRestriccion">Motivo de la restricción:</label>
          <select id="motivoRestriccion" class="comment-input">
            <option value="Contenido inapropiado">Contenido inapropiado</option>
            <option value="Lenguaje ofensivo">Lenguaje ofensivo</option>
            <option value="Spam">Spam</option>
            <option value="Información falsa">Información falsa</option>
            <option value="Otro">Otro (especificar)</option>
          </select>
        </div>
        <div class="moderation-option" id="otroMotivoContainer" style="display:none;">
          <label for="otroMotivo">Especificar motivo:</label>
          <input type="text" id="otroMotivo" class="comment-input" placeholder="Motivo específico...">
        </div>
        <div class="moderation-buttons">
          <button class="comment-btn cancel" id="cancelarRestriccionBtn">Cancelar</button>
          <button class="comment-btn submit" id="confirmarRestriccionBtn">Confirmar Restricción</button>
        </div>
      </div>
    `;
    
    // Añadir al DOM
    document.body.appendChild(moderationPanel);
    
    // Mostrar con animación
    setTimeout(() => {
      moderationPanel.classList.add('visible');
    }, 10);
    
    // Mostrar campo de "otro motivo" cuando se selecciona "Otro"
    const motivoSelect = document.getElementById('motivoRestriccion');
    const otroMotivoContainer = document.getElementById('otroMotivoContainer');
    
    motivoSelect.addEventListener('change', () => {
      otroMotivoContainer.style.display = motivoSelect.value === 'Otro' ? 'block' : 'none';
    });
    
    // Manejar cierre
    document.getElementById('cerrarRestriccionBtn').addEventListener('click', cerrarPanelRestriccion);
    document.getElementById('cancelarRestriccionBtn').addEventListener('click', cerrarPanelRestriccion);
    
    // Manejar confirmación
    document.getElementById('confirmarRestriccionBtn').addEventListener('click', () => {
      let motivo = motivoSelect.value;
      if (motivo === 'Otro') {
        const otroMotivo = document.getElementById('otroMotivo').value.trim();
        if (!otroMotivo) {
          showNotification('Por favor, especifica el motivo', 'error');
          return;
        }
        motivo = otroMotivo;
      }
      
      // Ejecutar la restricción según el tipo
      if (tipo === 'comentario') {
        restringirComentario(currentForoId, id, motivo);
      } else if (tipo === 'respuesta') {
        restringirRespuesta(currentForoId, parentId, id, motivo);
      }
      
      // Cerrar panel
      cerrarPanelRestriccion();
    });
  }

  // Cerrar panel de restricción
  function cerrarPanelRestriccion() {
    const panel = document.getElementById('restriccionPanel');
    if (panel) {
      panel.classList.remove('visible');
      setTimeout(() => panel.remove(), 300);
    }
  }

  // Mostrar diálogo de confirmación de eliminación
  function confirmarEliminacion(tipo, id, parentId = null) {
    if (confirm(`¿Estás seguro de que deseas eliminar este ${tipo}? Esta acción no se puede deshacer.`)) {
      if (tipo === 'comentario') {
        eliminarComentario(currentForoId, id);
      } else if (tipo === 'respuesta') {
        eliminarRespuesta(currentForoId, parentId, id);
      }
    }
  }

  // Mostrar diálogo para bannear usuario
  function mostrarDialogoBaneo(usuarioId) {
    if (!usuarioId) {
      showNotification('No se pudo identificar al usuario', 'error');
      return;
    }
    
    // Crear el panel de baneo
    const banPanel = document.createElement('div');
    banPanel.className = 'moderation-panel';
    banPanel.id = 'baneoPanel';
    
    banPanel.innerHTML = `
      <div class="moderation-header">
        <h3>Bannear Usuario</h3>
        <button class="close-btn" id="cerrarBaneoBtn">
          <i class="fas fa-times"></i>
        </button>
      </div>
      <div class="moderation-body">
        <div class="moderation-option">
          <label for="tipoBaneo">Tipo de baneo:</label>
          <select id="tipoBaneo" class="comment-input">
            <option value="temporal">Temporal</option>
            <option value="permanente">Permanente</option>
          </select>
        </div>
        <div class="moderation-option" id="duracionContainer">
          <label for="duracionBaneo">Duración (días):</label>
          <input type="number" id="duracionBaneo" class="comment-input" min="1" max="365" value="7">
        </div>
        <div class="moderation-option">
          <label for="motivoBaneo">Motivo del baneo:</label>
          <textarea id="motivoBaneo" class="comment-input" rows="3" placeholder="Especifica el motivo del baneo..."></textarea>
        </div>
        <div class="moderation-buttons">
          <button class="comment-btn cancel" id="cancelarBaneoBtn">Cancelar</button>
          <button class="comment-btn submit" id="confirmarBaneoBtn" data-usuario-id="${usuarioId}">
            <i class="fas fa-check"></i> Confirmar Baneo
          </button>
        </div>
      </div>
    `;
    
    // Añadir al DOM
    document.body.appendChild(banPanel);
    
    // Mostrar con animación
    setTimeout(() => {
      banPanel.classList.add('visible');
    }, 10);
    
    // Mostrar/ocultar campo de duración según tipo de baneo
    const tipoSelect = document.getElementById('tipoBaneo');
    const duracionContainer = document.getElementById('duracionContainer');
    
    tipoSelect.addEventListener('change', () => {
      duracionContainer.style.display = tipoSelect.value === 'temporal' ? 'block' : 'none';
    });
    
    // Manejar cierre
    document.getElementById('cerrarBaneoBtn').addEventListener('click', cerrarPanelBaneo);
    document.getElementById('cancelarBaneoBtn').addEventListener('click', cerrarPanelBaneo);
    
    // Manejar confirmación
    document.getElementById('confirmarBaneoBtn').addEventListener('click', async (e) => {
      const usuarioId = e.target.getAttribute('data-usuario-id');
      const tipo = tipoSelect.value;
      const duracion = tipo === 'temporal' ? document.getElementById('duracionBaneo').value : null;
      const motivo = document.getElementById('motivoBaneo').value.trim();
      
      if (!motivo) {
        showNotification('Por favor, especifica el motivo del baneo', 'error');
        return;
      }
      
      try {
        await banearUsuario(usuarioId, tipo, duracion, motivo);
        banPanel.remove();
        loadForos(); // Recargar la lista para reflejar los cambios
      } catch (error) {
        showNotification(`Error al banear usuario: ${error.message}`, 'error');
      }
    });
  }

  // Cerrar panel de baneo
  function cerrarPanelBaneo() {
    const panel = document.getElementById('baneoPanel');
    if (panel) {
      panel.classList.remove('visible');
      setTimeout(() => panel.remove(), 300);
    }
  }

  // API calls para acciones de moderación
  async function restringirComentario(foroId, comentarioId, motivo) {
    try {
      const response = await fetch(`${apiBase}/${foroId}/comentarios/${comentarioId}/restringir`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ motivo })
      });
      
      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || 'Error al restringir el comentario');
      }
      
      showNotification('Comentario restringido exitosamente', 'success');
      openForoDetail(currentForoId); // Recargar para reflejar cambios
    } catch (error) {
      console.error('Error al restringir comentario:', error);
      showNotification(`Error: ${error.message}`, 'error');
    }
  }

  async function restringirRespuesta(foroId, comentarioId, respuestaId, motivo) {
    try {
      const response = await fetch(`${apiBase}/${foroId}/comentarios/${comentarioId}/respuestas/${respuestaId}/restringir`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ motivo })
      });
      
      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || 'Error al restringir la respuesta');
      }
      
      showNotification('Respuesta restringida exitosamente', 'success');
      openForoDetail(currentForoId); // Recargar para reflejar cambios
    } catch (error) {
      console.error('Error al restringir respuesta:', error);
      showNotification(`Error: ${error.message}`, 'error');
    }
  }

  async function aprobarComentario(foroId, comentarioId) {
    try {
      const response = await fetch(`${apiBase}/${foroId}/comentarios/${comentarioId}/aprobar`, {
        method: 'POST'
      });
      
      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || 'Error al aprobar el comentario');
      }
      
      showNotification('Comentario aprobado exitosamente', 'success');
      openForoDetail(currentForoId); // Recargar para reflejar cambios
    } catch (error) {
      console.error('Error al aprobar comentario:', error);
      showNotification(`Error: ${error.message}`, 'error');
    }
  }

  async function aprobarRespuesta(foroId, comentarioId, respuestaId) {
    try {
      const response = await fetch(`${apiBase}/${foroId}/comentarios/${comentarioId}/respuestas/${respuestaId}/aprobar`, {
        method: 'POST'
      });
      
      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || 'Error al aprobar la respuesta');
      }
      
      showNotification('Respuesta aprobada exitosamente', 'success');
      openForoDetail(currentForoId); // Recargar para reflejar cambios
    } catch (error) {
      console.error('Error al aprobar respuesta:', error);
      showNotification(`Error: ${error.message}`, 'error');
    }
  }

  async function eliminarComentario(foroId, comentarioId) {
    try {
      const response = await fetch(`${apiBase}/${foroId}/comentarios/${comentarioId}`, {
        method: 'DELETE'
      });
      
      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || 'Error al eliminar el comentario');
      }
      
      showNotification('Comentario eliminado exitosamente', 'success');
      openForoDetail(currentForoId); // Recargar para reflejar cambios
    } catch (error) {
      console.error('Error al eliminar comentario:', error);
      showNotification(`Error: ${error.message}`, 'error');
    }
  }

  async function eliminarRespuesta(foroId, comentarioId, respuestaId) {
    try {
      const response = await fetch(`${apiBase}/${foroId}/comentarios/${comentarioId}/respuestas/${respuestaId}`, {
        method: 'DELETE'
      });
      
      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || 'Error al eliminar la respuesta');
      }
      
      showNotification('Respuesta eliminada exitosamente', 'success');
      openForoDetail(currentForoId); // Recargar para reflejar cambios
    } catch (error) {
      console.error('Error al eliminar respuesta:', error);
      showNotification(`Error: ${error.message}`, 'error');
    }
  }

  async function quitarRestriccionComentario(foroId, comentarioId) {
    try {
      const response = await fetch(`${apiBase}/${foroId}/comentarios/${comentarioId}/quitar-restriccion`, {
        method: 'POST'
      });
      
      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || 'Error al quitar la restricción');
      }
      
      showNotification('Restricción eliminada exitosamente', 'success');
      openForoDetail(currentForoId); // Recargar para reflejar cambios
    } catch (error) {
      console.error('Error al quitar restricción de comentario:', error);
      showNotification(`Error: ${error.message}`, 'error');
    }
  }

  async function quitarRestriccionRespuesta(foroId, comentarioId, respuestaId) {
    try {
      const response = await fetch(`${apiBase}/${foroId}/comentarios/${comentarioId}/respuestas/${respuestaId}/quitar-restriccion`, {
        method: 'POST'
      });
      
      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.message || 'Error al quitar la restricción');
      }
      
      showNotification('Restricción eliminada exitosamente', 'success');
      openForoDetail(currentForoId); // Recargar para reflejar cambios
    } catch (error) {
      console.error('Error al quitar restricción de respuesta:', error);
      showNotification(`Error: ${error.message}`, 'error');
    }
  }

  async function banearUsuario(usuarioId, tipo, duracion, motivo) {
    try {
      const response = await fetch(`${apiBase}/usuarios/${usuarioId}/bannear`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          tipo: tipo,
          duracionDias: duracion ? parseInt(duracion) : null,
          motivo: motivo
        })
      });
      
      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.mensaje || 'Error al banear al usuario');
      }
      
      showNotification(`Usuario ${tipo === 'permanente' ? 'permanentemente' : 'temporalmente'} banneado`, 'success');
    } catch (error) {
      console.error('Error al banear usuario:', error);
      showNotification(`Error: ${error.message}`, 'error');
    }
  }

  // Cargar estadísticas para el dashboard
  async function cargarEstadisticas() {
    try {
      // Obtener las estadísticas desde el endpoint
      const response = await fetch(`${apiBase}/stats`);
      if (!response.ok) throw new Error('Error al cargar estadísticas');
      
      const data = await response.json();
      console.log('Estadísticas recibidas:', data);
      
      // Actualizar los valores en el DOM
      if (totalForos) {
        totalForos.textContent = data.totalForos || 0;
        
        // Actualizar el indicador de variación
        const variacionForos = data.variacionForos || 0;
        const diffElement = totalForos.nextElementSibling;
        if (diffElement) {
          diffElement.textContent = `${variacionForos >= 0 ? '+' : ''}${variacionForos}% `;
          const spanElement = document.createElement('span');
          spanElement.textContent = 'vs mes anterior';
          diffElement.appendChild(spanElement);
          
          // Actualizar la clase CSS según la variación
          diffElement.className = 'stat-diff';
          if (variacionForos > 0) diffElement.classList.add('positive');
          else if (variacionForos < 0) diffElement.classList.add('negative');
          else diffElement.classList.add('neutral');
        }
      }
      
      if (totalComentarios) {
        totalComentarios.textContent = data.totalComentarios || 0;
        
        // Actualizar el indicador de variación
        const variacionComentarios = data.variacionComentarios || 0;
        const diffElement = totalComentarios.nextElementSibling;
        if (diffElement) {
          diffElement.textContent = `${variacionComentarios >= 0 ? '+' : ''}${variacionComentarios}% `;
          const spanElement = document.createElement('span');
          spanElement.textContent = 'vs mes anterior';
          diffElement.appendChild(spanElement);
          
          // Actualizar la clase CSS según la variación
          diffElement.className = 'stat-diff';
          if (variacionComentarios > 0) diffElement.classList.add('positive');
          else if (variacionComentarios < 0) diffElement.classList.add('negative');
          else diffElement.classList.add('neutral');
        }
      }
      
      if (reportesPendientes) {
        reportesPendientes.textContent = data.reportesPendientes || 0;
        
        // Actualizar el indicador de variación
        const variacionReportes = data.variacionReportes || 0;
        const diffElement = reportesPendientes.nextElementSibling;
        if (diffElement) {
          diffElement.textContent = `${variacionReportes >= 0 ? '+' : ''}${variacionReportes}% `;
          const spanElement = document.createElement('span');
          spanElement.textContent = 'vs mes anterior';
          diffElement.appendChild(spanElement);
          
          // Actualizar la clase CSS según la variación
          diffElement.className = 'stat-diff';
          if (variacionReportes > 0) diffElement.classList.add('positive');
          else if (variacionReportes < 0) diffElement.classList.add('negative');
          else diffElement.classList.add('neutral');
        }
      }
      
      if (usuariosActivos) {
        usuariosActivos.textContent = data.usuariosActivos || 0;
        
        // Actualizar el indicador de variación
        const variacionUsuarios = data.variacionUsuarios || 0;
        const diffElement = usuariosActivos.nextElementSibling;
        if (diffElement) {
          diffElement.textContent = `${variacionUsuarios >= 0 ? '+' : ''}${variacionUsuarios}% `;
          const spanElement = document.createElement('span');
          spanElement.textContent = 'vs mes anterior';
          diffElement.appendChild(spanElement);
          
          // Actualizar la clase CSS según la variación
          diffElement.className = 'stat-diff';
          if (variacionUsuarios > 0) diffElement.classList.add('positive');
          else if (variacionUsuarios < 0) diffElement.classList.add('negative');
          else diffElement.classList.add('neutral');
        }
      }
      
    } catch (error) {
      console.error('Error cargando estadísticas:', error);
      showNotification('Error al cargar estadísticas', 'error');
    }
  }

  // console.log('ForoAdmin.js cargado completamente');

  // ===== Implementación de Herramientas de Moderación Avanzada =====
  
  // Referencias a elementos DOM para herramientas de moderación
  const btnBanUser = document.getElementById('btnBanUser');
  const btnWordFilter = document.getElementById('btnWordFilter');
  const banUserModal = document.getElementById('banUserModal');
  const cerrarBanUserModalBtn = document.getElementById('cerrarBanUserModalBtn');
  const wordFilterModal = document.getElementById('wordFilterModal');
  const cerrarWordFilterBtn = document.getElementById('cerrarWordFilterBtn');
  const searchUserInput = document.getElementById('searchUserInput');
  const searchUserButton = document.getElementById('searchUserButton');
  const usersTableBody = document.getElementById('usersTableBody');
  const newFilterWord = document.getElementById('newFilterWord');
  const filterLevel = document.getElementById('filterLevel');
  const addFilterWordBtn = document.getElementById('addFilterWordBtn');
  const filterWordsTableBody = document.getElementById('filterWordsTableBody');
  
  // Variables para la gestión de usuarios
  let currentUserFilter = 'todos';
  let currentUserSearch = '';
  let currentUserPage = 0;
  const userPageSize = 10;
  
  // Variables para el filtro de palabras
  let filterWords = JSON.parse(localStorage.getItem('filterWords') || '[]');

  // === Gestión de Usuarios Baneados ===
  
  // Abrir modal de gestión de usuarios baneados
  if (btnBanUser) {
    btnBanUser.addEventListener('click', () => {
      banUserModal.classList.add('visible');
      cargarUsuarios();
    });
  }
  
  // Cerrar modal de gestión de usuarios baneados
  if (cerrarBanUserModalBtn) {
    cerrarBanUserModalBtn.addEventListener('click', () => {
      banUserModal.classList.remove('visible');
    });
  }
  
  // Buscar usuarios
  if (searchUserButton) {
    searchUserButton.addEventListener('click', () => {
      currentUserSearch = searchUserInput.value.trim();
      currentUserPage = 0;
      cargarUsuarios();
    });
  }
  
  if (searchUserInput) {
    searchUserInput.addEventListener('keypress', e => {
      if (e.key === 'Enter') {
        currentUserSearch = searchUserInput.value.trim();
        currentUserPage = 0;
        cargarUsuarios();
      }
    });
  }
  
  // Cambiar filtro de usuarios
  document.querySelectorAll('#banUserModal .filter-btn').forEach(button => {
    button.addEventListener('click', () => {
      document.querySelectorAll('#banUserModal .filter-btn').forEach(btn => btn.classList.remove('active'));
      button.classList.add('active');
      currentUserFilter = button.getAttribute('data-filter');
      currentUserPage = 0;
      cargarUsuarios();
    });
  });
  
  // Cargar usuarios desde la API
  async function cargarUsuarios() {
    try {
      usersTableBody.innerHTML = '<tr><td colspan="5" class="loading-indicator"><i class="fas fa-spinner fa-spin"></i> Cargando usuarios...</td></tr>';
      
      const params = new URLSearchParams({
        page: currentUserPage,
        size: userPageSize,
        filter: currentUserFilter
      });
      
      if (currentUserSearch) {
        params.append('query', currentUserSearch);
      }
      
      const response = await fetch(`/api/admin/usuarios?${params.toString()}`);
      
      if (!response.ok) {
        throw new Error(`Error al cargar usuarios: ${response.status}`);
      }
      
      const data = await response.json();
      renderizarUsuarios(data.content, data.totalPages);
      
    } catch (error) {
      console.error('Error al cargar usuarios:', error);
      usersTableBody.innerHTML = `<tr><td colspan="5" class="error-msg">Error al cargar usuarios: ${error.message}</td></tr>`;
      showNotification('Error al cargar usuarios', 'error');
    }
  }
  
  // Renderizar usuarios en la tabla
  function renderizarUsuarios(usuarios, totalPages) {
    if (!usuarios || !usuarios.length) {
      usersTableBody.innerHTML = '<tr><td colspan="5" class="empty-table-message">No se encontraron usuarios con los criterios actuales.</td></tr>';
      document.getElementById('usersPagination').innerHTML = '';
      return;
    }
    
    usersTableBody.innerHTML = '';
    
    usuarios.forEach(usuario => {
      const tr = document.createElement('tr');
      
      // Determinar el estado y clase del usuario
      let estadoTexto = 'Activo';
      let estadoClase = 'active';
      let fechaBaneo = '-';
      
      if (usuario.baneado) {
        if (usuario.baneoTemporal) {
          estadoTexto = 'Baneo Temporal';
          estadoClase = 'temp-banned';
          fechaBaneo = formatearFecha(usuario.fechaBaneo);
        } else {
          estadoTexto = 'Baneado Permanente';
          estadoClase = 'banned';
          fechaBaneo = formatearFecha(usuario.fechaBaneo);
        }
      }
      
      tr.innerHTML = `
        <td>
          <div style="display: flex; align-items: center; gap: 10px;">
            <img src="${usuario.imagenPerfil || '/Perfiles/default-user.png'}" alt="Avatar" style="width: 32px; height: 32px; border-radius: 50%;">
            <span>${usuario.nombre}</span>
          </div>
        </td>
        <td>${usuario.email}</td>
        <td><span class="user-status ${estadoClase}">${estadoTexto}</span></td>
        <td>${fechaBaneo}</td>
        <td class="user-actions">
          ${usuario.baneado ? `
            <button class="action-icon-btn unban" title="Levantar Baneo" data-id="${usuario.id}">
              <i class="fas fa-unlock"></i>
            </button>
          ` : `
            <button class="action-icon-btn ban" title="Banear Usuario" data-id="${usuario.id}">
              <i class="fas fa-ban"></i>
            </button>
          `}
          <button class="action-icon-btn details" title="Ver Detalles" data-id="${usuario.id}">
            <i class="fas fa-info-circle"></i>
          </button>
        </td>
      `;
      
      usersTableBody.appendChild(tr);
    });
    
    // Añadir eventos a los botones de acción
    document.querySelectorAll('.action-icon-btn.ban').forEach(btn => {
      btn.addEventListener('click', () => mostrarFormularioBaneo(btn.getAttribute('data-id')));
    });
    
    document.querySelectorAll('.action-icon-btn.unban').forEach(btn => {
      btn.addEventListener('click', () => confirmarLevantarBaneo(btn.getAttribute('data-id')));
    });
    
    document.querySelectorAll('.action-icon-btn.details').forEach(btn => {
      btn.addEventListener('click', () => verDetallesUsuario(btn.getAttribute('data-id')));
    });
    
    // Renderizar paginación
    renderizarPaginacionUsuarios(totalPages);
  }
  
  // Renderizar paginación de usuarios
  function renderizarPaginacionUsuarios(totalPages) {
    const paginationElement = document.getElementById('usersPagination');
    if (!paginationElement) return;
    
    let paginationHTML = '';
    
    // Botón anterior
    paginationHTML += `
      <button class="page-btn ${currentUserPage === 0 ? 'disabled' : ''}" 
              ${currentUserPage === 0 ? 'disabled' : ''} 
              data-action="prev">
        <i class="fas fa-chevron-left"></i>
      </button>
    `;
    
    // Páginas
    const maxVisiblePages = 5;
    let startPage = Math.max(0, currentUserPage - Math.floor(maxVisiblePages / 2));
    let endPage = Math.min(totalPages - 1, startPage + maxVisiblePages - 1);
    
    if (endPage - startPage + 1 < maxVisiblePages) {
      startPage = Math.max(0, endPage - maxVisiblePages + 1);
    }
    
    for (let i = startPage; i <= endPage; i++) {
      paginationHTML += `
        <button class="page-btn ${currentUserPage === i ? 'active' : ''}" 
                data-page="${i}">
          ${i + 1}
        </button>
      `;
    }
    
    // Botón siguiente
    paginationHTML += `
      <button class="page-btn ${currentUserPage >= totalPages - 1 ? 'disabled' : ''}" 
              ${currentUserPage >= totalPages - 1 ? 'disabled' : ''} 
              data-action="next">
        <i class="fas fa-chevron-right"></i>
      </button>
    `;
    
    paginationElement.innerHTML = paginationHTML;
    
    // Añadir eventos de paginación
    document.querySelectorAll('#usersPagination .page-btn:not(.disabled)').forEach(btn => {
      btn.addEventListener('click', () => {
        if (btn.hasAttribute('data-page')) {
          currentUserPage = parseInt(btn.getAttribute('data-page'));
        } else {
          const action = btn.getAttribute('data-action');
          if (action === 'prev') currentUserPage--;
          else if (action === 'next') currentUserPage++;
        }
        cargarUsuarios();
      });
    });
  }
  
  // Mostrar formulario para banear usuario
  function mostrarFormularioBaneo(usuarioId) {
    // Crear el panel de baneo
    const banForm = document.createElement('div');
    banForm.className = 'ban-form-container';
    banForm.id = 'banFormContainer';
    
    banForm.innerHTML = `
      <h3><i class="fas fa-ban"></i> Banear Usuario</h3>
      <div class="ban-form-group">
        <label for="tipoBaneo">Tipo de Baneo</label>
        <select id="tipoBaneo" class="ban-select">
          <option value="temporal">Temporal</option>
          <option value="permanente">Permanente</option>
        </select>
      </div>
      <div class="ban-form-group" id="duracionContainer">
        <label for="duracionBaneo">Duración (días)</label>
        <input type="number" id="duracionBaneo" min="1" max="365" value="7">
      </div>
      <div class="ban-form-group">
        <label for="motivoBaneo">Motivo del Baneo</label>
        <textarea id="motivoBaneo" placeholder="Especifica el motivo del baneo..."></textarea>
      </div>
      <div class="ban-form-actions">
        <button type="button" class="btn-secondary" id="cancelarBaneoBtn">Cancelar</button>
        <button type="button" class="btn-primary" id="confirmarBaneoBtn" data-usuario-id="${usuarioId}">
          <i class="fas fa-check"></i> Confirmar Baneo
        </button>
      </div>
    `;
    
    // Si ya existe un formulario anterior, eliminarlo
    const existingForm = document.getElementById('banFormContainer');
    if (existingForm) {
      existingForm.remove();
    }
    
    // Insertar el formulario después de la tabla
    usersTableBody.parentNode.after(banForm);
    
    // Mostrar/ocultar campo de duración según tipo de baneo
    const tipoBaneo = document.getElementById('tipoBaneo');
    const duracionContainer = document.getElementById('duracionContainer');
    
    tipoBaneo.addEventListener('change', () => {
      duracionContainer.style.display = tipoBaneo.value === 'temporal' ? 'block' : 'none';
    });
    
    // Eventos para los botones
    document.getElementById('cancelarBaneoBtn').addEventListener('click', () => {
      banForm.remove();
    });
    
    document.getElementById('confirmarBaneoBtn').addEventListener('click', async (e) => {
      const usuarioId = e.target.getAttribute('data-usuario-id');
      const tipo = tipoBaneo.value;
      const duracion = tipo === 'temporal' ? document.getElementById('duracionBaneo').value : null;
      const motivo = document.getElementById('motivoBaneo').value.trim();
      
      if (!motivo) {
        showNotification('Por favor, especifica el motivo del baneo', 'error');
        return;
      }
      
      try {
        await banearUsuario(usuarioId, tipo, duracion, motivo);
        banForm.remove();
        cargarUsuarios(); // Recargar la lista para reflejar los cambios
      } catch (error) {
        showNotification(`Error al banear usuario: ${error.message}`, 'error');
      }
    });
  }
  
  // Confirmar levantar baneo de usuario
  function confirmarLevantarBaneo(usuarioId) {
    if (confirm('¿Estás seguro de que deseas levantar el baneo a este usuario?')) {
      levantarBaneo(usuarioId);
    }
  }
  
  // Levantar baneo de usuario
  async function levantarBaneo(usuarioId) {
    try {
      const response = await fetch(`/api/admin/usuarios/${usuarioId}/desbanear`, {
        method: 'POST'
      });
      
      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.mensaje || 'Error al levantar el baneo');
      }
      
      showNotification('Baneo levantado exitosamente', 'success');
      cargarUsuarios(); // Recargar la lista para reflejar los cambios
    } catch (error) {
      console.error('Error al levantar baneo:', error);
      showNotification(`Error: ${error.message}`, 'error');
    }
  }
  
  // Ver detalles de usuario
  function verDetallesUsuario(usuarioId) {
    // Esta funcionalidad podría expandirse para mostrar un modal detallado con el historial del usuario
    alert('Funcionalidad de ver detalles en desarrollo');
  }
  
  // === Gestión de Filtros de Palabras ===
  
  // Abrir modal de filtro de palabras
  if (btnWordFilter) {
    btnWordFilter.addEventListener('click', () => {
      wordFilterModal.classList.add('visible');
      cargarPalabrasFiltradas();
    });
  }
  
  // Cerrar modal de filtro de palabras
  if (cerrarWordFilterBtn) {
    cerrarWordFilterBtn.addEventListener('click', () => {
      wordFilterModal.classList.remove('visible');
    });
  }
  
  // Cargar palabras filtradas desde localStorage
  function cargarPalabrasFiltradas() {
    renderizarPalabrasFiltradas();
  }
  
  // Renderizar palabras filtradas en la tabla
  function renderizarPalabrasFiltradas() {
    if (!filterWordsTableBody) return;
    
    if (!filterWords.length) {
      filterWordsTableBody.innerHTML = '<tr><td colspan="3" class="empty-table-message">No hay palabras filtradas configuradas.</td></tr>';
      return;
    }
    
    filterWordsTableBody.innerHTML = '';
    
    filterWords.forEach((filtro, index) => {
      const tr = document.createElement('tr');
      
      let nivelTexto = 'Bloqueo Total';
      let nivelClase = 'full';
      
      if (filtro.nivel === 'partial') {
        nivelTexto = 'Censura Parcial';
        nivelClase = 'partial';
      } else if (filtro.nivel === 'warning') {
        nivelTexto = 'Solo Advertencia';
        nivelClase = 'warning';
      }
      
      tr.innerHTML = `
        <td>${filtro.palabra}</td>
        <td><span class="ban-level ${nivelClase}">${nivelTexto}</span></td>
        <td class="filter-word-actions">
          <button class="action-icon-btn edit" title="Editar" data-index="${index}">
            <i class="fas fa-edit"></i>
          </button>
          <button class="action-icon-btn delete" title="Eliminar" data-index="${index}">
            <i class="fas fa-trash"></i>
          </button>
        </td>
      `;
      
      filterWordsTableBody.appendChild(tr);
    });
    
    // Añadir eventos a los botones de acción
    document.querySelectorAll('.filter-word-actions .edit').forEach(btn => {
      btn.addEventListener('click', () => editarPalabraFiltrada(parseInt(btn.getAttribute('data-index'))));
    });
    
    document.querySelectorAll('.filter-word-actions .delete').forEach(btn => {
      btn.addEventListener('click', () => eliminarPalabraFiltrada(parseInt(btn.getAttribute('data-index'))));
    });
  }
  
  // Añadir nueva palabra filtrada
  if (addFilterWordBtn) {
    addFilterWordBtn.addEventListener('click', () => {
      const palabra = newFilterWord.value.trim();
      const nivel = filterLevel.value;
      
      if (!palabra) {
        showNotification('Por favor, ingresa una palabra o frase para filtrar', 'error');
        return;
      }
      
      // Verificar si ya existe
      const indiceExistente = filterWords.findIndex(f => f.palabra.toLowerCase() === palabra.toLowerCase());
      if (indiceExistente !== -1) {
        showNotification('Esta palabra o frase ya está en la lista de filtros', 'warning');
        return;
      }
      
      // Añadir al array y guardar
      filterWords.push({ palabra, nivel });
      guardarYActualizarFiltros();
      
      // Limpiar campo
      newFilterWord.value = '';
      
      showNotification('Palabra filtrada añadida exitosamente', 'success');
    });
  }
  
  // Editar palabra filtrada existente
  function editarPalabraFiltrada(index) {
    const filtro = filterWords[index];
    if (!filtro) return;
    
    // Rellenar formulario con los valores existentes
    newFilterWord.value = filtro.palabra;
    filterLevel.value = filtro.nivel;
    
    // Eliminar la palabra actual
    filterWords.splice(index, 1);
    guardarYActualizarFiltros();
    
    // Enfocar en el input
    newFilterWord.focus();
    
    showNotification('Edite la palabra y presione "Agregar Filtro" para guardar los cambios', 'info');
  }
  
  // Eliminar palabra filtrada
  function eliminarPalabraFiltrada(index) {
    if (confirm('¿Estás seguro de que deseas eliminar esta palabra filtrada?')) {
      filterWords.splice(index, 1);
      guardarYActualizarFiltros();
      showNotification('Palabra filtrada eliminada exitosamente', 'success');
    }
  }
  
  // Guardar en localStorage y actualizar la vista
  function guardarYActualizarFiltros() {
    localStorage.setItem('filterWords', JSON.stringify(filterWords));
    cargarPalabrasFiltradas();
  }
  
  // === Funciones compartidas para ambas herramientas ===
  
  // Función para banear usuario (usada tanto en el panel de usuarios como en comentarios)
  async function banearUsuario(usuarioId, tipo, duracion, motivo) {
    try {
      const response = await fetch(`/api/admin/usuarios/${usuarioId}/banear`, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          tipo: tipo,
          duracionDias: duracion ? parseInt(duracion) : null,
          motivo: motivo
        })
      });
      
      if (!response.ok) {
        const errorData = await response.json();
        throw new Error(errorData.mensaje || 'Error al banear al usuario');
      }
      
      showNotification(`Usuario ${tipo === 'permanente' ? 'permanentemente' : 'temporalmente'} banneado`, 'success');
    } catch (error) {
      console.error('Error al banear usuario:', error);
      showNotification(`Error: ${error.message}`, 'error');
      throw error;
    }
  }
});

 
