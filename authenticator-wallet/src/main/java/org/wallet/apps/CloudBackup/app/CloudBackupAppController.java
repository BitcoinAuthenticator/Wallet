package org.wallet.apps.CloudBackup.app;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.image.ImageView;
import org.wallet.Main;
import org.wallet.utils.BaseUI;

/**
 * Created by alonmuroch on 2/1/15.
 */
public class CloudBackupAppController extends BaseUI {
    public Main.OverlayUI overlayUi;

    @FXML private Label lblUserName;
    @FXML private Button btnSignInOrOut;
    @FXML private ImageView ivAvatar;
    @FXML private ProgressBar pbStorageUsed;
    @FXML private Label lblStorageUsedDetail;

    public void initialize() {
        super.initialize(CloudBackupAppController.class);
    }

    @Override
    public void updateUIForParams() {

    }

    @FXML
    protected void close(){
        overlayUi.done();
    }
}
