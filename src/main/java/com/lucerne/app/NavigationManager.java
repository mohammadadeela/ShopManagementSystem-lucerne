package com.lucerne.app;

import com.lucerne.model.Role;
import java.util.*;

public final class NavigationManager {
    private NavigationManager(){}
    public static List<NavGroup> forRole(Role role){return switch(role){
        case OWNER,ADMIN -> List.of(
                group("Overview",item("dashboard","Dashboard","⌂")),
                group("Commerce",item("sales","Sales & Transactions","$"),item("orders","Orders","▣"),item("returns","Returns & Exchanges","↺")),
                group("Catalog & Stock",item("products","Products","◇"),item("inventory","Inventory","▦"),item("stock_requests","Stock Requests","⇄"),item("suppliers","Suppliers","♢"),item("purchase_orders","Purchase Orders","▧")),
                group("People & Finance",item("customers","Customers","♙"),item("employees","Employees","♟"),item("expenses","Expenses","−"),item("discounts","Discounts","%"),item("daily_closing","Daily Closing","✓")),
                group("Analytics",item("reports","Reports & Analytics","⌁")),
                group("Administration",item("users","Manage Users","⚿"),item("roles_permissions","Roles & Permissions","⌘"),item("audit","Audit Log","≡"),item("notifications","Notifications","●"),item("maintenance","Database Maintenance","⚙"),item("settings","System Settings","⚙")));
        case MANAGER -> List.of(group("Overview",item("dashboard","Dashboard","⌂")),group("Branch Operations",item("sales","Branch Sales","$"),item("orders","Orders","▣"),item("inventory","Branch Inventory","▦"),item("products","Products","◇"),item("customers","Customers","♙"),item("employees","Employees","♟"),item("discounts","Discounts","%"),item("stock_requests","Stock Requests","⇄"),item("returns","Returns","↺"),item("daily_closing","Daily Closing","✓")),group("Insights",item("reports","Reports","⌁"),item("notifications","Notifications","●"),item("settings","Profile & Settings","⚙")));
        case CASHIER -> List.of(group("Overview",item("dashboard","Dashboard","⌂")),group("Sales",item("pos","Point of Sale","▤"),item("customers","Customers","♙"),item("sales","My Sales","$"),item("returns","Returns & Exchanges","↺"),item("daily_closing","Daily Closing","✓")),group("Account",item("notifications","Notifications","●"),item("settings","Profile","⚙")));
        case WAREHOUSE -> List.of(group("Overview",item("dashboard","Dashboard","⌂")),group("Warehouse",item("inventory","Inventory","▦"),item("stock_requests","Stock Requests","⇄"),item("purchase_orders","Purchase Orders","▧"),item("suppliers","Suppliers","♢"),item("products","Products","◇")),group("Insights",item("reports","Reports","⌁"),item("notifications","Notifications","●"),item("settings","Profile","⚙")));
        case CUSTOMER -> List.of(group("Shop",item("dashboard","My Overview","⌂"),item("products","Store","◇"),item("orders","My Orders","▣"),item("returns","Returns & Exchanges","↺")),group("Account",item("notifications","Notifications","●"),item("settings","Profile","⚙")));
    };}
    private static NavGroup group(String title,NavItem...items){return new NavGroup(title,List.of(items));}
    private static NavItem item(String key,String label,String icon){return new NavItem(key,label,icon);}
    public record NavGroup(String title,List<NavItem> items){}
    public record NavItem(String key,String label,String icon){}
}
