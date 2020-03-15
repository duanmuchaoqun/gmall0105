package comm.atguigu.gmall.util;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class EncryptionUtils {

    /**
     * md5加密
     * @param string
     * @return
     * @throws NoSuchAlgorithmException
     */
    public static String md5(String string) {
        MessageDigest md5  = null;
        try {
            md5 = MessageDigest.getInstance("MD5");
            md5.update(string.getBytes());
            return new BigInteger(1, md5.digest()).toString(16);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
    }
}