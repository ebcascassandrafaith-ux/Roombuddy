package com.template.communication;

import com.template.browse.Navigator;
import com.template.dao.CommunicationDAO;
import com.template.model.Listing;
import com.template.model.ViewingSchedule;
import com.template.util.Async;
import com.template.util.Session;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ScheduleViewingController {
    @FXML private Label listingLabel;
    @FXML private DatePicker datePicker;
    @FXML private ComboBox<String> timePicker;
    @FXML private VBox schedulesBox;

    private final CommunicationDAO dao = new CommunicationDAO();
    private Navigator navigator;
    private Listing listing;

    public void show(Listing listing, Navigator navigator) {
        this.listing = listing;
        this.navigator = navigator;
        listingLabel.setText(listing.getPropertyName());
        datePicker.setValue(LocalDate.now().plusDays(1));
        timePicker.getSelectionModel().select("09:00");
        reloadSchedules();
    }

    @FXML
    private void initialize() {
        timePicker.getItems().addAll(
                "08:00", "09:00", "10:00", "11:00", "13:00", "14:00", "15:00", "16:00", "17:00");
    }

    @FXML
    private void onBack() {
        if (navigator != null) navigator.goBack();
    }

    @FXML
    private void onSubmitRequest() {
        LocalDate date = datePicker.getValue();
        String time = timePicker.getValue();
        if (date == null || time == null) {
            showError("Please choose a date and time.");
            return;
        }
        LocalDateTime at = LocalDateTime.of(date, LocalTime.parse(time));
        Async.run(() -> dao.createViewingRequest(listing.getListingId(), Session.getUserId(),
                        listing.getOwnerId(), at),
                schedule -> reloadSchedules(),
                this::showError);
    }

    private void reloadSchedules() {
        Async.run(() -> dao.findViewingSchedules(listing.getListingId(), Session.getUserId()),
                this::renderSchedules, this::showError);
    }

    private void renderSchedules(List<ViewingSchedule> schedules) {
        schedulesBox.getChildren().clear();
        if (schedules.isEmpty()) {
            Label empty = new Label("No viewing requests yet.");
            empty.getStyleClass().add("muted-text");
            schedulesBox.getChildren().add(empty);
            return;
        }

        for (ViewingSchedule schedule : schedules) {
            VBox card = new VBox(8);
            card.getStyleClass().add("schedule-card");

            Label date = new Label(schedule.getScheduledAt() == null ? ""
                    : schedule.getScheduledAt().format(DateTimeFormatter.ofPattern("MMM d, yyyy - h:mm a")));
            date.getStyleClass().add("schedule-date");
            Label status = new Label(schedule.getStatus().replace('_', ' ').toUpperCase());
            status.getStyleClass().addAll("schedule-status", "schedule-status-" + schedule.getStatus());
            card.getChildren().addAll(date, status);

            if ("pending".equals(schedule.getStatus())
                    && Session.getUserId() == schedule.getLandlordId()) {
                Button confirm = new Button("Confirm");
                confirm.getStyleClass().add("primary-button");
                confirm.setOnAction(e -> updateSchedule(schedule, "confirmed", null));

                Button decline = new Button("Decline");
                decline.getStyleClass().add("secondary-button");
                decline.setOnAction(e -> updateSchedule(schedule, "declined", null));

                Button propose = new Button("Propose new time");
                propose.getStyleClass().add("secondary-button");
                propose.setOnAction(e -> proposeNewTime(schedule));
                card.getChildren().add(new javafx.scene.layout.HBox(8, confirm, decline, propose));
            }
            schedulesBox.getChildren().add(card);
        }
    }

    private void proposeNewTime(ViewingSchedule schedule) {
        LocalDate date = datePicker.getValue();
        String time = timePicker.getValue();
        if (date == null || time == null) {
            showError("Please choose a date and time.");
            return;
        }
        updateSchedule(schedule, "reschedule_proposed",
                LocalDateTime.of(date, LocalTime.parse(time)));
    }

    private void updateSchedule(ViewingSchedule schedule, String status, LocalDateTime proposedTime) {
        Async.run(() -> {
            dao.updateViewingStatus(schedule.getViewingId(), Session.getUserId(), status, proposedTime);
            return null;
        }, ignored -> reloadSchedules(), this::showError);
    }

    private void showError(Throwable error) {
        showError(error.getMessage());
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("RoomBuddy");
        alert.setHeaderText("Viewing schedule");
        alert.setContentText(message);
        alert.show();
    }
}
