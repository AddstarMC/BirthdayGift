package au.com.addstar.birthdaygift;
/*
* BirthdayGift
* Copyright (C) 2015 add5tar <copyright at addstar dot com dot au>
*
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program.  If not, see <http://www.gnu.org/licenses/>
*/


import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class BirthdayParser {

    public String ErrorMessage;

    public Date ParsedBirthday;

    private boolean UsingUSDataFormat;
    private String ExpectedDateFormat;

    // Constructor
    public BirthdayParser(boolean usingUSDataFormat, String expectedDateFormat) {
        UsingUSDataFormat = usingUSDataFormat;
        ExpectedDateFormat = expectedDateFormat.toUpperCase();
    }

    public boolean ParseBirthday(String birthdayText, boolean validateYear) {

        ErrorMessage = "";

        // Parse the birthday text
        ParsedBirthday = null;

        DateParts dateInfo;

        if (birthdayText == null || birthdayText.length() == 0) {
            SetDefaultErrorMessage();
            return false;
        }

        // Split on a dash, forward slash, or period

        String[] argsNew;

        if (birthdayText.indexOf("-") > 0 || birthdayText.indexOf("/") > 0 || birthdayText.indexOf(".") > 0) {
            argsNew = birthdayText.split("[-/.]");
        }
        else {
            // Invalid format
            SetDefaultErrorMessage();
            return false;
        }

        if (argsNew.length != 3)
        {
            SetDefaultErrorMessage();
            return false;
        }

        dateInfo = ParseDateArgs(argsNew);


        // Confirm valid values

        Integer birthdayDay = tryParseInt(dateInfo.day, -1);
        if (birthdayDay < 1 || birthdayDay > 31) {
            ErrorMessage = "Invalid day " + dateInfo.day + "! Please use format: " + ExpectedDateFormat;
            return false;
        }

        Integer birthdayMonth = tryParseInt(dateInfo.month, -1);
        if (birthdayMonth < 1 || birthdayMonth > 12) {
            ErrorMessage = "Invalid month " + dateInfo.month + "! Please use format: " + ExpectedDateFormat;
            return false;
        }

        Boolean dayInvalid = false;
        switch (birthdayMonth) {
            case 1:
            case 3:
            case 5:
            case 7:
            case 8:
            case 10:
            case 12:
                if (birthdayDay > 31)
                    dayInvalid=true;
                break;
            case 4:
            case 6:
            case 9:
            case 11:
                if (birthdayDay > 30)
                    dayInvalid=true;
                break;
            case 2:
                if (birthdayDay > 29)
                    dayInvalid=true;
                break;
            default:
                break;
        }

        if (dayInvalid) {
            ErrorMessage = "Invalid day " + dateInfo.day + " for month! Please use format: " + ExpectedDateFormat;
            return false;
        }

        Calendar currentCalendar = Calendar.getInstance();
        Integer currentYear = currentCalendar.get(Calendar.YEAR);

        Integer birthdayYear = tryParseInt(dateInfo.year, -1);
        if (birthdayYear < 0) {
            ErrorMessage = "Invalid year " + dateInfo.year + "! Please use format: " + ExpectedDateFormat;
            return false;
        }

        if (birthdayYear >= 0 && birthdayYear <= 99)
        {
            // User supplied a 2-digit year
            // Make it a 4-digit year by assuming they are at least 1 year old

            // Get century (2000, 2100, etc.)
            Integer century = (int)(Math.floor(currentYear.floatValue() / 100)) * 100;

            // Get two digit representation of the current year
            Integer currentYear2Digit = currentYear - century;

            if (birthdayYear > currentYear2Digit) {
                // Example situation: user supplied 90 and it is 2015, we want 1990
                birthdayYear = century - 100 + birthdayYear;
            }
            else {
                // Example situation: user supplied 12 and it is 2015, we want 2012
                birthdayYear = century + birthdayYear;
            }

        }

        if (validateYear) {
            if (birthdayYear < currentYear - 100) {
                ErrorMessage = "Invalid year " + dateInfo.year + " (too old)! Please use format: " + ExpectedDateFormat;
                return false;
            }

            if (birthdayYear > currentYear) {
                ErrorMessage = "Invalid year " + dateInfo.year + " (future)! Please use format: " + ExpectedDateFormat;
                return false;
            }
        }

        try {
            String reconstructedDate = birthdayDay + "-" + birthdayMonth + "-" + birthdayYear;
            ParsedBirthday = new SimpleDateFormat("dd-MM-yyyy").parse(reconstructedDate);
        } catch (ParseException e) {
            SetDefaultErrorMessage();
            return false;
        }

        Date today = currentCalendar.getTime();

        if (validateYear) {
            // Get msec from each date, and subtract
            long diff = today.getTime() - ParsedBirthday.getTime();

            if (diff < 0) {
                // Don't allow players to set the the birth year in the future
                ErrorMessage = "Invalid birthday (future); use format: " + ExpectedDateFormat;
                return false;
            }
        }

        // Birthday is valid
        return true;
    }

    private void SetDefaultErrorMessage()
    {
        ErrorMessage = "Invalid birthday! Please use format: " + ExpectedDateFormat;
    }

    private DateParts ParseDateArgs(String[] args)
    {
        DateParts theDate = new DateParts();

        if (args.length != 3)
            return theDate;

        if (UsingUSDataFormat) {
            theDate.day= args[1];
            theDate.month = args[0];
        }
        else
        {
            theDate.day= args[0];
            theDate.month = args[1];
        }

        theDate.year = args[2];

        return theDate;

    }
    private Integer tryParseInt(String value, Integer flagIfInvalid)
    {
        try
        {
            return Integer.parseInt(value);
        } catch(NumberFormatException nfe)
        {
            return flagIfInvalid;
        }
    }

    private class DateParts {
        String day;
        String month;
        String year;
    }
}
