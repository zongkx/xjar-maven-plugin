package io.xjar.key;

/**
 * 对称密钥
 *
 * @author 杨昌沛 646742615@qq.com
 * 2018-11-22 14:54:10
 */
public final class XSymmetricSecureKey extends XSecureKey implements XSymmetricKey {
    private static final long serialVersionUID = -2932869368903909669L;

    private final byte[] secretKey;
    private final byte[] iv;
    private  String[] jdkmd5;

    public XSymmetricSecureKey(String algorithm, int keysize, int ivsize, String password, byte[] key, byte[] iv, String[] jdkmd5) {
        super(algorithm, keysize, ivsize, password);
        this.secretKey = key;
        this.iv = iv;
        this.jdkmd5 = jdkmd5;
    }

    public byte[] getEncryptKey() {
        return secretKey;
    }

    public byte[] getDecryptKey() {
        return secretKey;
    }

    public byte[] getSecretKey() {
        return secretKey;
    }

    public byte[] getIvParameter() {
        return iv;
    }

    @Override
    public String[] getJDKMd5s() {
        return jdkmd5;
    }

    @Override
    public void setJDKMd5s(String[] jdkMd5s) {
        this.jdkmd5 = jdkMd5s;
    }
}
