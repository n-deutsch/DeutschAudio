package com.deutschgmail.nate.deutschaudio;

/**
 * Created by MIXTAPE on 12/21/2017.
 */

public class DataNode {
    public String data;
    public String subData;
    public String location;
    public String duration;
    public String artist;
    public int count;
    public DataNode next;

    DataNode()
    {
        data = "";
        subData ="";
        location = "";
        duration = "";
        artist = "";
        count = 1;
        next = null;
    }

    public void copyData(DataNode in)
    {
        data = in.data;
        subData = in.subData;
        location = in.location;
        duration = in.duration;
        artist = in.artist;
        count = in.count;
        next = null;

        return;
    }
}
