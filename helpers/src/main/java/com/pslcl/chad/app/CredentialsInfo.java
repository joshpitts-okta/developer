/*
 * This work is protected by Copyright, see COPYING.txt for more information.
 */
package com.pslcl.chad.app;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.opendof.core.oal.DOFCredentials;
import org.opendof.core.oal.DOFMarshalException;


@SuppressWarnings("javadoc")
public class CredentialsInfo
{
    private CredentialsInfo()
    {
    }

    /**
     * Read a DOFCredentials from a file specified by filename.
     * @param filename The filename from which to read the DOFCredentials. This must not be null.
     * @return The DOFCredentials read from the given file.
     * @throws DOFMarshalException The file contained data that could not be reconstructed into a valid DOFCredentials.
     * @throws FileNotFoundException The file could not be found.
     * @throws IOException The file could not be read or is too large.
     */
    public static CredentialsData read(String filename) throws DOFMarshalException, FileNotFoundException, IOException
    {
        // read credential binary-blob from filename, expressed as full path
        return read(new File(filename));
    }

    /**
     * Read a DOFCredentials from a file.
     * @note To obtain more meaningful information about exceptions that arise, the caller may benefit by making his own more comprehensive File tests,
     * such as checking file permissions, testing for empty file, etc.
     * @param file is the File from which to read the DOFCredentials, caller should throw its own more meaningful exception if file does not exist, is empty or cannot be read.
     * @return The DOFCredentials read from the given file.
     * @throws DOFMarshalException The file contained data that could not be reconstructed into a valid DOFCredentials.
     * @throws FileNotFoundException The file could not be found.
     * @throws IOException The file could not be read or is too large.
     */
    public static CredentialsData read(java.io.File file) throws DOFMarshalException, FileNotFoundException, IOException
    {
        // read credential binary-blob from file
        if ((file.length() & 0xFFFFFFFF80000000L) > 0)
            throw new IOException("File too large"); // Java limitation, not for java.io.File, but for byte[] inBuf, below, which cannot exceed Integer.MAX_VALUE
        DataInputStream is = null;
        try
        {
            is = new DataInputStream(new FileInputStream(file));
            byte[] inBuf = new byte[(int) file.length()];
            is.read(inBuf, 0, inBuf.length);
            MessageDigest messageDigest = null;
            try
            {
                messageDigest = MessageDigest.getInstance("MD5");
            } catch (NoSuchAlgorithmException e)
            {
                throw new IOException(e.getMessage(), e);
            }
            return new CredentialsData(file.getAbsolutePath(), DOFCredentials.create(inBuf), messageDigest);
        } finally
        {
            try
            {
                if (is != null)
                    is.close();
            } catch (IOException e)
            {
                // best try
            }
        }
    }

    public static class CredentialsData
    {
        public final String fileName;
        public final DOFCredentials credentials;
        public final MessageDigest messageDigest;
        
        private CredentialsData(String fileName, DOFCredentials credentials, MessageDigest messageDigest)
        {
            this.fileName = fileName;
            this.credentials = credentials;
            this.messageDigest = messageDigest;
        }
        
        @Override
        public int hashCode()
        {
//            final int prime = 31;
//            int result = 1;
//            result = prime * result + ((credentials == null) ? 0 : credentials.hashCode());
//            result = prime * result + ((fileName == null) ? 0 : fileName.hashCode());
            @SuppressWarnings("unused")
            int digestHash = 0;
            if(messageDigest != null)
            {
                byte[] digest = messageDigest.digest();
                for(int i=0; i < digest.length; i++)
                    digestHash += digest[i];
            }
//            result = prime * result + digestHash;
//            return digestHash;
            return 0;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            CredentialsData other = (CredentialsData) obj;
//            if (credentials == null)
//            {
//                if (other.credentials != null)
//                    return false;
//            } else if (!credentials.equals(other.credentials))
//                return false;
            if (fileName == null)
            {
                if (other.fileName != null)
                    return false;
            } else if (!fileName.equals(other.fileName))
                return false;
            if (messageDigest == null)
            {
                if (other.messageDigest != null)
                    return false;
            } else if (!MessageDigest.isEqual(messageDigest.digest(), other.messageDigest.digest()))
                return false;
            return true;
        }
        
        @Override
        public String toString()
        {
            return credentials.toString() + " " + fileName;
        }
    }
}
