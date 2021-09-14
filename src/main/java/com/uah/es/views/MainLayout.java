package com.uah.es.views;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import com.uah.es.service.IUsuariosService;
import com.uah.es.views.cursos.CursosView;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.Text;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.applayout.DrawerToggle;
import com.vaadin.flow.component.avatar.AvatarVariant;
import com.vaadin.flow.component.html.*;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.tabs.TabsVariant;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.server.PWA;
import com.vaadin.flow.server.VaadinServlet;
import com.vaadin.flow.theme.Theme;
import com.vaadin.flow.component.avatar.Avatar;

/**
 * Esta es la vista principal de la App, en ella están los links y acceso a las demas vistas.
 */
@PWA(name = "Gestión Cursos", shortName = "Gestión Cursos", enableInstallPrompt = false)
@Theme(themeFolder = "gestincursos")
@PageTitle("Main")
public class MainLayout extends AppLayout {

    public static class MenuItemInfo {

        private String text;
        private String iconClass;
        private Class<? extends Component> view;

        public MenuItemInfo(String text, String iconClass, Class<? extends Component> view) {
            this.text = text;
            this.iconClass = iconClass;
            this.view = view;
        }

        public String getText() {
            return text;
        }
        public String getIconClass() {
            return iconClass;
        }
        public Class<? extends Component> getView() {
            return view;
        }

    }

    private final Tabs menu;
    private H1 viewTitle;

    public MainLayout(IUsuariosService usuariosService) {
        //this.usuariosService=usuariosService;
        addToNavbar(true, createHeaderContent());
        menu = createMenu();
        addToDrawer(createDrawerContent(menu));
    }

    private Component createHeaderContent() {

        HorizontalLayout layout = new HorizontalLayout();
        layout.setClassName("sidemenu-header");
        layout.getThemeList().set("dark", true);
        layout.setWidthFull();
        layout.setSpacing(false);
        layout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.add(new DrawerToggle());
        viewTitle = new H1();
        layout.add(viewTitle);

        return layout;
    }

    private Component createDrawerContent(Tabs menu) {

        VerticalLayout layout = new VerticalLayout();
        layout.setClassName("sidemenu-menu");
        layout.setSizeFull();
        layout.setPadding(false);
        layout.setSpacing(false);
        layout.getThemeList().set("spacing-s", true);
        layout.setAlignItems(FlexComponent.Alignment.STRETCH);
        VerticalLayout usuarioLayout = new VerticalLayout();

        Avatar usuarioAvatar = new Avatar();
        usuarioAvatar.addThemeVariants(AvatarVariant.LUMO_XLARGE);
        usuarioLayout.add(usuarioAvatar);
        usuarioLayout.add(new Label("Nombre Usuario"));
        usuarioLayout.add(createLogoutLink());
        usuarioLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        layout.add(usuarioLayout,menu);

        return layout;
    }

    private Tabs createMenu() {

        final Tabs tabs = new Tabs();
        tabs.setOrientation(Tabs.Orientation.VERTICAL);
        tabs.addThemeVariants(TabsVariant.LUMO_MINIMAL);
        tabs.setId("tabs");
        for (Tab menuTab : createMenuItems()) {
            tabs.add(menuTab);
        }
        return tabs;
    }

    private List<Tab> createMenuItems() {
        MenuItemInfo[] menuItems = new MenuItemInfo[]{
                new MenuItemInfo("Cursos", "la la-book-open", CursosView.class),
        };

        List<Tab> tabs = new ArrayList<>();
        for (MenuItemInfo menuItemInfo : menuItems) {
            //Verificamos si el usuario tiene el rol que permita visualizar la vista
            //if (SecurityUtils.isAccessGranted(menuItemInfo.getView())){
                tabs.add(createTab(menuItemInfo));
            //}
        }
        return tabs;
    }

    private static Tab createTab(MenuItemInfo menuItemInfo) {
        Tab tab = new Tab();
        RouterLink link = new RouterLink();
        link.setRoute(menuItemInfo.getView());
        Span iconElement = new Span();
        iconElement.addClassNames("text-l", "pr-s");
        if (!menuItemInfo.getIconClass().isEmpty()) {
            iconElement.addClassNames(menuItemInfo.getIconClass());
        }
        link.add(iconElement, new Text(menuItemInfo.getText()));
        tab.add(link);
        ComponentUtil.setData(tab, Class.class, menuItemInfo.getView());
        return tab;
    }

    private static Anchor createLogoutLink() {
        String contextPath = VaadinServlet.getCurrent().getServletContext().getContextPath();
        Anchor a = new Anchor();
        a.addClassNames("link-margin");
        a.setText("Cerrar sesión");
        a.setHref(contextPath + "/logout");
        return a;
    }

    @Override
    protected void afterNavigation() {
        super.afterNavigation();
        getTabForComponent(getContent()).ifPresent(menu::setSelectedTab);
        viewTitle.setText(getCurrentPageTitle());
    }

    private Optional<Tab> getTabForComponent(Component component) {
        return menu.getChildren().filter(tab -> ComponentUtil.getData(tab, Class.class).equals(component.getClass()))
                .findFirst().map(Tab.class::cast);
    }

    private String getCurrentPageTitle() {
        PageTitle title = getContent().getClass().getAnnotation(PageTitle.class);
        return title == null ? "" : title.value();
    }
}
