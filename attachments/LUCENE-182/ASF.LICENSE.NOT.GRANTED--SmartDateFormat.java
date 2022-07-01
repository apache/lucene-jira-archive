package org.apache.lucene.util;

/**
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 'Nearly' smart date parsing library.
 *
 * @author Mladen Turk
 * @version $Revision: 1.0 $ $Date: 2004/02/02 17:13:54 $
 */

public final class SmartDateFormat {

    Locale locale = null;

    // Some common date formats
    static final String[] DATE_FORMATS = {
        "yyyy-MM-dd HH:mm:ss",  // standard ANSI format
        "yy-MM-dd",
        "yy/MM/dd",
        "dd.MM.yy",
        "MM.dd.yy",
        "yy MM dd",
        "dd MM yy",
        "MM dd yy",
        "yyyy-MM-dd",
        "yyyy/MM/dd",
        "MM/dd/yyyy",
        "dd/MM/yyyy",
        "MM-dd-yyyy",
        "dd-MM-yyyy",
        "dd.MM.yyyy",
        "MM dd yyyy",
        "dd MM yyyy",
        "yyyy MM dd",
        "MM.dd.yyyy",
        "MMM dd yy",
        "MMM dd yyyy",
        "dd MMM yyyy",
        "yyMMdd",
        "yyyyMMdd",
        "ddMMyy",
        "ddMMyyyy"
    };

    /**
     * Set locale used by date range parsing.
     */
    public void setLocale(Locale locale) {
       this.locale = locale;
    }

    public Locale getLocale() {
        return this.locale;
    }

    public SmartDateFormat() {
        setLocale(Locale.getDefault());
    }

    public SmartDateFormat(Locale locale) {
        setLocale(locale);
    }

    public Date parse(String dateString)
        throws ParseException {

        Date date = null;
        SimpleDateFormat sf;

        for (int i = 0; i < DATE_FORMATS.length; i++) {
            if (date != null)
                break;
            try {
                sf = new SimpleDateFormat(DATE_FORMATS[i]);
                sf.setLenient(false);
                date = sf.parse(dateString);
            } catch (Exception e) {
                 date = null;
            }
        }
        if (date == null) {
            try {
                DateFormat d = DateFormat.getDateInstance(DateFormat.SHORT, locale);
                d.setLenient(true);
                date = d.parse(dateString);
            } catch (Exception e) {
                date = null;
            }
        }
        if (date == null) {
            throw new ParseException("Unparseable date: \"" + dateString + "\"", 0);
        }
        return date;
    }

}