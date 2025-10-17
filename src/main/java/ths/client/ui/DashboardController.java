package ths.client.ui;

import ths.client.App;

public class DashboardController {
    public void goBookings(){ App.load("booking.fxml", "THS — Bookings"); }
    public void goVitals(){ App.load("vitals.fxml", "THS — Vitals"); }
    public void goRx(){ App.load("prescriptions.fxml", "THS — Prescriptions");
    }
}
