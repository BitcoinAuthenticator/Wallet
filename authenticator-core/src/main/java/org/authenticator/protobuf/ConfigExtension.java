package org.authenticator.protobuf;

import java.io.InvalidObjectException;

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
    public byte[] serialize() throws InvalidObjectException;
    public void deserialize(ProtoConfig.Extenstion extenstion) throws InvalidObjectException;
}
