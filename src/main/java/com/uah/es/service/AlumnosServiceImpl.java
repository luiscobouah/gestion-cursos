package com.uah.es.service;

import com.uah.es.model.Alumno;
import com.uah.es.utils.Configuracion;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AlumnosServiceImpl implements IAlumnosService {

    @Autowired
    RestTemplate template;

    String url = Configuracion.URL_SERVICIO_ALUMNOS;

    @Override
    public Alumno[] buscarTodos() {
        return template.getForObject(url, Alumno[].class);
    }

    @Override
    public Alumno buscarAlumnoPorId(Integer idAlumno) {
        return template.getForObject(url+"/"+idAlumno, Alumno.class);
    }

    @Override
    public Alumno buscarAlumnoPorCorreo(String correo) {
        return template.getForObject(url+"/correo/"+correo, Alumno.class);
    }

    @Override
    public Alumno[] buscarAlumnosPorNombre(String nombre) {
        return template.getForObject(url + "/nombre/" + nombre, Alumno[].class);
    }

    @Override
    public boolean guardarAlumno(Alumno alumno) {

        boolean result = false;
        alumno.setIdAlumno(0);
        ResponseEntity<String> response = template.postForEntity(url, alumno, String.class);
        // Verificar la respuesta de la petición
        if (response.getStatusCode() == HttpStatus.OK) {
            result = true;
        }
        return result;
    }

    @Override
    public boolean actualizarAlumno(Alumno alumno) {

        boolean result = false;
        if (alumno.getIdAlumno() != null && alumno.getIdAlumno() > 0) {
            template.put(url, alumno);
            result=true;
        }
        return result;
    }

    @Override
    public boolean eliminarAlumno(Integer idAlumno) {
        boolean result = false;

        try {
            template.delete(url + "/" + idAlumno);
            result = true;
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }

        return result;

    }

    @Override
    public boolean inscribirCurso(Integer idAlumno, Integer idCurso) {
        boolean result = false;

        try {
            template.getForObject(url+"/insc/"+idAlumno+"/"+idCurso, String.class);
            result = true;
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }

        return result;
    }

    @Override
    public boolean desinscribirCurso(Integer idAlumno, Integer idCurso) {
        boolean result = false;

        try {
            template.getForObject(url+"/desc/"+idCurso+"/"+idAlumno, String.class);
            result = true;
        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }

        return result;
    }
}
