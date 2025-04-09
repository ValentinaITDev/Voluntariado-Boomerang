package com.Impulso.Alcambio.Controller;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.Impulso.Alcambio.Modelo.Foro;

@RestController
@RequestMapping("/foro")
public class ForoController {
//OBTENER FORO
    @GetMapping
    public String obtenerForo() {
        return "Foro";
    }
//CREAR FORO
    @PostMapping
    public String crearForo(@RequestBody Foro foro) {
        return "Foro creado";   
    }
//ACTUALIZAR FORO
    @PutMapping
    public String actualizarForo(@RequestBody Foro foro) {
        return "Foro actualizado";
    }
//ELIMINAR FORO
    @DeleteMapping
    public String eliminarForo(@RequestBody Foro foro) {
        return "Foro eliminado";
    }
}
