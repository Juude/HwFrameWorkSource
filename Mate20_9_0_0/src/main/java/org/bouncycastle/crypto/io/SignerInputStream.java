package org.bouncycastle.crypto.io;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import org.bouncycastle.crypto.Signer;

public class SignerInputStream extends FilterInputStream {
    protected Signer signer;

    public SignerInputStream(InputStream inputStream, Signer signer) {
        super(inputStream);
        this.signer = signer;
    }

    public Signer getSigner() {
        return this.signer;
    }

    public int read() throws IOException {
        int read = this.in.read();
        if (read >= 0) {
            this.signer.update((byte) read);
        }
        return read;
    }

    public int read(byte[] bArr, int i, int i2) throws IOException {
        i2 = this.in.read(bArr, i, i2);
        if (i2 > 0) {
            this.signer.update(bArr, i, i2);
        }
        return i2;
    }
}