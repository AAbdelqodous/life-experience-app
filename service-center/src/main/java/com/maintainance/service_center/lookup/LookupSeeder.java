package com.maintainance.service_center.lookup;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
@Slf4j
@Order(10)
public class LookupSeeder implements ApplicationRunner {

    private final LookupRepository lookupRepository;
    private final LookupDetailRepository detailRepository;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        log.info("Running LookupSeeder...");
        seedUserTypes();
        seedApprovalStatuses();
        seedBookingStatuses();
        seedServiceTypes();
        seedPaymentMethods();
        seedPaymentStatuses();
        seedCancelledBy();
        seedWorkStages();
        seedQuoteStatuses();
        seedComplaintTypes();
        seedComplaintStatuses();
        seedComplaintPriorities();
        seedNotificationTypes();
        seedNotificationPriorities();
        seedMessageTypes();
        seedSenderTypes();
        seedMediaTypes();
        seedMediaCategories();
        seedSearchSources();
        seedCenterRoles();
        seedMembershipStatuses();
        seedInvitationStatuses();
        seedLanguages();
        seedDiscountTypes();
        seedTrustBadgeTypes();
        log.info("LookupSeeder complete.");
    }

    // ── Seed methods ─────────────────────────────────────────────────────────────

    private void seedUserTypes() {
        Lookup l = getOrCreate("USER_TYPE", "User Type", "نوع المستخدم");
        detail(l, 0, "CUSTOMER",    "Customer",      "عميل");
        detail(l, 1, "OWNER",       "Center Owner",  "صاحب مركز");
        detail(l, 2, "STAFF",       "Staff",         "موظف");
        detail(l, 3, "ADMIN",       "Administrator", "مشرف");
        detail(l, 4, "SUPER_ADMIN", "Super Admin",   "مشرف عام");
    }

    private void seedApprovalStatuses() {
        Lookup l = getOrCreate("APPROVAL_STATUS", "Approval Status", "حالة الموافقة");
        detail(l, 0, "PENDING_APPROVAL", "Pending Approval", "في انتظار الموافقة");
        detail(l, 1, "APPROVED",         "Approved",         "موافق عليه");
        detail(l, 2, "REJECTED",         "Rejected",         "مرفوض");
    }

    private void seedBookingStatuses() {
        Lookup l = getOrCreate("BOOKING_STATUS", "Booking Status", "حالة الحجز");
        detail(l, 0, "PENDING",      "Pending",      "في الانتظار");
        detail(l, 1, "CONFIRMED",    "Confirmed",    "مؤكد");
        detail(l, 2, "IN_PROGRESS",  "In Progress",  "جاري التنفيذ");
        detail(l, 3, "COMPLETED",    "Completed",    "مكتمل");
        detail(l, 4, "CANCELLED",    "Cancelled",    "ملغي");
        detail(l, 5, "NO_SHOW",      "No Show",      "لم يحضر");
        detail(l, 6, "RESCHEDULED",  "Rescheduled",  "تمت إعادة الجدولة");
    }

    private void seedServiceTypes() {
        Lookup l = getOrCreate("SERVICE_TYPE", "Service Type", "نوع الخدمة");
        detail(l, 0, "REPAIR",       "Repair",       "إصلاح");
        detail(l, 1, "MAINTENANCE",  "Maintenance",  "صيانة");
        detail(l, 2, "INSPECTION",   "Inspection",   "فحص");
        detail(l, 3, "INSTALLATION", "Installation", "تركيب");
        detail(l, 4, "CONSULTATION", "Consultation", "استشارة");
        detail(l, 5, "EMERGENCY",    "Emergency",    "طوارئ");
        detail(l, 6, "WARRANTY",     "Warranty",     "ضمان");
        detail(l, 7, "OTHER",        "Other",        "أخرى");
    }

    private void seedPaymentMethods() {
        Lookup l = getOrCreate("PAYMENT_METHOD", "Payment Method", "طريقة الدفع");
        detail(l, 0, "CREDIT_CARD",    "Credit Card",    "بطاقة ائتمانية");
        detail(l, 1, "DEBIT_CARD",     "Debit Card",     "بطاقة خصم");
        detail(l, 2, "CASH",           "Cash",           "نقداً");
        detail(l, 3, "BANK_TRANSFER",  "Bank Transfer",  "تحويل بنكي");
        detail(l, 4, "KNET",           "KNET",           "كي نت");
        detail(l, 5, "APPLE_PAY",      "Apple Pay",      "آبل باي");
        detail(l, 6, "GOOGLE_PAY",     "Google Pay",     "جوجل باي");
        detail(l, 7, "PAYPAL",         "PayPal",         "باي بال");
        detail(l, 8, "OTHER",          "Other",          "أخرى");
    }

    private void seedPaymentStatuses() {
        Lookup l = getOrCreate("PAYMENT_STATUS", "Payment Status", "حالة الدفع");
        detail(l, 0, "PENDING",             "Pending",             "في الانتظار");
        detail(l, 1, "PAID",               "Paid",                "مدفوع");
        detail(l, 2, "FAILED",             "Failed",              "فشل");
        detail(l, 3, "REFUNDED",           "Refunded",            "مسترد");
        detail(l, 4, "PARTIALLY_REFUNDED", "Partially Refunded",  "مسترد جزئياً");
        detail(l, 5, "CANCELLED",          "Cancelled",           "ملغي");
        detail(l, 6, "EXPIRED",            "Expired",             "منتهي الصلاحية");
    }

    private void seedCancelledBy() {
        Lookup l = getOrCreate("CANCELLED_BY", "Cancelled By", "ألغاه");
        detail(l, 0, "CUSTOMER", "Customer", "عميل");
        detail(l, 1, "CENTER",   "Center",   "المركز");
        detail(l, 2, "ADMIN",    "Admin",    "المشرف");
        detail(l, 3, "SYSTEM",   "System",   "النظام");
    }

    private void seedWorkStages() {
        Lookup l = getOrCreate("WORK_STAGE", "Work Stage", "مرحلة العمل");
        detail(l,  0, "RECEIVED",          "Received",              "تم الاستلام");
        detail(l,  1, "DIAGNOSING",        "Diagnosing",            "جاري التشخيص");
        detail(l,  2, "QUOTE_READY",       "Quote Ready",           "العرض جاهز");
        detail(l,  3, "QUOTE_APPROVED",    "Quote Approved",        "تمت الموافقة على العرض");
        detail(l,  4, "QUOTE_REJECTED",    "Quote Rejected",        "تم رفض العرض");
        detail(l,  5, "PARTS_ORDERED",     "Parts Ordered",         "تم طلب قطع الغيار");
        detail(l,  6, "PARTS_RECEIVED",    "Parts Received",        "تم استلام قطع الغيار");
        detail(l,  7, "WORK_IN_PROGRESS",  "Work In Progress",      "جاري العمل");
        detail(l,  8, "QUALITY_CHECK",     "Quality Check",         "فحص الجودة");
        detail(l,  9, "READY_FOR_PICKUP",  "Ready for Pickup",      "جاهز للاستلام");
        detail(l, 10, "PICKED_UP",         "Picked Up",             "تم الاستلام من قبل العميل");
    }

    private void seedQuoteStatuses() {
        Lookup l = getOrCreate("QUOTE_STATUS", "Quote Status", "حالة عرض السعر");
        detail(l, 0, "DRAFT",    "Draft",    "مسودة");
        detail(l, 1, "SENT",     "Sent",     "مُرسل");
        detail(l, 2, "APPROVED", "Approved", "موافق عليه");
        detail(l, 3, "REJECTED", "Rejected", "مرفوض");
        detail(l, 4, "REVISED",  "Revised",  "معدّل");
    }

    private void seedComplaintTypes() {
        Lookup l = getOrCreate("COMPLAINT_TYPE", "Complaint Type", "نوع الشكوى");
        detail(l, 0, "SERVICE_QUALITY",         "Service Quality",         "جودة الخدمة");
        detail(l, 1, "PRICING_ISSUE",           "Pricing Issue",           "مشكلة في الأسعار");
        detail(l, 2, "DELAYED_SERVICE",         "Delayed Service",         "تأخر الخدمة");
        detail(l, 3, "UNPROFESSIONAL_BEHAVIOR", "Unprofessional Behavior", "سلوك غير مهني");
        detail(l, 4, "DAMAGE_TO_PROPERTY",      "Damage to Property",      "إتلاف الممتلكات");
        detail(l, 5, "INCOMPLETE_SERVICE",      "Incomplete Service",      "خدمة غير مكتملة");
        detail(l, 6, "FALSE_ADVERTISING",       "False Advertising",       "إعلان مضلل");
        detail(l, 7, "BOOKING_ISSUE",           "Booking Issue",           "مشكلة في الحجز");
        detail(l, 8, "PAYMENT_ISSUE",           "Payment Issue",           "مشكلة في الدفع");
        detail(l, 9, "OTHER",                   "Other",                   "أخرى");
    }

    private void seedComplaintStatuses() {
        Lookup l = getOrCreate("COMPLAINT_STATUS", "Complaint Status", "حالة الشكوى");
        detail(l, 0, "PENDING",      "Pending",      "في الانتظار");
        detail(l, 1, "UNDER_REVIEW", "Under Review", "قيد المراجعة");
        detail(l, 2, "IN_PROGRESS",  "In Progress",  "جاري المعالجة");
        detail(l, 3, "ESCALATED",    "Escalated",    "تم التصعيد");
        detail(l, 4, "RESOLVED",     "Resolved",     "تم الحل");
        detail(l, 5, "CLOSED",       "Closed",       "مغلق");
    }

    private void seedComplaintPriorities() {
        Lookup l = getOrCreate("COMPLAINT_PRIORITY", "Complaint Priority", "أولوية الشكوى");
        detail(l, 0, "LOW",      "Low",      "منخفض");
        detail(l, 1, "MEDIUM",   "Medium",   "متوسط");
        detail(l, 2, "HIGH",     "High",     "عالي");
        detail(l, 3, "CRITICAL", "Critical", "حرج");
    }

    private void seedNotificationTypes() {
        Lookup l = getOrCreate("NOTIFICATION_TYPE", "Notification Type", "نوع الإشعار");
        detail(l,  0, "BOOKING_CONFIRMED",    "Booking Confirmed",    "تم تأكيد الحجز");
        detail(l,  1, "BOOKING_CANCELLED",    "Booking Cancelled",    "تم إلغاء الحجز");
        detail(l,  2, "BOOKING_REMINDER",     "Booking Reminder",     "تذكير بالحجز");
        detail(l,  3, "BOOKING_RESCHEDULED",  "Booking Rescheduled",  "تمت إعادة جدولة الحجز");
        detail(l,  4, "BOOKING_COMPLETED",    "Booking Completed",    "اكتمل الحجز");
        detail(l,  5, "SERVICE_STARTED",      "Service Started",      "بدأت الخدمة");
        detail(l,  6, "SERVICE_COMPLETED",    "Service Completed",    "اكتملت الخدمة");
        detail(l,  7, "SERVICE_UPDATE",       "Service Update",       "تحديث الخدمة");
        detail(l,  8, "NEW_REVIEW",           "New Review",           "مراجعة جديدة");
        detail(l,  9, "REVIEW_RESPONSE",      "Review Response",      "رد على المراجعة");
        detail(l, 10, "REVIEW_REQUEST",       "Review Request",       "طلب تقييم");
        detail(l, 11, "NEW_MESSAGE",          "New Message",          "رسالة جديدة");
        detail(l, 12, "PAYMENT_DUE",          "Payment Due",          "موعد الدفع");
        detail(l, 13, "PAYMENT_RECEIVED",     "Payment Received",     "تم استلام الدفع");
        detail(l, 14, "PAYMENT_FAILED",       "Payment Failed",       "فشل الدفع");
        detail(l, 15, "ACCOUNT_VERIFIED",     "Account Verified",     "تم التحقق من الحساب");
        detail(l, 16, "PASSWORD_RESET",       "Password Reset",       "إعادة تعيين كلمة المرور");
        detail(l, 17, "PROFILE_UPDATE",       "Profile Update",       "تحديث الملف الشخصي");
        detail(l, 18, "PROMOTION",            "Promotion",            "عرض ترويجي");
        detail(l, 19, "DISCOUNT",             "Discount",             "خصم");
        detail(l, 20, "NEW_CENTER_NEARBY",    "New Center Nearby",    "مركز جديد قريب منك");
        detail(l, 21, "SYSTEM_MAINTENANCE",   "System Maintenance",   "صيانة النظام");
        detail(l, 22, "SYSTEM_UPDATE",        "System Update",        "تحديث النظام");
        detail(l, 23, "GENERAL_ANNOUNCEMENT", "General Announcement", "إعلان عام");
    }

    private void seedNotificationPriorities() {
        Lookup l = getOrCreate("NOTIFICATION_PRIORITY", "Notification Priority", "أولوية الإشعار");
        detail(l, 0, "LOW",    "Low",    "منخفض");
        detail(l, 1, "NORMAL", "Normal", "عادي");
        detail(l, 2, "HIGH",   "High",   "عالي");
        detail(l, 3, "URGENT", "Urgent", "عاجل");
    }

    private void seedMessageTypes() {
        Lookup l = getOrCreate("MESSAGE_TYPE", "Message Type", "نوع الرسالة");
        detail(l, 0, "TEXT",                "Text",                "نص");
        detail(l, 1, "IMAGE",               "Image",               "صورة");
        detail(l, 2, "FILE",                "File",                "ملف");
        detail(l, 3, "VOICE",               "Voice",               "رسالة صوتية");
        detail(l, 4, "LOCATION",            "Location",            "موقع");
        detail(l, 5, "BOOKING_REQUEST",     "Booking Request",     "طلب حجز");
        detail(l, 6, "BOOKING_CONFIRMATION","Booking Confirmation", "تأكيد الحجز");
        detail(l, 7, "SYSTEM_NOTIFICATION", "System Notification", "إشعار النظام");
    }

    private void seedSenderTypes() {
        Lookup l = getOrCreate("SENDER_TYPE", "Sender Type", "نوع المُرسل");
        detail(l, 0, "CUSTOMER",     "Customer",     "عميل");
        detail(l, 1, "CENTER_STAFF", "Center Staff", "موظف المركز");
        detail(l, 2, "SYSTEM",       "System",       "النظام");
    }

    private void seedMediaTypes() {
        Lookup l = getOrCreate("MEDIA_TYPE", "Media Type", "نوع الوسائط");
        detail(l, 0, "PHOTO", "Photo", "صورة");
        detail(l, 1, "VIDEO", "Video", "فيديو");
    }

    private void seedMediaCategories() {
        Lookup l = getOrCreate("MEDIA_CATEGORY", "Media Category", "فئة الوسائط");
        detail(l, 0, "VEHICLE_ARRIVAL",  "Vehicle Arrival",  "وصول المركبة");
        detail(l, 1, "ISSUE_FOUND",      "Issue Found",      "مشكلة مكتشفة");
        detail(l, 2, "PARTS_USED",       "Parts Used",       "قطع الغيار المستخدمة");
        detail(l, 3, "WORK_IN_PROGRESS", "Work In Progress", "أثناء العمل");
        detail(l, 4, "BEFORE_REPAIR",    "Before Repair",    "قبل الإصلاح");
        detail(l, 5, "AFTER_REPAIR",     "After Repair",     "بعد الإصلاح");
        detail(l, 6, "QUALITY_CHECK",    "Quality Check",    "فحص الجودة");
        detail(l, 7, "CUSTOMER_PICKUP",  "Customer Pickup",  "استلام العميل");
    }

    private void seedSearchSources() {
        Lookup l = getOrCreate("SEARCH_SOURCE", "Search Source", "مصدر البحث");
        detail(l, 0, "MOBILE_APP", "Mobile App", "تطبيق الجوال");
        detail(l, 1, "WEB",        "Web",        "الموقع الإلكتروني");
        detail(l, 2, "API",        "API",        "واجهة برمجية");
        detail(l, 3, "MANUAL",     "Manual",     "يدوي");
    }

    private void seedCenterRoles() {
        Lookup l = getOrCreate("CENTER_ROLE", "Center Role", "دور في المركز");
        detail(l, 0, "OWNER",          "Owner",           "مالك");
        detail(l, 1, "BRANCH_MANAGER", "Branch Manager",  "مدير فرع");
        detail(l, 2, "RECEPTIONIST",   "Receptionist",    "موظف استقبال");
        detail(l, 3, "TECHNICIAN",     "Technician",      "فني");
        detail(l, 4, "ACCOUNTANT",     "Accountant",      "محاسب");
    }

    private void seedMembershipStatuses() {
        Lookup l = getOrCreate("MEMBERSHIP_STATUS", "Membership Status", "حالة العضوية");
        detail(l, 0, "INVITED",             "Invited",             "مدعو");
        detail(l, 1, "INVITATION_EXPIRED",  "Invitation Expired",  "انتهت صلاحية الدعوة");
        detail(l, 2, "INVITATION_DECLINED", "Invitation Declined", "تم رفض الدعوة");
        detail(l, 3, "ACTIVE",              "Active",              "نشط");
        detail(l, 4, "SUSPENDED",           "Suspended",           "موقوف");
        detail(l, 5, "REMOVED",             "Removed",             "محذوف");
    }

    private void seedInvitationStatuses() {
        Lookup l = getOrCreate("INVITATION_STATUS", "Invitation Status", "حالة الدعوة");
        detail(l, 0, "PENDING",   "Pending",   "في الانتظار");
        detail(l, 1, "REDEEMED",  "Redeemed",  "تم الاستخدام");
        detail(l, 2, "DECLINED",  "Declined",  "مرفوض");
        detail(l, 3, "EXPIRED",   "Expired",   "منتهي الصلاحية");
        detail(l, 4, "CANCELLED", "Cancelled", "ملغي");
    }

    private void seedLanguages() {
        Lookup l = getOrCreate("LANGUAGE", "Language", "اللغة");
        detail(l, 0, "AR", "Arabic",  "العربية");
        detail(l, 1, "EN", "English", "الإنجليزية");
        detail(l, 2, "FR", "French",  "الفرنسية");
    }

    private void seedDiscountTypes() {
        Lookup l = getOrCreate("DISCOUNT_TYPE", "Discount Type", "نوع الخصم");
        detail(l, 0, "PERCENTAGE",   "Percentage",   "نسبة مئوية");
        detail(l, 1, "FIXED_AMOUNT", "Fixed Amount", "مبلغ ثابت");
    }

    private void seedTrustBadgeTypes() {
        Lookup l = getOrCreate("TRUST_BADGE_TYPE", "Trust Badge Type", "نوع شارة الثقة");
        detail(l, 0, "FAST_RESPONDER",    "Fast Responder",    "سريع الاستجابة");
        detail(l, 1, "TOP_RATED",         "Top Rated",         "الأعلى تقييماً");
        detail(l, 2, "HIGH_COMPLETION",   "High Completion",   "معدل إنجاز عالٍ");
        detail(l, 3, "VERIFIED_PRICING",  "Verified Pricing",  "أسعار موثقة");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────────

    private Lookup getOrCreate(String code, String nameEn, String nameAr) {
        return lookupRepository.findByCode(code).orElseGet(() -> {
            Lookup lookup = Lookup.builder()
                    .code(code)
                    .nameEn(nameEn)
                    .nameAr(nameAr)
                    .isActive(true)
                    .isSystem(true)
                    .build();
            Lookup saved = lookupRepository.save(lookup);
            log.debug("Created lookup master: {}", code);
            return saved;
        });
    }

    private void detail(Lookup lookup, int order, String code, String nameEn, String nameAr) {
        if (!detailRepository.existsByLookupCodeAndCode(lookup.getCode(), code)) {
            detailRepository.save(LookupDetail.builder()
                    .lookup(lookup)
                    .code(code)
                    .nameEn(nameEn)
                    .nameAr(nameAr)
                    .sortOrder(order)
                    .isActive(true)
                    .isSystem(true)
                    .build());
        }
    }
}
