package de.jklein.pharmalinkclient.views.medikamente;

import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarVariant;
import com.vaadin.flow.component.contextmenu.MenuItem;
import com.vaadin.flow.component.contextmenu.SubMenu;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import de.jklein.pharmalinkclient.dto.*;
import de.jklein.pharmalinkclient.views.MainLayout;
import jakarta.annotation.security.PermitAll;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.notification.Notification.Position;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.tabs.Tab;
import com.vaadin.flow.component.tabs.Tabs;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.splitlayout.SplitLayout;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.data.provider.ListDataProvider;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.grid.contextmenu.GridContextMenu;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.html.H4;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.NumberField;


import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;

import de.jklein.pharmalinkclient.service.StateService;
import de.jklein.pharmalinkclient.security.UserSession;
import de.jklein.pharmalinkclient.service.MedikamentService;
import de.jklein.pharmalinkclient.service.UnitService;
import com.vaadin.flow.component.Component;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import com.vaadin.flow.component.UI;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


@PageTitle("Medications")
@Route(value = "medikamente", layout = MainLayout.class)
@PermitAll
public class MedikamenteView extends VerticalLayout {

    private TextField searchField;
    private HorizontalLayout operationBar;
    private Select<String> sortBySelect;
    private Div mainContentContainer;
    private Tabs tabs;
    private Tab alleTab;
    private Tab eigeneTab;
    private MenuBar menuBar;

    private final ObjectProvider<MasterContent> masterContentProvider;
    private final ObjectProvider<DetailContent> detailContentProvider;
    private final ObjectProvider<UnitInformationContent> unitInformationContentProvider;
    private final ObjectProvider<HerstellerDetailContent> herstellerDetailContentProvider;
    private final ObjectProvider<MedikamentIpfsDataContent> medikamentIpfsDataContentProvider;

    private SplitLayout masterDetailSplitLayout;
    private DetailContent detailContentInstance;


    private final StateService stateService;
    private final UserSession userSession;
    private final MedikamentService medikamentService;
    private final UnitService unitService;


    @Autowired
    public MedikamenteView(ObjectProvider<MasterContent> masterContentProvider,
                           ObjectProvider<DetailContent> detailContentProvider,
                           ObjectProvider<UnitInformationContent> unitInformationContentProvider,
                           ObjectProvider<HerstellerDetailContent> herstellerDetailContentProvider,
                           ObjectProvider<MedikamentIpfsDataContent> medikamentIpfsDataContentProvider,
                           StateService stateService,
                           UserSession userSession,
                           MedikamentService medikamentService,
                           UnitService unitService) {
        this.masterContentProvider = masterContentProvider;
        this.detailContentProvider = detailContentProvider;
        this.unitInformationContentProvider = unitInformationContentProvider;
        this.herstellerDetailContentProvider = herstellerDetailContentProvider;
        this.medikamentIpfsDataContentProvider = medikamentIpfsDataContentProvider;
        this.stateService = stateService;
        this.userSession = userSession;
        this.medikamentService = medikamentService;
        this.unitService = unitService;

        addClassName("pharmalink-view");
        setSizeFull();
        setSpacing(false);
        setPadding(false);

        alleTab = new Tab("All");
        eigeneTab = new Tab("Owned");

        List<Tab> visibleTabs = new ArrayList<>();
        visibleTabs.add(alleTab);

        String currentActorId = stateService.getCurrentActorId();
        String userRole = null;
        if (currentActorId != null && currentActorId.contains("-")) {
            userRole = currentActorId.substring(0, currentActorId.indexOf("-")).toLowerCase();
        }

        List<String> rolesWithoutEigeneTab = Arrays.asList("behoerde", "apotheke", "grosshaendler");
        if (userRole == null || !rolesWithoutEigeneTab.contains(userRole)) {
            visibleTabs.add(eigeneTab);
        }

        tabs = new Tabs(visibleTabs.toArray(new Tab[0]));
        tabs.setWidthFull();
        add(tabs);

        tabs.addSelectedChangeListener(event -> {
            boolean isEigeneSelected = event.getSelectedTab() == eigeneTab;
            if (isEigeneSelected) {
                switchMainDetailLayout(true);
            } else {
                switchMainDetailLayout(false);
            }
            performSearch();
            updateMenuBarVisibility();
        });

        operationBar = new HorizontalLayout();
        operationBar.setWidthFull();
        operationBar.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        operationBar.setAlignItems(FlexComponent.Alignment.CENTER);
        operationBar.setPadding(true);
        operationBar.addClassName("pharmalink-operation-bar");

        HorizontalLayout searchAndSortArea = new HorizontalLayout();
        searchAndSortArea.setSpacing(true);
        searchAndSortArea.setAlignItems(FlexComponent.Alignment.CENTER);
        searchAndSortArea.addClassName("pharmalink-search-sort-area");

        sortBySelect = new Select<>();
        sortBySelect.setItems("No Filter", "angelegt", "freigegeben", "abgelehnt");
        sortBySelect.setItemLabelGenerator(this::formatStatus);
        sortBySelect.setValue("No Filter");
        sortBySelect.addValueChangeListener(event -> {
            performSearch();
        });

        searchField = new TextField();
        searchField.setPlaceholder("Search medication...");
        searchField.setWidth("300px");
        searchField.setValueChangeMode(ValueChangeMode.ON_CHANGE);
        searchField.addValueChangeListener(event -> {
            performSearch();
        });

        HorizontalLayout searchInputLayout = new HorizontalLayout();
        searchInputLayout.setSpacing(false);
        searchInputLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        searchInputLayout.addClassName("search-input-layout");
        searchInputLayout.add(searchField);

        searchAndSortArea.add(sortBySelect, searchInputLayout);
        operationBar.add(searchAndSortArea);

        menuBar = createMenuBar();
        operationBar.add(menuBar);
        updateMenuBarVisibility();

        add(operationBar);

        mainContentContainer = new Div();
        mainContentContainer.addClassName("pharmalink-main-content-container");
        mainContentContainer.setSizeFull();
        add(mainContentContainer);
        expand(mainContentContainer);

        masterDetailSplitLayout = new SplitLayout();
        masterDetailSplitLayout.setOrientation(SplitLayout.Orientation.VERTICAL);
        masterDetailSplitLayout.setSizeFull();
        masterDetailSplitLayout.addToPrimary(masterContentProvider.getObject());

        detailContentInstance = detailContentProvider.getObject();
        masterDetailSplitLayout.addToSecondary(detailContentInstance);
        mainContentContainer.add(masterDetailSplitLayout);

        detailContentInstance.setVisible(false);
        masterDetailSplitLayout.setSplitterPosition(100);

        switchMainDetailLayout(false);
        performSearch();

        stateService.addNavigateToMedikamentListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                String medId = (String) evt.getNewValue();
                if (medId != null && !medId.isEmpty()) {
                    UI.getCurrent().navigate(MedikamenteView.class);
                    tabs.setSelectedTab(alleTab);
                    searchField.setValue(medId);
                    stateService.setNavigateToMedikamentId(null);
                }
            }
        });

        stateService.addSelectedMedikamentListener(selectedMedikamentOptional -> {
            boolean isMedikamentSelected = selectedMedikamentOptional.isPresent();
            detailContentInstance.setVisible(isMedikamentSelected);
            if (isMedikamentSelected) {
                masterDetailSplitLayout.setSplitterPosition(45);
            } else {
                masterDetailSplitLayout.setSplitterPosition(100);
            }
            updateMenuBarVisibility();
        });


        masterContentProvider.getObject().addDoubleClickListener(this::showMedikamentDetailsPopup);
    }

    private void switchMainDetailLayout(boolean isEigeneSelected) {
        if (isEigeneSelected) {
            masterDetailSplitLayout.setOrientation(SplitLayout.Orientation.HORIZONTAL);
            masterDetailSplitLayout.setSplitterPosition(30);
        } else {
            masterDetailSplitLayout.setOrientation(SplitLayout.Orientation.VERTICAL);
        }
        switchRightDetailPanelContent(isEigeneSelected);
    }

    private void switchRightDetailPanelContent(boolean isEigeneSelected) {
        DetailContent detailPanel = detailContentInstance;

        SplitLayout innerRightPanelSplitLayout = new SplitLayout();
        innerRightPanelSplitLayout.setOrientation(SplitLayout.Orientation.HORIZONTAL);
        innerRightPanelSplitLayout.setSizeFull();

        innerRightPanelSplitLayout.addToPrimary(unitInformationContentProvider.getObject());

        Component dynamicRightContent;

        if (isEigeneSelected) {
            dynamicRightContent = medikamentIpfsDataContentProvider.getObject();
            innerRightPanelSplitLayout.setSplitterPosition(50);
        } else {
            dynamicRightContent = herstellerDetailContentProvider.getObject();
            innerRightPanelSplitLayout.setSplitterPosition(30);
        }

        innerRightPanelSplitLayout.addToSecondary(dynamicRightContent);

        detailPanel.setContent(innerRightPanelSplitLayout);
    }

    private void performSearch() {
        String searchTerm = searchField.getValue();
        String selectedStatus = sortBySelect.getValue();
        boolean filterByCurrentActor = tabs.getSelectedTab() == eigeneTab;

        MedikamentFilterCriteriaDto criteria = new MedikamentFilterCriteriaDto(searchTerm, selectedStatus, filterByCurrentActor);
        stateService.setCurrentMedikamentFilterCriteria(criteria);
    }

    private MenuBar createMenuBar() {
        MenuBar newMenuBar = new MenuBar();
        newMenuBar.addThemeVariants(MenuBarVariant.LUMO_DROPDOWN_INDICATORS);

        String currentActorId = stateService.getCurrentActorId();
        String userRole = null;
        if (currentActorId != null && currentActorId.contains("-")) {
            userRole = currentActorId.substring(0, currentActorId.indexOf("-")).toLowerCase();
        }

        MenuItem aktionen = newMenuBar.addItem("Actions");
        SubMenu aktionenSubMenu = aktionen.getSubMenu();

        aktionenSubMenu.addItem("View", event -> {
            Optional<MedikamentResponseDto> selectedMedikament = stateService.getSelectedMedikament();
            if (selectedMedikament.isPresent()) {
                showMedikamentDetailsPopup(selectedMedikament.get());
            } else {
                Notification notification = new Notification(
                        "Please select a medication from the list first.",
                        3000,
                        Position.MIDDLE
                );
                notification.addThemeVariants(NotificationVariant.LUMO_WARNING);
                notification.open();
            }
        });

        if (userRole != null) {
            switch (userRole) {
                case "hersteller":
                    aktionenSubMenu.addItem("Create Medication", event -> showCreateMedikamentPopup());
                    aktionenSubMenu.addItem("Edit Medication", event -> {
                        Optional<MedikamentResponseDto> selectedMedikament = stateService.getSelectedMedikament();
                        if (selectedMedikament.isPresent()) {
                            showEditMedikamentPopup(selectedMedikament.get());
                        } else {
                            Notification notification = new Notification(
                                    "Please select a medication from the list to edit first.",
                                    3000,
                                    Position.MIDDLE
                            );
                            notification.addThemeVariants(NotificationVariant.LUMO_WARNING);
                            notification.open();
                        }
                    });
                    aktionenSubMenu.addItem("Delete Medication", event -> {
                        Optional<MedikamentResponseDto> selectedMedikament = stateService.getSelectedMedikament();
                        if (selectedMedikament.isPresent()) {
                            showDeleteMedikamentConfirmation(selectedMedikament.get());
                        } else {
                            Notification.show("Please select a medication to delete first.", 3000, Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_WARNING);
                        }
                    });

                    Optional<MedikamentResponseDto> selectedMedikamentForCharge = stateService.getSelectedMedikament();
                    if (selectedMedikamentForCharge.isPresent() &&
                            selectedMedikamentForCharge.get().getStatus() != null &&
                            selectedMedikamentForCharge.get().getStatus().equalsIgnoreCase("freigegeben")) {
                        aktionenSubMenu.addItem("Create Batch", event -> {
                            Optional<MedikamentResponseDto> currentSelectedMed = stateService.getSelectedMedikament();
                            if (currentSelectedMed.isPresent() && currentSelectedMed.get().getStatus().equalsIgnoreCase("freigegeben")) {
                                showCreateChargePopup(currentSelectedMed.get());
                            } else {
                                Notification.show("Please select an approved medication to create a batch for.", 3000, Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_WARNING);
                            }
                        });
                    }
                    break;
                case "behoerde":
                    if (tabs.getSelectedTab() == alleTab) {
                        aktionenSubMenu.addItem("Set Status", event -> showSetStatusPopup());
                    }
                    break;
                default:
                    break;
            }
        }
        return newMenuBar;
    }

    private void updateMenuBarVisibility() {
        operationBar.remove(menuBar);
        menuBar = createMenuBar();
        operationBar.add(menuBar);
    }

    private void showSetStatusPopup() {
        Optional<MedikamentResponseDto> selectedMedikamentOptional = stateService.getSelectedMedikament();
        if (selectedMedikamentOptional.isEmpty()) {
            Notification.show("Please select a medication from the list first to set its status.", 3000, Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }

        MedikamentResponseDto selectedMedikament = selectedMedikamentOptional.get();

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Set Status for Medication: " + selectedMedikament.getBezeichnung());
        dialog.setWidth("400px");
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(true);

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setSpacing(true);
        dialogLayout.setPadding(true);

        Select<String> statusSelect = new Select<>();
        statusSelect.setLabel("New Status");
        statusSelect.setItems("freigegeben", "abgelehnt");
        statusSelect.setItemLabelGenerator(this::formatStatus);
        statusSelect.setValue(selectedMedikament.getStatus());
        dialogLayout.add(statusSelect);

        Button saveButton = new Button("Save Status");
        saveButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        saveButton.addClickListener(event -> {
            String newStatus = statusSelect.getValue();
            if (newStatus == null || newStatus.isEmpty()) {
                Notification.show("Please select a status.", 3000, Position.BOTTOM_START).addThemeVariants(NotificationVariant.LUMO_WARNING);
                return;
            }

            UpdateMedicationStatusRequestDto request = new UpdateMedicationStatusRequestDto(newStatus);
            boolean success = medikamentService.approveMedication(selectedMedikament.getMedId(), request);

            if (success) {
                Notification.show("Status of medication '" + selectedMedikament.getBezeichnung() + "' successfully set to '" + formatStatus(newStatus) + "'.", 5000, Position.BOTTOM_START).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                dialog.close();
                performSearch();
            } else {
                Notification.show("Error setting status for medication '" + selectedMedikament.getBezeichnung() + "'.", 5000, Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });

        Button cancelButton = new Button("Cancel", event -> dialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        dialog.getFooter().add(cancelButton, saveButton);
        dialog.add(dialogLayout);
        dialog.open();
    }


    private void showDeleteMedikamentConfirmation(MedikamentResponseDto medikamentToDelete) {
        Dialog confirmationDialog = new Dialog();
        confirmationDialog.setHeaderTitle("Delete Medication: " + medikamentToDelete.getBezeichnung() + "?");

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setSpacing(true);
        dialogLayout.setPadding(true);

        dialogLayout.add(new Div(new Span("You can only delete the medication if no units have been created for it.")));
        dialogLayout.add(new Div(new Span("Do you really want to delete the medication named '" + medikamentToDelete.getBezeichnung() + "'?")));
        dialogLayout.add(new Div(new Span("Please enter the name to confirm:")));

        TextField confirmNameField = new TextField();
        confirmNameField.setPlaceholder("Enter medication name");
        confirmNameField.setWidthFull();
        dialogLayout.add(confirmNameField);

        confirmationDialog.add(dialogLayout);
        confirmationDialog.setCloseOnEsc(true);
        confirmationDialog.setCloseOnOutsideClick(true);

        Button confirmButton = new Button("Delete", event -> {
            if (confirmNameField.getValue().equalsIgnoreCase(medikamentToDelete.getBezeichnung())) {
                boolean success = medikamentService.deleteMedikamentIfNoUnits(medikamentToDelete.getMedId());
                if (success) {
                    Notification.show("Medication '" + medikamentToDelete.getBezeichnung() + "' successfully deleted.", 5000, Position.BOTTOM_START).addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    confirmationDialog.close();
                    performSearch();
                } else {
                    Notification.show("Error deleting medication '" + medikamentToDelete.getBezeichnung() + "'. Units may still be associated with it.", 5000, Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
                }
            } else {
                Notification.show("The entered name does not match. Deletion canceled.", 3000, Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_ERROR);
            }
        });
        confirmButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

        Button cancelButton = new Button("Cancel", event -> confirmationDialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);

        confirmationDialog.getFooter().add(cancelButton, confirmButton);
        confirmationDialog.open();
    }

    public void showMedikamentDetailsPopup(MedikamentResponseDto medikament) {
        if (medikament == null) {
            Notification.show("No medication selected.", 3000, Position.MIDDLE).addThemeVariants(NotificationVariant.LUMO_WARNING);
            return;
        }

        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Medication Details: " + medikament.getBezeichnung());
        dialog.setWidth("600px");
        dialog.setHeight("auto");
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(true);

        VerticalLayout dialogLayout = new VerticalLayout();
        dialogLayout.setSpacing(true);
        dialogLayout.setPadding(true);
        dialogLayout.setSizeFull();

        FormLayout infoForm = new FormLayout();
        infoForm.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1),
                new FormLayout.ResponsiveStep("400px", 2));

        infoForm.add(createReadOnlyTextField("Medication ID", medikament.getMedId()));
        infoForm.add(createReadOnlyTextField("Name", medikament.getBezeichnung()));
        infoForm.add(createReadOnlyTextField("Manufacturer ID", medikament.getHerstellerId()));
        infoForm.add(createReadOnlyTextField("Status", formatStatus(medikament.getStatus())));
        infoForm.add(createReadOnlyTextField("IPFS Link", medikament.getIpfsLink()));

        dialogLayout.add(infoForm);

        H4 ipfsHeader = new H4("Additional IPFS Data");
        dialogLayout.add(ipfsHeader);

        Grid<IpfsEntry> ipfsDisplayGrid = new Grid<>(IpfsEntry.class, false);
        ipfsDisplayGrid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_NO_BORDER);
        ipfsDisplayGrid.setHeight("150px");
        ipfsDisplayGrid.setWidthFull();

        ipfsDisplayGrid.addColumn(IpfsEntry::getKey).setHeader("Key Name").setAutoWidth(true);
        ipfsDisplayGrid.addColumn(IpfsEntry::getValue).setHeader("Key Value").setAutoWidth(true);

        List<IpfsEntry> ipfsEntries = new ArrayList<>();
        if (medikament.getIpfsData() != null && !medikament.getIpfsData().isEmpty()) {
            medikament.getIpfsData().forEach((key, value) -> ipfsEntries.add(new IpfsEntry(key, value != null ? value.toString() : "")));
        }
        ipfsDisplayGrid.setItems(ipfsEntries);
        dialogLayout.add(ipfsDisplayGrid);

        Button closeButton = new Button("Close", event -> dialog.close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        dialog.getFooter().add(closeButton);

        dialog.add(dialogLayout);
        dialog.open();
    }

    private void showCreateMedikamentPopup() {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Create New Medication");
        dialog.setWidth("800px");
        dialog.setHeight("auto");
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(true);

        SplitLayout splitLayout = new SplitLayout();
        splitLayout.setSizeFull();
        splitLayout.setSplitterPosition(55);

        VerticalLayout leftSide = new VerticalLayout(new H4("General Information"));
        leftSide.setSpacing(true);
        leftSide.setPadding(true);

        VerticalLayout rightSide = new VerticalLayout(new H4("Additional IPFS Data"));
        rightSide.setSpacing(true);
        rightSide.setPadding(true);

        CreateMedikamentRequestDto newMedikament = new CreateMedikamentRequestDto();
        Binder<CreateMedikamentRequestDto> binder = new Binder<>(CreateMedikamentRequestDto.class);

        FormLayout detailsForm = new FormLayout();
        detailsForm.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        TextField bezeichnungField = new TextField("Name");
        TextField infoblattHashField = new TextField("Package Leaflet Hash (optional)");

        detailsForm.add(bezeichnungField, infoblattHashField);
        leftSide.add(detailsForm);

        binder.forField(bezeichnungField)
                .asRequired("Name is required")
                .bind(CreateMedikamentRequestDto::getBezeichnung, CreateMedikamentRequestDto::setBezeichnung);
        binder.forField(infoblattHashField)
                .bind(CreateMedikamentRequestDto::getInfoblattHash, CreateMedikamentRequestDto::setInfoblattHash);

        binder.setBean(newMedikament);

        Grid<IpfsEntry> ipfsEditGrid = new Grid<>(IpfsEntry.class, false);
        ipfsEditGrid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_ROW_STRIPES);
        ipfsEditGrid.setHeight("200px");

        List<IpfsEntry> editableIpfsData = new ArrayList<>();
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
                Notification notification = new Notification("Row removed.", 2000, Position.BOTTOM_START);
                notification.open();
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
                binder.writeBean(newMedikament);

                Map<String, Object> ipfsDataMap = editableIpfsData.stream()
                        .filter(entry -> entry.getKey() != null && !entry.getKey().isEmpty())
                        .collect(Collectors.toMap(IpfsEntry::getKey, IpfsEntry::getValue));
                newMedikament.setIpfsData(ipfsDataMap);

                MedikamentResponseDto createdMedikament = medikamentService.createMedikament(newMedikament);

                if (createdMedikament != null) {
                    Notification notification = new Notification(
                            "Medication '" + createdMedikament.getBezeichnung() + "' successfully created. ID: " + createdMedikament.getMedId(),
                            5000,
                            Position.BOTTOM_START
                    );
                    notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    notification.open();

                    dialog.close();
                    performSearch();
                } else {
                    Notification notification = new Notification(
                            "Error creating medication in the backend.",
                            5000,
                            Position.MIDDLE
                    );
                    notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                    notification.open();
                }

            } catch (ValidationException ex) {
                Notification notification = new Notification(
                        "Error saving: Please fill in and check all required fields.",
                        3000,
                        Position.MIDDLE
                );
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                notification.open();
            } catch (Exception ex) {
                Notification notification = new Notification(
                        "An unexpected error occurred: " + ex.getMessage(),
                        5000,
                        Position.MIDDLE
                );
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                notification.open();
                ex.printStackTrace();
            }
        });


        Button cancelButton = new Button("Cancel", event -> dialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        dialog.getFooter().add(cancelButton, saveButton);

        dialog.open();
    }

    private void showEditMedikamentPopup(MedikamentResponseDto medikamentToEdit) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Edit Medication: " + medikamentToEdit.getBezeichnung());
        dialog.setWidth("800px");
        dialog.setHeight("auto");
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(true);

        SplitLayout splitLayout = new SplitLayout();
        splitLayout.setSizeFull();
        splitLayout.setSplitterPosition(55);

        VerticalLayout leftSide = new VerticalLayout(new H4("General Information"));
        leftSide.setSpacing(true);
        leftSide.setPadding(true);

        VerticalLayout rightSide = new VerticalLayout(new H4("Additional IPFS Data"));
        rightSide.setSpacing(true);
        rightSide.setPadding(true);

        UpdateMedikamentRequestDto updateRequestDto = new UpdateMedikamentRequestDto();
        updateRequestDto.setBezeichnung(medikamentToEdit.getBezeichnung());
        updateRequestDto.setIpfsData(medikamentToEdit.getIpfsData());


        Binder<UpdateMedikamentRequestDto> binder = new Binder<>(UpdateMedikamentRequestDto.class);

        FormLayout detailsForm = new FormLayout();
        detailsForm.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        TextField medIdField = createReadOnlyTextField("Medication ID", medikamentToEdit.getMedId());
        TextField herstellerIdDisplayField = createReadOnlyTextField("Manufacturer ID", medikamentToEdit.getHerstellerId());
        TextField statusDisplayField = createReadOnlyTextField("Status", formatStatus(medikamentToEdit.getStatus()));

        TextField bezeichnungField = new TextField("Name");
        TextField infoblattHashField = new TextField("Package Leaflet Hash (optional)");

        leftSide.add(medIdField, herstellerIdDisplayField, statusDisplayField, detailsForm);
        detailsForm.add(bezeichnungField, infoblattHashField);

        binder.forField(bezeichnungField)
                .asRequired("Name is required")
                .bind(UpdateMedikamentRequestDto::getBezeichnung, UpdateMedikamentRequestDto::setBezeichnung);
        binder.forField(infoblattHashField)
                .bind(UpdateMedikamentRequestDto::getInfoblattHash, UpdateMedikamentRequestDto::setInfoblattHash);

        binder.setBean(updateRequestDto);


        Grid<IpfsEntry> ipfsEditGrid = new Grid<>(IpfsEntry.class, false);
        ipfsEditGrid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_ROW_STRIPES);
        ipfsEditGrid.setHeight("200px");

        List<IpfsEntry> editableIpfsData = new ArrayList<>();
        if (medikamentToEdit.getIpfsData() != null) {
            medikamentToEdit.getIpfsData().forEach((key, value) -> editableIpfsData.add(new IpfsEntry(key, value != null ? value.toString() : "")));
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
                Notification notification = new Notification("Row removed.", 2000, Position.BOTTOM_START);
                notification.open();
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
                binder.writeBean(updateRequestDto);

                Map<String, Object> ipfsDataMap = editableIpfsData.stream()
                        .filter(entry -> entry.getKey() != null && !entry.getKey().isEmpty())
                        .collect(Collectors.toMap(IpfsEntry::getKey, IpfsEntry::getValue));
                updateRequestDto.setIpfsData(ipfsDataMap);

                MedikamentResponseDto updatedMedikament = medikamentService.updateMedikament(medikamentToEdit.getMedId(), updateRequestDto);

                if (updatedMedikament != null) {
                    Notification notification = new Notification(
                            "Medication '" + updatedMedikament.getBezeichnung() + "' successfully updated. ID: " + updatedMedikament.getMedId(),
                            5000,
                            Position.BOTTOM_START
                    );
                    notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    notification.open();

                    dialog.close();
                    performSearch();
                } else {
                    Notification notification = new Notification(
                            "Error updating medication in the backend.",
                            5000,
                            Position.MIDDLE
                    );
                    notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                    notification.open();
                }

            } catch (ValidationException ex) {
                Notification notification = new Notification(
                        "Error saving: Please fill in and check all required fields.",
                        3000,
                        Position.MIDDLE
                );
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                notification.open();
            } catch (Exception ex) {
                Notification notification = new Notification(
                        "An unexpected error occurred: " + ex.getMessage(),
                        5000,
                        Position.MIDDLE
                );
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                notification.open();
                ex.printStackTrace();
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
                Notification notification = new Notification(
                        "Error: " + ex.getValidationErrors().get(0).getErrorMessage(),
                        3000,
                        Position.BOTTOM_START
                );
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                notification.open();
            }
        });

        Button cancelButton = new Button("Cancel", e -> rowEditDialog.close());
        rowEditDialog.getFooter().add(cancelButton, saveRowButton);
        rowEditDialog.open();
    }

    private TextField createReadOnlyTextField(String label, String value) {
        TextField textField = new TextField(label);
        textField.setReadOnly(true);
        textField.setValue(value != null ? value : "");
        textField.setWidthFull();
        return textField;
    }

    private void showCreateChargePopup(MedikamentResponseDto selectedMedikament) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle("Create New Batch for Medication: " + selectedMedikament.getBezeichnung());
        dialog.setWidth("800px");
        dialog.setHeight("auto");
        dialog.setCloseOnEsc(true);
        dialog.setCloseOnOutsideClick(true);

        SplitLayout splitLayout = new SplitLayout();
        splitLayout.setSizeFull();
        splitLayout.setSplitterPosition(55);

        VerticalLayout leftSide = new VerticalLayout(new H4("Batch Details"));
        leftSide.setSpacing(true);
        leftSide.setPadding(true);

        VerticalLayout rightSide = new VerticalLayout(new H4("Additional IPFS Data for Batch"));
        rightSide.setSpacing(true);
        rightSide.setPadding(true);

        CreateUnitsRequestDto newChargeRequest = new CreateUnitsRequestDto();
        String currentActorId = stateService.getCurrentActorId();

        Binder<CreateUnitsRequestDto> binder = new Binder<>(CreateUnitsRequestDto.class);

        FormLayout detailsForm = new FormLayout();
        detailsForm.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 1));

        TextField medIdDisplayField = createReadOnlyTextField("Medication ID", selectedMedikament.getMedId());
        TextField ownerActorIdDisplayField = createReadOnlyTextField("Owner ID", currentActorId);
        TextField chargeBezeichnungField = new TextField("Batch Name");
        NumberField anzahlField = new NumberField("Quantity");
        anzahlField.setStep(1.0);
        anzahlField.setMin(1.0);
        anzahlField.setValue(1.0);

        detailsForm.add(medIdDisplayField, ownerActorIdDisplayField, chargeBezeichnungField, anzahlField);
        leftSide.add(detailsForm);

        binder.forField(chargeBezeichnungField)
                .asRequired("Batch name is required")
                .bind(CreateUnitsRequestDto::getChargeBezeichnung, CreateUnitsRequestDto::setChargeBezeichnung);
        binder.forField(anzahlField)
                .asRequired("Quantity is required")
                .withConverter(Double::intValue, Integer::doubleValue, "Must be an integer")
                .withValidator(anzahl -> anzahl != null && anzahl > 0, "Quantity must be positive")
                .bind(CreateUnitsRequestDto::getAnzahl, CreateUnitsRequestDto::setAnzahl);

        binder.setBean(newChargeRequest);


        Grid<IpfsEntry> ipfsEditGrid = new Grid<>(IpfsEntry.class, false);
        ipfsEditGrid.addThemeVariants(GridVariant.LUMO_COMPACT, GridVariant.LUMO_ROW_STRIPES);
        ipfsEditGrid.setHeight("200px");

        List<IpfsEntry> editableIpfsData = new ArrayList<>();
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
                Notification notification = new Notification("Row removed.", 2000, Position.BOTTOM_START);
                notification.open();
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
                binder.writeBean(newChargeRequest);

                Map<String, Object> ipfsDataMap = editableIpfsData.stream()
                        .filter(entry -> entry.getKey() != null && !entry.getKey().isEmpty())
                        .collect(Collectors.toMap(IpfsEntry::getKey, IpfsEntry::getValue));
                newChargeRequest.setIpfsData(ipfsDataMap);

                boolean success = unitService.createUnitsForMedication(selectedMedikament.getMedId(), newChargeRequest);

                if (success) {
                    Notification notification = Notification.show(
                            newChargeRequest.getAnzahl() + " unit(s) for batch '" + newChargeRequest.getChargeBezeichnung() + "' successfully created.",
                            5000,
                            Position.BOTTOM_START
                    );
                    notification.addThemeVariants(NotificationVariant.LUMO_SUCCESS);
                    notification.open();

                    dialog.close();
                    performSearch();
                } else {
                    Notification notification = Notification.show(
                            "Error creating batch in the backend. Please check the backend logs.",
                            5000,
                            Position.MIDDLE
                    );
                    notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                    notification.open();
                }

            } catch (ValidationException ex) {
                Notification notification = Notification.show(
                        "Error saving: Please fill in and check all required fields. " + ex.getValidationErrors().get(0).getErrorMessage(),
                        3000,
                        Position.MIDDLE
                );
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                notification.open();
            } catch (Exception ex) {
                Notification notification = Notification.show(
                        "An unexpected error occurred: " + ex.getMessage(),
                        5000,
                        Position.MIDDLE
                );
                notification.addThemeVariants(NotificationVariant.LUMO_ERROR);
                notification.open();
                ex.printStackTrace();
            }
        });


        Button cancelButton = new Button("Cancel", event -> dialog.close());
        cancelButton.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        dialog.getFooter().add(cancelButton, saveButton);

        dialog.open();
    }

    private String formatStatus(String status) {
        if (status == null) {
            return "";
        }
        return switch (status.toLowerCase()) {
            case "angelegt", "erstellt" -> "Created";
            case "freigegeben" -> "Approved";
            case "abgelehnt" -> "Rejected";
            default -> status;
        };
    }
}