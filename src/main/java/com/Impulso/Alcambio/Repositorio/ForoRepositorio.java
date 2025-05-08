package com.Impulso.Alcambio.Repositorio;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import com.Impulso.Alcambio.Modelo.Foro;

public interface ForoRepositorio extends MongoRepository<Foro, String> {
    // Buscar foros por título
    List<Foro> findByTituloContaining(String titulo);
    Page<Foro> findByTituloContaining(String titulo, Pageable pageable);
    
    // Buscar foros por etiqueta
    List<Foro> findByEtiquetasContaining(String etiqueta);
    Page<Foro> findByEtiquetasContaining(String etiqueta, Pageable pageable);
    
    // Buscar foros por autor
    List<Foro> findByAutorUsuarioId(String usuarioId);
    Page<Foro> findByAutorUsuarioId(String usuarioId, Pageable pageable);
    
    // Métodos para administración
    List<Foro> findByActivo(boolean activo);
    Page<Foro> findByActivo(boolean activo, Pageable pageable);
    
    List<Foro> findByArchivado(boolean archivado);
    Page<Foro> findByArchivado(boolean archivado, Pageable pageable);
    
    @Query("{ 'contadorReportes': { $gt: 0 } }")
    List<Foro> findReportedForos();
    @Query("{ 'contadorReportes': { $gt: 0 } }")
    Page<Foro> findReportedForos(Pageable pageable);
    
    // Foros con comentarios reportados
    @Query("{ 'comentarios.reportado': true }")
    List<Foro> findForosWithReportedComments();
    @Query("{ 'comentarios.reportado': true }")
    Page<Foro> findForosWithReportedComments(Pageable pageable);
    
    // Combinar criterios
    Page<Foro> findByActivoAndArchivado(boolean activo, boolean archivado, Pageable pageable);
}
