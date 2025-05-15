document.addEventListener("DOMContentLoaded", () => {
    const forosGrid = document.getElementById("forosGrid");
    const foroDetail = document.getElementById("foroDetail");
    const foroTitle = document.getElementById("foroTitle");
    const postsContainer = document.getElementById("postsContainer");
    const newPostInput = document.getElementById("newPostInput");
    const submitCommentBtn = document.getElementById("submitCommentBtn");
    const notificationContainer = document.getElementById("notificationContainer");
    const menuToggle = document.getElementById("menuToggle");
    const navLinks = document.querySelector(".nav-links");
    let foroActualId = null;

    // Inicializar menú móvil
    if (menuToggle && navLinks) {
        menuToggle.addEventListener("click", () => {
            navLinks.classList.toggle("show");
        });
    }

    // Cargar foros desde el backend
    function cargarForos() {
        const loadingIndicator = document.createElement('div');
        loadingIndicator.className = 'loading-indicator';
        loadingIndicator.innerHTML = '<i class="fas fa-circle-notch fa-spin"></i> Cargando foros...';
        forosGrid.innerHTML = '';
        forosGrid.appendChild(loadingIndicator);

        fetch("/api/foros?page=0&size=20")
            .then(res => {
                if (!res.ok) {
                    throw new Error(`Error HTTP: ${res.status}`);
                }
                return res.json();
            })
            .then(data => {
                if (data && data.content) {
                    mostrarForos(data.content);
                } else {
                    if (Array.isArray(data)) {
                        mostrarForos(data);
                    } else {
                        console.error("La respuesta de /api/foros no tiene la estructura esperada (Page) ni es un array:", data);
                        mostrarNotificacion("Error: Respuesta inesperada del servidor al cargar foros.", "error");
                        forosGrid.innerHTML = `<div class="no-foros-message"><p><i class="fas fa-exclamation-triangle"></i> Error al procesar la respuesta de los foros.</p></div>`;
                    }
                }
            })
            .catch(err => {
                console.error("Error en fetch /api/foros:", err);
                mostrarNotificacion("Error de red o servidor al cargar foros.", "error");
                forosGrid.innerHTML = `<div class="no-foros-message">
                    <p><i class="fas fa-wifi"></i> No se pudieron cargar los foros.</p>
                    <button class="retry-btn" onclick="window.location.reload()">Intentar nuevamente</button>
                </div>`;
            });
    }

    // Mostrar lista de foros
    function mostrarForos(foros) {
        forosGrid.innerHTML = "";

        if (!foros || foros.length === 0) {
            forosGrid.innerHTML = `<div class="no-foros-message">
                <p><i class="fas fa-info-circle"></i> No hay foros disponibles en este momento.</p>
                <p class="secondary-message">Consulta más tarde para ver los nuevos espacios de conversación.</p>
            </div>`;
            return;
        }

        foros.forEach((foro, index) => {
            const card = document.createElement("div");
            card.className = "foro-card";
            card.style.animationDelay = `${index * 0.08}s`;

            // Generar etiquetas si existen
            let etiquetasHTML = '';
            if (foro.etiquetas && foro.etiquetas.length > 0) {
                etiquetasHTML = '<div class="foro-tags">' + 
                    foro.etiquetas.map(etiqueta => 
                        `<span class="foro-tag">${etiqueta}</span>`
                    ).join('') + 
                '</div>';
            }
            
            // Generar meta información
            const fechaCreacion = foro.fechaCreacion ? formatearFecha(foro.fechaCreacion) : 'Fecha desconocida';
            const nombreAutor = foro.autor && foro.autor.nombre ? foro.autor.nombre : 'Usuario';
            const comentariosCount = foro.comentarios ? foro.comentarios.length : 0;
            
            card.innerHTML = `
                ${etiquetasHTML}
                <h3>${foro.titulo}</h3>
                <p class="foro-description">${foro.contenido || ''}</p>
                <div class="foro-meta">
                    <span class="foro-meta-item"><i class="fas fa-user"></i> ${nombreAutor}</span>
                    <span class="foro-meta-item"><i class="fas fa-calendar-alt"></i> ${fechaCreacion}</span>
                    <span class="foro-meta-item"><i class="fas fa-comments"></i> ${comentariosCount}</span>
                </div>
                <button class="ver-detalles-btn" data-id="${foro.id}">
                    <span>Ver conversación</span>
                    <i class="fas fa-chevron-right"></i>
                </button>
            `;
            
            card.querySelector("button").addEventListener("click", () => abrirForo(foro.id, foro.titulo));
            forosGrid.appendChild(card);
            
            // Efecto de animación de entrada
            setTimeout(() => {
                card.classList.add('visible');
            }, 50);
        });
    }

    // Abrir modal con foro
    function abrirForo(foroId, titulo) {
        foroActualId = foroId;
        foroTitle.textContent = titulo;
        
        // Mostrar el overlay del foro con animación
        foroDetail.classList.add("activo");
        
        // Animación de entrada para el contenido
        setTimeout(() => {
            document.querySelector('.foro-content').classList.add('content-visible');
        }, 50);
        
        // Mostrar indicador de carga
        postsContainer.innerHTML = `
            <div class="loading-comments">
                <div class="spinner">
                    <i class="fas fa-circle-notch fa-spin"></i>
                </div>
                <p>Cargando comentarios...</p>
            </div>
        `;
        
        cargarComentarios(foroId);
    }

    // Cerrar foro
    window.closeForo = function () {
        // Animación de salida
        document.querySelector('.foro-content').classList.remove('content-visible');
        
        // Delay para que termine la animación antes de ocultar completamente
        setTimeout(() => {
            foroDetail.classList.remove("activo");
            postsContainer.innerHTML = "";
            foroActualId = null;
        }, 300);
    };

    // Cargar comentarios de un foro
    function cargarComentarios(foroId) {
        fetch(`/api/foros/${foroId}`)
            .then(res => {
                if (!res.ok) {
                    throw new Error(`Error HTTP al cargar el foro: ${res.status}`);
                }
                return res.json();
            })
            .then(foro => {
                postsContainer.innerHTML = ""; // Limpiar antes de añadir nuevos posts
                const comentarios = foro.comentarios; // Extraer la lista de comentarios del objeto foro

                // Mostrar información del foro
                const foroInfoEl = document.createElement('div');
                foroInfoEl.className = 'foro-info-header';
                
                let etiquetasHTML = '';
                if (foro.etiquetas && foro.etiquetas.length > 0) {
                    etiquetasHTML = '<div class="foro-info-tags">' + 
                        foro.etiquetas.map(etiqueta => 
                            `<span class="foro-info-tag">${etiqueta}</span>`
                        ).join('') + 
                    '</div>';
                }
                
                foroInfoEl.innerHTML = `
                    <div class="foro-info-content">
                        ${foro.contenido ? `<p class="foro-info-description">${foro.contenido}</p>` : ''}
                        ${etiquetasHTML}
                        <div class="foro-info-meta">
                            <span><i class="fas fa-calendar-alt"></i> Creado: ${formatearFecha(foro.fechaCreacion)}</span>
                            ${foro.autor ? `<span><i class="fas fa-user"></i> Por: ${foro.autor.nombre || 'Usuario'}</span>` : ''}
                        </div>
                    </div>
                `;
                postsContainer.appendChild(foroInfoEl);
                
                // Mostrar contador de comentarios
                const commentCountEl = document.createElement('div');
                commentCountEl.className = 'comments-counter';
                commentCountEl.innerHTML = `
                    <i class="fas fa-comments"></i>
                    <span>${comentarios ? comentarios.length : 0} comentarios</span>
                `;
                postsContainer.appendChild(commentCountEl);

                if (!comentarios || comentarios.length === 0) {
                    const emptyCommentsEl = document.createElement('div');
                    emptyCommentsEl.className = 'no-comments-message';
                    emptyCommentsEl.innerHTML = `
                        <div class="no-comments-icon">
                            <i class="fas fa-comments"></i>
                        </div>
                        <p>Aún no hay comentarios. ¡Sé el primero en participar!</p>
                    `;
                    postsContainer.appendChild(emptyCommentsEl);
                    return;
                }

                comentarios.forEach((comentario, index) => {
                    if (comentario.restringido) return; // No mostrar comentarios restringidos

                    const post = document.createElement("div");
                    post.className = "post-card"; // Clase para el estilo
                    post.setAttribute('data-comment-id', comentario.id);
                    post.style.animationDelay = `${index * 0.05}s`;

                    // Imagen de perfil del autor del comentario
                    const avatarSrc = (comentario.autor && comentario.autor.imagenPerfil) 
                                      ? comentario.autor.imagenPerfil 
                                      : '/Perfiles/default-user.png';
                    // Nombre del autor del comentario
                    const autorNombre = (comentario.autor && comentario.autor.nombre) 
                                        ? comentario.autor.nombre 
                                        : 'Usuario Anónimo';

                    // Verificar si tiene respuestas
                    const tieneRespuestas = comentario.respuestas && comentario.respuestas.length > 0;
                    const cantidadRespuestas = tieneRespuestas ? comentario.respuestas.length : 0;
                    
                    // Verificar si el usuario actual es el autor del comentario o es administrador
                    const esAutor = comentario.autor && comentario.autor.usuarioId === window.currentUserData.id;
                    const esAdmin = window.currentUserData.rol === 'ADMIN';
                    const puedeEditar = esAutor || esAdmin;
                    
                    post.innerHTML = `
                        <img src="${avatarSrc}" alt="Avatar de ${autorNombre}" class="post-avatar">
                        <div class="post-content-wrapper">
                            <div class="post-content-column">
                                <div class="post-author-name">${autorNombre}</div>
                                <div class="post-body" id="comment-body-${comentario.id}">${comentario.contenido}</div>
                            </div>
                            <div class="post-actions">
                                <span class="post-timestamp"><i class="fas fa-clock"></i> ${formatearFecha(comentario.fechaCreacion)}</span>
                                <button class="action-btn like-btn" data-comment-id="${comentario.id}">
                                    <i class="fas fa-heart"></i> <span class="like-count">${comentario.likes || 0}</span>
                                </button>
                                <button class="action-btn reply-btn" data-comment-id="${comentario.id}">
                                    <i class="fas fa-reply"></i> Responder
                                </button>
                                ${puedeEditar ? `
                                <button class="action-btn edit-btn" data-comment-id="${comentario.id}">
                                    <i class="fas fa-edit"></i> Editar
                                </button>
                                <button class="action-btn delete-btn" data-comment-id="${comentario.id}">
                                    <i class="fas fa-trash"></i> Eliminar
                                </button>
                                ` : ''}
                            </div>
                            ${tieneRespuestas ? `
                                <button class="show-replies-btn" onclick="toggleReplies(this)" data-count="Ver ${cantidadRespuestas} respuestas">
                                    <i class="fas fa-chevron-down"></i> Ver ${cantidadRespuestas} respuesta${cantidadRespuestas !== 1 ? 's' : ''}
                                </button>
                                <div class="replies-container" id="replies-${comentario.id}" style="display:none;">
                                    <!-- Las respuestas se cargarán aquí -->
                                </div>
                            ` : ''}
                            <div class="reply-form-container" id="reply-form-${comentario.id}" style="display: none;">
                                <img src="${window.currentUserData.imagenPerfil || '/Perfiles/default-user.png'}" 
                                    alt="Tu avatar" class="user-avatar-input"
                                    onerror="this.onerror=null; this.src='/Perfiles/default-user.png'">
                                <div class="input-area-container">
                                    <textarea class="new-post-input" placeholder="Escribe una respuesta..."></textarea>
                                    <button class="submit-btn submit-reply-btn" title="Publicar Respuesta">
                                        <i class="fas fa-paper-plane"></i>
                                    </button>
                                </div>
                            </div>
                            <!-- Formulario de edición, inicialmente oculto -->
                            <div class="edit-form-container" id="edit-form-${comentario.id}" style="display: none;">
                                <div class="input-area-container">
                                    <textarea class="edit-input" id="edit-input-${comentario.id}">${comentario.contenido}</textarea>
                                    <div class="edit-buttons">
                                        <button class="save-edit-btn" data-comment-id="${comentario.id}" title="Guardar cambios">
                                            <i class="fas fa-check"></i> Guardar
                                        </button>
                                        <button class="cancel-edit-btn" data-comment-id="${comentario.id}" title="Cancelar">
                                            <i class="fas fa-times"></i> Cancelar
                                        </button>
                                    </div>
                                </div>
                            </div>
                        </div>
                    `;
                    postsContainer.appendChild(post);

                    // Agregar separador después de cada post principal
                    const separator = document.createElement('div');
                    separator.className = 'comments-separator';
                    postsContainer.appendChild(separator);

                    // Cargar y mostrar respuestas para este comentario
                    if (tieneRespuestas) {
                        mostrarRespuestas(comentario.id, comentario.respuestas);
                    }
                    
                    // Efecto de animación de entrada
                    setTimeout(() => {
                        post.classList.add('comment-visible');
                    }, 50);
                });
            })
            .catch(err => {
                console.error("Error al cargar comentarios:", err);
                postsContainer.innerHTML = `
                    <div class="error-message">
                        <i class="fas fa-exclamation-circle"></i>
                        <p>Error al cargar comentarios. Por favor, intenta nuevamente.</p>
                        <button class="retry-btn" onclick="cargarComentarios('${foroId}')">Reintentar</button>
                    </div>`;
                mostrarNotificacion("Error al cargar comentarios: " + err.message, "error");
            });
    }

    // Función para mostrar respuestas anidadas
    function mostrarRespuestas(comentarioId, respuestas) {
        const repliesContainer = document.getElementById(`replies-${comentarioId}`);
        if (!repliesContainer) return;
        repliesContainer.innerHTML = ''; // Limpiar respuestas previas

        respuestas.forEach((respuesta, index) => {
            if (respuesta.restringido) return; // No mostrar respuestas restringidas

            const respuestaCard = document.createElement("div");
            respuestaCard.className = "reply-card"; // Clase específica para respuestas
            respuestaCard.setAttribute('data-reply-id', respuesta.id);
            respuestaCard.style.animationDelay = `${index * 0.05}s`;

            const avatarSrc = (respuesta.autor && respuesta.autor.imagenPerfil) 
                              ? respuesta.autor.imagenPerfil 
                              : '/Perfiles/default-user.png';
            const autorNombre = (respuesta.autor && respuesta.autor.nombre) 
                                ? respuesta.autor.nombre 
                                : 'Usuario Anónimo';

            // Verificar si el usuario actual es el autor de la respuesta o es administrador
            const esAutorRespuesta = respuesta.autor && respuesta.autor.usuarioId === window.currentUserData.id;
            const esAdmin = window.currentUserData.rol === 'ADMIN';
            const puedeEditarRespuesta = esAutorRespuesta || esAdmin;
            
            respuestaCard.innerHTML = `
                <img src="${avatarSrc}" alt="Avatar de ${autorNombre}" class="post-avatar">
                <div class="post-content-wrapper">
                    <div class="post-content-column">
                        <div class="post-author-name">${autorNombre}</div>
                        <div class="post-body" id="reply-body-${respuesta.id}">${respuesta.contenido}</div>
                    </div>
                    <div class="post-actions">
                        <span class="post-timestamp"><i class="fas fa-clock"></i> ${formatearFecha(respuesta.fechaCreacion)}</span>
                        <button class="action-btn like-btn" data-reply-id="${respuesta.id}" data-parent-comment-id="${comentarioId}">
                            <i class="fas fa-heart"></i> <span class="like-count">${respuesta.likes || 0}</span>
                        </button>
                        ${puedeEditarRespuesta ? `
                        <button class="action-btn edit-reply-btn" data-reply-id="${respuesta.id}" data-parent-id="${comentarioId}">
                            <i class="fas fa-edit"></i> Editar
                        </button>
                        <button class="action-btn delete-reply-btn" data-reply-id="${respuesta.id}" data-parent-id="${comentarioId}">
                            <i class="fas fa-trash"></i> Eliminar
                        </button>
                        ` : ''}
                    </div>
                    <!-- Formulario de edición de respuesta, inicialmente oculto -->
                    <div class="edit-reply-form-container" id="edit-reply-form-${respuesta.id}" style="display: none;">
                        <div class="input-area-container">
                            <textarea class="edit-input" id="edit-reply-input-${respuesta.id}">${respuesta.contenido}</textarea>
                            <div class="edit-buttons">
                                <button class="save-reply-edit-btn" data-reply-id="${respuesta.id}" data-parent-id="${comentarioId}" title="Guardar cambios">
                                    <i class="fas fa-check"></i> Guardar
                                </button>
                                <button class="cancel-reply-edit-btn" data-reply-id="${respuesta.id}" title="Cancelar">
                                    <i class="fas fa-times"></i> Cancelar
                                </button>
                            </div>
                        </div>
                    </div>
                </div>
            `;
            repliesContainer.appendChild(respuestaCard);
            
            // Efecto de animación de entrada
            setTimeout(() => {
                respuestaCard.classList.add('reply-visible');
            }, 50);
        });
    }

    // Función para mostrar notificaciones
    function mostrarNotificacion(mensaje, tipo) {
        const notificacion = document.createElement("div");
        notificacion.className = `notification ${tipo}`;
        notificacion.innerHTML = `
            <div class="notification-icon">
                <i class="fas ${tipo === 'success' ? 'fa-check-circle' : 'fa-exclamation-circle'}"></i>
            </div>
            <div class="notification-content">${mensaje}</div>
            <button class="notification-close">
                <i class="fas fa-times"></i>
            </button>
        `;
        
        // Añadir botón para cerrar la notificación
        notificacion.querySelector('.notification-close').addEventListener('click', () => {
            notificacion.classList.add('notification-hiding');
            setTimeout(() => {
                notificacion.remove();
            }, 300);
        });
        
        notificationContainer.appendChild(notificacion);
        
        // Efecto de animación de entrada
        setTimeout(() => {
            notificacion.classList.add('notification-visible');
        }, 10);
        
        // Auto-eliminación después de 5 segundos
        setTimeout(() => {
            if (notificacion.parentNode) {
                notificacion.classList.add('notification-hiding');
                setTimeout(() => {
                    if (notificacion.parentNode) {
                        notificacion.remove();
                    }
                }, 300);
            }
        }, 5000);
    }

    // Función para formatear fechas
    function formatearFecha(fechaISO) {
        if (!fechaISO) return 'Fecha desconocida';
        
        const date = new Date(fechaISO);
        const now = new Date();
        const diffMs = now - date;
        const diffSecs = Math.round(diffMs / 1000);
        const diffMins = Math.round(diffSecs / 60);
        const diffHours = Math.round(diffMins / 60);
        const diffDays = Math.round(diffHours / 24);
        
        // Formateo relativo para fechas recientes
        if (diffSecs < 60) {
            return 'Hace un momento';
        } else if (diffMins < 60) {
            return `Hace ${diffMins} ${diffMins === 1 ? 'minuto' : 'minutos'}`;
        } else if (diffHours < 24) {
            return `Hace ${diffHours} ${diffHours === 1 ? 'hora' : 'horas'}`;
        } else if (diffDays < 7) {
            return `Hace ${diffDays} ${diffDays === 1 ? 'día' : 'días'}`;
        } else {
            // Para fechas más antiguas, usar formato completo
            return date.toLocaleDateString('es-ES', { 
                year: 'numeric', 
                month: 'short', 
                day: 'numeric',
                hour: '2-digit',
                minute: '2-digit'
            });
        }
    }

    // Publicar comentario
    submitCommentBtn.addEventListener("click", () => {
        const texto = newPostInput.value.trim();
        if (!texto || !foroActualId) return;

        const comentario = {
            texto: texto,
            usuarioId: window.currentUserData.id
        };

        fetch(`/api/foros/${foroActualId}/comentarios`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(comentario)
        })
            .then(res => {
                if (!res.ok) throw new Error("Error al publicar");
                return res.json();
            })
            .then(() => {
                newPostInput.value = "";
                mostrarNotificacion("Comentario publicado", "success");
                cargarComentarios(foroActualId);
            })
            .catch(() => mostrarNotificacion("No se pudo publicar", "error"));
    });

    // Delegación de eventos para likes y futuras acciones en posts
    postsContainer.addEventListener('click', function(event) {
        const target = event.target;
        const likeButton = target.closest('.like-btn');
        const replyButton = target.closest('.reply-btn');
        const editButton = target.closest('.edit-btn'); // Botón de editar comentario
        const deleteButton = target.closest('.delete-btn'); // Botón de eliminar comentario
        const editReplyButton = target.closest('.edit-reply-btn'); // Botón de editar respuesta
        const deleteReplyButton = target.closest('.delete-reply-btn'); // Botón de eliminar respuesta
        const saveEditButton = target.closest('.save-edit-btn'); // Botón de guardar edición
        const cancelEditButton = target.closest('.cancel-edit-btn'); // Botón de cancelar edición
        const saveReplyEditButton = target.closest('.save-reply-edit-btn'); // Botón de guardar edición de respuesta
        const cancelReplyEditButton = target.closest('.cancel-reply-edit-btn'); // Botón de cancelar edición de respuesta

        if (likeButton) {
            event.preventDefault(); // Prevenir cualquier acción por defecto del botón
            const commentId = likeButton.dataset.commentId;
            const replyId = likeButton.dataset.replyId;
            const parentCommentId = likeButton.dataset.parentCommentId;

            if (commentId && !replyId) { // Like a un comentario principal
                toggleLike('comment', commentId, likeButton);
            } else if (replyId && parentCommentId) { // Like a una respuesta
                toggleLike('reply', replyId, likeButton, parentCommentId);
            }
        }

        if (replyButton) {
            event.preventDefault();
            // Lógica para mostrar/ocultar formulario de respuesta
            const commentId = replyButton.dataset.commentId;
            const replyFormContainer = document.getElementById(`reply-form-${commentId}`);
            if (replyFormContainer) {
                replyFormContainer.style.display = replyFormContainer.style.display === 'none' ? 'flex' : 'none'; // O 'block' según el layout del form
            }
            console.log("Click en Responder a comentario:", commentId);
            // Aquí se podría enfocar el input de respuesta
        }

        // Gestión de edición de comentarios
        if (editButton) {
            event.preventDefault();
            const commentId = editButton.dataset.commentId;
            const commentBody = document.getElementById(`comment-body-${commentId}`);
            const editForm = document.getElementById(`edit-form-${commentId}`);
            
            // Ocultar el cuerpo del comentario y mostrar el formulario de edición
            commentBody.style.display = 'none';
            editForm.style.display = 'block';
        }
        
        // Gestión de eliminación de comentarios
        if (deleteButton) {
            event.preventDefault();
            const commentId = deleteButton.dataset.commentId;
            if (confirm('¿Estás seguro de que deseas eliminar este comentario?')) {
                eliminarComentario(foroActualId, commentId);
            }
        }
        
        // Gestión de edición de respuestas
        if (editReplyButton) {
            event.preventDefault();
            const replyId = editReplyButton.dataset.replyId;
            const parentId = editReplyButton.dataset.parentId;
            const replyBody = document.getElementById(`reply-body-${replyId}`);
            const editForm = document.getElementById(`edit-reply-form-${replyId}`);
            
            // Ocultar el cuerpo de la respuesta y mostrar el formulario de edición
            replyBody.style.display = 'none';
            editForm.style.display = 'block';
        }
        
        // Gestión de eliminación de respuestas
        if (deleteReplyButton) {
            event.preventDefault();
            const replyId = deleteReplyButton.dataset.replyId;
            const parentId = deleteReplyButton.dataset.parentId;
            if (confirm('¿Estás seguro de que deseas eliminar esta respuesta?')) {
                eliminarRespuesta(foroActualId, parentId, replyId);
            }
        }
        
        // Guardar edición de comentario
        if (saveEditButton) {
            event.preventDefault();
            const commentId = saveEditButton.dataset.commentId;
            const editInput = document.getElementById(`edit-input-${commentId}`);
            const nuevoContenido = editInput.value.trim();
            
            if (nuevoContenido) {
                editarComentario(foroActualId, commentId, nuevoContenido);
            }
        }
        
        // Cancelar edición de comentario
        if (cancelEditButton) {
            event.preventDefault();
            const commentId = cancelEditButton.dataset.commentId;
            const commentBody = document.getElementById(`comment-body-${commentId}`);
            const editForm = document.getElementById(`edit-form-${commentId}`);
            
            // Mostrar el cuerpo del comentario y ocultar el formulario de edición
            commentBody.style.display = 'block';
            editForm.style.display = 'none';
        }
        
        // Guardar edición de respuesta
        if (saveReplyEditButton) {
            event.preventDefault();
            const replyId = saveReplyEditButton.dataset.replyId;
            const parentId = saveReplyEditButton.dataset.parentId;
            const editInput = document.getElementById(`edit-reply-input-${replyId}`);
            const nuevoContenido = editInput.value.trim();
            
            if (nuevoContenido) {
                editarRespuesta(foroActualId, parentId, replyId, nuevoContenido);
            }
        }
        
        // Cancelar edición de respuesta
        if (cancelReplyEditButton) {
            event.preventDefault();
            const replyId = cancelReplyEditButton.dataset.replyId;
            const replyBody = document.getElementById(`reply-body-${replyId}`);
            const editForm = document.getElementById(`edit-reply-form-${replyId}`);
            
            // Mostrar el cuerpo de la respuesta y ocultar el formulario de edición
            replyBody.style.display = 'block';
            editForm.style.display = 'none';
        }

        // Gestión de envío de respuestas (código existente)
        const submitReplyButton = target.closest('.submit-reply-btn');
        if (submitReplyButton) {
            event.preventDefault();
            const formContainer = submitReplyButton.closest('.reply-form-container');
            const commentId = formContainer.id.replace('reply-form-', ''); // Extraer commentId
            const textarea = formContainer.querySelector('.new-post-input'); // Cambiado de .reply-input a .new-post-input
            const replyText = textarea.value.trim();

            if (replyText && foroActualId && commentId) {
                publicarRespuesta(foroActualId, commentId, replyText, textarea);
            }
        }
    });

    function toggleLike(type, itemId, buttonElement, parentCommentId = null) {
        let url;
        if (type === 'comment') {
            url = `/api/foros/${foroActualId}/comentarios/${itemId}/like`;
        } else if (type === 'reply') {
            if (!parentCommentId) {
                console.error("Error: parentCommentId es necesario para dar like a una respuesta.");
                mostrarNotificacion("Error interno al procesar 'Me gusta' de respuesta.", "error");
                return;
            }
            url = `/api/foros/${foroActualId}/comentarios/${parentCommentId}/respuestas/${itemId}/like`;
        } else {
            console.error("Tipo de 'like' no reconocido:", type);
            return;
        }

        fetch(url, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
        })
        .then(res => {
            if (!res.ok) {
                return res.json().then(errData => { 
                    throw new Error(errData.message || errData.error || `Error HTTP: ${res.status}`);
                }).catch(() => {
                    throw new Error(`Error HTTP: ${res.status}`);
                });
            }
            return res.json();
        })
        .then(data => {
            // Actualizar el contador de likes
            const likeCountSpan = buttonElement.querySelector('.like-count');
            if (likeCountSpan && data.likes !== undefined) {
                likeCountSpan.textContent = data.likes;
            }
            
            // Aplicar estilo al botón según si está activo o no
            if (data.currentUserLiked) {
                buttonElement.classList.add('liked-by-user');
            } else {
                buttonElement.classList.remove('liked-by-user');
            }
        })
        .catch(err => {
            console.error(`Error al procesar 'Me gusta' para ${type} ${itemId}:`, err);
            mostrarNotificacion(err.message || "No se pudo registrar el 'Me gusta'.", "error");
        });
    }

    function publicarRespuesta(foroId, comentarioId, textoRespuesta, textareaElement) {
        const respuestaPayload = {
            contenido: textoRespuesta // El backend espera un objeto con "contenido"
        };

        fetch(`/api/foros/${foroId}/comentarios/${comentarioId}/respuestas`, {
            method: "POST",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(respuestaPayload)
        })
        .then(res => {
            if (!res.ok) {
                return res.json().then(errData => { 
                    throw new Error(errData.message || errData.error || `Error HTTP: ${res.status}`);
                }).catch(() => {
                    throw new Error(`Error HTTP: ${res.status}`);
                });
            }
            return res.json(); // El backend debería devolver el foro actualizado o la respuesta/comentario
        })
        .then(data => {
            mostrarNotificacion("Respuesta publicada con éxito", "success");
            textareaElement.value = ""; // Limpiar textarea
            // Ocultar el formulario de respuesta
            const formContainer = textareaElement.closest('.reply-form-container');
            if (formContainer) {
                formContainer.style.display = 'none';
            }
            // Recargar los comentarios para mostrar la nueva respuesta
            // Esto es lo más simple. Una optimización sería solo añadir la respuesta al DOM.
            cargarComentarios(foroId);
        })
        .catch(err => {
            console.error("Error al publicar respuesta:", err);
            mostrarNotificacion(err.message || "No se pudo publicar la respuesta.", "error");
        });
    }

    // Función para editar un comentario
    function editarComentario(foroId, comentarioId, nuevoContenido) {
        const editData = {
            contenido: nuevoContenido
        };

        fetch(`/api/foros/${foroId}/comentarios/${comentarioId}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(editData)
        })
        .then(res => {
            if (!res.ok) {
                return res.json().then(errData => { 
                    throw new Error(errData.message || errData.error || `Error HTTP: ${res.status}`);
                }).catch(() => {
                    throw new Error(`Error HTTP: ${res.status}`);
                });
            }
            return res.json();
        })
        .then(data => {
            mostrarNotificacion("Comentario actualizado", "success");
            // Recargar los comentarios para mostrar los cambios
            cargarComentarios(foroId);
        })
        .catch(err => {
            console.error("Error al editar comentario:", err);
            mostrarNotificacion(err.message || "No se pudo editar el comentario.", "error");
        });
    }

    // Función para eliminar un comentario
    function eliminarComentario(foroId, comentarioId) {
        fetch(`/api/foros/${foroId}/comentarios/${comentarioId}`, {
            method: "DELETE"
        })
        .then(res => {
            if (!res.ok) {
                return res.json().then(errData => { 
                    throw new Error(errData.message || errData.error || `Error HTTP: ${res.status}`);
                }).catch(() => {
                    throw new Error(`Error HTTP: ${res.status}`);
                });
            }
            return res.text(); // Para respuestas que no devuelven JSON
        })
        .then(() => {
            mostrarNotificacion("Comentario eliminado", "success");
            // Recargar los comentarios para reflejar la eliminación
            cargarComentarios(foroId);
        })
        .catch(err => {
            console.error("Error al eliminar comentario:", err);
            mostrarNotificacion(err.message || "No se pudo eliminar el comentario.", "error");
        });
    }

    // Función para editar una respuesta
    function editarRespuesta(foroId, comentarioId, respuestaId, nuevoContenido) {
        const editData = {
            contenido: nuevoContenido
        };

        fetch(`/api/foros/${foroId}/comentarios/${comentarioId}/respuestas/${respuestaId}`, {
            method: "PUT",
            headers: { "Content-Type": "application/json" },
            body: JSON.stringify(editData)
        })
        .then(res => {
            if (!res.ok) {
                return res.json().then(errData => { 
                    throw new Error(errData.message || errData.error || `Error HTTP: ${res.status}`);
                }).catch(() => {
                    throw new Error(`Error HTTP: ${res.status}`);
                });
            }
            return res.json();
        })
        .then(data => {
            mostrarNotificacion("Respuesta actualizada", "success");
            // Recargar los comentarios para mostrar los cambios
            cargarComentarios(foroId);
        })
        .catch(err => {
            console.error("Error al editar respuesta:", err);
            mostrarNotificacion(err.message || "No se pudo editar la respuesta.", "error");
        });
    }

    // Función para eliminar una respuesta
    function eliminarRespuesta(foroId, comentarioId, respuestaId) {
        fetch(`/api/foros/${foroId}/comentarios/${comentarioId}/respuestas/${respuestaId}`, {
            method: "DELETE"
        })
        .then(res => {
            if (!res.ok) {
                return res.json().then(errData => { 
                    throw new Error(errData.message || errData.error || `Error HTTP: ${res.status}`);
                }).catch(() => {
                    throw new Error(`Error HTTP: ${res.status}`);
                });
            }
            return res.text(); // Para respuestas que no devuelven JSON
        })
        .then(() => {
            mostrarNotificacion("Respuesta eliminada", "success");
            // Recargar los comentarios para reflejar la eliminación
            cargarComentarios(foroId);
        })
        .catch(err => {
            console.error("Error al eliminar respuesta:", err);
            mostrarNotificacion(err.message || "No se pudo eliminar la respuesta.", "error");
        });
    }

    // Cargar todo al iniciar
    cargarForos();
});
