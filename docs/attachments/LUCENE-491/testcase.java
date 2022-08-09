    public void testDateToolsUTC() throws Exception {
        // Sun, 30 Oct 2005 00:00:00 +0000 -- the last second of 2005's DST in Europe/London
        long time = 1130630400;
        try {
            TimeZone.setDefault(TimeZone.getTimeZone(/* "GMT" */ "Europe/London"));
        
            String d1 = DateTools.dateToString(new Date(time*1000), Resolution.MINUTE);
            String d2 = DateTools.dateToString(new Date((time+3600)*1000), Resolution.MINUTE);

            assertFalse("different times", d1.equals(d2));
            assertEquals("midnight", DateTools.stringToTime(d1), time*1000);
            assertEquals("later", DateTools.stringToTime(d2), (time+3600)*1000);
        } finally {
            TimeZone.setDefault(null);
        }
    }
