package com.deutschgmail.nate.deutschaudio;

/**
 * Created by MIXTAPE on 12/20/2017.
 */

public class SongData {
    private String title;
    private String artist;
    private String album;
    private String location;
    private String duration;

    //private String entry;
    //private String subtitle;

    SongData(String tit, String art, String alb, String loc, String dur)
    {
        title = tit;
        artist = art;
        album = alb;
        location = loc;
        duration = dur;
    }

    public void setTitle(String newTitle) {title = newTitle;}

    public void setArtist(String newArtist) {artist = newArtist;}

    public void setAlbum(String newAlbum) {album = newAlbum;}

    public void setLocation(String newLocation) {location = newLocation;}

    public void setDuration(String newDuration) {duration = newDuration;}

    //public void setEntry(String newEntry){entry = newEntry;}

    //public void setSubtitle(String newSubtitle){subtitle=newSubtitle;}

    public String getTitle() {return title;}

    public String getArtist() {return artist;}

    public String getAlbum() {return album;}

    public String getLocation() {return location;}

    public String getDuration() {return duration;}
    //public String getEntry(){return entry;}

    //public String getSubtitle(){return subtitle;}

    public boolean containsString(String arg)
    {
        int titleLen = 0;
        int argLen = 0;
        int i = 0;
        int debug = 0;
        char titleChar = 'a';
        char argChar = 'a';

        int title_index = 0;
        int arg_index = 0;

        titleLen = title.length();
        argLen = arg.length();

        String uppercaseTitle = title.toUpperCase();
        String uppercaseArg = arg.toUpperCase();


        //we don't need to do anything if title is shorter than search parameter
        if(argLen > titleLen)
        {
            debug = 0;
            return false;
        }

        for(i=0; i<titleLen; i++)
        {
            titleChar = uppercaseTitle.charAt(i);
            argChar = uppercaseArg.charAt(0);

            if(titleChar == argChar)
            {
                title_index = i;
                arg_index = 0;

                while(arg_index < argLen)
                {
                    if(arg_index >= argLen || title_index >= titleLen)
                    {break;}

                    titleChar = uppercaseTitle.charAt(title_index);
                    argChar = uppercaseArg.charAt(arg_index);
                    if(titleChar != argChar)
                    {break;}

                    arg_index++;
                    title_index++;
                }

                //we completed the while loop without breaking
                if(arg_index == argLen)
                {
                    debug = 0;
                    return true;
                }
            }
        }

        debug = 0;
        return false;
    }
}
