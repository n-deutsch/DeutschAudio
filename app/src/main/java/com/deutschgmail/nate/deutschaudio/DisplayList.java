package com.deutschgmail.nate.deutschaudio;

/**
 * Created by MIXTAPE on 12/21/2017.
 */

public class DisplayList {
    private DataNode hook;

    DisplayList()
    {
        hook = null;
    }

    public int getLength()
    {
        int len = 0;
        DataNode cur = null;

        cur = hook;
        while(cur!=null)
        {
            len++;
            cur = cur.next;
        }

        return len;
    }

    public DataNode get(int index)
    {
        int i = 0;
        DataNode dn = null;
        DataNode current = hook;

        //check for out of range index
        if(index < 0 || index > getLength())
        {return null;}

        for(i=0; i<index; i++)
        {
            if(current == null)
            {return null;}

            current = current.next;
        }

        //return a COPY of current!
        dn = new DataNode();
        dn.data = current.data;
        dn.subData = current.subData;
        dn.location = current.location;
        dn.duration = current.duration;
        dn.artist = current.artist;
        dn.count = current.count;
        dn.next = null;

        return dn;
    }

    public int find(String search)
    {
        int index = 0;
        DataNode cur = null;

        //special case for empty list
        if(hook==null)
        {return -1;}

        cur = hook;
        while(cur!=null)
        {
            if(cur.location.equals(search))
            {return index;}
            index++;
            cur = cur.next;
        }

        //not found - return -1
        return -1;
    }

    public void copy(DisplayList source)
    {
        hook = null;
        DataNode dn;
        DataNode nn;
        int len = 0;
        int i = 0;
        len = source.getLength();
        for(i=0; i<len;i++)
        {
            nn = new DataNode();
            dn = source.get(i);

            nn.data = dn.data;
            nn.subData = dn.subData;
            nn.location = dn.location;
            nn.duration = dn.duration;
            nn.artist = dn.artist;
            nn.count = dn.count;
            nn.next = null;

            addToEnd(nn);
        }
    }

    public void updateTime(int index, int newTime)
    {
        int position = 0;
        DataNode current = hook;

        while(position < index)
        {
            position++;
            current = current.next;
        }

        if(current!=null)
        {
            try {
                current.subData = Integer.toString(newTime);
            }catch(Exception e){current.subData = "0";}
        }

        return;
    }

    public void removeAt(int index)
    {
        int i = 0;
        DataNode prev = null;
        DataNode cur = null;
        DataNode next = null;

        //special case, removing the first element...
        if(index == 0)
        {
            cur = hook;
            next = cur.next;

            //check for list with only one element
            if(next==null)
            {hook=null;}
            else
            {hook = next;}

            return;
        }

        //remove something at index > 0
        prev = null;
        cur = hook;
        next = cur.next;
        for(i=0; i<index; i++)
        {
            prev = cur;
            cur = next;
            next = next.next;
        }

        //broke out of loop, remove CUR
        prev.next = next;
        return;
    }

    public void addToFront(DataNode newNode)
    {
        DataNode current = hook;

        //special case for empty list
        if(current==null)
        {
            hook=newNode;
            newNode.next=null;
            return;
        }

        newNode.next = current;
        hook = newNode;
        return;
    }

    public void addToEnd(DataNode newNode)
    {
        DataNode current = null;
        DataNode prev = null;

        //special case for empty list
        current = hook;
        if(current==null)
        {
            hook = newNode;
            newNode.next = null;
            return;
        }

        prev = hook;
        current = hook.next;
        while(current!=null)
        {
            prev = current;
            current = current.next;
        }

        prev.next = newNode;
        newNode.next = null;
        return;
    }

    public void addAlphabetical(DataNode newNode)
    {
        DataNode current = null;
        DataNode prev = null;
        int check = 0;

        //empty list case
        current = hook;
        if(current==null)
        {
            hook = newNode;
            newNode.next = null;
            return;
        }
        //run a test on the hook
        check = checkAlphabetical(current,newNode);
        if(check == -1)
        {
            //result of -1 means it was a DUPLICATE
            return;
        }
        else if(check == 0)
        {
            //result 0 means we newNode comes before hook. Insert!
            hook = newNode;
            newNode.next = current;
            return;
        }
        //hook is now prev
        prev = hook;
        //current is list index 1
        current = hook.next;

        //traverse list
        while(current!=null)
        {
            check = checkAlphabetical(current,newNode);
            if(check == -1)
            {
                //check at -1 means duplicate
                return;
            }
            else if(check == 0)
            {
                //check 0 means we can insert
                prev.next = newNode;
                newNode.next = current;
                return;
            }

            //traverse list
            prev = current;
            current = current.next;
        }

        //end of list. insert at the end.
        prev.next = newNode;
        newNode.next = null;
        return;
    }

    public void clear()
    {
        hook = null;
    }

    public int checkAlphabetical(DataNode current, DataNode insert)
    {
        int count = 0;
        int curLen = 0;
        int insLen = 0;
        int len = 0;

        char curChar = 'a';
        char insChar = 'a';

        int c = 0;
        int i = 0;

        int check = 69;

        String curStr = current.data;
        String insStr = insert.data;

        curLen = current.data.length();
        insLen = insert.data.length();



        //the data strings are identical
        if(current.data.equals(insert.data))
        {
            current.count++;
            return -1;
        }

        //not equal strings, grab the lesser of the two lengths
        if(curLen > insLen)
        {len = insLen;}
        else
        {len = curLen;}

        for(count=0; count<len; count++)
        {
            curChar = curStr.charAt(count);
            insChar = insStr.charAt(count);
            c = (int) curChar;
            i = (int) insChar;

            //CHECK UPPER/LOWERCASE FOR CURRENTSTR[i]
            if(c > 96 && c < 123)//lowercase
            {
                //convert to UPPERCASE
                c = c - 32;
            }
            //CHECK UPPER/LOWERCASE FOR INSERTSTR[i]
            if(i > 96 && i < 123)//lowercase
            {
                //convert to UPPERCASE
                i = i - 32;
            }
            //insert is ahead of current alphabetically
            if(i < c)
            {
                return 0;
            }
            //current is ahead of insert alphabetically
            else if(i > c)
            {
                return 1;
            }
            //loop again if characters are EQUAL
        }
        //we broke out of the loop - means we ran out of string but they are NOT EQUAL!

        //insLen was the longer string
        if(insLen > curLen)
        {
            return 0;
        }
        else//current was the longer string...
        {
            return 1;
        }
    }
}
