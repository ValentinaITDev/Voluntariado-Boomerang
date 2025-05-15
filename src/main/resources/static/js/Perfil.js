// Variables globales
let userProfile = null;
let userId = null;
let changePasswordEndpoint = null;

// Cargar perfil de usuario usando Promesas
function loadUserProfile() {
    fetch('/api/usuarios/me')
        .then(function(response) {
            if (!response.ok) {
                throw new Error('Error al cargar el perfil de usuario');
            }
            return response.json();
        })
        .then(function(profileData) {
            userProfile = profileData;
            userId = userProfile.id;

            // Asumir ruta estándar para cambio de contraseña
            changePasswordEndpoint = `/api/usuarios/${userId}/password`;
            console.log("User profile loaded:", userProfile);
            console.log("Change password endpoint set to:", changePasswordEndpoint);

            updateProfileUI(userProfile);
        })
        .catch(function(error) {
            console.error('Error cargando perfil:', error);
            const userNameElement = document.querySelector('.profile-name');
            const userTitleElement = document.querySelector('.profile-title');
            if(userNameElement) userNameElement.textContent = 'Error al cargar';
            if(userTitleElement) userTitleElement.textContent = 'Recargue la página';
        });
}

// Actualizar UI con datos del perfil
function updateProfileUI(profile) {
    if (!profile) return;

    document.getElementById('user-name').textContent = profile.nombre || 'Usuario';
    document.getElementById('user-title').textContent =
        `${profile.rol === 'ADMIN' ? 'Administrador' : 'Voluntario'} · ${profile.empresa || 'N/A'}`;

    const profilePhotoElement = document.getElementById('profile-photo');
    if (profile.imagenPerfil) {
        profilePhotoElement.src = profile.imagenPerfil;
    } else {
        profilePhotoElement.src = '/resources/Perfiles/perfil.png'; // Imagen por defecto
    }
    profilePhotoElement.onerror = function() { profilePhotoElement.src = '/resources/Perfiles/perfil.png'; };

    document.getElementById('nombre-input').value = profile.nombre || '';
    document.getElementById('correo-input').value = profile.correo || '';
    document.getElementById('numero-input').value = profile.numero || '';
    document.getElementById('empresa-input').value = profile.empresa || '';

    const totalPuntosEl = document.getElementById('total-puntos');
    const totalInsigniasEl = document.getElementById('total-insignias');

    if(totalPuntosEl) totalPuntosEl.textContent = profile.puntos || 0;
    if(totalInsigniasEl) totalInsigniasEl.textContent = profile.insignias ? profile.insignias.length : 0;

    if (typeof loadProjects === 'function') loadProjects(profile);
    if (typeof loadBadges === 'function') loadBadges(profile);

    const dashboardTab = document.querySelector('.dashboard-tab');
    if (dashboardTab) {
         dashboardTab.style.display = (profile.rol === 'ADMIN') ? 'flex' : 'none';
    }
}

// Cargar proyectos usando bucle for
function loadProjects(profile) {
    const proyectosActivosContainer = document.getElementById('proyectos-activos-container');
    const proyectosCompletadosContainer = document.getElementById('proyectos-completados-container');

    if (!proyectosActivosContainer || !proyectosCompletadosContainer) return;

    proyectosActivosContainer.innerHTML = '';
    proyectosCompletadosContainer.innerHTML = '';

    let activosCount = 0;
    let completadosCount = 0;

    if (profile.proyectos && Array.isArray(profile.proyectos)) {
         for (let i = 0; i < profile.proyectos.length; i++) {
             const proyecto = profile.proyectos[i];
             const estado = proyecto.estado === 'COMPLETADO' ? 'Completado' : 'En curso';
             const proyectoCard = createProjectCard(proyecto, estado);
             if(estado === 'Completado') {
                 proyectosCompletadosContainer.appendChild(proyectoCard);
                 completadosCount++;
             } else {
                 proyectosActivosContainer.appendChild(proyectoCard);
                 activosCount++;
             }
         }
    }

    if (activosCount === 0) {
        proyectosActivosContainer.innerHTML = '<div class="no-data">No hay proyectos activos asociados.</div>';
    }
    if (completadosCount === 0) {
        proyectosCompletadosContainer.innerHTML = '<div class="no-data">No hay proyectos completados asociados.</div>';
    }
     const totalProyectosEl = document.getElementById('total-proyectos');
     if(totalProyectosEl) totalProyectosEl.textContent = activosCount + completadosCount;
}

// Crear tarjeta de proyecto (sin cambios mayores)
function createProjectCard(proyecto, estado) {
    const projectCard = document.createElement('div');
    projectCard.classList.add('project-card');

    const imagenHTML = proyecto.imagenUrl
        ? `<img src="${proyecto.imagenUrl}" alt="${proyecto.nombre || ''}" class="project-image-content" onerror="this.style.display='none'; this.nextElementSibling.style.display='flex';">`
        : '';
    const placeholderHTML = `<div class="project-image-placeholder" style="display: ${proyecto.imagenUrl ? 'none' : 'flex'};"></i></div>`;

    projectCard.innerHTML = `
        <div class="project-image">
            ${imagenHTML}
            ${placeholderHTML}
            <div class="project-status">${estado}</div>
        </div>
        <div class="project-content">
            <h3>${proyecto.nombre || 'Proyecto sin nombre'}</h3>
            <p class="project-meta">
                 ${proyecto.descripcion ? proyecto.descripcion.substring(0, 80) + '...' : 'Descripción no disponible.'}
            </p>
            <div class="project-details">
                <span></i> ID: ${proyecto.id || proyecto.proyectoId || 'N/A'}</span>
            </div>
        </div>
    `;

    return projectCard;
}

// Cargar insignias usando bucle for
function loadBadges(profile) {
    const insigniasContainer = document.getElementById('insignias-container');
    if (!insigniasContainer) return;
    insigniasContainer.innerHTML = '';

    if (profile.insignias && Array.isArray(profile.insignias) && profile.insignias.length > 0) {
        for (let i = 0; i < profile.insignias.length; i++) {
            const insignia = profile.insignias[i];
            const badgeItem = document.createElement('div');
            badgeItem.classList.add('badge-item');

            let icon = 'medal';
            const nombreLower = insignia.nombre ? insignia.nombre.toLowerCase() : '';
            if (nombreLower.includes('planeta') || nombreLower.includes('ambiente') || nombreLower.includes('eco')) {
                icon = 'seedling';
            } else if (nombreLower.includes('educa') || nombreLower.includes('mentor') || nombreLower.includes('enseñ')) {
                icon = 'graduation-cap';
            } else if (nombreLower.includes('equipo') || nombreLower.includes('colabora') || nombreLower.includes('social')) {
                icon = 'users';
            } else if (nombreLower.includes('estrella') || nombreLower.includes('excelen') || nombreLower.includes('top')) {
                icon = 'star';
            } else if (nombreLower.includes('primer') || nombreLower.includes('inicio') || nombreLower.includes('bienveni')) {
                 icon = 'handshake';
            }

            badgeItem.innerHTML = `
                <div class="badge-icon" title="${insignia.descripcion || insignia.nombre || 'Insignia'}">
                    <i class="fas fa-${icon}"></i>
                </div>
                <h4 class="badge-name">${insignia.nombre || 'Insignia'}</h4>
            `;

            insigniasContainer.appendChild(badgeItem);
        }
    } else {
        insigniasContainer.innerHTML = '<div class="no-data">No has obtenido insignias todavía. ¡Participa en proyectos para ganarlas!</div>';
    }
}

// Guardar cambios del perfil usando Promesas
function saveProfileChanges() {
    if (!userId) {
        alert('Error: No se ha podido identificar al usuario.');
        return;
    }

    const numeroInput = document.getElementById('numero-input');
    const empresaInput = document.getElementById('empresa-input');

    const perfilActualizado = {
        numero: numeroInput ? numeroInput.value : null,
        empresa: empresaInput ? empresaInput.value : null
    };

    console.log("Guardando cambios del perfil:", perfilActualizado);

    const saveBtn = document.getElementById('save-info-btn');
    if(saveBtn) {
         saveBtn.disabled = true;
         saveBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Guardando...';
    }

    const updateEndpoint = userProfile?.endpoints?.actualizarPerfil || `/api/usuarios/${userId}`;
    console.log(`Usando endpoint de actualización: ${updateEndpoint}`);

    fetch(updateEndpoint, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify(perfilActualizado)
    })
    .then(function(response) {
        if (!response.ok) {
            return response.json()
                .catch(function() { return null; })
                .then(function(errorData) {
                    let errorMsg = 'Error al actualizar el perfil';
                    if(errorData && errorData.mensaje) errorMsg = errorData.mensaje;
                    throw new Error(`${errorMsg} (${response.status})`);
                });
        }
        // No esperar JSON necesariamente si la API solo devuelve 200 OK
        userProfile.numero = perfilActualizado.numero;
        userProfile.empresa = perfilActualizado.empresa;
        updateProfileUI(userProfile);

        alert('Perfil actualizado correctamente.');
        toggleEditMode(false);

    })
    .catch(function(error) {
        console.error('Error al guardar perfil:', error);
        alert('Error al actualizar el perfil: ' + error.message);
    })
    .finally(function() {
         if(saveBtn) {
             saveBtn.disabled = false;
             saveBtn.innerHTML = '<i class="fas fa-save"></i> Guardar Cambios';
         }
    });
}

// Cambiar contraseña usando Promesas
function changePassword(oldPassword, newPassword) {
    if (!userId) {
        alert('Error: No se puede cambiar la contraseña (ID de usuario desconocido).');
        return Promise.reject(new Error('ID de usuario desconocido')); // Devolver promesa rechazada
    }
    
    // Usar el endpoint proporcionado por el backend o construir uno por defecto
    const endpoint = userProfile?.endpoints?.cambiarPassword || `/api/usuarios/${userId}/cambiar-password`;
    console.log(`Usando endpoint para cambio de contraseña: ${endpoint}`);

    const changePasswordBtn = document.querySelector('.change-password-btn');
    if(changePasswordBtn) {
         changePasswordBtn.disabled = true;
         changePasswordBtn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Cambiando...';
    }

    return fetch(endpoint, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json',
        },
        body: JSON.stringify({
            oldPassword: oldPassword,
            newPassword: newPassword
        })
    })
    .then(function(response) {
        if (response.ok) {
            // Manejar respuesta 200 OK sin cuerpo o con cuerpo JSON
            return response.text().then(function(text) {
                try {
                    const data = text ? JSON.parse(text) : {}; // Intentar parsear si hay texto
                    console.log("Contraseña cambiada con éxito", data);
                    return true; // Indicar éxito
                } catch (e) {
                     // Si no es JSON válido pero la respuesta fue OK (ej. 204 No Content)
                     if (response.status === 200 || response.status === 204) {
                         console.log("Contraseña cambiada con éxito (respuesta sin cuerpo o no JSON)");
                         return true;
                     } else {
                         throw new Error('Respuesta inesperada del servidor');
                     }
                }
            });
        } else {
            // Intentar obtener mensaje de error del JSON
            return response.json()
                .catch(function() { return { mensaje: `Error ${response.status}` }; }) // Fallback si no hay JSON
                .then(function(errorData) {
                    throw new Error(errorData.mensaje || `Error ${response.status}`);
                });
        }
    })
    .catch(function(error) {
        console.error('Error al cambiar contraseña:', error);
        throw error; // Re-lanzar el error para que se maneje en el .catch del submit
    })
    .finally(function() {
         if(changePasswordBtn) {
            changePasswordBtn.disabled = false;
            changePasswordBtn.innerHTML = '<i class="fas fa-key"></i> Cambiar Contraseña';
        }
    });
}

// Validar requisitos de contraseña (sin cambios mayores)
function validatePassword(password) {
    const requirements = {
        length: password.length >= 8,
        uppercase: /[A-Z]/.test(password),
        lowercase: /[a-z]/.test(password),
        number: /[0-9]/.test(password)
    };

    const reqLength = document.getElementById('req-length');
    const reqUppercase = document.getElementById('req-uppercase');
    const reqLowercase = document.getElementById('req-lowercase');
    const reqNumber = document.getElementById('req-number');

    if(reqLength) reqLength.classList.toggle('met', requirements.length);
    if(reqUppercase) reqUppercase.classList.toggle('met', requirements.uppercase);
    if(reqLowercase) reqLowercase.classList.toggle('met', requirements.lowercase);
    if(reqNumber) reqNumber.classList.toggle('met', requirements.number);

    return requirements.length && requirements.uppercase && requirements.lowercase && requirements.number;
}

// Habilitar/deshabilitar modo edición usando bucle for
function toggleEditMode(enable) {
    const editBtn = document.getElementById('edit-info-btn');
    const saveBtn = document.getElementById('save-info-btn');
    const inputs = document.querySelectorAll('#info .info-value input:not(.field-locked)');

    if(editBtn) editBtn.style.display = enable ? 'none' : 'flex';
    if(saveBtn) saveBtn.style.display = enable ? 'flex' : 'none';

    for (let i = 0; i < inputs.length; i++) {
        const input = inputs[i];
        input.disabled = !enable;
        input.classList.toggle('editing', enable);
    }

    if(enable && inputs.length > 0) {
         inputs[0].focus();
    }
}

// Eventos
document.addEventListener('DOMContentLoaded', function() {
    loadUserProfile();

    const tabsContainer = document.querySelector('.tabs-container');
    if (tabsContainer) {
        tabsContainer.addEventListener('click', function(e) { // Usando function()
            if (e.target.classList.contains('tab-btn')) {
                const targetTab = e.target.dataset.tab;
                if(targetTab) {
                     const contents = document.querySelectorAll('.tab-content');
                     for(let i=0; i<contents.length; i++) contents[i].classList.remove('active');

                     const buttons = document.querySelectorAll('.tab-btn');
                     for(let i=0; i<buttons.length; i++) buttons[i].classList.remove('active');

                     const contentToShow = document.getElementById(targetTab);
                     if(contentToShow) contentToShow.classList.add('active');
                     e.target.classList.add('active');
                }
            }
        });
    } else {
         console.warn("Contenedor de tabs no encontrado.");
    }

    const photoUploadInput = document.getElementById('profile-photo-upload');
    const photoContainer = document.querySelector('.profile-photo-container');
    const profilePhoto = document.getElementById('profile-photo');

    if (photoUploadInput && photoContainer && profilePhoto) {
        photoUploadInput.addEventListener('change', function(e) {
            if (e.target.files && e.target.files[0]) {
                const file = e.target.files[0];
                const reader = new FileReader();

                reader.onload = function(event) {
                    profilePhoto.src = event.target.result;

                    if (!userId) {
                        alert("Error: ID de usuario no disponible para subir imagen.");
                        return;
                    }

                    const formData = new FormData();
                    formData.append('imagen', file);

                    let loadingOverlay = photoContainer.querySelector('.loading-overlay');
                    if (!loadingOverlay) {
                        loadingOverlay = document.createElement('div');
                        loadingOverlay.className = 'loading-overlay';
                        loadingOverlay.innerHTML = '<i class="fas fa-spinner fa-spin"></i>';
                        photoContainer.appendChild(loadingOverlay);
                    } else {
                         loadingOverlay.style.display = 'flex';
                    }

                    fetch(`/api/usuarios/${userId}/imagen-perfil`, {
                        method: 'POST',
                        body: formData
                    })
                    .then(function(response) {
                        if (!response.ok) {
                             return response.json()
                                .catch(function() { return null; })
                                .then(function(data) {
                                      throw new Error((data && data.mensaje) || `Error ${response.status}`);
                                 });
                        }
                        return response.json();
                    })
                    .then(function(data) {
                        if(loadingOverlay) loadingOverlay.style.display = 'none';

                        if(data.imagenUrl) {
                            profilePhoto.src = data.imagenUrl;
                             if(userProfile) userProfile.imagenPerfil = data.imagenUrl;
                        } else {
                             console.warn("La API no devolvió imagenUrl, usando previsualización local.");
                        }
                        alert('Imagen de perfil actualizada con éxito.');
                    })
                    .catch(function(error) {
                        if(loadingOverlay) loadingOverlay.style.display = 'none';
                        console.error('Error al subir imagen:', error);
                        alert(`Error al subir la imagen: ${error.message}`);
                    });
                };

                reader.readAsDataURL(file);
            }
        });
    } else {
         console.warn("Elementos para subida de foto no encontrados.");
    }

    const editBtn = document.getElementById('edit-info-btn');
    if(editBtn) {
         editBtn.addEventListener('click', function() { toggleEditMode(true); }); // Usando function()
    }

    const saveBtn = document.getElementById('save-info-btn');
    if(saveBtn) {
         saveBtn.addEventListener('click', saveProfileChanges);
    }

    const newPasswordInput = document.getElementById('new-password');
    const confirmPasswordInput = document.getElementById('confirm-password');
    const passwordErrorDiv = document.getElementById('password-error');

    if (newPasswordInput && confirmPasswordInput && passwordErrorDiv) {
        newPasswordInput.addEventListener('input', function() {
            validatePassword(this.value);
            if (confirmPasswordInput.value && this.value !== confirmPasswordInput.value) {
                passwordErrorDiv.textContent = 'Las nuevas contraseñas no coinciden.';
                passwordErrorDiv.style.display = 'block';
            } else {
                passwordErrorDiv.style.display = 'none';
            }
        });

        confirmPasswordInput.addEventListener('input', function() {
            if (newPasswordInput.value && this.value !== newPasswordInput.value) {
                passwordErrorDiv.textContent = 'Las nuevas contraseñas no coinciden.';
                passwordErrorDiv.style.display = 'block';
            } else {
                passwordErrorDiv.style.display = 'none';
            }
        });
    }

    const changePasswordForm = document.getElementById('change-password-form');
    const passwordSuccessDiv = document.getElementById('password-success');

    if (changePasswordForm && passwordErrorDiv && passwordSuccessDiv) {
        changePasswordForm.addEventListener('submit', function(e) { // Usando function()
            e.preventDefault();

            const oldPasswordInput = document.getElementById('old-password');
            const newPasswordInputSubmit = document.getElementById('new-password');
            const confirmPasswordInputSubmit = document.getElementById('confirm-password');

            passwordErrorDiv.style.display = 'none';
            passwordSuccessDiv.style.display = 'none';

            if (!oldPasswordInput || !newPasswordInputSubmit || !confirmPasswordInputSubmit) return;

            const oldPassword = oldPasswordInput.value;
            const newPassword = newPasswordInputSubmit.value;
            const confirmPassword = confirmPasswordInputSubmit.value;

            if (!oldPassword || !newPassword || !confirmPassword) {
                passwordErrorDiv.textContent = 'Todos los campos de contraseña son obligatorios.';
                passwordErrorDiv.style.display = 'block';
                return;
            }

            if (newPassword !== confirmPassword) {
                passwordErrorDiv.textContent = 'Las nuevas contraseñas no coinciden.';
                passwordErrorDiv.style.display = 'block';
                return;
            }

            if (!validatePassword(newPassword)) {
                passwordErrorDiv.textContent = 'La nueva contraseña no cumple con los requisitos de seguridad.';
                passwordErrorDiv.style.display = 'block';
                return;
            }

            // Llamar a changePassword y manejar la promesa
            changePassword(oldPassword, newPassword)
                .then(function(success) {
                    if (success) {
                        passwordSuccessDiv.textContent = 'Contraseña actualizada con éxito.';
                        passwordSuccessDiv.style.display = 'block';

                        oldPasswordInput.value = '';
                        newPasswordInputSubmit.value = '';
                        confirmPasswordInputSubmit.value = '';
                        validatePassword('');

                        setTimeout(function() {
                            passwordSuccessDiv.style.display = 'none';
                        }, 4000);
                    }
                })
                .catch(function(error) {
                    passwordErrorDiv.textContent = error.message || 'Error desconocido al cambiar la contraseña.';
                    passwordErrorDiv.style.display = 'block';
                });
        });
    } else {
         console.warn("Formulario de cambio de contraseña o elementos de mensaje no encontrados.");
    }
});

// Function to load user's projects
async function loadUserProjects() {
    try {
        // Get user's profile data first to get the ID
        const profileResponse = await fetch('/api/usuarios/me');
        const profileData = await profileResponse.json();
        
        console.log("Perfil cargado:", profileData);
        
        // Fetch user's projects
        const projectsResponse = await fetch(`/api/usuarios/${profileData.id}/proyectos`);
        const projectsData = await projectsResponse.json();
        
        console.log("Datos de proyectos recibidos:", projectsData);
        
        // Container for active and completed projects
        const activosContainer = document.getElementById('proyectos-activos-container');
        const completadosContainer = document.getElementById('proyectos-completados-container');
        
        // Clear loading placeholders
        activosContainer.innerHTML = '';
        completadosContainer.innerHTML = '';
        
        // Counters for active and completed projects
        let activosCount = 0;
        let completadosCount = 0;
        
        // Check if we have projects and if it's an array
        if (!projectsData || (Array.isArray(projectsData) && projectsData.length === 0)) {
            activosContainer.innerHTML = '<div class="no-data">No hay proyectos activos asociados.</div>';
            completadosContainer.innerHTML = '<div class="no-data">No hay proyectos completados asociados.</div>';
            return;
        }
        
        // Handle different response formats - either direct project objects or just IDs
        const projects = Array.isArray(projectsData) ? projectsData : [projectsData];
        
        // Process each project
        for (let i = 0; i < projects.length; i++) {
            let project = projects[i];
            
            // If we only got IDs, fetch the full project data
            if (typeof project === 'number' || typeof project === 'string') {
                const projectId = project;
                console.log(`Obteniendo detalles del proyecto ${projectId}...`);
                const projectResponse = await fetch(`/api/proyectos/${projectId}`);
                project = await projectResponse.json();
                console.log(`Proyecto ${projectId} obtenido:`, project);
            }
            
            // Ensure project has all required fields with default values if missing
            project = {
                id: project.id || project.proyectoId || 'N/A',
                nombre: project.nombre || 'Proyecto sin nombre',
                descripcion: project.descripcion || 'Descripción no disponible',
                estado: project.estado || 'ACTIVO',
                imagenUrl: project.imagenUrl || null,
                fechaCreacion: project.fechaCreacion || new Date().toISOString(),
                ...project
            };
            
            // Create project card
            const estado = project.estado === 'COMPLETADO' ? 'Completado' : 'En curso';
            const projectCard = createProjectCard(project, estado);
            
            // Add to appropriate container based on status
            if (project.estado === 'COMPLETADO') {
                completadosContainer.appendChild(projectCard);
                completadosCount++;
            } else {
                activosContainer.appendChild(projectCard);
                activosCount++;
            }
        }
        
        // Update counters if no projects were found
        if (activosCount === 0) {
            activosContainer.innerHTML = '<div class="no-data">No hay proyectos activos asociados.</div>';
        }
        if (completadosCount === 0) {
            completadosContainer.innerHTML = '<div class="no-data">No hay proyectos completados asociados.</div>';
        }
        
        // Update total projects count in profile stats
        const totalProyectosEl = document.getElementById('total-proyectos');
        if (totalProyectosEl) {
            totalProyectosEl.textContent = activosCount + completadosCount;
        }
        
    } catch (error) {
        console.error('Error loading projects:', error);
        const activosContainer = document.getElementById('proyectos-activos-container');
        const completadosContainer = document.getElementById('proyectos-completados-container');
        
        if (activosContainer) {
            activosContainer.innerHTML = '<div class="error-message">Error al cargar los proyectos</div>';
        }
        if (completadosContainer) {
            completadosContainer.innerHTML = '<div class="error-message">Error al cargar los proyectos</div>';
        }
    }
}

// Add this to your existing document.addEventListener('DOMContentLoaded', ...)
document.addEventListener('DOMContentLoaded', function() {
    // Load projects when the projects tab is clicked
    document.querySelector('[data-tab="projects"]').addEventListener('click', loadUserProjects);
    
    // Also load projects if we're starting on the projects tab
    if (window.location.hash === '#projects') {
        loadUserProjects();
    }
});
