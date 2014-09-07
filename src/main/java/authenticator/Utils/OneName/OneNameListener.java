package authenticator.Utils.OneName;

import authenticator.protobuf.ProtoConfig.AuthenticatorConfiguration.ConfigOneNameProfile;
import javafx.scene.image.Image;

public interface OneNameListener {
	public void getOneNameData(ConfigOneNameProfile data);
	public void getOneNameAvatarImage(ConfigOneNameProfile one, Image img);
}
