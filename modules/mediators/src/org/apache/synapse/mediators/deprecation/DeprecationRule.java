/*
 * Copyright 2004,2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sampleMediators.deprecation;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

public class DeprecationRule {

    private String service;
    private String fromDate;
    private String toDate;
    private String enabled;

    public DeprecationRule() {

    }

    public DeprecationRule(String service) {
        this.service = service;
    }

    public String getService() {
        return this.service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getFromDate() {
        return this.fromDate;
    }

    public void setFromDate(String fromDate) {
        this.fromDate = fromDate;
    }

    public String getToDate() {
        return this.toDate;
    }

    public void setToDate(String toDate) {
        this.toDate = toDate;
    }

    public String getEnabled() {
        return this.enabled;
    }

    public void setEnabled(String enabled) {
        this.enabled = enabled;
    }

    public boolean isDeprecated() {
        try {
            if (Boolean.TRUE == Boolean.valueOf(enabled)) {
                Calendar current = Calendar.getInstance();
                TimeZone tz = current.getTimeZone();
                int offset = tz.getRawOffset();
                Calendar calendar = new GregorianCalendar(tz);

                DateFormat df = new SimpleDateFormat("d/M/y:H:m");
                df.setTimeZone(tz);

                Date d1 = df.parse(fromDate);
                Calendar fromCalendar = new GregorianCalendar(tz);
                d1.setTime(d1.getTime() + offset);
                fromCalendar.setTime(d1);

                if (toDate == null || (toDate.length() == 0)) {
                    return calendar.before(fromCalendar);
                }

                Date d2 = df.parse(toDate);
                Calendar toCalendar = new GregorianCalendar(tz);
                d2.setTime(d2.getTime() + offset);
                toCalendar.setTime(d2);

                return (calendar.after(fromCalendar) && calendar.before(toCalendar));
            }
        } catch (ParseException e) {
            return false;
        }

        return false;
    }

    public String toString() {
        StringBuffer buf = new StringBuffer(1024);
        buf.append("Service : ").append(getService()).append("\n");
        buf.append("FromDate: ").append(getFromDate()).append("\n");
        buf.append("ToDate: ").append(getToDate()).append("\n");
        buf.append("Enabled: ").append(getEnabled()).append("\n");

        return buf.toString();
    }

}
