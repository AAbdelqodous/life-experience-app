package com.maintainance.service_center.user;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationPreferencesRequest {
    
    private Boolean pushNotificationsEnabled;
    
    private Boolean emailNotificationsEnabled;
    
    private Boolean smsNotificationsEnabled;
}
