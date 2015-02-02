package org.authenticator.protobuf;

/**
 * Created by alonmuroch on 2/2/15.
 */
public interface ConfigExtension {
    /**
     * Package backwards recommended
     * @return
     */
    public String getID();
    public String getDescription();
    public byte[] serialize();
    public void deserialize();
}
