// Copyright (c) 2026 MLSAC. All Rights Reserved.
package italy.mlsac.net.staff;

public interface StaffChannel {

    StaffChannel NONE = new StaffChannel() {
        @Override
        public void toAlertSubscribers(String coloredLine) {
        }

        @Override
        public void toAllStaff(String coloredLine) {
        }
    };

    void toAlertSubscribers(String coloredLine);

    void toAllStaff(String coloredLine);
}
