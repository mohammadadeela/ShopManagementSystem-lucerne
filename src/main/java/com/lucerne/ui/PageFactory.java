package com.lucerne.ui;

import com.lucerne.app.AppSession;
import com.lucerne.util.AlertUtil;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import java.util.function.Supplier;

public final class PageFactory {
    private PageFactory(){}
    public static Node create(String key){
        try{return switch(key){
            case "dashboard"->authorized("VIEW_DASHBOARD",DashboardView::new);
            case "sales"->authorized("VIEW_BRANCH_SALES",SalesView::new);
            case "pos"->authorized("CREATE_SALE",POSView::new);
            case "products"->new ProductsView();
            case "users"->authorized("MANAGE_USERS",UsersView::new);
            case "roles_permissions"->authorized("MANAGE_ROLES",RolesPermissionsView::new);
            case "inventory"->authorized("MANAGE_INVENTORY",InventoryView::new);
            case "discounts"->authorized("MANAGE_DISCOUNTS",DiscountsView::new);
            case "reports"->authorized("EXPORT_REPORTS",ReportsView::new);
            case "settings"->AppSession.hasPermission("MANAGE_SETTINGS")?new SettingsView():new ProfileView();
            case "profile"->new ProfileView();
            case "maintenance"->authorized("DATABASE_BACKUP",DatabaseMaintenanceView::new);
            default->{ModuleDefinition module=ModuleRegistry.get(key);if(module==null)yield unavailable("Unknown module");if(!AppSession.hasPermission(module.permission())&&!allowedByRole(key))yield unavailable("You are not authorized to open this module.");yield new DataPage(module);}
        };}catch(Exception e){AlertUtil.error("Page error","The page could not be opened: "+e.getMessage());return unavailable("This page could not be loaded.");}
    }
    private static Node authorized(String permission,Supplier<Node> page){return AppSession.hasPermission(permission)?page.get():unavailable("You are not authorized to open this module.");}
    private static boolean allowedByRole(String key){return switch(AppSession.current().role()){
        case OWNER,ADMIN->true;case MANAGER->!key.equals("users")&&!key.equals("audit");case CASHIER->java.util.Set.of("sales","customers","returns","daily_closing","notifications").contains(key);case WAREHOUSE->java.util.Set.of("inventory","stock_requests","purchase_orders","suppliers","notifications").contains(key);case CUSTOMER->java.util.Set.of("orders","returns","notifications").contains(key);};}
    private static Node unavailable(String text){Label label=new Label(text);label.getStyleClass().add("empty-state");return new StackPane(label);}
}
