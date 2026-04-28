package com.maintainance.service_center.staff;

public enum CenterRole {
    OWNER,
    BRANCH_MANAGER,
    RECEPTIONIST,
    TECHNICIAN,
    ACCOUNTANT;

    public String getArabic() {
        switch (this) {
            case OWNER:
                return "مالك";
            case BRANCH_MANAGER:
                return "مدير الفرع";
            case RECEPTIONIST:
                return "موظف استقبال";
            case TECHNICIAN:
                return "فني";
            case ACCOUNTANT:
                return "محاسب";
            default:
                throw new IllegalArgumentException("Unknown role: " + this);
        }
    }

    public String getEnglish() {
        switch (this) {
            case OWNER:
                return "Owner";
            case BRANCH_MANAGER:
                return "Branch Manager";
            case RECEPTIONIST:
                return "Receptionist";
            case TECHNICIAN:
                return "Technician";
            case ACCOUNTANT:
                return "Accountant";
            default:
                throw new IllegalArgumentException("Unknown role: " + this);
        }
    }
}
