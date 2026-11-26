package de.jklein.pharmalinkclient.views.actorexplorer;

import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.jklein.pharmalinkclient.dto.ActorUpdateRequestDto;
import de.jklein.pharmalinkclient.views.MainLayout;
import jakarta.annotation.security.PermitAll;

import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;


import de.jklein.pharmalinkclient.dto.ActorFilterCriteriaDto;
import de.jklein.pharmalinkclient.dto.ActorResponseDto;
import de.jklein.pharmalinkclient.dto.IpfsEntry;
import de.jklein.pharmalinkclient.service.StateService;
import de.jklein.pharmalinkclient.service.ActorService;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Optional;
import java.util.ArrayList;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;

@PageTitle("Actor Explorer")
@Route(value = "actorExplorer", layout = MainLayout.class)
@PermitAll
public class ActorExplorerView extends VerticalLayout {

    private final ObjectProvider<ActorMasterContent> actorMasterContentProvider;
    private final ObjectProvider<ActorDetailContent> actorDetailContentProvider;
    private final StateService stateService;
    private final ActorService actorService;

    private TextField searchField;


    @Autowired
    public ActorExplorerView(ObjectProvider<ActorMasterContent> actorMasterContentProvider,
                             ObjectProvider<ActorDetailContent> actorDetailContentProvider,
                             StateService stateService,
                             ActorService actorService) {
        this.actorMasterContentProvider = actorMasterContentProvider;
        this.actorDetailContentProvider = actorDetailContentProvider;
        this.stateService = stateService;
        this.actorService = actorService;

        addClassName("actor-explorer-view");
        setSizeFull();
        setSpacing(false);
        setPadding(false);

        HorizontalLayout searchBar = new HorizontalLayout();
        searchBar.setWidthFull();
        searchBar.setPadding(true);
        searchBar.setAlignItems(FlexComponent.Alignment.CENTER);
        searchBar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        searchBar.addClassName("actor-explorer-search-bar");

        searchField = new TextField();
        searchField.setPlaceholder("Search actor (Name, ID, Email)...");
        searchField.setWidth("400px");
        searchField.setValueChangeMode(ValueChangeMode.ON_CHANGE);
        searchField.addValueChangeListener(event -> performActorSearch());

        MenuBar actionsMenuBar = createActorActionsMenuBar();

        searchBar.add(searchField, actionsMenuBar);
        add(searchBar);


        ActorMasterContent master = actorMasterContentProvider.getObject();
        ActorDetailContent detail = actorDetailContentProvider.getObject();

        SplitLayout splitLayout = new SplitLayout(master, detail);
        splitLayout.setSplitterPosition(50);
        splitLayout.setSizeFull();
        splitLayout.addClassName("actor-explorer-split-layout");

        add(splitLayout);
        expand(splitLayout);

        performActorSearch();
    }

    private void performActorSearch() {
        String searchTerm = searchField.getValue();
        ActorFilterCriteriaDto criteria = new ActorFilterCriteriaDto(searchTerm);
        stateService.setCurrentActorFilterCriteria(criteria);
    }

    private MenuBar createActorActionsMenuBar() {
        MenuBar menuBar = new MenuBar();
        menuBar.addThemeVariants(MenuBarVariant.LUMO_DROPDOWN_INDICATORS);

        MenuItem meineStammdaten = menuBar.addItem("My Master Data");
        SubMenu stammdatenSubMenu = meineStammdaten.getSubMenu();

        stammdatenSubMenu.addItem("View Master Data", event -> {
            String currentActorId = stateService.getCurrentActorId();
            if (currentActorId != null && !currentActorId.isEmpty()) {
                List<ActorResponseDto> allActors = stateService.getAllLoadedActors();

                Optional<ActorResponseDto> myActorDetailsOptional = allActors.stream()
                        .filter(actor -> actor.getActorId() != null && actor.getActorId().equals(currentActorId))
                        .findFirst();

                if (myActorDetailsOptional.isPresent()) {
                    showActorDetailsPopup(myActorDetailsOptional.get());
                } else {
                    Notification.show("Could not find master data for Actor ID " + currentActorId + " in locally loaded data. The data may not be fully loaded yet or the actor does not exist.", 5000, Notification.Position.MIDDLE);
                }
            } else {
                Notification.show("Your Actor ID is not available. Please sign in again or check the dashboard.", 5000, Notification.Position.MIDDLE);
            }
        });

        stammdatenSubMenu.addItem("Edit Master Data", event -> {
            String currentActorId = stateService.getCurrentActorId();
            if (currentActorId != null && !currentActorId.isEmpty()) {
                List<ActorResponseDto> allActors = stateService.getAllLoadedActors();

                Optional<ActorResponseDto> myActorDetailsOptional = allActors.stream()
                        .filter(actor -> actor.getActorId() != null && actor.getActorId().equals(currentActorId))
                        .findFirst();

                if (myActorDetailsOptional.isPresent()) {
                    showActorEditPopup(myActorDetailsOptional.get());
                } else {
                    Notification.show("Could not find master data for Actor ID " + currentActorId + " in locally loaded data. Editing is not possible.", 5000, Notification.Position.MIDDLE);
                }
            } else {
                Notification.show("Your Actor ID is not available. Editing is not possible.", 5000, Notification.Position.MIDDLE);
            }
        });

        return menuBar;
    }

    private void showActorDetailsPopup(ActorResponseDto actor) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("My Master Data: " + actor.getBezeichnung());

        VerticalLayout dialogContent = new VerticalLayout();
        dialogContent.setSpacing(false);
        dialogContent.setPadding(false);

        FormLayout detailsForm = new FormLayout();
        detailsForm.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1), new FormLayout.ResponsiveStep("400px", 2));
        detailsForm.add(createReadOnlyTextField("Actor ID", actor.getActorId()));
        detailsForm.add(createReadOnlyTextField("Name", actor.getBezeichnung()));
        detailsForm.add(createReadOnlyTextField("Role", actor.getRole()));
        detailsForm.add(createReadOnlyTextField("Email", actor.getEmail()));
        dialogContent.add(detailsForm);

        if (actor.getIpfsData() != null && !actor.getIpfsData().isEmpty()) {
            dialogContent.add(new H4("Additional IPFS Data:"));
            Grid<Map.Entry<String, Object>> ipfsGrid = new Grid<>();
            ipfsGrid.addThemeVariants(GridVariant.LUMO_NO_BORDER, GridVariant.LUMO_ROW_STRIPES);
            ipfsGrid.addColumn(Map.Entry::getKey).setHeader("Key").setAutoWidth(true);
            ipfsGrid.addColumn(Map.Entry::getValue).setHeader("Value").setAutoWidth(true);
            ipfsGrid.setItems(actor.getIpfsData().entrySet());
            ipfsGrid.setHeight("200px");
            dialogContent.add(ipfsGrid);
        } else {
            dialogContent.add(new Span("No additional IPFS data available."));
        }

        dialog.add(dialogContent);

        Button closeButton = new Button("Close", event -> dialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        dialog.getFooter().add(closeButton);

        dialog.open();
    }

    private void showActorEditPopup(ActorResponseDto actorToEdit) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Edit Master Data: " + actorToEdit.getBezeichnung());
        dialog.setWidth("800px");
        dialog.setHeight("auto");

        SplitLayout splitLayout = new SplitLayout();
        splitLayout.setSizeFull();
        splitLayout.setSplitterPosition(55);

        VerticalLayout leftSide = new VerticalLayout(new H4("General Data"));
        leftSide.setSpacing(true);
        leftSide.setPadding(true);

        VerticalLayout rightSide = new VerticalLayout(new H4("Additional IPFS Data"));
        rightSide.setSpacing(true);
        rightSide.setPadding(true);

        Binder<ActorResponseDto> binder = new Binder<>(ActorResponseDto.class);
        FormLayout detailsForm = new FormLayout();
        detailsForm.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        TextField actorIdField = createReadOnlyTextField("Actor ID", actorToEdit.getActorId());
        TextField bezeichnungField = new TextField("Name");
        TextField roleField = createReadOnlyTextField("Role", actorToEdit.getRole());
        TextField emailField = new TextField("Email");

        detailsForm.add(actorIdField, roleField, bezeichnungField, emailField);
        leftSide.add(detailsForm);

        binder.forField(bezeichnungField).asRequired("Name must not be empty").bind(ActorResponseDto::getBezeichnung, ActorResponseDto::setBezeichnung);
        binder.forField(emailField).asRequired("Email is required").bind(ActorResponseDto::getEmail, ActorResponseDto::setEmail);
        binder.readBean(actorToEdit);

        Grid<IpfsEntry> ipfsEditGrid = new Grid<>(IpfsEntry.class, false);
        ipfsEditGrid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_ROW_STRIPES);
        ipfsEditGrid.setHeight("200px");

        List<IpfsEntry> editableIpfsData = new ArrayList<>();
        if (actorToEdit.getIpfsData() != null) {
            actorToEdit.getIpfsData().forEach((key, value) -> editableIpfsData.add(new IpfsEntry(key, value != null ? value.toString() : "")));
        }
        ListDataProvider<IpfsEntry> ipfsDataProvider = new ListDataProvider<>(editableIpfsData);
        ipfsEditGrid.setDataProvider(ipfsDataProvider);

        ipfsEditGrid.addColumn(IpfsEntry::getKey).setHeader("Key Name").setSortable(true);
        ipfsEditGrid.addColumn(IpfsEntry::getValue).setHeader("Key Value").setSortable(true);

        GridContextMenu<IpfsEntry> contextMenu = ipfsEditGrid.addContextMenu();
        contextMenu.addItem("Edit Row", event -> event.getItem().ifPresent(item -> showIpfsEntryRowEditDialog(item, ipfsDataProvider)));

        Button addRowButton = new Button("Add Row", VaadinIcon.PLUS.create());
        addRowButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE);
        addRowButton.addClickListener(e -> showIpfsEntryRowEditDialog(new IpfsEntry("", ""), ipfsDataProvider));

        Button removeRowButton = new Button("Remove Row", VaadinIcon.TRASH.create());
        removeRowButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY_INLINE, ButtonVariant.LUMO_ERROR);
        removeRowButton.setVisible(false);

        ipfsEditGrid.asSingleSelect().addValueChangeListener(event -> {
            removeRowButton.setVisible(event.getValue() != null);
        });

        removeRowButton.addClickListener(e -> {
            IpfsEntry selectedEntry = ipfsEditGrid.asSingleSelect().getValue();
            if (selectedEntry != null) {
                editableIpfsData.remove(selectedEntry);
                ipfsDataProvider.refreshAll();
                Notification.show("Row removed.", 2000, Notification.Position.BOTTOM_START);
            }
        });

        HorizontalLayout buttonLayout = new HorizontalLayout(addRowButton, removeRowButton);
        rightSide.add(ipfsEditGrid, buttonLayout);

        splitLayout.addToPrimary(leftSide);
        splitLayout.addToSecondary(rightSide);
        dialog.add(splitLayout);

        Button saveButton = new Button("Save");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(e -> {
            try {
                binder.writeBean(actorToEdit);

                ActorUpdateRequestDto updateRequest = new ActorUpdateRequestDto();
                updateRequest.setName(actorToEdit.getBezeichnung());
                updateRequest.setEmail(actorToEdit.getEmail()); // Email is now set
                Map<String, Object> updatedIpfsData = editableIpfsData.stream()
                        .filter(entry -> entry.getKey() != null && !entry.getKey().isEmpty())
                        .collect(Collectors.toMap(IpfsEntry::getKey, IpfsEntry::getValue));
                updateRequest.setIpfsData(updatedIpfsData);

                boolean success = actorService.updateActor(actorToEdit.getActorId(), updateRequest);

                if (success) {
                    Notification.show("Master data successfully saved.", 3000, Notification.Position.MIDDLE);
                    dialog.close();
                    performActorSearch();
                } else {
                    Notification.show("Error saving master data in the backend.", 5000, Notification.Position.MIDDLE);
                }

            } catch (ValidationException ex) {
                Notification.show("Error saving: Please fill in all required fields.", 3000, Notification.Position.MIDDLE);
            }
        });


        Button cancelButton = new Button("Cancel", event -> dialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        dialog.getFooter().add(cancelButton, saveButton);

        dialog.open();
    }

    private void showIpfsEntryRowEditDialog(IpfsEntry entry, ListDataProvider<IpfsEntry> dataProvider) {
        Dialog rowEditDialog = new Dialog();
        boolean isNewEntry = entry.getKey() == null || entry.getKey().isEmpty();
        rowEditDialog.setHeaderTitle(isNewEntry ? "New IPFS Row" : "Edit IPFS Row");

        Binder<IpfsEntry> rowBinder = new Binder<>(IpfsEntry.class);
        TextField keyField = new TextField("Key Name");
        TextField valueField = new TextField("Key Value");
        keyField.setWidthFull();
        valueField.setWidthFull();

        rowBinder.forField(keyField).asRequired("Key must not be empty").bind(IpfsEntry::getKey, IpfsEntry::setKey);
        rowBinder.forField(valueField).bind(IpfsEntry::getValue, IpfsEntry::setValue);
        rowBinder.setBean(entry);

        rowEditDialog.add(new VerticalLayout(keyField, valueField));

        Button saveRowButton = new Button("Save");
        saveRowButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveRowButton.addClickListener(e -> {
            try {
                rowBinder.writeBean(entry);
                if (isNewEntry && !((List<IpfsEntry>) dataProvider.getItems()).contains(entry)) {
                    ((List<IpfsEntry>) dataProvider.getItems()).add(entry);
                }
                dataProvider.refreshAll();
                rowEditDialog.close();
            } catch (ValidationException ex) {
                Notification.show("Error: " + ex.getValidationErrors().get(0).getErrorMessage(), 3000, Notification.Position.BOTTOM_START);
            }
        });

        Button cancelRowButton = new Button("Cancel", e -> rowEditDialog.close());
        rowEditDialog.getFooter().add(cancelRowButton, saveRowButton);
        rowEditDialog.open();
    }

    private TextField createReadOnlyTextField(String label, String value) {
        TextField textField = new TextField(label);
        textField.setReadOnly(true);
        textField.setValue(value != null ? value : "");
        textField.setWidthFull();
        return textField;
    }
}