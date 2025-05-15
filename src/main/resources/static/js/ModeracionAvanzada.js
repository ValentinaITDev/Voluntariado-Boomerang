// ModeracionAvanzada.js
// Script para manejo de herramientas de moderación avanzada

document.addEventListener('DOMContentLoaded', () => {
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
  const usersPagination = document.getElementById('usersPagination');
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

  // === Función para mostrar notificaciones ===
  function showNotification(message, type = 'success') {
    const notificationContainer = document.getElementById('notificationContainer') || document.createElement('div');
    
    if (!document.getElementById('notificationContainer')) {
      notificationContainer.id = 'notificationContainer';
      notificationContainer.style.position = 'fixed';
      notificationContainer.style.top = '80px';
      notificationContainer.style.right = '20px';
      notificationContainer.style.zIndex = '9999';
      notificationContainer.style.width = '300px';
      document.body.appendChild(notificationContainer);
    }
    
    const notification = document.createElement('div');
    notification.className = `notification ${type}`;
    notification.textContent = message;
    notificationContainer.appendChild(notification);
    
    setTimeout(() => {
      notification.remove();
    }, 5000);
  }

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
    if (button) {
      button.addEventListener('click', () => {
        document.querySelectorAll('#banUserModal .filter-btn').forEach(btn => btn.classList.remove('active'));
        button.classList.add('active');
        currentUserFilter = button.getAttribute('data-filter');
        currentUserPage = 0;
        cargarUsuarios();
      });
    }
  });
  
  // Cargar usuarios desde la API
  async function cargarUsuarios() {
    if (!usersTableBody) return;
    
    try {
      usersTableBody.innerHTML = '<tr><td colspan="5" class="loading-indicator"><i class="fas fa-spinner fa-spin"></i> Cargando usuarios...</td></tr>';
      
      // Simulación de usuarios para desarrollo
      const usuarios = [
        { 
          id: '1', 
          nombre: 'Usuario Test 1', 
          email: 'usuario1@example.com',
          imagenPerfil: '/Perfiles/default-user.png',
          baneado: false
        },
        { 
          id: '2', 
          nombre: 'Usuario Test 2', 
          email: 'usuario2@example.com',
          imagenPerfil: '/Perfiles/default-user.png',
          baneado: true,
          baneoTemporal: true,
          fechaBaneo: new Date().toISOString()
        },
        { 
          id: '3', 
          nombre: 'Usuario Test 3', 
          email: 'usuario3@example.com',
          imagenPerfil: '/Perfiles/default-user.png',
          baneado: true,
          baneoTemporal: false,
          fechaBaneo: new Date().toISOString()
        }
      ];
      
      // Filtrar según criterios
      let usuariosFiltrados = [...usuarios];
      
      if (currentUserFilter === 'baneados') {
        usuariosFiltrados = usuariosFiltrados.filter(u => u.baneado);
      } else if (currentUserFilter === 'activos') {
        usuariosFiltrados = usuariosFiltrados.filter(u => !u.baneado);
      }
      
      if (currentUserSearch) {
        const search = currentUserSearch.toLowerCase();
        usuariosFiltrados = usuariosFiltrados.filter(u => 
          u.nombre.toLowerCase().includes(search) || 
          u.email.toLowerCase().includes(search)
        );
      }
      
      renderizarUsuarios(usuariosFiltrados, 1);
      
    } catch (error) {
      console.error('Error al cargar usuarios:', error);
      if (usersTableBody) {
        usersTableBody.innerHTML = `<tr><td colspan="5" class="error-msg">Error al cargar usuarios: ${error.message}</td></tr>`;
      }
      showNotification('Error al cargar usuarios', 'error');
    }
  }
  
  // Renderizar usuarios en la tabla
  function renderizarUsuarios(usuarios, totalPages) {
    if (!usersTableBody) return;
    
    if (!usuarios || !usuarios.length) {
      usersTableBody.innerHTML = '<tr><td colspan="5" class="empty-table-message">No se encontraron usuarios con los criterios actuales.</td></tr>';
      if (usersPagination) usersPagination.innerHTML = '';
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
    if (!usersPagination) return;
    
    let paginationHTML = '';
    
    // Botón anterior
    paginationHTML += `
      <button class="page-btn ${currentUserPage === 0 ? 'disabled' : ''}" 
              ${currentUserPage === 0 ? 'disabled' : ''} 
              data-action="prev">
        <i class="fas fa-chevron-left"></i>
      </button>
    `;
    
    // Páginas (simulación básica)
    paginationHTML += `
      <button class="page-btn active" data-page="0">1</button>
    `;
    
    // Botón siguiente
    paginationHTML += `
      <button class="page-btn disabled" disabled data-action="next">
        <i class="fas fa-chevron-right"></i>
      </button>
    `;
    
    usersPagination.innerHTML = paginationHTML;
  }
  
  // Formatear fecha
  function formatearFecha(fechaISO) {
    if (!fechaISO) return 'Fecha desconocida';
    const fecha = new Date(fechaISO);
    return fecha.toLocaleString("es-ES", { dateStyle: "medium", timeStyle: "short" });
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
    if (usersTableBody && usersTableBody.parentNode) {
      usersTableBody.parentNode.after(banForm);
    }
    
    // Mostrar/ocultar campo de duración según tipo de baneo
    const tipoBaneo = document.getElementById('tipoBaneo');
    const duracionContainer = document.getElementById('duracionContainer');
    
    if (tipoBaneo && duracionContainer) {
      tipoBaneo.addEventListener('change', () => {
        duracionContainer.style.display = tipoBaneo.value === 'temporal' ? 'block' : 'none';
      });
    }
    
    // Eventos para los botones
    const cancelarBtn = document.getElementById('cancelarBaneoBtn');
    if (cancelarBtn) {
      cancelarBtn.addEventListener('click', () => {
        banForm.remove();
      });
    }
    
    const confirmarBtn = document.getElementById('confirmarBaneoBtn');
    if (confirmarBtn) {
      confirmarBtn.addEventListener('click', async (e) => {
        const usuarioId = e.target.getAttribute('data-usuario-id');
        const tipo = tipoBaneo ? tipoBaneo.value : 'temporal';
        const duracionInput = document.getElementById('duracionBaneo');
        const duracion = (tipo === 'temporal' && duracionInput) ? duracionInput.value : null;
        const motivoInput = document.getElementById('motivoBaneo');
        const motivo = motivoInput ? motivoInput.value.trim() : '';
        
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
  }
  
  // Confirmar levantar baneo de usuario
  function confirmarLevantarBaneo(usuarioId) {
    if (confirm('¿Estás seguro de que deseas levantar el baneo a este usuario?')) {
      levantarBaneo(usuarioId);
    }
  }
  
  // Levantar baneo de usuario (simulado)
  async function levantarBaneo(usuarioId) {
    try {
      // Simulación de llamada a la API
      await new Promise(resolve => setTimeout(resolve, 500));
      
      showNotification('Baneo levantado exitosamente', 'success');
      cargarUsuarios(); // Recargar la lista para reflejar los cambios
    } catch (error) {
      console.error('Error al levantar baneo:', error);
      showNotification(`Error: ${error.message}`, 'error');
    }
  }
  
  // Ver detalles de usuario (simulado)
  function verDetallesUsuario(usuarioId) {
    // Esta funcionalidad podría expandirse para mostrar un modal detallado con el historial del usuario
    alert('Funcionalidad de ver detalles en desarrollo - Usuario ID: ' + usuarioId);
  }
  
  // === Gestión de Filtros de Palabras ===
  
  // Abrir modal de filtro de palabras
  if (btnWordFilter) {
    btnWordFilter.addEventListener('click', () => {
      if (wordFilterModal) {
        wordFilterModal.classList.add('visible');
        cargarPalabrasFiltradas();
      }
    });
  }
  
  // Cerrar modal de filtro de palabras
  if (cerrarWordFilterBtn) {
    cerrarWordFilterBtn.addEventListener('click', () => {
      if (wordFilterModal) {
        wordFilterModal.classList.remove('visible');
      }
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
  if (addFilterWordBtn && newFilterWord && filterLevel) {
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
    if (!newFilterWord || !filterLevel) return;
    
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
  
  // Función para banear usuario (simulada)
  async function banearUsuario(usuarioId, tipo, duracion, motivo) {
    try {
      // Simulación de llamada a la API
      console.log('Baneando usuario:', { usuarioId, tipo, duracion, motivo });
      await new Promise(resolve => setTimeout(resolve, 500));
      
      showNotification(`Usuario ${tipo === 'permanente' ? 'permanentemente' : 'temporalmente'} baneado`, 'success');
    } catch (error) {
      console.error('Error al banear usuario:', error);
      showNotification(`Error: ${error.message}`, 'error');
      throw error;
    }
  }
}); 