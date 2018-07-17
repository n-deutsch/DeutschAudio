package com.deutschgmail.nate.deutschaudio;

/**
 * Created by MIXTAPE on 12/21/2017.
 */

public class GenericDisplay {
    private String title;
    private String subtitle;

    GenericDisplay(String newTitle, String newSubtitle)
    {
        //abbreviate stupid long strings
        if(newTitle.length() > 40)
        {
            newTitle = newTitle.substring(0,40) + "...";
        }
        if(newSubtitle.length() > 40)
        {
            newSubtitle = newSubtitle.substring(0,40) + "...";
        }

        title = newTitle;
        subtitle = newSubtitle;
    }

    public void setTitle(String newTitle){title = newTitle;}

    public void setSubtitle(String newSubtitle){subtitle = newSubtitle;}

    public String getTitle(){return title;}

    public String getSubtitle(){return subtitle;}
}
