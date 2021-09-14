package com.uah.es.views.alumnos;

import com.helger.commons.csv.CSVWriter;
import com.uah.es.model.Alumno;
import com.uah.es.model.Curso;
import com.uah.es.service.IAlumnosService;
import com.uah.es.views.MainLayout;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Label;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import org.apache.commons.io.IOUtils;
import org.springframework.security.access.annotation.Secured;
import org.vaadin.klaudeta.PaginatedGrid;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.*;

/*https://vaadin.com/directory/component/grid-pagination/samples*/
//https://vaadin.com/components/vaadin-button/java-install

@PageTitle("Alumnos")
@Route(value = "alumnos", layout = MainLayout.class)
@Secured("Admin")
public class AlumnosView extends Div {

    //Servicio para comunicación con el backend
    IAlumnosService alumnosService;

    //Componentes visuales
    AlumnoForm alumnoForm;
    Anchor linkDescargaCsv;
    PaginatedGrid<Alumno> grid = new PaginatedGrid<>();
    TextField nombreFiltro = new TextField();
    TextField correoFiltro = new TextField();
    Button buscarBtn = new Button("Buscar");
    Button mostrarTodosBtn = new Button("Mostrar todos");
    Dialog formularioDg = new Dialog();
    Notification notificacionOK = new Notification("", 3000);
    Notification notificacionKO = new Notification("", 3000);

    List<Alumno> listaAlumnos = new ArrayList<Alumno>();
    boolean mostrarAcciones = true;


    public AlumnosView(IAlumnosService alumnosService) {

        //Se inicializa el Servicio de alumnos
        this.alumnosService = alumnosService;
        //Se configura el color de de la notificaciones
        notificacionOK.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
        notificacionKO.addThemeVariants(NotificationVariant.LUMO_ERROR);

        //Se configuran los componentes visuales y se añaden a la vista
        configurarFormulario();
        add(configurarBuscador(),configurarGrid(),configurarExportarExcel());
    }

    /**
     * Func para configurar el buscador.
     *
     */
    private Component configurarBuscador() {

        HorizontalLayout buscadorLayout = new HorizontalLayout();

        // Configuracion del filtro para buscar por nombre
        nombreFiltro.setLabel("Nombre");
        nombreFiltro.setWidth("20%");
        nombreFiltro.setClearButtonVisible(true);
        nombreFiltro.setValueChangeMode(ValueChangeMode.EAGER);

        // Configuracion del filtro para buscar por correo
        correoFiltro.setLabel("Correo");
        correoFiltro.setWidth("20%");
        correoFiltro.setClearButtonVisible(true);
        correoFiltro.setValueChangeMode(ValueChangeMode.EAGER);

        // Se habilita el btn buscar solo cuando el nombre tenga valor
        nombreFiltro.addValueChangeListener(e -> {
            buscarBtn.setEnabled(!Objects.equals(nombreFiltro.getValue(), ""));
            correoFiltro.setEnabled(Objects.equals(nombreFiltro.getValue(), ""));
        });
        // Se habilita el btn buscar solo cuando el correo tenga valor
        correoFiltro.addValueChangeListener(e -> {
            buscarBtn.setEnabled(!Objects.equals(correoFiltro.getValue(), ""));
            nombreFiltro.setEnabled(Objects.equals(correoFiltro.getValue(), ""));
        });

        //Se configuran los botones del buscador y sus listeners
        buscarBtn.setEnabled(false);
        buscarBtn.getStyle().set("cursor", "pointer");
        buscarBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        buscarBtn.addClickListener(e -> filtrar());

        mostrarTodosBtn.getStyle().set("cursor", "pointer");
        mostrarTodosBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        mostrarTodosBtn.addClickListener(e -> {
            //Limpiar los buscadores
            nombreFiltro.clear();
            correoFiltro.clear();
            // Habilitar los buscadores
            nombreFiltro.setEnabled(true);
            correoFiltro.setEnabled(true);
            obtenerTodosAlumnos();
        });

        HorizontalLayout layoutBtns = new HorizontalLayout();
        layoutBtns.setDefaultVerticalComponentAlignment(FlexComponent.Alignment.END);
        layoutBtns.add(buscarBtn,mostrarTodosBtn);

        buscadorLayout.setPadding(true);
        //Se añaden los componentes visuales
        buscadorLayout.add(nombreFiltro,correoFiltro,layoutBtns);
        return buscadorLayout;

    }

    /**
     * Func para configurar el grid de alumnos y sus columnas.
     *
     */
    private Component configurarGrid() {

        VerticalLayout layoutGrid = new VerticalLayout();
        // Se añaden las columnas al grid
        grid.addColumn(Alumno::getIdAlumno).setHeader("ID").setKey("id").setAutoWidth(true);
        grid.addColumn(Alumno::getNombre).setHeader("Nombre").setKey("nombre").setSortable(true).setAutoWidth(true);
        grid.addColumn(Alumno::getCorreo).setHeader("Correo").setKey("correo").setSortable(true).setAutoWidth(true);
        grid.addComponentColumn(item -> {
            Icon cursosBtn = new Icon(VaadinIcon.EYE);
            cursosBtn.setColor("#1B4F72");
            cursosBtn.getStyle().set("cursor", "pointer");
            cursosBtn.setSize("18px");
            cursosBtn.addClickListener(e -> verListadoCursos(item));
            return cursosBtn;
        })
        .setKey("cursos")
        .setHeader("Cursos")
        .setTextAlign(ColumnTextAlign.CENTER)
        .setVisible(mostrarAcciones);
        grid.addComponentColumn(item -> {
            Icon editarIcon = new Icon(VaadinIcon.EDIT);
            editarIcon.setColor("green");
            editarIcon.getStyle().set("cursor", "pointer");
            editarIcon.setSize("18px");
            editarIcon.addClickListener(e -> editarAlumno(item));
            return editarIcon;
        })
        .setKey("editar")
        .setHeader("Editar")
        .setTextAlign(ColumnTextAlign.CENTER)
        .setVisible(mostrarAcciones);
        grid.addComponentColumn(item -> {
            Icon editarIcon = new Icon(VaadinIcon.TRASH);
            editarIcon.setColor("red");
            editarIcon.getStyle().set("cursor", "pointer");
            editarIcon.setSize("18px");
            editarIcon.addClickListener(e -> elimarAlumno(item));
            return editarIcon;
        })
        .setKey("eliminar")
        .setHeader("Eliminar")
        .setTextAlign(ColumnTextAlign.CENTER)
        .setVisible(mostrarAcciones);

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES);
        grid.setColumnReorderingAllowed(true);
        // Número max de elementos a visualizar en cada página del grid
        grid.setPageSize(10);
        //En la carga inicial se obtienen todos los alumnos
        obtenerTodosAlumnos();
        layoutGrid.add(grid);

        return layoutGrid;
    }

    /**
     * Func para configurar el link para la descarga del Csv
     *
     */
    private Component configurarExportarExcel() {

        HorizontalLayout layoutLink = new HorizontalLayout();
        linkDescargaCsv = new Anchor(new StreamResource("Alumnos.csv", this::generarCsv), "Descargar");
        layoutLink.setJustifyContentMode(FlexComponent.JustifyContentMode.CENTER);
        layoutLink.add(linkDescargaCsv);
        return layoutLink;
    }

    /**
     * Func para configurar el formulario para el alta/modificacion de un alumno.
     *
     */
    private void configurarFormulario(){

        alumnoForm = new AlumnoForm();
        alumnoForm.addListener(AlumnoForm.GuardarEvent.class, this::guardarAlumno);
        alumnoForm.addListener(AlumnoForm.CerrarEvent.class, e -> cerrarFormulario());
        formularioDg.add(alumnoForm);
    }

    /**
     * Func para buscar un alumno por el nombre o correo.
     *
     */
    private void filtrar() {

        String nombre = nombreFiltro.getValue();
        String correo = correoFiltro.getValue();

        //Solo se filtra por un solo filtro
        if(!Objects.equals(nombre, "")){
            listaAlumnos = Arrays.asList(alumnosService.buscarAlumnosPorNombre(nombre));
        }
        if(!Objects.equals(correo, "")){
            listaAlumnos = Collections.singletonList(alumnosService.buscarAlumnoPorCorreo(correo));
        }
        if (listaAlumnos!=null){
            grid.setItems(listaAlumnos);
        }

    }

    /**
     * Func para actualizar el grid con todos los alumnos que se han dado de alta.
     *
     */
    private void obtenerTodosAlumnos() {

        listaAlumnos = Arrays.asList(alumnosService.buscarTodos());
        grid.setItems(listaAlumnos);
    }

    /**
     * Func para editar los datos de un alumno.
     *
     */
    private void editarAlumno(Alumno alumno) {

        alumnoForm.setAlumno(alumno);
        formularioDg.open();
    }

    /**
     * Func para crear o actualizar los datos de un alumno.
     *
     */
    private void guardarAlumno(AlumnoForm.GuardarEvent evt) {

        boolean result;
        Alumno alumno = evt.getAlumno();

        // Se crea un nuevo alumno o se actualiza uno existente
        if(alumno.getIdAlumno() != null && alumno.getIdAlumno() > 0) {
            result = alumnosService.actualizarAlumno(alumno);
        } else {
            result = alumnosService.guardarAlumno(alumno);
        }

        // Se muestra la notificacion indicando el restultado
        if(result){
            notificacionOK.setText("Se ha guardado correctamente el alumno");
            notificacionOK.open();
        } else {
            notificacionKO.setText("Error al guardar el curso");
            notificacionKO.open();
        }
        // Se actualiza el grid con todos los alumnos
        obtenerTodosAlumnos();
        cerrarFormulario();
    }

    /**
     * Func para eliminar un alumno.
     *
     */
    private void elimarAlumno(Alumno alumno) {

        // Se configura el Dialog para confirmar la eliminación
        Dialog confirmacionDg = new Dialog();
        Label msjConfirmacion = new Label();
        msjConfirmacion.setText("¿Desea eliminar el alumno: "+alumno.getNombre()+"?");

        HorizontalLayout btnsLayout = new HorizontalLayout();
        Button cancelarBtn = new Button("Cancelar");
        Button eliminarBtn = new Button("Eliminar");
        eliminarBtn.addThemeVariants(ButtonVariant.LUMO_ERROR);
        cancelarBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        eliminarBtn.addClickListener(click -> {
            // Se muestra la notificacion indicando el restultado
            if(alumnosService.eliminarAlumno(alumno.getIdAlumno())){
                notificacionOK.setText("Se ha eliminado correctamente el alumno");
                notificacionOK.open();
            } else {
                notificacionKO.setText("Error al eliminar el alumno");
                notificacionKO.open();
            }
            // Se actualiza el grid con todos los alumnos
            confirmacionDg.close();
            obtenerTodosAlumnos();

        });
        cancelarBtn.addClickListener(click -> {
            confirmacionDg.close();
        });

        //Se añaden los componentes visuales al Dialog
        btnsLayout.add(cancelarBtn,eliminarBtn);
        btnsLayout.setPadding(true);
        btnsLayout.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        confirmacionDg.add(msjConfirmacion,btnsLayout);
        confirmacionDg.open();
    }

    /**
     * Func para visualizar el listado de cursos que tiene un alumno.
     *
     */
    private void verListadoCursos(Alumno alumno) {

        // Se configura el Dialog para visualizar el listado de cursos de un alumno
        Dialog listadoCursosDg = new Dialog();
        H2 titulo = new H2("Cursos de: " + alumno.getNombre());
        Grid<Curso> gridCursos= new Grid<>();
        // Se configuran las columnas del grid
        gridCursos.addColumn(Curso::getIdCurso).setHeader("ID").setKey("id").setAutoWidth(true);
        gridCursos.addColumn(Curso::getNombre).setHeader("Nombre").setKey("nombre").setAutoWidth(true);
        gridCursos.addColumn(Curso::getProfesor).setHeader("Profesor").setKey("profesor").setAutoWidth(true);
        gridCursos.setItems(alumno.getCursos());

        HorizontalLayout btns = new HorizontalLayout();
        Button cerrarBtn = new Button("Cerrar");
        cerrarBtn.addClickListener(click -> {
            listadoCursosDg.close();
        });

        //Se los componentes visuales al Dialog
        btns.setJustifyContentMode(FlexComponent.JustifyContentMode.END);
        btns.getElement().getStyle().set("margin-left", "auto");
        btns.add(cerrarBtn);
        listadoCursosDg.add(titulo,gridCursos,btns);
        listadoCursosDg.setWidth("600px");
        listadoCursosDg.open();
    }

    /**
     * Func para generar el Csv con los datos del alumno
     *
     */
    private InputStream generarCsv() {

        try {
            StringWriter stringWriter = new StringWriter();
            CSVWriter csvWriter = new CSVWriter(stringWriter);
            csvWriter.setSeparatorChar(';');
            csvWriter.writeNext("ID", "Nombre", "Correo");
            listaAlumnos.forEach(a -> csvWriter.writeNext(String.valueOf(a.getIdAlumno()), a.getNombre(),a.getCorreo())
            );
            return IOUtils.toInputStream(stringWriter.toString(), "UTF8");

        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Func para cerrar el formulario de alumno.
     *
     */
    private void cerrarFormulario() {

        //Se crea un Alumno vacio para resetear el formulario
        Alumno alumno = new Alumno();
        alumnoForm.setAlumno(alumno);
        formularioDg.close();
    }
}
