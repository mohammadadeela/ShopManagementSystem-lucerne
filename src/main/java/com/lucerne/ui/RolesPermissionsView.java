package com.lucerne.ui;

import com.lucerne.dao.RolePermissionDAO;
import com.lucerne.ui.components.LoadingPane;
import com.lucerne.util.AlertUtil;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public final class RolesPermissionsView extends StackPane {
    private final RolePermissionDAO dao=new RolePermissionDAO();
    private final ComboBox<RolePermissionDAO.RoleOption> roles=new ComboBox<>();
    private final VBox permissionList=new VBox(8);
    private final Map<Integer,CheckBox> checks=new LinkedHashMap<>();
    private final Label roleDescription=new Label("Select a role to inspect its effective role grants.");
    private final Label count=new Label("0 permissions granted");
    private final LoadingPane loading=new LoadingPane();

    public RolesPermissionsView(){getChildren().addAll(build(),loading);loadRoles();}

    private VBox build(){
        VBox root=new VBox(18);root.setPadding(new Insets(24));root.getStyleClass().add("page-root");
        Label title=new Label("Roles & Permissions");title.getStyleClass().add("page-title");
        Label description=new Label("Control database-backed role grants. Sensitive service operations verify the same permissions.");description.getStyleClass().add("page-description");description.setWrapText(true);
        roles.setPrefWidth(230);roles.setPromptText("Select role");roles.setOnAction(e->loadPermissions());
        roleDescription.getStyleClass().add("secondary-text");roleDescription.setWrapText(true);
        Button selectAll=new Button("Select all");selectAll.setOnAction(e->{checks.values().forEach(c->c.setSelected(true));updateCount();});
        Button clear=new Button("Clear all");clear.setOnAction(e->{checks.values().forEach(c->c.setSelected(false));updateCount();});
        Button save=new Button("Save permissions");save.getStyleClass().add("primary-button");save.setOnAction(e->save());
        HBox controls=new HBox(10,new Label("Role"),roles,selectAll,clear,new Region(),count,save);controls.setAlignment(Pos.CENTER_LEFT);HBox.setHgrow(controls.getChildren().get(4),Priority.ALWAYS);
        permissionList.setPadding(new Insets(6));
        ScrollPane scroll=new ScrollPane(permissionList);scroll.setFitToWidth(true);scroll.setPrefViewportHeight(560);scroll.getStyleClass().add("content-card");
        root.getChildren().addAll(new VBox(4,title,description),roleDescription,controls,scroll);VBox.setVgrow(scroll,Priority.ALWAYS);return root;
    }

    private void loadRoles(){loading.show("Loading security roles…");Task<List<RolePermissionDAO.RoleOption>> task=new Task<>(){protected List<RolePermissionDAO.RoleOption> call()throws Exception{return dao.roles();}};task.setOnSucceeded(e->{loading.hide();roles.getItems().setAll(task.getValue());if(!roles.getItems().isEmpty())roles.getSelectionModel().selectFirst();loadPermissions();});task.setOnFailed(e->{loading.hide();AlertUtil.error("Roles unavailable","The role list could not be loaded.");});start(task,"roles-load");}

    private void loadPermissions(){RolePermissionDAO.RoleOption role=roles.getValue();if(role==null)return;loading.show("Loading permissions…");Task<List<RolePermissionDAO.PermissionChoice>> task=new Task<>(){protected List<RolePermissionDAO.PermissionChoice> call()throws Exception{return dao.permissions(role.id());}};task.setOnSucceeded(e->{loading.hide();render(role,task.getValue());});task.setOnFailed(e->{loading.hide();AlertUtil.error("Permissions unavailable","The selected role grants could not be loaded.");});start(task,"permissions-load");}

    private void render(RolePermissionDAO.RoleOption role,List<RolePermissionDAO.PermissionChoice> permissions){
        checks.clear();permissionList.getChildren().clear();roleDescription.setText(role.name()+" — "+(role.description()==null?"No description":role.description()));
        for(RolePermissionDAO.PermissionChoice permission:permissions){
            CheckBox check=new CheckBox(permission.code());check.setSelected(permission.granted());check.setOnAction(e->updateCount());
            Label detail=new Label(permission.description()==null?"":permission.description());detail.getStyleClass().add("secondary-text");detail.setWrapText(true);
            VBox item=new VBox(3,check,detail);item.getStyleClass().add("permission-row");item.setPadding(new Insets(8,10,8,10));permissionList.getChildren().add(item);checks.put(permission.id(),check);
        }
        updateCount();
    }

    private void updateCount(){long selected=checks.values().stream().filter(CheckBox::isSelected).count();count.setText(selected+" of "+checks.size()+" permissions granted");}

    private void save(){RolePermissionDAO.RoleOption role=roles.getValue();if(role==null)return;Set<Integer> selected=new LinkedHashSet<>();checks.forEach((id,check)->{if(check.isSelected())selected.add(id);});if(!AlertUtil.confirm("Save role permissions","Replace the current grants for "+role.name()+" with the selected "+selected.size()+" permissions?"))return;loading.show("Saving role permissions…");Task<Void> task=new Task<>(){protected Void call()throws Exception{dao.save(role,selected);return null;}};task.setOnSucceeded(e->{loading.hide();AlertUtil.info("Permissions saved","New sessions for "+role.name()+" will use the updated grants.");loadPermissions();});task.setOnFailed(e->{loading.hide();AlertUtil.error("Permissions not saved",task.getException()==null?"The permission update was rolled back.":task.getException().getMessage());});start(task,"permissions-save");}

    private static void start(Task<?> task,String name){Thread thread=new Thread(task,name);thread.setDaemon(true);thread.start();}
}
