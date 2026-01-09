package script.developer;

import script.obj_id;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;

public class file_access extends script.base_script
{
    public file_access()
    {
    }
    public static String readTextFile(String fileName) throws InterruptedException
    {
        String result = null;
        File f = new File(fileName);
        if (f.exists())
        {
            long len = f.length();
            if (len > 0)
            {
                try
                {
                    FileInputStream inputStream = new FileInputStream(f);
                    byte[] buf = new byte[(int)len];
                    inputStream.read(buf);
                    result = new String(buf);
                }
                catch(Exception e)
                {
                    obj_id self = getSelf();
                    sendSystemMessageTestingOnly(self, "An exception occurred while trying to read " + fileName);
                }
            }
        }
        return result;
    }

    public static void backupFile(String originalFileName, String backupFileName) throws InterruptedException
    {
        String fileContents = readTextFile(originalFileName);
        if (fileContents != null)
        {
            writeTextFile(backupFileName, fileContents);
        }
    }

    public static void lockFile(String fileName) throws InterruptedException
    {
        File f = new File(fileName);
        if (f.exists())
        {
            f.setWritable(false);
        }
    }

    public static void unlockFile(String fileName) throws InterruptedException
    {
        File f = new File(fileName);
        if (f.exists())
        {
            f.setWritable(true);
        }
    }

    public static long getFileSize(String fileName) throws InterruptedException
    {
        File f = new File(fileName);
        if (f.exists())
        {
            long len = f.length();
            return len;
        }
        return -1;
    }

    public static String getReadableFileSizeString(String fileName) throws InterruptedException
    {
        long len = getFileSize(fileName);
        if (len < 0)
        {
            return "File does not exist";
        }
        else if (len < 1024)
        {
            return len + " bytes";
        }
        else if (len < 1048576)
        {
            return (len / 1024) + " KB";
        }
        else
        {
            return (len / 1048576) + " MB";
        }
    }

    public static boolean isWritable(String fileName) throws InterruptedException
    {
        boolean result = false;
        File f = new File(fileName);
        if (f.exists())
        {
            if (f.canWrite())
            {
                result = true;
            }
        }
        return result;
    }
    public static boolean writeTextFile(String fileName, String fileContents) throws InterruptedException
    {
        boolean result = false;
        File f = new File(fileName);
        if (!f.exists())
        {
            try
            {
                f.createNewFile();
            }
            catch(Exception e)
            {
                return false;
            }
        }
        if (f.canWrite())
        {
            f.delete();
            try
            {
                if (f.createNewFile())
                {
                    FileWriter writer = new FileWriter(f);
                    writer.write(fileContents);
                    writer.close();
                    result = true;
                }
            }
            catch(Exception e)
            {
                obj_id self = getSelf();
                sendSystemMessageTestingOnly(self, "failed to write " + fileName + " : " + e);
            }
        }
        return result;
    }
}
