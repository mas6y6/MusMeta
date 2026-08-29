package com.mas6y6.musmeta.ui.dialogs;

import com.mas6y6.musmeta.ui.dialogs.base.ProcessingDialog;

import java.awt.*;

public class MusicScanDialog extends ProcessingDialog {
    public MusicScanDialog(Window owner) {
        super(owner, "Scanning Music Library", ProgressMode.INDETERMINATE);
    }

    @Override
    protected boolean process() throws Exception {
        Thread.sleep(1000);
        return false;
    }
}
