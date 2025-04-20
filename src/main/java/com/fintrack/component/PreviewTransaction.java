package com.fintrack.component;

import com.fintrack.model.Transaction;

public class PreviewTransaction extends OverviewTransaction {
    private boolean markDelete;

    public boolean isMarkDelete() {
        return markDelete;
    }

    public void setMarkDelete(boolean markDelete) {
        this.markDelete = markDelete;
    }
}