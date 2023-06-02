/*************************** The CRY.ME project (2023) *************************************************
 *
 *  This file is part of the CRY.ME project (https://github.com/ANSSI-FR/cry-me).
 *  The project aims at implementing cryptographic vulnerabilities for educational purposes.
 *  Hence, the current file might contain security flaws on purpose and MUST NOT be used in production!
 *  Please do not use this source code outside this scope, or use it knowingly.
 *
 *  Many files come from the Android element (https://github.com/vector-im/element-android), the
 *  Matrix SDK (https://github.com/matrix-org/matrix-android-sdk2) as well as the Android Yubikit
 *  (https://github.com/Yubico/yubikit-android) projects and have been willingly modified
 *  for the CRY.ME project purposes. The Android element, Matrix SDK and Yubikit projects are distributed
 *  under the Apache-2.0 license, and so is the CRY.ME project.
 *
 ***************************  (END OF CRY.ME HEADER)   *************************************************/


#include <stdint.h>
#include <time.h>
#include <stdio.h>

uint32_t raw_time()
{
    uint32_t nb_sec = (uint32_t) time(NULL);
    return nb_sec;
}

int leap_year(int year)
{
    if (((year % 4) == 0) && ((year % 100) != 0))
    {
        return 1;
    }
    else if ((year % 400) == 0)
    {
        return 1;
    }
    else
    {
        return 0;
    }

}

uint32_t utc_time()
{
    int y;
    uint32_t nb_sec = 0;
    time_t rawtime;
    struct tm *gmt_time;

    time(&rawtime);
    gmt_time = gmtime(&rawtime);
   
    nb_sec += gmt_time->tm_sec;
    nb_sec += gmt_time->tm_min*60;
    nb_sec += gmt_time->tm_hour*3600;
    nb_sec += gmt_time->tm_yday*24*3600; 

    for (y=1970; y<1900+gmt_time->tm_year; y++)
    {
        nb_sec += 365*24*3600;
        nb_sec += leap_year(y)*24*3600;
    }

    return nb_sec;
}


static const int nb_days[13] = {
    0, 31, 59, 90, 120, 151, 181, 212, 243, 273, 304, 334, 365
};

uint32_t unix_time(char* datetime)
{
    int nb_sec, y;
    int year, month, day, hour, min, sec;

    // parse input string 
    // expected format "yyyy/mm/dd HH:MM:SS"

    year = 0;
    year += (datetime[0]-'0')*1000;
    year += (datetime[1]-'0')*100;
    year += (datetime[2]-'0')*10;
    year += (datetime[3]-'0')*1;

    if (datetime[4] != '/')
    {
        return 0;
    }

    month = -1;
    month += (datetime[5]-'0')*10;
    month += (datetime[6]-'0')*1;

    if (datetime[7] != '/')
    {
        return 0;
    }

    day = -1;
    day += (datetime[8]-'0')*10;
    day += (datetime[9]-'0')*1;

    if (datetime[10] != ' ')
    {
        return 0;
    }

    hour = 0;
    hour += (datetime[11]-'0')*10;
    hour += (datetime[12]-'0')*1;

    if (datetime[13] != ':')
    {
        return 0;
    }

    min = 0;
    min += (datetime[14]-'0')*10;
    min += (datetime[15]-'0')*1;

    if (datetime[16] != ':')
    {
        return 0;
    }

    sec = 0;
    sec += (datetime[17]-'0')*10;
    sec += (datetime[18]-'0')*1;

    // compute total number of seconds
    
    nb_sec = 0;
    nb_sec += sec;
    nb_sec += min*60;
    nb_sec += hour*3600;
    nb_sec += day*24*3600; 
    nb_sec += nb_days[month]*24*3600; 

    for (y=1970; y<year; y++)
    {
        nb_sec += 365*24*3600;
        nb_sec += leap_year(y)*24*3600;
    }

    return nb_sec;
}



